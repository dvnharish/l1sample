package com.elavon.codegen.engine.scanner;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.PackageDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.expr.AnnotationExpr;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.DirectoryFileFilter;
import org.apache.commons.io.filefilter.SuffixFileFilter;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Scanner to detect Spring Boot base packages from an existing project.
 * This is critical for accurate code generation.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PackageScanner {
    
    private static final Set<String> SPRING_COMPONENT_ANNOTATIONS = Set.of(
        "SpringBootApplication",
        "Component",
        "Service",
        "Controller",
        "RestController",
        "Repository",
        "Configuration"
    );
    
    private static final Set<String> SPRING_BOOT_APP_ANNOTATIONS = Set.of(
        "SpringBootApplication",
        "EnableAutoConfiguration"
    );
    
    private final JavaParser javaParser = new JavaParser();
    
    /**
     * Scan the project and detect package structure.
     * 
     * @param projectRoot the root directory of the Spring Boot project
     * @return detected packages or throw exception if not found
     */
    public DetectedPackages scanProject(String projectRoot) {
        Path rootPath = Paths.get(projectRoot).toAbsolutePath();
        File srcMainJava = rootPath.resolve("src/main/java").toFile();
        
        if (!srcMainJava.exists() || !srcMainJava.isDirectory()) {
            throw new PackageScannerException(
                "Cannot find src/main/java directory in project: " + projectRoot);
        }
        
        log.info("Scanning project at: {}", srcMainJava.getAbsolutePath());
        
        // Collect all Java files
        Collection<File> javaFiles = FileUtils.listFiles(
            srcMainJava,
            new SuffixFileFilter(".java"),
            DirectoryFileFilter.DIRECTORY
        );
        
        if (javaFiles.isEmpty()) {
            throw new PackageScannerException(
                "No Java files found in src/main/java");
        }
        
        // Parse files and collect package information
        Map<String, PackageInfo> packageInfoMap = new ConcurrentHashMap<>();
        String springBootAppPackage = null;
        
        for (File javaFile : javaFiles) {
            try {
                ParseResult<CompilationUnit> parseResult = javaParser.parse(javaFile);
                if (parseResult.isSuccessful() && parseResult.getResult().isPresent()) {
                    CompilationUnit cu = parseResult.getResult().get();
                    
                    // Get package name
                    Optional<PackageDeclaration> packageDecl = cu.getPackageDeclaration();
                    if (packageDecl.isEmpty()) {
                        continue;
                    }
                    
                    String packageName = packageDecl.get().getNameAsString();
                    PackageInfo info = packageInfoMap.computeIfAbsent(
                        packageName, k -> new PackageInfo(k));
                    info.incrementFileCount();
                    
                    // Check for Spring annotations
                    cu.findAll(ClassOrInterfaceDeclaration.class).forEach(classDecl -> {
                        for (AnnotationExpr annotation : classDecl.getAnnotations()) {
                            String annotationName = annotation.getNameAsString();
                            
                            if (SPRING_COMPONENT_ANNOTATIONS.contains(annotationName)) {
                                info.incrementSpringComponentCount();
                                info.addComponentType(annotationName);
                            }
                            
                            if (SPRING_BOOT_APP_ANNOTATIONS.contains(annotationName)) {
                                springBootAppPackage = packageName;
                                log.info("Found @SpringBootApplication in package: {}", packageName);
                            }
                        }
                    });
                }
            } catch (IOException e) {
                log.warn("Failed to parse file: {}", javaFile.getPath(), e);
            }
        }
        
        // Determine base package
        String basePackage = determineBasePackage(packageInfoMap, springBootAppPackage);
        
        // Detect sub-packages
        return detectSubPackages(packageInfoMap, basePackage);
    }
    
    private String determineBasePackage(Map<String, PackageInfo> packageInfoMap, 
                                       String springBootAppPackage) {
        if (StringUtils.isNotBlank(springBootAppPackage)) {
            log.info("Using @SpringBootApplication package as base: {}", springBootAppPackage);
            return springBootAppPackage;
        }
        
        // Find package with most Spring components
        Optional<Map.Entry<String, PackageInfo>> maxComponents = packageInfoMap.entrySet()
            .stream()
            .filter(e -> e.getValue().getSpringComponentCount() > 0)
            .max(Comparator.comparingInt(e -> e.getValue().getSpringComponentCount()));
        
        if (maxComponents.isPresent()) {
            String packageName = maxComponents.get().getKey();
            log.info("Using package with most Spring components as base: {} ({})", 
                packageName, maxComponents.get().getValue().getSpringComponentCount());
            return packageName;
        }
        
        // Find common root package
        Set<String> allPackages = packageInfoMap.keySet();
        String commonRoot = findCommonRootPackage(allPackages);
        
        if (StringUtils.isNotBlank(commonRoot)) {
            log.info("Using common root package as base: {}", commonRoot);
            return commonRoot;
        }
        
        throw new PackageScannerException(
            "Unable to determine base package. No @SpringBootApplication found and no clear package structure detected. " +
            "Please ensure your project has proper Spring Boot structure.");
    }
    
    private String findCommonRootPackage(Set<String> packages) {
        if (packages.isEmpty()) {
            return null;
        }
        
        // Find shortest package that is a prefix of most packages
        Map<String, Integer> prefixCounts = new HashMap<>();
        
        for (String pkg : packages) {
            String[] parts = pkg.split("\\.");
            StringBuilder prefix = new StringBuilder();
            
            for (int i = 0; i < parts.length - 1; i++) {
                if (i > 0) prefix.append(".");
                prefix.append(parts[i]);
                prefixCounts.merge(prefix.toString(), 1, Integer::sum);
            }
        }
        
        return prefixCounts.entrySet().stream()
            .filter(e -> e.getValue() >= packages.size() / 2) // At least half of packages
            .min(Comparator.comparingInt(e -> e.getKey().split("\\.").length))
            .map(Map.Entry::getKey)
            .orElse(null);
    }
    
    private DetectedPackages detectSubPackages(Map<String, PackageInfo> packageInfoMap,
                                              String basePackage) {
        DetectedPackages detected = new DetectedPackages();
        detected.setBasePackage(basePackage);
        
        // Find standard sub-packages
        String controllerPkg = findSubPackage(packageInfoMap, basePackage, 
            Set.of("controller", "controllers", "web", "rest", "api"),
            Set.of("Controller", "RestController"));
        detected.setControllerPackage(controllerPkg != null ? controllerPkg : basePackage + ".controller");
        
        String servicePkg = findSubPackage(packageInfoMap, basePackage,
            Set.of("service", "services", "business", "logic"),
            Set.of("Service"));
        detected.setServicePackage(servicePkg != null ? servicePkg : basePackage + ".service");
        
        String dtoPkg = findSubPackage(packageInfoMap, basePackage,
            Set.of("dto", "dtos", "model", "models", "domain", "entity", "entities"),
            Set.of());
        detected.setDtoPackage(dtoPkg != null ? dtoPkg : basePackage + ".dto");
        
        String clientPkg = findSubPackage(packageInfoMap, basePackage,
            Set.of("client", "clients", "integration", "external"),
            Set.of());
        detected.setClientPackage(clientPkg != null ? clientPkg : basePackage + ".client");
        
        String mapperPkg = findSubPackage(packageInfoMap, basePackage,
            Set.of("mapper", "mappers", "mapping", "converter", "converters"),
            Set.of());
        detected.setMapperPackage(mapperPkg != null ? mapperPkg : basePackage + ".mapper");
        
        log.info("Detected packages: {}", detected);
        return detected;
    }
    
    private String findSubPackage(Map<String, PackageInfo> packageInfoMap,
                                 String basePackage,
                                 Set<String> namePatterns,
                                 Set<String> annotationTypes) {
        // First try to find by name pattern
        for (String pattern : namePatterns) {
            String candidate = basePackage + "." + pattern;
            if (packageInfoMap.containsKey(candidate)) {
                return candidate;
            }
        }
        
        // Then try to find by annotation type
        if (!annotationTypes.isEmpty()) {
            return packageInfoMap.entrySet().stream()
                .filter(e -> e.getKey().startsWith(basePackage + "."))
                .filter(e -> e.getValue().getComponentTypes().stream()
                    .anyMatch(annotationTypes::contains))
                .min(Comparator.comparingInt(e -> e.getKey().length()))
                .map(Map.Entry::getKey)
                .orElse(null);
        }
        
        return null;
    }
    
    private static class PackageInfo {
        private final String packageName;
        private int fileCount = 0;
        private int springComponentCount = 0;
        private final Set<String> componentTypes = new HashSet<>();
        
        public PackageInfo(String packageName) {
            this.packageName = packageName;
        }
        
        public void incrementFileCount() {
            fileCount++;
        }
        
        public void incrementSpringComponentCount() {
            springComponentCount++;
        }
        
        public void addComponentType(String type) {
            componentTypes.add(type);
        }
        
        public int getSpringComponentCount() {
            return springComponentCount;
        }
        
        public Set<String> getComponentTypes() {
            return componentTypes;
        }
    }
}
