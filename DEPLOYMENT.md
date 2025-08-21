# Elavon Codegen MCP Deployment Guide

## Prerequisites

- Java 17 or higher
- Maven 3.8+
- OpenAPI specifications in `./specs/` directory
- Spring Boot project structure for target projects

## 1. Local Development Deployment

### Build the MCP Server
```bash
# Fix remaining compilation issues first, then:
mvn clean install -DskipTests

# Or build individual modules
mvn clean compile -pl modelcontextprotocol
```

### Run Locally
```bash
# Method 1: Using Maven
cd modelcontextprotocol
mvn spring-boot:run

# Method 2: Using JAR
java -jar modelcontextprotocol/target/modelcontextprotocol-1.0.0-SNAPSHOT.jar

# Method 3: With custom configuration
java -jar modelcontextprotocol/target/modelcontextprotocol-1.0.0-SNAPSHOT.jar \
  --server.port=8080 \
  --elavon.base-url=https://api.sandbox.elavon.com
```

## 2. Docker Deployment

### Create Dockerfile
```dockerfile
FROM openjdk:17-jdk-slim

# Copy source code
COPY . /app

# The specs are already included in the engine jar
# COPY engine/src/main/resources/specs/ /app/specs/

# Set working directory
WORKDIR /app

# Copy the JAR file
COPY modelcontextprotocol/target/modelcontextprotocol-1.0.0-SNAPSHOT.jar app.jar

# Expose port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
```

### Build and Run Docker Container
```bash
# Build the application first
mvn clean package -DskipTests

# Build Docker image
docker build -t elavon-codegen-mcp:latest .

# Run container
docker run -d \
  --name elavon-mcp \
  -p 8080:8080 \
  -e ELAVON_BASE_URL=https://api.sandbox.elavon.com \
  -e ELAVON_API_KEY=your-api-key \
  -v $(pwd)/specs:/app/specs:ro \
  elavon-codegen-mcp:latest
```

## 3. Kubernetes Deployment

### ConfigMap for Specs
```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: elavon-specs
data:
  converge-openapi.json: |
    # Paste your Converge OpenAPI spec here
  elavon-openapi.json: |
    # Paste your Elavon OpenAPI spec here
```

### Secret for API Keys
```yaml
apiVersion: v1
kind: Secret
metadata:
  name: elavon-credentials
type: Opaque
stringData:
  api-key: "your-elavon-api-key"
  api-secret: "your-elavon-api-secret"
```

### Deployment
```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: elavon-codegen-mcp
spec:
  replicas: 3
  selector:
    matchLabels:
      app: elavon-codegen-mcp
  template:
    metadata:
      labels:
        app: elavon-codegen-mcp
    spec:
      containers:
      - name: mcp-server
        image: elavon-codegen-mcp:latest
        ports:
        - containerPort: 8080
        env:
        - name: ELAVON_BASE_URL
          value: "https://api.elavon.com"
        - name: ELAVON_API_KEY
          valueFrom:
            secretKeyRef:
              name: elavon-credentials
              key: api-key
        - name: ELAVON_API_SECRET
          valueFrom:
            secretKeyRef:
              name: elavon-credentials
              key: api-secret
        volumeMounts:
        - name: specs
          mountPath: /app/specs
          readOnly: true
        livenessProbe:
          httpGet:
            path: /actuator/health
            port: 8080
          initialDelaySeconds: 30
          periodSeconds: 30
        readinessProbe:
          httpGet:
            path: /actuator/health
            port: 8080
          initialDelaySeconds: 15
          periodSeconds: 5
      volumes:
      - name: specs
        configMap:
          name: elavon-specs
---
apiVersion: v1
kind: Service
metadata:
  name: elavon-codegen-mcp-service
spec:
  selector:
    app: elavon-codegen-mcp
  ports:
  - protocol: TCP
    port: 80
    targetPort: 8080
  type: ClusterIP
```

## 4. Cloud Deployment

### AWS ECS/Fargate
1. Push Docker image to ECR
2. Create ECS task definition
3. Deploy as ECS service
4. Use AWS Secrets Manager for credentials

### Azure Container Instances
```bash
az container create \
  --resource-group myResourceGroup \
  --name elavon-mcp \
  --image elavon-codegen-mcp:latest \
  --cpu 2 \
  --memory 4 \
  --ports 8080 \
  --environment-variables \
    ELAVON_BASE_URL=https://api.elavon.com \
  --secure-environment-variables \
    ELAVON_API_KEY=your-api-key
```

### Google Cloud Run
```bash
# Deploy to Cloud Run
gcloud run deploy elavon-codegen-mcp \
  --image elavon-codegen-mcp:latest \
  --platform managed \
  --region us-central1 \
  --allow-unauthenticated \
  --port 8080 \
  --set-env-vars ELAVON_BASE_URL=https://api.elavon.com \
  --set-secrets ELAVON_API_KEY=elavon-api-key:latest
```

## 5. Using the MCP Server

### MCP Tool Registration
Once deployed, your MCP server exposes the `elavonCodegen` tool:

```json
{
  "name": "elavonCodegen",
  "description": "Generate and migrate Elavon payment APIs",
  "schema": {
    "type": "object",
    "required": ["mode"],
    "properties": {
      "mode": {
        "type": "string",
        "enum": ["upgrade", "create"]
      },
      "scope": {
        "type": "string", 
        "enum": ["all", "tags", "operations"],
        "default": "all"
      }
    }
  }
}
```

