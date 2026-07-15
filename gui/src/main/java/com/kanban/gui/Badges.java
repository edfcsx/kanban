package com.kanban.gui;

import com.kanban.core.TaskCategory;
import com.kanban.core.TaskStatus;

import javafx.scene.control.Label;

/** Small colored pill labels used to show a task's category or status. */
final class Badges {

    private Badges() {
    }

    static Label category(TaskCategory category) {
        Label badge = new Label(category.label());
        badge.getStyleClass().addAll("badge", categoryStyleClass(category));
        return badge;
    }

    static Label status(TaskStatus status) {
        Label badge = new Label(status.label());
        badge.getStyleClass().addAll("badge", Theme.statusBadgeStyleClass(status));
        return badge;
    }

    private static String categoryStyleClass(TaskCategory category) {
        return switch (category) {
            case NONE -> "badge-category-none";
            case FEATURE -> "badge-category-feature";
            case BUG -> "badge-category-bug";
            case SECURITY -> "badge-category-security";
            case CHORE -> "badge-category-chore";
            case DOCS -> "badge-category-docs";
        };
    }
}
