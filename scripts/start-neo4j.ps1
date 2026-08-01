$JAVA11 = "C:\Program Files\Eclipse Adoptium\jdk-11.0.32.9-hotspot"
$env:JAVA_HOME = $JAVA11

Write-Host "JAVA_HOME set to JDK 11 ($JAVA11)" -ForegroundColor Green
Write-Host "Starting Neo4j 4.4.4..." -ForegroundColor Cyan

& "D:\neo4j-community-4.4.4\bin\neo4j.bat" console
