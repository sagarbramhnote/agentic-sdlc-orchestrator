# Architecture Overview

## System Overview

The Agentic SDLC Orchestrator is a production-grade system that transforms requirements into reviewable engineering outcomes using an agentic execution model. It demonstrates requirement understanding, task decomposition, multi-step execution, and output generation/validation with controlled autonomy.

## Core Components

### 1. Orchestration Engine

**SdlcOrchestratorEngine**
- Central coordination engine for the entire SDLC lifecycle
- Manages stage transitions based on dependency graph
- Supports parallel execution using CompletableFuture and ExecutorService
- Implements policy guardrails at each transition point
- Tracks reliability metrics (latency, retries, rollbacks)
- Supports dynamic re-planning when upstream outputs change
- Enforces human approval checkpoints for high-impact actions

**StageDependencyGraph**
- Explicit dependency graph representation of SDLC stages
- Defines entry/exit gates and stage dependencies
- Supports cycle detection for validation
- Enables parallel execution path identification
- Provides synchronization point management

**PolicyEngine**
- Enforces security, compliance, and change control policies
- Validates code generation against security best practices
- Checks architecture for required components (error handling, security, scalability)
- Validates stage transitions based on governance rules
- Provides policy violation reporting with severity levels

### 2. State Management

**ExecutionContext**
- Maintains complete execution context across all stages
- Tracks current stage, scenario type, and requirements
- Stores normalized requirements, task breakdown, and architecture plans
- Manages generated code artifacts
- Implements reliability metrics tracking:
  - Stage execution counts
  - Stage latency measurements
  - Total execution time
  - Retry and rollback counts
  - End-to-end latency
- Maintains audit-grade trail with AuditRecord

**SdlcStage**
- Enum defining all SDLC lifecycle stages:
  - INTAKE
  - REQUIREMENTS_ANALYSIS
  - ARCHITECTURE_DESIGN
  - CODE_SYNTHESIS
  - BUILD_AND_TEST
  - AWAITING_APPROVAL
  - RELEASED
  - FAILED
  - ROLLED_BACK

**AuditRecord**
- Immutable record of all agent actions
- Captures timestamp, stage, agent name, action, output summary, and gate pass/fail
- Provides complete traceability for audit requirements

### 3. Specialized Agents

**RequirementAnalystAgent**
- Interprets intent and identifies ambiguity in requirements
- Normalizes requirements into clear engineering specifications
- Performs task decomposition with dependencies and sequencing
- Uses Spring AI ChatClient for intelligent analysis

**SoftwareArchitectAgent**
- Formulates microservice design plans based on specifications
- Defines REST API contracts
- Designs in-memory storage/caching architecture
- Specifies error handling boundaries and encoding logic

**CodeSynthesizerAgent**
- Generates production-quality code artifacts
- Produces API/schema definitions
- Creates unit and integration tests
- Supports deterministic code generation for URL shortener service

**CodebaseAnalyzerAgent**
- Analyzes existing codebase for brownfield scenarios
- Identifies impacted modules, APIs, and data flows
- Demonstrates architectural understanding
- Performs impact analysis with risk assessment

**QualityGateAgent**
- Validates generated artifacts against quality standards
- Verifies code syntax and compilation
- Enforces test coverage requirements
- Implements bounded retry logic with fallback

### 4. Execution Tools

**FileSystemTools**
- Creates isolated sandbox workspaces for each run
- Manages file operations (create, write, delete)
- Implements safe rollback mechanisms
- Ensures workspace isolation between runs

**BuildVerificationTools**
- Performs actual Java compilation using javax.tools.JavaCompiler
- Validates code syntax with comprehensive checks
- Provides compilation error reporting
- Implements code quality checks (exception handling, hardcoded values, logging)
- Falls back to syntax validation when compiler unavailable

### 5. API Layer

**ApprovalController**
- REST API endpoints for orchestration control:
  - POST /api/orchestrator/run - Initialize new SDLC run
  - GET /api/orchestrator/status/{runId} - Get execution status
  - POST /api/orchestrator/approve/{runId} - Approve release checkpoint
- Provides human-in-the-loop interface for approvals
- Returns ExecutionContext with full state and metrics

## Orchestration Model

### Stage Flow

```
INTAKE → REQUIREMENTS_ANALYSIS → ARCHITECTURE_DESIGN → CODE_SYNTHESIS → BUILD_AND_TEST → AWAITING_APPROVAL → RELEASED
                                                                    ↓
                                                                  FAILED → ROLLED_BACK
```

### Dependency Graph

