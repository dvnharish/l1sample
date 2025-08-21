package com.elavon.codegen.engine.generator;

import com.elavon.codegen.engine.openapi.OperationManifest;
import com.elavon.codegen.engine.scanner.DetectedPackages;
import com.squareup.javapoet.*;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.parameters.RequestBody;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;

import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import javax.lang.model.element.Modifier;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Generates WebClient-based API clients from OpenAPI specifications.
 */
@Slf4j
@Component
public class ClientGenerator extends BaseGenerator {
    
    /**
     * Generate a WebClient for a specific tag.
     * Groups all operations with the same tag into one client class.
     */
    public String generate(DetectedPackages packages, OpenAPI spec,
                          OperationManifest.OperationInfo operationInfo) {
        
        String tag = operationInfo.getTag();
        String clientPackage = packages.getTagPackage("client", tag);
        String clientClassName = toClassName(tag) + "Client";
        
        log.info("Generating client: {} for tag: {}", clientClassName, tag);
        
        // Collect all operations for this tag
        List<OperationManifest.OperationInfo> tagOperations = 
            collectOperationsForTag(spec, tag);
        
        TypeSpec clientClass = generateClientClass(packages, spec, clientClassName, 
            tag, tagOperations);
        
        return writeJavaFile(clientPackage, clientClass, getOutputDir());
    }
    
    private List<OperationManifest.OperationInfo> collectOperationsForTag(
            OpenAPI spec, String tag) {
        List<OperationManifest.OperationInfo> operations = new ArrayList<>();
        
        spec.getPaths().forEach((path, pathItem) -> {
            Map<PathItem.HttpMethod, Operation> ops = new LinkedHashMap<>();
            if (pathItem.getGet() != null) ops.put(PathItem.HttpMethod.GET, pathItem.getGet());
            if (pathItem.getPost() != null) ops.put(PathItem.HttpMethod.POST, pathItem.getPost());
            if (pathItem.getPut() != null) ops.put(PathItem.HttpMethod.PUT, pathItem.getPut());
            if (pathItem.getDelete() != null) ops.put(PathItem.HttpMethod.DELETE, pathItem.getDelete());
            if (pathItem.getPatch() != null) ops.put(PathItem.HttpMethod.PATCH, pathItem.getPatch());
            
            ops.forEach((method, operation) -> {
                if (operation.getTags() != null && operation.getTags().contains(tag)) {
                    operations.add(OperationManifest.OperationInfo.builder()
                        .operationId(operation.getOperationId())
                        .path(path)
                        .method(method.toString())
                        .operation(operation)
                        .tag(tag)
                        .build());
                }
            });
        });
        
        return operations;
    }
    
    private TypeSpec generateClientClass(DetectedPackages packages, OpenAPI spec,
                                       String className, String tag,
                                       List<OperationManifest.OperationInfo> operations) {
        
        TypeSpec.Builder classBuilder = TypeSpec.classBuilder(className)
            .addModifiers(Modifier.PUBLIC)
            .addAnnotation(Slf4j.class)
            .addAnnotation(Component.class)
            .addAnnotation(RequiredArgsConstructor.class)
            .addJavadoc("WebClient for Elavon $L operations.\n", tag);
        
        // Add fields
        classBuilder.addField(FieldSpec.builder(WebClient.class, "webClient")
            .addModifiers(Modifier.PRIVATE, Modifier.FINAL)
            .build());
        
        // Configuration fields
        classBuilder.addField(FieldSpec.builder(String.class, "baseUrl")
            .addModifiers(Modifier.PRIVATE)
            .addAnnotation(AnnotationSpec.builder(Value.class)
                .addMember("value", "$S", "${elavon.base-url:https://api.elavon.com}")
                .build())
            .build());
        
        classBuilder.addField(FieldSpec.builder(Duration.class, "timeout")
            .addModifiers(Modifier.PRIVATE)
            .addAnnotation(AnnotationSpec.builder(Value.class)
                .addMember("value", "$S", "${elavon.client.timeout:PT30S}")
                .build())
            .build());
        
        classBuilder.addField(FieldSpec.builder(Integer.class, "maxRetries")
            .addModifiers(Modifier.PRIVATE)
            .addAnnotation(AnnotationSpec.builder(Value.class)
                .addMember("value", "$S", "${elavon.client.max-retries:3}")
                .build())
            .build());
        
        // Add constructor
        classBuilder.addMethod(generateConstructor());
        
        // Add method for each operation
        for (OperationManifest.OperationInfo operation : operations) {
            MethodSpec method = generateClientMethod(packages, spec, operation);
            classBuilder.addMethod(method);
        }
        
        // Add helper methods
        classBuilder.addMethod(generateRetrySpec());
        classBuilder.addMethod(generateErrorHandler());
        
        return classBuilder.build();
    }
    
