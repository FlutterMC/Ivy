/**
 * Rollback.kt
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
import cat.aubrey.ivy.types.Punishment
import cat.aubrey.ivy.types.Time
import co.aikar.commands.BaseCommand
import co.aikar.commands.annotation.*
import co.aikar.commands.annotation.Optional
import net.kyori.adventure.text.Component
import org.bukkit.command.CommandSender
import java.time.Duration
import java.time.Instant
import java.util.*

@CommandAlias("rollback")
class Rollback(private val plugin: Ivy) : BaseCommand() {

    @Default
    @CommandCompletion("@players @timeperiods MUTE|BAN|KICK|ALL")
    @Description("Rollback punishments issued by a moderator")
    @Syntax("<moderator> <period> [type]")
    @CommandPermission("ivy.rollback")
    fun onRollback(
        sender: CommandSender,
        moderator: String,
        period: String,
        @Optional type: String?
    ) {
        val timePeriod = Time.fromString(period)
        if (timePeriod == null) {
            sender.sendMessage(Component.text("Invalid period format. Use values from the Time enum.", Palette.RED))
            return
        }

        val duration = parseDuration(timePeriod.duration)
        if (duration == null) {
            sender.sendMessage(Component.text("Invalid period format. Use values from the Time enum.", Palette.RED))
            return
        }

        val punishmentType = if (type != null) {
            try {
                Punishment.Type.valueOf(type.uppercase(Locale.getDefault()))
            } catch (e: IllegalArgumentException) {
                if (type.uppercase(Locale.getDefault()) != "ALL") {
                    sender.sendMessage(Component.text("Invalid punishment type. Use MUTE, BAN, KICK, or ALL.", Palette.RED))
                    return
                }
                null
            }
        } else null

        val rollbackTime = Instant.now().minus(duration).toEpochMilli()
        val rolledBackPunishments = plugin.dataManager.rollbackPunishments(moderator, rollbackTime, punishmentType)

        if (rolledBackPunishments.isEmpty()) {
            sender.sendMessage(Component.text("No punishments found to rollback.", Palette.YELLOW))
        } else {
            sender.sendMessage(Component.text("Rolled back ${rolledBackPunishments.size} punishments:", Palette.GREEN))
            rolledBackPunishments.forEach { punishment ->
                sender.sendMessage(Component.text("- ${punishment.type} for ${punishment.playerUUID} issued at ${Instant.ofEpochMilli(punishment.issuedAt)}", Palette.FG))
            }
        }
    }

    private fun parseDuration(period: String): Duration? {
        val amount = period.dropLast(1).toLongOrNull() ?: return null
        return when (period.last().lowercaseChar()) {
            'm' -> Duration.ofMinutes(amount)
            'h' -> Duration.ofHours(amount)
            'd' -> Duration.ofDays(amount)
            else -> null
        }
    }
}