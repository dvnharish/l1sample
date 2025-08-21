package com.elavon.codegen.engine.openapi;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.parser.core.models.ParseOptions;
import io.swagger.v3.parser.core.models.SwaggerParseResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * Loads and resolves OpenAPI specifications with full reference resolution.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OpenApiLoader {
    
    /**
     * Load an OpenAPI specification from a file path or classpath.
     * Handles $ref resolution, circular refs, oneOf/allOf/anyOf, discriminators, etc.
     */
    public OpenAPI loadSpec(String specPath) {
        log.info("Loading OpenAPI spec from: {}", specPath);
        
        String specContent;
        try {
            if (specPath.startsWith("classpath:")) {
                specContent = loadFromClasspath(specPath);
            } else {
                specContent = loadFromFile(specPath);
            }
        } catch (IOException e) {
            throw new OpenApiLoaderException("Failed to read OpenAPI spec: " + specPath, e);
        }
        
        // Configure parser options for full resolution
        ParseOptions parseOptions = new ParseOptions();
        parseOptions.setResolve(true); // Resolve all $refs
        parseOptions.setResolveFully(true); // Fully resolve nested refs
        parseOptions.setResolveCombinators(true); // Resolve allOf/anyOf/oneOf
        parseOptions.setFlatten(true); // Flatten combined schemas
        
        // Parse the OpenAPI spec
        SwaggerParseResult result = new OpenAPIV3Parser().readContents(
            specContent, null, parseOptions);
        
        if (result.getOpenAPI() == null) {
            String errors = String.join("; ", result.getMessages());
            throw new OpenApiLoaderException("Failed to parse OpenAPI spec: " + errors);
        }
        
        if (result.getMessages() != null && !result.getMessages().isEmpty()) {
            log.warn("OpenAPI parsing warnings: {}", result.getMessages());
        }
        
        OpenAPI openAPI = result.getOpenAPI();
        log.info("Loaded OpenAPI spec: {} {}", 
            openAPI.getInfo().getTitle(), 
            openAPI.getInfo().getVersion());
        
        return openAPI;
    }

    private String loadFromFile(String specPath) throws IOException {
        Path path = Paths.get(specPath);
        if (!Files.exists(path)) {
            throw new OpenApiLoaderException("OpenAPI spec file not found: " + specPath);
        }
        return Files.readString(path);
    }

    private String loadFromClasspath(String specPath) throws IOException {
        String resourceName = specPath.substring("classpath:".length());
        ClassPathResource resource = new ClassPathResource(resourceName);
        if (!resource.exists()) {
            throw new OpenApiLoaderException("OpenAPI spec not found on classpath: " + resourceName);
        }
        try (InputStream inputStream = resource.getInputStream()) {
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
    
    /**
     * Build an operation manifest from an OpenAPI specification.
     */
    public OperationManifest buildManifest(OpenAPI spec, String specName) {
        log.info("Building operation manifest for: {}", specName);
        
        OperationManifest manifest = new OperationManifest(specName);
        
        if (spec.getPaths() == null) {
            return manifest;
        }
        
        // Process all paths and operations
        spec.getPaths().forEach((pathStr, pathItem) -> {
            processPathItem(manifest, pathStr, pathItem);
        });
        
        log.info("Built manifest with {} operations across {} tags", 
            manifest.getAllOperations().size(), 
            manifest.getAllTags().size());
        
        return manifest;
    }
    
    private void processPathItem(OperationManifest manifest, String path, PathItem pathItem) {
        Map<PathItem.HttpMethod, Operation> operations = new LinkedHashMap<>();
        
        // Collect all operations for this path
        if (pathItem.getGet() != null) operations.put(PathItem.HttpMethod.GET, pathItem.getGet());
        if (pathItem.getPost() != null) operations.put(PathItem.HttpMethod.POST, pathItem.getPost());
        if (pathItem.getPut() != null) operations.put(PathItem.HttpMethod.PUT, pathItem.getPut());
        if (pathItem.getDelete() != null) operations.put(PathItem.HttpMethod.DELETE, pathItem.getDelete());
        if (pathItem.getPatch() != null) operations.put(PathItem.HttpMethod.PATCH, pathItem.getPatch());
        if (pathItem.getHead() != null) operations.put(PathItem.HttpMethod.HEAD, pathItem.getHead());
        if (pathItem.getOptions() != null) operations.put(PathItem.HttpMethod.OPTIONS, pathItem.getOptions());
        if (pathItem.getTrace() != null) operations.put(PathItem.HttpMethod.TRACE, pathItem.getTrace());
        
        // Process each operation
        operations.forEach((method, operation) -> {
            if (operation.getOperationId() == null) {
                // Generate operation ID if missing
                String operationId = generateOperationId(path, method.toString());
                operation.setOperationId(operationId);
                log.warn("Generated operationId for {} {}: {}", method, path, operationId);
            }
            
            // Determine tags
            List<String> tags = operation.getTags();
            if (tags == null || tags.isEmpty()) {
                tags = List.of("default");
            }
            
            // Create operation info
            OperationManifest.OperationInfo info = OperationManifest.OperationInfo.builder()
                .operationId(operation.getOperationId())
                .path(path)
                .method(method.toString())
                .summary(operation.getSummary())
                .description(operation.getDescription())
                .tag(tags.get(0)) // Primary tag
                .tags(tags)
                .operation(operation)
                .build();
            
            manifest.addOperation(info);
        });
    }
    
    private String generateOperationId(String path, String method) {
        // Convert path to camelCase operation ID
        String[] parts = path.split("/");
        StringBuilder operationId = new StringBuilder(method.toLowerCase());
        
        for (String part : parts) {
            if (part.isEmpty()) continue;
            
            if (part.startsWith("{") && part.endsWith("}")) {
                // Path parameter - add "By" + parameter name
                String param = part.substring(1, part.length() - 1);
                operationId.append("By").append(capitalize(param));
            } else {
                // Regular path segment
                operationId.append(capitalize(part));
            }
        }
        
        return operationId.toString();
    }
    
    private String capitalize(String str) {
        if (str == null || str.isEmpty()) return str;
        return Character.toUpperCase(str.charAt(0)) + str.substring(1);
    }
}
