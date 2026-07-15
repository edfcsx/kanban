# Kanban

A simple, local-first kanban board. A JavaFX desktop app and a scriptable CLI
share the same task database, so you (or an AI assistant) can manage tasks
from either one interchangeably.

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

## Requirements

- JDK 21+
- Linux (the packaged GUI jar bundles JavaFX's Linux native libraries; see
  [Packaging](#packaging) below if you need another OS).

## Building

```bash
./gradlew build
```

This builds all three modules: `core` (domain model + XML storage),
`gui` (JavaFX board) and `cli` (command-line front-end).

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

## Installing (Linux)

```bash
./install.sh              # installs into ~/.local (or /opt with sudo)
./install.sh --uninstall  # removes everything the installer created
```

This builds the CLI and GUI jars, installs a `kanban-cli` launcher on your
`PATH`, and adds a desktop entry + icon so the GUI shows up in your
application menu.

## Where tasks are stored

Tasks live in a single XML file, resolved per-OS (`KanbanPaths`):

- Linux: `$XDG_DATA_HOME/kanban/kanban.xml` (defaults to `~/.local/share/kanban/kanban.xml`)
- macOS: `~/Library/Application Support/Kanban/kanban.xml`
- Windows: `%APPDATA%\Kanban\kanban.xml`

Every read/write is guarded by an OS-level file lock, so the GUI and any
number of concurrent CLI calls can't corrupt each other's writes.

## Project structure

- `core/` — domain model (`Task`, `TaskStatus`, `TaskCategory`) and the
  `KanbanRepository` that reads/writes the XML database. No UI dependency.
- `gui/` — JavaFX board (`MainView`, `TaskCard`, `TaskDialog`,
  `TaskDetailView`) plus the `kanban.css` stylesheet.
- `cli/` — `KanbanCli`, a thin argument-parsing layer over `core`.

## Packaging

`gui/build.gradle.kts` bundles the JavaFX runtime for Linux directly into
`kanban-gui.jar`, so `java -jar kanban-gui.jar` works standalone without a
separate JavaFX install. To target macOS or Windows, change the
`setPlatform("linux")` call in the `javafx { ... }` block of that file to
build a jar with that platform's native libraries instead (one platform
per jar).