The StageDependencyGraph defines explicit dependencies:
- REQUIREMENTS_ANALYSIS depends on INTAKE
- ARCHITECTURE_DESIGN depends on REQUIREMENTS_ANALYSIS
- CODE_SYNTHESIS depends on ARCHITECTURE_DESIGN
- BUILD_AND_TEST depends on CODE_SYNTHESIS
- AWAITING_APPROVAL depends on BUILD_AND_TEST
- RELEASED depends on AWAITING_APPROVAL

### Parallel Execution

Stages that have no dependencies on each other can execute in parallel using the ExecutorService with a fixed thread pool (4 threads). This enables:
- Concurrent agent execution where possible
- Improved throughput for independent tasks
- Synchronization points at dependency boundaries

### Control Flow

1. **Initialization**: Create ExecutionContext, validate requirements against policies, create sandbox workspace
2. **Stage Execution**: For each stage:
   - Check policy guardrails for transition permission
   - Execute stage asynchronously with timeout (5 minutes)
   - Track stage latency and execution count
   - Update audit trail
3. **Quality Gates**: At BUILD_AND_TEST:
   - Verify code artifacts present
   - Compile generated code
   - Check code quality metrics
   - On failure: retry up to maxRetries (3) or rollback
4. **Human Checkpoint**: At AWAITING_APPROVAL:
   - Suspend execution for human review
   - Wait for approval via API
   - On approval: proceed to RELEASED
5. **Dynamic Re-planning**: Can trigger re-execution from current stage if upstream outputs change
6. **Metrics Collection**: Track all reliability metrics throughout execution

## Policy Guardrails

### Security Policies
- SEC-001: Block dynamic code execution (eval, Runtime.exec)
- SEC-002: Detect hardcoded credentials (password, secret, api_key)

### Compliance Policies
- COMPL-001: Require proper logging framework (no System.out)
- GOV-001: Require human approval before release
- GOV-002: Require passing quality gates before release

### Quality Policies
- QUAL-001: Block empty catch blocks
- ARCH-001: Require error handling strategy in architecture
- ARCH-002: Require security considerations in architecture
- ARCH-003: Require scalability considerations in architecture

### Requirement Policies
- REQ-001: Flag overly brief requirements
- REQ-002: Flag ambiguous terms (etc, and so on)

## Reliability Metrics

The system tracks the following metrics for observability:

- **totalExecutionTimeMs**: Cumulative time across all stages
- **totalRollbacks**: Number of rollback operations performed
- **retryCount**: Number of retry attempts for failed stages
- **stageExecutionCounts**: Map of stage → execution count
- **stageLatencyMs**: Map of stage → latency in milliseconds
- **endToEndLatencyMs**: Total time from start to release
- **auditTrailSize**: Number of audit records generated

These metrics enable calculation of:
- Success rate across runs
- MTTR (Mean Time To Recovery)
- Stage-specific performance bottlenecks
- Retry/rollback frequency analysis

## Key Design Decisions

1. **Dependency Graph over Linear Chain**: Enables parallel execution and non-linear workflows
2. **Policy-First Approach**: Guardrails embedded at every transition point
3. **Audit-Grade Traceability**: Complete audit trail for compliance requirements
4. **Sandbox Isolation**: Each run in isolated workspace for safety
5. **Bounded Autonomy**: Agents execute within defined boundaries with human oversight
6. **Retry with Rollback**: Safe failure handling with automatic cleanup
7. **Dynamic Re-planning**: Ability to adapt to changing requirements mid-execution
8. **Production-Grade Compilation**: Actual Java compiler integration, not just syntax checks

## Technology Stack

- **Framework**: Spring Boot 3.x
- **Language**: Java 17+
- **AI Integration**: Spring AI with ChatClient
- **Concurrency**: Java CompletableFuture, ExecutorService
- **Compilation**: javax.tools.JavaCompiler
- **Build Tool**: Maven/Gradle (for project management)
- **API**: Spring Web (REST controllers)
- **Data Structures**: ConcurrentHashMap for thread-safe collections

## Security Considerations

- All code generation validated against security policies
- No dynamic code execution in generated artifacts
- Credentials must use environment variables or vaults
- Sandbox isolation prevents file system conflicts
- Human approval required for high-impact actions
- Audit trail provides complete change history

## Scalability Considerations

- Thread pool limits concurrent executions (configurable)
- ConcurrentHashMap enables thread-safe context management
- Async execution prevents blocking on long-running stages
- Metrics collection has minimal performance impact
- Sandbox cleanup prevents resource leaks

## Limitations

1. Single JVM execution (no distributed orchestration)
2. In-memory context storage (not persisted across restarts)
3. Fixed thread pool size (not auto-scaling)
4. Simple policy rules (not ML-based anomaly detection)
5. Basic code quality checks (not full static analysis)
6. No integration with external CI/CD systems
7. Limited to Java code generation (not multi-language)
