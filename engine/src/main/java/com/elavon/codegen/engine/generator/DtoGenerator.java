package com.elavon.codegen.engine.generator;

import com.elavon.codegen.engine.openapi.OperationManifest;
import com.elavon.codegen.engine.scanner.DetectedPackages;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import com.squareup.javapoet.*;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.media.*;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.parameters.RequestBody;
import io.swagger.v3.oas.models.responses.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.lang.model.element.Modifier;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Generates DTO/POJO classes from OpenAPI schemas.
 * Handles all request/response models and nested types.
 */
@Slf4j
@Component
public class DtoGenerator extends BaseGenerator {
    
    private static final Set<String> PRIMITIVE_TYPES = Set.of(
        "string", "number", "integer", "boolean"
    );
    
    /**
     * Generate all DTOs for an operation.
     * 
     * @return List of generated file paths
     */
    public List<String> generate(DetectedPackages packages, OpenAPI spec, 
                                OperationManifest.OperationInfo operationInfo) {
        List<String> generatedFiles = new ArrayList<>();
        Set<String> processedSchemas = new HashSet<>();
        
        Operation operation = operationInfo.getOperation();
        String tag = operationInfo.getTag();
        String basePackage = packages.getTagPackage("dto", tag);
        
        log.info("Generating DTOs for operation: {} (tag: {})", 
            operationInfo.getOperationId(), tag);
        
        // Process request body schemas
        if (operation.getRequestBody() != null) {
            List<String> requestFiles = processRequestBody(
                packages, spec, operation.getRequestBody(), 
                basePackage, processedSchemas);
            generatedFiles.addAll(requestFiles);
        }
        
        // Process response schemas
        if (operation.getResponses() != null) {
            operation.getResponses().forEach((statusCode, apiResponse) -> {
                List<String> responseFiles = processResponse(
                    packages, spec, apiResponse, statusCode,
                    basePackage, processedSchemas);
                generatedFiles.addAll(responseFiles);
            });
        }
        
        // Process parameter schemas (query, header, path)
        if (operation.getParameters() != null) {
            for (Parameter parameter : operation.getParameters()) {
                if (parameter.getSchema() != null) {
                    List<String> paramFiles = processSchema(
                        packages, spec, parameter.getSchema(),
                        toClassName(parameter.getName()),
                        basePackage, processedSchemas);
                    generatedFiles.addAll(paramFiles);
                }
            }
        }
        
        return generatedFiles;
    }
    
    private List<String> processRequestBody(DetectedPackages packages, OpenAPI spec,
                                          RequestBody requestBody, String basePackage,
                                          Set<String> processedSchemas) {
        List<String> files = new ArrayList<>();
        
        if (requestBody.getContent() == null) {
            return files;
        }
        
        requestBody.getContent().forEach((mediaType, mediaTypeObj) -> {
            if (mediaTypeObj.getSchema() != null) {
                String className = deriveClassName(mediaTypeObj.getSchema(), "Request");
                files.addAll(processSchema(packages, spec, mediaTypeObj.getSchema(),
                    className, basePackage, processedSchemas));
            }
        });
        
        return files;
    }
    
    private List<String> processResponse(DetectedPackages packages, OpenAPI spec,
                                       ApiResponse apiResponse, String statusCode,
                                       String basePackage, Set<String> processedSchemas) {
        List<String> files = new ArrayList<>();
        
        if (apiResponse.getContent() == null) {
            return files;
        }
        
        apiResponse.getContent().forEach((mediaType, mediaTypeObj) -> {
            if (mediaTypeObj.getSchema() != null) {
                String suffix = statusCode.startsWith("2") ? "Response" : "ErrorResponse";
                String className = deriveClassName(mediaTypeObj.getSchema(), suffix);
                files.addAll(processSchema(packages, spec, mediaTypeObj.getSchema(),
                    className, basePackage, processedSchemas));
            }
        });
        
        return files;
    }
    
