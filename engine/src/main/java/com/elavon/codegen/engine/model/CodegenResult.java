package com.elavon.codegen.engine.model;

import com.elavon.codegen.engine.scanner.DetectedPackages;
import lombok.Builder;
import lombok.Data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Result of the code generation process.
 */
@Data
@Builder
public class CodegenResult {
    
    private boolean success;
    private boolean partialSuccess;
    private DetectedPackages detectedPackages;
    private List<OperationMapping> operationMappings;
    private FileChanges changes;
    private Map<String, TestResult> testResults;
    private List<String> todos;
    private String reportPath;
    private List<String> errors;
    
    /**
     * Create a builder with default collections.
     */
    public static CodegenResultBuilder withDefaults() {
        return builder()
            .operationMappings(new ArrayList<>())
            .changes(FileChanges.builder()
                .created(new ArrayList<>())
                .updated(new ArrayList<>())
                .deleted(new ArrayList<>())
                .build())
            .testResults(new HashMap<>())
            .todos(new ArrayList<>())
            .errors(new ArrayList<>());
    }
    
    /**
     * Add a created file to the changes.
     */
    public void addCreatedFile(String filePath) {
        if (changes == null) {
            changes = FileChanges.builder()
                .created(new ArrayList<>())
                .updated(new ArrayList<>())
                .deleted(new ArrayList<>())
                .build();
        }
        if (changes.getCreated() == null) {
            changes.setCreated(new ArrayList<>());
        }
        changes.getCreated().add(filePath);
    }
    
    /**
     * Add an updated file to the changes.
     */
    public void addUpdatedFile(String filePath) {
        if (changes == null) {
            changes = FileChanges.builder()
                .created(new ArrayList<>())
                .updated(new ArrayList<>())
                .deleted(new ArrayList<>())
                .build();
        }
        if (changes.getUpdated() == null) {
            changes.setUpdated(new ArrayList<>());
        }
        changes.getUpdated().add(filePath);
    }
    
    /**
     * Add a deleted file to the changes.
     */
    public void addDeletedFile(String filePath) {
        if (changes == null) {
            changes = FileChanges.builder()
                .created(new ArrayList<>())
                .updated(new ArrayList<>())
                .deleted(new ArrayList<>())
                .build();
        }
        if (changes.getDeleted() == null) {
            changes.setDeleted(new ArrayList<>());
        }
        changes.getDeleted().add(filePath);
    }
    
    /**
     * Add an operation mapping.
     */
    public void addOperationMapping(String legacy, String target, String tag) {
        if (operationMappings == null) {
            operationMappings = new ArrayList<>();
        }
        operationMappings.add(OperationMapping.builder()
            .legacy(legacy)
            .target(target)
            .tag(tag)
            .build());
    }
    
    /**
     * Add a test result.
     */
    public void addTestResult(String operation, boolean passed, String message, long duration) {
        if (testResults == null) {
            testResults = new HashMap<>();
        }
        testResults.put(operation, TestResult.builder()
            .passed(passed)
            .message(message)
            .duration(duration)
            .build());
    }
    
    /**
     * Add a TODO item.
     */
    public void addTodo(String todo) {
        if (todos == null) {
            todos = new ArrayList<>();
        }
        todos.add(todo);
    }
    
    /**
     * Add an error message.
     */
    public void addError(String error) {
        if (errors == null) {
            errors = new ArrayList<>();
        }
        errors.add(error);
    }
}
