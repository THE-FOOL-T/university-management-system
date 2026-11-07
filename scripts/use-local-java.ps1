# Optional helper for the portable verification JDK supplied in .tools on this machine.
$jdk = Get-ChildItem (Join-Path $PSScriptRoot '../.tools') -Directory -Filter 'jdk-17*' -ErrorAction SilentlyContinue | Select-Object -First 1
if (-not $jdk) { throw 'No project-local JDK. Install JDK 17 and set JAVA_HOME (see README.md).' }
$env:JAVA_HOME = $jdk.FullName
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
$env:MAVEN_USER_HOME = Join-Path $PSScriptRoot '../.tools/maven-user'
Write-Host "Java 17 selected for this PowerShell session."
