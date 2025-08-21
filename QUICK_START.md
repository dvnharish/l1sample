# Quick Start Guide - Elavon Codegen MCP

## 🚀 Get Started in 5 Minutes

### Option 1: Local Development (Recommended for testing)

1. **Build and run**:
   ```bash
   chmod +x build-and-run.sh
   ./build-and-run.sh
   ```

2. **Add your OpenAPI specs**:
   ```bash
   # Copy your specs to the specs directory
   cp /path/to/your/elavon-openapi.json engine/src/main/resources/specs/
   cp /path/to/your/converge-openapi.json engine/src/main/resources/specs/  # Optional for upgrade mode
   ```

3. **Start the server**:
   ```bash
   ./run-mcp.sh
   ```

4. **Test the server**:
   ```bash
   curl http://localhost:8080/actuator/health
   ```

### Option 2: Docker (Recommended for production)

1. **Using Docker Compose**:
   ```bash
   # Set your API key
   export ELAVON_API_KEY=your-actual-api-key
   
   # Place your specs in ./specs/ directory
   mkdir -p specs
   cp your-elavon-spec.json specs/elavon-openapi.json
   
   # Start with Docker Compose
   docker-compose up -d
   
   # Check logs
   docker-compose logs -f elavon-mcp
   ```

2. **Direct Docker**:
   ```bash
   # Build image
   docker build -t elavon-mcp .
   
   # Run container
   docker run -d \
     --name elavon-mcp \
     -p 8080:8080 \
     -e ELAVON_API_KEY=your-api-key \
     -v $(pwd)/specs:/app/specs:ro \
     elavon-mcp
   ```

## 🛠️ Using the MCP Tool

### Basic Usage Examples

#### 1. Dry Run - Preview Changes
```bash
curl -X POST http://localhost:8080/mcp/tools/elavonCodegen \
  -H "Content-Type: application/json" \
  -d '{
    "mode": "create",
    "scope": "all",
    "projectRoot": "/path/to/your/spring/project",
    "dryRun": true
  }'
```

#### 2. Generate Specific Tags
```bash
curl -X POST http://localhost:8080/mcp/tools/elavonCodegen \
  -H "Content-Type: application/json" \
  -d '{
    "mode": "create",
    "scope": "tags",
    "tags": ["Transactions", "Tokens"],
    "projectRoot": "/path/to/your/project",
    "dryRun": false
  }'
```

#### 3. Upgrade from Converge to Elavon
```bash
curl -X POST http://localhost:8080/mcp/tools/elavonCodegen \
  -H "Content-Type: application/json" \
  -d '{
    "mode": "upgrade",
    "scope": "all",
    "convergeSpecPath": "./specs/converge-openapi.json",
    "elavonSpecPath": "./specs/elavon-openapi.json",
    "projectRoot": "/path/to/your/project",
    "backupBranch": "backup/migration-2024",
    "dryRun": false
  }'
```

### Response Format
```json
{
  "status": "success",
  "mode": "create",
  "scope": "all",
  "basePackage": "com.yourcompany.payment",
  "operationMappings": [
    {
      "target": "processPayment",
      "tag": "Transactions"
    }
  ],
  "changes": {
    "created": [
      "src/main/java/com/yourcompany/payment/dto/elavon/transactions/PaymentRequest.java",
      "src/main/java/com/yourcompany/payment/client/elavon/TransactionsClient.java"
    ],
    "updated": [],
    "deleted": []
  },
  "testResults": {
    "processPayment": {
      "passed": true,
      "duration": 150
    }
  },
  "reportPath": "MCP_Generation_Report_create_all_20240101_120000.md"
}
```

## 🎯 What Gets Generated

### For Each API Tag (e.g., "Transactions"):

1. **DTOs** - `com.yourcompany.dto.elavon.transactions.*`
   - Request/Response models
   - Validation annotations
   - Jackson serialization

2. **WebClient** - `com.yourcompany.client.elavon.TransactionsClient`
   - Reactive HTTP client
   - Retry logic
   - Error handling

3. **Service** - `com.yourcompany.service.elavon.TransactionsService`
   - Business logic layer
   - Validation
   - Error mapping

