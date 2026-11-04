# Builds the portable Windows package: dist\CyberQuest-portable.zip
# Run from the folder that contains pom.xml: powershell -ExecutionPolicy Bypass -File .\build-protable.ps1
$ErrorActionPreference = 'Stop'
Set-Location $PSScriptRoot

Write-Host '1/5 Building the application jar (Maven)...'
mvn clean package -DskipTests
if ($LASTEXITCODE -ne 0) { throw 'The Maven build failed.' }

$jar = Get-ChildItem -Path 'target' -Filter 'demo-*.jar' | Select-Object -First 1 if (-not $jar) { throw 'Could not find the built jar in the target folder.'}

$out = Join-Path $PSScriptRoot 'dist\CyberQuest'
if (Test-Path $out) { Remove-Item $out -Recurse -Force}
New-Item -ItemType Directory -Path (Join-Path $out 'app') -Force | Out-Null
Copy-Item $jar.FullName (Join-Path $out 'app\cyberquest.jar')

Write-Host '2/5 Find the JDK...'
$javaHome = $env:JAVA_HOME
if (-not $javaHome) {
    $javaExe = (Get-command java -ErrorAction Stop).Source
    $javaHome = Split-Path (Split-Path $javaExe)
}

$javaBin = Join-Path $javaHome 'bin\java.exe'
if (-not (Test-Path $javaBin)) {
    throw "Could not find a JDK at '$javaHome'. Set the JAVA_HOME environment variable to your JDK folder and try again."

}
Write-Host "     Using $javaHome"

Write-Host '3/5 Create a small java runtime (jlink)...'
$runtime = Join-Path $out 'runtime'
$jlink = Join-Path $javaHome 'bin\jlink.exe'
if (Test-Path $jlink) {
    $mods = 'java.se,jdk.unsupported'
    $available = & $javaBin --list-modules
    if ($available -match 'jdk.crypto.ec') { $mods = $mods + ',jdk.crypto.ec'}
    & $jlink --add-modules $mods --strip-debug --no-header-files --no-man-pages --output
    $runtime 
    if ($LASTEXITCODE -ne 0) { throw 'jlink failed.'}
} else {
    Write-Warning 'jlink was not found. Copying the whole JDK instead (larger package).'
    Copy-Item $javaHome $runtime -Recurse 
}
if (-not (Test-Path (Join-Path $runtime '\bin\javaw.exe'))) { throw 'The runtime folder is missing javaw.exe'}

Write-Host '4/5 Adding launch scripts and instructions...'
Copy-Item (Join-Path $PSScriptRoot 'portable\start.bat') $out
Copy-Item (Join-Path $PSScriptRoot 'portable\stop.bat') $out
Copy-Item (Join-Path $PSScriptRoot 'portable\README-PORTABLE.txt') $out

Write-Host '5/5 Creating the zip file...'
$zip = Join-Path $PSScriptRoot 'dist\CyberQuest-portable.zip'
if (Test-Path $zip) { Remove-Item $zip -Force}
Compress-Archive -Path $out -DestinationPath $zip
$size = [math]::Round((Get-Item $zip).Length / 1MB, 1)
Write-Host ' '
Write-Host "Done: $zip ($size MB)"
Write-Host 'Test it: extract the zip somewhere else, double-click start.bat and sign in.'