### Example Usage

#### 1. Upgrade All APIs (CLI)
```bash
curl -X POST http://localhost:8080/mcp/tools/elavonCodegen \
  -H "Content-Type: application/json" \
  -d '{
    "mode": "upgrade",
    "scope": "all",
    "projectRoot": "/path/to/your/project",
    "dryRun": false
  }'
```

#### 2. Create Specific Tags (Programmatic)
```javascript
const mcpClient = new MCPClient('http://localhost:8080');

const result = await mcpClient.callTool('elavonCodegen', {
  mode: 'create',
  scope: 'tags',
  tags: ['Transactions', 'Tokens'],
  projectRoot: './my-payment-app',
  dryRun: true
});

console.log('Generated files:', result.changes.created);
```

#### 3. Integration with IDEs/Editors
```json
// VS Code MCP Configuration
{
  "mcp.servers": {
    "elavon-codegen": {
      "url": "http://localhost:8080",
      "tools": ["elavonCodegen"]
    }
  }
}
```

## 6. Configuration

### Environment Variables
```bash
# Required
ELAVON_BASE_URL=https://api.elavon.com
ELAVON_API_KEY=your-api-key

# Optional
ELAVON_API_SECRET=your-api-secret
ELAVON_CLIENT_TIMEOUT=PT30S
ELAVON_CLIENT_MAX_RETRIES=3
SERVER_PORT=8080
DEFAULT_CURRENCY_CODE=USD
```

### Application Properties
Create `application-prod.yml`:
```yaml
elavon:
  base-url: https://api.elavon.com
  client:
    timeout: PT30S
    max-retries: 3
  auth:
    api-key: ${ELAVON_API_KEY}
    
logging:
  level:
    com.elavon.codegen: INFO
    
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics
```

## 7. Monitoring & Operations

### Health Checks
```bash
# Health endpoint
curl http://localhost:8080/actuator/health

# Metrics
curl http://localhost:8080/actuator/metrics

# Info
curl http://localhost:8080/actuator/info
```

### Logs
```bash
# View logs
docker logs elavon-mcp

# Follow logs  
docker logs -f elavon-mcp

# Kubernetes logs
kubectl logs -f deployment/elavon-codegen-mcp
```

### Scaling
```bash
# Docker Swarm
docker service scale elavon-mcp=3

# Kubernetes
kubectl scale deployment elavon-codegen-mcp --replicas=5

# Auto-scaling (Kubernetes)
kubectl autoscale deployment elavon-codegen-mcp --cpu-percent=80 --min=2 --max=10
```

## 8. Security

### Production Security Checklist
- [ ] Use HTTPS for all communications
- [ ] Store API keys in secure secret management
- [ ] Enable authentication for MCP endpoints
- [ ] Configure proper CORS settings
- [ ] Use non-root container users
- [ ] Regular security updates
- [ ] Monitor for suspicious activity

### Sample Security Configuration
```yaml
# application-prod.yml
server:
  ssl:
    enabled: true
    key-store: classpath:keystore.p12
    key-store-password: ${SSL_KEYSTORE_PASSWORD}
    
spring:
  security:
    oauth2:
      client:
        registration:
          elavon:
            client-id: ${OAUTH_CLIENT_ID}
            client-secret: ${OAUTH_CLIENT_SECRET}
```

## 9. Troubleshooting

### Common Issues

1. **Build Failures**
   ```bash
   # Check Java version
   java -version
   
   # Clean and rebuild
   mvn clean compile -X
   ```

2. **Connection Issues**
   ```bash
   # Test connectivity
   curl -v http://localhost:8080/actuator/health
   
   # Check port binding
   netstat -tlnp | grep 8080
   ```

3. **OpenAPI Spec Issues**
   ```bash
   # Validate specs
   swagger-codegen validate -i specs/elavon-openapi.json
   ```

4. **Memory Issues**
   ```bash
   # Increase JVM memory
   java -Xmx2g -Xms1g -jar app.jar
   ```

## 10. Development Workflow

### Local Development
```bash
# 1. Start MCP server
mvn spring-boot:run -pl modelcontextprotocol

# 2. Test with sample project
cd /path/to/test-project
curl -X POST http://localhost:8080/mcp/tools/elavonCodegen \
  -d '{"mode":"create","scope":"all","dryRun":true}'

# 3. Review generated report
open MCP_Generation_Report_*.md
```

### CI/CD Pipeline
```yaml
# GitHub Actions example
name: Deploy MCP Server
on:
  push:
    branches: [main]

jobs:
  deploy:
    runs-on: ubuntu-latest
    steps:
    - uses: actions/checkout@v2
    - name: Setup Java
      uses: actions/setup-java@v2
      with:
        java-version: '17'
    - name: Build
      run: mvn clean package -DskipTests
    - name: Build Docker
      run: docker build -t ${{ secrets.REGISTRY }}/elavon-mcp:${{ github.sha }} .
    - name: Deploy
      run: docker push ${{ secrets.REGISTRY }}/elavon-mcp:${{ github.sha }}
```

This deployment guide covers all the major deployment scenarios for your Elavon Codegen MCP server. Choose the deployment method that best fits your infrastructure and requirements.
