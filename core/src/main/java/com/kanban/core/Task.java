package com.kanban.core;

import java.time.Instant;
import java.util.Objects;

/**
 * A single kanban task. Mutable on purpose: the GUI edits fields in place
 * before asking the repository to persist the whole list back to disk.
 */
public final class Task {
    private final String id;
    private String title;
    private String description;
    private TaskStatus status;
    private TaskCategory category;
    private final Instant createdAt;
    private Instant updatedAt;

    public Task(String id, String title, String description, TaskStatus status,
                TaskCategory category, Instant createdAt, Instant updatedAt) {
        this.id = Objects.requireNonNull(id, "id");
        this.title = Objects.requireNonNull(title, "title");
        this.description = description != null ? description : "";
        this.status = Objects.requireNonNull(status, "status");
        this.category = category != null ? category : TaskCategory.NONE;
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt");
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description != null ? description : "";
    }

    public TaskStatus getStatus() {
        return status;
    }

    public void setStatus(TaskStatus status) {
        this.status = status;
    }

    public TaskCategory getCategory() {
        return category;
    }

    public void setCategory(TaskCategory category) {
        this.category = category != null ? category : TaskCategory.NONE;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Task task)) return false;
        return id.equals(task.id)
                && title.equals(task.title)
                && description.equals(task.description)
                && status == task.status
                && category == task.category
                && createdAt.equals(task.createdAt)
                && updatedAt.equals(task.updatedAt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, title, description, status, category, createdAt, updatedAt);
    }
}