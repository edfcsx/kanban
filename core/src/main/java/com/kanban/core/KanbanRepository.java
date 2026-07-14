package com.kanban.core;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.function.Consumer;

/**
 * Reads and writes tasks to a single XML file. Every operation is wrapped in
 * an OS-level {@link FileLock} on a companion lock file, so the Swing app
 * and any number of concurrent CLI invocations (e.g. an AI assistant calling
 * the CLI repeatedly) can never interleave a read-modify-write cycle.
 * Writes go to a temp file followed by an atomic move, so a reader never
 * observes a half-written document.
 */
public final class KanbanRepository {

    private final File dbFile;
    private final File lockFile;

    public KanbanRepository() {
        this(KanbanPaths.databaseFile(), KanbanPaths.lockFile());
    }

    public KanbanRepository(File dbFile, File lockFile) {
        this.dbFile = dbFile;
        this.lockFile = lockFile;
    }

    public File getDatabaseFile() {
        return dbFile;
    }

    public List<Task> loadAll() {
        return withLock(this::readTasks);
    }

    public Task findById(String id) {
        return withLock(() -> readTasks().stream()
                .filter(t -> t.getId().equals(id))
                .findFirst()
                .orElse(null));
    }

    public Task addTask(String title, String description) {
        return addTask(title, description, TaskCategory.NONE);
    }

    public Task addTask(String title, String description, TaskCategory category) {
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("title is required");
        }
        return withLock(() -> {
            List<Task> tasks = readTasks();
            Instant now = Instant.now();
            Task task = new Task(UUID.randomUUID().toString(), title.trim(),
                    description == null ? "" : description, TaskStatus.TODO, category, now, now);
            tasks.add(task);
            writeTasks(tasks);
            return task;
        });
    }

    public boolean updateStatus(String id, TaskStatus status) {
        return withLock(() -> mutate(id, task -> task.setStatus(status)));
    }

    public boolean updateTask(String id, String newTitle, String newDescription) {
        return updateTask(id, newTitle, newDescription, null);
    }

    public boolean updateTask(String id, String newTitle, String newDescription, TaskCategory newCategory) {
        return withLock(() -> mutate(id, task -> {
            if (newTitle != null && !newTitle.isBlank()) {
                task.setTitle(newTitle.trim());
            }
            if (newDescription != null) {
                task.setDescription(newDescription);
            }
            if (newCategory != null) {
                task.setCategory(newCategory);
            }
        }));
    }

    public boolean deleteTask(String id) {
        return withLock(() -> {
            List<Task> tasks = readTasks();
            boolean removed = tasks.removeIf(t -> t.getId().equals(id));
            if (removed) {
                writeTasks(tasks);
            }
            return removed;
        });
    }

    private boolean mutate(String id, Consumer<Task> mutation) {
        List<Task> tasks = readTasks();
        for (Task task : tasks) {
            if (task.getId().equals(id)) {
                mutation.accept(task);
                task.setUpdatedAt(Instant.now());
                writeTasks(tasks);
                return true;
            }
        }
        return false;
    }

    private <T> T withLock(Callable<T> action) {
        File parent = lockFile.getParentFile();
        if (parent != null) {
            parent.mkdirs();
        }
        try (RandomAccessFile raf = new RandomAccessFile(lockFile, "rw");
             FileChannel channel = raf.getChannel();
             FileLock lock = channel.lock()) {
            return action.call();
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new KanbanStorageException("Failed to access kanban database", e);
        }
    }

    private List<Task> readTasks() {
        List<Task> tasks = new ArrayList<>();
        if (!dbFile.exists()) {
            return tasks;
        }
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setXIncludeAware(false);
            factory.setExpandEntityReferences(false);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(dbFile);
            doc.getDocumentElement().normalize();

            NodeList nodes = doc.getElementsByTagName("task");
            for (int i = 0; i < nodes.getLength(); i++) {
                Element el = (Element) nodes.item(i);
                String id = el.getAttribute("id");
                TaskStatus status = TaskStatus.valueOf(el.getAttribute("status"));
                String categoryAttr = el.getAttribute("category");
                TaskCategory category = categoryAttr.isBlank() ? TaskCategory.NONE : TaskCategory.valueOf(categoryAttr);
                Instant createdAt = Instant.parse(el.getAttribute("createdAt"));
                Instant updatedAt = Instant.parse(el.getAttribute("updatedAt"));
                String title = textOf(el, "title");
                String description = textOf(el, "description");
                tasks.add(new Task(id, title, description, status, category, createdAt, updatedAt));
            }
        } catch (Exception e) {
            throw new KanbanStorageException("Failed to read kanban database at " + dbFile, e);
        }
        return tasks;
    }

    private static String textOf(Element parent, String tag) {
        NodeList list = parent.getElementsByTagName(tag);
        if (list.getLength() == 0) {
            return "";
        }
        Node node = list.item(0).getFirstChild();
        return node != null ? node.getNodeValue() : "";
    }

    private void writeTasks(List<Task> tasks) {
        File parent = dbFile.getParentFile();
        try {
            if (parent != null) {
                parent.mkdirs();
            }

            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.newDocument();
            Element root = doc.createElement("kanban");
            doc.appendChild(root);

            for (Task task : tasks) {
                Element taskEl = doc.createElement("task");
                taskEl.setAttribute("id", task.getId());
                taskEl.setAttribute("status", task.getStatus().name());
                taskEl.setAttribute("category", task.getCategory().name());
                taskEl.setAttribute("createdAt", task.getCreatedAt().toString());
                taskEl.setAttribute("updatedAt", task.getUpdatedAt().toString());

                Element titleEl = doc.createElement("title");
                titleEl.setTextContent(task.getTitle());
                Element descEl = doc.createElement("description");
                descEl.setTextContent(task.getDescription());

                taskEl.appendChild(titleEl);
                taskEl.appendChild(descEl);
                root.appendChild(taskEl);
            }

            Transformer transformer = TransformerFactory.newInstance().newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");

            File tempFile = File.createTempFile("kanban", ".xml.tmp", parent != null ? parent : new File("."));
            try (OutputStream out = Files.newOutputStream(tempFile.toPath())) {
                transformer.transform(new DOMSource(doc), new StreamResult(out));
            }

            try {
                Files.move(tempFile.toPath(), dbFile.toPath(),
                        StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(tempFile.toPath(), dbFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (Exception e) {
            throw new KanbanStorageException("Failed to write kanban database at " + dbFile, e);
        }
    }
}