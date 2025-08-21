package com.elavon.codegen.engine.openapi;

import io.swagger.v3.oas.models.Operation;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Manifest of all operations in an OpenAPI specification.
 * Provides efficient lookup by various criteria.
 */
@Getter
public class OperationManifest {
    
    private final String specName;
    private final Map<String, OperationInfo> operationById = new ConcurrentHashMap<>();
    private final Map<String, List<OperationInfo>> operationsByTag = new ConcurrentHashMap<>();
    private final List<OperationInfo> allOperations = new ArrayList<>();
    
    public OperationManifest(String specName) {
        this.specName = specName;
    }
    
    /**
     * Add an operation to the manifest.
     */
    public void addOperation(OperationInfo info) {
        // Add to all operations
        allOperations.add(info);
        
        // Index by operation ID
        operationById.put(info.getOperationId(), info);
        
        // Index by tags
        for (String tag : info.getTags()) {
            operationsByTag.computeIfAbsent(tag, k -> new ArrayList<>()).add(info);
        }
    }
    
    /**
     * Get all unique tags.
     */
    public Set<String> getAllTags() {
        return new HashSet<>(operationsByTag.keySet());
    }
    
    /**
     * Get operations by tag names.
     */
    public List<OperationInfo> getOperationsByTags(List<String> tags) {
        if (tags == null || tags.isEmpty()) {
            return List.of();
        }
        
        return tags.stream()
            .map(tag -> operationsByTag.getOrDefault(tag, List.of()))
            .flatMap(List::stream)
            .distinct()
            .collect(Collectors.toList());
    }
    
    /**
     * Get operations by operation IDs.
     */
    public List<OperationInfo> getOperationsByIds(List<String> operationIds) {
        if (operationIds == null || operationIds.isEmpty()) {
            return List.of();
        }
        
        return operationIds.stream()
            .map(operationById::get)
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    }
    
    /**
     * Find an operation by a fuzzy match (operationId, summary, or path).
     */
    public OperationInfo findOperation(String query) {
        if (query == null) return null;
        
        String lowerQuery = query.toLowerCase();
        
        // First try exact operation ID match
        OperationInfo exact = operationById.get(query);
        if (exact != null) return exact;
        
        // Then try case-insensitive operation ID match
        for (OperationInfo info : allOperations) {
            if (info.getOperationId().equalsIgnoreCase(query)) {
                return info;
            }
        }
        
        // Try summary match
        for (OperationInfo info : allOperations) {
            if (info.getSummary() != null && 
                info.getSummary().toLowerCase().contains(lowerQuery)) {
                return info;
            }
        }
        
        // Try path match
        for (OperationInfo info : allOperations) {
            if (info.getPath().toLowerCase().contains(lowerQuery)) {
                return info;
            }
        }
        
        return null;
    }
    
    /**
     * Information about a single operation.
     */
    @Data
    @Builder
    public static class OperationInfo {
        private String operationId;
        private String path;
        private String method;
        private String summary;
        private String description;
        private String tag; // Primary tag
        private List<String> tags; // All tags
        private Operation operation; // The full Swagger operation object
        
        /**
         * Get a friendly display name for the operation.
         */
        public String getDisplayName() {
            if (summary != null && !summary.isEmpty()) {
                return summary;
            }
            return operationId;
        }
        
        /**
         * Get the HTTP method as an enum.
         */
        public io.swagger.v3.oas.models.PathItem.HttpMethod getHttpMethod() {
            return io.swagger.v3.oas.models.PathItem.HttpMethod.valueOf(method.toUpperCase());
        }
    }
}
