package com.elavon.codegen.engine.config;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * Configuration for the code generation engine.
 */
@Data
@Builder
public class CodegenConfig {
    
    public enum Mode {
        UPGRADE,  // Migrate Converge XML to Elavon JSON
        CREATE    // Scaffold Elavon JSON only
    }
    
    public enum Scope {
        ALL,        // All operations
        TAGS,       // Selected tags
        OPERATIONS  // Selected operations
    }
    
    private Mode mode;
    private Scope scope;
    private List<String> tags;
    private List<String> operations;
    private String convergeSpecPath;
    private String elavonSpecPath;
    private String projectRoot;
    private String backupBranch;
    private boolean dryRun;
    
    /**
     * Check if this is an upgrade from Converge to Elavon.
     */
    public boolean isUpgradeMode() {
        return mode == Mode.UPGRADE;
    }
    
    /**
     * Check if this is a create mode for new Elavon APIs.
     */
    public boolean isCreateMode() {
        return mode == Mode.CREATE;
    }
    
    /**
     * Check if all operations should be processed.
     */
    public boolean isAllScope() {
        return scope == Scope.ALL;
    }
    
    /**
     * Check if only selected tags should be processed.
     */
    public boolean isTagsScope() {
        return scope == Scope.TAGS;
    }
    
    /**
     * Check if only selected operations should be processed.
     */
    public boolean isOperationsScope() {
        return scope == Scope.OPERATIONS;
    }
}
