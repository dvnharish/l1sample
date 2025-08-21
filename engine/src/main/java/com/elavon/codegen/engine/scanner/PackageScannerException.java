package com.elavon.codegen.engine.scanner;

/**
 * Exception thrown when package scanning fails.
 */
public class PackageScannerException extends RuntimeException {
    
    public PackageScannerException(String message) {
        super(message);
    }
    
    public PackageScannerException(String message, Throwable cause) {
        super(message, cause);
    }
}
