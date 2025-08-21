package com.elavon.codegen.engine.report;

import com.elavon.codegen.engine.config.CodegenConfig;
import com.elavon.codegen.engine.model.CodegenResult;
import com.elavon.codegen.engine.model.FileChanges;
import com.elavon.codegen.engine.model.OperationMapping;
import com.elavon.codegen.engine.model.TestResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Generates comprehensive reports for code generation results.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReportGenerator {
    
    private final ObjectMapper objectMapper = new ObjectMapper()
        .enable(SerializationFeature.INDENT_OUTPUT);
    
    /**
     * Generate a comprehensive markdown report.
     * 
     * @return Path to the generated report
     */
    public String generateReport(CodegenConfig config, CodegenResult result) {
        String timestamp = LocalDateTime.now()
            .format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        
        String reportName = String.format("MCP_Generation_Report_%s_%s_%s.md",
            config.getMode().toString().toLowerCase(),
            config.getScope().toString().toLowerCase(),
            timestamp);
        
        Path reportPath = Paths.get(config.getProjectRoot(), reportName);
        
        try {
            String reportContent = buildReportContent(config, result);
            Files.writeString(reportPath, reportContent, StandardCharsets.UTF_8);
            
            log.info("Generated report: {}", reportPath);
            
            // Also generate JSON summary
            generateJsonSummary(config, result, timestamp);
            
            return reportPath.toString();
            
        } catch (IOException e) {
            log.error("Failed to generate report", e);
            return null;
        }
    }
    
    private String buildReportContent(CodegenConfig config, CodegenResult result) {
        StringBuilder report = new StringBuilder();
        
        // Header
        report.append("# Elavon Code Generation Report\n\n");
        report.append("Generated: ").append(LocalDateTime.now()
            .format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)).append("\n\n");
        
        // Configuration Summary
        report.append("## Configuration\n\n");
        report.append("| Parameter | Value |\n");
        report.append("|-----------|-------|\n");
        report.append("| Mode | ").append(config.getMode()).append(" |\n");
        report.append("| Scope | ").append(config.getScope()).append(" |\n");
        report.append("| Project Root | ").append(config.getProjectRoot()).append(" |\n");
        report.append("| Dry Run | ").append(config.isDryRun()).append(" |\n");
        
        if (config.isTagsScope()) {
            report.append("| Selected Tags | ").append(String.join(", ", config.getTags())).append(" |\n");
        }
        if (config.isOperationsScope()) {
            report.append("| Selected Operations | ").append(String.join(", ", config.getOperations())).append(" |\n");
        }
        report.append("\n");
        
        // Spec Information
        report.append("## OpenAPI Specifications\n\n");
        addSpecInfo(report, "Elavon Spec", config.getElavonSpecPath());
        if (config.isUpgradeMode()) {
            addSpecInfo(report, "Converge Spec", config.getConvergeSpecPath());
        }
        report.append("\n");
        
        // Detected Packages
        report.append("## Detected Package Structure\n\n");
        report.append("```json\n");
        try {
            report.append(objectMapper.writeValueAsString(result.getDetectedPackages()));
        } catch (Exception e) {
            report.append("Error serializing packages: ").append(e.getMessage());
        }
        report.append("\n```\n\n");
        
        // Operation Mappings
        if (result.getOperationMappings() != null && !result.getOperationMappings().isEmpty()) {
            report.append("## Operation Mappings\n\n");
            report.append("| Legacy Operation | Target Operation | Tag |\n");
            report.append("|------------------|------------------|-----|\n");
            
            for (OperationMapping mapping : result.getOperationMappings()) {
                report.append("| ")
                    .append(mapping.getLegacy() != null ? mapping.getLegacy() : "N/A")
                    .append(" | ")
                    .append(mapping.getTarget())
                    .append(" | ")
                    .append(mapping.getTag())
                    .append(" |\n");
            }
            report.append("\n");
        }
        
        // File Changes
        report.append("## File Changes\n\n");
        
        if (result.getChanges() != null) {
            FileChanges changes = result.getChanges();
            
            report.append("### Created Files (").append(changes.getCreated().size()).append(")\n\n");
            if (!changes.getCreated().isEmpty()) {
                changes.getCreated().forEach(file -> 
                    report.append("- `").append(file).append("`\n"));
            }
            report.append("\n");
            
            report.append("### Updated Files (").append(changes.getUpdated().size()).append(")\n\n");
            if (!changes.getUpdated().isEmpty()) {
                changes.getUpdated().forEach(file -> 
                    report.append("- `").append(file).append("`\n"));
            }
            report.append("\n");
            
            report.append("### Deleted Files (").append(changes.getDeleted().size()).append(")\n\n");
            if (!changes.getDeleted().isEmpty()) {
                changes.getDeleted().forEach(file -> 
                    report.append("- `").append(file).append("`\n"));
            }
            report.append("\n");
        }
        
        // Test Results
        if (result.getTestResults() != null && !result.getTestResults().isEmpty()) {
            report.append("## Test Results\n\n");
            report.append("| Operation | Status | Duration | Message |\n");
            report.append("|-----------|--------|----------|----------|\n");
            
            result.getTestResults().forEach((operation, testResult) -> {
                report.append("| ")
                    .append(operation)
                    .append(" | ")
                    .append(testResult.isPassed() ? "✅ PASSED" : "❌ FAILED")
                    .append(" | ")
                    .append(testResult.getDuration()).append("ms")
                    .append(" | ")
                    .append(testResult.getMessage() != null ? testResult.getMessage() : "")
                    .append(" |\n");
            });
            report.append("\n");
        }
        
        // Errors
        if (result.getErrors() != null && !result.getErrors().isEmpty()) {
            report.append("## Errors\n\n");
            result.getErrors().forEach(error -> 
                report.append("- ").append(error).append("\n"));
            report.append("\n");
        }
        
        // TODOs
        if (result.getTodos() != null && !result.getTodos().isEmpty()) {
            report.append("## Follow-up Actions Required\n\n");
            result.getTodos().forEach(todo -> 
                report.append("- [ ] ").append(todo).append("\n"));
            report.append("\n");
        } else {
            // Add default TODOs
            report.append("## Follow-up Actions Required\n\n");
            report.append("- [ ] Review generated code for correctness\n");
            report.append("- [ ] Update authentication configuration in application properties\n");
            report.append("- [ ] Configure Elavon API base URL and credentials\n");
            report.append("- [ ] Run integration tests against sandbox environment\n");
            report.append("- [ ] Review and update error handling as needed\n");
            report.append("- [ ] Verify PCI compliance for card data handling\n");
            if (config.isUpgradeMode()) {
                report.append("- [ ] Test backward compatibility with existing clients\n");
                report.append("- [ ] Plan migration strategy for production\n");
                report.append("- [ ] Update API documentation\n");
            }
            report.append("\n");
        }
        
        // Summary
        report.append("## Summary\n\n");
        report.append("Generation Status: **").append(getStatusText(result)).append("**\n\n");
        
        if (result.isSuccess()) {
            report.append("✅ Code generation completed successfully.\n");
        } else if (result.isPartialSuccess()) {
            report.append("⚠️ Code generation completed with some errors. Please review the errors section.\n");
        } else {
            report.append("❌ Code generation failed. Please check the errors and try again.\n");
        }
        
        return report.toString();
    }
    
    private void addSpecInfo(StringBuilder report, String specName, String specPath) {
        report.append("### ").append(specName).append("\n\n");
        report.append("- Path: `").append(specPath).append("`\n");
        
        try {
            File specFile = new File(specPath);
            if (specFile.exists()) {
                report.append("- Size: ").append(FileUtils.byteCountToDisplaySize(specFile.length())).append("\n");
                report.append("- Hash: ").append(calculateFileHash(specFile)).append("\n");
                
                // Try to read version from spec
                String content = FileUtils.readFileToString(specFile, StandardCharsets.UTF_8);
                if (content.contains("\"version\"")) {
                    // Simple extraction - could be improved
                    int versionIndex = content.indexOf("\"version\"");
                    int colonIndex = content.indexOf(":", versionIndex);
                    int quoteStart = content.indexOf("\"", colonIndex);
                    int quoteEnd = content.indexOf("\"", quoteStart + 1);
                    if (quoteEnd > quoteStart) {
                        String version = content.substring(quoteStart + 1, quoteEnd);
                        report.append("- Version: ").append(version).append("\n");
                    }
                }
            } else {
                report.append("- Status: File not found\n");
            }
        } catch (Exception e) {
            report.append("- Error reading spec: ").append(e.getMessage()).append("\n");
        }
    }
    
    private String calculateFileHash(File file) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] fileBytes = Files.readAllBytes(file.toPath());
            byte[] hashBytes = md.digest(fileBytes);
            
            return Base64.getEncoder().encodeToString(hashBytes).substring(0, 16) + "...";
        } catch (Exception e) {
            return "Unable to calculate hash";
        }
    }
    
    private String getStatusText(CodegenResult result) {
        if (result.isSuccess()) {
            return "SUCCESS";
        } else if (result.isPartialSuccess()) {
            return "PARTIAL SUCCESS";
        } else {
            return "FAILED";
        }
    }
    
    private void generateJsonSummary(CodegenConfig config, CodegenResult result, 
                                   String timestamp) throws IOException {
        Map<String, Object> summary = new LinkedHashMap<>();
        
        summary.put("timestamp", timestamp);
        summary.put("mode", config.getMode());
        summary.put("scope", config.getScope());
        summary.put("status", getStatusText(result));
        summary.put("detectedPackages", result.getDetectedPackages());
        
        if (result.getOperationMappings() != null) {
            summary.put("operationCount", result.getOperationMappings().size());
        }
        
        if (result.getChanges() != null) {
            Map<String, Integer> changeStats = new LinkedHashMap<>();
            changeStats.put("created", result.getChanges().getCreated().size());
            changeStats.put("updated", result.getChanges().getUpdated().size());
            changeStats.put("deleted", result.getChanges().getDeleted().size());
            summary.put("changeStats", changeStats);
        }
        
        if (result.getTestResults() != null) {
            long passedTests = result.getTestResults().values().stream()
                .filter(TestResult::isPassed)
                .count();
            Map<String, Object> testStats = new LinkedHashMap<>();
            testStats.put("total", result.getTestResults().size());
            testStats.put("passed", passedTests);
            testStats.put("failed", result.getTestResults().size() - passedTests);
            summary.put("testStats", testStats);
        }
        
        if (result.getErrors() != null) {
            summary.put("errorCount", result.getErrors().size());
        }
        
        String jsonPath = String.format("codegen-summary-%s.json", timestamp);
        Path outputPath = Paths.get(config.getProjectRoot(), jsonPath);
        
        objectMapper.writeValue(outputPath.toFile(), summary);
        log.info("Generated JSON summary: {}", outputPath);
    }
}
