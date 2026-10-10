# Testing Approach

## Overview

This document describes the testing approach for the Agentic SDLC Orchestrator system, including unit tests, integration tests, and scenario demonstrations.

## Testing Strategy

### 1. Unit Testing

Unit tests focus on individual components in isolation.

#### Test Coverage Areas

**Orchestration Components**
- `StageDependencyGraph` - Dependency resolution, cycle detection, parallel path identification
- `PolicyEngine` - Policy validation for code, architecture, requirements, and transitions
- `ExecutionContext` - State management, metrics tracking, audit trail

**Agents**
- `RequirementAnalystAgent` - Requirement normalization, task breakdown extraction
- `SoftwareArchitectAgent` - Architecture plan generation
- `CodeSynthesizerAgent` - Code generation, file writing
- `CodebaseAnalyzerAgent` - Impact analysis, architecture understanding
- `QualityGateAgent` - Artifact validation, quality gate enforcement

**Tools**
- `FileSystemTools` - Sandbox creation, file operations, rollback
- `BuildVerificationTools` - Syntax validation, compilation, code quality checks

#### Example Unit Test

```java
@SpringBootTest
class PolicyEngineTest {

    @Autowired
    private PolicyEngine policyEngine;

    @Test
    void testSecurityPolicyViolation() {
        String maliciousCode = "Runtime.getRuntime().exec(\"rm -rf /\")";
        ExecutionContext ctx = new ExecutionContext();
        
        List<PolicyViolation> violations = policyEngine.validateCodeGeneration(maliciousCode, ctx);
        
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getSeverity() == Severity.CRITICAL));
    }

    @Test
    void testArchitectureValidation() {
        String incompleteArch = "Simple service with no error handling";
        ExecutionContext ctx = new ExecutionContext();
        
        List<PolicyViolation> violations = policyEngine.validateArchitecture(incompleteArch, ctx);
        
        assertTrue(violations.stream().anyMatch(v -> v.getPolicyName().equals("ARCH-001")));
    }
}
```

### 2. Integration Testing

Integration tests verify the interaction between components.

#### Test Scenarios

**End-to-End SDLC Flow**
- Initialize run with requirement
- Execute through all stages
- Verify audit trail completeness
- Check metrics collection
- Validate final artifacts

**Policy Enforcement**
- Attempt blocked transitions
- Verify policy violations are caught
- Check audit records for policy events

**Parallel Execution**
- Run multiple concurrent stages
- Verify synchronization points
- Check thread safety of context

**Error Handling**
- Simulate agent failures
- Verify retry logic
- Check rollback execution
- Validate fallback behavior

#### Example Integration Test

```java
@SpringBootTest
@AutoConfigureMockMvc
class SdlcOrchestratorIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SdlcOrchestratorEngine engine;

    @Test
    void testGreenfieldScenarioEndToEnd() throws Exception {
        // Start a new run
        MvcResult result = mockMvc.perform(post("/api/orchestrator/run")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"requirement\":\"Build URL shortener\",\"scenario\":\"GREENFIELD\"}"))
            .andExpect(status().isOk())
            .andReturn();

        ExecutionContext ctx = objectMapper.readValue(
            result.getResponse().getContentAsString(), 
            ExecutionContext.class
        );

        // Wait for execution to complete
        await().atMost(30, TimeUnit.SECONDS)
            .until(() -> engine.getContext(ctx.getRunId()).getCurrentStage() == SdlcStage.AWAITING_APPROVAL);

        // Verify artifacts generated
        ExecutionContext finalCtx = engine.getContext(ctx.getRunId());
        assertTrue(finalCtx.getGeneratedCodeArtifacts().containsKey("UrlShortenerService.java"));
        assertTrue(finalCtx.getGeneratedCodeArtifacts().containsKey("UrlShortenerServiceTest.java"));

        // Verify audit trail
        assertFalse(finalCtx.getAuditTrail().isEmpty());
    }
}
```

### 3. Scenario Demonstrations

Three comprehensive scenarios demonstrate the system's capabilities.

#### Scenario 1: Greenfield - URL Shortener Service

**Objective**: Build a complete URL shortener service from scratch.

