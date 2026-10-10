package com.sdlc.agentic.orchestrator;

import lombok.Data;
import java.nio.file.Path;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Data
public class ExecutionContext {
    private final String runId = UUID.randomUUID().toString();
    private String scenario; // GREENFIELD, BROWNFIELD, AMBIGUOUS
    private String rawRequirement;
    private SdlcStage currentStage = SdlcStage.INTAKE;
    
    private String normalizedRequirement;
    private List<String> taskBreakdown = new ArrayList<>();
    private String architecturePlan;
    private Map<String, String> generatedCodeArtifacts = new HashMap<>();
    
    private boolean testPassed = false;
    private int retryCount = 0;
    private final int maxRetries = 3;
    private boolean humanApproved = false;
    
    private final List<AuditRecord> auditTrail = Collections.synchronizedList(new ArrayList<>());
    private Path targetWorkspace;

    // Reliability metrics
    private Instant startTime;
    private Instant endTime;
    private long totalStageExecutionTimeMs = 0;
    private int totalRollbacks = 0;
    private String failureReason;
    private Map<SdlcStage, Integer> stageExecutionCounts = new ConcurrentHashMap<>();
    private Map<SdlcStage, Long> stageLatencyMs = new ConcurrentHashMap<>();

    public void addAudit(String agent, String action, String summary, boolean passed) {
        auditTrail.add(new AuditRecord(java.time.Instant.now(), currentStage, agent, action, summary, passed));
    }
}
