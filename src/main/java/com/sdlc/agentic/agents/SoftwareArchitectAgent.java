package com.sdlc.agentic.agents;

import com.sdlc.agentic.orchestrator.ExecutionContext;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

@Component
public class SoftwareArchitectAgent {

    private final ChatClient chatClient;

    public SoftwareArchitectAgent(ChatClient.Builder builder) {
        this.chatClient = builder.build();
    }

    public void execute(ExecutionContext ctx) {
        String prompt = """
            You are a Principal Software Architect.
            Formulate a microservice design plan based on this specification:
            %s

            Define:
            1. REST API Contracts (POST /api/v1/shorten, GET /{shortCode}, GET /api/v1/analytics/{shortCode}).
            2. In-memory storage/caching architecture using ConcurrentHashMap.
            3. Error handling boundaries and Base62 encoding logic.
            """.formatted(ctx.getNormalizedRequirement());

        String plan = chatClient.prompt().user(prompt).call().content();
        ctx.setArchitecturePlan(plan);
        ctx.addAudit("SoftwareArchitectAgent", "DESIGN", "Architecture plan synthesized", true);
    }
}
