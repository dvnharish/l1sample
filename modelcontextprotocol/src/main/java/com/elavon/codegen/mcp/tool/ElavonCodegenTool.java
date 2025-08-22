package com.elavon.codegen.mcp.tool;

import com.elavon.codegen.engine.CodegenEngine;
import com.elavon.codegen.engine.config.CodegenConfig;
import com.elavon.codegen.engine.model.CodegenResult;
import com.elavon.codegen.mcp.model.ElavonCodegenRequest;
import com.elavon.codegen.mcp.model.ElavonCodegenResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * MCP Tool implementation for Elavon code generation.
 * Handles both "upgrade" (XML to JSON migration) and "create" (JSON scaffolding) modes.
 */
@Slf4j
@Component("elavonCodegen")
@RequiredArgsConstructor
public class ElavonCodegenTool {
    
    private final CodegenEngine codegenEngine;
    private final ObjectMapper objectMapper;
    
    /**
     * Execute the Elavon code generation tool.
     * 
     * @param args Tool arguments as a map
     * @return Tool execution result
     */
    public ElavonCodegenResponse execute(Map<String, Object> args) {
        try {
            // Parse request
            ElavonCodegenRequest request = objectMapper.convertValue(args, ElavonCodegenRequest.class);
            request.validate();
            
            log.info("Executing Elavon codegen tool: mode={}, scope={}", 
                request.getMode(), request.getScope());
            
            // Convert to engine config
            CodegenConfig config = buildConfig(request);
            
            // Execute code generation
            CodegenResult result = codegenEngine.generate(config);
            
            // Build response
            return buildResponse(request, result);
            
        } catch (IllegalArgumentException e) {
            log.error("Invalid request parameters", e);
            return ElavonCodegenResponse.builder()
                .status("failed")
                .mode(null)
                .scope(null)
                .error("Invalid request: " + e.getMessage())
                .build();
        } catch (Exception e) {
            log.error("Code generation failed", e);
            return ElavonCodegenResponse.builder()
                .status("failed")
                .mode(args.get("mode") != null ? args.get("mode").toString() : null)
                .scope(args.get("scope") != null ? args.get("scope").toString() : null)
                .error("Generation failed: " + e.getMessage())
                .build();
        }
    }
    
    private CodegenConfig buildConfig(ElavonCodegenRequest request) {
        return CodegenConfig.builder()
            .mode(CodegenConfig.Mode.valueOf(request.getMode().toUpperCase()))
            .scope(CodegenConfig.Scope.valueOf(request.getScope().toUpperCase()))
            .tags(request.getTags())
            .operations(request.getOperations())
            .convergeSpecPath(request.getConvergeSpecPath())
            .elavonSpecPath(request.getElavonSpecPath())
            .projectRoot(request.getProjectRoot())
            .backupBranch(request.getBackupBranch())
            .dryRun(request.isDryRun())
            .build();
    }
    
    private ElavonCodegenResponse buildResponse(ElavonCodegenRequest request, 
                                               CodegenResult result) {
        return ElavonCodegenResponse.builder()
            .status(determineStatus(result))
            .mode(request.getMode())
            .scope(request.getScope())
            .basePackage(result.getDetectedPackages().getBasePackage())
            .controllerPackage(result.getDetectedPackages().getControllerPackage())
            .servicePackage(result.getDetectedPackages().getServicePackage())
            .dtoPackage(result.getDetectedPackages().getDtoPackage())
            .clientPackage(result.getDetectedPackages().getClientPackage())
            .mapperPackage(result.getDetectedPackages().getMapperPackage())
            .operationMappings(mapOperationMappings(result.getOperationMappings()))
            .changes(mapFileChanges(result.getChanges()))
            .testResults(mapTestResults(result.getTestResults()))
            .todos(result.getTodos())
            .reportPath(result.getReportPath())
            .build();
    }
    
    private String determineStatus(CodegenResult result) {
        if (result.isSuccess()) {
            return "success";
        } else if (result.isPartialSuccess()) {
            return "partial";
        } else {
            return "failed";
        }
    }
    
    private List<ElavonCodegenResponse.OperationMapping> mapOperationMappings(
            List<com.elavon.codegen.engine.model.OperationMapping> engineMappings) {
        if (engineMappings == null) return null;
        
        return engineMappings.stream()
            .map(engineMapping -> ElavonCodegenResponse.OperationMapping.builder()
                .legacy(engineMapping.getLegacy())
                .target(engineMapping.getTarget())
                .tag(engineMapping.getTag())
                .build())
            .toList();
    }
    
    private ElavonCodegenResponse.FileChanges mapFileChanges(
            com.elavon.codegen.engine.model.FileChanges engineChanges) {
        if (engineChanges == null) return null;
        
        return ElavonCodegenResponse.FileChanges.builder()
            .created(engineChanges.getCreated())
            .updated(engineChanges.getUpdated())
            .deleted(engineChanges.getDeleted())
            .build();
    }
    
    private Map<String, ElavonCodegenResponse.TestResult> mapTestResults(
            Map<String, com.elavon.codegen.engine.model.TestResult> engineResults) {
        if (engineResults == null) return null;
        
        return engineResults.entrySet().stream()
            .collect(java.util.stream.Collectors.toMap(
                Map.Entry::getKey,
                entry -> ElavonCodegenResponse.TestResult.builder()
                    .passed(entry.getValue().isPassed())
                    .message(entry.getValue().getMessage())
                    .duration(entry.getValue().getDuration())
                    .build()
            ));
    }
}
