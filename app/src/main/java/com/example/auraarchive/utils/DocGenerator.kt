package com.example.auraarchive.utils

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.example.auraarchive.network.model.AuraPost
import java.io.File
import java.io.FileOutputStream

object DocGenerator {

    private const val GOOGLE_DOCS_PACKAGE = "com.google.android.apps.docs"

    fun generateAndOpenInDocs(context: Context, post: AuraPost) {
        val title = post.title ?: "Untitled"

        val htmlBody = """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <style>
                    body { font-family: sans-serif; line-height: 1.6; color: #171C1E; margin: 40px; }
                    h1 { color: #00687A; margin-bottom: 5px; border-bottom: 1px solid #ADECFF; }
                    h2, h3 { color: #334A51; margin-top: 20px; margin-bottom: 5px; }
                    p { margin: 0 0 10px 0; }
                    .summary-box { background-color: #F5FAFC; border: 1px solid #ADECFF; padding: 15px; margin: 20px 0; font-style: italic; }
                    footer { margin-top: 40px; font-size: 0.8em; color: #70797C; border-top: 1px solid #BFC8CB; padding-top: 10px; }
                </style>
            </head>
            <body>
                <h1>$title</h1>
                <div class="summary-box">
                    <strong>Summary:</strong><br/>
                    ${simpleMarkdownToHtml(post.summary ?: "")}
                </div>
                <div class="content">
                    ${simpleMarkdownToHtml(post.content ?: "")}
                </div>
                <footer>Generated via Aura Archive</footer>
            </body>
            </html>
        """.trimIndent()

        val file = File(context.cacheDir, "${title.replace(" ", "_")}.html")

        try {
            FileOutputStream(file).use { it.write(htmlBody.toByteArray()) }

            val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/html"
                putExtra(Intent.EXTRA_STREAM, uri)
                setPackage(GOOGLE_DOCS_PACKAGE)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                putExtra(Intent.EXTRA_TITLE, title)
            }

            context.startActivity(intent)

        } catch (e: Exception) {
            val fallback = Intent(Intent.ACTION_SEND).apply {
                type = "text/html"
                putExtra(Intent.EXTRA_STREAM, FileProvider.getUriForFile(context, "${context.packageName}.provider", file))
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(fallback, "Open with..."))
        }
    }

    private fun simpleMarkdownToHtml(markdown: String): String {
        return markdown
            .replace(Regex("(?m)^# (.*)$"), "<h1>$1</h1>")
            .replace(Regex("(?m)^## (.*)$"), "<h2>$1</h2>")
            .replace(Regex("(?m)^### (.*)$"), "<h3>$1</h3>")
            .replace(Regex("\\*\\*(.*?)\\*\\*"), "<strong>$1</strong>")
            .replace(Regex("\\*(.*?)\\*"), "<em>$1</em>")
            .split("\n")
            .filter { it.isNotBlank() }
            .joinToString("") { "<p>$it</p>" }
    }
}