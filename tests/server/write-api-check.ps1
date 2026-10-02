# The checks of test-write-api.bat: the write endpoints end to end, with three fresh users.
# A creates an LMSR event and an order book event, B and C trade in them. Every request is sent
# with curl.exe; every number is compared with a value computed here from the formula of the
# LMSR (C = b * ln(sum e^(q/b))) and from the rule that a winning share pays 1, not read from the
# server. Prints one [PASS]/[FAIL] line per check; exits with the number of failures.

$base = 'http://localhost:8080/guessmarket'
$tmp = $env:TEMP
$bodyFile = Join-Path $tmp 'gm_write_body.txt'
$jsonFile = Join-Path $tmp 'gm_write_request.json'
$rnd = '{0}{1}' -f (Get-Random -Maximum 99999), (Get-Random -Maximum 99999)

function New-Session($name) {
    $cookies = Join-Path $tmp "gm_write_cookies_$name.txt"
    Remove-Item $cookies -ErrorAction SilentlyContinue
    $user = 'write_{0}_{1}' -f $name, (Get-Random -Maximum 99999)
    curl.exe -s -o NUL -c $cookies -X POST "$base/login?username=$user" | Out-Null
    [pscustomobject]@{ Cookies = $cookies; User = $user }
}

# Sends one request and returns its status and the text of the answer; $body is an object sent as JSON.
function Send($session, $method, $path, $body = $null) {
    Remove-Item $bodyFile -ErrorAction SilentlyContinue
    $curlArgs = @('-s', '-o', $bodyFile, '-w', '%{http_code}', '-X', $method, '-b', $session.Cookies)
    if ($null -ne $body) {
        [IO.File]::WriteAllText($jsonFile, ($body | ConvertTo-Json -Compress), (New-Object Text.UTF8Encoding($false)))
        $curlArgs += @('-H', 'Content-Type: application/json', '--data-binary', "@$jsonFile")
    }
    $status = curl.exe @curlArgs "$base/$path"
    $text = if (Test-Path $bodyFile) { Get-Content -Raw -Encoding UTF8 $bodyFile } else { '' }
    [pscustomobject]@{ Status = [int]$status; Text = $text }
}

. (Join-Path $PSScriptRoot 'report.ps1')

# The answer has the status, and the text (any case) in it.
function Expect($name, $reply, $status, $text = '') {
    if ($reply.Status -ne $status) { Fail $name "expected status $status, got $($reply.Status): $($reply.Text)"; return }
    if ($text -and $reply.Text.IndexOf($text, [StringComparison]::OrdinalIgnoreCase) -lt 0) {
        Fail $name "the answer should have [$text], and it is: $($reply.Text)"; return
    }
    Pass $name "$status$(if ($text) { " with [$text]" })"
}

function Near($name, $actual, $expected) {
    if ([math]::Abs($actual - $expected) -lt 0.01) { Pass $name "$actual (expected $expected)" }
    else { Fail $name "expected $expected, got $actual" }
}

function Balance($session) { (curl.exe -s -b $session.Cookies "$base/account" | ConvertFrom-Json).balance }

function New-Lmsr($name, $percent, $type) {
    @{ name = $name; description = 'check'; commissionPercent = $percent; commissionType = $type
       firstOption = 'Yes'; secondOption = 'No'; type = 'LMSR'; liquidity = 100 }
}

function New-OrderBook($name, $baseValue, $initial) {
    @{ name = $name; description = 'check'; commissionPercent = 0; commissionType = 'ON_CLOSE'
       firstOption = 'Yes'; secondOption = 'No'; type = 'ORDER_BOOK'; baseValue = $baseValue
       allowMint = $false; initialInvestment = $initial }
}

$a = New-Session 'a'
$b = New-Session 'b'
$c = New-Session 'c'
$nobody = [pscustomobject]@{ Cookies = Join-Path $tmp 'gm_write_no_such_cookies.txt'; User = '' }

# --- no session: every write is refused ---
Expect 'no-session-open' (Send $nobody POST 'event/open?id=1') 401 'not logged in'
Expect 'no-session-buy' (Send $nobody POST 'event/buy' @{ eventId = 1; optionIndex = 0; quantity = 1 }) 401 'not logged in'

