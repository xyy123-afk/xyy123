package com.fenglin.springboottest.util;

import org.commonmark.Extension;
import org.commonmark.ext.gfm.tables.TablesExtension;
import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Markdown -> HTML 渲染器（服务端渲染，结果直接入库 posts.content_html）。
 *
 * <p>为什么放服务端：详情页拿到 HTML 直接输出，浏览器不用等 JS 渲染，SEO 也友好；
 * 编辑器里的实时预览是另一条链路（前端 markdown-it），两者互不影响。
 *
 * <p>XSS：escapeHtml(true) 会把 Markdown 里的原始 HTML 标签转义成纯文本，
 * 所以别人在正文里写 &lt;script&gt; 只会显示成文字，不会被执行。
 * 这样就不需要再引 sanitizer 依赖。
 */
@Component
public class MarkdownRenderer {

    private final Parser parser;
    private final HtmlRenderer renderer;

    public MarkdownRenderer() {
        List<Extension> extensions = List.of(TablesExtension.create());
        this.parser = Parser.builder().extensions(extensions).build();
        this.renderer = HtmlRenderer.builder()
                .extensions(extensions)
                .escapeHtml(true)
                .build();
    }

    /** 渲染 Markdown；空内容返回空串（草稿允许正文为空，但存库字段是 NOT NULL） */
    public String render(String markdown) {
        if (markdown == null || markdown.isBlank()) {
            return "";
        }
        Node document = parser.parse(markdown);
        return renderer.render(document);
    }

    /**
     * 从 Markdown 里粗略抽出纯文本摘要，作者没填摘要时自动生成。
     * 只做去标记 + 截断，不追求完美——真的想控制摘要，就在编辑器里手填。
     */
    public String autoSummary(String markdown, int maxLength) {
        if (markdown == null || markdown.isBlank()) {
            return "";
        }
        String text = markdown
                .replaceAll("(?s)```.*?```", " ")              // 代码块整体去掉
                .replaceAll("<[^>]*>", " ")                     // 正文里写的原始 HTML 标签
                .replaceAll("`[^`]*`", " ")                      // 行内代码
                .replaceAll("!\\[[^\\]]*\\]\\([^)]*\\)", " ")    // 图片
                .replaceAll("\\[([^\\]]*)\\]\\([^)]*\\)", "$1")  // 链接只留文字
                .replaceAll("(?m)^\\s{0,3}#{1,6}\\s*", "")       // 标题符号
                .replaceAll("(?m)^\\s*[-*+]\\s+", "")            // 列表符号
                .replaceAll("(?m)^\\s*\\d+\\.\\s+", "")          // 有序列表
                .replaceAll("[*_~>|<>\"/\\\\]", " ")
                .replaceAll("\\s+", " ")
                .trim();
        return text.length() <= maxLength ? text : text.substring(0, maxLength) + "…";
    }
}
