package com.elavon.codegen.engine.generator;

import com.elavon.codegen.engine.openapi.OperationManifest;
import com.elavon.codegen.engine.scanner.DetectedPackages;
import com.squareup.javapoet.*;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.parameters.Parameter;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import javax.lang.model.element.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.RequestBody;
import io.swagger.v3.oas.models.responses.ApiResponse;
import java.util.Map;
import com.elavon.codegen.engine.config.CodegenConfig;

/**
 * Generates REST controller classes from OpenAPI specifications.
 */
@Slf4j
@org.springframework.stereotype.Component
public class ControllerGenerator extends BaseGenerator {
    
    /**
     * Generate a controller class for a specific tag.
     */
    public String generate(CodegenConfig config, DetectedPackages packages, OpenAPI spec,
                          OperationManifest.OperationInfo operationInfo) {
        
        String tag = operationInfo.getTag();
        String controllerPackage = packages.getTagPackage("controller", tag);
        String controllerClassName = toClassName(tag) + "Controller";
        
        log.info("Generating controller: {} for tag: {}", controllerClassName, tag);
        
        // Collect all operations for this tag
        List<OperationManifest.OperationInfo> tagOperations = 
            collectOperationsForTag(spec, tag);
        
        TypeSpec controllerClass = generateControllerClass(packages, spec, 
            controllerClassName, tag, tagOperations);
        
        return writeJavaFile(controllerPackage, controllerClass, getOutputDir(), config.isDryRun());
    }
    
