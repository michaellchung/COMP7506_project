package hk.hkucs.comp7506_project.model;

import java.util.List;

public class FeatureModule {
    private final ModuleType type;
    private final String title;
    private final String description;
    private final List<String> nextTasks;
    private final AiResult demoResult;

    public FeatureModule(ModuleType type, String title, String description, List<String> nextTasks, AiResult demoResult) {
        this.type = type;
        this.title = title;
        this.description = description;
        this.nextTasks = nextTasks;
        this.demoResult = demoResult;
    }

    public ModuleType getType() {
        return type;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public List<String> getNextTasks() {
        return nextTasks;
    }

    public AiResult getDemoResult() {
        return demoResult;
    }
}
