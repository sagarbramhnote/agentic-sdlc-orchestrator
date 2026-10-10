package com.sdlc.agentic.orchestrator;

import lombok.Data;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class PolicyEngine {

    @Data
    public static class PolicyViolation {
        private final String policyName;
        private final String description;
        private final String recommendation;
        private final Severity severity;
    }

    public enum Severity {
        CRITICAL,
        HIGH,
        MEDIUM,
        LOW
    }

    public List<PolicyViolation> validateCodeGeneration(String code, ExecutionContext ctx) {
        List<PolicyViolation> violations = new ArrayList<>();

        // Security policies
        if (code.contains("eval(") || code.contains("Runtime.exec(")) {
            violations.add(new PolicyViolation(
                "SEC-001",
                "Dynamic code execution detected",
                "Avoid using eval() or Runtime.exec() in generated code. Use safe alternatives.",
                Severity.CRITICAL
            ));
        }

        if (code.contains("password") || code.contains("secret") || code.contains("api_key")) {
            violations.add(new PolicyViolation(
                "SEC-002",
                "Potential hardcoded credentials detected",
                "Use environment variables or secure vault for credentials.",
                Severity.HIGH
            ));
        }

        // Compliance policies
        if (code.contains("System.out.println") || code.contains("printStackTrace")) {
            violations.add(new PolicyViolation(
                "COMPL-001",
                "Debug logging in production code",
                "Use proper logging framework (SLF4J/Logback) instead of System.out.",
                Severity.MEDIUM
            ));
        }

        // Quality policies
        if (code.contains("catch (Exception e) {}") || code.contains("catch(Exception e){}")) {
            violations.add(new PolicyViolation(
                "QUAL-001",
                "Empty catch block detected",
                "Handle exceptions properly or log them.",
                Severity.HIGH
            ));
        }

        return violations;
    }

    public List<PolicyViolation> validateArchitecture(String architecturePlan, ExecutionContext ctx) {
        List<PolicyViolation> violations = new ArrayList<>();

        // Architecture policies
        if (!architecturePlan.toLowerCase().contains("error handling") && 
            !architecturePlan.toLowerCase().contains("exception")) {
            violations.add(new PolicyViolation(
                "ARCH-001",
                "Missing error handling strategy",
                "Define explicit error handling and recovery mechanisms.",
                Severity.HIGH
            ));
        }

        if (!architecturePlan.toLowerCase().contains("security") && 
            !architecturePlan.toLowerCase().contains("authentication")) {
            violations.add(new PolicyViolation(
                "ARCH-002",
                "Missing security considerations",
                "Include security, authentication, and authorization in architecture.",
                Severity.HIGH
            ));
        }

        if (!architecturePlan.toLowerCase().contains("scalability") && 
            !architecturePlan.toLowerCase().contains("scale")) {
            violations.add(new PolicyViolation(
                "ARCH-003",
                "Missing scalability considerations",
                "Define scaling strategy for the architecture.",
                Severity.MEDIUM
            ));
        }

        return violations;
    }

    public List<PolicyViolation> validateStageTransition(SdlcStage from, SdlcStage to, ExecutionContext ctx) {
        List<PolicyViolation> violations = new ArrayList<>();

        // Change control policies
        if (to == SdlcStage.RELEASED && !ctx.isHumanApproved()) {
            violations.add(new PolicyViolation(
                "GOV-001",
                "Release without human approval",
                "Human approval is required before release.",
                Severity.CRITICAL
            ));
        }

        if (to == SdlcStage.RELEASED && !ctx.isTestPassed()) {
            violations.add(new PolicyViolation(
                "GOV-002",
                "Release without passing quality gates",
                "All quality gates must pass before release.",
                Severity.CRITICAL
            ));
        }

        return violations;
    }

    public boolean allowTransition(SdlcStage from, SdlcStage to, ExecutionContext ctx) {
        List<PolicyViolation> violations = validateStageTransition(from, to, ctx);
        return violations.stream().noneMatch(v -> v.getSeverity() == Severity.CRITICAL);
    }

    public List<PolicyViolation> validateRequirement(String requirement, ExecutionContext ctx) {
        List<PolicyViolation> violations = new ArrayList<>();

        // Requirement clarity policies
        if (requirement.length() < 50) {
            violations.add(new PolicyViolation(
                "REQ-001",
                "Requirement too brief",
                "Provide more detailed requirements for better understanding.",
                Severity.MEDIUM
            ));
        }

        if (requirement.toLowerCase().contains("etc") || requirement.toLowerCase().contains("and so on")) {
            violations.add(new PolicyViolation(
                "REQ-002",
                "Ambiguous requirement detected",
                "Avoid vague terms like 'etc' or 'and so on'. Be specific.",
                Severity.HIGH
            ));
        }

        return violations;
    }
}
