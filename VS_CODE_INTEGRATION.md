# VS Code Integration Guide - Elavon Codegen MCP

## 🎯 Overview

This guide shows you how to integrate the Elavon Codegen MCP server with VS Code using stdio communication, which is the preferred method for editor integration.

## 🚀 Quick Setup

### Step 1: Build the MCP Server
```cmd
# Build the project
build-and-run.bat

# Verify the JAR was created
dir modelcontextprotocol\target\modelcontextprotocol-1.0.0-SNAPSHOT.jar
```

### Step 2: Configure Environment Variables
```cmd
# Set your Elavon API credentials
set ELAVON_API_KEY=your-actual-api-key
set ELAVON_BASE_URL=https://api.sandbox.elavon.com

# Or create a .env file in your project root
echo ELAVON_API_KEY=your-actual-api-key > .env
echo ELAVON_BASE_URL=https://api.sandbox.elavon.com >> .env
```

### Step 3: Test Stdio Mode
```cmd
# Test the MCP server in stdio mode
run-mcp-stdio.bat
```

### Step 4: Configure VS Code

The project includes a pre-configured `.vscode/settings.json` file:

```json
{
  "mcp.servers": {
    "elavon-codegen": {
      "command": "java",
      "args": [
        "-jar", 
        "./modelcontextprotocol/target/modelcontextprotocol-1.0.0-SNAPSHOT.jar",
        "--stdio"
      ],
      "cwd": "${workspaceFolder}",
      "env": {
        "ELAVON_BASE_URL": "https://api.sandbox.elavon.com",
        "ELAVON_API_KEY": "${env:ELAVON_API_KEY}",
        "LOGGING_LEVEL_COM_ELAVON_CODEGEN": "INFO"
      }
    }
  },
  "mcp.enabled": true
}
```

## 🔧 Manual Configuration

If you need to manually configure VS Code:

### Option 1: User Settings
1. Open VS Code settings (`Ctrl+,`)
2. Search for "mcp"
3. Add the configuration above to your user settings

### Option 2: Workspace Settings
1. Open your workspace in VS Code
2. Create/edit `.vscode/settings.json`
3. Add the MCP configuration

### Option 3: MCP Extension Settings
If you have an MCP extension installed:
1. Open Command Palette (`Ctrl+Shift+P`)
2. Type "MCP: Configure Servers"
3. Add the Elavon Codegen server configuration

## 🛠️ Using the MCP in VS Code

### Available Tool: `elavonCodegen`

Once configured, you can use the MCP tool in VS Code:

#### Command Palette Usage
1. Open Command Palette (`Ctrl+Shift+P`)
2. Type "MCP: Call Tool"
3. Select "elavonCodegen"
4. Provide the parameters

#### Chat Integration
If your VS Code has chat integration with MCP:

```
@elavonCodegen Generate Elavon APIs for my Spring Boot project

Mode: create
Scope: all
Project Root: ./my-payment-app
Dry Run: true
```

#### Parameters Reference

| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| `mode` | string | Yes | - | "upgrade" or "create" |
| `scope` | string | No | "all" | "all", "tags", or "operations" |
| `tags` | string[] | No | `["transactions"]` | Tags to include |
| `operations` | string[] | No | `["createTransaction"]` | Operation IDs to include |
| `projectRoot` | string | No | `"."` | Path to target project |
| `convergeSpecPath` | string | No | `"classpath:specs/converge-openapi.json"` | Legacy spec path |
| `elavonSpecPath` | string | No | `"classpath:specs/elavon-openapi.json"` | Modern spec path |
| `backupBranch` | string | No | `"mcp-backup"` | Git backup branch |
| `dryRun` | boolean | No | false | Preview without applying |

## 📋 Example Usage Scenarios

### Scenario 1: Create All Elavon APIs (Dry Run)
```json
{
  "mode": "create",
  "scope": "all",
  "projectRoot": "C:/my-projects/payment-service",
  "dryRun": true
}
```

### Scenario 2: Generate Specific Tags
```json
{
  "mode": "create",
  "scope": "tags",
  "tags": ["Transactions", "Tokens", "Merchants"],
  "projectRoot": "./my-spring-app",
  "dryRun": false
}
```

