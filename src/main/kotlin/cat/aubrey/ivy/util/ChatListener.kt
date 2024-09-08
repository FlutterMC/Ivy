@file:Suppress("DEPRECATION")

/**
 * ChatListener.kt
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

package cat.aubrey.ivy.util

import cat.aubrey.ivy.Ivy
import cat.aubrey.ivy.types.Palette
import cat.aubrey.ivy.types.Punishment
import net.kyori.adventure.text.Component
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.AsyncPlayerChatEvent
import java.util.concurrent.TimeUnit

@Suppress("DEPRECATION")
class ChatListener(private val plugin: Ivy) : Listener {

    @EventHandler
    fun onPlayerChat(event: AsyncPlayerChatEvent) {
        val player = event.player
        val mutePunishment = plugin.dataManager.getActivePunishment(player.uniqueId, Punishment.Type.MUTE)

        mutePunishment?.let { punishment ->
            event.isCancelled = true
            val message = createMuteMessage(punishment)
            player.sendMessage(message)

            if (punishment.hasExpired()) {
                plugin.dataManager.removePunishment(player.uniqueId, Punishment.Type.MUTE)
            }
        }
    }

    private fun createMuteMessage(punishment: Punishment): Component {
        return when {
            punishment.expiration != null -> createTemporaryMuteMessage(punishment.expiration)
            else -> Component.text("You are permanently muted and cannot send messages.", Palette.RED)
        }
    }

    private fun createTemporaryMuteMessage(expiration: Long): Component {
        val remainingTime = expiration - System.currentTimeMillis()
        return if (remainingTime > 0) {
            Component.text()
                .append(Component.text("You are muted. ", Palette.RED))
                .append(Component.text("Your mute will expire in ", Palette.YELLOW))
                .append(Component.text(formatDuration(remainingTime), Palette.AQUA))
                .append(Component.text("."))
                .build()
        } else {
            Component.text("Your mute has expired. You can now send messages.", Palette.GREEN)
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

    private fun Punishment.hasExpired(): Boolean =
        expiration != null && expiration <= System.currentTimeMillis()
}