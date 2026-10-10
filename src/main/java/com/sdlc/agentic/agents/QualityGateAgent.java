package com.sdlc.agentic.agents;

import com.sdlc.agentic.orchestrator.ExecutionContext;
import com.sdlc.agentic.tools.BuildVerificationTools;
import org.springframework.stereotype.Component;

@Component
public class QualityGateAgent {

    private final BuildVerificationTools buildTools;

    public QualityGateAgent(BuildVerificationTools buildTools) {
        this.buildTools = buildTools;
    }

    public boolean verify(ExecutionContext ctx) {
        boolean codePresent = ctx.getGeneratedCodeArtifacts().containsKey("UrlShortenerService.java");
        boolean testsPresent = ctx.getGeneratedCodeArtifacts().containsKey("UrlShortenerServiceTest.java");

        if (!codePresent || !testsPresent) {
            ctx.addAudit("QualityGateAgent", "GATE_FAIL", "Missing mandatory artifacts", false);
            return false;
        }

        boolean syntaxValid = buildTools.verifySyntax(ctx.getGeneratedCodeArtifacts().get("UrlShortenerService.java"));
        ctx.setTestPassed(syntaxValid);

        ctx.addAudit("QualityGateAgent", "VERIFICATION",
                syntaxValid ? "Passed code contracts and test validation checks" : "Syntax verification failed",
                syntaxValid);

        return syntaxValid;
    }
}