    private MethodSpec generateConstructor() {
        return MethodSpec.constructorBuilder()
            .addModifiers(Modifier.PUBLIC)
            .addParameter(ParameterSpec.builder(WebClient.Builder.class, "webClientBuilder")
                .build())
            .addStatement("this.webClient = webClientBuilder.baseUrl(baseUrl).build()")
            .addJavadoc("Constructor for dependency injection.\n")
            .build();
    }
    
    private MethodSpec generateClientMethod(DetectedPackages packages, OpenAPI spec,
                                          OperationManifest.OperationInfo operationInfo) {
        
        Operation operation = operationInfo.getOperation();
        String methodName = toCamelCase(operationInfo.getOperationId());
        
        MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder(methodName)
            .addModifiers(Modifier.PUBLIC);
        
        // Add method documentation
        if (operation.getSummary() != null) {
            methodBuilder.addJavadoc(sanitizeComment(operation.getSummary()) + "\n");
        }
        if (operation.getDescription() != null) {
            methodBuilder.addJavadoc("\n" + sanitizeComment(operation.getDescription()) + "\n");
        }
        
        // Determine request and response types
        TypeName requestType = determineRequestType(packages, spec, operation);
        TypeName responseType = determineResponseType(packages, spec, operation);
        
        // Add parameters
        List<ParameterSpec> methodParams = generateMethodParameters(packages, spec, operation);
        methodParams.forEach(methodBuilder::addParameter);
        
        // Set return type
        methodBuilder.returns(ParameterizedTypeName.get(
            ClassName.get(Mono.class), responseType));
        
        // Generate method body
        generateMethodBody(methodBuilder, operationInfo, methodParams);
        
        return methodBuilder.build();
    }
    
    private List<ParameterSpec> generateMethodParameters(DetectedPackages packages,
                                                       OpenAPI spec,
                                                       Operation operation) {
        List<ParameterSpec> params = new ArrayList<>();
        
        // Path parameters
        if (operation.getParameters() != null) {
            operation.getParameters().stream()
                .filter(p -> "path".equals(p.getIn()))
                .forEach(param -> {
                    TypeName type = getParameterType(param.getSchema());
                    params.add(ParameterSpec.builder(type, toCamelCase(param.getName()))
                        .addJavadoc("@param $L $L\n", param.getName(), 
                            sanitizeComment(param.getDescription()))
                        .build());
                });
        }
        
        // Query parameters - collect into a map or individual params
        List<Parameter> queryParams = operation.getParameters() != null ?
            operation.getParameters().stream()
                .filter(p -> "query".equals(p.getIn()))
                .collect(Collectors.toList()) : List.of();
        
        if (!queryParams.isEmpty()) {
            // For multiple query params, use a Map
            if (queryParams.size() > 3) {
                params.add(ParameterSpec.builder(
                    ParameterizedTypeName.get(Map.class, String.class, Object.class),
                    "queryParams")
                    .addJavadoc("@param queryParams Query parameters\n")
                    .build());
            } else {
                // Individual parameters
                queryParams.forEach(param -> {
                    TypeName type = getParameterType(param.getSchema());
                    params.add(ParameterSpec.builder(type, toCamelCase(param.getName()))
                        .addJavadoc("@param $L $L\n", param.getName(),
                            sanitizeComment(param.getDescription()))
                        .build());
                });
            }
        }
        
        // Request body
        if (operation.getRequestBody() != null) {
            TypeName requestType = determineRequestType(packages, spec, operation);
            params.add(ParameterSpec.builder(requestType, "request")
                .addJavadoc("@param request The request body\n")
                .build());
        }
        
        // Headers - if custom headers are defined
        List<Parameter> headerParams = operation.getParameters() != null ?
            operation.getParameters().stream()
                .filter(p -> "header".equals(p.getIn()))
                .filter(p -> !isStandardHeader(p.getName()))
                .collect(Collectors.toList()) : List.of();
        
        if (!headerParams.isEmpty()) {
            params.add(ParameterSpec.builder(HttpHeaders.class, "headers")
                .addJavadoc("@param headers Custom headers\n")
                .build());
        }
        
        return params;
    }
    
