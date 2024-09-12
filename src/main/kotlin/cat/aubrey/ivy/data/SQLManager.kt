/**
 * SQLManager.kt
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

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import cat.aubrey.ivy.types.Punishment
import cat.aubrey.ivy.types.AuditLog
import java.sql.SQLException
import java.util.UUID
import org.bukkit.Bukkit

class SQLManager(connectionString: String) : DatabaseManager {
    private val dataSource: HikariDataSource

    init {
        val config = HikariConfig()
        config.jdbcUrl = connectionString
        dataSource = HikariDataSource(config)
    }

    override fun initTables() {
        executeUpdate(CREATE_PUNISHMENTS_TABLE)
        executeUpdate(CREATE_AUDIT_LOG_TABLE)
        executeUpdate(CREATE_EVIDENCE_TABLE)
    }

    override fun addPunishment(punishment: Punishment): Int = withConnection { conn ->
        conn.prepareStatement(INSERT_PUNISHMENT, java.sql.Statement.RETURN_GENERATED_KEYS).use { stmt ->
            stmt.setString(1, punishment.playerUUID.toString())
            stmt.setString(2, punishment.type.name)
            stmt.setString(3, punishment.reason)
            stmt.setObject(4, punishment.expiration)
            stmt.setString(5, punishment.issuer)
            stmt.setLong(6, punishment.issuedAt)
            stmt.executeUpdate()

            stmt.generatedKeys.use { keys ->
                if (keys.next()) keys.getInt(1) else throw SQLException("Creating punishment failed, no ID obtained.")
            }
        }
    }

    override fun removePunishment(playerUUID: UUID, type: Punishment.Type): Boolean = withConnection { conn ->
        conn.prepareStatement(DELETE_PUNISHMENT).use { stmt ->
            stmt.setString(1, playerUUID.toString())
            stmt.setString(2, type.name)
            stmt.setLong(3, System.currentTimeMillis())
            stmt.executeUpdate() > 0
        }
    }

    override fun getActivePunishment(playerUUID: UUID, type: Punishment.Type): Punishment? = withConnection { conn ->
        conn.prepareStatement(GET_ACTIVE_PUNISHMENT).use { stmt ->
            stmt.setString(1, playerUUID.toString())
            stmt.setString(2, type.name)
            stmt.setLong(3, System.currentTimeMillis())
            stmt.executeQuery().use { rs ->
                if (rs.next()) {
                    Punishment(
                        rs.getInt("id"),
                        UUID.fromString(rs.getString("player_uuid")),
                        Punishment.Type.valueOf(rs.getString("type")),
                        rs.getString("reason"),
                        rs.getObject("expiration") as Long?,
                        rs.getString("issuer"),
                        rs.getLong("issued_at")
                    )
                } else null
            }
        }
    }

    override fun cleanExpiredPunishments() = withConnection { conn ->
        conn.prepareStatement(DELETE_EXPIRED_PUNISHMENTS).use { stmt ->
            stmt.setLong(1, System.currentTimeMillis())
            val deletedCount = stmt.executeUpdate()
            Bukkit.getLogger().info("Cleaned up $deletedCount expired punishments")
        }
    }

    override fun rollbackPunishments(moderator: String, rollbackTime: Long, type: Punishment.Type?): List<Punishment> = withConnection { conn ->
        val (query, deleteQuery) = if (type == null) {
            SELECT_ROLLBACK_PUNISHMENTS to DELETE_ROLLBACK_PUNISHMENTS
        } else {
            SELECT_ROLLBACK_PUNISHMENTS_WITH_TYPE to DELETE_ROLLBACK_PUNISHMENTS_WITH_TYPE
        }

        val punishments = mutableListOf<Punishment>()
        conn.prepareStatement(query).use { stmt ->
            stmt.setString(1, moderator)
            stmt.setLong(2, rollbackTime)
            if (type != null) stmt.setString(3, type.name)
            stmt.executeQuery().use { rs ->
                while (rs.next()) {
                    punishments.add(
                        Punishment(
                            rs.getInt("id"),
                            UUID.fromString(rs.getString("player_uuid")),
                            Punishment.Type.valueOf(rs.getString("type")),
                            rs.getString("reason"),
                            rs.getObject("expiration") as Long?,
                            rs.getString("issuer"),
                            rs.getLong("issued_at")
                        )
                    )
                }
            }
        }

        conn.prepareStatement(deleteQuery).use { stmt ->
            stmt.setString(1, moderator)
            stmt.setLong(2, rollbackTime)
            if (type != null) stmt.setString(3, type.name)
            stmt.executeUpdate()
        }

        punishments
    }

    override fun addEvidence(punishmentId: Int, evidence: String): Boolean = withConnection { conn ->
        conn.prepareStatement(INSERT_EVIDENCE).use { stmt ->
            stmt.setInt(1, punishmentId)
            stmt.setString(2, evidence)
            stmt.executeUpdate() > 0
        }
    }

    override fun removeEvidence(punishmentId: Int, evidenceId: Int): Boolean = withConnection { conn ->
        conn.prepareStatement(DELETE_EVIDENCE).use { stmt ->
            stmt.setInt(1, evidenceId)
            stmt.setInt(2, punishmentId)
            stmt.executeUpdate() > 0
        }
    }

    override fun getEvidenceForPunishment(punishmentId: Int): List<Pair<Int, String>> = withConnection { conn ->
        conn.prepareStatement(GET_EVIDENCE).use { stmt ->
            stmt.setInt(1, punishmentId)
            stmt.executeQuery().use { rs ->
                generateSequence {
                    if (rs.next()) Pair(rs.getInt("id"), rs.getString("evidence"))
                    else null
                }.toList()
            }
        }
    }

    override fun addAuditLogEntry(entry: AuditLog): Boolean = withConnection { conn ->
        conn.prepareStatement(INSERT_AUDIT_LOG).use { stmt ->
            stmt.setString(1, entry.action)
            stmt.setString(2, entry.moderator)
            stmt.setString(3, entry.target)
            stmt.setString(4, entry.details)
            stmt.setLong(5, entry.timestamp)
            stmt.executeUpdate() > 0
        }
    }

    override fun getAuditLog(limit: Int, offset: Int): List<AuditLog> = withConnection { conn ->
        conn.prepareStatement(GET_AUDIT_LOG).use { stmt ->
            stmt.setInt(1, limit)
            stmt.setInt(2, offset)
            stmt.executeQuery().use { rs ->
                generateSequence {
                    if (rs.next()) AuditLog(
                        rs.getInt("id"),
                        rs.getString("action"),
                        rs.getString("moderator"),
                        rs.getString("target"),
                        rs.getString("details"),
                        rs.getLong("timestamp")
                    ) else null
                }.toList()
            }
        }
    }

    override fun getAuditLogForPlayer(playerName: String, limit: Int, offset: Int): List<AuditLog> = withConnection { conn ->
        conn.prepareStatement(GET_AUDIT_LOG_FOR_PLAYER).use { stmt ->
            stmt.setString(1, playerName)
            stmt.setInt(2, limit)
            stmt.setInt(3, offset)
            stmt.executeQuery().use { rs ->
                generateSequence {
                    if (rs.next()) AuditLog(
                        rs.getInt("id"),
                        rs.getString("action"),
                        rs.getString("moderator"),
                        rs.getString("target"),
                        rs.getString("details"),
                        rs.getLong("timestamp")
                    ) else null
                }.toList()
            }
        }
    }

    override fun getAuditLogEntry(id: Int): AuditLog? = withConnection { conn ->
        conn.prepareStatement(GET_AUDIT_LOG_ENTRY).use { stmt ->
            stmt.setInt(1, id)
            stmt.executeQuery().use { rs ->
                if (rs.next()) AuditLog(
                    rs.getInt("id"),
                    rs.getString("action"),
                    rs.getString("moderator"),
                    rs.getString("target"),
                    rs.getString("details"),
                    rs.getLong("timestamp")
                ) else null
            }
        }
    }

    override fun getRecentPunishmentIds(limit: Int): List<String> = withConnection { conn ->
        conn.prepareStatement(GET_RECENT_PUNISHMENT_IDS).use { stmt ->
            stmt.setInt(1, limit)
            stmt.executeQuery().use { rs ->
                generateSequence { if (rs.next()) rs.getInt("id").toString() else null }.toList()
            }
        }
    }

    override fun getActivePunishmentIds(): List<Int> = withConnection { conn ->
        conn.prepareStatement(GET_ACTIVE_PUNISHMENT_IDS).use { stmt ->
            stmt.setLong(1, System.currentTimeMillis())
            stmt.executeQuery().use { rs ->
                generateSequence { if (rs.next()) rs.getInt("id") else null }.toList()
            }
        }
    }

    override fun close() {
        if (!dataSource.isClosed) {
            dataSource.close()
        }
    }

    private fun <T> withConnection(block: (java.sql.Connection) -> T): T {
        return try {
            dataSource.connection.use(block)
        } catch (e: SQLException) {
            Bukkit.getLogger().severe("Database operation failed: ${e.message}")
            throw e
        }
    }

    private fun executeUpdate(sql: String) {
        withConnection { conn ->
            conn.createStatement().use { stmt ->
                stmt.execute(sql)
            }
        }
    }

    companion object {
        // SQL statements
        private const val CREATE_PUNISHMENTS_TABLE = """
            CREATE TABLE IF NOT EXISTS punishments (
                id INT AUTO_INCREMENT PRIMARY KEY,
                player_uuid VARCHAR(36) NOT NULL,
                type VARCHAR(10) NOT NULL,
                reason TEXT,
                expiration BIGINT,
                issuer VARCHAR(36) NOT NULL,
                issued_at BIGINT NOT NULL
            )
        """

        private const val CREATE_AUDIT_LOG_TABLE = """
            CREATE TABLE IF NOT EXISTS audit_log (
                id INT AUTO_INCREMENT PRIMARY KEY,
                action VARCHAR(50) NOT NULL,
                moderator VARCHAR(36) NOT NULL,
                target VARCHAR(36) NOT NULL,
                details TEXT,
                timestamp BIGINT NOT NULL
            )
        """

        private const val CREATE_EVIDENCE_TABLE = """
            CREATE TABLE IF NOT EXISTS evidence (
                id INT AUTO_INCREMENT PRIMARY KEY,
                punishment_id INT NOT NULL,
                evidence TEXT NOT NULL,
                FOREIGN KEY (punishment_id) REFERENCES punishments(id)
            )
        """

        private const val INSERT_PUNISHMENT = """
            INSERT INTO punishments (player_uuid, type, reason, expiration, issuer, issued_at)
            VALUES (?, ?, ?, ?, ?, ?)
        """

        private const val DELETE_PUNISHMENT = """
            DELETE FROM punishments
            WHERE player_uuid = ? AND type = ? AND (expiration > ? OR expiration IS NULL)
        """

        private const val GET_ACTIVE_PUNISHMENT = """
            SELECT * FROM punishments
            WHERE player_uuid = ? AND type = ? AND (expiration > ? OR expiration IS NULL)
            LIMIT 1
        """

        private const val DELETE_EXPIRED_PUNISHMENTS = """
            DELETE FROM punishments
            WHERE expiration IS NOT NULL AND expiration <= ?
        """

        private const val SELECT_ROLLBACK_PUNISHMENTS = """
            SELECT * FROM punishments WHERE issuer = ? AND issued_at >= ?
        """

        private const val SELECT_ROLLBACK_PUNISHMENTS_WITH_TYPE = """
            SELECT * FROM punishments WHERE issuer = ? AND issued_at >= ? AND type = ?
        """

        private const val DELETE_ROLLBACK_PUNISHMENTS = """
            DELETE FROM punishments WHERE issuer = ? AND issued_at >= ?
        """

        private const val DELETE_ROLLBACK_PUNISHMENTS_WITH_TYPE = """
            DELETE FROM punishments WHERE issuer = ? AND issued_at >= ? AND type = ?
        """

        private const val INSERT_EVIDENCE = """
            INSERT INTO evidence (punishment_id, evidence) VALUES (?, ?)
        """

        private const val DELETE_EVIDENCE = """
            DELETE FROM evidence WHERE id = ? AND punishment_id = ?
        """

        private const val GET_EVIDENCE = """
            SELECT id, evidence FROM evidence WHERE punishment_id = ?
        """

        private const val INSERT_AUDIT_LOG = """
            INSERT INTO audit_log (action, moderator, target, details, timestamp)
            VALUES (?, ?, ?, ?, ?)
        """

        private const val GET_AUDIT_LOG = """
            SELECT * FROM audit_log
            ORDER BY timestamp DESC
            LIMIT ? OFFSET ?
        """

        private const val GET_AUDIT_LOG_FOR_PLAYER = """
            SELECT * FROM audit_log
            WHERE target = ?
            ORDER BY timestamp DESC
            LIMIT ? OFFSET ?
        """

        private const val GET_AUDIT_LOG_ENTRY = """
            SELECT * FROM audit_log WHERE id = ?
        """

        private const val GET_RECENT_PUNISHMENT_IDS = """
            SELECT id FROM punishments
            ORDER BY issued_at DESC
            LIMIT ?
        """

        private const val GET_ACTIVE_PUNISHMENT_IDS = """
            SELECT id FROM punishments
            WHERE expiration > ? OR expiration IS NULL
        """
    }
}