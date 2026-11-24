package dev.zyklone.echoes.commands

import dev.zyklone.echoes.Echoes
import dev.zyklone.echoes.Echoes.Companion.runOnScheduler
import dev.zyklone.echoes.contents.echoes.EchoRegistry
import dev.zyklone.echoes.contents.echoes.PlayerEchoData
import dev.zyklone.echoes.contents.echoes.echoData
import dev.zyklone.echoes.contents.echoes.isEchoData
import dev.zyklone.echoes.gui.GuiEnhanceEquipped
import dev.zyklone.echoes.gui.GuiEnhanceOption
import dev.zyklone.echoes.gui.GuiEquip
import org.incendo.cloud.paper.util.sender.PlayerSource

object MainCommand {
    fun register(mgr: CommandManager) {
        val root = mgr.commandBuilder("echoes")
            .permission("echoes.command")
        mgr.command(equip(root))
        mgr.command(skill(root))
        mgr.command(enhance(root))
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

    private fun enhance(builder: CommandBuilder) =
        builder.literal("enhance")
            .senderType(PlayerSource::class.java)
            .handler {
                val player = it.sender().source()
                if (player.inventory.itemInMainHand.isEmpty) {
                    GuiEnhanceEquipped.openAsync(player)
                } else {
                    player.runOnScheduler { _ ->
                        val ih = player.inventory.itemInMainHand
                        if (ih.persistentDataContainer.isEchoData) {
                            GuiEnhanceOption.openSync(player, ih.persistentDataContainer.echoData!!) { data ->
                                val lore = requireNotNull(EchoRegistry[data.key]).asItem(data).itemMeta.lore()
                                ih.editMeta { meta ->
                                    meta.persistentDataContainer.echoData = data
                                    meta.lore(lore)
                                }
                            }
                        }
                    }
                }
            }
}
