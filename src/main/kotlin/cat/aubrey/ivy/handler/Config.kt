/**
 * Config.kt
 * 
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * @author FlutterMC (https://github.com/FlutterMC/)
 * @contributor Aubrey @ aubrey.rs
 * @since 2024-09-12
 * @version 1.0
 */

package cat.aubrey.ivy.handler

import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.plugin.java.JavaPlugin
import java.io.File

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

    fun getDatabaseType(): String = getString("database.type", "sqlite")

    fun getDatabaseConnectionString(): String {
        val defaultPath = File(plugin.dataFolder, "ivy.db").absolutePath
        val configPath = getString("database.connection-string", "")

        return when (getDatabaseType().toLowerCase()) {
            "sqlite" -> {
                if (configPath.isNotEmpty()) {
                    "jdbc:sqlite:${File(plugin.dataFolder, configPath).absolutePath}"
                } else {
                    "jdbc:sqlite:$defaultPath"
                }
            }
            else -> configPath
        }
    }

    private fun getString(path: String, default: String): String =
        config.getString(path) ?: default

    private fun getInt(path: String, default: Int): Int =
        config.getInt(path, default)
}