# --- the LMSR event: b = 100, 5 percent on every purchase ---
$reply = Send $a POST 'event/create' (New-Lmsr "Write check LMSR $rnd" 5 'ON_PURCHASE')
Expect 'create-lmsr' $reply 200 "Write check LMSR $rnd"
$lmsr = ($reply.Text | ConvertFrom-Json)
if ($lmsr.status -eq 'INACTIVE' -and $lmsr.marketMakerName -eq $a.User) { Pass 'create-lmsr-inactive-and-mine' 'INACTIVE, market maker is the creator' }
else { Fail 'create-lmsr-inactive-and-mine' "status $($lmsr.status), market maker $($lmsr.marketMakerName)" }
Expect 'create-same-name-other-case' (Send $a POST 'event/create' (New-Lmsr "write check lmsr $rnd" 5 'ON_PURCHASE')) 400 'already in use'
$noLiquidity = New-Lmsr "Missing liquidity $rnd" 5 'ON_PURCHASE'; $noLiquidity.Remove('liquidity')
Expect 'create-missing-liquidity' (Send $a POST 'event/create' $noLiquidity) 400 "'liquidity' is missing"
[IO.File]::WriteAllText($jsonFile, '{"name":"broken', (New-Object Text.UTF8Encoding($false)))
Remove-Item $bodyFile -ErrorAction SilentlyContinue
$broken = curl.exe -s -o $bodyFile -w '%{http_code}' -X POST -b $a.Cookies -H 'Content-Type: application/json' --data-binary "@$jsonFile" "$base/event/create"
Expect 'create-broken-json' ([pscustomobject]@{ Status = [int]$broken; Text = $(if (Test-Path $bodyFile) { Get-Content -Raw $bodyFile } else { '' }) }) 400 'not a valid JSON'

# --- opening ---
Expect 'open-not-market-maker' (Send $b POST "event/open?id=$($lmsr.id)") 400 'Only its market maker'
Expect 'open-no-money' (Send $a POST "event/open?id=$($lmsr.id)") 400 'balance'
Expect 'open-as-get' (Send $a GET "event/open?id=$($lmsr.id)") 400 'must be sent as POST'
Expect 'open-no-id' (Send $a POST 'event/open') 400 "'id' is missing"
Expect 'a-deposit' (Send $a POST 'account/deposit?amount=1000') 200 'balance'
Expect 'open-ok' (Send $a POST "event/open?id=$($lmsr.id)") 200 'ACTIVE'
Near 'a-balance-after-open' (Balance $a) (1000 - 100 * [math]::Log(2))
Expect 'open-twice' (Send $a POST "event/open?id=$($lmsr.id)") 400 'opened'

# --- buying: B pays the price of 10 shares of the first option, and 5 percent on top ---
Expect 'b-deposit' (Send $b POST 'account/deposit?amount=500') 200 'balance'
Expect 'buy-missing-quantity' (Send $b POST 'event/buy' @{ eventId = $lmsr.id; optionIndex = 0 }) 400 "'quantity' is missing"
Expect 'buy-missing-option' (Send $b POST 'event/buy' @{ eventId = $lmsr.id; quantity = 5 }) 400 "'optionIndex' is missing"
Expect 'buy-option-out-of-range' (Send $b POST 'event/buy' @{ eventId = $lmsr.id; optionIndex = 5; quantity = 5 }) 400 'option'
Expect 'buy-zero-quantity' (Send $b POST 'event/buy' @{ eventId = $lmsr.id; optionIndex = 0; quantity = 0 }) 400 'positive'
Expect 'buy-unknown-event' (Send $b POST 'event/buy' @{ eventId = 999999; optionIndex = 0; quantity = 5 }) 400 '999999'
$reply = Send $b POST 'event/buy' @{ eventId = $lmsr.id; optionIndex = 0; quantity = 10 }
Expect 'buy-ok' $reply 200 'buyerBalance'
$purchase = $reply.Text | ConvertFrom-Json
$cost = 100 * ([math]::Log([math]::Exp(0.1) + 1) - [math]::Log(2))
Near 'buy-price' $purchase.sharesCost $cost
Near 'buy-commission' $purchase.commissionPaid ($cost * 0.05)
Near 'b-balance-after-buy' $purchase.buyerBalance (500 - $cost * 1.05)