    private void generateMethodBody(MethodSpec.Builder methodBuilder,
                                  OperationManifest.OperationInfo operationInfo,
                                  List<ParameterSpec> methodParams) {
        
        String path = operationInfo.getPath();
        String httpMethod = operationInfo.getMethod().toUpperCase();
        
        methodBuilder.addStatement("$T.info($S, $S)", 
            ClassName.get("log"), "Calling " + operationInfo.getOperationId(), path);
        
        // Build URI
        methodBuilder.addCode("\n");
        methodBuilder.addStatement("$T uriBuilder = $T.fromPath($S)",
            UriComponentsBuilder.class, UriComponentsBuilder.class, path);
        
        // Add path parameters
        methodParams.stream()
            .filter(p -> path.contains("{" + p.name + "}"))
            .forEach(param -> {
                methodBuilder.addStatement("uriBuilder.buildAndExpand($L)", param.name);
            });
        
        // Add query parameters
        if (methodParams.stream().anyMatch(p -> p.name.equals("queryParams"))) {
            methodBuilder.addStatement("queryParams.forEach(uriBuilder::queryParam)");
        } else {
            methodParams.stream()
                .filter(p -> !path.contains("{" + p.name + "}") && 
                        !p.name.equals("request") && 
                        !p.name.equals("headers"))
                .forEach(param -> {
                    methodBuilder.addStatement("if ($L != null) uriBuilder.queryParam($S, $L)",
                        param.name, param.name, param.name);
                });
        }
        
        methodBuilder.addStatement("String uri = uriBuilder.toUriString()");
        methodBuilder.addCode("\n");
        
        // Build request
        methodBuilder.addStatement("return webClient.method($T.$L)",
            HttpMethod.class, httpMethod);
        methodBuilder.addStatement("    .uri(uri)");
        methodBuilder.addStatement("    .headers(h -> {");
        methodBuilder.addStatement("        h.setContentType($T.APPLICATION_JSON)",
            org.springframework.http.MediaType.class);
        methodBuilder.addStatement("        h.setAccept($T.singletonList($T.APPLICATION_JSON))",
            Collections.class, org.springframework.http.MediaType.class);
        
        // Add custom headers if present
        if (methodParams.stream().anyMatch(p -> p.name.equals("headers"))) {
            methodBuilder.addStatement("        h.addAll(headers)");
        }
        
        methodBuilder.addStatement("    })");
        
        // Add request body if present
        if (methodParams.stream().anyMatch(p -> p.name.equals("request"))) {
            methodBuilder.addStatement("    .bodyValue(request)");
        }
        
        // Add response handling
        methodBuilder.addStatement("    .retrieve()");
        methodBuilder.addStatement("    .onStatus(status -> status.isError(), this::handleError)");
        methodBuilder.addStatement("    .bodyToMono($T.class)", 
            determineResponseType(null, null, operationInfo.getOperation()));
        methodBuilder.addStatement("    .timeout(timeout)");
        methodBuilder.addStatement("    .retryWhen(retrySpec())");
        methodBuilder.addStatement("    .doOnError(error -> log.error($S, error))",
            "Error calling " + operationInfo.getOperationId());
    }
    
    private MethodSpec generateRetrySpec() {
        return MethodSpec.methodBuilder("retrySpec")
            .addModifiers(Modifier.PRIVATE)
            .returns(ClassName.get(Retry.class))
            .addStatement("return $T.backoff(maxRetries, $T.ofSeconds(1))",
                Retry.class, Duration.class)
            .addStatement("    .maxBackoff($T.ofSeconds(10))", Duration.class)
            .addStatement("    .jitter(0.5)")
            .addStatement("    .filter(throwable -> throwable instanceof $T)",
                ClassName.get("org.springframework.web.reactive.function.client", 
                    "WebClientResponseException$ServiceUnavailable"))
            .build();
    }
    
