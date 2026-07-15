package com.kanban.core;

/**
 * A named group of tasks, backed by its own directory under
 * {@link KanbanPaths#projectsDirectory()}. {@code slug} is the directory
 * name (URL/filesystem-safe); {@code displayName} is what the user typed.
 */
public record Project(String slug, String displayName) {
}
