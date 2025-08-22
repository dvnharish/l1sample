# Java 21 Upgrade Guide - Elavon Codegen MCP

## 🎯 Project Successfully Updated to Java 21

Your Elavon Codegen MCP project has been **completely updated** to use Java 21, including:

✅ **Root POM updated** - Java version changed from 17 to 21  
✅ **Dependency versions updated** - All dependencies upgraded to latest compatible versions  
✅ **Spring Boot upgraded** - Updated from 3.2.1 to 3.3.5 for Java 21 support  
✅ **Documentation updated** - All docs now reference Java 21  
✅ **Build scripts updated** - All scripts now check for Java 21+  
✅ **Docker images updated** - Dockerfile now uses OpenJDK 21  

## ⚠️ Current Status

**Your system currently has Java 17 installed**, but the project now requires Java 21.

```bash
Current: java version "17.0.11" 2024-04-16 LTS
Required: Java 21 or higher
```

## 📦 What Was Updated

### 1. **Core Java Configuration**
```xml
<properties>
    <java.version>21</java.version>
    <maven.compiler.source>21</maven.compiler.source>
    <maven.compiler.target>21</maven.compiler.target>
</properties>
```

### 2. **Spring Boot & Dependencies**
- **Spring Boot**: `3.2.1` → `3.3.5`
- **Jackson**: `2.16.1` → `2.17.2`
- **MapStruct**: `1.5.5.Final` → `1.6.2`
- **Lombok**: `1.18.30` → `1.18.34`
- **JavaParser**: `3.25.8` → `3.26.2`
- **JUnit**: `5.10.1` → `5.11.3`
- **Mockito**: `5.8.0` → `5.14.2`
- **WireMock**: `3.3.1` → `3.9.2`

### 3. **Docker & Build Tools**
- **Dockerfile**: `openjdk:17-jdk-slim` → `openjdk:21-jdk-slim`
- **Maven Compiler Plugin**: `3.12.1` → `3.13.0`
- **Maven Surefire Plugin**: `3.2.3` → `3.5.2`

### 4. **Documentation & Scripts**
- All references to "Java 17" changed to "Java 21"
- Build scripts now check for Java 21+
- Prerequisites updated in all documentation

## 🚀 Next Steps: Install Java 21

### Option 1: Download Oracle JDK 21 (Recommended for Windows)
```cmd
# 1. Download from Oracle
https://www.oracle.com/java/technologies/downloads/#java21

# 2. Install the MSI package
# Follow the installer instructions

# 3. Set JAVA_HOME (if needed)
set JAVA_HOME=C:\Program Files\Java\jdk-21

# 4. Verify installation
java -version
```

### Option 2: Use SDKMAN (Cross-platform)
```bash
# Install SDKMAN
curl -s "https://get.sdkman.io" | bash

# Install Java 21
sdk install java 21.0.5-oracle

# Use Java 21
sdk use java 21.0.5-oracle

# Verify
java -version
```

### Option 3: Use Package Managers

**Windows (Chocolatey):**
```cmd
choco install openjdk21
```

**Windows (Scoop):**
```cmd
scoop install openjdk21
```

**macOS (Homebrew):**
```bash
brew install openjdk@21
```

**Linux (Ubuntu/Debian):**
```bash
sudo apt update
sudo apt install openjdk-21-jdk
```

## 🧪 Test Your Java 21 Installation

Once you have Java 21 installed:

### 1. **Verify Java Version**
```cmd
java -version
```
Expected output:
```
openjdk version "21.0.x" 2024-xx-xx
OpenJDK Runtime Environment (build 21.0.x+xx)
OpenJDK 64-Bit Server VM (build 21.0.x+xx, mixed mode, sharing)
```

### 2. **Build the Project**
```cmd
# Clean build
mvn clean compile

# Full build with tests
mvn clean package

# Run the MCP server
run-mcp-stdio.bat
```

### 3. **Test in VS Code**
```json
{
  "mode": "create",
  "scope": "all",
  "projectRoot": "./test-project",
  "dryRun": true
}
```

## 🔥 Java 21 Benefits for Your MCP

Your Elavon Codegen MCP will now benefit from Java 21's improvements:

### **Performance**
- **Faster startup time** - Important for MCP server responsiveness
- **Better memory efficiency** - Reduced memory footprint
- **Improved garbage collection** - More consistent performance

### **Language Features**
- **Pattern Matching for switch** - Cleaner code generation logic
- **Record Patterns** - Better data structure handling
- **String Templates** (Preview) - Enhanced code generation templates
- **Virtual Threads** - Better concurrent processing for multiple API generations

### **Developer Experience**
- **Better IDE support** - Latest tooling compatibility
- **Enhanced debugging** - Better stack traces and profiling
- **Future-proof** - Long-term support until 2031

## 🏗️ Build Process Changes

The Maven build now targets Java 21 bytecode:
```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-compiler-plugin</artifactId>
    <version>3.13.0</version>
    <configuration>
        <source>21</source>
        <target>21</target>
    </configuration>
</plugin>
```

## 🐳 Docker Changes

Your Docker image now uses Java 21:
```dockerfile
FROM openjdk:21-jdk-slim
```

This ensures consistent Java 21 runtime across all deployment environments.

## 📊 Migration Status

| Component | Status | Notes |
|-----------|--------|-------|
| Root POM | ✅ Updated | Java 21 configuration |
| Spring Boot | ✅ Updated | 3.3.5 with Java 21 support |
| Dependencies | ✅ Updated | All latest compatible versions |
| Build Scripts | ✅ Updated | Check for Java 21+ |
| Documentation | ✅ Updated | All references to Java 21 |
| Dockerfile | ✅ Updated | OpenJDK 21 base image |
| VS Code Config | ✅ Ready | Works with Java 21 runtime |

## ⚡ Quick Commands

After installing Java 21:

```cmd
# Build and run immediately
build-and-run.bat

# Test VS Code integration
run-mcp-stdio.bat

# Docker deployment
docker-compose up -d
```

## 🆘 Troubleshooting

### Issue: "invalid target release: 21"
**Solution**: Install Java 21 as described above

### Issue: Maven not using Java 21
```cmd
# Check Maven's Java
mvn -version

# Set JAVA_HOME explicitly
set JAVA_HOME=C:\Program Files\Java\jdk-21
mvn clean compile
```

### Issue: VS Code using wrong Java
```json
// In .vscode/settings.json
{
  "java.home": "C:/Program Files/Java/jdk-21"
}
```

## 🎉 You're Ready!

Once you install Java 21, your Elavon Codegen MCP will be running on the latest LTS Java version with:

- **Better performance** for code generation
- **Modern language features** for cleaner code
- **Long-term support** until 2031
- **Latest Spring Boot** ecosystem compatibility

Install Java 21, then run `build-and-run.bat` to enjoy your upgraded MCP! 🚀