**Steps**:
1. Submit requirement: "Build a high-performance URL shortener with base62 encoding and analytics"
2. System analyzes requirement and normalizes specification
3. Architecture is designed with REST API contracts
4. Code is synthesized with service and test classes
5. Quality gates verify compilation and syntax
6. Human approves release
7. Metrics are collected throughout

**Expected Outcomes**:
- Complete service implementation
- Unit test suite
- Architecture documentation
- Audit trail with all stages
- Reliability metrics (latency, retries)

**Test Command**:
```bash
curl -X POST http://localhost:8080/api/orchestrator/run \
  -H "Content-Type: application/json" \
  -d '{
    "requirement": "Build a high-performance URL shortener with base62 encoding, analytics, and rate limiting",
    "scenario": "GREENFIELD"
  }'
```

#### Scenario 2: Brownfield - Add Rate Limiting

**Objective**: Enhance existing URL shortener with rate limiting.

**Steps**:
1. Submit requirement: "Add rate limiting to existing URL shortener service"
2. CodebaseAnalyzerAgent scans existing code
3. Impact analysis identifies affected modules
4. Architecture understanding identifies integration points
5. Code synthesizer generates rate limiting implementation
6. Quality gates verify integration
7. Human approves changes

**Expected Outcomes**:
- Impact analysis report
- Rate limiting implementation
- Integration tests
- Modified architecture documentation
- Audit trail showing brownfield-specific steps

**Test Command**:
```bash
curl -X POST http://localhost:8080/api/orchestrator/run \
  -H "Content-Type: application/json" \
  -d '{
    "requirement": "Add rate limiting to existing URL shortener service with 100 requests per minute per IP",
    "scenario": "BROWNFIELD"
  }'
```

#### Scenario 3: Ambiguous - Vague Requirements

**Objective**: Handle unclear requirements with intelligent clarification.

**Steps**:
1. Submit requirement: "Build a URL shortener etc"
2. RequirementAnalystAgent identifies ambiguities
3. Policy engine flags vague terms
4. System makes reasonable assumptions
5. Assumptions are documented in audit trail
6. Architecture is designed with documented assumptions
7. Code is generated based on assumptions

**Expected Outcomes**:
- Identified ambiguities in audit trail
- Documented assumptions
- Policy violation warnings
- Working implementation based on assumptions
- Clear documentation of what was assumed

**Test Command**:
```bash
curl -X POST http://localhost:8080/api/orchestrator/run \
  -H "Content-Type: application/json" \
  -d '{
    "requirement": "Build a URL shortener etc with some features",
    "scenario": "AMBIGUOUS"
  }'
```

### 4. Performance Testing

#### Metrics to Track

- **Stage Latency**: Time per stage execution
- **End-to-End Latency**: Total time from start to release
- **Throughput**: Number of concurrent runs supported
- **Memory Usage**: Heap consumption during execution
- **Thread Pool Utilization**: Executor service efficiency

#### Performance Test Example

```java
@Test
void testConcurrentExecutionPerformance() {
    int concurrentRuns = 10;
    CountDownLatch latch = new CountDownLatch(concurrentRuns);
    
    long startTime = System.currentTimeMillis();
    
    for (int i = 0; i < concurrentRuns; i++) {
        CompletableFuture.runAsync(() -> {
            try {
                engine.initializeRun("Build URL shortener", "GREENFIELD");
            } catch (IOException e) {
                fail("Execution failed: " + e.getMessage());
            } finally {
                latch.countDown();
            }
        });
    }
    
    latch.await(5, TimeUnit.MINUTES);
    long duration = System.currentTimeMillis() - startTime;
    
    // Verify all runs completed
    assertEquals(concurrentRuns, engine.getActiveContexts().size());
    
    // Verify acceptable performance
    assertTrue(duration < 300000, "Execution took too long: " + duration + "ms");
}
```

### 5. Security Testing

#### Test Cases

- **Code Injection**: Attempt to inject malicious code in requirements
- **Credential Hardcoding**: Verify detection of hardcoded secrets
- **Dynamic Execution**: Verify blocking of eval/Runtime.exec
- **Policy Bypass**: Attempt to bypass policy guardrails
- **Sandbox Escape**: Verify workspace isolation

#### Security Test Example

