package com.kanban.core;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Properties;

/**
 * Tracks the set of projects (each its own task database directory under
 * {@code projects/<slug>/}) and which one is "current" — the project the
 * CLI falls back to when a caller doesn't pass {@code project=}, and the
 * one the GUI reopens on next launch.
 */
public final class ProjectRegistry {

    private static final String DEFAULT_PROJECT_NAME = "Default";
    private static final String PROJECT_PROPERTIES_FILE = "project.properties";
    private static final String NAME_KEY = "name";
    private static final String CURRENT_PROJECT_KEY = "currentProject";

    private final File projectsDir;
    private final File stateFile;
    private final File legacyDbFile;
    private final File legacyLockFile;

    public ProjectRegistry() {
        this(KanbanPaths.projectsDirectory(), KanbanPaths.stateFile(),
                KanbanPaths.databaseFile(), KanbanPaths.lockFile());
    }

    public ProjectRegistry(File projectsDir, File stateFile, File legacyDbFile, File legacyLockFile) {
        this.projectsDir = projectsDir;
        this.stateFile = stateFile;
        this.legacyDbFile = legacyDbFile;
        this.legacyLockFile = legacyLockFile;
    }

    public List<Project> listProjects() {
        List<Project> projects = new ArrayList<>();
        File[] dirs = projectsDir.listFiles(File::isDirectory);
        if (dirs == null) {
            return projects;
        }
        for (File dir : dirs) {
            File propsFile = new File(dir, PROJECT_PROPERTIES_FILE);
            if (!propsFile.exists()) {
                continue;
            }
            projects.add(new Project(dir.getName(), readDisplayName(propsFile, dir.getName())));
        }
        projects.sort(Comparator.comparing(Project::displayName, String.CASE_INSENSITIVE_ORDER));
        return projects;
    }

    public Project createProject(String displayName) {
        if (displayName == null || displayName.isBlank()) {
            throw new IllegalArgumentException("project name is required");
        }
        String trimmed = displayName.trim();
        String slug = uniqueSlug(slugify(trimmed));
        File dir = new File(projectsDir, slug);
        if (!dir.mkdirs()) {
            throw new KanbanStorageException("Failed to create project directory " + dir, null);
        }
        writeDisplayName(new File(dir, PROJECT_PROPERTIES_FILE), trimmed);
        Project project = new Project(slug, trimmed);
        setCurrentProject(project);
        return project;
    }

    public Optional<Project> findBySlug(String slug) {
        return listProjects().stream().filter(p -> p.slug().equals(slug)).findFirst();
    }

    public File taskDbFile(Project project) {
        return new File(new File(projectsDir, project.slug()), "kanban.xml");
    }

    public File taskLockFile(Project project) {
        return new File(new File(projectsDir, project.slug()), "kanban.xml.lock");
    }

    public Optional<Project> currentProject() {
        if (!stateFile.exists()) {
            return Optional.empty();
        }
        Properties props = new Properties();
        try (InputStream in = Files.newInputStream(stateFile.toPath())) {
            props.load(in);
        } catch (IOException e) {
            throw new KanbanStorageException("Failed to read " + stateFile, e);
        }
        String slug = props.getProperty(CURRENT_PROJECT_KEY);
        return slug == null ? Optional.empty() : findBySlug(slug);
    }

    public void setCurrentProject(Project project) {
        Properties props = new Properties();
        props.setProperty(CURRENT_PROJECT_KEY, project.slug());
        try {
            File parent = stateFile.getParentFile();
            if (parent != null) {
                Files.createDirectories(parent.toPath());
            }
            try (OutputStream out = Files.newOutputStream(stateFile.toPath())) {
                props.store(out, null);
            }
        } catch (IOException e) {
            throw new KanbanStorageException("Failed to write " + stateFile, e);
        }
    }

    /**
     * One-time migration: if this is the first run after adding projects
     * (no projects/ directory yet) and the old single-file database exists,
     * moves it into a new "Default" project instead of silently orphaning
     * the user's existing tasks.
     */
    public Optional<Project> migrateLegacyDatabaseIfNeeded() {
        if (projectsDir.exists() || !legacyDbFile.exists()) {
            return Optional.empty();
        }
        Project defaultProject = createProject(DEFAULT_PROJECT_NAME);
        moveIfExists(legacyDbFile, taskDbFile(defaultProject));
        moveIfExists(legacyLockFile, taskLockFile(defaultProject));
        return Optional.of(defaultProject);
    }

    private void moveIfExists(File source, File target) {
        if (!source.exists()) {
            return;
        }
        try {
            Files.move(source.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new KanbanStorageException("Failed to migrate " + source + " to " + target, e);
        }
    }

    private String uniqueSlug(String base) {
        String candidate = base;
        int suffix = 2;
        while (new File(projectsDir, candidate).exists()) {
            candidate = base + "-" + suffix++;
        }
        return candidate;
    }

    private static String slugify(String name) {
        String slug = name.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-+|-+$", "");
        return slug.isEmpty() ? "project" : slug;
    }

    private static String readDisplayName(File propsFile, String fallback) {
        Properties props = new Properties();
        try (InputStream in = Files.newInputStream(propsFile.toPath())) {
            props.load(in);
        } catch (IOException e) {
            return fallback;
        }
        String name = props.getProperty(NAME_KEY);
        return name == null || name.isBlank() ? fallback : name;
    }

    private static void writeDisplayName(File propsFile, String name) {
        Properties props = new Properties();
        props.setProperty(NAME_KEY, name);
        try (OutputStream out = Files.newOutputStream(propsFile.toPath())) {
            props.store(out, null);
        } catch (IOException e) {
            throw new KanbanStorageException("Failed to write " + propsFile, e);
        }
    }
}
