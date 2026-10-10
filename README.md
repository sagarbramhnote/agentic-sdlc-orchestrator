# Agentic SDLC Orchestrator

A production-grade system that transforms requirements into reviewable engineering outcomes using an agentic execution model. Demonstrates requirement understanding, task decomposition, multi-step execution, and output generation/validation with controlled autonomy.

## Overview

This system implements an intelligent SDLC orchestration engine that coordinates the full software development lifecycle across requirements analysis, architecture design, code synthesis, testing, and release readiness. It features non-linear, stateful execution with governance, explicit dependency graphs, human approval checkpoints, and comprehensive reliability metrics tracking.

## Features

- **Requirement Understanding**: Interprets intent, identifies ambiguity, normalizes into clear engineering specifications
- **Task Decomposition**: Converts high-level requirements into actionable tasks with dependencies and sequencing
- **Codebase Reasoning**: Analyzes existing code for brownfield scenarios with impact analysis
- **Advanced Orchestration**: 
  - Explicit dependency graph with entry/exit gates
  - Parallel execution paths with synchronization
  - Cross-stage context preservation
  - Decision lineage tracking
  - Human approval checkpoints
  - Bounded retries, fallback, rollback, and safe-stop controls
- **Policy Guardrails**: Security, compliance, and change control enforcement
- **Audit-Grade Observability**: Complete traceability and audit trail
- **Reliability Metrics**: Success rate, retry/rollback frequency, MTTR, end-to-end latency
- **Dynamic Re-planning**: Adapts to upstream changes while maintaining governance
- **Controlled Autonomy**: Agents execute within defined boundaries with human oversight

## Prerequisites

- Java 17 or higher
- Maven 3.6+ or Gradle 7+
- Spring Boot 3.x
- Spring AI (for ChatClient integration)

## Setup Instructions

### 1. Clone the Repository

```bash
git clone <repository-url>
cd agentic-sdlc-orchestrator
```

### 2. Configure Dependencies

Add the following to your `pom.xml` (Maven):

```xml
<dependencies>
    <!-- Spring Boot -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
    
    <!-- Spring AI -->
    <dependency>
        <groupId>org.springframework.ai</groupId>
        <artifactId>spring-ai-openai-spring-boot-starter</artifactId>
        <version>1.0.0-M4</version>
    </dependency>
    
    <!-- Lombok -->
    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
        <optional>true</optional>
    </dependency>
    
    <!-- Testing -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-test</artifactId>
        <scope>test</scope>
    </dependency>
</dependencies>
```

### 3. Configure Application Properties

Create `src/main/resources/application.yml`:

```yaml
spring:
  ai:
    openai:
      api-key: ${OPENAI_API_KEY}
      chat:
        options:
          model: gpt-4
          temperature: 0.7

server:
  port: 8080
```

Set your OpenAI API key as an environment variable:

```bash
export OPENAI_API_KEY=your-api-key-here
```

### 4. Build the Project

```bash
./mvnw clean install
```

Or with Gradle:

```bash
./gradlew clean build
```

### 5. Run the Application

```bash
./mvnw spring-boot:run
```

Or with Gradle:

```bash
./gradlew bootRun
```

The application will start on `http://localhost:8080`

## Usage

### Starting a New SDLC Run

```bash
curl -X POST http://localhost:8080/api/orchestrator/run \
  -H "Content-Type: application/json" \
  -d '{
    "requirement": "Build a high-performance URL shortener with analytics",
    "scenario": "GREENFIELD"
  }'
```

Response:

```json
{
  "runId": "550e8400-e29b-41d4-a716-446655440000",
  "scenario": "GREENFIELD",
  "currentStage": "REQUIREMENTS_ANALYSIS",
  "rawRequirement": "Build a high-performance URL shortener with analytics",
  "normalizedRequirement": "...",
  "taskBreakdown": ["1. Define API contracts", "2. Implement Base62 encoding", "..."],
  "architecturePlan": "...",
  "generatedCodeArtifacts": {},
  "testPassed": false,
  "retryCount": 0,
  "humanApproved": false,
  "auditTrail": [...]
}
```

### Checking Execution Status

```bash
curl http://localhost:8080/api/orchestrator/status/{runId}
```

### Approving Release Checkpoint

