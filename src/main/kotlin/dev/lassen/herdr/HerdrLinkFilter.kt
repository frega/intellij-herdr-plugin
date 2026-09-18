package dev.lassen.herdr

import com.intellij.ide.BrowserUtil
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.jediterm.terminal.model.hyperlinks.HyperlinkFilter
import com.jediterm.terminal.model.hyperlinks.LinkInfo
import com.jediterm.terminal.model.hyperlinks.LinkResult
import com.jediterm.terminal.model.hyperlinks.LinkResultItem
import java.io.File

private val URL_PATTERN = Regex("""https?://\S+""")

// Path-like token (needs at least one `/`), optionally followed by :line or :line:column.
// Loose on purpose: a match only becomes a link once it resolves to a real file under the project root.
private val PATH_PATTERN = Regex("""(?:[\w.-]+/)+[\w.-]+(?::(\d+)(?::(\d+))?)?""")

private val URL_TRAILING_PUNCTUATION = ".,;:!?\"'".toSet()
private val URL_UNBALANCED_CLOSERS = mapOf(')' to '(', ']' to '[', '}' to '{')

/**
 * Prose/markdown often wraps or follows a URL with punctuation (e.g. `(https://example.com/x)` or
 * `see https://example.com/x.`), which `\S+` happily swallows. Strip trailing sentence punctuation
 * unconditionally, and a trailing bracket only if it's unbalanced within the match, so URLs that
 * legitimately end in one (e.g. a Wikipedia `_(disambiguation)` link) are left alone.
 */
private fun trimTrailingPunctuation(url: String): String {
    var end = url.length
    while (end > 0) {
        val c = url[end - 1]
        val opener = URL_UNBALANCED_CLOSERS[c]
        end = when {
            c in URL_TRAILING_PUNCTUATION -> end - 1
            opener != null && url.count { it == c } > url.count { it == opener } -> end - 1
            else -> break
        }
    }
    return url.substring(0, end)
}

/** Makes URLs open in the system browser, and project file paths (optionally `path:line:col`) open in the editor. */
class HerdrLinkFilter(private val project: Project) : HyperlinkFilter {

    override fun apply(line: String): LinkResult? {
        val items = mutableListOf<LinkResultItem>()

        for (match in URL_PATTERN.findAll(line)) {
            val url = trimTrailingPunctuation(match.value)
            if (url.isEmpty()) continue
            val end = match.range.first + url.length
            items += LinkResultItem(match.range.first, end, LinkInfo { BrowserUtil.browse(url) })
        }

        for (match in PATH_PATTERN.findAll(line)) {
            val rawPath = match.value.substringBefore(':')
            val file = resolveProjectFile(rawPath) ?: continue
            val lineNumber = match.groupValues.getOrNull(1)?.toIntOrNull()
            val column = match.groupValues.getOrNull(2)?.toIntOrNull()
            items += LinkResultItem(match.range.first, match.range.last + 1, LinkInfo {
                openInEditor(file, lineNumber, column)
            })
        }

        return items.takeIf { it.isNotEmpty() }?.let { LinkResult(it) }
    }

    private fun resolveProjectFile(rawPath: String): File? {
        val base = project.basePath ?: return null
        val candidate = File(rawPath).let { if (it.isAbsolute) it else File(base, rawPath) }
        val canonicalBase = File(base).canonicalFile
        val canonicalCandidate = runCatching { candidate.canonicalFile }.getOrNull() ?: return null
        if (!canonicalCandidate.path.startsWith(canonicalBase.path + File.separator)) return null
        return canonicalCandidate.takeIf { it.isFile }
    }

    private fun openInEditor(file: File, line: Int?, column: Int?) {
        ApplicationManager.getApplication().invokeLater {
            val virtualFile = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(file) ?: return@invokeLater
            val lineIndex = ((line ?: 1) - 1).coerceAtLeast(0)
            val columnIndex = ((column ?: 1) - 1).coerceAtLeast(0)
            OpenFileDescriptor(project, virtualFile, lineIndex, columnIndex).navigate(true)
        }
    }
}
