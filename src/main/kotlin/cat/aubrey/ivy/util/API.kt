/**
 * API.kt
 * 
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * @author FlutterMC (https://github.com/FlutterMC/)
 * @contributor Aubrey @ aubrey.rs
 * @since 2024-09-08
 * @version 1.0
 */

package cat.aubrey.ivy.util

import com.sun.net.httpserver.HttpServer
import com.sun.net.httpserver.HttpExchange
import cat.aubrey.ivy.Ivy
import cat.aubrey.ivy.types.Punishment
import com.google.gson.Gson
import java.net.InetSocketAddress
import java.util.UUID

class API(
    private val plugin: Ivy,
    private val port: Int,
    private val apiKey: String
) {
    private val server: HttpServer = HttpServer.create(InetSocketAddress(port), 0)
    private val gson = Gson()

    init {
        setupEndpoints()
    }

    fun start() {
        server.executor = null
        server.start()
        plugin.logger.info("API server started on port $port")
    }

    fun stop() {
        server.stop(0)
        plugin.logger.info("API server stopped")
    }

    private fun setupEndpoints() {
        server.createContext("/api/v1/punishments", this::handlePunishments)
        server.createContext("/api/v1/auditlog", this::handleAuditLog)
        server.createContext("/api/v1/evidence", this::handleEvidence)
        server.createContext("/api/v1/commands", this::handleCommands)
    }

    private fun handlePunishments(exchange: HttpExchange) {
        if (!authorize(exchange)) return

        when (exchange.requestMethod) {
            "GET" -> handleGetPunishments(exchange)
            "POST" -> handlePostPunishment(exchange)
            "DELETE" -> handleDeletePunishment(exchange)
            else -> respondMethodNotAllowed(exchange)
        }
    }

    private fun handleGetPunishments(exchange: HttpExchange) {
        val params = exchange.requestURI.query?.parseQueryString()
        val playerId = params?.get("playerId")
        val punishments = if (playerId != null) {
            plugin.dataManager.getActivePunishment(UUID.fromString(playerId), Punishment.Type.MUTE)?.let { listOf(it) } ?: emptyList()
        } else {
            plugin.dataManager.getActivePunishmentIds().mapNotNull {
                plugin.dataManager.getActivePunishment(UUID.randomUUID(), Punishment.Type.MUTE) // Replace with actual logic
            }
        }
        respond(exchange, 200, gson.toJson(punishments))
    }

    private fun handlePostPunishment(exchange: HttpExchange) {
        val punishment = gson.fromJson(exchange.requestBody.reader(), Punishment::class.java)
        val id = plugin.dataManager.addPunishment(punishment)
        respond(exchange, 201, gson.toJson(mapOf("message" to "Punishment added.", "id" to id)))
    }

    private fun handleDeletePunishment(exchange: HttpExchange) {
        val params = exchange.requestURI.query?.parseQueryString()
        val playerId = params?.get("playerId")
        if (playerId != null) {
            val result = plugin.dataManager.removePunishment(UUID.fromString(playerId), Punishment.Type.MUTE)
            val message = if (result) "Punishment removed." else "Punishment not found."
            respond(exchange, if (result) 200 else 404, gson.toJson(mapOf("message" to message)))
        } else {
            respondBadRequest(exchange, "Missing playerId")
        }
    }

    private fun handleAuditLog(exchange: HttpExchange) {
        if (!authorize(exchange)) return

        if (exchange.requestMethod == "GET") {
            val params = exchange.requestURI.query?.parseQueryString()
            val player = params?.get("player")
            val entries = player?.let { plugin.dataManager.getAuditLogForPlayer(it) } ?: plugin.dataManager.getAuditLog()
            respond(exchange, 200, gson.toJson(entries))
        } else {
            respondMethodNotAllowed(exchange)
        }
    }

    private fun handleEvidence(exchange: HttpExchange) {
        if (!authorize(exchange)) return

        when (exchange.requestMethod) {
            "GET" -> handleGetEvidence(exchange)
            "POST" -> handlePostEvidence(exchange)
            else -> respondMethodNotAllowed(exchange)
        }
    }

    private fun handleGetEvidence(exchange: HttpExchange) {
        val params = exchange.requestURI.query?.parseQueryString()
        val punishmentId = params?.get("punishmentId")?.toIntOrNull()
        if (punishmentId != null) {
            val evidence = plugin.dataManager.getEvidenceForPunishment(punishmentId)
            respond(exchange, 200, gson.toJson(evidence))
        } else {
            respondBadRequest(exchange, "Invalid or missing punishmentId")
        }
    }

    private fun handlePostEvidence(exchange: HttpExchange) {
        val evidenceRequest = gson.fromJson(exchange.requestBody.reader(), EvidenceRequest::class.java)
        if (evidenceRequest.punishmentId != null && evidenceRequest.evidence != null) {
            val result = plugin.dataManager.addEvidence(evidenceRequest.punishmentId, evidenceRequest.evidence)
            val message = if (result) "Evidence added." else "Punishment not found."
            respond(exchange, if (result) 201 else 404, gson.toJson(mapOf("message" to message)))
        } else {
            respondBadRequest(exchange, "Missing punishmentId or evidence")
        }
    }

    private fun handleCommands(exchange: HttpExchange) {
        if (!authorize(exchange)) return

        if (exchange.requestMethod == "POST") {
            val commandRequest = gson.fromJson(exchange.requestBody.reader(), CommandRequest::class.java)
            if (commandRequest.command != null) {
                executeCommand(commandRequest.command)
                respond(exchange, 200, gson.toJson(mapOf("message" to "Command executed", "command" to commandRequest.command)))
            } else {
                respondBadRequest(exchange, "Missing command")
            }
        } else {
            respondMethodNotAllowed(exchange)
        }
    }

    private fun executeCommand(command: String) {
        plugin.server.scheduler.runTask(plugin, Runnable {
            plugin.server.dispatchCommand(plugin.server.consoleSender, command)
        })
    }

    private fun authorize(exchange: HttpExchange): Boolean {
        val authKey = exchange.requestHeaders.getFirst("Authorization")
        if (authKey != apiKey) {
            respondUnauthorized(exchange)
            return false
        }
        return true
    }

    private fun respond(exchange: HttpExchange, statusCode: Int, response: String) {
        exchange.sendResponseHeaders(statusCode, response.toByteArray().size.toLong())
        exchange.responseBody.use { os ->
            os.write(response.toByteArray())
        }
    }

    private fun respondBadRequest(exchange: HttpExchange, error: String) {
        respond(exchange, 400, gson.toJson(mapOf("error" to error)))
    }

    private fun respondUnauthorized(exchange: HttpExchange) {
        respond(exchange, 401, gson.toJson(mapOf("error" to "Unauthorized")))
    }

    private fun respondMethodNotAllowed(exchange: HttpExchange) {
        respond(exchange, 405, gson.toJson(mapOf("error" to "Method Not Allowed")))
    }

    private fun String.parseQueryString(): Map<String, String> =
        split("&").associate {
            val (key, value) = it.split("=")
            key to value
        }

    data class EvidenceRequest(val punishmentId: Int?, val evidence: String?)
    data class CommandRequest(val command: String?)
}