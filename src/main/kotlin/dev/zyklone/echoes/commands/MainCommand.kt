package dev.zyklone.echoes.commands

import dev.zyklone.echoes.Echoes
import dev.zyklone.echoes.contents.echoes.PlayerEchoData
import dev.zyklone.echoes.gui.GuiEquip
import org.incendo.cloud.paper.util.sender.PlayerSource

object MainCommand {
    fun register(mgr: CommandManager) {
        val root = mgr.commandBuilder("echoes")
            .permission("echoes.command")
        mgr.command(equip(root))
        mgr.command(skill(root))
    }

    private fun equip(builder: CommandBuilder) =
        builder.literal("equip")
            .senderType(PlayerSource::class.java)
            .handler {
                GuiEquip.openEquipAsync(it.sender().source())
            }

    private fun skill(builder: CommandBuilder) =
        builder.literal("skill")
            .senderType(PlayerSource::class.java)
            .handler {
                val player = it.sender().source()
                player.scheduler.run(Echoes.INST, {
                    PlayerEchoData[player].doSkill(player)
                }, null)
            }
}
