# Shared by the PowerShell checks, which load it with a dot: one [PASS]/[FAIL] line per check, and the
# count of the failures, which the check exits with.

$script:fails = 0

function Pass($name, $detail) { Write-Host "[PASS] ${name}: $detail" }
function Fail($name, $detail) { Write-Host "[FAIL] ${name}: $detail"; $script:fails++ }
