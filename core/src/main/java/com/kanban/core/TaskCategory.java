package com.kanban.core;

/**
 * A fixed set of labels used to classify what kind of work a task is.
 * Kept as a closed enum (rather than free-form tags) so the GUI can assign
 * each one a stable, predictable color.
 */
public enum TaskCategory {
    NONE("Uncategorized"),
    FEATURE("Feature"),
    BUG("Bug"),
    SECURITY("Security"),
    CHORE("Chore"),
    DOCS("Docs");

    private final String label;

    TaskCategory(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    /**
     * Lenient parser so callers (CLI users, AI assistants) can pass
     * "sec", "enhancement", "doc", etc. Blank/null means "no category".
     */
    public static TaskCategory parse(String raw) {
        if (raw == null || raw.isBlank()) {
            return NONE;
        }
        String normalized = raw.trim().toLowerCase().replaceAll("[^a-z]", "");
        return switch (normalized) {
            case "none", "uncategorized" -> NONE;
            case "feature", "enhancement" -> FEATURE;
            case "bug", "defect", "fix" -> BUG;
            case "security", "sec", "vuln", "vulnerability" -> SECURITY;
            case "chore", "maintenance" -> CHORE;
            case "docs", "documentation", "doc" -> DOCS;
            default -> throw new IllegalArgumentException(
                    "Unknown category '" + raw + "'. Use: none, feature, bug, security, chore, docs");
        };
    }
}