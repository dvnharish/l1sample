package com.elavon.codegen.mcp.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Response model for the Elavon codegen MCP tool.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ElavonCodegenResponse {
    
    @JsonProperty("status")
    private String status; // "success" | "partial" | "failed"
    
    @JsonProperty("mode")
    private String mode;
    
    @JsonProperty("scope")
    private String scope;
    
    @JsonProperty("basePackage")
    private String basePackage;
    
    @JsonProperty("controllerPackage")
    private String controllerPackage;
    
    @JsonProperty("servicePackage")
    private String servicePackage;
    
    @JsonProperty("dtoPackage")
    private String dtoPackage;
    
    @JsonProperty("clientPackage")
    private String clientPackage;
    
    @JsonProperty("mapperPackage")
    private String mapperPackage;
    
    @JsonProperty("operationMappings")
    private List<OperationMapping> operationMappings;
    
    @JsonProperty("changes")
    private FileChanges changes;
    
    @JsonProperty("testResults")
    private Map<String, TestResult> testResults;
    
    @JsonProperty("todos")
    private List<String> todos;
    
    @JsonProperty("reportPath")
    private String reportPath;
    
    @JsonProperty("error")
    private String error;
    
    /**
     * Represents a mapping between legacy and target operations.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OperationMapping {
        @JsonProperty("legacy")
        private String legacy;
        
        @JsonProperty("target")
        private String target;
        
        @JsonProperty("tag")
        private String tag;
    }
    
    /**
     * Represents file changes made during code generation.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FileChanges {
        @JsonProperty("created")
        private List<String> created;
        
        @JsonProperty("updated")
        private List<String> updated;
        
        @JsonProperty("deleted")
        private List<String> deleted;
    }
    
    /**
     * Represents test results for an operation.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TestResult {
        @JsonProperty("passed")
        private boolean passed;
        
        @JsonProperty("message")
        private String message;
        
        @JsonProperty("duration")
        private long duration;
    }
}
