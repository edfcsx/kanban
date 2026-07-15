package com.kanban.gui;

import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;

/**
 * Converts task-description markdown (code fences included) into HTML shown
 * inside a {@link javafx.scene.web.WebView}, which renders modern CSS
 * properly (unlike the old Swing HTML kit), so the stylesheet below can use
 * real border-radius, shadows and web-safe fonts.
 */
final class MarkdownRenderer {

    private static final Parser PARSER = Parser.builder().build();
    private static final HtmlRenderer RENDERER = HtmlRenderer.builder().build();

    private static final String CSS = """
            body {
                font-family: -apple-system, "Segoe UI", "Noto Sans", sans-serif;
                font-size: 13px;
                color: #1F2328;
                margin: 0;
                padding: 2px 4px;
                line-height: 1.5;
            }
            code {
                font-family: "Cascadia Code", "Consolas", monospace;
                background-color: #EEF0F2;
                border-radius: 4px;
                padding: 1px 5px;
                font-size: 12px;
            }
            pre {
                background-color: #F6F8FA;
                border: 1px solid #DDDDDD;
                border-radius: 8px;
                padding: 10px 12px;
                overflow-x: auto;
            }
            pre code {
                background-color: transparent;
                padding: 0;
            }
            h1, h2, h3 { color: #1F2328; }
            a { color: #2F6FED; }
            blockquote {
                color: #57606A;
                border-left: 3px solid #DDDDDD;
                margin-left: 0;
                padding-left: 10px;
            }
            table { border-collapse: collapse; }
            th, td { border: 1px solid #DDDDDD; padding: 4px 8px; }
            """;

    private MarkdownRenderer() {
    }

    static String toHtml(String markdown) {
        Node document = PARSER.parse(markdown == null ? "" : markdown);
        String body = RENDERER.render(document);
        return "<html><head><style>" + CSS + "</style></head><body>" + body + "</body></html>";
    }
}