    private List<String> processSchema(DetectedPackages packages, OpenAPI spec,
                                     Schema<?> schema, String suggestedName,
                                     String basePackage, Set<String> processedSchemas) {
        List<String> files = new ArrayList<>();
        
        // Handle $ref schemas
        if (schema.get$ref() != null) {
            String ref = schema.get$ref();
            String schemaName = extractSchemaName(ref);
            
            if (!processedSchemas.contains(ref)) {
                processedSchemas.add(ref);
                
                Schema<?> resolvedSchema = resolveSchema(spec, ref);
                if (resolvedSchema != null) {
                    files.addAll(processSchema(packages, spec, resolvedSchema,
                        schemaName, basePackage, processedSchemas));
                }
            }
            return files;
        }
        
        // Handle array schemas
        if (schema instanceof ArraySchema) {
            ArraySchema arraySchema = (ArraySchema) schema;
            if (arraySchema.getItems() != null) {
                files.addAll(processSchema(packages, spec, arraySchema.getItems(),
                    suggestedName + "Item", basePackage, processedSchemas));
            }
            return files;
        }
        
        // Handle object schemas
        if ("object".equals(schema.getType()) || schema.getProperties() != null) {
            String className = deriveClassName(schema, suggestedName);
            String filePath = generateObjectDto(packages, spec, schema, className,
                basePackage, processedSchemas);
            files.add(filePath);
            
            // Process nested schemas
            if (schema.getProperties() != null) {
                schema.getProperties().forEach((propName, propSchema) -> {
                    if (needsSeparateClass(propSchema)) {
                        files.addAll(processSchema(packages, spec, propSchema,
                            className + toClassName(propName),
                            basePackage, processedSchemas));
                    }
                });
            }
        }
        
        // Handle composed schemas (allOf, anyOf, oneOf)
        if (schema instanceof ComposedSchema) {
            ComposedSchema composedSchema = (ComposedSchema) schema;
            
            if (composedSchema.getAllOf() != null) {
                for (Schema<?> subSchema : (java.util.List<Schema<?>>) composedSchema.getAllOf()) {
                    files.addAll(processSchema(packages, spec, subSchema,
                        suggestedName, basePackage, processedSchemas));
                }
            }
            
            if (composedSchema.getAnyOf() != null || composedSchema.getOneOf() != null) {
                // Generate interface or base class for polymorphic types
                String filePath = generatePolymorphicDto(packages, spec, composedSchema,
                    suggestedName, basePackage, processedSchemas);
                files.add(filePath);
            }
        }
        
        return files;
    }
    
    private String generateObjectDto(DetectedPackages packages, OpenAPI spec,
                                   Schema<?> schema, String className,
                                   String packageName, Set<String> processedSchemas) {
        
        TypeSpec.Builder classBuilder = TypeSpec.classBuilder(className)
            .addModifiers(Modifier.PUBLIC)
            .addAnnotation(Data.class)
            .addAnnotation(Builder.class)
            .addAnnotation(NoArgsConstructor.class)
            .addAnnotation(AllArgsConstructor.class)
            .addAnnotation(AnnotationSpec.builder(JsonInclude.class)
                .addMember("value", "$T.Include.NON_NULL", JsonInclude.class)
                .build());
        
        // Add class-level description
        if (schema.getDescription() != null) {
            classBuilder.addJavadoc(sanitizeComment(schema.getDescription()) + "\n");
        }
        
        // Add properties
        if (schema.getProperties() != null) {
            List<String> required = schema.getRequired() != null ? 
                schema.getRequired() : List.of();
            
            schema.getProperties().forEach((propName, propSchema) -> {
                FieldSpec field = generateField(propName, propSchema, 
                    required.contains(propName), spec);
                classBuilder.addField(field);
            });
        }
        
        // Handle discriminator for polymorphic types
        if (schema.getDiscriminator() != null) {
            String discriminatorProperty = schema.getDiscriminator().getPropertyName();
            classBuilder.addAnnotation(AnnotationSpec.builder(ClassName.get(
                    "com.fasterxml.jackson.annotation", "JsonTypeInfo"))
                .addMember("use", "$T.Id.NAME", 
                    ClassName.get("com.fasterxml.jackson.annotation", "JsonTypeInfo"))
                .addMember("property", "$S", discriminatorProperty)
                .build());
        }
        
        TypeSpec typeSpec = classBuilder.build();
        return writeJavaFile(packageName, typeSpec, getOutputDir());
    }
    
