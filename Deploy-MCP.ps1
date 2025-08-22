# Elavon Codegen MCP - PowerShell Deployment Script

param(
    [Parameter(Mandatory=$false)]
    [ValidateSet("local", "docker", "kubernetes")]
    [string]$DeploymentType = "local",
    
    [Parameter(Mandatory=$false)]
    [string]$ElavonApiKey = "",
    
    [Parameter(Mandatory=$false)]
    [string]$ServerPort = "8080"
)

Write-Host "🚀 Deploying Elavon Codegen MCP Server..." -ForegroundColor Green

# Function to check prerequisites
function Test-Prerequisites {
    Write-Host "Checking prerequisites..." -ForegroundColor Yellow
    
    if (-not (Get-Command java -ErrorAction SilentlyContinue)) {
        Write-Host "❌ Java 21+ is required but not found" -ForegroundColor Red
        return $false
    }
    
    if (-not (Get-Command mvn -ErrorAction SilentlyContinue)) {
        Write-Host "❌ Maven is required but not found" -ForegroundColor Red
        return $false
    }
    
    Write-Host "✅ Prerequisites check passed" -ForegroundColor Green
    return $true
}

# Function for local deployment
function Deploy-Local {
    Write-Host "🔨 Building project for local deployment..." -ForegroundColor Yellow
    
    # Build the project
    $buildResult = & mvn clean package -DskipTests
    if ($LASTEXITCODE -ne 0) {
        Write-Host "❌ Build failed" -ForegroundColor Red
        return $false
    }
    
    # Setup environment
    $env:ELAVON_BASE_URL = "https://api.sandbox.elavon.com"
    $env:SERVER_PORT = $ServerPort
    if ($ElavonApiKey) {
        $env:ELAVON_API_KEY = $ElavonApiKey
    }
    
    # Create specs directory
    if (-not (Test-Path "specs")) {
        New-Item -ItemType Directory -Path "specs" | Out-Null
        Write-Host "📁 Created specs directory" -ForegroundColor Green
    }
    
    Write-Host "🌐 Server will be available at: http://localhost:$ServerPort" -ForegroundColor Cyan
    Write-Host "📊 Health check: http://localhost:$ServerPort/actuator/health" -ForegroundColor Cyan
    
    # Start the server
    $jarPath = "modelcontextprotocol\target\modelcontextprotocol-1.0.0-SNAPSHOT.jar"
    if (Test-Path $jarPath) {
        Write-Host "Starting MCP server..." -ForegroundColor Yellow
        & java -jar $jarPath --server.port=$ServerPort --logging.level.com.elavon.codegen=DEBUG
    } else {
        Write-Host "❌ JAR file not found: $jarPath" -ForegroundColor Red
        return $false
    }
    
    return $true
}

# Function for Docker deployment
function Deploy-Docker {
    Write-Host "🐳 Building Docker image..." -ForegroundColor Yellow
    
    if (-not (Get-Command docker -ErrorAction SilentlyContinue)) {
        Write-Host "❌ Docker is required but not found" -ForegroundColor Red
        return $false
    }
    
    # Build Docker image
    $buildResult = & docker build -t elavon-codegen-mcp:latest .
    if ($LASTEXITCODE -ne 0) {
        Write-Host "❌ Docker build failed" -ForegroundColor Red
        return $false
    }
    
    # Setup environment variables
    $envVars = @()
    $envVars += "-e", "ELAVON_BASE_URL=https://api.sandbox.elavon.com"
    $envVars += "-e", "SERVER_PORT=$ServerPort"
    if ($ElavonApiKey) {
        $envVars += "-e", "ELAVON_API_KEY=$ElavonApiKey"
    }
    
    # Create specs directory if it doesn't exist
    if (-not (Test-Path "specs")) {
        New-Item -ItemType Directory -Path "specs" | Out-Null
    }
    
    Write-Host "🌐 Server will be available at: http://localhost:$ServerPort" -ForegroundColor Cyan
    
    # Run container
    $dockerArgs = @(
        "run", "-d",
        "--name", "elavon-mcp",
        "-p", "$ServerPort`:8080",
        "-v", "$PWD\specs:/app/specs:ro"
    ) + $envVars + @("elavon-codegen-mcp:latest")
    
    Write-Host "Starting Docker container..." -ForegroundColor Yellow
    & docker @dockerArgs
    
    if ($LASTEXITCODE -eq 0) {
        Write-Host "✅ Container started successfully!" -ForegroundColor Green
        Write-Host "View logs with: docker logs -f elavon-mcp" -ForegroundColor Cyan
    } else {
        Write-Host "❌ Failed to start container" -ForegroundColor Red
        return $false
    }
    
    return $true
}

