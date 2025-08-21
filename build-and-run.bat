@echo off
REM Elavon Codegen MCP - Build and Run Script for Windows

echo 🚀 Building Elavon Codegen MCP Server...

REM Check prerequisites
where java >nul 2>nul
if %errorlevel% neq 0 (
    echo ❌ Java 17+ is required but not found
    pause
    exit /b 1
)

where mvn >nul 2>nul
if %errorlevel% neq 0 (
    echo ❌ Maven is required but not found
    pause
    exit /b 1
)

echo ✅ Prerequisites check passed

REM Clean and build
echo 🔨 Building project...
call mvn clean compile -DskipTests -q

if %errorlevel% equ 0 (
    echo ✅ Build completed successfully!
) else (
    echo ❌ Build failed. See errors above.
    echo 💡 Note: Some compilation errors are expected in the current version.
    echo    The core MCP server and package scanner should work.
    pause
    exit /b 1
)

REM Create executable JAR (simplified)
echo 📦 Creating executable JAR...
cd modelcontextprotocol
call mvn package -DskipTests -q
cd ..

if exist "modelcontextprotocol\target\modelcontextprotocol-1.0.0-SNAPSHOT.jar" (
    echo ✅ JAR created successfully!
) else (
    echo ❌ JAR creation failed
    pause
    exit /b 1
)

REM Setup configuration
echo ⚙️  Setting up configuration...

if not exist "specs" (
    mkdir specs
    echo 📁 Created specs\ directory
    echo    Please place your OpenAPI specs here:
    echo    - specs\converge-openapi.json
    echo    - specs\elavon-openapi.json
)

REM Create run script
echo @echo off > run-mcp.bat
echo. >> run-mcp.bat
echo echo 🚀 Starting Elavon Codegen MCP Server... >> run-mcp.bat
echo. >> run-mcp.bat
echo REM Default configuration >> run-mcp.bat
echo if not defined ELAVON_BASE_URL set ELAVON_BASE_URL=https://api.sandbox.elavon.com >> run-mcp.bat
echo if not defined SERVER_PORT set SERVER_PORT=8080 >> run-mcp.bat
echo. >> run-mcp.bat
echo REM Check for specs >> run-mcp.bat
echo if not exist "specs\elavon-openapi.json" ^( >> run-mcp.bat
echo     echo ⚠️  Warning: specs\elavon-openapi.json not found >> run-mcp.bat
echo     echo    The server will start but code generation may fail >> run-mcp.bat
echo ^) >> run-mcp.bat
echo. >> run-mcp.bat
echo REM Start the server >> run-mcp.bat
echo echo 🌐 Server will be available at: http://localhost:%%SERVER_PORT%% >> run-mcp.bat
echo echo 📊 Health check: http://localhost:%%SERVER_PORT%%/actuator/health >> run-mcp.bat
echo echo 🛠️  MCP Tool endpoint: http://localhost:%%SERVER_PORT%%/mcp/tools/elavonCodegen >> run-mcp.bat
echo echo. >> run-mcp.bat
echo. >> run-mcp.bat
echo java -jar modelcontextprotocol\target\modelcontextprotocol-1.0.0-SNAPSHOT.jar ^>> run-mcp.bat
echo     --server.port=%%SERVER_PORT%% ^>> run-mcp.bat
echo     --elavon.base-url=%%ELAVON_BASE_URL%% ^>> run-mcp.bat
echo     --logging.level.com.elavon.codegen=DEBUG >> run-mcp.bat
echo. >> run-mcp.bat
echo pause >> run-mcp.bat

echo.
echo 🎉 Setup complete!
echo.
echo 📋 Next steps:
echo 1. Place your OpenAPI specs in the specs\ directory:
echo    - specs\converge-openapi.json (for upgrade mode)
echo    - specs\elavon-openapi.json (required)
echo.
echo 2. Start the MCP server:
echo    run-mcp.bat
echo.
echo 3. Test the server:
echo    curl http://localhost:8080/actuator/health
echo.
echo 4. Use the MCP tool:
echo    curl -X POST http://localhost:8080/mcp/tools/elavonCodegen \
echo      -H "Content-Type: application/json" \
echo      -d "{\"mode\":\"create\",\"scope\":\"all\",\"dryRun\":true}"
echo.
echo 📖 For detailed deployment options, see DEPLOYMENT.md
echo.
pause