```java
@Test
void testMaliciousCodeDetection() {
    String maliciousRequirement = "Build a service that executes: Runtime.exec('rm -rf /')";
    ExecutionContext ctx = engine.initializeRun(maliciousRequirement, "GREENFIELD");
    
    // Verify policy violation detected
    Optional<AuditRecord> violation = ctx.getAuditTrail().stream()
        .filter(a -> a.getAction().equals("BLOCKED"))
        .findFirst();
    
    assertTrue(violation.isPresent());
    assertEquals(SdlcStage.INTAKE, ctx.getCurrentStage());
}
```

### 6. Reliability Testing

#### Test Scenarios

- **Retry Logic**: Force quality gate failure, verify retry
- **Rollback**: Trigger max retries, verify sandbox cleanup
- **Timeout**: Simulate long-running stage, verify timeout handling
- **Recovery**: Trigger failure, verify recovery path
- **Metrics Accuracy**: Verify metrics collection accuracy

#### Reliability Test Example

```java
@Test
void testRetryWithRollback() {
    // Inject failing code
    String failingCode = "class Broken { invalid syntax here }";
    
    ExecutionContext ctx = new ExecutionContext();
    ctx.getGeneratedCodeArtifacts().put("Broken.java", failingCode);
    
    // Execute quality gate (should fail)
    boolean passed = qualityGateAgent.verify(ctx);
    assertFalse(passed);
    
    // Verify retry count incremented
    assertTrue(ctx.getRetryCount() > 0);
    
    // After max retries, verify rollback
    if (ctx.getRetryCount() >= ctx.getMaxRetries()) {
        assertEquals(SdlcStage.FAILED, ctx.getCurrentStage());
        assertTrue(ctx.getTotalRollbacks() > 0);
    }
}
```

## Test Execution

### Run All Tests

```bash
./mvnw test
```

### Run Specific Test Class

```bash
./mvnw test -Dtest=PolicyEngineTest
```

### Run Integration Tests Only

```bash
./mvnw test -Dtest=*IntegrationTest
```

### Run with Coverage Report

```bash
./mvnw test jacoco:report
```

Coverage report will be in `target/site/jacoco/index.html`

## Test Data

### Sample Requirements

**Greenfield**:
```
Build a URL shortener service with:
- Base62 encoding for short codes
- In-memory storage with ConcurrentHashMap
- Analytics tracking click counts
- REST API for shorten and resolve operations
- Rate limiting (100 req/min per IP)
```

**Brownfield**:
```
Add TTL (time-to-live) support to existing URL shortener:
- URLs expire after 30 days
- Background cleanup job
- Configuration for TTL duration
```

**Ambiguous**:
```
Build a URL shortener etc with some features and stuff
```

## Limitations of Testing

1. **AI Dependency**: Tests requiring ChatClient may need mocked responses
2. **External Services**: Integration tests may require external API availability
3. **Resource Cleanup**: Sandbox cleanup may be incomplete in test failures
4. **Timing**: Async execution may cause flaky tests without proper waits
5. **Environment**: Tests may behave differently in CI/CD vs local

## Continuous Integration

### CI Pipeline Steps

1. **Build**: `./mvnw clean compile`
2. **Unit Tests**: `./mvnw test`
3. **Integration Tests**: `./mvnw verify`
4. **Coverage**: `./mvnw jacoco:report` (ensure >80% coverage)
5. **Security Scan**: Run dependency check
6. **Package**: `./mvnw package`

### GitHub Actions Example

```yaml
name: CI

on: [push, pull_request]

jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v2
      - name: Set up JDK 17
        uses: actions/setup-java@v2
        with:
          java-version: '17'
      - name: Run tests
        run: ./mvnw test
      - name: Generate coverage
        run: ./mvnw jacoco:report
```

## Test Maintenance

### When to Update Tests

- New agent added → Add unit and integration tests
- Policy changed → Update policy validation tests
- New stage added → Add orchestration tests
- Bug fix → Add regression test
- Performance regression → Update performance baselines

### Test Quality Guidelines

- Tests should be independent and isolated
- Use descriptive test names
- Mock external dependencies
- Verify behavior, not implementation
- Keep tests fast and focused
- Maintain high coverage (>80%)
