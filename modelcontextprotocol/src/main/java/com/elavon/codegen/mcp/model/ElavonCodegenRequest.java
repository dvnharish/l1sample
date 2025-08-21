package com.elavon.codegen.mcp.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Request model for the Elavon codegen MCP tool.
 */
@Data
@NoArgsConstructor
public class ElavonCodegenRequest {
    
    @NotNull
    @Pattern(regexp = "upgrade|create")
    @JsonProperty("mode")
    private String mode;
    
    @Pattern(regexp = "all|tags|operations")
    @JsonProperty("scope")
    private String scope = "all";
    
    @JsonProperty("tags")
    private List<String> tags;
    
    @JsonProperty("operations")
    private List<String> operations;
    
    @Builder.Default
    private String convergeSpecPath = "classpath:specs/converge-openapi.json";
    
    @Builder.Default
    private String elavonSpecPath = "classpath:specs/elavon-openapi.json";
    
    @Builder.Default
    private String projectRoot = ".";
    
    @JsonProperty("backupBranch")
    private String backupBranch = "backup/converge-to-elavon";
    
    @JsonProperty("dryRun")
    private boolean dryRun = false;
    
    /**
     * Validate the request parameters.
     */
    public void validate() {
        if (!"upgrade".equals(mode) && !"create".equals(mode)) {
            throw new IllegalArgumentException("Mode must be 'upgrade' or 'create'");
        }
        
        if (scope == null) {
            scope = "all";
        }
        
        if (!"all".equals(scope) && !"tags".equals(scope) && !"operations".equals(scope)) {
            throw new IllegalArgumentException("Scope must be 'all', 'tags', or 'operations'");
        }
        
        if ("tags".equals(scope) && (tags == null || tags.isEmpty())) {
            throw new IllegalArgumentException("Tags must be provided when scope is 'tags'");
        }
        
        if ("operations".equals(scope) && (operations == null || operations.isEmpty())) {
            throw new IllegalArgumentException("Operations must be provided when scope is 'operations'");
        }
        
        if ("upgrade".equals(mode) && convergeSpecPath == null) {
            throw new IllegalArgumentException("convergeSpecPath is required for upgrade mode");
        }
        
        if (elavonSpecPath == null) {
            throw new IllegalArgumentException("elavonSpecPath is required");
        }
    }
}
