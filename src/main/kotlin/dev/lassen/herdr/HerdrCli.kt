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

    /** Aggregates [listAgents] across every currently running session. */
    fun listAllAgents(binaryPath: String): List<Agent> =
        listSessions(binaryPath).filter { it.running }.flatMap { listAgents(binaryPath, it.name) }

    /** Runs `herdr --session <name> agent focus <paneId>`, fire-and-forget. */
    fun focusAgent(binaryPath: String, sessionName: String, paneId: String) {
        runCatching {
            ProcessBuilder(binaryPath, "--session", sessionName, "agent", "focus", paneId).start()
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
        )
    }
}
