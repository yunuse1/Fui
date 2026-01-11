$javaPath = Get-Command java | Select-Object -ExpandProperty Source
$javaHome = Split-Path (Split-Path $javaPath)
Write-Host "Detected JAVA_HOME: $javaHome"
$env:JAVA_HOME = $javaHome
.\gradlew.bat :server:assemble --stacktrace