    private FieldSpec generateField(String propertyName, Schema<?> schema,
                                  boolean required, OpenAPI spec) {
        TypeName typeName = getTypeName(schema, spec);
        
        FieldSpec.Builder fieldBuilder = FieldSpec.builder(typeName, toCamelCase(propertyName))
            .addModifiers(Modifier.PRIVATE)
            .addAnnotation(AnnotationSpec.builder(JsonProperty.class)
                .addMember("value", "$S", propertyName)
                .build());
        
        // Add description
        if (schema.getDescription() != null) {
            fieldBuilder.addJavadoc(sanitizeComment(schema.getDescription()) + "\n");
        }
        
        // Add validation annotations
        if (required) {
            fieldBuilder.addAnnotation(NotNull.class);
        }
        
        if (schema instanceof StringSchema) {
            StringSchema stringSchema = (StringSchema) schema;
            
            if (stringSchema.getMinLength() != null || stringSchema.getMaxLength() != null) {
                AnnotationSpec.Builder sizeBuilder = AnnotationSpec.builder(Size.class);
                if (stringSchema.getMinLength() != null) {
                    sizeBuilder.addMember("min", "$L", stringSchema.getMinLength());
                }
                if (stringSchema.getMaxLength() != null) {
                    sizeBuilder.addMember("max", "$L", stringSchema.getMaxLength());
                }
                fieldBuilder.addAnnotation(sizeBuilder.build());
            }
            
            if (stringSchema.getPattern() != null) {
                fieldBuilder.addAnnotation(AnnotationSpec.builder(Pattern.class)
                    .addMember("regexp", "$S", stringSchema.getPattern())
                    .build());
            }
            
            if ("email".equals(stringSchema.getFormat())) {
                fieldBuilder.addAnnotation(Email.class);
            }
        }
        
        if (schema instanceof NumberSchema || schema instanceof IntegerSchema) {
            if (schema.getMinimum() != null) {
                fieldBuilder.addAnnotation(AnnotationSpec.builder(Min.class)
                    .addMember("value", "$L", schema.getMinimum())
                    .build());
            }
            if (schema.getMaximum() != null) {
                fieldBuilder.addAnnotation(AnnotationSpec.builder(Max.class)
                    .addMember("value", "$L", schema.getMaximum())
                    .build());
            }
        }
        
        // Add @Valid for nested objects and arrays
        if (schema instanceof ObjectSchema || schema instanceof ArraySchema ||
            schema.get$ref() != null) {
            fieldBuilder.addAnnotation(Valid.class);
        }
        
        return fieldBuilder.build();
    }
    
