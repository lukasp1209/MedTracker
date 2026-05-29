$ErrorActionPreference = "Stop"

$projectRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$apkSource = Join-Path $projectRoot "app\build\outputs\apk\debug\app-debug.apk"
$apkTarget = Join-Path $projectRoot "MedTracker.apk"

if (-not (Get-Command java -ErrorAction SilentlyContinue)) {
    $jdkCandidates = @(
        "C:\Program Files\Android\Android Studio\jbr",
        "C:\Program Files\Java\jdk-21",
        "C:\Program Files\Java\jdk-17",
        "C:\Program Files\Eclipse Adoptium\jdk-21",
        "C:\Program Files\Eclipse Adoptium\jdk-17"
    )

    $jdkHome = $jdkCandidates |
        Where-Object { Test-Path -LiteralPath (Join-Path $_ "bin\java.exe") } |
        Select-Object -First 1

    if ($jdkHome) {
        $env:JAVA_HOME = $jdkHome
        $env:Path = "$jdkHome\bin;$env:Path"
    }
    else {
        throw "Java was not found. Install a JDK or set JAVA_HOME before running this script."
    }
}

Push-Location $projectRoot
try {
    & .\gradlew.bat :app:assembleDebug --no-daemon
    if ($LASTEXITCODE -ne 0) {
        throw "Gradle build failed with exit code $LASTEXITCODE"
    }

    if (-not (Test-Path -LiteralPath $apkSource)) {
        throw "APK was not found at expected path: $apkSource"
    }

    Copy-Item -LiteralPath $apkSource -Destination $apkTarget -Force
    Write-Host "APK ready: $apkTarget"
}
finally {
    Pop-Location
}
