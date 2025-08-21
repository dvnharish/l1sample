package com.elavon.codegen.engine.generator;

import com.elavon.codegen.engine.openapi.OperationManifest;
import com.elavon.codegen.engine.scanner.DetectedPackages;
import com.squareup.javapoet.*;
import io.swagger.v3.oas.models.OpenAPI;
import lombok.extern.slf4j.Slf4j;
import org.mapstruct.*;
import org.springframework.stereotype.Component;

import javax.lang.model.element.Modifier;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Generates MapStruct mappers for converting between Converge (XML) and Elavon (JSON) models.
 */
@Slf4j
@Component
public class MapperGenerator extends BaseGenerator {
    
    /**
     * Generate a mapper interface for a specific operation.
     * Used in upgrade mode to map between legacy and new models.
     */
    public String generate(DetectedPackages packages, OpenAPI spec,
                          OperationManifest.OperationInfo operationInfo) {
        
        String tag = operationInfo.getTag();
        String mapperPackage = packages.getTagPackage("mapper", tag);
        String mapperClassName = toClassName(operationInfo.getOperationId()) + "Mapper";
        
        log.info("Generating mapper: {} for operation: {}", 
            mapperClassName, operationInfo.getOperationId());
        
        TypeSpec mapperInterface = generateMapperInterface(packages, spec, 
            mapperClassName, operationInfo);
        
        return writeJavaFile(mapperPackage, mapperInterface, getOutputDir());
    }
    
    private TypeSpec generateMapperInterface(DetectedPackages packages, OpenAPI spec,
                                           String className,
                                           OperationManifest.OperationInfo operationInfo) {
        
        TypeSpec.Builder interfaceBuilder = TypeSpec.interfaceBuilder(className)
            .addModifiers(Modifier.PUBLIC)
            .addAnnotation(AnnotationSpec.builder(Mapper.class)
                .addMember("componentModel", "$S", "spring")
                .addMember("unmappedTargetPolicy", "$T.WARN", ReportingPolicy.class)
                .addMember("nullValuePropertyMappingStrategy", 
                    "$T.IGNORE", NullValuePropertyMappingStrategy.class)
                .build())
            .addJavadoc("MapStruct mapper for converting between Converge and Elavon models.\n")
            .addJavadoc("Operation: $L\n", operationInfo.getOperationId());
        
        // Add mapping methods
        addRequestMappingMethod(interfaceBuilder, operationInfo);
        addResponseMappingMethod(interfaceBuilder, operationInfo);
        
        // Add custom mapping methods for common transformations
        addAmountMappingMethods(interfaceBuilder);
        addDateMappingMethods(interfaceBuilder);
        addCardMappingMethods(interfaceBuilder);
        addStatusMappingMethods(interfaceBuilder);
        
        return interfaceBuilder.build();
    }
    
    private void addRequestMappingMethod(TypeSpec.Builder interfaceBuilder,
                                       OperationManifest.OperationInfo operationInfo) {
        
        // Placeholder types - in real implementation would resolve from schemas
        ClassName convergeRequest = ClassName.get("com.legacy.converge.dto", 
            "Converge" + toClassName(operationInfo.getOperationId()) + "Request");
        ClassName elavonRequest = ClassName.get("com.elavon.dto", 
            toClassName(operationInfo.getOperationId()) + "Request");
        
        MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder("toElavonRequest")
            .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
            .returns(elavonRequest)
            .addParameter(convergeRequest, "convergeRequest")
            .addJavadoc("Convert Converge XML request to Elavon JSON request.\n");
        
        // Add mapping annotations for common field transformations
        addCommonRequestMappings(methodBuilder);
        
        interfaceBuilder.addMethod(methodBuilder.build());
    }
    
    private void addResponseMappingMethod(TypeSpec.Builder interfaceBuilder,
                                        OperationManifest.OperationInfo operationInfo) {
        
        // Placeholder types - in real implementation would resolve from schemas
        ClassName elavonResponse = ClassName.get("com.elavon.dto",
            toClassName(operationInfo.getOperationId()) + "Response");
        ClassName convergeResponse = ClassName.get("com.legacy.converge.dto",
            "Converge" + toClassName(operationInfo.getOperationId()) + "Response");
        
        MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder("toConvergeResponse")
            .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
            .returns(convergeResponse)
            .addParameter(elavonResponse, "elavonResponse")
            .addJavadoc("Convert Elavon JSON response to Converge XML response format.\n")
            .addJavadoc("This is used to maintain backward compatibility.\n");
        
        // Add mapping annotations for common field transformations
        addCommonResponseMappings(methodBuilder);
        
        interfaceBuilder.addMethod(methodBuilder.build());
    }
    
