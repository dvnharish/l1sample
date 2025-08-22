package com.elavon.codegen.engine.generator;

import com.elavon.codegen.engine.openapi.OperationManifest;
import com.elavon.codegen.engine.scanner.DetectedPackages;
import com.squareup.javapoet.*;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import javax.lang.model.element.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import com.elavon.codegen.engine.config.CodegenConfig;

/**
 * Generates service classes that orchestrate business logic and client calls.
 */
@Slf4j
@org.springframework.stereotype.Component
public class ServiceGenerator extends BaseGenerator {
    
    /**
     * Generate a service class for a specific tag.
     */
    public String generate(CodegenConfig config, DetectedPackages packages, OpenAPI spec,
                          OperationManifest.OperationInfo operationInfo) {
        
        String tag = operationInfo.getTag();
        String servicePackage = packages.getTagPackage("service", tag);
        String serviceClassName = toClassName(tag) + "Service";
        
        log.info("Generating service: {} for tag: {}", serviceClassName, tag);
        
        // Collect all operations for this tag
        List<OperationManifest.OperationInfo> tagOperations = 
            collectOperationsForTag(spec, tag);
        
        TypeSpec serviceClass = generateServiceClass(packages, spec, serviceClassName, 
            tag, tagOperations);
        
        return writeJavaFile(servicePackage, serviceClass, getOutputDir(), config.isDryRun());
    }
    
