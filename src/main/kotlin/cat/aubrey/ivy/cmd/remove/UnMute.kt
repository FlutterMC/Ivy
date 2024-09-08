package cat.aubrey.ivy.cmd.remove

import cat.aubrey.ivy.Ivy
import cat.aubrey.ivy.types.Palette
import cat.aubrey.ivy.types.Punishment
import co.aikar.commands.BaseCommand
import co.aikar.commands.annotation.*
import net.kyori.adventure.text.Component
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

@CommandAlias("unmute")
class UnMute(private val plugin: Ivy) : BaseCommand() {

    @Default
    @CommandCompletion("@players")
    @Description("Unmute a player")
    @Syntax("<player>")
    @CommandPermission("ivy.unmute")
    fun onUnMute(
        sender: CommandSender,
        @Flags("other") target: Player
    ) {
        val removed = try {
            plugin.dataManager.removePunishment(target.uniqueId, Punishment.Type.MUTE)
        } catch (e: Exception) {
            sender.sendMessage(Component.text("An error occurred while trying to unmute the player.", Palette.RED))
            plugin.logger.severe("Error unmuting player ${target.name}: ${e.message}")
            return
        }

        if (removed) {
            val message = Component.text()
                .append(Component.text("You have unmuted ", Palette.FG))
                .append(Component.text(target.name, Palette.YELLOW))
                .append(Component.text(".", Palette.FG))
                .build()

            sender.sendMessage(message)

            target.sendMessage(
                Component.text("You have been unmuted.", Palette.GREEN)
            )
        } else {
            sender.sendMessage(
                Component.text("${target.name} is not currently muted.", Palette.RED)
            )
        }
    }
}