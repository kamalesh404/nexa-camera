$ErrorActionPreference = 'Stop'
$version = '8.10.2'
$root = Split-Path -Parent $PSScriptRoot
$dist = Join-Path $root '.gradle-dist'
$install = Join-Path $dist "gradle-$version"
$gradle = Join-Path $install 'bin\gradle.bat'

$systemGradle = Get-Command gradle -ErrorAction SilentlyContinue
if ($systemGradle) {
    & $systemGradle.Source @args
    exit $LASTEXITCODE
}

if (-not (Test-Path $gradle)) {
    New-Item -ItemType Directory -Force -Path $dist | Out-Null
    $zip = Join-Path $dist "gradle-$version-bin.zip"
    $url = "https://services.gradle.org/distributions/gradle-$version-bin.zip"
    Write-Host "Downloading Gradle $version..."
    Invoke-WebRequest -Uri $url -OutFile $zip
    Expand-Archive -Path $zip -DestinationPath $dist -Force
}

& $gradle @args
exit $LASTEXITCODE
