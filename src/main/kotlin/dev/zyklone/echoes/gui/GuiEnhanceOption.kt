package dev.zyklone.echoes.gui

import dev.zyklone.echoes.Echoes.Companion.runOnScheduler
import dev.zyklone.echoes.commands.MESSAGE_HEADER
import dev.zyklone.echoes.commands.append
import dev.zyklone.echoes.contents.echoes.EchoData
import dev.zyklone.echoes.contents.echoes.EchoRegistry
import dev.zyklone.echoes.contents.sonata.SonataRegistry
import dev.zyklone.echoes.utils.literal
import io.papermc.paper.dialog.Dialog
import io.papermc.paper.registry.data.dialog.ActionButton
import io.papermc.paper.registry.data.dialog.DialogBase
import io.papermc.paper.registry.data.dialog.action.DialogAction
import io.papermc.paper.registry.data.dialog.type.DialogType
import net.kyori.adventure.text.Component
import org.bukkit.entity.Player

object GuiEnhanceOption {
    private val title = "强化".literal()

    fun openAsync(player: Player, data: EchoData, callback: EchoEnhanceCallback) {
        val ui = this.ui(player, data, callback)
        player.runOnScheduler {
            player.showDialog(ui ?: return@runOnScheduler)
        }
    }

    fun openSync(player: Player, data: EchoData, callback: EchoEnhanceCallback) {
        player.showDialog(ui(player, data, callback) ?: return)
    }

    private val upgrade = "升级".literal()
    private val tune = "调谐".literal()
    private val modify = "调频".literal()

    val invalid = Component.text()
        .append(MESSAGE_HEADER)
        .append("此声骸数据异常，无法继续".literal())
        .build()

    private fun ui(player: Player, data: EchoData, callback: EchoEnhanceCallback): Dialog? {
        val echo = EchoRegistry[data.key]
        val sonata = SonataRegistry[data.sonataEffect]
        if (echo == null || sonata == null) {
            player.sendMessage(invalid)
            return null
        }

        return Dialog.create {
            it.empty()
                .base(DialogBase.builder(Component.text()
                    .append(title)
                    .appendSpace()
                    .append(data, echo, sonata)
                    .build())
                    .build())
                .type(DialogType.multiAction(buildList {
                    if (data.level < data.maxLevel) {
                        add(ActionButton.builder(upgrade)
                            .action(DialogAction.customClick({ _, _ ->
                                GuiUpgrade.openSync(player, data, callback)
                            }, DEFAULT_CALLBACK_OPTION))
                            .build())
                    }
                    if (data.rarity.toInt() > 0 && data.attributes.drop(EchoData.MAIN_ATTRIBUTE_COUNT).any { it == null }) {
                        add(ActionButton.builder(tune)
                            .action(DialogAction.customClick({ _, _ ->
                                GuiUpgrade.openTuneSync(player, data, callback)
                            }, DEFAULT_CALLBACK_OPTION))
                            .build())
                    }
                    add(ActionButton.builder(modify)
                        .action(DialogAction.customClick({ _, _ ->

                        }, DEFAULT_CALLBACK_OPTION))
                        .build())
                }).build())
        }
    }
}
