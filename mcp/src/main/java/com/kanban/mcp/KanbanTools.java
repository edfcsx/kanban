package com.kanban.mcp;

import com.kanban.core.KanbanRepository;
import com.kanban.core.Project;
import com.kanban.core.ProjectRegistry;
import com.kanban.core.Task;
import com.kanban.core.TaskCategory;
import com.kanban.core.TaskStatus;

import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.spec.McpSchema;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Tool handlers exposed over MCP, one per CLI action (list_projects,
 * create_project, list_tasks, add_task, move_task, edit_task, delete_task).
 * Every handler resolves its target project the same way the CLI does: an
 * explicit {@code project} argument, falling back to the current project,
 * auto-creating "Default" on first-ever use.
 */
final class KanbanTools {

    private final ProjectRegistry registry;

    KanbanTools(ProjectRegistry registry) {
        this.registry = registry;
    }

    List<McpServerFeatures.SyncToolSpecification> specifications() {
        return List.of(
                listProjectsTool(),
                createProjectTool(),
                listTasksTool(),
                addTaskTool(),
                moveTaskTool(),
                editTaskTool(),
                deleteTaskTool());
    }

    private McpServerFeatures.SyncToolSpecification listProjectsTool() {
        McpSchema.Tool tool = McpSchema.Tool.builder("list_projects", objectSchema(Map.of(), List.of()))
                .description("List every project, marking which one is current. Call this first if you don't "
                        + "know the project slug you need.")
                .build();
        return McpServerFeatures.SyncToolSpecification.builder()
                .tool(tool)
                .callHandler((exchange, request) -> handle(() -> {
                    List<Project> projects = registry.listProjects();
                    if (projects.isEmpty()) {
                        return "(no projects yet - use create_project to make one)";
                    }
                    Optional<Project> current = registry.currentProject();
                    StringBuilder out = new StringBuilder();
                    for (Project project : projects) {
                        boolean isCurrent = current.isPresent() && current.get().slug().equals(project.slug());
                        out.append(isCurrent ? "* " : "  ")
                                .append(project.slug()).append(" - ").append(project.displayName()).append('\n');
                    }
                    return out.toString().stripTrailing();
                }))
                .build();
    }

    private McpServerFeatures.SyncToolSpecification createProjectTool() {
        McpSchema.Tool tool = McpSchema.Tool.builder("create_project", objectSchema(
                        Map.of("name", stringProperty("Display name for the project, e.g. \"Kanban App\"")),
                        List.of("name")))
                .description("Create a new project and make it the current one. Use this to start tracking tasks "
                        + "for a new codebase or initiative.")
                .build();
        return McpServerFeatures.SyncToolSpecification.builder()
                .tool(tool)
                .callHandler((exchange, request) -> handle(() -> {
                    String name = stringArg(request, "name");
                    Project project = registry.createProject(name);
                    return "OK project=" + project.slug() + " name=\"" + project.displayName() + "\" (now current)";
                }))
                .build();
    }

    private McpServerFeatures.SyncToolSpecification listTasksTool() {
        McpSchema.Tool tool = McpSchema.Tool.builder("list_tasks", objectSchema(
                        Map.of(
                                "project", stringProperty("Project slug (see list_projects). Defaults to the current project."),
                                "status", stringProperty("Filter by status: todo, in_progress, or completed."),
                                "category", stringProperty("Filter by category: none, feature, bug, security, chore, or docs.")),
                        List.of()))
                .description("List tasks in a project, optionally filtered by status and/or category. Omit "
                        + "'project' to use the current project.")
                .build();
        return McpServerFeatures.SyncToolSpecification.builder()
                .tool(tool)
                .callHandler((exchange, request) -> handle(() -> {
                    KanbanRepository repository = repositoryFor(request);
                    String statusArg = stringArg(request, "status");
                    String categoryArg = stringArg(request, "category");
                    TaskStatus statusFilter = statusArg == null ? null : TaskStatus.parse(statusArg);
                    TaskCategory categoryFilter = categoryArg == null ? null : TaskCategory.parse(categoryArg);

                    List<Task> tasks = repository.loadAll().stream()
                            .filter(t -> statusFilter == null || t.getStatus() == statusFilter)
                            .filter(t -> categoryFilter == null || t.getCategory() == categoryFilter)
                            .toList();

                    if (tasks.isEmpty()) {
                        return "(no tasks)";
                    }
                    StringBuilder out = new StringBuilder();
                    for (Task task : tasks) {
                        out.append('[').append(task.getStatus()).append("] [").append(task.getCategory())
                                .append("] ").append(task.getId()).append(" - ").append(task.getTitle()).append('\n');
                    }
                    return out.toString().stripTrailing();
                }))
                .build();
    }

    private McpServerFeatures.SyncToolSpecification addTaskTool() {
        McpSchema.Tool tool = McpSchema.Tool.builder("add_task", objectSchema(
                        Map.of(
                                "title", stringProperty("Task title."),
                                "description", stringProperty("Task description, Markdown supported. Optional."),
                                "category", stringProperty("One of: none, feature, bug, security, chore, docs. Optional, defaults to none."),
                                "project", stringProperty("Project slug. Defaults to the current project.")),
                        List.of("title")))
                .description("Create a new task in the Todo column. Description supports Markdown, including "
                        + "```code``` fences. Omit 'project' to use the current project.")
                .build();
        return McpServerFeatures.SyncToolSpecification.builder()
                .tool(tool)
                .callHandler((exchange, request) -> handle(() -> {
                    KanbanRepository repository = repositoryFor(request);
                    String title = stringArg(request, "title");
                    String description = stringArg(request, "description");
                    TaskCategory category = TaskCategory.parse(stringArg(request, "category"));
                    Task task = repository.addTask(title, description == null ? "" : description, category);
                    return "OK id=" + task.getId() + " status=" + task.getStatus()
                            + " category=" + task.getCategory() + " title=\"" + task.getTitle() + "\"";
                }))
                .build();
    }