    private MethodSpec generateErrorHandler() {
        return MethodSpec.methodBuilder("handleError")
            .addModifiers(Modifier.PRIVATE)
            .returns(ParameterizedTypeName.get(Mono.class, WildcardTypeName.subtypeOf(Throwable.class)))
            .addParameter(ClassName.get("org.springframework.web.reactive.function.client", 
                "ClientResponse"), "response")
            .addStatement("return response.bodyToMono($T.class)", String.class)
            .addStatement("    .flatMap(body -> {")
            .addStatement("        log.error($S, response.statusCode(), body)",
                "API error - Status: {}, Body: {}")
            .addStatement("        return $T.error(new $T($S + response.statusCode()))",
                Mono.class, RuntimeException.class, "API error: ")
            .addStatement("    })")
            .build();
    }
    
    private TypeName getParameterType(Schema<?> schema) {
        if (schema == null) return ClassName.get(String.class);
        
        String type = schema.getType();
        String format = schema.getFormat();
        
        if ("integer".equals(type)) {
            return "int64".equals(format) ? ClassName.get(Long.class) : ClassName.get(Integer.class);
        } else if ("number".equals(type)) {
            return ClassName.get(Double.class);
        } else if ("boolean".equals(type)) {
            return ClassName.get(Boolean.class);
        }
        
        return ClassName.get(String.class);
    }
    
    private TypeName determineRequestType(DetectedPackages packages, OpenAPI spec,
                                        Operation operation) {
        if (operation.getRequestBody() == null) {
            return ClassName.get(Void.class);
        }
        
        RequestBody requestBody = operation.getRequestBody();
        if (requestBody.getContent() == null) {
            return ClassName.get(Object.class);
        }
        
        // Get the first content type (usually application/json)
        io.swagger.v3.oas.models.media.MediaType mediaType = requestBody.getContent().values().iterator().next();
        if (mediaType.getSchema() == null) {
            return ClassName.get(Object.class);
        }
        
        return getSchemaTypeName(packages, spec, mediaType.getSchema(), "Request");
    }
    
    private TypeName determineResponseType(DetectedPackages packages, OpenAPI spec,
                                         Operation operation) {
        if (operation.getResponses() == null) {
            return ClassName.get(Void.class);
        }
        
        // Look for 200/201 response
        ApiResponse successResponse = operation.getResponses().get("200");
        if (successResponse == null) {
            successResponse = operation.getResponses().get("201");
        }
        if (successResponse == null) {
            successResponse = operation.getResponses().get("default");
        }
        
        if (successResponse == null || successResponse.getContent() == null) {
            return ClassName.get(Object.class);
        }
        
        io.swagger.v3.oas.models.media.MediaType mediaType = successResponse.getContent().values().iterator().next();
        if (mediaType.getSchema() == null) {
            return ClassName.get(Object.class);
        }
        
        return getSchemaTypeName(packages, spec, mediaType.getSchema(), "Response");
    }
    
    private TypeName getSchemaTypeName(DetectedPackages packages, OpenAPI spec,
                                     Schema<?> schema, String suffix) {
        if (schema.get$ref() != null) {
            String schemaName = extractSchemaName(schema.get$ref());
            return ClassName.bestGuess(schemaName);
        }
        
        if (schema instanceof io.swagger.v3.oas.models.media.ArraySchema) {
            io.swagger.v3.oas.models.media.ArraySchema arraySchema = (io.swagger.v3.oas.models.media.ArraySchema) schema;
            TypeName itemType = getSchemaTypeName(packages, spec, 
                arraySchema.getItems(), suffix);
            return ParameterizedTypeName.get(ClassName.get(List.class), itemType);
        }
        
        // For inline schemas, generate a name
        String title = schema.getTitle();
        if (title != null) {
            return ClassName.bestGuess(toClassName(title));
        }
        
        return ClassName.get(Object.class);
    }
    
    private String extractSchemaName(String ref) {
        String[] parts = ref.split("/");
        return parts[parts.length - 1];
    }
    
    private boolean isStandardHeader(String headerName) {
        Set<String> standardHeaders = Set.of(
            "Content-Type", "Accept", "Authorization", 
            "User-Agent", "Host", "Connection"
        );
        return standardHeaders.contains(headerName);
    }
}