# Function for Kubernetes deployment
function Deploy-Kubernetes {
    Write-Host "☸️ Deploying to Kubernetes..." -ForegroundColor Yellow
    
    if (-not (Get-Command kubectl -ErrorAction SilentlyContinue)) {
        Write-Host "❌ kubectl is required but not found" -ForegroundColor Red
        return $false
    }
    
    # Create namespace
    & kubectl create namespace elavon-mcp --dry-run=client -o yaml | kubectl apply -f -
    
    # Create ConfigMap for specs
    if (Test-Path "specs\elavon-openapi.json") {
        & kubectl create configmap elavon-specs --from-file=specs\ -n elavon-mcp --dry-run=client -o yaml | kubectl apply -f -
        Write-Host "✅ ConfigMap created for OpenAPI specs" -ForegroundColor Green
    }
    
    # Create Secret for API key
    if ($ElavonApiKey) {
        & kubectl create secret generic elavon-credentials --from-literal=api-key=$ElavonApiKey -n elavon-mcp --dry-run=client -o yaml | kubectl apply -f -
        Write-Host "✅ Secret created for API credentials" -ForegroundColor Green
    }
    
    # Apply Kubernetes manifests
    $manifestPath = "k8s\deployment.yaml"
    if (Test-Path $manifestPath) {
        & kubectl apply -f $manifestPath -n elavon-mcp
        Write-Host "✅ Kubernetes deployment applied" -ForegroundColor Green
    } else {
        Write-Host "⚠️ Kubernetes manifests not found at $manifestPath" -ForegroundColor Yellow
        Write-Host "Please create Kubernetes deployment files or see DEPLOYMENT.md" -ForegroundColor Yellow
    }
    
    return $true
}

# Function to test deployment
function Test-Deployment {
    param([string]$Url = "http://localhost:$ServerPort")
    
    Write-Host "🧪 Testing deployment..." -ForegroundColor Yellow
    
    $maxAttempts = 30
    $attempt = 0
    
    do {
        $attempt++
        try {
            $response = Invoke-RestMethod -Uri "$Url/actuator/health" -Method Get -TimeoutSec 5
            if ($response.status -eq "UP") {
                Write-Host "✅ Health check passed!" -ForegroundColor Green
                Write-Host "Server is ready at: $Url" -ForegroundColor Cyan
                return $true
            }
        } catch {
            Write-Host "Attempt $attempt/$maxAttempts - Server not ready yet..." -ForegroundColor Yellow
            Start-Sleep -Seconds 2
        }
    } while ($attempt -lt $maxAttempts)
    
    Write-Host "❌ Health check failed after $maxAttempts attempts" -ForegroundColor Red
    return $false
}

# Main execution
try {
    if (-not (Test-Prerequisites)) {
        exit 1
    }
    
    $success = $false
    
    switch ($DeploymentType) {
        "local" { 
            $success = Deploy-Local 
        }
        "docker" { 
            $success = Deploy-Docker
            if ($success) {
                Start-Sleep -Seconds 10  # Wait for container to start
                $success = Test-Deployment
            }
        }
        "kubernetes" { 
            $success = Deploy-Kubernetes 
        }
    }
    
    if ($success) {
        Write-Host ""
        Write-Host "🎉 Deployment completed successfully!" -ForegroundColor Green
        Write-Host ""
        Write-Host "📋 Next steps:" -ForegroundColor Cyan
        Write-Host "1. Test the health endpoint:" -ForegroundColor White
        Write-Host "   curl http://localhost:$ServerPort/actuator/health" -ForegroundColor Gray
        Write-Host ""
        Write-Host "2. Use the MCP tool:" -ForegroundColor White
        Write-Host "   curl -X POST http://localhost:$ServerPort/mcp/tools/elavonCodegen \" -ForegroundColor Gray
        Write-Host "     -H 'Content-Type: application/json' \" -ForegroundColor Gray
        Write-Host "     -d '{\"mode\":\"create\",\"scope\":\"all\",\"dryRun\":true}'" -ForegroundColor Gray
        Write-Host ""
        Write-Host "📖 For more examples, see QUICK_START.md" -ForegroundColor Cyan
    } else {
        Write-Host "❌ Deployment failed" -ForegroundColor Red
        exit 1
    }
    
} catch {
    Write-Host "❌ Deployment failed with error: $($_.Exception.Message)" -ForegroundColor Red
    exit 1
}
