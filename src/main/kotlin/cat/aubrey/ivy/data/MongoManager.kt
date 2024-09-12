/**
 * MongoManager.kt
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

import com.mongodb.client.MongoClients
import com.mongodb.client.MongoClient
import com.mongodb.client.MongoDatabase
import com.mongodb.client.model.Filters
import com.mongodb.client.model.Updates
import com.mongodb.client.model.Sorts
import org.bson.Document
import cat.aubrey.ivy.types.Punishment
import cat.aubrey.ivy.types.AuditLog
import java.util.UUID
import org.bukkit.Bukkit
import org.bson.types.ObjectId

class MongoManager(connectionString: String) : DatabaseManager {
    private val client: MongoClient = MongoClients.create(connectionString)
    private val database: MongoDatabase = client.getDatabase("ivy")

    private val punishmentsCollection = database.getCollection("punishments")
    private val auditLogCollection = database.getCollection("audit_log")
    private val evidenceCollection = database.getCollection("evidence")

    override fun initTables() {
        // mongo doesn't require table creation but we can create indexes for better performance
        punishmentsCollection.createIndex(Document("player_uuid", 1).append("type", 1))
        punishmentsCollection.createIndex(Document("expiration", 1))
        auditLogCollection.createIndex(Document("timestamp", -1))
        evidenceCollection.createIndex(Document("punishment_id", 1))
    }

    override fun addPunishment(punishment: Punishment): Int {
        val document = Document()
            .append("player_uuid", punishment.playerUUID.toString())
            .append("type", punishment.type.name)
            .append("reason", punishment.reason)
            .append("expiration", punishment.expiration)
            .append("issuer", punishment.issuer)
            .append("issued_at", punishment.issuedAt)

        val result = punishmentsCollection.insertOne(document)
        return result.insertedId?.asObjectId()?.value.toString().hashCode()
    }

    override fun removePunishment(playerUUID: UUID, type: Punishment.Type): Boolean {
        val result = punishmentsCollection.deleteOne(
            Filters.and(
                Filters.eq("player_uuid", playerUUID.toString()),
                Filters.eq("type", type.name),
                Filters.or(
                    Filters.gt("expiration", System.currentTimeMillis()),
                    Filters.eq("expiration", null)
                )
            )
        )
        return result.deletedCount > 0
    }

    override fun getActivePunishment(playerUUID: UUID, type: Punishment.Type): Punishment? {
        val document = punishmentsCollection.find(
            Filters.and(
                Filters.eq("player_uuid", playerUUID.toString()),
                Filters.eq("type", type.name),
                Filters.or(
                    Filters.gt("expiration", System.currentTimeMillis()),
                    Filters.eq("expiration", null)
                )
            )
        ).first()

        return document?.let { documentToPunishment(it) }
    }

    override fun cleanExpiredPunishments() {
        val result = punishmentsCollection.deleteMany(
            Filters.and(
                Filters.ne("expiration", null),
                Filters.lte("expiration", System.currentTimeMillis())
            )
        )
        Bukkit.getLogger().info("Cleaned up ${result.deletedCount} expired punishments")
    }

    override fun rollbackPunishments(moderator: String, rollbackTime: Long, type: Punishment.Type?): List<Punishment> {
        val filter = Filters.and(
            Filters.eq("issuer", moderator),
            Filters.gte("issued_at", rollbackTime)
        ).let { if (type != null) Filters.and(it, Filters.eq("type", type.name)) else it }

        val punishments = punishmentsCollection.find(filter)
            .map { documentToPunishment(it) }
            .toList()

        punishmentsCollection.deleteMany(filter)

        return punishments
    }

    override fun addEvidence(punishmentId: Int, evidence: String): Boolean {
        val document = Document()
            .append("punishment_id", punishmentId)
            .append("evidence", evidence)

        val result = evidenceCollection.insertOne(document)
        return result.wasAcknowledged()
    }

    override fun removeEvidence(punishmentId: Int, evidenceId: Int): Boolean {
        val result = evidenceCollection.deleteOne(
            Filters.and(
                Filters.eq("_id", ObjectId(evidenceId.toString())),
                Filters.eq("punishment_id", punishmentId)
            )
        )
        return result.deletedCount > 0
    }

    override fun getEvidenceForPunishment(punishmentId: Int): List<Pair<Int, String>> {
        return evidenceCollection.find(Filters.eq("punishment_id", punishmentId))
            .map { Pair(it.getObjectId("_id").toString().hashCode(), it.getString("evidence")) }
            .toList()
    }

    override fun addAuditLogEntry(entry: AuditLog): Boolean {
        val document = Document()
            .append("action", entry.action)
            .append("moderator", entry.moderator)
            .append("target", entry.target)
            .append("details", entry.details)
            .append("timestamp", entry.timestamp)

        val result = auditLogCollection.insertOne(document)
        return result.wasAcknowledged()
    }

    override fun getAuditLog(limit: Int, offset: Int): List<AuditLog> {
        return auditLogCollection.find()
            .sort(Sorts.descending("timestamp"))
            .skip(offset)
            .limit(limit)
            .map { documentToAuditLog(it) }
            .toList()
    }

    override fun getAuditLogForPlayer(playerName: String, limit: Int, offset: Int): List<AuditLog> {
        return auditLogCollection.find(Filters.eq("target", playerName))
            .sort(Sorts.descending("timestamp"))
            .skip(offset)
            .limit(limit)
            .map { documentToAuditLog(it) }
            .toList()
    }

    override fun getAuditLogEntry(id: Int): AuditLog? {
        val document = auditLogCollection.find(Filters.eq("_id", ObjectId(id.toString()))).first()
        return document?.let { documentToAuditLog(it) }
    }

    override fun getRecentPunishmentIds(limit: Int): List<String> {
        return punishmentsCollection.find()
            .sort(Sorts.descending("issued_at"))
            .limit(limit)
            .map { it.getObjectId("_id").toString() }
            .toList()
    }

    override fun getActivePunishmentIds(): List<Int> {
        return punishmentsCollection.find(
            Filters.or(
                Filters.gt("expiration", System.currentTimeMillis()),
                Filters.eq("expiration", null)
            )
        ).map { it.getObjectId("_id").toString().hashCode() }
            .toList()
    }

    private fun documentToPunishment(document: Document): Punishment {
        return Punishment(
            document.getObjectId("_id").toString().hashCode(),
            UUID.fromString(document.getString("player_uuid")),
            Punishment.Type.valueOf(document.getString("type")),
            document.getString("reason"),
            document.getLong("expiration"),
            document.getString("issuer"),
            document.getLong("issued_at")
        )
    }

    private fun documentToAuditLog(document: Document): AuditLog {
        return AuditLog(
            document.getObjectId("_id").toString().hashCode(),
            document.getString("action"),
            document.getString("moderator"),
            document.getString("target"),
            document.getString("details"),
            document.getLong("timestamp")
        )
    }

    override fun close() {
        client.close()
    }
}
