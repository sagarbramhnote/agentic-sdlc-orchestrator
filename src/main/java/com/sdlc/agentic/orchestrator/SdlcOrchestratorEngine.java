package com.sdlc.agentic.orchestrator;

import com.sdlc.agentic.agents.*;
import com.sdlc.agentic.tools.FileSystemTools;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;

@Service
public class SdlcOrchestratorEngine {

    private final RequirementAnalystAgent requirementAgent;
    private final SoftwareArchitectAgent architectAgent;
    private final CodeSynthesizerAgent codeAgent;
    private final CodebaseAnalyzerAgent codebaseAnalyzerAgent;
    private final QualityGateAgent qualityGateAgent;
    private final FileSystemTools fileTools;
    private final StageDependencyGraph dependencyGraph;
    private final PolicyEngine policyEngine;
    private final ExecutorService executorService;

    private final Map<String, ExecutionContext> activeContexts = new ConcurrentHashMap<>();

    public SdlcOrchestratorEngine(RequirementAnalystAgent requirementAgent,
                                  SoftwareArchitectAgent architectAgent,
                                  CodeSynthesizerAgent codeAgent,
                                  CodebaseAnalyzerAgent codebaseAnalyzerAgent,
                                  QualityGateAgent qualityGateAgent,
                                  FileSystemTools fileTools,
                                  StageDependencyGraph dependencyGraph,
                                  PolicyEngine policyEngine) {
        this.requirementAgent = requirementAgent;
        this.architectAgent = architectAgent;
        this.codeAgent = codeAgent;
        this.codebaseAnalyzerAgent = codebaseAnalyzerAgent;
        this.qualityGateAgent = qualityGateAgent;
        this.fileTools = fileTools;
        this.dependencyGraph = dependencyGraph;
        this.policyEngine = policyEngine;
        this.executorService = Executors.newFixedThreadPool(4);
    }

    public ExecutionContext initializeRun(String requirement, String scenario) throws IOException {
        ExecutionContext ctx = new ExecutionContext();
        ctx.setRawRequirement(requirement);
        ctx.setScenario(scenario);
        ctx.setTargetWorkspace(Paths.get("./target/sandbox-" + ctx.getRunId()));
        ctx.setStartTime(Instant.now());
        fileTools.createSandbox(ctx.getTargetWorkspace());

        // Validate requirement against policies
        var policyViolations = policyEngine.validateRequirement(requirement, ctx);
        if (!policyViolations.isEmpty()) {
            ctx.addAudit("PolicyEngine", "REQUIREMENT_VALIDATION", 
                "Found " + policyViolations.size() + " policy violations", false);
        }

        activeContexts.put(ctx.getRunId(), ctx);
        executeNextStage(ctx);
        return ctx;
    }

    public void executeNextStage(ExecutionContext ctx) {
        SdlcStage currentStage = ctx.getCurrentStage();
        Instant stageStart = Instant.now();

        // Track stage execution count
        ctx.getStageExecutionCounts().merge(currentStage, 1, Integer::sum);

        switch (currentStage) {
            case INTAKE -> {
                SdlcStage nextStage = SdlcStage.REQUIREMENTS_ANALYSIS;
                if (policyEngine.allowTransition(currentStage, nextStage, ctx)) {
                    ctx.setCurrentStage(nextStage);
                    executeStageAsync(ctx, nextStage, () -> {
                        requirementAgent.execute(ctx);
                    });
                } else {
                    ctx.addAudit("PolicyEngine", "BLOCKED", "Transition blocked by policy guardrails", false);
                }
            }
            case REQUIREMENTS_ANALYSIS -> {
                SdlcStage nextStage = SdlcStage.ARCHITECTURE_DESIGN;
                if (policyEngine.allowTransition(currentStage, nextStage, ctx)) {
                    ctx.setCurrentStage(nextStage);
                    executeStageAsync(ctx, nextStage, () -> {
                        architectAgent.execute(ctx);
                    });
                }
            }
            case ARCHITECTURE_DESIGN -> {
                // Validate architecture against policies
                var violations = policyEngine.validateArchitecture(ctx.getArchitecturePlan(), ctx);
                if (!violations.isEmpty()) {
                    ctx.addAudit("PolicyEngine", "ARCHITECTURE_VALIDATION", 
                        "Found " + violations.size() + " architecture policy violations", 
                        violations.stream().noneMatch(v -> v.getSeverity() == PolicyEngine.Severity.CRITICAL));
                }

                SdlcStage nextStage = SdlcStage.CODE_SYNTHESIS;
                if (policyEngine.allowTransition(currentStage, nextStage, ctx)) {
                    ctx.setCurrentStage(nextStage);
                    executeStageAsync(ctx, nextStage, () -> {
                        codeAgent.execute(ctx);
                    });
                }
            }
            case CODE_SYNTHESIS -> {
                SdlcStage nextStage = SdlcStage.BUILD_AND_TEST;
                if (policyEngine.allowTransition(currentStage, nextStage, ctx)) {
                    ctx.setCurrentStage(nextStage);
                    executeStageAsync(ctx, nextStage, () -> {
                        boolean pass = qualityGateAgent.verify(ctx);
                        handleQualityGateResult(ctx, pass);
                    });
                }
            }
            case BUILD_AND_TEST -> {
                if (ctx.isTestPassed()) {
                    SdlcStage nextStage = SdlcStage.AWAITING_APPROVAL;
                    if (policyEngine.allowTransition(currentStage, nextStage, ctx)) {
                        ctx.setCurrentStage(nextStage);
                        ctx.addAudit("Gatekeeper", "HOLD", "Build passed. Suspended for human approval checkpoint.", true);
                    }
                }
            }
            case AWAITING_APPROVAL -> {
                if (ctx.isHumanApproved()) {
                    SdlcStage nextStage = SdlcStage.RELEASED;
                    if (policyEngine.allowTransition(currentStage, nextStage, ctx)) {
                        ctx.setCurrentStage(nextStage);
                        ctx.setEndTime(Instant.now());
                        ctx.addAudit("Governance", "RELEASE", "Approved by human reviewer. Artifact ready.", true);
                    }
                }
            }
            default -> {}
        }

        // Track stage latency
        long stageLatency = java.time.Duration.between(stageStart, Instant.now()).toMillis();
        ctx.getStageLatencyMs().put(currentStage, stageLatency);
        ctx.setTotalStageExecutionTimeMs(ctx.getTotalStageExecutionTimeMs() + stageLatency);
    }

