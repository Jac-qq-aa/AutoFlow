@echo off
setlocal
set "MAVEN_VERSION=3.9.11"
set "WRAPPER_HOME=%~dp0.mvn\wrapper\dists\apache-maven-%MAVEN_VERSION%"
set "MAVEN_CMD=%WRAPPER_HOME%\apache-maven-%MAVEN_VERSION%\bin\mvn.cmd"
if not exist "%MAVEN_CMD%" (
  echo Downloading Apache Maven %MAVEN_VERSION%...
  if not exist "%WRAPPER_HOME%" mkdir "%WRAPPER_HOME%"
  powershell -NoProfile -ExecutionPolicy Bypass -Command "$ProgressPreference='SilentlyContinue'; $zip=Join-Path '%WRAPPER_HOME%' 'maven.zip'; Invoke-WebRequest -Uri 'https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/%MAVEN_VERSION%/apache-maven-%MAVEN_VERSION%-bin.zip' -OutFile $zip; Expand-Archive -LiteralPath $zip -DestinationPath '%WRAPPER_HOME%' -Force; Remove-Item -LiteralPath $zip"
  if errorlevel 1 exit /b 1
)
call "%MAVEN_CMD%" %*

