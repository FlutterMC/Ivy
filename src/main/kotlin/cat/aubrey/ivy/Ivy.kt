/**
 * Ivy.kt
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

package cat.aubrey.ivy

import co.aikar.commands.PaperCommandManager
import org.bukkit.plugin.java.JavaPlugin
import cat.aubrey.ivy.cmd.add.Mute
import cat.aubrey.ivy.cmd.remove.UnMute
import cat.aubrey.ivy.cmd.Rollback
import cat.aubrey.ivy.cmd.AuditLog
import cat.aubrey.ivy.cmd.Evidence
import cat.aubrey.ivy.data.DatabaseManager
import cat.aubrey.ivy.data.SQLiteManager
import cat.aubrey.ivy.data.SQLManager
import cat.aubrey.ivy.data.MongoManager
import cat.aubrey.ivy.util.ChatListener
import cat.aubrey.ivy.handler.Config
import cat.aubrey.ivy.util.Webhook
import cat.aubrey.ivy.util.API
import cat.aubrey.ivy.types.Time
import org.bukkit.Bukkit

class Ivy : JavaPlugin() {

    lateinit var dataManager: DatabaseManager
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
        config = Config(this).apply { load() }
        webhook = Webhook(config.getDiscordWebhookUrl())
        initializeDatabase()
        commandManager = PaperCommandManager(this)
    }

    private fun initializeDatabase() {
        val dbType = config.getDatabaseType()
        val connectionString = config.getDatabaseConnectionString()

        dataManager = try {
            when (dbType.toLowerCase()) {
                "sqlite" -> SQLiteManager(connectionString)
                "mysql" -> SQLManager(connectionString)
//                "mongodb" -> MongoManager(connectionString)
                else -> throw IllegalArgumentException("Unsupported database type: $dbType")
            }
        } catch (e: Exception) {
            logger.severe("Failed to initialize database: ${e.message}")
            throw e
        }
        dataManager.initTables()
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
        if (::dataManager.isInitialized) {
            dataManager.close()
        }
        if (::apiServer.isInitialized) {
            apiServer.stop()
        }
        logger.info("Ivy has been disabled! D:")
    }
}
