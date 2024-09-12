/**
 * Evidence.kt
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