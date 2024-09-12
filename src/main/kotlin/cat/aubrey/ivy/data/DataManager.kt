/**
 * DataManager.kt
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

package cat.aubrey.ivy.data

import cat.aubrey.ivy.types.AuditLog
import cat.aubrey.ivy.types.Punishment
import com.zaxxer.hikari.HikariDataSource
import org.bukkit.plugin.java.JavaPlugin
import java.util.*

class DataManager(private val plugin: JavaPlugin) {
    private lateinit var databaseManager: DatabaseManager

    fun initialize(dbType: String, connectionString: String) {
        databaseManager = when (dbType.toLowerCase()) {
            "sqlite" -> SQLiteManager(connectionString)
            "sql" -> SQLManager(connectionString)
//            "mongo" -> MongoManager(connectionString)
            else -> throw IllegalArgumentException("Unsupported database type: $dbType")
        }
        databaseManager.initTables()
    }

    fun addPunishment(punishment: Punishment) = databaseManager.addPunishment(punishment)
    fun removePunishment(playerUUID: UUID, type: Punishment.Type) = databaseManager.removePunishment(playerUUID, type)
    fun getActivePunishment(playerUUID: UUID, type: Punishment.Type) = databaseManager.getActivePunishment(playerUUID, type)
    fun cleanExpiredPunishments() = databaseManager.cleanExpiredPunishments()
    fun rollbackPunishments(moderator: String, rollbackTime: Long, type: Punishment.Type?) =
        databaseManager.rollbackPunishments(moderator, rollbackTime, type)
    fun addEvidence(punishmentId: Int, evidence: String) = databaseManager.addEvidence(punishmentId, evidence)
    fun removeEvidence(punishmentId: Int, evidenceId: Int) = databaseManager.removeEvidence(punishmentId, evidenceId)
    fun getEvidenceForPunishment(punishmentId: Int) = databaseManager.getEvidenceForPunishment(punishmentId)
    fun addAuditLogEntry(entry: AuditLog) = databaseManager.addAuditLogEntry(entry)
    fun getAuditLog(limit: Int = 10, offset: Int = 0) = databaseManager.getAuditLog(limit, offset)
    fun getAuditLogForPlayer(playerName: String, limit: Int = 10, offset: Int = 0) =
        databaseManager.getAuditLogForPlayer(playerName, limit, offset)
    fun getAuditLogEntry(id: Int) = databaseManager.getAuditLogEntry(id)
    fun getRecentPunishmentIds(limit: Int) = databaseManager.getRecentPunishmentIds(limit)
    fun getActivePunishmentIds() = databaseManager.getActivePunishmentIds()
}