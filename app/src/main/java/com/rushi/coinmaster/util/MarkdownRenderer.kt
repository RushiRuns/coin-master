package com.rushi.coinmaster.util

import android.text.Spanned
import androidx.core.text.HtmlCompat

object MarkdownRenderer {
    /**
     * Converts a basic Markdown string into a Spanned object for rendering in a TextView.
     * Supports:
     * - Headings: # (H3), ## (H4), ### (H5)
     * - Bold: **text** or __text__
     * - Italic: *text* or _text_
     * - Inline code: `code`
     * - Bullet points: - item or * item
     * - Links: [text](url)
     */
    fun render(markdown: String): Spanned {
        if (markdown.isBlank()) {
            return HtmlCompat.fromHtml("", HtmlCompat.FROM_HTML_MODE_LEGACY)
        }

        val lines = markdown.split("\n")
        val processedLines = mutableListOf<String>()

        for (line in lines) {
            val trimmed = line.trim()
            var processedLine: String

            if (trimmed.startsWith("# ")) {
                processedLine = "<h3>${escapeHtml(trimmed.substring(2))}</h3>"
            } else if (trimmed.startsWith("## ")) {
                processedLine = "<h4>${escapeHtml(trimmed.substring(3))}</h4>"
            } else if (trimmed.startsWith("### ")) {
                processedLine = "<h5>${escapeHtml(trimmed.substring(4))}</h5>"
            } else if (trimmed.startsWith("- ") || trimmed.startsWith("* ")) {
                processedLine = "&bull; ${escapeHtml(trimmed.substring(2))}"
            } else {
                processedLine = escapeHtml(line)
            }

            processedLine = replaceInlineMarkdown(processedLine)
            processedLines.add(processedLine)
        }

        val htmlContent = processedLines.joinToString("<br/>")
        return HtmlCompat.fromHtml(htmlContent, HtmlCompat.FROM_HTML_MODE_LEGACY)
    }

    private fun escapeHtml(text: String): String {
        return text
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
    }

    private fun replaceInlineMarkdown(html: String): String {
        var result = html
        // Bold: **text** or __text__
        result = result.replace(Regex("\\*\\*(.*?)\\*\\*"), "<b>$1</b>")
        result = result.replace(Regex("__(.*?)__"), "<b>$1</b>")
        
        // Italic: *text* or _text_
        result = result.replace(Regex("\\*(.*?)\\*"), "<i>$1</i>")
        result = result.replace(Regex("_(.*?)_"), "<i>$1</i>")

        // Inline Code: `text`
        result = result.replace(Regex("`(.*?)`"), "<font face=\"monospace\" color=\"#FF8A65\">$1</font>")

        // Links: [text](url)
        result = result.replace(Regex("\\[(.*?)\\]\\((.*?)\\)"), "<a href=\"$2\">$1</a>")

        return result
    }
}
