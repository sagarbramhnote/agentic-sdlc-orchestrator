package com.sdlc.agentic.orchestrator;

import java.time.Instant;

public record AuditRecord(
    Instant timestamp,
    SdlcStage stage,
    String agentName,
    String action,
    String outputSummary,
    boolean passedGate
) {}
