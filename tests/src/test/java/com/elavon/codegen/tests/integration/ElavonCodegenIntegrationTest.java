package com.elavon.codegen.tests.integration;

import com.elavon.codegen.engine.CodegenEngine;
import com.elavon.codegen.engine.config.CodegenConfig;
import com.elavon.codegen.engine.model.CodegenResult;
import com.elavon.codegen.mcp.ElavonCodegenMcpApplication;
import com.elavon.codegen.mcp.tool.ElavonCodegenTool;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for the Elavon Codegen MCP tool.
 */
@SpringBootTest(classes = ElavonCodegenMcpApplication.class)
@TestPropertySource(properties = {
    "elavon.base-url=https://api.sandbox.elavon.com",
    "logging.level.com.elavon.codegen=DEBUG"
})
class ElavonCodegenIntegrationTest {
    
    @Autowired
    private ElavonCodegenTool elavonCodegenTool;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @TempDir
    Path tempDir;
    
    @BeforeEach
    void setUp() throws IOException {
        // Create mock project structure
        Path srcMainJava = tempDir.resolve("src/main/java/com/example/payment");
        Files.createDirectories(srcMainJava);
        
        // Create a mock Spring Boot application class
        String appClass = """
            package com.example.payment;
            
            import org.springframework.boot.SpringApplication;
            import org.springframework.boot.autoconfigure.SpringBootApplication;
            
            @SpringBootApplication
            public class PaymentApplication {
                public static void main(String[] args) {
                    SpringApplication.run(PaymentApplication.class, args);
                }
            }
            """;
        
        Files.writeString(srcMainJava.resolve("PaymentApplication.java"), appClass);
        
        // Create some existing controllers
        Path controllerDir = srcMainJava.resolve("controller");
        Files.createDirectories(controllerDir);
        
        String legacyController = """
            package com.example.payment.controller;
            
            import org.springframework.web.bind.annotation.*;
            
            @RestController
            @RequestMapping("/api/v1/payments")
            public class PaymentController {
                
                @PostMapping(value = "/processxml.do", 
                    consumes = "application/x-www-form-urlencoded")
                public String processPayment(@RequestParam("xmldata") String xmlData) {
                    // Legacy XML processing
                    return processXml(xmlData);
                }
                
                private String processXml(String xml) {
                    // XML processing logic
                    return "<response>OK</response>";
                }
            }
            """;
        
        Files.writeString(controllerDir.resolve("PaymentController.java"), legacyController);
    }
    
    @Test
    void testCreateMode_AllScope() {
        // Prepare request
        Map<String, Object> args = Map.of(
            "mode", "create",
            "scope", "all",
            "elavonSpecPath", "src/test/resources/specs/elavon-test-spec.json",
            "projectRoot", tempDir.toString(),
            "dryRun", false
        );
        
        // Execute
        var response = elavonCodegenTool.execute(args);
        
        // Verify
        assertThat(response.getStatus()).isEqualTo("success");
        assertThat(response.getMode()).isEqualTo("create");
        assertThat(response.getScope()).isEqualTo("all");
        assertThat(response.getBasePackage()).isEqualTo("com.example.payment");
        assertThat(response.getChanges().getCreated()).isNotEmpty();
    }
    
    @Test
    void testUpgradeMode_TagsScope() {
        // Prepare request
        Map<String, Object> args = Map.of(
            "mode", "upgrade",
            "scope", "tags",
            "tags", new String[]{"Transactions"},
            "convergeSpecPath", "src/test/resources/specs/converge-test-spec.json",
            "elavonSpecPath", "src/test/resources/specs/elavon-test-spec.json",
            "projectRoot", tempDir.toString(),
            "dryRun", true
        );
        
        // Execute
        var response = elavonCodegenTool.execute(args);
        
        // Verify
        assertThat(response.getStatus()).isEqualTo("success");
        assertThat(response.getMode()).isEqualTo("upgrade");
        assertThat(response.getScope()).isEqualTo("tags");
        assertThat(response.getOperationMappings()).isNotEmpty();
        
        // In dry run, no actual files should be created
        assertThat(response.getChanges().getCreated()).isEmpty();
    }
    
    @Test
    void testPackageDetection_MissingSpringBootApp() throws IOException {
        // Create project without @SpringBootApplication
        Path badProject = tempDir.resolve("bad-project");
        Path srcMainJava = badProject.resolve("src/main/java/com/example");
        Files.createDirectories(srcMainJava);
        
        // Just a regular class
        String regularClass = """
            package com.example;
            
            public class RegularClass {
                public void doSomething() {
                    System.out.println("Not a Spring Boot app");
                }
            }
            """;
        
        Files.writeString(srcMainJava.resolve("RegularClass.java"), regularClass);
        
        // Prepare request
        Map<String, Object> args = Map.of(
            "mode", "create",
            "scope", "all",
            "elavonSpecPath", "src/test/resources/specs/elavon-test-spec.json",
            "projectRoot", badProject.toString()
        );
        
        // Execute
        var response = elavonCodegenTool.execute(args);
        
        // Verify - should succeed by finding a common root package
        assertThat(response.getStatus()).isEqualTo("success");
        assertThat(response.getBasePackage()).isEqualTo("com");
    }
    
    @Test
    void testDryRunMode() {
        // Prepare request with dry run
        Map<String, Object> args = Map.of(
            "mode", "create",
            "scope", "operations",
            "operations", new String[]{"processPayment", "getTransaction"},
            "elavonSpecPath", "src/test/resources/specs/elavon-test-spec.json",
            "projectRoot", tempDir.toString(),
            "dryRun", true
        );
        
        // Execute
        var response = elavonCodegenTool.execute(args);
        
        // Verify
        assertThat(response.getStatus()).isEqualTo("success");
        assertThat(response.getReportPath()).isNotNull();
        
        // Check that report was NOT generated
        Path reportPath = Path.of(response.getReportPath());
        assertThat(Files.exists(reportPath)).isFalse();
    }
}
