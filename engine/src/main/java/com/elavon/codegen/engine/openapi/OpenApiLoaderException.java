package com.elavon.codegen.engine.openapi;

/**
 * Exception thrown when OpenAPI loading fails.
 */
public class OpenApiLoaderException extends RuntimeException {
    
    public OpenApiLoaderException(String message) {
        super(message);
    }
    
    public OpenApiLoaderException(String message, Throwable cause) {
        super(message, cause);
    }
}