    private void addCommonRequestMappings(MethodSpec.Builder methodBuilder) {
        // Amount mapping
        methodBuilder.addAnnotation(AnnotationSpec.builder(Mapping.class)
            .addMember("source", "$S", "amount")
            .addMember("target", "$S", "total.amount")
            .addMember("qualifiedByName", "$S", "formatAmount")
            .build());
        
        // Currency mapping
        methodBuilder.addAnnotation(AnnotationSpec.builder(Mapping.class)
            .addMember("source", "$S", "currency")
            .addMember("target", "$S", "total.currencyCode")
            .build());
        
        // Card expiry mapping
        methodBuilder.addAnnotation(AnnotationSpec.builder(Mapping.class)
            .addMember("source", "$S", "cardExpiry")
            .addMember("target", "$S", "card.expirationMonth")
            .addMember("qualifiedByName", "$S", "extractExpiryMonth")
            .build());
        
        methodBuilder.addAnnotation(AnnotationSpec.builder(Mapping.class)
            .addMember("source", "$S", "cardExpiry")
            .addMember("target", "$S", "card.expirationYear")
            .addMember("qualifiedByName", "$S", "extractExpiryYear")
            .build());
        
        // PAN mapping (with masking)
        methodBuilder.addAnnotation(AnnotationSpec.builder(Mapping.class)
            .addMember("source", "$S", "cardNumber")
            .addMember("target", "$S", "card.number")
            .addMember("qualifiedByName", "$S", "maskCardNumber")
            .build());
    }
    
    private void addCommonResponseMappings(MethodSpec.Builder methodBuilder) {
        // Status mapping
        methodBuilder.addAnnotation(AnnotationSpec.builder(Mapping.class)
            .addMember("source", "$S", "status")
            .addMember("target", "$S", "result")
            .addMember("qualifiedByName", "$S", "mapStatusToResult")
            .build());
        
        // Authorization code
        methodBuilder.addAnnotation(AnnotationSpec.builder(Mapping.class)
            .addMember("source", "$S", "authorizationCode")
            .addMember("target", "$S", "approvalCode")
            .build());
        
        // Transaction ID
        methodBuilder.addAnnotation(AnnotationSpec.builder(Mapping.class)
            .addMember("source", "$S", "transactionId")
            .addMember("target", "$S", "txnId")
            .build());
        
        // Timestamp mapping
        methodBuilder.addAnnotation(AnnotationSpec.builder(Mapping.class)
            .addMember("source", "$S", "createdAt")
            .addMember("target", "$S", "timestamp")
            .addMember("qualifiedByName", "$S", "formatTimestamp")
            .build());
    }
    
    private void addAmountMappingMethods(TypeSpec.Builder interfaceBuilder) {
        // Format amount from cents to decimal string
        MethodSpec formatAmount = MethodSpec.methodBuilder("formatAmount")
            .addModifiers(Modifier.DEFAULT)
            .addAnnotation(Named.class)
            .addParameter(Long.class, "amountInCents")
            .returns(String.class)
            .addStatement("if (amountInCents == null) return null")
            .addStatement("return $T.valueOf(amountInCents).divide($T.valueOf(100)).toPlainString()",
                BigDecimal.class, BigDecimal.class)
            .build();
        
        interfaceBuilder.addMethod(formatAmount);
        
        // Parse amount from decimal string to cents
        MethodSpec parseAmount = MethodSpec.methodBuilder("parseAmount")
            .addModifiers(Modifier.DEFAULT)
            .addAnnotation(Named.class)
            .addParameter(String.class, "amountStr")
            .returns(Long.class)
            .addStatement("if (amountStr == null) return null")
            .addStatement("return $T.valueOf(amountStr).multiply($T.valueOf(100)).longValue()",
                BigDecimal.class, BigDecimal.class)
            .build();
        
        interfaceBuilder.addMethod(parseAmount);
    }
    