```bash
curl -X POST http://localhost:8080/api/orchestrator/approve/{runId}
```

### Getting Reliability Metrics

```bash
curl http://localhost:8080/api/orchestrator/metrics/{runId}
```

## Scenarios

### Greenfield Scenario

New system development from scratch. The system:
- Analyzes requirements from scratch
- Designs new architecture
- Generates all code artifacts
- Creates comprehensive test suites

Example:

```bash
curl -X POST http://localhost:8080/api/orchestrator/run \
  -H "Content-Type: application/json" \
  -d '{
    "requirement": "Build a URL shortener service with base62 encoding and analytics",
    "scenario": "GREENFIELD"
  }'
```

### Brownfield Scenario

Enhancement to existing system. The system:
- Analyzes existing codebase
- Identifies impacted modules and APIs
- Performs impact analysis
- Generates targeted changes

Example:

```bash
curl -X POST http://localhost:8080/api/orchestrator/run \
  -H "Content-Type: application/json" \
  -d '{
    "requirement": "Add rate limiting to existing URL shortener",
    "scenario": "BROWNFIELD"
  }'
```

### Ambiguous Scenario

Requirements with unclear specifications. The system:
- Identifies ambiguities
- Requests clarification
- Normalizes requirements
- Makes reasonable assumptions with documentation

Example:

```bash
curl -X POST http://localhost:8080/api/orchestrator/run \
  -H "Content-Type: application/json" \
  -d '{
    "requirement": "Build a URL shortener etc",
    "scenario": "AMBIGUOUS"
  }'
```

## Architecture

See [ARCHITECTURE.md](ARCHITECTURE.md) for detailed architecture documentation.

## Testing

See [TESTING.md](TESTING.md) for testing approach and instructions.

## API Endpoints

### POST /api/orchestrator/run
Initialize a new SDLC run with given requirement and scenario.

**Request Body:**
```json
{
  "requirement": "string",
  "scenario": "GREENFIELD | BROWNFIELD | AMBIGUOUS"
}
```

**Response:** ExecutionContext object

### GET /api/orchestrator/status/{runId}
Get current execution status for a run.

**Response:** ExecutionContext object

### POST /api/orchestrator/approve/{runId}
Approve the release checkpoint for a run.

**Response:** ExecutionContext object

### GET /api/orchestrator/metrics/{runId}
Get reliability metrics for a run.

**Response:** Metrics object with execution times, retry counts, etc.

## Policy Guardrails

The system enforces the following policies:

- **Security**: No dynamic code execution, no hardcoded credentials
- **Compliance**: Proper logging framework, human approval required
- **Quality**: No empty catch blocks, proper error handling
- **Architecture**: Error handling, security, and scalability considerations required
- **Requirements**: Sufficient detail, no ambiguous terms

See [ARCHITECTURE.md](ARCHITECTURE.md) for complete policy definitions.

## Reliability Metrics

The system tracks:

- Total execution time
- Stage-specific latency
- Retry and rollback counts
- End-to-end latency
- Stage execution counts
- Audit trail size

These metrics enable calculation of success rate, MTTR, and performance analysis.

## Limitations

1. Single JVM execution (not distributed)
2. In-memory context storage (not persisted)
3. Fixed thread pool size (not auto-scaling)
4. Simple policy rules (not ML-based)
5. Basic code quality checks (not full static analysis)
6. No external CI/CD integration
7. Limited to Java code generation

## Troubleshooting

### Java Compiler Not Available

If you see "Java compiler not available in current environment", ensure you're running with a JDK (not JRE):

```bash
java -version  # Should show "OpenJDK Runtime Environment" or similar
```

### OpenAI API Key Not Set

Ensure the `OPENAI_API_KEY` environment variable is set:

```bash
echo $OPENAI_API_KEY
```

### Port Already in Use

Change the port in `application.yml`:

```yaml
server:
  port: 8081
```

## Contributing

This is a prototype system for demonstration purposes. Contributions should focus on:

- Enhancing policy guardrails
- Adding more sophisticated code analysis
- Improving brownfield scenario handling
- Expanding language support beyond Java
- Adding distributed execution capabilities

## License

Internal use only - Schwab Confidential

## Support

For questions or issues, contact the development team.
