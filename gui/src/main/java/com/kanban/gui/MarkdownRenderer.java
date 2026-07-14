package com.kanban.gui;

import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;

/**
 * Converts task-description markdown (code fences included) into HTML that
 * Swing's {@link javax.swing.JEditorPane} can render. Swing only understands
 * a subset of HTML/CSS, so the stylesheet below sticks to properties it
 * actually supports rather than modern CSS.
 */
final class MarkdownRenderer {

    private static final Parser PARSER = Parser.builder().build();
    private static final HtmlRenderer RENDERER = HtmlRenderer.builder().build();

    private static final String CSS = """
            body { font-family: SansSerif; font-size: 12px; color: #1F2328; }
            code { font-family: Monospaced; background-color: #EEF0F2; }
            pre { font-family: Monospaced; background-color: #F0F1F3; \
            border: 1px solid #DDDDDD; padding: 8px; }
            pre code { background-color: transparent; }
            h1, h2, h3 { color: #1F2328; }
            a { color: #0969DA; }
            blockquote { color: #57606A; border-left: 3px solid #DDDDDD; padding-left: 8px; }
            """;

    private MarkdownRenderer() {
    }

    static String toHtml(String markdown) {
        Node document = PARSER.parse(markdown == null ? "" : markdown);
        String body = RENDERER.render(document);
        return "<html><head><style>" + CSS + "</style></head><body>" + body + "</body></html>";
    }
}
