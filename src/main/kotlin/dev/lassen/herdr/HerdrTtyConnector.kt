package dev.lassen.herdr

import com.jediterm.core.util.TermSize
import com.jediterm.terminal.ProcessTtyConnector
import com.pty4j.PtyProcess
import com.pty4j.WinSize
import java.nio.charset.Charset

/**
 * We implement this directly on pty4j/JediTerm instead of using
 * com.intellij.terminal.pty.PtyProcessTtyConnector: that class is internal IntelliJ Platform
 * glue whose constructor signature changed between IDE build 262 and 263 (PtyProcess -> Process
 * parameter), breaking plugin binary compatibility. pty4j's PtyProcess/WinSize API is external
 * and has stayed stable across that change.
 */
class HerdrTtyConnector(
    private val process: PtyProcess,
    charset: Charset,
) : ProcessTtyConnector(process, charset) {
    override fun getName(): String = "herdr"

    override fun resize(termSize: TermSize) {
        if (isConnected) {
            process.winSize = WinSize(termSize.columns, termSize.rows)
        }
    }
}
