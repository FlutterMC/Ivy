/**
 * Ivy.kt
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

package cat.aubrey.ivy

import co.aikar.commands.PaperCommandManager
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.bukkit.plugin.java.JavaPlugin
import cat.aubrey.ivy.cmd.add.Mute
import cat.aubrey.ivy.cmd.remove.UnMute
import cat.aubrey.ivy.cmd.Rollback
import cat.aubrey.ivy.cmd.AuditLog
import cat.aubrey.ivy.cmd.Evidence
import cat.aubrey.ivy.data.DataManager
import cat.aubrey.ivy.util.ChatListener
import cat.aubrey.ivy.handler.Config
import cat.aubrey.ivy.util.Webhook
import cat.aubrey.ivy.util.API
import cat.aubrey.ivy.types.Time
import org.bukkit.Bukkit
import java.io.File

class Ivy : JavaPlugin() {

    lateinit var dataManager: DataManager
    private lateinit var hikariDataSource: HikariDataSource
    lateinit var commandManager: PaperCommandManager
    private lateinit var config: Config
    lateinit var webhook: Webhook
    private lateinit var apiServer: API

    override fun onEnable() {
        try {
            initializePlugin()
            registerListenersAndCommands()
            scheduleCleanupTask()
            startApiServer()
            logger.info("Ivy has been enabled! <3")
        } catch (e: Exception) {
            logger.severe("Failed to enable Ivy D: : ${e.message}")
            e.printStackTrace()
            isEnabled = false
        }
    }

    override fun onDisable() {
        shutdownPlugin()
    }

    private fun initializePlugin() {
        ensureDataFolderExists()
        config = Config(this).apply { load() }
        webhook = Webhook(config.getDiscordWebhookUrl())
        initializeDatabase()
        dataManager = DataManager(hikariDataSource)
        commandManager = PaperCommandManager(this)
    }

    private fun ensureDataFolderExists() {
        if (!dataFolder.exists()) {
            dataFolder.mkdirs()
        }
    }

    private fun initializeDatabase() {
        val dbFile = File(dataFolder, "ivy.db")
        val config = HikariConfig().apply {
            jdbcUrl = "jdbc:sqlite:${dbFile.absolutePath}"
            driverClassName = "org.sqlite.JDBC"
            maximumPoolSize = 10
        }
        hikariDataSource = HikariDataSource(config)
        DataManager(hikariDataSource).initTables()
    }

    private fun registerListenersAndCommands() {
        server.pluginManager.registerEvents(ChatListener(this), this)
        registerCommands()
    }

    private fun scheduleCleanupTask() {
        server.scheduler.runTaskTimerAsynchronously(this, Runnable {
            dataManager.cleanExpiredPunishments()
        }, 20L * 60 * 5, 20L * 60 * 60)
    }

    private fun startApiServer() {
        apiServer = API(this, config.getApiPort(), config.getApiKey())
        apiServer.start()
    }

    private fun registerCommands() {
        listOf(Mute(this), UnMute(this), Rollback(this), AuditLog(this), Evidence(this)).forEach {
            commandManager.registerCommand(it)
        }

        with(commandManager.commandCompletions) {
            registerCompletion("timeperiods") { Time.entries.map { it.duration } }
            registerCompletion("players") { Bukkit.getOnlinePlayers().map { it.name } }
            registerCompletion("punishments") { dataManager.getActivePunishmentIds().map { it.toString() } }
            registerAsyncCompletion("evidenceIds") { c ->
                val punishmentId = c.getContextValue(Int::class.java, 0)
                dataManager.getEvidenceForPunishment(punishmentId).map { it.first.toString() }
            }
        }

        Mute.registerCompletions(this)
        Evidence.registerCompletions(this)
    }

    private fun shutdownPlugin() {
        if (::hikariDataSource.isInitialized) {
            hikariDataSource.close()
        }
        if (::apiServer.isInitialized) {
            apiServer.stop()
        }
        logger.info("Ivy has been disabled! D:")
    }
}