    private TypeName getTypeName(Schema<?> schema, OpenAPI spec) {
        // Handle $ref
        if (schema.get$ref() != null) {
            String schemaName = extractSchemaName(schema.get$ref());
            return ClassName.bestGuess(schemaName);
        }
        
        // Handle arrays
        if (schema instanceof ArraySchema) {
            ArraySchema arraySchema = (ArraySchema) schema;
            TypeName itemType = getTypeName(arraySchema.getItems(), spec);
            return ParameterizedTypeName.get(ClassName.get(List.class), itemType);
        }
        
        // Handle basic types
        String type = schema.getType();
        String format = schema.getFormat();
        
        if ("string".equals(type)) {
            if ("date".equals(format)) {
                return ClassName.get(LocalDate.class);
            } else if ("date-time".equals(format)) {
                return ClassName.get(OffsetDateTime.class);
            } else if ("uuid".equals(format)) {
                return ClassName.get(UUID.class);
            }
            return ClassName.get(String.class);
        } else if ("integer".equals(type)) {
            if ("int64".equals(format)) {
                return ClassName.get(Long.class);
            }
            return ClassName.get(Integer.class);
        } else if ("number".equals(type)) {
            if ("float".equals(format)) {
                return ClassName.get(Float.class);
            } else if ("double".equals(format)) {
                return ClassName.get(Double.class);
            }
            return ClassName.get(BigDecimal.class);
        } else if ("boolean".equals(type)) {
            return ClassName.get(Boolean.class);
        } else if ("object".equals(type)) {
            // For generic objects without properties
            return ParameterizedTypeName.get(
                ClassName.get(java.util.Map.class),
                ClassName.get(String.class),
                ClassName.get(Object.class)
            );
        }
        
        // Default to Object for unknown types
        return ClassName.get(Object.class);
    }
    
    private String generatePolymorphicDto(DetectedPackages packages, OpenAPI spec,
                                        ComposedSchema schema, String className,
                                        String packageName, Set<String> processedSchemas) {
        // For now, generate a simple class that extends/implements the composed schemas
        TypeSpec.Builder classBuilder = TypeSpec.classBuilder(className)
            .addModifiers(Modifier.PUBLIC)
            .addAnnotation(Data.class)
            .addAnnotation(NoArgsConstructor.class)
            .addAnnotation(AllArgsConstructor.class);
        
        // Process anyOf/oneOf as a union type (simplified for now)
        List<Schema<?>> schemas = new ArrayList<>();
        if (schema.getAnyOf() != null) schemas.addAll((java.util.List<Schema<?>>) schema.getAnyOf());
        if (schema.getOneOf() != null) schemas.addAll((java.util.List<Schema<?>>) schema.getOneOf());
        
        // Add a field for each possible type
        for (int i = 0; i < schemas.size(); i++) {
            Schema<?> subSchema = schemas.get(i);
            String fieldName = "option" + (i + 1);
            TypeName typeName = getTypeName(subSchema, spec);
            
            FieldSpec field = FieldSpec.builder(typeName, fieldName)
                .addModifiers(Modifier.PRIVATE)
                .addAnnotation(AnnotationSpec.builder(JsonProperty.class)
                    .addMember("value", "$S", fieldName)
                    .build())
                .build();
            
            classBuilder.addField(field);
        }
        
        TypeSpec typeSpec = classBuilder.build();
        return writeJavaFile(packageName, typeSpec, getOutputDir());
    }
    
    private String deriveClassName(Schema<?> schema, String defaultName) {
        if (schema.get$ref() != null) {
            return extractSchemaName(schema.get$ref());
        }
        
        if (schema.getTitle() != null) {
            return toClassName(schema.getTitle());
        }
        
        return toClassName(defaultName);
    }
    
    private String extractSchemaName(String ref) {
        // Extract schema name from $ref like "#/components/schemas/Transaction"
        String[] parts = ref.split("/");
        return parts[parts.length - 1];
    }
    
    private Schema<?> resolveSchema(OpenAPI spec, String ref) {
        if (spec.getComponents() == null || spec.getComponents().getSchemas() == null) {
            return null;
        }
        
        String schemaName = extractSchemaName(ref);
        return spec.getComponents().getSchemas().get(schemaName);
    }
    
    private boolean needsSeparateClass(Schema<?> schema) {
        if (schema.get$ref() != null) {
            return false; // Already defined elsewhere
        }
        
        if (schema instanceof ArraySchema) {
            ArraySchema arraySchema = (ArraySchema) schema;
            return needsSeparateClass(arraySchema.getItems());
        }
        
        if ("object".equals(schema.getType()) && schema.getProperties() != null) {
            return true;
        }
        
        if (schema instanceof ComposedSchema) {
            return true;
        }
        
        return false;
    }
}
