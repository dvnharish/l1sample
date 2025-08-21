package com.elavon.codegen.engine;

import com.elavon.codegen.engine.config.CodegenConfig;
import com.elavon.codegen.engine.generator.*;
import com.elavon.codegen.engine.model.CodegenResult;
import com.elavon.codegen.engine.openapi.OpenApiLoader;
import com.elavon.codegen.engine.openapi.OperationManifest;
import com.elavon.codegen.engine.refactor.RefactorEngine;
import com.elavon.codegen.engine.report.ReportGenerator;
import com.elavon.codegen.engine.scanner.DetectedPackages;
import com.elavon.codegen.engine.scanner.PackageScanner;
import io.swagger.v3.oas.models.OpenAPI;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * Main code generation engine that orchestrates the entire process.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CodegenEngine {
    
    private final PackageScanner packageScanner;
    private final OpenApiLoader openApiLoader;
    private final DtoGenerator dtoGenerator;
    private final ClientGenerator clientGenerator;
    private final ServiceGenerator serviceGenerator;
    private final ControllerGenerator controllerGenerator;
    private final MapperGenerator mapperGenerator;
    private final RefactorEngine refactorEngine;
    private final ReportGenerator reportGenerator;
    
    /**
     * Generate code based on the provided configuration.
     */
    public CodegenResult generate(CodegenConfig config) {
        log.info("Starting code generation: mode={}, scope={}", config.getMode(), config.getScope());
        
        CodegenResult result = CodegenResult.withDefaults().build();
        
        try {
            // Step 1: Scan packages
            DetectedPackages packages = scanPackages(config, result);
            if (packages == null) {
                return result;
            }
            result.setDetectedPackages(packages);
            
            // Step 2: Save detected packages
            saveDetectedPackages(config, packages);
            
            // Step 3: Load OpenAPI specs
            OpenAPI elavonSpec = openApiLoader.loadSpec(config.getElavonSpecPath());
            OperationManifest elavonManifest = openApiLoader.buildManifest(elavonSpec, "elavon");
            
            OpenAPI convergeSpec = null;
            OperationManifest convergeManifest = null;
            if (config.isUpgradeMode()) {
                convergeSpec = openApiLoader.loadSpec(config.getConvergeSpecPath());
                convergeManifest = openApiLoader.buildManifest(convergeSpec, "converge");
            }
            
            // Step 4: Filter operations based on scope
            List<OperationManifest.OperationInfo> targetOperations = 
                filterOperations(elavonManifest, config);
            
            if (targetOperations.isEmpty()) {
                result.addError("No operations found matching the specified scope");
                return result;
            }
            
            // Step 5: Create backup if not dry run and upgrade mode
            if (!config.isDryRun() && config.isUpgradeMode()) {
                createBackup(config);
            }
            
            // Step 6: Generate code
            generateCode(config, packages, elavonSpec, targetOperations, result);
            
            // Step 7: Handle upgrade mode refactoring
            if (config.isUpgradeMode() && !config.isDryRun()) {
                refactorLegacyCode(config, packages, convergeManifest, elavonManifest, 
                    targetOperations, result);
            }
            
            // Step 8: Generate report
            String reportPath = reportGenerator.generateReport(config, result);
            result.setReportPath(reportPath);
            
            // Determine final status
            determineStatus(result);
            
        } catch (Exception e) {
            log.error("Code generation failed", e);
            result.setSuccess(false);
            result.addError("Fatal error: " + e.getMessage());
        }
        
        return result;
    }
    
    private DetectedPackages scanPackages(CodegenConfig config, CodegenResult result) {
        try {
            log.info("Scanning project for packages at: {}", config.getProjectRoot());
            DetectedPackages packages = packageScanner.scanProject(config.getProjectRoot());
            packages.validate();
            return packages;
        } catch (Exception e) {
            log.error("Package scanning failed", e);
            result.setSuccess(false);
            result.addError("Package scanning failed: " + e.getMessage());
            return null;
        }
    }
    
    private void saveDetectedPackages(CodegenConfig config, DetectedPackages packages) {
        try {
            Path outputPath = Paths.get(config.getProjectRoot(), "detected-packages.json");
            com.fasterxml.jackson.databind.ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();
            String json = objectMapper.writeValueAsString(packages);
            Files.writeString(outputPath, json);
            log.info("Saved detected packages to: {}", outputPath);
        } catch (IOException e) {
            log.warn("Failed to save detected packages", e);
        }
    }
    
    private List<OperationManifest.OperationInfo> filterOperations(
            OperationManifest manifest, CodegenConfig config) {
        
        if (config.isAllScope()) {
            return manifest.getAllOperations();
        } else if (config.isTagsScope()) {
            return manifest.getOperationsByTags(config.getTags());
        } else if (config.isOperationsScope()) {
            return manifest.getOperationsByIds(config.getOperations());
        }
        
        return List.of();
    }
    
    private void createBackup(CodegenConfig config) {
        try {
            log.info("Creating backup branch: {}", config.getBackupBranch());
            ProcessBuilder pb = new ProcessBuilder(
                "git", "checkout", "-b", config.getBackupBranch());
            pb.directory(new java.io.File(config.getProjectRoot()));
            Process process = pb.start();
            process.waitFor();
            
            if (process.exitValue() != 0) {
                log.warn("Failed to create backup branch, continuing anyway");
            }
        } catch (Exception e) {
            log.warn("Failed to create backup branch", e);
        }
    }
    
    private void generateCode(CodegenConfig config, DetectedPackages packages,
                             OpenAPI spec, List<OperationManifest.OperationInfo> operations,
                             CodegenResult result) {
        
        log.info("Generating code for {} operations", operations.size());
        
        for (OperationManifest.OperationInfo operation : operations) {
            try {
                // Generate DTOs
                List<String> dtoFiles = dtoGenerator.generate(packages, spec, operation);
                dtoFiles.forEach(result::addCreatedFile);
                
                // Generate Client
                String clientFile = clientGenerator.generate(packages, spec, operation);
                result.addCreatedFile(clientFile);
                
                // Generate Service
                String serviceFile = serviceGenerator.generate(packages, spec, operation);
                result.addCreatedFile(serviceFile);
                
                // Generate Controller
                String controllerFile = controllerGenerator.generate(packages, spec, operation);
                result.addCreatedFile(controllerFile);
                
                // Generate Mapper (for upgrade mode)
                if (config.isUpgradeMode()) {
                    String mapperFile = mapperGenerator.generate(packages, spec, operation);
                    result.addCreatedFile(mapperFile);
                }
                
                result.addOperationMapping(null, operation.getOperationId(), operation.getTag());
                
            } catch (Exception e) {
                log.error("Failed to generate code for operation: {}", 
                    operation.getOperationId(), e);
                result.addError("Failed to generate " + operation.getOperationId() + 
                    ": " + e.getMessage());
                result.setPartialSuccess(true);
            }
        }
    }
    
    private void refactorLegacyCode(CodegenConfig config, DetectedPackages packages,
                                   OperationManifest convergeManifest,
                                   OperationManifest elavonManifest,
                                   List<OperationManifest.OperationInfo> targetOperations,
                                   CodegenResult result) {
        
        log.info("Refactoring legacy code for upgrade mode");
        
        try {
            List<String> refactoredFiles = refactorEngine.refactor(
                config, packages, convergeManifest, elavonManifest, targetOperations);
            
            refactoredFiles.forEach(result::addUpdatedFile);
            
            // Add operation mappings
            for (OperationManifest.OperationInfo target : targetOperations) {
                String legacy = refactorEngine.findLegacyOperation(convergeManifest, target);
                if (legacy != null) {
                    result.addOperationMapping(legacy, target.getOperationId(), target.getTag());
                }
            }
            
        } catch (Exception e) {
            log.error("Refactoring failed", e);
            result.addError("Refactoring failed: " + e.getMessage());
            result.setPartialSuccess(true);
        }
    }
    
    private void determineStatus(CodegenResult result) {
        if (result.getErrors() == null || result.getErrors().isEmpty()) {
            result.setSuccess(true);
            result.setPartialSuccess(false);
        } else if (result.isPartialSuccess()) {
            result.setSuccess(false);
        } else {
            result.setSuccess(false);
            result.setPartialSuccess(false);
        }
    }
}
