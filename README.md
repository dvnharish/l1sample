# Elavon Codegen MCP

A Model Context Protocol (MCP) server for generating and migrating payment processing code from Converge XML APIs to Elavon JSON APIs.

## Overview

This MCP server provides intelligent code generation capabilities for:
- **Upgrade Mode**: Migrate existing Converge XML APIs to Elavon JSON APIs
- **Create Mode**: Scaffold new Elavon JSON APIs without modifying legacy code

## Features

- 🔍 **Accurate Package Detection**: Automatically scans Spring Boot projects to detect base packages
- 📝 **Complete Code Generation**: Generates DTOs, clients, services, controllers, and mappers
- 🔄 **Smart Migration**: Refactors XML-based code to JSON with proper data transformations
- 🛡️ **PCI Compliance**: Built-in security features for sensitive payment data
- 📊 **Comprehensive Reports**: Detailed generation reports with test results
- ✅ **Full Test Coverage**: Generates fixtures and integration tests

## Prerequisites

- Java 17 or higher
- Maven 3.8+
- Spring Boot project structure
- Local OpenAPI specifications:
  - `./specs/converge-openapi.json` (Converge XML APIs)
  - `./specs/elavon-openapi.json` (Elavon JSON APIs)

## 📂 Project Structure

- `engine/src/main/resources/specs/`: Contains OpenAPI specifications.
  - `converge-openapi.json` (Converge XML APIs)
  - `elavon-openapi.json` (Elavon JSON APIs)
- `engine/`: The core code generation engine.
- `modelcontextprotocol/`: The MCP server for VS Code integration.
- `tests/`: Integration tests.
- `generated/`: Output directory for generated code.

## 🚀 Quick Start

The MCP server exposes a single MCP tool called `elavonCodegen` with the following parameters:

| Parameter          | Type         | Required | Default                           | Description                                   |
|--------------------|--------------|----------|-----------------------------------|-----------------------------------------------|
| `mode`             | `string`     | Yes      | `"upgrade"` or `"create"`        | `"upgrade"` or `"create"`                    |
| `scope`            | `string`     | No       | `"all"`                           | `"all"`, `"tags"`, or `"operations"`        |
| `tags`             | `string[]`   | No       | `["transactions"]`                | Tags to generate (for `TAGS` scope).          |
| `operations`       | `string[]`   | No       | `["createTransaction"]`           | Operations to generate (for `OPERATIONS` scope). |
| `convergeSpecPath` | `string`     | No       | `classpath:specs/converge-openapi.json` | Path to legacy Converge spec.                 |
| `elavonSpecPath`   | `string`     | No       | `classpath:specs/elavon-openapi.json`   | Path to modern Elavon spec.                   |
| `projectRoot`      | `string`     | No       | `.`                               | Root directory of the target project.         |
| `backupBranch`     | `string`     | No       | `mcp-backup/{timestamp}`          | Git branch for backups (in `UPGRADE` mode).   |
| `dryRun`           | `boolean`    | No       | `false`                           | If true, generates a report without changing files. |

#### via VS Code (`mcp-config.json`)

```json
{
    "mode": "CREATE",
    "scope": "OPERATIONS",
    "operations": ["getTransaction", "listTransactions"],
    "projectRoot": "D:/path/to/your/project",
    "elavonSpecPath": "classpath:specs/elavon-openapi.json",
    "dryRun": false
}
```

```json
{
    "mode": "UPGRADE",
    "scope": "TAGS",
    "tags": ["transactions", "customers"],
    "projectRoot": "D:/path/to/your/project",
    "convergeSpecPath": "classpath:specs/converge-openapi.json",
    "elavonSpecPath": "classpath:specs/elavon-openapi.json",
    "dryRun": false
}
```

## Generated Code Structure

### Package Organization

The tool generates code under the detected base package:

