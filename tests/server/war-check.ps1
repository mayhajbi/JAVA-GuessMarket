# The checks of test-war.bat: the WAR file that IntelliJ builds (out\artifacts\guessmarket\guessmarket.war)
# holds the jar of every module the server needs and every third party library, does not hold the
# servlet API (Tomcat brings its own), and was built after the last change of the sources that are
# inside it. Prints one [PASS]/[FAIL] line per check; exits with the number of failures.

. (Join-Path $PSScriptRoot 'report.ps1')

$root = (Resolve-Path (Join-Path $PSScriptRoot '..\..')).Path
$war = Join-Path $root 'out\artifacts\guessmarket\guessmarket.war'
$rebuild = 'In IntelliJ: Build Artifacts, guessmarket, Rebuild - and then restart Tomcat.'
$libraries = 'gm-dto.jar', 'gm-api.jar', 'gm-engine.jar', 'gson-*.jar', 'angus-activation.jar',
    'jakarta.activation-api.jar', 'jakarta.xml.bind-api.jar', 'jaxb-core.jar', 'jaxb-impl.jar'

if (-not (Test-Path $war)) {
    Fail 'war-exists' "there is no $war. $rebuild"
    exit $script:fails
}
Pass 'war-exists' 'out\artifacts\guessmarket\guessmarket.war'

Add-Type -AssemblyName System.IO.Compression.FileSystem
$zip = [IO.Compression.ZipFile]::OpenRead($war)
$entries = $zip.Entries | ForEach-Object { $_.FullName }
$zip.Dispose()

$inLib = $entries | Where-Object { $_ -like 'WEB-INF/lib/*.jar' } | ForEach-Object { $_.Substring('WEB-INF/lib/'.Length) }
$missing = $libraries | Where-Object { $pattern = $_; -not ($inLib | Where-Object { $_ -like $pattern }) }
if ($missing) { Fail 'war-libraries' "WEB-INF\lib has no $($missing -join ', ')" }
else { Pass 'war-libraries' "WEB-INF\lib holds the $($libraries.Count) jar files the server needs" }

$servletApi = $entries | Where-Object { $_ -like '*servlet-api*' }
if ($servletApi) { Fail 'war-no-servlet-api' "the WAR holds $($servletApi -join ', '), which Tomcat brings itself" }
else { Pass 'war-no-servlet-api' 'the servlet API is not inside the WAR' }

# The newest file that goes into the WAR: the sources of the four modules, and the web.xml file.
$sources = 'dto\src', 'api\src', 'engine\src', 'server\src', 'server\web' | ForEach-Object { Join-Path $root $_ }
$newest = Get-ChildItem -Path $sources -Recurse -File | Sort-Object LastWriteTime | Select-Object -Last 1
$built = (Get-Item $war).LastWriteTime
if ($newest.LastWriteTime -gt $built) {
    Fail 'war-up-to-date' "$($newest.FullName.Substring($root.Length + 1)) changed after the WAR was built. $rebuild"
} else {
    Pass 'war-up-to-date' "built $($built.ToString('yyyy-MM-dd HH:mm')), after the last change of the sources"
}

exit $script:fails
