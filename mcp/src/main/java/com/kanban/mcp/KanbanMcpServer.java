package com.kanban.mcp;

import com.kanban.core.ProjectRegistry;

import io.modelcontextprotocol.json.McpJsonDefaults;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.server.McpSyncServer;
import io.modelcontextprotocol.server.transport.StdioServerTransportProvider;
import io.modelcontextprotocol.spec.McpSchema;

import java.util.concurrent.CountDownLatch;

/**
 * Entry point for the Kanban MCP server: exposes the same task/project
 * operations as the CLI as native MCP tools an agent can call directly, over
 * stdio (the transport hosts like Claude Code use when spawning a local MCP
 * server as a subprocess).
 *
 * <p>stdout is reserved for JSON-RPC protocol frames written by the
 * transport; nothing else in this process may write to it, so all
 * diagnostics go to stderr.
 */
public final class KanbanMcpServer {

    public static void main(String[] args) throws InterruptedException {
        ProjectRegistry registry = new ProjectRegistry();
        registry.migrateLegacyDatabaseIfNeeded();

        StdioServerTransportProvider transportProvider =
                new StdioServerTransportProvider(McpJsonDefaults.getMapper());

        McpSyncServer server = McpServer.sync(transportProvider)
                .serverInfo("kanban", "1.0.0")
                .capabilities(McpSchema.ServerCapabilities.builder().tools(true).build())
                .build();

        for (McpServerFeatures.SyncToolSpecification tool : new KanbanTools(registry).specifications()) {
            server.addTool(tool);
        }

        CountDownLatch shutdownLatch = new CountDownLatch(1);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            server.close();
            shutdownLatch.countDown();
        }));

        System.err.println("Kanban MCP server ready (stdio).");
        shutdownLatch.await();
    }

    private KanbanMcpServer() {
    }
}
