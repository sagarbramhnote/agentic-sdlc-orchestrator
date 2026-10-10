package com.sdlc.agentic.demos;

import com.sdlc.agentic.orchestrator.ExecutionContext;
import com.sdlc.agentic.orchestrator.SdlcOrchestratorEngine;
import com.sdlc.agentic.orchestrator.SdlcStage;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;

/**
 * Demonstrates the three key scenarios: Greenfield, Brownfield, and Ambiguous requirements.
 * Each scenario shows how the agentic system handles different requirement types.
 */
@Component
public class ScenarioDemonstrations {

    private final SdlcOrchestratorEngine orchestratorEngine;

    public ScenarioDemonstrations(SdlcOrchestratorEngine orchestratorEngine) {
        this.orchestratorEngine = orchestratorEngine;
    }

    /**
     * Scenario 1: Greenfield - Building a new URL shortener service from scratch
     * 
     * This scenario demonstrates:
     * - Requirement understanding and normalization
     * - Task decomposition for new system
     * - Architecture design from scratch
     * - Complete code generation
     * - Quality gate validation
     * - Human approval checkpoint
     * - Reliability metrics collection
     */
    public ScenarioResult runGreenfieldScenario() throws IOException {
        String requirement = """
            Build a high-performance URL shortener service with the following features:
            - Base62 encoding for short codes (6-7 characters)
            - In-memory storage using ConcurrentHashMap for fast access
            - Analytics tracking click counts per short code
            - REST API endpoints: POST /api/v1/shorten, GET /{shortCode}, GET /api/v1/analytics/{shortCode}
            - Rate limiting: 100 requests per minute per IP address
            - Error handling for invalid URLs and non-existent codes
            - TTL support: URLs expire after 30 days
            """;

        System.out.println("=== GREENFIELD SCENARIO ===");
        System.out.println("Requirement: " + requirement);
        System.out.println("Starting SDLC orchestration...\n");

        ExecutionContext ctx = orchestratorEngine.initializeRun(requirement, "GREENFIELD");

        // Wait for execution to reach approval checkpoint
        waitForStage(ctx.getRunId(), SdlcStage.AWAITING_APPROVAL);

        ExecutionContext finalCtx = orchestratorEngine.getContext(ctx.getRunId());
        Map<String, Object> metrics = orchestratorEngine.getReliabilityMetrics(ctx.getRunId());

        System.out.println("\n=== GREENFIELD RESULTS ===");
        System.out.println("Current Stage: " + finalCtx.getCurrentStage());
        System.out.println("Normalized Requirement: " + finalCtx.getNormalizedRequirement());
        System.out.println("Task Breakdown: " + finalCtx.getTaskBreakdown());
        System.out.println("Generated Artifacts: " + finalCtx.getGeneratedCodeArtifacts().keySet());
        System.out.println("Audit Trail Size: " + finalCtx.getAuditTrail().size());
        System.out.println("Total Execution Time: " + metrics.get("totalExecutionTimeMs") + "ms");
        System.out.println("Stage Latencies: " + metrics.get("stageLatencyMs"));

        return new ScenarioResult(
            "GREENFIELD",
            finalCtx.getCurrentStage(),
            finalCtx.getGeneratedCodeArtifacts().keySet().size(),
            finalCtx.getAuditTrail().size(),
            (Long) metrics.get("totalExecutionTimeMs"),
            finalCtx.getTaskBreakdown()
        );
    }

