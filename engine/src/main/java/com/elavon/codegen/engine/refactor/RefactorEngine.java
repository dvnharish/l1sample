package com.elavon.codegen.engine.refactor;

import com.elavon.codegen.engine.config.CodegenConfig;
import com.elavon.codegen.engine.openapi.OperationManifest;
import com.elavon.codegen.engine.scanner.DetectedPackages;
import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.stmt.BlockStmt;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.DirectoryFileFilter;
import org.apache.commons.io.filefilter.SuffixFileFilter;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Refactors existing code from Converge XML to Elavon JSON.
 * Identifies and updates controllers, services, and XML handling code.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RefactorEngine {
    
    private final JavaParser javaParser = new JavaParser();
    
    /**
     * Refactor legacy code to use new Elavon APIs.
     * 
     * @return List of refactored file paths
     */
    public List<String> refactor(CodegenConfig config, 
                                DetectedPackages packages,
                                OperationManifest convergeManifest,
                                OperationManifest elavonManifest,
                                List<OperationManifest.OperationInfo> targetOperations) {
        
        log.info("Starting refactoring for {} operations", targetOperations.size());
        
        List<String> refactoredFiles = new ArrayList<>();
        
        // Find all Java files in the project
        Path srcPath = Paths.get(config.getProjectRoot(), "src/main/java");
        Collection<File> javaFiles = FileUtils.listFiles(
            srcPath.toFile(),
            new SuffixFileFilter(".java"),
            DirectoryFileFilter.DIRECTORY
        );
        
        // Identify files with XML processing
        List<LegacyCodeLocation> xmlLocations = findXmlProcessingCode(javaFiles);
        log.info("Found {} files with XML processing", xmlLocations.size());
        
        // Refactor each location
        for (LegacyCodeLocation location : xmlLocations) {
            try {
                boolean refactored = refactorFile(location, packages, 
                    convergeManifest, elavonManifest, targetOperations);
                
                if (refactored) {
                    refactoredFiles.add(location.getFilePath());
                }
            } catch (Exception e) {
                log.error("Failed to refactor file: {}", location.getFilePath(), e);
            }
        }
        
        log.info("Refactored {} files", refactoredFiles.size());
        return refactoredFiles;
    }
    
    /**
     * Find the legacy operation that maps to a target Elavon operation.
     */
    public String findLegacyOperation(OperationManifest convergeManifest,
                                     OperationManifest.OperationInfo targetOperation) {
        if (convergeManifest == null) return null;
        
        // Try to match by operation ID similarity
        String targetId = targetOperation.getOperationId();
        
        // Direct match
        OperationManifest.OperationInfo direct = convergeManifest.getOperationById()
            .get(targetId);
        if (direct != null) return direct.getOperationId();
        
        // Try fuzzy match
        for (OperationManifest.OperationInfo convergeOp : convergeManifest.getAllOperations()) {
            if (isSimilarOperation(convergeOp, targetOperation)) {
                return convergeOp.getOperationId();
            }
        }
        
        return null;
    }
    
    private List<LegacyCodeLocation> findXmlProcessingCode(Collection<File> javaFiles) {
        List<LegacyCodeLocation> locations = new ArrayList<>();
        
        for (File file : javaFiles) {
            try {
                String content = FileUtils.readFileToString(file, StandardCharsets.UTF_8);
                
                // Quick checks for XML indicators
                if (containsXmlIndicators(content)) {
                    ParseResult<CompilationUnit> parseResult = javaParser.parse(file);
                    if (parseResult.isSuccessful() && parseResult.getResult().isPresent()) {
                        CompilationUnit cu = parseResult.getResult().get();
                        
                        // Find specific XML usage
                        cu.findAll(MethodDeclaration.class).forEach(method -> {
                            if (isXmlProcessingMethod(method)) {
                                locations.add(new LegacyCodeLocation(
                                    file.getAbsolutePath(),
                                    cu,
                                    method
                                ));
                            }
                        });
                    }
                }
            } catch (IOException e) {
                log.warn("Failed to read file: {}", file.getPath(), e);
            }
        }
        
        return locations;
    }
    
    private boolean containsXmlIndicators(String content) {
        return content.contains("processxml.do") ||
               content.contains("xmldata") ||
               content.contains("application/x-www-form-urlencoded") ||
               content.contains("JAXB") ||
               content.contains("XPath") ||
               content.contains("dom4j") ||
               content.contains("XMLStreamReader") ||
               content.contains("DocumentBuilder");
    }
    
    private boolean isXmlProcessingMethod(MethodDeclaration method) {
        String methodBody = method.toString();
        
        // Check for XML-related annotations
        if (method.getAnnotations().stream().anyMatch(a -> 
            a.toString().contains("application/x-www-form-urlencoded") ||
            a.toString().contains("text/xml"))) {
            return true;
        }
        
        // Check method body for XML processing
        return methodBody.contains("xmldata") ||
               methodBody.contains("processxml") ||
               methodBody.contains("marshal") ||
               methodBody.contains("unmarshal") ||
               methodBody.contains("XPath") ||
               methodBody.contains("Document") ||
               methodBody.contains("Element");
    }
    
    private boolean refactorFile(LegacyCodeLocation location,
                               DetectedPackages packages,
                               OperationManifest convergeManifest,
                               OperationManifest elavonManifest,
                               List<OperationManifest.OperationInfo> targetOperations) 
                               throws IOException {
        
        log.info("Refactoring file: {}", location.getFilePath());
        
        CompilationUnit cu = location.getCompilationUnit();
        boolean modified = false;
        
        // Update imports
        if (updateImports(cu, packages)) {
            modified = true;
        }
        
        // Update class annotations
        cu.findAll(ClassOrInterfaceDeclaration.class).forEach(classDecl -> {
            if (updateClassAnnotations(classDecl)) {
                modified = true;
            }
        });
        
        // Update methods
        cu.findAll(MethodDeclaration.class).forEach(method -> {
            if (refactorMethod(method, packages, targetOperations)) {
                modified = true;
            }
        });
        
        // Save the modified file
        if (modified) {
            String modifiedContent = cu.toString();
            FileUtils.writeStringToFile(
                new File(location.getFilePath()), 
                modifiedContent, 
                StandardCharsets.UTF_8
            );
            log.info("Saved refactored file: {}", location.getFilePath());
        }
        
        return modified;
    }
    
    private boolean updateImports(CompilationUnit cu, DetectedPackages packages) {
        boolean modified = false;
        
        // Remove XML-related imports
        cu.getImports().removeIf(importDecl -> {
            String importName = importDecl.getNameAsString();
            boolean shouldRemove = importName.contains("jaxb") ||
                                 importName.contains("dom4j") ||
                                 importName.contains("javax.xml") ||
                                 importName.contains("org.w3c.dom");
            if (shouldRemove) {
                log.debug("Removing import: {}", importName);
            }
            return shouldRemove;
        });
        
        // Add new imports for JSON processing
        cu.addImport("com.fasterxml.jackson.databind.ObjectMapper");
        cu.addImport("org.springframework.http.MediaType");
        cu.addImport("reactor.core.publisher.Mono");
        
        return true;
    }
    
    private boolean updateClassAnnotations(ClassOrInterfaceDeclaration classDecl) {
        boolean modified = false;
        
        // Update RequestMapping to use JSON
        classDecl.getAnnotations().forEach(annotation -> {
            if (annotation.getNameAsString().equals("RequestMapping")) {
                annotation.asNormalAnnotationExpr().getPairs().forEach(pair -> {
                    if (pair.getNameAsString().equals("consumes") || 
                        pair.getNameAsString().equals("produces")) {
                        // Change to JSON
                        pair.setValue(new StringLiteralExpr("application/json"));
                        modified = true;
                    }
                });
            }
        });
        
        return modified;
    }
    
    private boolean refactorMethod(MethodDeclaration method,
                                 DetectedPackages packages,
                                 List<OperationManifest.OperationInfo> targetOperations) {
        
        // Check if this is a controller endpoint
        boolean isEndpoint = method.getAnnotations().stream().anyMatch(a ->
            Set.of("GetMapping", "PostMapping", "PutMapping", 
                   "DeleteMapping", "PatchMapping", "RequestMapping")
                .contains(a.getNameAsString()));
        
        if (!isEndpoint) {
            return false;
        }
        
        log.debug("Refactoring endpoint method: {}", method.getNameAsString());
        
        // Update method annotations
        updateMethodAnnotations(method);
        
        // Update method parameters
        updateMethodParameters(method);
        
        // Update method body
        updateMethodBody(method, packages);
        
        return true;
    }
    
    private void updateMethodAnnotations(MethodDeclaration method) {
        method.getAnnotations().forEach(annotation -> {
            String annotationName = annotation.getNameAsString();
            
            // Update mapping annotations
            if (Set.of("PostMapping", "PutMapping", "PatchMapping")
                    .contains(annotationName)) {
                if (annotation.isNormalAnnotationExpr()) {
                    annotation.asNormalAnnotationExpr().getPairs().forEach(pair -> {
                        if (pair.getNameAsString().equals("consumes")) {
                            pair.setValue(new StringLiteralExpr("application/json"));
                        }
                        if (pair.getNameAsString().equals("produces")) {
                            pair.setValue(new StringLiteralExpr("application/json"));
                        }
                    });
                }
            }
        });
    }
    
    private void updateMethodParameters(MethodDeclaration method) {
        method.getParameters().forEach(param -> {
            // Change XML-related parameter types
            String typeName = param.getTypeAsString();
            if (typeName.contains("Document") || typeName.contains("Element")) {
                param.setType("Map<String, Object>");
            }
            
            // Update annotations
            param.getAnnotations().forEach(annotation -> {
                if (annotation.getNameAsString().equals("RequestParam") &&
                    param.getNameAsString().equals("xmldata")) {
                    // Change to RequestBody (simplified - would need proper annotation manipulation)
                    // param.removeAnnotation(annotation); // Not available in this JavaParser version
                    // param.addAnnotation("RequestBody");
                    // param.addAnnotation("Valid");
                }
            });
        });
    }
    
    private void updateMethodBody(MethodDeclaration method, DetectedPackages packages) {
        BlockStmt body = method.getBody().orElse(null);
        if (body == null) return;
        
        // This is a simplified version - real implementation would be more sophisticated
        body.accept(new com.github.javaparser.ast.visitor.VoidVisitorAdapter<Void>() {
            @Override
            public void visit(MethodCallExpr n, Void arg) {
                // Replace XML processing calls
                if (n.getNameAsString().equals("processXml") ||
                    n.getNameAsString().equals("marshal") ||
                    n.getNameAsString().equals("unmarshal")) {
                    
                    // Replace with JSON processing (simplified)
                    n.setName("writeValueAsString");
                    n.setScope(new NameExpr("objectMapper"));
                }
                
                super.visit(n, arg);
            }
        }, null);
    }
    
    private boolean isSimilarOperation(OperationManifest.OperationInfo convergeOp,
                                     OperationManifest.OperationInfo elavonOp) {
        // Compare by various criteria
        String convergePath = convergeOp.getPath().toLowerCase();
        String elavonPath = elavonOp.getPath().toLowerCase();
        
        // Similar paths
        if (convergePath.contains(elavonPath) || elavonPath.contains(convergePath)) {
            return true;
        }
        
        // Similar operation IDs
        String convergeId = convergeOp.getOperationId().toLowerCase();
        String elavonId = elavonOp.getOperationId().toLowerCase();
        
        if (convergeId.contains(elavonId) || elavonId.contains(convergeId)) {
            return true;
        }
        
        // Similar summaries
        if (convergeOp.getSummary() != null && elavonOp.getSummary() != null) {
            String convergeSummary = convergeOp.getSummary().toLowerCase();
            String elavonSummary = elavonOp.getSummary().toLowerCase();
            
            // Check for common keywords
            Set<String> convergeWords = Set.of(convergeSummary.split("\\s+"));
            Set<String> elavonWords = Set.of(elavonSummary.split("\\s+"));
            
            long commonWords = convergeWords.stream()
                .filter(elavonWords::contains)
                .count();
            
            return commonWords >= 2; // At least 2 common words
        }
        
        return false;
    }
    
    @lombok.Data
    @lombok.AllArgsConstructor
    private static class LegacyCodeLocation {
        private String filePath;
        private CompilationUnit compilationUnit;
        private MethodDeclaration method;
    }
}