    public ExecutionContext approveRelease(String runId) {
        ExecutionContext ctx = activeContexts.get(runId);
        if (ctx != null && ctx.getCurrentStage() == SdlcStage.AWAITING_APPROVAL) {
            ctx.setHumanApproved(true);
            executeNextStage(ctx);
        }
        return ctx;
    }

    public ExecutionContext getContext(String runId) {
        return activeContexts.get(runId);
    }

    private void executeStageAsync(ExecutionContext ctx, SdlcStage stage, Runnable task) {
        CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
            try {
                task.run();
                executeNextStage(ctx);
            } catch (Exception e) {
                ctx.addAudit("Orchestrator", "STAGE_ERROR", "Stage execution failed: " + e.getMessage(), false);
                ctx.setFailureReason(e.getMessage());
            }
        }, executorService);

        try {
            future.get(5, TimeUnit.MINUTES);
        } catch (TimeoutException e) {
            ctx.addAudit("Orchestrator", "TIMEOUT", "Stage execution timed out", false);
            ctx.setFailureReason("Stage execution timeout");
        } catch (Exception e) {
            ctx.addAudit("Orchestrator", "ERROR", "Stage execution error: " + e.getMessage(), false);
            ctx.setFailureReason(e.getMessage());
        }
    }

    private void handleQualityGateResult(ExecutionContext ctx, boolean pass) {
        if (pass) {
            ctx.setCurrentStage(SdlcStage.AWAITING_APPROVAL);
            ctx.addAudit("Gatekeeper", "HOLD", "Build passed. Suspended for human approval checkpoint.", true);
        } else if (ctx.getRetryCount() < ctx.getMaxRetries()) {
            ctx.setRetryCount(ctx.getRetryCount() + 1);
            ctx.addAudit("Gatekeeper", "RETRY", "Verification failed. Triggering re-synthesis attempt " + ctx.getRetryCount(), false);
            ctx.setCurrentStage(SdlcStage.CODE_SYNTHESIS);
            executeNextStage(ctx);
        } else {
            ctx.setCurrentStage(SdlcStage.FAILED);
            ctx.setTotalRollbacks(ctx.getTotalRollbacks() + 1);
            ctx.addAudit("Gatekeeper", "FAIL", "Max retries reached. Triggering safe-stop rollback.", false);
            fileTools.rollbackSandbox(ctx.getTargetWorkspace());
        }
    }

    public Map<String, Object> getReliabilityMetrics(String runId) {
        ExecutionContext ctx = activeContexts.get(runId);
        if (ctx == null) {
            return Collections.emptyMap();
        }

        Map<String, Object> metrics = new HashMap<>();
        metrics.put("runId", ctx.getRunId());
        metrics.put("currentStage", ctx.getCurrentStage());
        metrics.put("totalExecutionTimeMs", ctx.getTotalStageExecutionTimeMs());
        metrics.put("totalRollbacks", ctx.getTotalRollbacks());
        metrics.put("retryCount", ctx.getRetryCount());
        metrics.put("stageExecutionCounts", ctx.getStageExecutionCounts());
        metrics.put("stageLatencyMs", ctx.getStageLatencyMs());
        metrics.put("auditTrailSize", ctx.getAuditTrail().size());
        
        if (ctx.getStartTime() != null && ctx.getEndTime() != null) {
            metrics.put("endToEndLatencyMs", java.time.Duration.between(ctx.getStartTime(), ctx.getEndTime()).toMillis());
        }
        
        return metrics;
    }

    public void triggerReplan(String runId) {
        ExecutionContext ctx = activeContexts.get(runId);
        if (ctx != null) {
            ctx.addAudit("Orchestrator", "REPLAN", "Dynamic re-planning triggered due to upstream changes", true);
            // Re-execute from current stage with updated context
            executeNextStage(ctx);
        }
    }
}
