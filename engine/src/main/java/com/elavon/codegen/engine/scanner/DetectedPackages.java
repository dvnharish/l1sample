package com.elavon.codegen.engine.scanner;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents the detected package structure of a Spring Boot project.
 */
@Data
@NoArgsConstructor
public class DetectedPackages {
    
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
    
    /**
     * Get the error package path based on base package.
     */
    public String getErrorPackage() {
        return basePackage + ".error";
    }
    
    /**
     * Get sub-package for a specific tag under a category.
     * Example: com.acme.merchant.dto.elavon.transactions
     */
    public String getTagPackage(String category, String tag) {
        String baseCategory = switch (category) {
            case "dto" -> dtoPackage;
            case "client" -> clientPackage;
            case "service" -> servicePackage;
            case "controller" -> controllerPackage;
            case "mapper" -> mapperPackage;
            case "error" -> getErrorPackage();
            default -> throw new IllegalArgumentException("Unknown category: " + category);
        };
        
        return baseCategory + ".elavon." + tag.toLowerCase().replaceAll("[^a-z0-9]", "");
    }
    
    /**
     * Validate that all required packages are set.
     */
    public void validate() {
        if (basePackage == null || basePackage.trim().isEmpty()) {
            throw new PackageScannerException("Base package is required but not detected");
        }
        
        if (controllerPackage == null || servicePackage == null || 
            dtoPackage == null || clientPackage == null || mapperPackage == null) {
            throw new PackageScannerException("One or more required packages not detected");
        }
    }
}
