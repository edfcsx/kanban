# Kanban

A simple, local-first kanban board. A JavaFX desktop app, a scriptable CLI,
and an MCP server all share the same task database, so you (or an AI agent)
can manage tasks from any of the three interchangeably.

## Features

- **Projects**: tasks are grouped into projects, each with its own task list.
  The GUI opens on a project picker (choose an existing one or create a new
  one); the CLI defaults to whichever project was opened last, or you can
  target one explicitly with `project=<slug>`.
- Three columns per project: **Todo**, **In Progress**, **Completed**.
- Drag and drop cards between columns, or use the ‹ Move / Move › buttons.
- Categories (Feature, Bug, Security, Chore, Docs) with colored badges, plus
  a category filter and free-text search in the toolbar.
- Markdown descriptions (code fences included), rendered live in a preview
  tab and in the task detail view.
- The GUI polls the current project's database every 2 seconds, so changes
  made through the CLI while the window is open show up automatically.
- **MCP server**: an [MCP](https://modelcontextprotocol.io) server exposes
  project/task management as native tools an AI agent can call directly
  (create/list/move/edit/delete tasks, list/create projects) — see
  [MCP Server](#mcp-server) below.

## Requirements

- JDK 21+
- Linux (the packaged GUI jar bundles JavaFX's Linux native libraries; see
  [Packaging](#packaging) below if you need another OS).

## Building

```bash
./gradlew build
```

This builds all four modules: `core` (domain model + XML storage),
`gui` (JavaFX board), `cli` (command-line front-end), and `mcp` (MCP server).

## Running

**GUI:**

```bash
./gradlew :gui:run
# or, after building the jar:
java -jar gui/build/libs/kanban-gui.jar
```

**CLI:**

```bash
java -jar cli/build/libs/kanban-api.jar action=help
```

The CLI takes plain `key=value` arguments, in any order:

```bash
java -jar cli/build/libs/kanban-api.jar action=add title="Fix login bug" desc="Repro steps..." category=bug
java -jar cli/build/libs/kanban-api.jar action=list status=todo category=bug
java -jar cli/build/libs/kanban-api.jar action=move id=<id> status=in_progress
java -jar cli/build/libs/kanban-api.jar action=edit id=<id> title="New title"
java -jar cli/build/libs/kanban-api.jar action=delete id=<id>
java -jar cli/build/libs/kanban-api.jar action=projects
java -jar cli/build/libs/kanban-api.jar action=create-project name="Another Project"
```

Every action above also accepts `project=<slug>` to target a specific
project; when omitted, it falls back to the current project (whichever was
last opened in the GUI or selected via the CLI), auto-creating a "Default"
project on first-ever use so a fresh install never blocks on it.

All output goes to stdout on success and stderr on failure, with a
non-zero exit code on error, so it's safe to script against.

## MCP Server

For agents that speak [MCP](https://modelcontextprotocol.io) natively (Claude
Code, Claude Desktop, etc.), `kanban-mcp.jar` exposes the same operations as
the CLI as structured tools instead of `key=value` strings, over stdio:

- `list_projects`, `create_project`
- `list_tasks`, `get_task`, `add_task`, `move_task`, `edit_task`, `delete_task`

Every task tool accepts an optional `project` argument, falling back to the
current project exactly like the CLI does.

Build it with `./gradlew :mcp:jar` (or `./install.sh`, which installs it
alongside the other jars). Register it with Claude Code:

```bash
claude mcp add kanban -- java -jar /path/to/kanban-mcp.jar
```

Or add it directly to a `claude_desktop_config.json` (or equivalent MCP
client config):

```json
{
  "mcpServers": {
    "kanban": {
      "command": "java",
      "args": ["-jar", "/path/to/kanban-mcp.jar"]
    }
  }
}
```

## Installing (Linux)

```bash
./install.sh              # installs into ~/.local (or /opt with sudo)
./install.sh --uninstall  # removes everything the installer created
```

This builds the CLI, GUI and MCP server jars, installs a `kanban-cli`
launcher on your `PATH`, and adds a desktop entry + icon so the GUI shows up
in your application menu. The MCP server jar is installed but not
auto-registered with any MCP client — see [MCP Server](#mcp-server) above.

## Where tasks are stored

Each project gets its own directory under a per-OS data root
(`KanbanPaths`/`ProjectRegistry`):

- Linux: `$XDG_DATA_HOME/kanban/projects/<slug>/` (defaults to `~/.local/share/kanban/projects/<slug>/`)
- macOS: `~/Library/Application Support/Kanban/projects/<slug>/`
- Windows: `%APPDATA%\Kanban\projects\<slug>\`

Inside each project directory: `project.properties` (display name),
`kanban.xml` (tasks) and `kanban.xml.lock`. Which project is "current" is
tracked in a small `state.properties` file at the data root. Every
read/write is guarded by an OS-level file lock, so the GUI and any number
of concurrent CLI calls can't corrupt each other's writes.

If you used an earlier version of this app (single global `kanban.xml`,
no projects), it's migrated automatically and losslessly into a "Default"
project the first time you run the new version — nothing to do manually.

## Project structure

- `core/` — domain model (`Task`, `TaskStatus`, `TaskCategory`), the
  `KanbanRepository` that reads/writes a project's XML database, and
  `ProjectRegistry` (list/create projects, track the current one, migrate
  legacy single-file databases). No UI dependency.
- `gui/` — JavaFX app: `ProjectPickerView` (startup screen) and the board
  (`MainView`, `TaskCard`, `TaskDialog`, `TaskDetailView`) plus the
  `kanban.css` stylesheet.
- `cli/` — `KanbanCli`, a thin argument-parsing layer over `core`.
- `mcp/` — `KanbanMcpServer` (stdio transport wiring) and `KanbanTools`
  (the 7 tool handlers), another thin layer over `core`.

## Packaging

`gui/build.gradle.kts` bundles the JavaFX runtime for Linux directly into
`kanban-gui.jar`, so `java -jar kanban-gui.jar` works standalone without a
separate JavaFX install. To target macOS or Windows, change the
`setPlatform("linux")` call in the `javafx { ... }` block of that file to
build a jar with that platform's native libraries instead (one platform
per jar).