    private McpServerFeatures.SyncToolSpecification moveTaskTool() {
        McpSchema.Tool tool = McpSchema.Tool.builder("move_task", objectSchema(
                        Map.of(
                                "id", stringProperty("Task id, as returned by add_task or list_tasks."),
                                "status", stringProperty("New status: todo, in_progress, or completed."),
                                "project", stringProperty("Project slug. Defaults to the current project.")),
                        List.of("id", "status")))
                .description("Move a task to a different status/column. Omit 'project' to use the current project.")
                .build();
        return McpServerFeatures.SyncToolSpecification.builder()
                .tool(tool)
                .callHandler((exchange, request) -> handle(() -> {
                    KanbanRepository repository = repositoryFor(request);
                    String id = stringArg(request, "id");
                    TaskStatus status = TaskStatus.parse(stringArg(request, "status"));
                    if (!repository.updateStatus(id, status)) {
                        throw new IllegalArgumentException("no task found with id=" + id);
                    }
                    return "OK id=" + id + " status=" + status;
                }))
                .build();
    }

    private McpServerFeatures.SyncToolSpecification editTaskTool() {
        McpSchema.Tool tool = McpSchema.Tool.builder("edit_task", objectSchema(
                        Map.of(
                                "id", stringProperty("Task id, as returned by add_task or list_tasks."),
                                "title", stringProperty("New title. Optional."),
                                "description", stringProperty("New description, Markdown supported. Optional."),
                                "category", stringProperty("One of: none, feature, bug, security, chore, docs. Optional."),
                                "project", stringProperty("Project slug. Defaults to the current project.")),
                        List.of("id")))
                .description("Update a task's title, description and/or category. Only the provided fields "
                        + "change. Omit 'project' to use the current project.")
                .build();
        return McpServerFeatures.SyncToolSpecification.builder()
                .tool(tool)
                .callHandler((exchange, request) -> handle(() -> {
                    KanbanRepository repository = repositoryFor(request);
                    String id = stringArg(request, "id");
                    String title = stringArg(request, "title");
                    String description = stringArg(request, "description");
                    String categoryArg = stringArg(request, "category");
                    TaskCategory category = categoryArg == null ? null : TaskCategory.parse(categoryArg);
                    if (!repository.updateTask(id, title, description, category)) {
                        throw new IllegalArgumentException("no task found with id=" + id);
                    }
                    return "OK id=" + id + " updated";
                }))
                .build();
    }

    private McpServerFeatures.SyncToolSpecification deleteTaskTool() {
        McpSchema.Tool tool = McpSchema.Tool.builder("delete_task", objectSchema(
                        Map.of(
                                "id", stringProperty("Task id, as returned by add_task or list_tasks."),
                                "project", stringProperty("Project slug. Defaults to the current project.")),
                        List.of("id")))
                .description("Permanently delete a task. Omit 'project' to use the current project.")
                .build();
        return McpServerFeatures.SyncToolSpecification.builder()
                .tool(tool)
                .callHandler((exchange, request) -> handle(() -> {
                    KanbanRepository repository = repositoryFor(request);
                    String id = stringArg(request, "id");
                    if (!repository.deleteTask(id)) {
                        throw new IllegalArgumentException("no task found with id=" + id);
                    }
                    return "OK id=" + id + " deleted";
                }))
                .build();
    }

    // ---- shared plumbing ----

    private KanbanRepository repositoryFor(McpSchema.CallToolRequest request) {
        Project project = resolveProject(request);
        return new KanbanRepository(registry.taskDbFile(project), registry.taskLockFile(project));
    }

    private Project resolveProject(McpSchema.CallToolRequest request) {
        String slug = stringArg(request, "project");
        if (slug != null) {
            return registry.findBySlug(slug)
                    .orElseThrow(() -> new IllegalArgumentException(
                            "no project found with slug '" + slug + "'. Call list_projects to see valid slugs."));
        }
        return registry.currentProject().orElseGet(() -> registry.createProject("Default"));
    }

    private static String stringArg(McpSchema.CallToolRequest request, String key) {
        Object value = request.arguments().get(key);
        if (value == null) {
            return null;
        }
        String text = value.toString().trim();
        return text.isEmpty() ? null : text;
    }

    /** Runs a handler body, turning any domain error into an MCP tool error the caller can read and retry from. */
    private static McpSchema.CallToolResult handle(java.util.function.Supplier<String> body) {
        try {
            return McpSchema.CallToolResult.builder()
                    .content(List.of(McpSchema.TextContent.builder(body.get()).build()))
                    .isError(false)
                    .build();
        } catch (Exception e) {
            return McpSchema.CallToolResult.builder()
                    .content(List.of(McpSchema.TextContent.builder("ERROR: " + e.getMessage()).build()))
                    .isError(true)
                    .build();
        }
    }

    private static Map<String, Object> stringProperty(String description) {
        return Map.of("type", "string", "description", description);
    }

    private static Map<String, Object> objectSchema(Map<String, Object> properties, List<String> required) {
        return Map.of("type", "object", "properties", properties, "required", required);
    }
}
