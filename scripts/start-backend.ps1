$JAVA17 = "C:\Program Files\Eclipse Adoptium\jdk-17.0.20.8-hotspot"
$env:JAVA_HOME = $JAVA17
$env:Path = "C:\temp\maven\apache-maven-3.9.6\bin;$env:PATH"
$env:MAVEN_OPTS = "-Dfile.encoding=UTF-8"

Write-Host "JAVA_HOME set to JDK 17 ($JAVA17)" -ForegroundColor Green
Write-Host "Starting Spring Boot backend..." -ForegroundColor Cyan

Set-Location -Path (Join-Path $PSScriptRoot "..\backend")
mvn.cmd --% clean package -DskipTests; if ($?) { java -jar target/zhi-platform-1.0.0.jar }
