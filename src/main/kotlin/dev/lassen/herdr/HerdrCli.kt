package dev.lassen.herdr

import com.google.gson.JsonObject
import com.google.gson.JsonParser

object HerdrCli {

    data class Session(val name: String, val running: Boolean, val isDefault: Boolean)

    data class Agent(
        val sessionName: String,
        val paneId: String,
        val tabId: String,
        val workspaceId: String,
        val status: String,
        val title: String,
        val tool: String,
    )

    /** Runs `herdr session list --json` and parses the result; returns an empty list on any failure. */
    fun listSessions(binaryPath: String): List<Session> = try {
        val process = ProcessBuilder(binaryPath, "session", "list", "--json")
            .redirectErrorStream(false)
            .start()
        val output = process.inputStream.bufferedReader().readText()
        process.waitFor()
        JsonParser.parseString(output).asJsonObject.getAsJsonArray("sessions").map { element ->
            val session = element.asJsonObject
            Session(
                name = session.get("name").asString,
                running = session.get("running").asBoolean,
                isDefault = session.get("default").asBoolean,
            )
        }
    } catch (e: Exception) {
        emptyList()
    }

    /** Runs `herdr --session <name> agent list` and parses the result; returns an empty list on any failure. */
    fun listAgents(binaryPath: String, sessionName: String): List<Agent> = try {
        val process = ProcessBuilder(binaryPath, "--session", sessionName, "agent", "list")
            .redirectErrorStream(false)
            .start()
        val output = process.inputStream.bufferedReader().readText()
        process.waitFor()
        val agents = JsonParser.parseString(output).asJsonObject
            .getAsJsonObject("result")
            .getAsJsonArray("agents")
        agents.map { element -> element.asJsonObject.toAgent(sessionName) }
    } catch (e: Exception) {
        emptyList()
    }

    data class SessionSummary(val running: Boolean, val workspaces: Int, val tabs: Int, val busyAgents: Int)

    /** What a suspend would close: counts of workspaces, tabs and agents still working or waiting for input. */
    fun sessionSummary(binaryPath: String, sessionName: String): SessionSummary {
        if (listSessions(binaryPath).none { it.name == sessionName && it.running }) {
            return SessionSummary(running = false, workspaces = 0, tabs = 0, busyAgents = 0)
        }
        val workspaces = try {
            val process = ProcessBuilder(binaryPath, "--session", sessionName, "workspace", "list")
                .redirectErrorStream(false)
                .start()
            val output = process.inputStream.bufferedReader().readText()
            process.waitFor()
            JsonParser.parseString(output).asJsonObject
                .getAsJsonObject("result")
                .getAsJsonArray("workspaces")
                .map { it.asJsonObject }
        } catch (e: Exception) {
            emptyList()
        }
        return SessionSummary(
            running = true,
            workspaces = workspaces.size,
            tabs = workspaces.sumOf { it.get("tab_count")?.asInt ?: 0 },
            busyAgents = listAgents(binaryPath, sessionName).count { it.status in BUSY_STATUSES },
        )
    }

    /**
     * Runs `herdr session stop <name>`. herdr keeps the session's layout and agent session refs in its
     * session.json and restores them (resuming agents) the next time the session starts.
     * Returns null on success, otherwise the error output.
     */
    fun stopSession(binaryPath: String, sessionName: String): String? = try {
        val process = ProcessBuilder(binaryPath, "session", "stop", sessionName)
            .redirectErrorStream(true)
            .start()
        val output = process.inputStream.bufferedReader().readText()
        if (process.waitFor() == 0) null else output.trim().ifEmpty { "herdr exited with ${process.exitValue()}" }
    } catch (e: Exception) {
        e.message ?: e.toString()
    }

    private val BUSY_STATUSES = setOf("working", "blocked")

    /** Aggregates [listAgents] across every currently running session. */
    fun listAllAgents(binaryPath: String): List<Agent> =
        listSessions(binaryPath).filter { it.running }.flatMap { listAgents(binaryPath, it.name) }

    /** Runs `herdr --session <name> agent focus <paneId>`, fire-and-forget. */
    fun focusAgent(binaryPath: String, sessionName: String, paneId: String) {
        runCatching {
            ProcessBuilder(binaryPath, "--session", sessionName, "agent", "focus", paneId).start()
        }
    }

    /** Runs `herdr --session <name> agent prompt <paneId> /ide`, fire-and-forget. */
    fun connectAgentToIde(binaryPath: String, sessionName: String, paneId: String) {
        runCatching {
            ProcessBuilder(binaryPath, "--session", sessionName, "agent", "prompt", paneId, "/ide").start()
        }
    }

    private fun JsonObject.toAgent(sessionName: String): Agent {
        val title = get("terminal_title_stripped")?.takeIf { !it.isJsonNull }?.asString
            ?: get("pane_id").asString
        return Agent(
            sessionName = sessionName,
            paneId = get("pane_id").asString,
            tabId = get("tab_id").asString,
            workspaceId = get("workspace_id").asString,
            status = get("agent_status").asString,
            title = title,
            tool = get("agent").asString,
        )
    }
}