# --- closing: only the market maker, once; the winners are paid 1 a share ---
Expect 'close-not-market-maker' (Send $b POST "event/close?id=$($lmsr.id)&winner=0") 400 'Only its market maker'
Expect 'close-no-winner' (Send $a POST "event/close?id=$($lmsr.id)") 400 "'winner' is missing"
Expect 'close-winner-out-of-range' (Send $a POST "event/close?id=$($lmsr.id)&winner=7") 400 'option'
Expect 'close-ok' (Send $a POST "event/close?id=$($lmsr.id)&winner=0") 200 'CLOSED'
$bAfterLmsr = 500 - $cost * 1.05 + 10
Near 'b-balance-after-close' (Balance $b) $bAfterLmsr
Near 'a-balance-after-close' (Balance $a) (1000 + $cost - 10 + $cost * 0.05)
Expect 'close-twice' (Send $a POST "event/close?id=$($lmsr.id)&winner=0") 400 'closed'
$log = (Send $b GET 'account/log').Text | ConvertFrom-Json
if (@($log).Count -eq 3) { Pass 'b-log-has-deposit-buy-and-payout' '3 movements' } else { Fail 'b-log-has-deposit-buy-and-payout' "$(@($log).Count) movements" }

# --- a blocked user: the purchase that drops the balance below zero goes through, the next action is refused ---
Expect 'a-deposit-again' (Send $a POST 'account/deposit?amount=1000') 200 'balance'
$lmsr2 = (Send $a POST 'event/create' (New-Lmsr "Write check LMSR two $rnd" 0 'ON_CLOSE')).Text | ConvertFrom-Json
Expect 'open-second-event' (Send $a POST "event/open?id=$($lmsr2.id)") 200 'ACTIVE'
$reply = Send $c POST 'event/buy' @{ eventId = $lmsr2.id; optionIndex = 0; quantity = 10 }
Expect 'c-buy-below-zero-goes-through' $reply 200 'buyerBlocked'
if (($reply.Text | ConvertFrom-Json).buyerBlocked) { Pass 'c-is-blocked-now' 'buyerBlocked is true' } else { Fail 'c-is-blocked-now' 'buyerBlocked is false' }
Expect 'c-blocked-buy-refused' (Send $c POST 'event/buy' @{ eventId = $lmsr2.id; optionIndex = 0; quantity = 10 }) 400 'blocked'
Expect 'c-can-still-deposit' (Send $c POST 'account/deposit?amount=5') 200 'balance'

# --- the order book event: d = 1, no commission; A holds 100 pairs after opening ---
$reply = Send $a POST 'event/create' (New-OrderBook "Write check OB $rnd" 1 100)
Expect 'create-ob' $reply 200 'ORDER_BOOK'
$ob = $reply.Text | ConvertFrom-Json
Expect 'create-ob-not-divisible' (Send $a POST 'event/create' (New-OrderBook "Write check OB bad $rnd" 3 100)) 400 'divide'
$noInitial = New-OrderBook "Write check OB missing $rnd" 1 100; $noInitial.Remove('initialInvestment')
Expect 'create-ob-missing-field' (Send $a POST 'event/create' $noInitial) 400 "'initialInvestment' is missing"
Expect 'open-ob' (Send $a POST "event/open?id=$($ob.id)") 200 'ACTIVE'
Expect 'order-missing-price' (Send $a POST 'event/order' @{ eventId = $ob.id; side = 'SELL'; optionIndex = 0; quantity = 10 }) 400 "'price' is missing"
Expect 'order-wrong-side' (Send $a POST 'event/order' @{ eventId = $ob.id; side = 'UP'; optionIndex = 0; quantity = 10; price = 0.6 }) 400 "'side'"
$reply = Send $a POST 'event/order' @{ eventId = $ob.id; userName = 'someone_else'; side = 'SELL'; optionIndex = 0; quantity = 10; price = 0.6 }
Expect 'order-sell-rests-and-ignores-name' $reply 200 'restingQuantity'
Near 'order-sell-resting-quantity' ($reply.Text | ConvertFrom-Json).restingQuantity 10
Expect 'b-deposit-for-order' (Send $b POST 'account/deposit?amount=100') 200 'balance'
$reply = Send $b POST 'event/order' @{ eventId = $ob.id; side = 'BUY'; optionIndex = 0; quantity = 10; price = 0.6 }
Expect 'order-buy-matches' $reply 200 'trades'
$order = $reply.Text | ConvertFrom-Json
Near 'order-buy-filled' $order.filledQuantity 10
$bAfterOrder = $bAfterLmsr + 100 - 6
Near 'order-buy-balance' $order.userBalance $bAfterOrder
Expect 'close-ob' (Send $a POST "event/close?id=$($ob.id)&winner=0") 200 'CLOSED'
Near 'b-balance-after-ob-close' (Balance $b) ($bAfterOrder + 10)

Remove-Item $bodyFile, $jsonFile -ErrorAction SilentlyContinue
exit $script:fails
