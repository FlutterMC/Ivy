/**
 * Mute.kt
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

package cat.aubrey.ivy.cmd.add

import cat.aubrey.ivy.Ivy
import cat.aubrey.ivy.types.*
import co.aikar.commands.BaseCommand
import co.aikar.commands.annotation.*
import co.aikar.commands.bukkit.contexts.OnlinePlayer
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.event.ClickEvent
import org.bukkit.command.CommandSender
import java.util.concurrent.TimeUnit

@CommandAlias("mute")
class Mute(private val plugin: Ivy) : BaseCommand() {

    @Default
    @CommandCompletion("@players @durations @reasons")
    @Description("Mute a player")
    @Syntax("<player> [duration] [reason]")
    @CommandPermission("ivy.mute")
    fun onMute(
        sender: CommandSender,
        @Flags("other") target: OnlinePlayer,
        @Optional duration: String?,
        @Optional reason: String?
    ) {
        if (isPlayerAlreadyMuted(target)) return

        val punishment = createPunishment(target, duration, reason, sender.name)
        val punishmentId = plugin.dataManager.addPunishment(punishment)

        notifyModerator(sender, target.player.name, punishment, punishmentId)
        notifyTarget(target, punishment)
        logAuditEntry(sender.name, target.player.name, punishment, punishmentId)
        sendWebhookNotification(punishment.copy(id = punishmentId))
    }

    private fun isPlayerAlreadyMuted(target: OnlinePlayer): Boolean {
        val existingPunishment = plugin.dataManager.getActivePunishment(target.player.uniqueId, Punishment.Type.MUTE)
        if (existingPunishment != null) {
            val message = Component.text()
                .append(Component.text("${target.player.name} is already muted ", Palette.RED))
                .append(Component.text("(ID: ${existingPunishment.id})", Palette.GRAY))
                .append(Component.text(". Use ", Palette.RED))
                .append(Component.text("/unmute", Palette.YELLOW))
                .append(Component.text(" first if you want to change the mute.", Palette.RED))
                .build()
            target.player.sendMessage(message)
            return true
        }
        return false
    }

    private fun createPunishment(target: OnlinePlayer, duration: String?, reason: String?, issuer: String): Punishment {
        val timePeriod = duration?.let { Time.fromString(it) }
        val expiration = timePeriod?.let { parseDuration(it.duration) }

        return Punishment(
            0, // temporary ID
            target.player.uniqueId,
            Punishment.Type.MUTE,
            reason,
            expiration,
            issuer,
            System.currentTimeMillis()
        )
    }

    private fun notifyModerator(sender: CommandSender, targetName: String, punishment: Punishment, punishmentId: Int) {
        val durationStr = formatDurationString(punishment.expiration)
        val message = Component.text()
            .append(Component.text("You have muted ", Palette.FG))
            .append(Component.text(targetName, Palette.YELLOW))
            .append(Component.text(" $durationStr", Palette.FG))
            .append(Component.text(punishment.reason?.let { " for: $it" } ?: "", Palette.FG))
            .append(Component.text(" (ID: $punishmentId)", Palette.GRAY))
            .clickEvent(ClickEvent.suggestCommand("/evidence $punishmentId "))
            .hoverEvent(Component.text("Click to add evidence", Palette.GRAY))
            .build()

        sender.sendMessage(message)
    }

    private fun notifyTarget(target: OnlinePlayer, punishment: Punishment) {
        val durationStr = formatDurationString(punishment.expiration)
        target.player.sendMessage(
            Component.text("You have been muted $durationStr${punishment.reason?.let { " for: $it" } ?: ""}.", Palette.RED)
        )
    }

    private fun logAuditEntry(moderator: String, targetName: String, punishment: Punishment, punishmentId: Int) {
        val durationStr = formatDurationString(punishment.expiration)
        plugin.dataManager.addAuditLogEntry(AuditLog(
            0, // id is autogenned
            "MUTE",
            moderator,
            targetName,
            "ID: $punishmentId, Reason: ${punishment.reason ?: "None"}, Duration: $durationStr",
            System.currentTimeMillis()
        ))
    }

    private fun sendWebhookNotification(punishment: Punishment) {
        plugin.webhook.sendPunishmentInfo(punishment)
    }

    private fun parseDuration(duration: String): Long? {
        if (duration == "permanent") return null
        val amount = duration.dropLast(1).toLongOrNull() ?: throw IllegalArgumentException("Invalid duration format")
        return when (duration.last().lowercaseChar()) {
            's' -> TimeUnit.SECONDS.toMillis(amount)
            'm' -> TimeUnit.MINUTES.toMillis(amount)
            'h' -> TimeUnit.HOURS.toMillis(amount)
            'd' -> TimeUnit.DAYS.toMillis(amount)
            else -> throw IllegalArgumentException("Invalid time unit")
        } + System.currentTimeMillis()
    }

    private fun formatDurationString(expiration: Long?): String {
        return when {
            expiration == null -> "permanently"
            else -> "for ${formatDuration(expiration - System.currentTimeMillis())}"
        }
    }

    private fun formatDuration(duration: Long): String {
        val days = TimeUnit.MILLISECONDS.toDays(duration)
        val hours = TimeUnit.MILLISECONDS.toHours(duration) % 24
        val minutes = TimeUnit.MILLISECONDS.toMinutes(duration) % 60
        val seconds = TimeUnit.MILLISECONDS.toSeconds(duration) % 60

        return buildString {
            if (days > 0) append("$days days ")
            if (hours > 0) append("$hours hours ")
            if (minutes > 0) append("$minutes minutes ")
            if (seconds > 0) append("$seconds seconds")
        }.trim()
    }

    companion object {
        fun registerCompletions(plugin: Ivy) {
            plugin.commandManager.commandCompletions.registerCompletion("durations") { _ -> Time.entries.map { it.duration } }
            plugin.commandManager.commandCompletions.registerCompletion("reasons") { _ -> Reasons.entries.map { it.reason } }
        }
    }
}