package com.sdlc.agentic.agents;

import com.sdlc.agentic.orchestrator.ExecutionContext;
import com.sdlc.agentic.tools.FileSystemTools;
import lombok.Data;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Component
public class CodebaseAnalyzerAgent {

    private final ChatClient chatClient;
    private final FileSystemTools fileTools;

    public CodebaseAnalyzerAgent(ChatClient.Builder builder, FileSystemTools fileTools) {
        this.chatClient = builder.build();
        this.fileTools = fileTools;
    }

    @Data
    public static class ImpactAnalysis {
        private final List<String> affectedModules = new ArrayList<>();
        private final List<String> affectedApis = new ArrayList<>();
        private final List<String> affectedDataFlows = new ArrayList<>();
        private final List<String> dependencies = new ArrayList<>();
        private final List<String> riskAreas = new ArrayList<>();
        private final String summary;
    }

    @Data
    public static class ArchitectureUnderstanding {
        private final Map<String, String> moduleDescriptions = new HashMap<>();
        private final Map<String, List<String>> moduleDependencies = new HashMap<>();
        private final List<String> dataFlowPatterns = new ArrayList<>();
        private final List<String> apiContracts = new ArrayList<>();
        private final String architecturalStyle;
    }

    public void execute(ExecutionContext ctx) {
        if (!"BROWNFIELD".equals(ctx.getScenario())) {
            ctx.addAudit("CodebaseAnalyzerAgent", "SKIP", "Analysis skipped for non-brownfield scenario", true);
            return;
        }

        Path workspace = ctx.getTargetWorkspace();
        ImpactAnalysis impact = analyzeImpact(ctx.getRawRequirement(), workspace);
        ArchitectureUnderstanding architecture = understandArchitecture(workspace);

        ctx.addAudit("CodebaseAnalyzerAgent", "IMPACT_ANALYSIS", 
            "Identified " + impact.getAffectedModules().size() + " affected modules", true);
        ctx.addAudit("CodebaseAnalyzerAgent", "ARCHITECTURE_UNDERSTANDING", 
            "Analyzed " + architecture.getModuleDescriptions().size() + " modules", true);

        // Store analysis results in context
        ctx.getGeneratedCodeArtifacts().put("impact_analysis.json", serializeImpactAnalysis(impact));
        ctx.getGeneratedCodeArtifacts().put("architecture_understanding.json", serializeArchitectureUnderstanding(architecture));
    }

    private ImpactAnalysis analyzeImpact(String requirement, Path workspace) {
        ImpactAnalysis impact = new ImpactAnalysis("Impact analysis for: " + requirement);

        try {
            if (!Files.exists(workspace)) {
                impact.getRiskAreas().add("Workspace does not exist - cannot perform analysis");
                return impact;
            }

            // Scan for Java files
            List<Path> javaFiles = Files.walk(workspace)
                .filter(p -> p.toString().endsWith(".java"))
                .toList();

            // Analyze each file for potential impact
            for (Path javaFile : javaFiles) {
                String content = Files.readString(javaFile);
                
                // Detect affected modules based on class names
                Pattern classPattern = Pattern.compile("class\\s+(\\w+)");
                Matcher classMatcher = classPattern.matcher(content);
                while (classMatcher.find()) {
                    String className = classMatcher.group(1);
                    if (isRelevantToRequirement(requirement, className, content)) {
                        impact.getAffectedModules().add(className);
                    }
                }

                // Detect API endpoints
                Pattern apiPattern = Pattern.compile("@(GetMapping|PostMapping|PutMapping|DeleteMapping|RequestMapping)\\s*\\(\\s*\"([^\"]+)\"");
                Matcher apiMatcher = apiPattern.matcher(content);
                while (apiMatcher.find()) {
                    String endpoint = apiMatcher.group(2);
                    impact.getAffectedApis().add(endpoint);
                }

                // Detect data flow patterns (service calls, repository access)
                if (content.contains("@Service") || content.contains("@Repository")) {
                    impact.getAffectedDataFlows().add(javaFile.getFileName().toString());
                }

                // Extract dependencies
                Pattern importPattern = Pattern.compile("import\\s+([\\w.]+);");
                Matcher importMatcher = importPattern.matcher(content);
                while (importMatcher.find()) {
                    String importStmt = importMatcher.group(1);
                    if (!importStmt.startsWith("java.") && !importStmt.startsWith("org.springframework")) {
                        impact.getDependencies().add(importStmt);
                    }
                }
            }

            // Identify risk areas
            if (impact.getAffectedModules().size() > 5) {
                impact.getRiskAreas().add("High impact - many modules affected");
            }
            if (impact.getAffectedApis().size() > 3) {
                impact.getRiskAreas().add("API contract changes required");
            }
            if (impact.getDependencies().contains("legacy")) {
                impact.getRiskAreas().add("Legacy code dependencies detected");
            }

        } catch (IOException e) {
            impact.getRiskAreas().add("Analysis failed: " + e.getMessage());
        }

        return impact;
    }