### Scenario 3: Upgrade from Converge to Elavon
```json
{
  "mode": "upgrade",
  "scope": "all",
  "projectRoot": "./legacy-payment-app",
  "backupBranch": "backup/elavon-migration",
  "dryRun": false
}
```

### Scenario 4: Specific Operations Only
```json
{
  "mode": "create",
  "scope": "operations", 
  "operations": ["processPayment", "voidTransaction", "getTransactionStatus"],
  "projectRoot": "./minimal-integration",
  "dryRun": true
}
```

## 📊 Understanding MCP Responses

The MCP server will return structured responses with:

### Success Response
```json
{
  "status": "success",
  "mode": "create",
  "scope": "all",
  "basePackage": "com.mycompany.payment",
  "changes": {
    "created": [
      "src/main/java/com/mycompany/payment/dto/elavon/transactions/PaymentRequest.java",
      "src/main/java/com/mycompany/payment/client/elavon/TransactionsClient.java"
    ]
  },
  "reportPath": "MCP_Generation_Report_create_all_20240101_120000.md"
}
```

### Error Response
```json
{
  "status": "failed",
  "error": "Package scanning failed: No @SpringBootApplication found"
}
```

## 🐛 Troubleshooting

### Common Issues

1. **MCP Server Not Starting**
   ```cmd
   # Check if JAR exists
   dir modelcontextprotocol\target\*.jar
   
   # Rebuild if necessary
   build-and-run.bat
   ```

2. **Environment Variables Not Set**
   ```cmd
   # Check environment
   echo %ELAVON_API_KEY%
   echo %ELAVON_BASE_URL%
   
   # Set if needed
   set ELAVON_API_KEY=your-key
   ```

3. **OpenAPI Specs Not Found**
   ```cmd
   # Check specs directory
   dir specs\*.json
   
   # Verify file contents
   type specs\elavon-openapi.json | findstr "openapi"
   ```

4. **VS Code Not Detecting MCP**
   - Restart VS Code after configuration changes
   - Check VS Code logs for MCP-related errors
   - Verify the MCP extension is installed and enabled

### Debug Mode

Enable debug logging by modifying the environment in `.vscode/settings.json`:

```json
{
  "env": {
    "LOGGING_LEVEL_COM_ELAVON_CODEGEN": "DEBUG",
    "LOGGING_LEVEL_ROOT": "INFO"
  }
}
```

### Manual Testing

Test the MCP server manually:

```cmd
# Start in stdio mode
run-mcp-stdio.bat

# In another terminal, send a test message
echo {"jsonrpc":"2.0","method":"initialize","id":1,"params":{"protocolVersion":"2024-11-05","capabilities":{}}} | java -jar modelcontextprotocol\target\modelcontextprotocol-1.0.0-SNAPSHOT.jar --stdio
```

## 🔄 Development Workflow

### Making Changes
1. Modify the MCP server code
2. Rebuild: `mvn clean package -DskipTests`
3. Restart VS Code or reload the MCP server
4. Test your changes

### Updating Configuration
1. Modify `.vscode/settings.json`
2. Reload VS Code window (`Ctrl+Shift+P` → "Developer: Reload Window")

## 📝 Logs and Debugging

### MCP Server Logs
- Logs are written to stderr (visible in VS Code output)
- Application logs: `logs/elavon-codegen-mcp.log`
- Generated reports: `MCP_Generation_Report_*.md`

### VS Code MCP Logs
- Open VS Code Output panel
- Select "MCP" from the dropdown
- View communication between VS Code and the MCP server

## 🎉 Success Indicators

You'll know everything is working when:

✅ VS Code detects the MCP server  
✅ The `elavonCodegen` tool appears in tool lists  
✅ Tool execution generates code successfully  
✅ Generated files appear in your project  
✅ Detailed reports are created  

## 📚 Additional Resources

- **OpenAPI Specs**: Place in `engine/src/main/resources/specs/` directory
- **Custom Templates**: Place in `./templates/` directory (if you add custom templates)
- **Run Scripts**: See `run-mcp-stdio.bat` or `build-and-run.sh`

Your Elavon Codegen MCP is now ready for seamless VS Code integration!