    /**
     * Scenario 2: Brownfield - Enhancing existing URL shortener with rate limiting
     * 
     * This scenario demonstrates:
     * - Codebase analysis and impact assessment
     * - Identification of affected modules and APIs
     * - Architecture understanding of existing system
     * - Targeted code generation for enhancements
     * - Integration testing with existing code
     * - Risk assessment for changes
     */
    public ScenarioResult runBrownfieldScenario() throws IOException {
        String requirement = """
            Add rate limiting to the existing URL shortener service:
            - Implement token bucket algorithm
            - 100 requests per minute per IP address
            - Return 429 Too Many Requests when limit exceeded
            - Include rate limit headers in response (X-RateLimit-Remaining, X-RateLimit-Reset)
            - Support configuration for rate limit values
            - Add metrics for rate limit violations
            """;

        System.out.println("=== BROWNFIELD SCENARIO ===");
        System.out.println("Requirement: " + requirement);
        System.out.println("Starting SDLC orchestration with codebase analysis...\n");

        ExecutionContext ctx = orchestratorEngine.initializeRun(requirement, "BROWNFIELD");

        // Wait for execution to reach approval checkpoint
        waitForStage(ctx.getRunId(), SdlcStage.AWAITING_APPROVAL);

        ExecutionContext finalCtx = orchestratorEngine.getContext(ctx.getRunId());
        Map<String, Object> metrics = orchestratorEngine.getReliabilityMetrics(ctx.getRunId());

        System.out.println("\n=== BROWNFIELD RESULTS ===");
        System.out.println("Current Stage: " + finalCtx.getCurrentStage());
        
        // Check for impact analysis output
        if (finalCtx.getGeneratedCodeArtifacts().containsKey("impact_analysis.json")) {
            System.out.println("Impact Analysis: Generated");
        }
        if (finalCtx.getGeneratedCodeArtifacts().containsKey("architecture_understanding.json")) {
            System.out.println("Architecture Understanding: Generated");
        }
        
        System.out.println("Generated Artifacts: " + finalCtx.getGeneratedCodeArtifacts().keySet());
        System.out.println("Audit Trail Size: " + finalCtx.getAuditTrail().size());
        System.out.println("Total Execution Time: " + metrics.get("totalExecutionTimeMs") + "ms");
        System.out.println("Stage Execution Counts: " + metrics.get("stageExecutionCounts"));

        return new ScenarioResult(
            "BROWNFIELD",
            finalCtx.getCurrentStage(),
            finalCtx.getGeneratedCodeArtifacts().keySet().size(),
            finalCtx.getAuditTrail().size(),
            (Long) metrics.get("totalExecutionTimeMs"),
            finalCtx.getTaskBreakdown()
        );
    }

    /**
     * Scenario 3: Ambiguous - Handling vague requirements
     * 
     * This scenario demonstrates:
     * - Ambiguity detection in requirements
     * - Policy violation identification
     * - Intelligent assumption making
     * - Documentation of assumptions
     * - Clarification requests (simulated)
     * - Working implementation despite ambiguity
     */
    public ScenarioResult runAmbiguousScenario() throws IOException {
        String requirement = """
            Build a URL shortener etc with some features and stuff for handling links
            """;

        System.out.println("=== AMBIGUOUS SCENARIO ===");
        System.out.println("Requirement: " + requirement);
        System.out.println("Starting SDLC orchestration with ambiguity handling...\n");

        ExecutionContext ctx = orchestratorEngine.initializeRun(requirement, "AMBIGUOUS");

        // Wait for execution to reach approval checkpoint
        waitForStage(ctx.getRunId(), SdlcStage.AWAITING_APPROVAL);

        ExecutionContext finalCtx = orchestratorEngine.getContext(ctx.getRunId());
        Map<String, Object> metrics = orchestratorEngine.getReliabilityMetrics(ctx.getRunId());

        System.out.println("\n=== AMBIGUOUS RESULTS ===");
        System.out.println("Current Stage: " + finalCtx.getCurrentStage());
        System.out.println("Normalized Requirement: " + finalCtx.getNormalizedRequirement());
        
        // Check for policy violations
        long policyViolations = finalCtx.getAuditTrail().stream()
            .filter(a -> a.agentName().equals("PolicyEngine") && !a.passedGate())
            .count();
        System.out.println("Policy Violations Detected: " + policyViolations);
        
        System.out.println("Generated Artifacts: " + finalCtx.getGeneratedCodeArtifacts().keySet());
        System.out.println("Audit Trail Size: " + finalCtx.getAuditTrail().size());
        System.out.println("Total Execution Time: " + metrics.get("totalExecutionTimeMs") + "ms");

        return new ScenarioResult(
            "AMBIGUOUS",
            finalCtx.getCurrentStage(),
            finalCtx.getGeneratedCodeArtifacts().keySet().size(),
            finalCtx.getAuditTrail().size(),
            (Long) metrics.get("totalExecutionTimeMs"),
            finalCtx.getTaskBreakdown()
        );
    }

    /**
     * Demonstrates human approval workflow
     */
    public void demonstrateApprovalWorkflow(String runId) {
        System.out.println("=== APPROVAL WORKFLOW DEMONSTRATION ===");
        
        ExecutionContext ctx = orchestratorEngine.getContext(runId);
        System.out.println("Current Stage: " + ctx.getCurrentStage());
        System.out.println("Human Approved: " + ctx.isHumanApproved());
        System.out.println("Test Passed: " + ctx.isTestPassed());
        
        // Review audit trail before approval
        System.out.println("\nAudit Trail Summary:");
        ctx.getAuditTrail().forEach(audit -> 
            System.out.println("  - " + audit.agentName() + ": " + audit.action() + 
                             " [" + (audit.passedGate() ? "PASS" : "FAIL") + "]")
        );
        
        // Approve the release
        System.out.println("\nApproving release...");
        orchestratorEngine.approveRelease(runId);
        
        ExecutionContext approvedCtx = orchestratorEngine.getContext(runId);
        System.out.println("New Stage: " + approvedCtx.getCurrentStage());
        System.out.println("Release Status: " + (approvedCtx.getCurrentStage() == SdlcStage.RELEASED ? "RELEASED" : "PENDING"));
    }