    private ArchitectureUnderstanding understandArchitecture(Path workspace) {
        ArchitectureUnderstanding arch = new ArchitectureUnderstanding("Microservice/Component-based");

        try {
            if (!Files.exists(workspace)) {
                return arch;
            }

            List<Path> javaFiles = Files.walk(workspace)
                .filter(p -> p.toString().endsWith(".java"))
                .toList();

            for (Path javaFile : javaFiles) {
                String content = Files.readString(javaFile);
                String fileName = javaFile.getFileName().toString();

                // Extract module descriptions
                Pattern classPattern = Pattern.compile("class\\s+(\\w+)");
                Matcher classMatcher = classPattern.matcher(content);
                if (classMatcher.find()) {
                    String className = classMatcher.group(1);
                    String description = extractClassDescription(content);
                    arch.getModuleDescriptions().put(className, description);
                }

                // Extract dependencies
                Pattern importPattern = Pattern.compile("import\\s+([\\w.]+);");
                Matcher importMatcher = importPattern.matcher(content);
                List<String> deps = new ArrayList<>();
                while (importMatcher.find()) {
                    String importStmt = importMatcher.group(1);
                    if (!importStmt.startsWith("java.")) {
                        deps.add(importStmt);
                    }
                }
                if (!deps.isEmpty()) {
                    arch.getModuleDependencies().put(fileName, deps);
                }

                // Detect architectural patterns
                if (content.contains("@RestController")) {
                    arch.getApiContracts().add(fileName + " - REST Controller");
                }
                if (content.contains("@Service")) {
                    arch.getDataFlowPatterns().add(fileName + " - Service Layer");
                }
                if (content.contains("@Repository")) {
                    arch.getDataFlowPatterns().add(fileName + " - Data Access Layer");
                }
            }

        } catch (IOException e) {
            // Return partial analysis
        }

        return arch;
    }

    private boolean isRelevantToRequirement(String requirement, String className, String content) {
        String lowerRequirement = requirement.toLowerCase();
        String lowerClassName = className.toLowerCase();
        String lowerContent = content.toLowerCase();

        // Simple relevance heuristics
        if (lowerRequirement.contains("url") && (lowerClassName.contains("url") || lowerContent.contains("url"))) {
            return true;
        }
        if (lowerRequirement.contains("shorten") && (lowerClassName.contains("short") || lowerContent.contains("short"))) {
            return true;
        }
        if (lowerRequirement.contains("analytics") && (lowerClassName.contains("analytic") || lowerContent.contains("analytic"))) {
            return true;
        }
        if (lowerRequirement.contains("cache") && (lowerClassName.contains("cache") || lowerContent.contains("cache"))) {
            return true;
        }

        return false;
    }

    private String extractClassDescription(String content) {
        // Extract Javadoc or comments
        Pattern commentPattern = Pattern.compile("/\\*\\*([^*]|\\*[^/])*\\*/");
        Matcher matcher = commentPattern.matcher(content);
        if (matcher.find()) {
            return matcher.group().replaceAll("/\\*\\*|\\*/|\\*", "").trim();
        }
        return "No description available";
    }

    private String serializeImpactAnalysis(ImpactAnalysis impact) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        sb.append("  \"affectedModules\": ").append(impact.getAffectedModules()).append(",\n");
        sb.append("  \"affectedApis\": ").append(impact.getAffectedApis()).append(",\n");
        sb.append("  \"affectedDataFlows\": ").append(impact.getAffectedDataFlows()).append(",\n");
        sb.append("  \"dependencies\": ").append(impact.getDependencies()).append(",\n");
        sb.append("  \"riskAreas\": ").append(impact.getRiskAreas()).append(",\n");
        sb.append("  \"summary\": \"").append(impact.getSummary()).append("\"\n");
        sb.append("}");
        return sb.toString();
    }

    private String serializeArchitectureUnderstanding(ArchitectureUnderstanding arch) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        sb.append("  \"moduleDescriptions\": ").append(arch.getModuleDescriptions()).append(",\n");
        sb.append("  \"moduleDependencies\": ").append(arch.getModuleDependencies()).append(",\n");
        sb.append("  \"dataFlowPatterns\": ").append(arch.getDataFlowPatterns()).append(",\n");
        sb.append("  \"apiContracts\": ").append(arch.getApiContracts()).append(",\n");
        sb.append("  \"architecturalStyle\": \"").append(arch.getArchitecturalStyle()).append("\"\n");
        sb.append("}");
        return sb.toString();
    }
}