    private List<OperationManifest.OperationInfo> collectOperationsForTag(
            OpenAPI spec, String tag) {
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
    
    private TypeSpec generateControllerClass(DetectedPackages packages, OpenAPI spec,
                                           String className, String tag,
                                           List<OperationManifest.OperationInfo> operations) {
        
        String serviceClassName = toClassName(tag) + "Service";
        String servicePackage = packages.getTagPackage("service", tag);
        ClassName serviceClass = ClassName.get(servicePackage, serviceClassName);
        
        TypeSpec.Builder classBuilder = TypeSpec.classBuilder(className)
            .addModifiers(Modifier.PUBLIC)
            .addAnnotation(Slf4j.class)
            .addAnnotation(RestController.class)
            .addAnnotation(RequiredArgsConstructor.class)
            .addAnnotation(Validated.class)
            .addAnnotation(AnnotationSpec.builder(RequestMapping.class)
                .addMember("value", "$S", "/elavon/" + toPackageName(tag))
                .build())
            .addJavadoc("REST controller for Elavon $L operations.\n", tag);
        
        // Add service field
        classBuilder.addField(FieldSpec.builder(serviceClass, toCamelCase(serviceClassName))
            .addModifiers(Modifier.PRIVATE, Modifier.FINAL)
            .build());
        
        // Add endpoints for each operation
        for (OperationManifest.OperationInfo operation : operations) {
            MethodSpec endpoint = generateEndpoint(packages, spec, operation, serviceClass);
            classBuilder.addMethod(endpoint);
        }
        
        // Add exception handler
        classBuilder.addMethod(generateExceptionHandler());
        
        return classBuilder.build();
    }
    
    private MethodSpec generateEndpoint(DetectedPackages packages, OpenAPI spec,
                                      OperationManifest.OperationInfo operationInfo,
                                      ClassName serviceClass) {
        
        Operation operation = operationInfo.getOperation();
        String methodName = toCamelCase(operationInfo.getOperationId());
        
        MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder(methodName)
            .addModifiers(Modifier.PUBLIC);
        
        // Add documentation
        if (operation.getSummary() != null) {
            methodBuilder.addJavadoc(sanitizeComment(operation.getSummary()) + "\n");
        }
        if (operation.getDescription() != null) {
            methodBuilder.addJavadoc("\n" + sanitizeComment(operation.getDescription()) + "\n");
        }
        
        // Add mapping annotation
        addMappingAnnotation(methodBuilder, operationInfo);
        
        // Add parameters
        List<ParameterSpec> params = generateEndpointParameters(packages, spec, operation);
        params.forEach(methodBuilder::addParameter);
        
        // Determine response type
        TypeName responseType = determineResponseType(packages, spec, operation);
        methodBuilder.returns(ParameterizedTypeName.get(
            ClassName.get(Mono.class),
            ParameterizedTypeName.get(
                ClassName.get(ResponseEntity.class),
                responseType)));
        
        // Generate method body
        generateEndpointBody(methodBuilder, operationInfo, params, serviceClass);
        
        return methodBuilder.build();
    }
    
    private void addMappingAnnotation(MethodSpec.Builder methodBuilder,
                                    OperationManifest.OperationInfo operationInfo) {
        String path = operationInfo.getPath();
        String method = operationInfo.getMethod().toUpperCase();
        
        // Convert OpenAPI path to Spring path
        String springPath = path.replaceAll("\\{([^}]+)\\}", "{$1}");
        
        AnnotationSpec.Builder mappingBuilder = switch (method) {
            case "GET" -> AnnotationSpec.builder(GetMapping.class);
            case "POST" -> AnnotationSpec.builder(PostMapping.class);
            case "PUT" -> AnnotationSpec.builder(PutMapping.class);
            case "DELETE" -> AnnotationSpec.builder(DeleteMapping.class);
            case "PATCH" -> AnnotationSpec.builder(PatchMapping.class);
            default -> throw new IllegalArgumentException("Unsupported HTTP method: " + method);
        };
        
        // Remove base path if it's included
        if (springPath.startsWith("/elavon/")) {
            springPath = springPath.substring("/elavon/".length());
        }
        
        mappingBuilder.addMember("value", "$S", springPath);
        
        // Add produces/consumes
        if (!"GET".equals(method) && !"DELETE".equals(method)) {
            mappingBuilder.addMember("consumes", "$S", "application/json");
        }
        mappingBuilder.addMember("produces", "$S", "application/json");
        
        methodBuilder.addAnnotation(mappingBuilder.build());
        
        // Add response status for POST
        if ("POST".equals(method)) {
            methodBuilder.addAnnotation(AnnotationSpec.builder(ResponseStatus.class)
                .addMember("value", "$T.CREATED", HttpStatus.class)
                .build());
        }
    }
    
    private List<ParameterSpec> generateEndpointParameters(DetectedPackages packages,
                                                         OpenAPI spec,
                                                         Operation operation) {
        List<ParameterSpec> params = new ArrayList<>();
        
        // Path variables
        if (operation.getParameters() != null) {
            operation.getParameters().stream()
                .filter(p -> "path".equals(p.getIn()))
                .forEach(param -> {
                    TypeName type = getParameterType(param.getSchema());
                    ParameterSpec.Builder paramBuilder = ParameterSpec.builder(
                        type, toCamelCase(param.getName()))
                        .addAnnotation(AnnotationSpec.builder(PathVariable.class)
                            .addMember("value", "$S", param.getName())
                            .build());
                    
                    if (param.getRequired() != null && param.getRequired()) {
                        paramBuilder.addAnnotation(AnnotationSpec.builder(
                            ClassName.get("jakarta.validation.constraints", "NotNull"))
                            .build());
                    }
                    
                    params.add(paramBuilder.build());
                });
        }
        
        // Query parameters
        List<Parameter> queryParams = operation.getParameters() != null ?
            operation.getParameters().stream()
                .filter(p -> "query".equals(p.getIn()))
                .collect(Collectors.toList()) : List.of();
        
        queryParams.forEach(param -> {
            TypeName type = getParameterType(param.getSchema());
            ParameterSpec.Builder paramBuilder = ParameterSpec.builder(
                type, toCamelCase(param.getName()))
                .addAnnotation(AnnotationSpec.builder(RequestParam.class)
                    .addMember("value", "$S", param.getName())
                    .addMember("required", "$L", 
                        param.getRequired() != null ? param.getRequired() : false)
                    .build());
            
            // Add default value if specified
            if (param.getSchema() != null && param.getSchema().getDefault() != null) {
                paramBuilder.addAnnotation(AnnotationSpec.builder(RequestParam.class)
                    .addMember("defaultValue", "$S", 
                        param.getSchema().getDefault().toString())
                    .build());
            }
            
            params.add(paramBuilder.build());
        });
        
        // Request body
        if (operation.getRequestBody() != null) {
            TypeName requestType = determineRequestType(packages, spec, operation);
            ParameterSpec.Builder bodyBuilder = ParameterSpec.builder(requestType, "request")
                .addAnnotation(Valid.class)
                .addAnnotation(RequestBody.class);
            
            if (operation.getRequestBody().getRequired() != null && 
                operation.getRequestBody().getRequired()) {
                bodyBuilder.addAnnotation(AnnotationSpec.builder(
                    ClassName.get("jakarta.validation.constraints", "NotNull"))
                    .build());
            }
            
            params.add(bodyBuilder.build());
        }
        
        // Headers
        List<Parameter> headerParams = operation.getParameters() != null ?
            operation.getParameters().stream()
                .filter(p -> "header".equals(p.getIn()))
                .filter(p -> !isStandardHeader(p.getName()))
                .collect(Collectors.toList()) : List.of();
        
        headerParams.forEach(param -> {
            ParameterSpec.Builder paramBuilder = ParameterSpec.builder(
                String.class, toCamelCase(param.getName()))
                .addAnnotation(AnnotationSpec.builder(RequestHeader.class)
                    .addMember("value", "$S", param.getName())
                    .addMember("required", "$L",
                        param.getRequired() != null ? param.getRequired() : false)
                    .build());
            
            params.add(paramBuilder.build());
        });
        
        return params;
    }
    
    private void generateEndpointBody(MethodSpec.Builder methodBuilder,
                                    OperationManifest.OperationInfo operationInfo,
                                    List<ParameterSpec> params,
                                    ClassName serviceClass) {
        
        String serviceFieldName = toCamelCase(serviceClass.simpleName());
        String methodName = toCamelCase(operationInfo.getOperationId());
        
        methodBuilder.addStatement("log.info($S, $S)",
            "Handling " + operationInfo.getMethod() + " request",
            operationInfo.getPath());
        
        // Build service call
        StringBuilder serviceCall = new StringBuilder();
        serviceCall.append("return ").append(serviceFieldName).append(".")
            .append(methodName).append("(");
        
        // Add parameters (excluding annotations)
        String paramList = params.stream()
            .map(p -> p.name)
            .collect(Collectors.joining(", "));
        serviceCall.append(paramList).append(")");
        
        methodBuilder.addCode("\n");
        methodBuilder.addStatement(serviceCall.toString());
        
        // Map to ResponseEntity
        methodBuilder.addStatement("    .map($T::ok)", ResponseEntity.class);
        methodBuilder.addStatement("    .doOnSuccess(response -> log.info($S))",
            "Successfully handled " + operationInfo.getOperationId());
        methodBuilder.addStatement("    .doOnError(error -> log.error($S, error))",
            "Error handling " + operationInfo.getOperationId());
    }
    
    private MethodSpec generateExceptionHandler() {
        return MethodSpec.methodBuilder("handleValidationException")
            .addModifiers(Modifier.PUBLIC)
            .addAnnotation(ExceptionHandler.class)
            .addParameter(ClassName.get("jakarta.validation", "ValidationException"), "ex")
            .returns(ParameterizedTypeName.get(
                ClassName.get(ResponseEntity.class),
                ClassName.get(java.util.Map.class)))
            .addStatement("log.error($S, ex)", "Validation error")
            .addStatement("var error = $T.of($S, ex.getMessage())",
                ClassName.get(java.util.Map.class), "error")
            .addStatement("return $T.badRequest().body(error)", ResponseEntity.class)
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
        if (operation.getRequestBody() == null) {
            return ClassName.get(Void.class);
        }
        RequestBody requestBody = ((RequestBody) operation.getRequestBody());
        if (requestBody.getContent() == null) {
            return ClassName.get(Object.class);
        }
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
            return ClassName.bestGuess(toClassName(schemaName));
        }
        if (schema instanceof io.swagger.v3.oas.models.media.ArraySchema) {
            io.swagger.v3.oas.models.media.ArraySchema arraySchema = (io.swagger.v3.oas.models.media.ArraySchema) schema;
            TypeName itemType = getSchemaTypeName(packages, spec,
                arraySchema.getItems(), suffix);
            return ParameterizedTypeName.get(ClassName.get(List.class), itemType);
        }
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
        return Set.of("Content-Type", "Accept", "Authorization").contains(headerName);
    }
}
