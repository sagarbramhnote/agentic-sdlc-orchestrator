package com.sdlc.agentic.orchestrator;

import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class StageDependencyGraph {

    private final Map<SdlcStage, Set<SdlcStage>> dependencies = new ConcurrentHashMap<>();
    private final Map<SdlcStage, Set<SdlcStage>> dependents = new ConcurrentHashMap<>();

    public StageDependencyGraph() {
        initializeDefaultGraph();
    }

    private void initializeDefaultGraph() {
        // Define stage dependencies
        addDependency(SdlcStage.REQUIREMENTS_ANALYSIS, SdlcStage.INTAKE);
        addDependency(SdlcStage.ARCHITECTURE_DESIGN, SdlcStage.REQUIREMENTS_ANALYSIS);
        addDependency(SdlcStage.CODE_SYNTHESIS, SdlcStage.ARCHITECTURE_DESIGN);
        addDependency(SdlcStage.BUILD_AND_TEST, SdlcStage.CODE_SYNTHESIS);
        addDependency(SdlcStage.AWAITING_APPROVAL, SdlcStage.BUILD_AND_TEST);
        addDependency(SdlcStage.RELEASED, SdlcStage.AWAITING_APPROVAL);
        addDependency(SdlcStage.FAILED, SdlcStage.BUILD_AND_TEST);
        addDependency(SdlcStage.ROLLED_BACK, SdlcStage.FAILED);
    }

    public void addDependency(SdlcStage stage, SdlcStage dependsOn) {
        dependencies.computeIfAbsent(stage, k -> new HashSet<>()).add(dependsOn);
        dependents.computeIfAbsent(dependsOn, k -> new HashSet<>()).add(stage);
    }

    public Set<SdlcStage> getDependencies(SdlcStage stage) {
        return dependencies.getOrDefault(stage, Collections.emptySet());
    }

    public Set<SdlcStage> getDependents(SdlcStage stage) {
        return dependents.getOrDefault(stage, Collections.emptySet());
    }

    public boolean canExecute(SdlcStage stage, Set<SdlcStage> completedStages) {
        Set<SdlcStage> deps = getDependencies(stage);
        return completedStages.containsAll(deps);
    }

    public List<SdlcStage> getExecutableStages(Set<SdlcStage> completedStages) {
        List<SdlcStage> executable = new ArrayList<>();
        for (SdlcStage stage : SdlcStage.values()) {
            if (canExecute(stage, completedStages) && !completedStages.contains(stage)) {
                executable.add(stage);
            }
        }
        return executable;
    }

    public boolean hasCycle() {
        Set<SdlcStage> visited = new HashSet<>();
        Set<SdlcStage> recursionStack = new HashSet<>();

        for (SdlcStage stage : SdlcStage.values()) {
            if (hasCycleUtil(stage, visited, recursionStack)) {
                return true;
            }
        }
        return false;
    }

    private boolean hasCycleUtil(SdlcStage stage, Set<SdlcStage> visited, Set<SdlcStage> recursionStack) {
        if (recursionStack.contains(stage)) {
            return true;
        }
        if (visited.contains(stage)) {
            return false;
        }

        visited.add(stage);
        recursionStack.add(stage);

        for (SdlcStage dependent : getDependents(stage)) {
            if (hasCycleUtil(dependent, visited, recursionStack)) {
                return true;
            }
        }

        recursionStack.remove(stage);
        return false;
    }
}
