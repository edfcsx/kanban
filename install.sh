#!/usr/bin/env bash
#
# Installer for Kanban (CLI + GUI + MCP server) on Linux.
#
#   ./install.sh            builds the project and installs it
#   ./install.sh --uninstall   removes everything the installer created
#
# Run as a normal user to install into $HOME (~/.local/...), or with sudo
# to install system-wide (/opt, /usr/local/bin, /usr/share/...).

set -euo pipefail

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" &>/dev/null && pwd)"
ICON_NAME="kanban"
ICON_SOURCE="$SCRIPT_DIR/assets/icon/kanban.svg"

if [[ "${EUID}" -eq 0 ]]; then
    APP_DIR="/opt/kanban"
    BIN_DIR="/usr/local/bin"
    DESKTOP_DIR="/usr/share/applications"
    ICON_DIR="/usr/share/icons/hicolor/scalable/apps"
else
    APP_DIR="$HOME/.local/share/kanban"
    BIN_DIR="$HOME/.local/bin"
    DESKTOP_DIR="$HOME/.local/share/applications"
    ICON_DIR="$HOME/.local/share/icons/hicolor/scalable/apps"
fi

log()  { printf '==> %s\n' "$1"; }
warn() { printf 'WARNING: %s\n' "$1" >&2; }

uninstall() {
    log "Removing Kanban from ${APP_DIR}, ${BIN_DIR}, ${DESKTOP_DIR}, ${ICON_DIR}"
    rm -rf "$APP_DIR"
    rm -f "$BIN_DIR/kanban-cli"
    rm -f "$DESKTOP_DIR/kanban.desktop"
    rm -f "$ICON_DIR/${ICON_NAME}.svg"
    command -v update-desktop-database &>/dev/null && update-desktop-database "$DESKTOP_DIR" &>/dev/null || true
    command -v gtk-update-icon-cache &>/dev/null && gtk-update-icon-cache -f -t "$(dirname "$(dirname "$ICON_DIR")")" &>/dev/null || true
    log "Uninstalled."
    exit 0
}

[[ "${1:-}" == "--uninstall" ]] && uninstall

command -v java &>/dev/null || { warn "Java is not installed or not on PATH. Install a JDK 21+ and re-run."; exit 1; }

log "Building CLI, GUI and MCP server jars with Gradle"
"$SCRIPT_DIR/gradlew" -p "$SCRIPT_DIR" :cli:jar :gui:jar :mcp:jar

CLI_JAR="$SCRIPT_DIR/cli/build/libs/kanban-api.jar"
GUI_JAR="$SCRIPT_DIR/gui/build/libs/kanban-gui.jar"
MCP_JAR="$SCRIPT_DIR/mcp/build/libs/kanban-mcp.jar"
[[ -f "$CLI_JAR" ]] || { warn "Build did not produce $CLI_JAR"; exit 1; }
[[ -f "$GUI_JAR" ]] || { warn "Build did not produce $GUI_JAR"; exit 1; }
[[ -f "$MCP_JAR" ]] || { warn "Build did not produce $MCP_JAR"; exit 1; }

log "Installing jars to $APP_DIR"
mkdir -p "$APP_DIR"
cp -f "$CLI_JAR" "$GUI_JAR" "$MCP_JAR" "$APP_DIR/"

log "Installing kanban-cli launcher to $BIN_DIR"
mkdir -p "$BIN_DIR"
cat > "$BIN_DIR/kanban-cli" <<EOF
#!/usr/bin/env bash
exec java -jar "$APP_DIR/kanban-api.jar" "\$@"
EOF
chmod +x "$BIN_DIR/kanban-cli"

mkdir -p "$ICON_DIR"
if [[ -f "$ICON_SOURCE" ]]; then
    log "Installing bundled icon (Candy Icons style) to $ICON_DIR"
    cp -f "$ICON_SOURCE" "$ICON_DIR/${ICON_NAME}.svg"
else
    warn "Bundled icon not found at $ICON_SOURCE, falling back to a generic icon"
    cat > "$ICON_DIR/${ICON_NAME}.svg" <<'EOF'
<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 48 48" width="48" height="48">
  <rect x="4" y="6" width="40" height="36" rx="4" fill="#20bdff"/>
  <rect x="8" y="12" width="10" height="24" rx="2" fill="#ffffff" fill-opacity=".9"/>
  <rect x="19" y="12" width="10" height="16" rx="2" fill="#ffffff" fill-opacity=".9"/>
  <rect x="30" y="12" width="10" height="20" rx="2" fill="#ffffff" fill-opacity=".9"/>
</svg>
EOF
fi

log "Installing desktop entry to $DESKTOP_DIR"
mkdir -p "$DESKTOP_DIR"
cat > "$DESKTOP_DIR/kanban.desktop" <<EOF
[Desktop Entry]
Type=Application
Name=Kanban
Comment=Simple kanban board manager
Exec=java -jar $APP_DIR/kanban-gui.jar
Icon=$ICON_NAME
Terminal=false
Categories=Office;ProjectManagement;
StartupWMClass=com.kanban.gui.KanbanApp
EOF

command -v update-desktop-database &>/dev/null && update-desktop-database "$DESKTOP_DIR" &>/dev/null || true
command -v gtk-update-icon-cache &>/dev/null && gtk-update-icon-cache -f -t "$(dirname "$(dirname "$ICON_DIR")")" &>/dev/null || true

if [[ "${EUID}" -ne 0 ]] && [[ ":$PATH:" != *":$BIN_DIR:"* ]]; then
    warn "$BIN_DIR is not on your PATH."
    warn "Add this to your ~/.bashrc or ~/.profile: export PATH=\"\$HOME/.local/bin:\$PATH\""
fi

log "Done. Launch the GUI from your application menu, or run: kanban-cli action=help"
log "MCP server jar installed at $APP_DIR/kanban-mcp.jar - see README.md to register it with an MCP client"