package com.elavon.codegen.engine.generator;

import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.TypeSpec;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import com.elavon.codegen.engine.config.CodegenConfig;

/**
 * Base class for all code generators.
 * Provides common functionality for generating Java source files.
 */
@Slf4j
public abstract class BaseGenerator {
    
    /**
     * Write a generated Java file to disk.
     * 
     * @param packageName The package name
     * @param typeSpec The type specification
     * @param outputDir The output directory (usually src/main/java)
     * @return The path to the generated file
     */
    protected String writeJavaFile(String packageName, TypeSpec typeSpec, String outputDir, boolean dryRun) {
        JavaFile javaFile = JavaFile.builder(packageName, typeSpec)
            .skipJavaLangImports(true)
            .indent("    ") // 4 spaces
            .build();
        
        Path outputPath = Paths.get(outputDir);
        String filePath = outputPath
            .resolve(packageName.replace('.', File.separatorChar))
            .resolve(typeSpec.name + ".java")
            .toString();

        if (dryRun) {
            log.info("[Dry Run] Would generate: {}", filePath);
            return filePath;
        }

        try {
            javaFile.writeTo(outputPath);
            log.debug("Generated: {}", filePath);
            return filePath;
        } catch (IOException e) {
            throw new GeneratorException("Failed to write Java file: " + typeSpec.name, e);
        }
    }
    
    /**
     * Write a raw source file to disk.
     * 
     * @param filePath The full file path
     * @param content The file content
     * @return The path to the generated file
     */
    protected String writeRawFile(String filePath, String content, boolean dryRun) {
        if (dryRun) {
            log.info("[Dry Run] Would generate: {}", filePath);
            return filePath;
        }
        try {
            File file = new File(filePath);
            FileUtils.writeStringToFile(file, content, StandardCharsets.UTF_8);
            log.debug("Generated: {}", filePath);
            return filePath;
        } catch (IOException e) {
            throw new GeneratorException("Failed to write file: " + filePath, e);
        }
    }
    
    /**
     * Convert a tag or operation name to a valid Java class name.
     * Examples:
     * - "transactions" -> "Transactions"
     * - "payment-methods" -> "PaymentMethods"
     * - "3ds-authentication" -> "ThreeDsAuthentication"
     */
    protected String toClassName(String name) {
        if (StringUtils.isBlank(name)) {
            return "Default";
        }
        
        // Handle special cases
        name = name.replaceAll("3ds", "ThreeDs");
        name = name.replaceAll("2fa", "TwoFa");
        
        // Convert kebab-case or snake_case to PascalCase
        String[] parts = name.split("[-_\\s]+");
        StringBuilder result = new StringBuilder();
        
        for (String part : parts) {
            if (!part.isEmpty()) {
                result.append(Character.toUpperCase(part.charAt(0)));
                if (part.length() > 1) {
                    result.append(part.substring(1).toLowerCase());
                }
            }
        }
        
        return result.toString();
    }
    
    /**
     * Convert a string to camelCase.
     * Examples:
     * - "payment-method" -> "paymentMethod"
     * - "TRANSACTION_ID" -> "transactionId"
     */
    protected String toCamelCase(String name) {
        if (StringUtils.isBlank(name)) {
            return "";
        }
        
        String className = toClassName(name);
        if (className.isEmpty()) {
            return "";
        }
        
        return Character.toLowerCase(className.charAt(0)) + className.substring(1);
    }
    
    /**
     * Convert a string to a valid package name component.
     * Examples:
     * - "Payment Methods" -> "paymentmethods"
     * - "3DS-Auth" -> "threedsauth"
     */
    protected String toPackageName(String name) {
        if (StringUtils.isBlank(name)) {
            return "default";
        }
        
        return name.toLowerCase()
            .replaceAll("3ds", "threeds")
            .replaceAll("2fa", "twofa")
            .replaceAll("[^a-z0-9]", "");
    }
    
    /**
     * Get the output directory for generated code.
     * Default is "generated/src/main/java".
     */
    protected String getOutputDir() {
        return "generated/src/main/java";
    }
    
    /**
     * Sanitize a string for use in generated code comments.
     */
    protected String sanitizeComment(String text) {
        if (text == null) return "";
        
        return text
            .replace("*/", "*\\/")
            .replace("\r\n", "\n")
            .replace("\r", "\n")
            .trim();
    }
}
