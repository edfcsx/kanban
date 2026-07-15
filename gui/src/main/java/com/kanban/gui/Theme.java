package com.kanban.gui;

import com.kanban.core.TaskStatus;

/** Style-class helpers matching the selectors defined in kanban.css. */
final class Theme {

    private Theme() {
    }

    static String columnHeaderStyleClass(TaskStatus status) {
        return switch (status) {
            case TODO -> "column-header-todo";
            case IN_PROGRESS -> "column-header-in-progress";
            case COMPLETED -> "column-header-completed";
        };
    }

    static String statusBadgeStyleClass(TaskStatus status) {
        return switch (status) {
            case TODO -> "badge-status-todo";
            case IN_PROGRESS -> "badge-status-in-progress";
            case COMPLETED -> "badge-status-completed";
        };
    }
}
