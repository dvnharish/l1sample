#!/bin/bash

# Elavon Codegen MCP - Build and Run Script

set -e

echo "🚀 Building Elavon Codegen MCP Server..."

# Check prerequisites
if ! command -v java &> /dev/null; then
    echo "❌ Java 17+ is required but not found"
    exit 1
fi

if ! command -v mvn &> /dev/null; then
    echo "❌ Maven is required but not found"
    exit 1
fi

# Check Java version
JAVA_VERSION=$(java -version 2>&1 | head -n 1 | cut -d'"' -f2 | cut -d'.' -f1)
if [ "$JAVA_VERSION" -lt 17 ]; then
    echo "❌ Java 17+ is required, found Java $JAVA_VERSION"
    exit 1
fi

echo "✅ Prerequisites check passed"

# Clean and build
echo "🔨 Building project..."
mvn clean compile -DskipTests -q

if [ $? -eq 0 ]; then
    echo "✅ Build completed successfully!"
else
    echo "❌ Build failed. See errors above."
    echo "💡 Note: Some compilation errors are expected in the current version."
    echo "   The core MCP server and package scanner should work."
    echo "----------------------------------------"
    echo "Creating initial directory structure..."
    mkdir -p generated/src/main/java
    mkdir -p engine/src/main/resources/specs
    echo "📁 Created generated/src/main/java directory"
    echo "📁 Created engine/src/main/resources/specs directory"
    echo ""
    echo "Please add your OpenAPI specifications to engine/src/main/resources/specs/:"
    echo "   - converge-openapi.json"
    echo "   - elavon-openapi.json"
    echo ""
    exit 1
fi

# Check for specs
if [ ! -f "engine/src/main/resources/specs/elavon-openapi.json" ]; then
    echo "⚠️  Warning: engine/src/main/resources/specs/elavon-openapi.json not found"
    echo "   The server will start but code generation may fail without the spec."
fi

# Create executable JAR (simplified)
echo "📦 Creating executable JAR..."
cd modelcontextprotocol
mvn package -DskipTests -q
cd ..

if [ -f "modelcontextprotocol/target/modelcontextprotocol-1.0.0-SNAPSHOT.jar" ]; then
    echo "✅ JAR created successfully!"
else
    echo "❌ JAR creation failed"
    exit 1
fi

# Setup configuration
echo "⚙️  Setting up configuration..."

if [ ! -d "specs" ]; then
    mkdir -p specs
    echo "📁 Created specs/ directory"
    echo "   Please place your OpenAPI specs here:"
    echo "   - specs/converge-openapi.json"
    echo "   - specs/elavon-openapi.json"
fi

# Create run script
cat > run-mcp.sh << 'EOF'
#!/bin/bash

echo "🚀 Starting Elavon Codegen MCP Server..."

# Default configuration
export ELAVON_BASE_URL=${ELAVON_BASE_URL:-https://api.sandbox.elavon.com}
export SERVER_PORT=${SERVER_PORT:-8080}

# Check for specs
if [ ! -f "specs/elavon-openapi.json" ]; then
    echo "⚠️  Warning: specs/elavon-openapi.json not found"
    echo "   The server will start but code generation may fail"
fi

# Start the server
echo "🌐 Server will be available at: http://localhost:$SERVER_PORT"
echo "📊 Health check: http://localhost:$SERVER_PORT/actuator/health"
echo "🛠️  MCP Tool endpoint: http://localhost:$SERVER_PORT/mcp/tools/elavonCodegen"
echo ""

java -jar modelcontextprotocol/target/modelcontextprotocol-1.0.0-SNAPSHOT.jar \
    --server.port=$SERVER_PORT \
    --elavon.base-url=$ELAVON_BASE_URL \
    --logging.level.com.elavon.codegen=DEBUG
EOF

chmod +x run-mcp.sh

echo ""
echo "🎉 Setup complete!"
echo ""
echo "📋 Next steps:"
echo "1. Place your OpenAPI specs in the specs/ directory:"
echo "   - specs/converge-openapi.json (for upgrade mode)"
echo "   - specs/elavon-openapi.json (required)"
echo ""
echo "2. Start the MCP server:"
echo "   ./run-mcp.sh"
echo ""
echo "3. Test the server:"
echo "   curl http://localhost:8080/actuator/health"
echo ""
echo "4. Use the MCP tool:"
echo "   curl -X POST http://localhost:8080/mcp/tools/elavonCodegen \\"
echo "     -H 'Content-Type: application/json' \\"
echo "     -d '{\"mode\":\"create\",\"scope\":\"all\",\"dryRun\":true}'"
echo ""
echo "📖 For detailed deployment options, see DEPLOYMENT.md"
echo ""

echo ""
echo "----------------------------------------"
echo "✅ Build and Run Complete"
echo "----------------------------------------"
echo "Next Steps:"
echo "1. Place your OpenAPI specs in the engine/src/main/resources/specs/ directory:"
echo "   - converge-openapi.json (for upgrade mode)"
echo "   - elavon-openapi.json (required)"
echo "2. Configure your API keys and environment in:"
echo "   - mcp-config.json (for VS Code)"
echo "   - application.yml (for standalone)"
echo "----------------------------------------"
