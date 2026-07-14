package com.kanban.cli;

import com.kanban.core.KanbanRepository;
import com.kanban.core.Task;
import com.kanban.core.TaskCategory;
import com.kanban.core.TaskStatus;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Command line front-end for the kanban database, meant to be driven by
 * scripts or an AI assistant, e.g.:
 * <pre>
 *   java -jar kanban-api.jar action=add title=xpto desc="any description"
 * </pre>
 * Arguments are plain {@code key=value} pairs, in any order. All output goes
 * to stdout on success and stderr on failure, with a non-zero exit code on
 * error, so callers can script against it reliably.
 */
public final class KanbanCli {

    public static void main(String[] args) {
        Map<String, String> params = parseArgs(args);
        String action = params.getOrDefault("action", "help").toLowerCase();
        KanbanRepository repository = new KanbanRepository();

        try {
            switch (action) {
                case "add" -> add(repository, params);
                case "list" -> list(repository, params);
                case "move", "update" -> move(repository, params);
                case "edit" -> edit(repository, params);
                case "delete", "remove" -> delete(repository, params);
                case "help" -> printHelp(repository);
                default -> {
                    System.err.println("ERROR: unknown action '" + action + "'");
                    printHelp(repository);
                    System.exit(1);
                }
            }
        } catch (IllegalArgumentException e) {
            System.err.println("ERROR: " + e.getMessage());
            System.exit(1);
        } catch (Exception e) {
            System.err.println("ERROR: " + e.getMessage());
            System.exit(2);
        }
    }

    private static void add(KanbanRepository repository, Map<String, String> params) {
        String title = require(params, "title");
        String description = params.getOrDefault("desc", params.getOrDefault("description", ""));
        TaskCategory category = TaskCategory.parse(params.get("category"));
        Task task = repository.addTask(title, description, category);
        System.out.println("OK id=" + task.getId() + " status=" + task.getStatus()
                + " category=" + task.getCategory() + " title=\"" + task.getTitle() + "\"");
    }

    private static void list(KanbanRepository repository, Map<String, String> params) {
        String statusFilter = params.get("status");
        TaskStatus filter = statusFilter != null ? TaskStatus.parse(statusFilter) : null;
        String categoryFilter = params.get("category");
        TaskCategory categoryF = categoryFilter != null ? TaskCategory.parse(categoryFilter) : null;

        List<Task> tasks = repository.loadAll();
        if (filter != null) {
            tasks = tasks.stream().filter(t -> t.getStatus() == filter).toList();
        }
        if (categoryF != null) {
            tasks = tasks.stream().filter(t -> t.getCategory() == categoryF).toList();
        }

        if (tasks.isEmpty()) {
            System.out.println("(no tasks)");
            return;
        }

        for (Task task : tasks) {
            System.out.println("[" + task.getStatus() + "] [" + task.getCategory() + "] "
                    + task.getId() + " - " + task.getTitle());
            if (task.getDescription() != null && !task.getDescription().isBlank()) {
                System.out.println("    " + task.getDescription().replace("\n", "\n    "));
            }
        }
    }

    private static void move(KanbanRepository repository, Map<String, String> params) {
        String id = require(params, "id");
        TaskStatus status = TaskStatus.parse(require(params, "status"));
        boolean updated = repository.updateStatus(id, status);
        if (!updated) {
            System.err.println("ERROR: no task found with id=" + id);
            System.exit(1);
            return;
        }
        System.out.println("OK id=" + id + " status=" + status);
    }

    private static void edit(KanbanRepository repository, Map<String, String> params) {
        String id = require(params, "id");
        String title = params.get("title");
        String description = params.containsKey("desc") ? params.get("desc") : params.get("description");
        TaskCategory category = params.containsKey("category") ? TaskCategory.parse(params.get("category")) : null;
        boolean updated = repository.updateTask(id, title, description, category);
        if (!updated) {
            System.err.println("ERROR: no task found with id=" + id);
            System.exit(1);
            return;
        }
        System.out.println("OK id=" + id + " updated");
    }

    private static void delete(KanbanRepository repository, Map<String, String> params) {
        String id = require(params, "id");
        boolean deleted = repository.deleteTask(id);
        if (!deleted) {
            System.err.println("ERROR: no task found with id=" + id);
            System.exit(1);
            return;
        }
        System.out.println("OK id=" + id + " deleted");
    }

    private static void printHelp(KanbanRepository repository) {
        System.out.println("""
                Kanban CLI - manage kanban tasks from the command line

                Usage:
                  java -jar kanban-api.jar action=add title="Task title" desc="Description" [category=feature|bug|security|chore|docs]
                  java -jar kanban-api.jar action=list [status=todo|in_progress|completed] [category=feature|bug|security|chore|docs]
                  java -jar kanban-api.jar action=move id=<id> status=todo|in_progress|completed
                  java -jar kanban-api.jar action=edit id=<id> [title="New title"] [desc="New description"] [category=feature|bug|security|chore|docs]
                  java -jar kanban-api.jar action=delete id=<id>

                Database file: %s
                """.formatted(repository.getDatabaseFile().getAbsolutePath()));
    }

    private static String require(Map<String, String> params, String key) {
        String value = params.get(key);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("missing required parameter '" + key + "'");
        }
        return value;
    }

    private static Map<String, String> parseArgs(String[] args) {
        Map<String, String> params = new HashMap<>();
        for (String arg : args) {
            int idx = arg.indexOf('=');
            if (idx < 0) {
                params.put(arg.toLowerCase(), "");
            } else {
                params.put(arg.substring(0, idx).toLowerCase(), arg.substring(idx + 1));
            }
        }
        return params;
    }
}