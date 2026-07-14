package com.kanban.core;

/**
 * The three columns of the board. Order matters: {@link #next()} and
 * {@link #previous()} move one column at a time and clamp at the ends.
 */
public enum TaskStatus {
    TODO("Todo"),
    IN_PROGRESS("In Progress"),
    COMPLETED("Completed");

    private final String label;

    TaskStatus(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    public TaskStatus next() {
        return switch (this) {
            case TODO -> IN_PROGRESS;
            case IN_PROGRESS -> COMPLETED;
            case COMPLETED -> COMPLETED;
        };
    }

    public TaskStatus previous() {
        return switch (this) {
            case TODO -> TODO;
            case IN_PROGRESS -> TODO;
            case COMPLETED -> IN_PROGRESS;
        };
    }

    /**
     * Lenient parser so callers (CLI users, AI assistants) can pass
     * "todo", "in-progress", "Done", "doing", etc.
     */
    public static TaskStatus parse(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("status is required");
        }
        String normalized = raw.trim().toLowerCase().replaceAll("[^a-z]", "");
        return switch (normalized) {
            case "todo" -> TODO;
            case "inprogress", "doing", "progress" -> IN_PROGRESS;
            case "completed", "complete", "done", "finished" -> COMPLETED;
            default -> throw new IllegalArgumentException(
                    "Unknown status '" + raw + "'. Use: todo, in_progress, completed");
        };
    }
}