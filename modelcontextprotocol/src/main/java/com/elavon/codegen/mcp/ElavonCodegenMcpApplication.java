package com.elavon.codegen.mcp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

/**
 * Main Spring Boot application for Elavon Codegen MCP Server.
 */
@SpringBootApplication
@ComponentScan(basePackages = {"com.elavon.codegen"})
public class ElavonCodegenMcpApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(ElavonCodegenMcpApplication.class, args);
    }
}
