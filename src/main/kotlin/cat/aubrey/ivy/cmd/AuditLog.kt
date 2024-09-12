/**
 * AuditLog.kt
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

package cat.aubrey.ivy.cmd

import cat.aubrey.ivy.Ivy
import cat.aubrey.ivy.types.Palette
import co.aikar.commands.BaseCommand
import co.aikar.commands.annotation.*
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.event.ClickEvent
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.command.CommandSender
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@CommandAlias("auditlog")
class AuditLog(private val plugin: Ivy) : BaseCommand() {

    @Default
    @CommandCompletion("@players")
    @Description("View the audit log")
    @CommandPermission("ivy.auditlog")
    fun onAuditLog(sender: CommandSender, @Optional player: String?) {
        val entries = if (player != null) {
            plugin.dataManager.getAuditLogForPlayer(player)
        } else {
            plugin.dataManager.getAuditLog()
        }

        if (entries.isEmpty()) {
            sender.sendMessage(Component.text("\nNo audit log entries found.\n", Palette.YELLOW))
            return
        }

        sender.sendMessage(Component.text("\nAudit Log:", Palette.GREEN).decorate(TextDecoration.UNDERLINED))
        sender.sendMessage(Component.text("")) // Add spacing
        entries.forEach { entry ->
            val (id, duration) = extractPunishmentDetails(entry.details)
            val message = Component.text()
                .append(Component.text("[${formatTimestamp(entry.timestamp)}] ", Palette.GRAY))
                .append(Component.text(entry.action, Palette.YELLOW))
                .append(Component.text(" by ", Palette.FG))
                .append(Component.text(entry.moderator, Palette.AQUA))
                .append(Component.text(" on ", Palette.FG))
                .append(Component.text(entry.target, Palette.YELLOW))
                .append(Component.text(" (ID: $id)", Palette.GRAY))
                .append(Component.text(" $duration", Palette.FG))
                .clickEvent(ClickEvent.runCommand("/auditlog details ${entry.id}"))
                .hoverEvent(Component.text("Click for more details", Palette.GRAY))
                .build()
            sender.sendMessage(message)
        }
        sender.sendMessage(Component.text(""))
    }

    @Subcommand("details")
    @Description("View details of a specific audit log entry")
    @CommandPermission("ivy.auditlog")
    fun onAuditLogDetails(sender: CommandSender, id: Int) {
        val entry = plugin.dataManager.getAuditLogEntry(id)
        if (entry == null) {
            sender.sendMessage(Component.text("Audit log entry not found.", Palette.RED))
            return
        }

        sender.sendMessage(Component.text("\nAudit Log Entry Details:", Palette.GREEN).decorate(TextDecoration.UNDERLINED))
        sender.sendMessage(Component.text(""))
        sender.sendMessage(Component.text("Action: ", Palette.GRAY).append(Component.text(entry.action, Palette.YELLOW)))
        sender.sendMessage(Component.text("Moderator: ", Palette.GRAY).append(Component.text(entry.moderator, Palette.AQUA)))
        sender.sendMessage(Component.text("Target: ", Palette.GRAY).append(Component.text(entry.target, Palette.YELLOW)))
        sender.sendMessage(Component.text("Timestamp: ", Palette.GRAY).append(Component.text(formatTimestamp(entry.timestamp), Palette.FG)))

        val (punishmentId, duration, reason) = extractPunishmentDetails(entry.details)
        sender.sendMessage(Component.text("Punishment ID: ", Palette.GRAY).append(Component.text(punishmentId ?: "N/A", Palette.FG)))
        sender.sendMessage(Component.text("Duration: ", Palette.GRAY).append(Component.text(duration ?: "N/A", Palette.FG)))
        sender.sendMessage(Component.text("Reason: ", Palette.GRAY).append(Component.text(reason ?: "None", Palette.FG)))

        if (punishmentId != null) {
            val evidence = plugin.dataManager.getEvidenceForPunishment(punishmentId.toInt())
            if (evidence.isNotEmpty()) {
                sender.sendMessage(Component.text("\nEvidence:", Palette.GREEN))
                evidence.forEach { (_, content) ->
                    sender.sendMessage(Component.text("- ", Palette.GRAY).append(Component.text(content, Palette.FG)))
                }
            } else {
                sender.sendMessage(Component.text("\nNo evidence found for this punishment.", Palette.YELLOW))
            }
        }
        sender.sendMessage(Component.text(""))
    }

    private fun formatTimestamp(timestamp: Long): String {
        val instant = Instant.ofEpochMilli(timestamp)
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
            .withZone(ZoneId.systemDefault())
        return formatter.format(instant)
    }

    private fun extractPunishmentDetails(details: String): Triple<String?, String?, String?> {
        val idPattern = "ID: (\\d+)".toRegex()
        val durationPattern = "Duration: ([^,]+)".toRegex()
        val reasonPattern = "Reason: ([^,]+)".toRegex()

        val idMatch = idPattern.find(details)
        val durationMatch = durationPattern.find(details)
        val reasonMatch = reasonPattern.find(details)

        return Triple(
            idMatch?.groupValues?.get(1),
            durationMatch?.groupValues?.get(1),
            reasonMatch?.groupValues?.get(1)
        )
    }
}