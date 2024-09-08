package cat.aubrey.ivy.util

import cat.aubrey.ivy.types.Palette
import cat.aubrey.ivy.types.Punishment
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import org.bukkit.Bukkit
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

class Webhook(private val webhookUrl: String) {
    private val client = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(10))
        .build()

    fun sendPunishmentInfo(punishment: Punishment) {
        try {
            val response = sendWebhookRequest(createPunishmentPayload(punishment))
            if (response.statusCode() != 204) {
                Bukkit.getLogger().warning("Failed to send webhook: ${response.statusCode()} ${response.body()}")
            }
        } catch (e: Exception) {
            Bukkit.getLogger().severe("Error sending webhook: ${e.message}")
        }
    }

    private fun sendWebhookRequest(payload: String): HttpResponse<String> {
        val request = HttpRequest.newBuilder()
            .uri(URI(webhookUrl))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(payload))
            .build()

        return client.send(request, HttpResponse.BodyHandlers.ofString())
    }

    private fun createPunishmentPayload(punishment: Punishment): String {
        val offenderName = Bukkit.getOfflinePlayer(punishment.playerUUID).name ?: "Unknown Player"

        val json = JsonObject().apply {
            addProperty("content", null as String?)
            add("embeds", JsonArray().apply {
                add(JsonObject().apply {
                    addProperty("title", "New Punishment Issued")
                    addProperty("color", Palette.RED.value())
                    add("thumbnail", JsonObject().apply {
                        addProperty("url", "https://minotar.net/helm/$offenderName/512.png")
                    })
                    add("fields", JsonArray().apply {
                        add(createField("ID", punishment.id.toString(), true))
                        add(createField("Type", punishment.type.name, true))
                        add(createField("Offender", offenderName, true))
                        add(createField("UUID", punishment.playerUUID.toString(), true))
                        add(createField("Reason", punishment.reason ?: "No reason provided", false))
                        add(createField("Expiry", formatExpiry(punishment.expiration), false))
                        add(createField("Issued By", punishment.issuer, true))
                        add(createField("Issued At", "<t:${punishment.issuedAt / 1000}:R>", true))
                    })
                })
            })
        }

        return json.toString()
    }

    private fun createField(name: String, value: String, inline: Boolean): JsonObject {
        return JsonObject().apply {
            addProperty("name", name)
            addProperty("value", value)
            addProperty("inline", inline)
        }
    }

    private fun formatExpiry(expiration: Long?): String = when {
        expiration == null -> "Permanent"
        else -> "<t:${expiration / 1000}:R>"
    }
}