4. **Controller** - `com.yourcompany.controller.elavon.TransactionsController`
   - REST endpoints
   - Request mapping
   - Response handling

5. **Mappers** (Upgrade mode only) - `com.yourcompany.mapper.elavon.transactions.*`
   - MapStruct mappers
   - Data transformation
   - Legacy compatibility

### Generated Directory Structure:
```
src/main/java/com/yourcompany/payment/
├── dto/
│   └── elavon/
│       ├── transactions/
│       │   ├── PaymentRequest.java
│       │   ├── PaymentResponse.java
│       │   └── TransactionDetails.java
│       └── tokens/
│           └── TokenRequest.java
├── client/
│   └── elavon/
│       ├── TransactionsClient.java
│       └── TokensClient.java
├── service/
│   └── elavon/
│       ├── TransactionsService.java
│       └── TokensService.java
├── controller/
│   └── elavon/
│       ├── TransactionsController.java
│       └── TokensController.java
└── mapper/
    └── elavon/
        └── transactions/
            └── PaymentMapper.java
```

## 🔧 Configuration

### Environment Variables
```bash
# Required
export ELAVON_BASE_URL=https://api.elavon.com
export ELAVON_API_KEY=your-api-key

# Optional
export ELAVON_API_SECRET=your-api-secret
export SERVER_PORT=8080
export DEFAULT_CURRENCY_CODE=USD
```

### Application Configuration
Create `config/application-local.yml`:
```yaml
elavon:
  base-url: https://api.sandbox.elavon.com
  client:
    timeout: PT30S
    max-retries: 3
  auth:
    api-key: ${ELAVON_API_KEY}

logging:
  level:
    com.elavon.codegen: DEBUG
```

## 📊 Monitoring

### Health Check
```bash
curl http://localhost:8080/actuator/health
```

### Metrics
```bash
curl http://localhost:8080/actuator/metrics
```

### Application Info
```bash
curl http://localhost:8080/actuator/info
```

## 🐛 Troubleshooting

### Common Issues

1. **"Package scanning failed"**
   - Ensure your target project has `@SpringBootApplication` annotation
   - Check that `src/main/java` exists with proper package structure

2. **"OpenAPI spec not found"**
   - Verify spec files are in the correct location
   - Check file permissions and format

3. **"Generation failed"**
   - Check server logs: `docker logs elavon-mcp`
   - Verify your OpenAPI spec is valid
   - Ensure target directory is writable

4. **Build Issues**
   - Check Java version: `java -version` (requires 17+)
   - Clean rebuild: `mvn clean compile`

### Getting Help

1. **Check logs**:
   ```bash
   # Docker
   docker logs elavon-mcp
   
   # Local
   tail -f logs/elavon-codegen-mcp.log
   ```

2. **Validate your OpenAPI spec**:
   ```bash
   # Online validator
   open https://editor.swagger.io/
   
   # CLI tool
   swagger-codegen validate -i engine/src/main/resources/specs/elavon-openapi.json
   ```

3. **Test with sample project**:
   ```bash
   # Create a minimal Spring Boot project for testing
   curl https://start.spring.io/starter.zip \
     -d dependencies=web \
     -d groupId=com.test \
     -d artifactId=test-project \
     -o test-project.zip
   ```

## 🚢 Production Deployment

For production deployment, see the detailed [DEPLOYMENT.md](DEPLOYMENT.md) guide which covers:

- Kubernetes deployment
- Cloud platform deployment (AWS, Azure, GCP)
- Security configuration
- Monitoring and logging
- Scaling considerations

## 📝 Generated Reports

After each code generation, you'll get a detailed report:

- **Markdown Report**: `MCP_Generation_Report_*.md`
- **JSON Summary**: `codegen-summary-*.json`
- **Package Detection**: `detected-packages.json`

The reports include:
- Configuration used
- OpenAPI spec details
- Detected packages
- Operation mappings
- File changes
- Test results
- Follow-up TODOs

## 🎉 Success!

If everything is working correctly, you should see:
- ✅ Server health check passes
- ✅ Generated code compiles
- ✅ Tests pass
- ✅ Report generated

Your Elavon Codegen MCP server is now ready to generate payment integration code!
