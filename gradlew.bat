@echo off
setlocal
set "ROOT=%~dp0"
set "GRADLE_BOOTSTRAP_PATH=%ROOT%gradle\wrapper\gradle-wrapper.jar"
powershell.exe -NoProfile -Command "$ErrorActionPreference='Stop'; $file=$env:GRADLE_BOOTSTRAP_PATH; $sha='81a82aaea5abcc8ff68b3dfcb58b3c3c429378efd98e7433460610fecd7ae45f'; if(-not (Test-Path -LiteralPath $file)){ $tmp=$file+'.download'; try { Invoke-WebRequest -Uri 'https://downloads.gradle.org/distributions/gradle-8.13-wrapper.jar' -UseBasicParsing -OutFile $tmp; $sum=(Get-FileHash -Algorithm SHA256 -LiteralPath $tmp).Hash.ToLowerInvariant(); if($sum -ne $sha){throw 'Gradle wrapper checksum mismatch'}; Move-Item -Force -LiteralPath $tmp -Destination $file } finally { Remove-Item -LiteralPath $tmp -Force -ErrorAction SilentlyContinue } }; $sum=(Get-FileHash -Algorithm SHA256 -LiteralPath $file).Hash.ToLowerInvariant(); if($sum -ne $sha){throw 'Unsafe Gradle wrapper JAR refused'}"
if errorlevel 1 exit /b 1
if defined JAVA_HOME (
    "%JAVA_HOME%\bin\java.exe" -classpath "%GRADLE_BOOTSTRAP_PATH%" org.gradle.wrapper.GradleWrapperMain %*
) else (
    java -classpath "%GRADLE_BOOTSTRAP_PATH%" org.gradle.wrapper.GradleWrapperMain %*
)
exit /b %ERRORLEVEL%
