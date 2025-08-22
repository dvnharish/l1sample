package com.elavon.codegen.mcp.server;

import com.elavon.codegen.mcp.model.ElavonCodegenRequest;
import com.elavon.codegen.mcp.model.ElavonCodegenResponse;
import com.elavon.codegen.mcp.tool.ElavonCodegenTool;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.io.*;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * MCP Server that communicates via stdio (stdin/stdout) for VS Code integration.
 * Implements the Model Context Protocol specification.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StdioMcpServer implements CommandLineRunner {
    
    private final ElavonCodegenTool elavonCodegenTool;
    private final ObjectMapper objectMapper;
    private final AtomicLong requestId = new AtomicLong(0);
    
    private BufferedReader stdin;
    private PrintWriter stdout;
    
    @Override
    public void run(String... args) throws Exception {
        // Check if we should run in stdio mode
        boolean stdioMode = args.length > 0 && "--stdio".equals(args[0]);
        
        if (!stdioMode) {
            log.info("MCP Server started. Use --stdio argument to run in stdio mode for VS Code.");
            return;
        }
        
        log.info("Starting MCP Server in stdio mode for VS Code integration...");
        
        try {
            initializeStreams();
            handleMcpCommunication();
        } catch (Exception e) {
            log.error("MCP Server error", e);
            System.exit(1);
        }
    }
    
    private void initializeStreams() {
        stdin = new BufferedReader(new InputStreamReader(System.in));
        stdout = new PrintWriter(System.out, true);
        
        // Redirect logging to stderr to avoid interfering with MCP communication
        System.setProperty("logging.pattern.console", "%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n");
    }
    
    private void handleMcpCommunication() throws IOException {
        String line;
        while ((line = stdin.readLine()) != null) {
            try {
                processMessage(line);
            } catch (Exception e) {
                log.error("Error processing MCP message: {}", line, e);
                sendErrorResponse("Internal server error: " + e.getMessage());
            }
        }
    }
    
    private void processMessage(String message) throws Exception {
        if (message.trim().isEmpty()) {
            return;
        }
        
        JsonNode request = objectMapper.readTree(message);
        String method = request.path("method").asText();
        JsonNode params = request.path("params");
        JsonNode id = request.path("id");
        
        log.debug("Received MCP request: method={}, id={}", method, id);
        
        switch (method) {
            case "initialize" -> handleInitialize(id, params);
            case "tools/list" -> handleToolsList(id);
            case "tools/call" -> handleToolsCall(id, params);
            case "notifications/initialized" -> handleInitialized();
            default -> sendErrorResponse(id, "Unknown method: " + method);
        }
    }
    
    private void handleInitialize(JsonNode id, JsonNode params) {
        ObjectNode response = objectMapper.createObjectNode();
        response.put("jsonrpc", "2.0");
        response.set("id", id);
        
        ObjectNode result = objectMapper.createObjectNode();
        result.put("protocolVersion", "2024-11-05");
        
        ObjectNode serverInfo = objectMapper.createObjectNode();
        serverInfo.put("name", "ElavonCodegenMCP");
        serverInfo.put("version", "1.0.0");
        result.set("serverInfo", serverInfo);
        
        ObjectNode capabilities = objectMapper.createObjectNode();
        ObjectNode tools = objectMapper.createObjectNode();
        tools.put("listChanged", false);
        capabilities.set("tools", tools);
        result.set("capabilities", capabilities);
        
        response.set("result", result);
        
        sendResponse(response);
        log.info("MCP Server initialized successfully");
    }
    
    private void handleToolsList(JsonNode id) {
        ObjectNode response = objectMapper.createObjectNode();
        response.put("jsonrpc", "2.0");
        response.set("id", id);
        
        ObjectNode result = objectMapper.createObjectNode();
        ArrayNode tools = objectMapper.createArrayNode();
        
        ObjectNode elavonTool = objectMapper.createObjectNode();
        elavonTool.put("name", "elavonCodegen");
        elavonTool.put("description", "Generate and migrate Elavon payment APIs from OpenAPI specifications");
        
        ObjectNode inputSchema = objectMapper.createObjectNode();
        inputSchema.put("type", "object");
        inputSchema.putArray("required").add("mode");
        
        ObjectNode properties = objectMapper.createObjectNode();
        
        ObjectNode modeProperty = objectMapper.createObjectNode();
        modeProperty.put("type", "string");
        modeProperty.putArray("enum").add("upgrade").add("create");
        modeProperty.put("description", "Generation mode: upgrade (XML to JSON migration) or create (new APIs only)");
        properties.set("mode", modeProperty);
        
        ObjectNode scopeProperty = objectMapper.createObjectNode();
        scopeProperty.put("type", "string");
        scopeProperty.putArray("enum").add("all").add("tags").add("operations");
        scopeProperty.put("default", "all");
        scopeProperty.put("description", "Scope of generation");
        properties.set("scope", scopeProperty);
        
        ObjectNode tagsProperty = objectMapper.createObjectNode();
        tagsProperty.put("type", "array");
        ObjectNode tagsItems = objectMapper.createObjectNode();
        tagsItems.put("type", "string");
        tagsProperty.set("items", tagsItems);
        tagsProperty.put("description", "Tags to include (when scope=tags)");
        properties.set("tags", tagsProperty);
        
        ObjectNode operationsProperty = objectMapper.createObjectNode();
        operationsProperty.put("type", "array");
        ObjectNode operationsItems = objectMapper.createObjectNode();
        operationsItems.put("type", "string");
        operationsProperty.set("items", operationsItems);
        operationsProperty.put("description", "Operation IDs to include (when scope=operations)");
        properties.set("operations", operationsProperty);
        
        ObjectNode projectRootProperty = objectMapper.createObjectNode();
        projectRootProperty.put("type", "string");
        projectRootProperty.put("default", ".");
        projectRootProperty.put("description", "Root directory of the target project");
        properties.set("projectRoot", projectRootProperty);
        
        ObjectNode dryRunProperty = objectMapper.createObjectNode();
        dryRunProperty.put("type", "boolean");
        dryRunProperty.put("default", false);
        dryRunProperty.put("description", "Preview changes without applying them");
        properties.set("dryRun", dryRunProperty);
        
        inputSchema.set("properties", properties);
        elavonTool.set("inputSchema", inputSchema);
        
        tools.add(elavonTool);
        result.set("tools", tools);
        response.set("result", result);
        
        sendResponse(response);
        log.debug("Sent tools list response");
    }
    
    private void handleToolsCall(JsonNode id, JsonNode params) {
        try {
            String toolName = params.path("name").asText();
            JsonNode arguments = params.path("arguments");
            
            if (!"elavonCodegen".equals(toolName)) {
                sendErrorResponse(id, "Unknown tool: " + toolName);
                return;
            }
            
            log.info("Executing elavonCodegen tool with arguments: {}", arguments);
            
            // Convert JsonNode to Map for the tool
            @SuppressWarnings("unchecked")
            Map<String, Object> args = objectMapper.convertValue(arguments, Map.class);
            
            // Execute the tool
            ElavonCodegenResponse toolResponse = elavonCodegenTool.execute(args);
            
            // Create MCP response
            ObjectNode response = objectMapper.createObjectNode();
            response.put("jsonrpc", "2.0");
            response.set("id", id);
            
            ObjectNode result = objectMapper.createObjectNode();
            ArrayNode content = objectMapper.createArrayNode();
            
            ObjectNode textContent = objectMapper.createObjectNode();
            textContent.put("type", "text");
            
            // Format the response as readable text
            String responseText = formatToolResponse(toolResponse);
            textContent.put("text", responseText);
            
            content.add(textContent);
            result.set("content", content);
            result.put("isError", "failed".equals(toolResponse.getStatus()));
            
            response.set("result", result);
            sendResponse(response);
            
            log.info("Tool execution completed with status: {}", toolResponse.getStatus());
            
        } catch (Exception e) {
            log.error("Error executing tool", e);
            sendErrorResponse(id, "Tool execution failed: " + e.getMessage());
        }
    }
    
    private String formatToolResponse(ElavonCodegenResponse response) {
        StringBuilder sb = new StringBuilder();
        
        sb.append("# Elavon Codegen Results\n\n");
        sb.append("**Status:** ").append(response.getStatus().toUpperCase()).append("\n");
        sb.append("**Mode:** ").append(response.getMode()).append("\n");
        sb.append("**Scope:** ").append(response.getScope()).append("\n\n");
        
        if (response.getError() != null) {
            sb.append("## ❌ Error\n");
            sb.append(response.getError()).append("\n\n");
            return sb.toString();
        }
        
        if (response.getBasePackage() != null) {
            sb.append("## 📦 Detected Packages\n");
            sb.append("- **Base Package:** ").append(response.getBasePackage()).append("\n");
            sb.append("- **Controller Package:** ").append(response.getControllerPackage()).append("\n");
            sb.append("- **Service Package:** ").append(response.getServicePackage()).append("\n");
            sb.append("- **DTO Package:** ").append(response.getDtoPackage()).append("\n");
            sb.append("- **Client Package:** ").append(response.getClientPackage()).append("\n");
            sb.append("- **Mapper Package:** ").append(response.getMapperPackage()).append("\n\n");
        }
        
        if (response.getChanges() != null) {
            var changes = response.getChanges();
            sb.append("## 📋 File Changes\n");
            
            if (changes.getCreated() != null && !changes.getCreated().isEmpty()) {
                sb.append("### ✅ Created Files (").append(changes.getCreated().size()).append(")\n");
                changes.getCreated().forEach(file -> 
                    sb.append("- `").append(file).append("`\n"));
                sb.append("\n");
            }
            
            if (changes.getUpdated() != null && !changes.getUpdated().isEmpty()) {
                sb.append("### 🔄 Updated Files (").append(changes.getUpdated().size()).append(")\n");
                changes.getUpdated().forEach(file -> 
                    sb.append("- `").append(file).append("`\n"));
                sb.append("\n");
            }
            
            if (changes.getDeleted() != null && !changes.getDeleted().isEmpty()) {
                sb.append("### 🗑️ Deleted Files (").append(changes.getDeleted().size()).append(")\n");
                changes.getDeleted().forEach(file -> 
                    sb.append("- `").append(file).append("`\n"));
                sb.append("\n");
            }
        }
        
        if (response.getOperationMappings() != null && !response.getOperationMappings().isEmpty()) {
            sb.append("## 🔗 Operation Mappings\n");
            response.getOperationMappings().forEach(mapping -> {
                sb.append("- **").append(mapping.getTarget()).append("** (").append(mapping.getTag()).append(")");
                if (mapping.getLegacy() != null) {
                    sb.append(" ← ").append(mapping.getLegacy());
                }
                sb.append("\n");
            });
            sb.append("\n");
        }
        
        if (response.getTestResults() != null && !response.getTestResults().isEmpty()) {
            sb.append("## 🧪 Test Results\n");
            response.getTestResults().forEach((operation, result) -> {
                sb.append("- **").append(operation).append(":** ");
                sb.append(result.isPassed() ? "✅ PASSED" : "❌ FAILED");
                sb.append(" (").append(result.getDuration()).append("ms)");
                if (result.getMessage() != null) {
                    sb.append(" - ").append(result.getMessage());
                }
                sb.append("\n");
            });
            sb.append("\n");
        }
        
        if (response.getTodos() != null && !response.getTodos().isEmpty()) {
            sb.append("## 📝 TODOs\n");
            response.getTodos().forEach(todo -> 
                sb.append("- [ ] ").append(todo).append("\n"));
            sb.append("\n");
        }
        
        if (response.getReportPath() != null) {
            sb.append("## 📊 Detailed Report\n");
            sb.append("See detailed report at: `").append(response.getReportPath()).append("`\n\n");
        }
        
        return sb.toString();
    }
    
    private void handleInitialized() {
        // No response needed for notification
        log.debug("Received initialized notification");
    }
    
    private void sendResponse(ObjectNode response) {
        try {
            stdout.println(objectMapper.writeValueAsString(response));
            stdout.flush();
        } catch (Exception e) {
            log.error("Failed to serialize MCP response", e);
        }
    }
    
    private void sendErrorResponse(String message) {
        sendErrorResponse(null, message);
    }
    
    private void sendErrorResponse(JsonNode id, String message) {
        ObjectNode response = objectMapper.createObjectNode();
        response.put("jsonrpc", "2.0");
        if (id != null) {
            response.set("id", id);
        }
        
        ObjectNode error = objectMapper.createObjectNode();
        error.put("code", -32603);
        error.put("message", message);
        response.set("error", error);
        
        sendResponse(response);
    }
}