    private List<OperationManifest.OperationInfo> collectOperationsForTag(
            OpenAPI spec, String tag) {
        // Similar to ClientGenerator's implementation
        List<OperationManifest.OperationInfo> operations = new ArrayList<>();
        
        spec.getPaths().forEach((path, pathItem) -> {
            pathItem.readOperationsMap().forEach((method, operation) -> {
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
    
    private TypeSpec generateServiceClass(DetectedPackages packages, OpenAPI spec,
                                        String className, String tag,
                                        List<OperationManifest.OperationInfo> operations) {
        
        String clientClassName = toClassName(tag) + "Client";
        String clientPackage = packages.getTagPackage("client", tag);
        ClassName clientClass = ClassName.get(clientPackage, clientClassName);
        
        TypeSpec.Builder classBuilder = TypeSpec.classBuilder(className)
            .addModifiers(Modifier.PUBLIC)
            .addAnnotation(Slf4j.class)
            .addAnnotation(Service.class)
            .addAnnotation(RequiredArgsConstructor.class)
            .addJavadoc("Service for Elavon $L operations.\n", tag)
            .addJavadoc("Orchestrates validation, business logic, and API calls.\n");
        
        // Add client field
        classBuilder.addField(FieldSpec.builder(clientClass, toCamelCase(clientClassName))
            .addModifiers(Modifier.PRIVATE, Modifier.FINAL)
            .build());
        
        // Add validator field if needed
        if (hasValidation(operations)) {
            classBuilder.addField(FieldSpec.builder(
                ClassName.get("jakarta.validation", "Validator"), "validator")
                .addModifiers(Modifier.PRIVATE, Modifier.FINAL)
                .build());
        }
        
        // Add method for each operation
        for (OperationManifest.OperationInfo operation : operations) {
            MethodSpec method = generateServiceMethod(packages, spec, operation, clientClass);
            classBuilder.addMethod(method);
        }
        
        // Add helper methods
        classBuilder.addMethod(generateValidationMethod());
        classBuilder.addMethod(generateErrorMappingMethod());
        
        return classBuilder.build();
    }
    
    private MethodSpec generateServiceMethod(DetectedPackages packages, OpenAPI spec,
                                           OperationManifest.OperationInfo operationInfo,
                                           ClassName clientClass) {
        
        Operation operation = operationInfo.getOperation();
        String methodName = toCamelCase(operationInfo.getOperationId());
        
        MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder(methodName)
            .addModifiers(Modifier.PUBLIC);
        
        // Add documentation
        if (operation.getSummary() != null) {
            methodBuilder.addJavadoc(sanitizeComment(operation.getSummary()) + "\n");
        }
        
        // Copy parameters from client method
        List<ParameterSpec> params = generateServiceParameters(packages, spec, operation);
        params.forEach(methodBuilder::addParameter);
        
        // Determine response type
        TypeName responseType = determineResponseType(packages, spec, operation);
        methodBuilder.returns(ParameterizedTypeName.get(
            ClassName.get(Mono.class), responseType));
        
        // Generate method body
        generateServiceMethodBody(methodBuilder, operationInfo, params, clientClass);
        
        return methodBuilder.build();
    }
    
    private List<ParameterSpec> generateServiceParameters(DetectedPackages packages,
                                                        OpenAPI spec,
                                                        Operation operation) {
        // Similar to ClientGenerator but simplified
        List<ParameterSpec> params = new ArrayList<>();
        
        // Path parameters
        if (operation.getParameters() != null) {
            operation.getParameters().stream()
                .filter(p -> "path".equals(p.getIn()))
                .forEach(param -> {
                    TypeName type = getParameterType(param.getSchema());
                    params.add(ParameterSpec.builder(type, toCamelCase(param.getName()))
                        .build());
                });
        }
        
        // Query parameters
        List<io.swagger.v3.oas.models.parameters.Parameter> queryParams = 
            operation.getParameters() != null ?
                operation.getParameters().stream()
                    .filter(p -> "query".equals(p.getIn()))
                    .collect(Collectors.toList()) : List.of();
        
        if (queryParams.size() > 3) {
            params.add(ParameterSpec.builder(
                ParameterizedTypeName.get(
                    ClassName.get(java.util.Map.class),
                    ClassName.get(String.class),
                    ClassName.get(Object.class)),
                "queryParams")
                .build());
        } else {
            queryParams.forEach(param -> {
                TypeName type = getParameterType(param.getSchema());
                params.add(ParameterSpec.builder(type, toCamelCase(param.getName()))
                    .build());
            });
        }
        
        // Request body
        if (operation.getRequestBody() != null) {
            TypeName requestType = determineRequestType(packages, spec, operation);
            params.add(ParameterSpec.builder(requestType, "request").build());
        }
        
        return params;
    }
    
    private void generateServiceMethodBody(MethodSpec.Builder methodBuilder,
                                         OperationManifest.OperationInfo operationInfo,
                                         List<ParameterSpec> params,
                                         ClassName clientClass) {
        
        String clientFieldName = toCamelCase(clientClass.simpleName());
        String methodName = toCamelCase(operationInfo.getOperationId());
        
        methodBuilder.addStatement("log.info($S, $S)", 
            "Processing " + operationInfo.getOperationId() + " request", 
            operationInfo.getPath());
        
        // Add validation for request body
        if (params.stream().anyMatch(p -> p.name.equals("request"))) {
            methodBuilder.addCode("\n");
            methodBuilder.addComment("Validate request");
            methodBuilder.addStatement("validateRequest(request)");
        }
        
        // Build client method call
        methodBuilder.addCode("\n");
        methodBuilder.addComment("Call Elavon API");
        StringBuilder clientCall = new StringBuilder();
        clientCall.append("return ").append(clientFieldName).append(".").append(methodName).append("(");
        
        // Add parameters
        String paramList = params.stream()
            .map(p -> p.name)
            .collect(Collectors.joining(", "));
        clientCall.append(paramList).append(")");
        
        methodBuilder.addStatement(clientCall.toString());
        
        // Add error handling
        methodBuilder.addStatement("    .doOnSuccess(response -> log.info($S, response))",
            "Successfully processed " + operationInfo.getOperationId());
        methodBuilder.addStatement("    .doOnError(error -> log.error($S, error))",
            "Error processing " + operationInfo.getOperationId());
        methodBuilder.addStatement("    .onErrorMap(this::mapError)");
    }
    
    private MethodSpec generateValidationMethod() {
        return MethodSpec.methodBuilder("validateRequest")
            .addModifiers(Modifier.PRIVATE)
            .addTypeVariable(TypeVariableName.get("T"))
            .addParameter(TypeVariableName.get("T"), "request")
            .addStatement("if (request == null) {")
            .addStatement("    throw new $T($S)",
                ClassName.get(IllegalArgumentException.class), "Request cannot be null")
            .addStatement("}")
            .addCode("\n")
            .addStatement("var violations = validator.validate(request)")
            .addStatement("if (!violations.isEmpty()) {")
            .addStatement("    var message = violations.stream()")
            .addStatement("        .map(v -> v.getPropertyPath() + $S + v.getMessage())", ": ")
            .addStatement("        .collect($T.joining($S))",
                ClassName.get(Collectors.class), ", ")
            .addStatement("    throw new $T($S + message)",
                ClassName.get("jakarta.validation", "ValidationException"),
                "Validation failed: ")
            .addStatement("}")
            .build();
    }
    
    private MethodSpec generateErrorMappingMethod() {
        return MethodSpec.methodBuilder("mapError")
            .addModifiers(Modifier.PRIVATE)
            .returns(Throwable.class)
            .addParameter(Throwable.class, "error")
            .addStatement("if (error instanceof $T) {",
                ClassName.get("org.springframework.web.reactive.function.client",
                    "WebClientResponseException"))
            .addStatement("    var webClientError = ($T) error",
                ClassName.get("org.springframework.web.reactive.function.client",
                    "WebClientResponseException"))
            .addStatement("    var status = webClientError.getStatusCode()")
            .addStatement("    var body = webClientError.getResponseBodyAsString()")
            .addCode("\n")
            .addStatement("    return switch (status.value()) {")
            .addStatement("        case 400 -> new $T($S + body)",
                ClassName.get(IllegalArgumentException.class), "Bad request: ")
            .addStatement("        case 401 -> new $T($S)",
                ClassName.get(SecurityException.class), "Unauthorized")
            .addStatement("        case 403 -> new $T($S)",
                ClassName.get(SecurityException.class), "Forbidden")
            .addStatement("        case 404 -> new $T($S + body)",
                ClassName.get("java.util", "NoSuchElementException"), "Not found: ")
            .addStatement("        case 429 -> new $T($S)",
                ClassName.get(RuntimeException.class), "Rate limit exceeded")
            .addStatement("        default -> new $T($S + status + $S + body)",
                ClassName.get(RuntimeException.class), "API error ", ": ")
            .addStatement("    }")
            .addStatement("}")
            .addCode("\n")
            .addStatement("return error")
            .build();
    }
    
    private TypeName getParameterType(io.swagger.v3.oas.models.media.Schema<?> schema) {
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
        // Simplified version - in real implementation would resolve schema references
        if (operation.getRequestBody() == null) {
            return ClassName.get(Void.class);
        }
        
        return ClassName.get(Object.class); // Placeholder
    }
    
    private TypeName determineResponseType(DetectedPackages packages, OpenAPI spec,
                                         Operation operation) {
        // Simplified version - in real implementation would resolve schema references
        if (operation.getResponses() == null) {
            return ClassName.get(Void.class);
        }
        
        return ClassName.get(Object.class); // Placeholder
    }
    
    private boolean hasValidation(List<OperationManifest.OperationInfo> operations) {
        return operations.stream()
            .anyMatch(op -> op.getOperation().getRequestBody() != null);
    }
}
