package cat.aubrey.ivy.cmd

import cat.aubrey.ivy.Ivy
import cat.aubrey.ivy.types.Palette
import co.aikar.commands.BaseCommand
import co.aikar.commands.annotation.*
import net.kyori.adventure.text.Component
import org.bukkit.command.CommandSender

@CommandAlias("evidence")
class Evidence(private val plugin: Ivy) : BaseCommand() {

    @Default
    @Description("Add evidence to a punishment")
    @CommandCompletion("@punishments")
    @CommandPermission("ivy.evidence.add")
    @Syntax("<punishmentId> <evidence>")
    fun onAddEvidence(sender: CommandSender, punishmentId: Int, evidence: String) {
        val result = plugin.dataManager.addEvidence(punishmentId, evidence)
        if (result) {
            sender.sendMessage(Component.text("Evidence added successfully to punishment #$punishmentId.", Palette.GREEN))
        } else {
            sender.sendMessage(Component.text("Failed to add evidence. Punishment #$punishmentId not found.", Palette.RED))
        }
    }

    companion object {
        fun registerCompletions(plugin: Ivy) {
            plugin.commandManager.commandCompletions.registerAsyncCompletion("punishments") { _ ->
                plugin.dataManager.getRecentPunishmentIds(10)
            }
        }
    }
}