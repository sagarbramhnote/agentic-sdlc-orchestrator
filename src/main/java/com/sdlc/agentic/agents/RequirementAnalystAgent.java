package com.sdlc.agentic.agents;

import com.sdlc.agentic.orchestrator.ExecutionContext;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Component
public class RequirementAnalystAgent {

    private final ChatClient chatClient;

    public RequirementAnalystAgent(ChatClient.Builder builder) {
        this.chatClient = builder.build();
    }

    public void execute(ExecutionContext ctx) {
        String prompt = """
            You are a Principal Systems Analyst.
            Analyze the following requirement for a URL Shortener service:
            Scenario Type: %s
            Requirement: %s
            
            Tasks:
            1. Clarify any ambiguities (e.g., base62 hashing, collision handling, TTL, rate limiting).
            2. Output a normalized engineering specification followed by a numbered task breakdown.
            Format output strictly as:
            SPECIFICATION: <details>
            TASKS:
            1. <task>
            2. <task>
            """.formatted(ctx.getScenario(), ctx.getRawRequirement());

        String response = chatClient.prompt().user(prompt).call().content();

        ctx.setNormalizedRequirement(response);
        ctx.getTaskBreakdown().addAll(
            Arrays.stream(response.split("\n"))
                  .filter(line -> line.matches("^\\d+\\..*"))
                  .toList()
        );

        ctx.addAudit("RequirementAnalystAgent", "NORMALIZE", "Extracted scope and normalized tasks", true);
    }
}