    private void addDateMappingMethods(TypeSpec.Builder interfaceBuilder) {
        // Extract month from MMYY
        MethodSpec extractMonth = MethodSpec.methodBuilder("extractExpiryMonth")
            .addModifiers(Modifier.DEFAULT)
            .addAnnotation(Named.class)
            .addParameter(String.class, "expiry")
            .returns(String.class)
            .addStatement("if (expiry == null || expiry.length() < 2) return null")
            .addStatement("return expiry.substring(0, 2)")
            .build();
        
        interfaceBuilder.addMethod(extractMonth);
        
        // Extract year from MMYY
        MethodSpec extractYear = MethodSpec.methodBuilder("extractExpiryYear")
            .addModifiers(Modifier.DEFAULT)
            .addAnnotation(Named.class)
            .addParameter(String.class, "expiry")
            .returns(String.class)
            .addStatement("if (expiry == null || expiry.length() < 4) return null")
            .addStatement("return $S + expiry.substring(2, 4)", "20")
            .build();
        
        interfaceBuilder.addMethod(extractYear);
        
        // Format timestamp
        MethodSpec formatTimestamp = MethodSpec.methodBuilder("formatTimestamp")
            .addModifiers(Modifier.DEFAULT)
            .addAnnotation(Named.class)
            .addParameter(OffsetDateTime.class, "dateTime")
            .returns(String.class)
            .addStatement("if (dateTime == null) return null")
            .addStatement("return dateTime.format($T.ISO_OFFSET_DATE_TIME)",
                DateTimeFormatter.class)
            .build();
        
        interfaceBuilder.addMethod(formatTimestamp);
    }
    
    private void addCardMappingMethods(TypeSpec.Builder interfaceBuilder) {
        // Mask card number (PCI compliance)
        MethodSpec maskCard = MethodSpec.methodBuilder("maskCardNumber")
            .addModifiers(Modifier.DEFAULT)
            .addAnnotation(Named.class)
            .addParameter(String.class, "cardNumber")
            .returns(String.class)
            .addStatement("// Never pass full PAN - this should already be tokenized")
            .addStatement("return null")
            .addJavadoc("PCI Compliance: Never include full card number.\n")
            .addJavadoc("Card should be tokenized before reaching this point.\n")
            .build();
        
        interfaceBuilder.addMethod(maskCard);
        
        // Get last 4 digits
        MethodSpec getLast4 = MethodSpec.methodBuilder("getLastFourDigits")
            .addModifiers(Modifier.DEFAULT)
            .addAnnotation(Named.class)
            .addParameter(String.class, "cardNumber")
            .returns(String.class)
            .addStatement("if (cardNumber == null || cardNumber.length() < 4) return null")
            .addStatement("return cardNumber.substring(cardNumber.length() - 4)")
            .build();
        
        interfaceBuilder.addMethod(getLast4);
    }
    
    private void addStatusMappingMethods(TypeSpec.Builder interfaceBuilder) {
        // Map Elavon status to Converge result
        MethodSpec mapStatus = MethodSpec.methodBuilder("mapStatusToResult")
            .addModifiers(Modifier.DEFAULT)
            .addAnnotation(Named.class)
            .addParameter(String.class, "status")
            .returns(String.class)
            .beginControlFlow("return switch (status)")
            .addStatement("case $S -> $S", "APPROVED", "APPROVAL")
            .addStatement("case $S -> $S", "DECLINED", "DECLINE")
            .addStatement("case $S -> $S", "PENDING", "PENDING")
            .addStatement("case $S -> $S", "CANCELLED", "VOID")
            .addStatement("case $S -> $S", "REFUNDED", "REFUND")
            .addStatement("default -> $S", "ERROR")
            .endControlFlow()
            .build();
        
        interfaceBuilder.addMethod(mapStatus);
        
        // Map Converge result to Elavon status
        MethodSpec mapResult = MethodSpec.methodBuilder("mapResultToStatus")
            .addModifiers(Modifier.DEFAULT)
            .addAnnotation(Named.class)
            .addParameter(String.class, "result")
            .returns(String.class)
            .beginControlFlow("return switch (result)")
            .addStatement("case $S -> $S", "APPROVAL", "APPROVED")
            .addStatement("case $S -> $S", "DECLINE", "DECLINED")
            .addStatement("case $S -> $S", "PENDING", "PENDING")
            .addStatement("case $S -> $S", "VOID", "CANCELLED")
            .addStatement("case $S -> $S", "REFUND", "REFUNDED")
            .addStatement("default -> $S", "FAILED")
            .endControlFlow()
            .build();
        
        interfaceBuilder.addMethod(mapResult);
    }
}
