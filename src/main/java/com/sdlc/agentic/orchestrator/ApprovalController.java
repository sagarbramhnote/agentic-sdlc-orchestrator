package com.sdlc.agentic.orchestrator;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/api/orchestrator")
public class ApprovalController {

    private final SdlcOrchestratorEngine engine;

    public ApprovalController(SdlcOrchestratorEngine engine) {
        this.engine = engine;
    }

    @PostMapping("/run")
    public ResponseEntity<ExecutionContext> runPipeline(@RequestBody Map<String, String> payload) throws IOException {
        String requirement = payload.getOrDefault("requirement", "Build a high-performance URL shortener");
        String scenario = payload.getOrDefault("scenario", "GREENFIELD");
        ExecutionContext ctx = engine.initializeRun(requirement, scenario);
        return ResponseEntity.ok(ctx);
    }

    @GetMapping("/status/{runId}")
    public ResponseEntity<ExecutionContext> getStatus(@PathVariable String runId) {
        ExecutionContext ctx = engine.getContext(runId);
        return ctx != null ? ResponseEntity.ok(ctx) : ResponseEntity.notFound().build();
    }

    @PostMapping("/approve/{runId}")
    public ResponseEntity<ExecutionContext> approveCheckpoint(@PathVariable String runId) {
        ExecutionContext ctx = engine.approveRelease(runId);
        return ctx != null ? ResponseEntity.ok(ctx) : ResponseEntity.notFound().build();
    }
}
