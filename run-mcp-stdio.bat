@echo off
REM Elavon Codegen MCP - VS Code Stdio Mode Runner

echo 🚀 Starting Elavon Codegen MCP in stdio mode for VS Code...

REM Set environment variables
if not defined ELAVON_BASE_URL set ELAVON_BASE_URL=https://api.sandbox.elavon.com
if not defined ELAVON_API_KEY set ELAVON_API_KEY=your-api-key

REM Check if JAR exists
set JAR_PATH=modelcontextprotocol\target\modelcontextprotocol-1.0.0-SNAPSHOT.jar
if not exist "%JAR_PATH%" (
    echo ❌ JAR file not found: %JAR_PATH%
    echo Please run build-and-run.bat first to build the project.
    pause
    exit /b 1
)

REM Check for specs
if not exist "engine\src\main\resources\specs\elavon-openapi.json" (
    echo ⚠️  Warning: engine\src\main\resources\specs\elavon-openapi.json not found
    echo   The MCP server will start but code generation may fail
)

echo ✅ Starting MCP server in stdio mode...
echo 📝 This will run until you stop it (Ctrl+C)
echo 🔗 For VS Code integration, see .vscode\settings.json
echo.

REM Run in stdio mode
java -jar "%JAR_PATH%" --stdio

echo.
echo MCP server stopped.
pause

