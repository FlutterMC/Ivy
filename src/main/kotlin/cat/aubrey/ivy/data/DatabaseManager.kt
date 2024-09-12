/**
 * DatabaseManager.kt
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

import cat.aubrey.ivy.types.Punishment
import cat.aubrey.ivy.types.AuditLog
import java.util.UUID

interface DatabaseManager {
    fun initTables()
    fun addPunishment(punishment: Punishment): Int
    fun removePunishment(playerUUID: UUID, type: Punishment.Type): Boolean
    fun getActivePunishment(playerUUID: UUID, type: Punishment.Type): Punishment?
    fun cleanExpiredPunishments()
    fun rollbackPunishments(moderator: String, rollbackTime: Long, type: Punishment.Type?): List<Punishment>
    fun addEvidence(punishmentId: Int, evidence: String): Boolean
    fun removeEvidence(punishmentId: Int, evidenceId: Int): Boolean
    fun getEvidenceForPunishment(punishmentId: Int): List<Pair<Int, String>>
    fun addAuditLogEntry(entry: AuditLog): Boolean
    fun getAuditLog(limit: Int = 10, offset: Int = 0): List<AuditLog>
    fun getAuditLogForPlayer(playerName: String, limit: Int = 10, offset: Int = 0): List<AuditLog>
    fun getAuditLogEntry(id: Int): AuditLog?
    fun getRecentPunishmentIds(limit: Int): List<String>
    fun getActivePunishmentIds(): List<Int>
    fun close()
}