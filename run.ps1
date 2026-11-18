# Ojas Agent PowerShell Launcher
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8

Write-Host "=======================================================================" -ForegroundColor Cyan
Write-Host "  Starting Ojas — Local-First Enterprise AI Agent" -ForegroundColor White
Write-Host "=======================================================================" -ForegroundColor Cyan

if (-not $env:GEMINI_API_KEY) {
    Write-Host "[WARNING] GEMINI_API_KEY environment variable is not set!" -ForegroundColor Yellow
    Write-Host "Set it in PowerShell via: `$env:GEMINI_API_KEY = 'your_key_here'" -ForegroundColor Yellow
}

$javaExe = "java"
if ($env:JAVA_HOME -and (Test-Path "$env:JAVA_HOME\bin\java.exe")) {
    $javaExe = "$env:JAVA_HOME\bin\java.exe"
} elseif (Test-Path "$HOME\.jdks\openjdk-27\bin\java.exe") {
    $javaExe = "$HOME\.jdks\openjdk-27\bin\java.exe"
}

if (-not (Test-Path "target\ojas-1.0-SNAPSHOT.jar")) {
    Write-Host "[INFO] Building Ojas fat JAR with Maven..." -ForegroundColor Cyan
    & mvn clean package -DskipTests
}

& $javaExe "--enable-native-access=ALL-UNNAMED" "-Djava.net.preferIPv4Stack=true" -jar "target\ojas-1.0-SNAPSHOT.jar" $args
