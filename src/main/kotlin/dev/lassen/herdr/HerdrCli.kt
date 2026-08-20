package dev.lassen.herdr

import com.google.gson.JsonParser

object HerdrCli {

    data class Session(val name: String, val running: Boolean, val isDefault: Boolean)

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
}
