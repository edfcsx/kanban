package com.kanban.gui;

import com.kanban.core.TaskCategory;
import com.kanban.core.TaskStatus;

import java.awt.Color;

/**
 * Fixed color palette for the whole board. Colors are applied explicitly
 * everywhere (backgrounds AND text) instead of relying on look-and-feel
 * defaults, so the app looks the same regardless of the OS light/dark theme.
 */
final class Theme {

    static final Color WINDOW_BACKGROUND = new Color(0xF4F5F7);
    static final Color SURFACE = Color.WHITE;
    static final Color BORDER = new Color(0xDDDDDD);

    static final Color TEXT_PRIMARY = new Color(0x1F2328);
    static final Color TEXT_SECONDARY = new Color(0x57606A);
    static final Color TEXT_MUTED = new Color(0x8A94A6);

    static final Color COLUMN_TODO = new Color(0xE8EAED);
    static final Color COLUMN_IN_PROGRESS = new Color(0xD7E8FB);
    static final Color COLUMN_COMPLETED = new Color(0xDBF3DE);

    private Theme() {
    }

    static Color statusColor(TaskStatus status) {
        return switch (status) {
            case TODO -> COLUMN_TODO;
            case IN_PROGRESS -> COLUMN_IN_PROGRESS;
            case COMPLETED -> COLUMN_COMPLETED;
        };
    }

    static Color categoryBackground(TaskCategory category) {
        return switch (category) {
            case NONE -> new Color(0xEFF0F2);
            case FEATURE -> new Color(0xDDEAFB);
            case BUG -> new Color(0xFBE2D5);
            case SECURITY -> new Color(0xFBDCDC);
            case CHORE -> new Color(0xE7E8EA);
            case DOCS -> new Color(0xE8DEFB);
        };
    }

    static Color categoryForeground(TaskCategory category) {
        return switch (category) {
            case NONE -> TEXT_MUTED;
            case FEATURE -> new Color(0x1B4C9C);
            case BUG -> new Color(0xB65C00);
            case SECURITY -> new Color(0xB42318);
            case CHORE -> new Color(0x5C5F66);
            case DOCS -> new Color(0x6A3FB5);
        };
    }
}