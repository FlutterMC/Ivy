package cat.aubrey.ivy.handler

import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.plugin.java.JavaPlugin

class Config(private val plugin: JavaPlugin) {
    private lateinit var config: FileConfiguration

    fun load() {
        plugin.saveDefaultConfig()
        plugin.reloadConfig()
        config = plugin.config
    }

    fun getDiscordWebhookUrl(): String = getString("discord-webhook-url", "")

    fun getApiPort(): Int = getInt("api.port", 8080)

    fun getApiKey(): String = getString("api.key", "default-key")

    private fun getString(path: String, default: String): String =
        config.getString(path) ?: default

    private fun getInt(path: String, default: Int): Int =
        config.getInt(path, default)

}