```
com.yourcompany.payment/
├── dto/
│   └── elavon/
│       ├── transactions/
│       ├── tokens/
│       └── merchants/
├── client/
│   └── elavon/
│       ├── TransactionsClient.java
│       ├── TokensClient.java
│       └── MerchantsClient.java
├── service/
│   └── elavon/
│       ├── TransactionsService.java
│       ├── TokensService.java
│       └── MerchantsService.java
├── controller/
│   └── elavon/
│       ├── TransactionsController.java
│       ├── TokensController.java
│       └── MerchantsController.java
└── mapper/
    └── elavon/
        └── transactions/
            └── ProcessPaymentMapper.java
```

### Generated Components

#### DTOs (Data Transfer Objects)
- Request/Response models with full validation
- Jackson annotations for JSON serialization
- Bean Validation constraints
- Proper handling of nested types and enums

#### WebClient Clients
- Reactive WebClient-based API clients
- Automatic retry with exponential backoff
- Proper error handling and logging
- Configuration via environment variables

#### Services
- Business logic orchestration
- Request validation
- Error mapping
- Transaction management

#### Controllers
- RESTful endpoints
- Request/Response mapping
- Validation handling
- OpenAPI documentation

#### Mappers (Upgrade Mode Only)
- MapStruct-based data transformation
- Legacy XML to modern JSON conversion
- Common transformations (amounts, dates, status codes)
- PCI-compliant data handling

## Data Transformations

### Common Mappings (Upgrade Mode)

| Converge (XML) | Elavon (JSON) | Notes |
|----------------|---------------|-------|
| amount | total.amount | Cents to decimal string |
| currency | total.currencyCode | ISO currency code |
| cardExpiry (MMYY) | expirationMonth, expirationYear | Split and expand year |
| cardNumber | card.number | Tokenized only |
| result | status | Status mapping |
| txnId | transactionId | Identifier mapping |
| timestamp | createdAt | ISO-8601 format |

## Security Features

- **PCI Compliance**: Never logs full card numbers or CVV
- **API Key Management**: Secure storage via environment variables
- **OAuth Support**: Optional OAuth2 authentication
- **Data Masking**: Automatic masking of sensitive fields
- **Audit Logging**: Comprehensive audit trail

## Testing

### Generated Tests
- Unit tests for all components
- Integration tests with MockWebServer/WireMock
- JSON schema validation
- Test fixtures for each operation

### Running Tests
```bash
# Run all tests
mvn test

# Run integration tests only
mvn test -Dtest=*IntegrationTest

# Run with coverage
mvn test jacoco:report
```

## Reports

After code generation, the tool produces:

1. **Markdown Report** (`MCP_Generation_Report_<mode>_<scope>_<timestamp>.md`)
   - Configuration summary
   - OpenAPI spec details
   - Package detection results
   - Operation mappings
   - File changes
   - Test results
   - Follow-up TODOs

2. **JSON Summary** (`codegen-summary-<timestamp>.json`)
   - Machine-readable summary
   - Statistics and metrics
   - Error details

3. **Package Detection** (`detected-packages.json`)
   - Detected package structure
   - Used for consistent code generation

## Troubleshooting

### Package Detection Fails
- Ensure your project follows standard Spring Boot structure
- Check for @SpringBootApplication annotation
- Verify src/main/java directory exists

### Generation Errors
- Check OpenAPI spec validity
- Ensure all $refs are resolvable
- Verify file permissions

### Test Failures
- Check API endpoint configuration
- Verify test fixtures match schemas
- Review generated report for details

## Development

### Building from Source
```bash
mvn clean install
```

### Running in Development
```bash
mvn spring-boot:run -pl modelcontextprotocol
```

### Code Style
The project uses Google Java Format. Run formatting:
```bash
mvn spotless:apply
```

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests
5. Run `mvn verify`
6. Submit a pull request

## License

This project is proprietary software. All rights reserved.

## Support

For issues and questions:
- Create an issue in the repository
- Contact the development team
- Review the generated reports for troubleshooting

---

Built with ❤️ for modern payment processing
