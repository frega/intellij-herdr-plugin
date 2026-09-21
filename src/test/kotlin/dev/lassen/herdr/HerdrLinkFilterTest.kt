package dev.lassen.herdr

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * What a line of terminal output gets detected as (a URL/path span), and what that span
 * actually resolves to (the target it would open) if clicked. Kept as plain JUnit tests
 * against the pure detectXxx functions, so they run without an IntelliJ Project/platform fixture.
 */
class HerdrLinkFilterTest {

    private fun targets(line: String) = detectUrlLinks(line).map { it.target }

    @Test
    fun `bare url with no surrounding text`() {
        val line = "https://example.com/x"
        assertEquals(listOf("https://example.com/x"), targets(line))
    }

    @Test
    fun `url followed by comma then more prose`() {
        // Reproduces the release-notes regression: "...0.7.3, with `foo.zip` attached..."
        val line = "Release is live: https://example.com/releases/0.7.3, with more text after."
        assertEquals(listOf("https://example.com/releases/0.7.3"), targets(line))
    }

    @Test
    fun `url followed by sentence-ending period`() {
        val line = "See https://example.com/x."
        assertEquals(listOf("https://example.com/x"), targets(line))
    }

    @Test
    fun `url followed by multiple trailing punctuation marks`() {
        val line = "Whoa, check this out: https://example.com/x!!"
        assertEquals(listOf("https://example.com/x"), targets(line))
    }

    @Test
    fun `url wrapped in parentheses loses the unbalanced closing paren`() {
        val line = "Report here (https://example.com/x)"
        assertEquals(listOf("https://example.com/x"), targets(line))
    }

    @Test
    fun `url ending in a balanced parenthetical keeps it`() {
        val line = "See https://en.wikipedia.org/wiki/Foo_(disambiguation)"
        assertEquals(listOf("https://en.wikipedia.org/wiki/Foo_(disambiguation)"), targets(line))
    }

    @Test
    fun `url ending in a balanced parenthetical still loses trailing prose punctuation`() {
        val line = "See https://en.wikipedia.org/wiki/Foo_(disambiguation)."
        assertEquals(listOf("https://en.wikipedia.org/wiki/Foo_(disambiguation)"), targets(line))
    }

    @Test
    fun `multiple urls on one line are each trimmed independently`() {
        val line = "Compare https://example.com/a, and https://example.com/b."
        assertEquals(
            listOf("https://example.com/a", "https://example.com/b"),
            targets(line),
        )
    }

    @Test
    fun `detected span excludes the trimmed trailing punctuation`() {
        val line = "See https://example.com/x, ok?"
        val link = detectUrlLinks(line).single()
        assertEquals(line.substring(link.start, link.end), link.target)
        assertEquals("https://example.com/x", line.substring(link.start, link.end))
    }

    @Test
    fun `no urls on a plain line`() {
        val line = "just some regular output, nothing to see"
        assertEquals(emptyList<String>(), targets(line))
    }

    @Test
    fun `path token detection extracts line and column`() {
        val token = detectPathTokens("open src/main/kotlin/Foo.kt:12:5 please").single()
        assertEquals("src/main/kotlin/Foo.kt", token.rawPath)
        assertEquals(12, token.line)
        assertEquals(5, token.column)
    }

    @Test
    fun `path token detection without line or column`() {
        val token = detectPathTokens("see src/main/kotlin/Foo.kt for details").single()
        assertEquals("src/main/kotlin/Foo.kt", token.rawPath)
        assertEquals(null, token.line)
        assertEquals(null, token.column)
    }
}
