@echo off
chcp 65001 > nul
setlocal

echo =======================================================================
echo   Starting Ojas — Local-First Enterprise AI Agent
echo =======================================================================

if "%GEMINI_API_KEY%"=="" (
    echo [WARNING] GEMINI_API_KEY environment variable is not set!
    echo Please set it in your environment:
    echo   set GEMINI_API_KEY=your_key_here
    echo.
)

REM Use JAVA_HOME if set, otherwise fallback to java on PATH or OpenJDK 27
set "JAVA_EXE=java"
if exist "%JAVA_HOME%\bin\java.exe" (
    set "JAVA_EXE=%JAVA_HOME%\bin\java.exe"
) else if exist "%USERPROFILE%\.jdks\openjdk-27\bin\java.exe" (
    set "JAVA_EXE=%USERPROFILE%\.jdks\openjdk-27\bin\java.exe"
)

REM Check if shaded jar exists, if not build it
if not exist "target\ojas-1.0-SNAPSHOT.jar" (
    echo [INFO] Building Ojas fat JAR with Maven...
    call mvn clean package -DskipTests
)

"%JAVA_EXE%" --enable-native-access=ALL-UNNAMED -Djava.net.preferIPv4Stack=true -jar "target\ojas-1.0-SNAPSHOT.jar" %*

endlocal