    /**
     * Demonstrates reliability metrics retrieval
     */
    public void demonstrateMetricsRetrieval(String runId) {
        System.out.println("=== RELIABILITY METRICS DEMONSTRATION ===");
        
        Map<String, Object> metrics = orchestratorEngine.getReliabilityMetrics(runId);
        
        System.out.println("Run ID: " + metrics.get("runId"));
        System.out.println("Current Stage: " + metrics.get("currentStage"));
        System.out.println("Total Execution Time: " + metrics.get("totalExecutionTimeMs") + "ms");
        System.out.println("Total Rollbacks: " + metrics.get("totalRollbacks"));
        System.out.println("Retry Count: " + metrics.get("retryCount"));
        System.out.println("Stage Execution Counts: " + metrics.get("stageExecutionCounts"));
        System.out.println("Stage Latencies (ms): " + metrics.get("stageLatencyMs"));
        System.out.println("Audit Trail Size: " + metrics.get("auditTrailSize"));
        
        if (metrics.containsKey("endToEndLatencyMs")) {
            System.out.println("End-to-End Latency: " + metrics.get("endToEndLatencyMs") + "ms");
        }
    }

    /**
     * Demonstrates dynamic re-planning
     */
    public void demonstrateReplanning(String runId) {
        System.out.println("=== DYNAMIC RE-PLANNING DEMONSTRATION ===");
        
        ExecutionContext ctx = orchestratorEngine.getContext(runId);
        System.out.println("Current Stage: " + ctx.getCurrentStage());
        System.out.println("Triggering re-plan due to upstream requirement change...");
        
        orchestratorEngine.triggerReplan(runId);
        
        ExecutionContext replannedCtx = orchestratorEngine.getContext(runId);
        System.out.println("Re-plan triggered. Check audit trail for details.");
        
        long replanEvents = replannedCtx.getAuditTrail().stream()
            .filter(a -> a.action().equals("REPLAN"))
            .count();
        System.out.println("Re-plan Events: " + replanEvents);
    }

    /**
     * Run all three scenarios sequentially
     */
    public void runAllScenarios() {
        try {
            System.out.println("\n" + "=".repeat(60));
            System.out.println("RUNNING ALL SCENARIO DEMONSTRATIONS");
            System.out.println("=".repeat(60) + "\n");

            ScenarioResult greenfield = runGreenfieldScenario();
            Thread.sleep(2000);

            ScenarioResult brownfield = runBrownfieldScenario();
            Thread.sleep(2000);

            ScenarioResult ambiguous = runAmbiguousScenario();

            System.out.println("\n" + "=".repeat(60));
            System.out.println("SCENARIO COMPARISON SUMMARY");
            System.out.println("=".repeat(60));
            System.out.printf("%-15s %-20s %-15s %-15s %-15s%n", 
                "Scenario", "Final Stage", "Artifacts", "Audit Records", "Execution Time");
            System.out.println("-".repeat(80));
            System.out.printf("%-15s %-20s %-15d %-15d %-15dms%n",
                greenfield.scenario(),
                greenfield.finalStage(),
                greenfield.artifactCount(),
                greenfield.auditSize(),
                greenfield.executionTimeMs());
            System.out.printf("%-15s %-20s %-15d %-15d %-15dms%n",
                brownfield.scenario(),
                brownfield.finalStage(),
                brownfield.artifactCount(),
                brownfield.auditSize(),
                brownfield.executionTimeMs());
            System.out.printf("%-15s %-20s %-15d %-15d %-15dms%n",
                ambiguous.scenario(),
                ambiguous.finalStage(),
                ambiguous.artifactCount(),
                ambiguous.auditSize(),
                ambiguous.executionTimeMs());

        } catch (IOException | InterruptedException e) {
            System.err.println("Error running scenarios: " + e.getMessage());
        }
    }

    private void waitForStage(String runId, SdlcStage targetStage) {
        int maxWaitSeconds = 60;
        int waited = 0;
        
        while (waited < maxWaitSeconds) {
            ExecutionContext ctx = orchestratorEngine.getContext(runId);
            if (ctx != null && ctx.getCurrentStage() == targetStage) {
                return;
            }
            try {
                Thread.sleep(1000);
                waited++;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
        System.out.println("Warning: Timeout waiting for stage " + targetStage);
    }

    public record ScenarioResult(
        String scenario,
        SdlcStage finalStage,
        int artifactCount,
        int auditSize,
        long executionTimeMs,
        java.util.List<String> taskBreakdown
    ) {}
}
