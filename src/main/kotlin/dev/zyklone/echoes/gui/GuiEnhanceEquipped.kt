package dev.zyklone.echoes.gui

import dev.zyklone.echoes.Echoes.Companion.runOnScheduler
import dev.zyklone.echoes.commands.append
import dev.zyklone.echoes.contents.echoes.EchoRegistry
import dev.zyklone.echoes.contents.echoes.PlayerEchoData
import dev.zyklone.echoes.contents.items.EchoItems
import dev.zyklone.echoes.contents.sonata.SonataRegistry
import io.papermc.paper.dialog.Dialog
import io.papermc.paper.registry.data.dialog.ActionButton
import io.papermc.paper.registry.data.dialog.DialogBase
import io.papermc.paper.registry.data.dialog.action.DialogAction
import io.papermc.paper.registry.data.dialog.type.DialogType
import net.kyori.adventure.text.Component
import org.bukkit.entity.Player

object GuiEnhanceEquipped {
    private val base = DialogBase.builder(Component.text("强化"))
        .canCloseWithEscape(true)
        .build()
    private val empty = ActionButton.builder(GuiEquip.empty)
        .build()

    fun openAsync(player: Player) {
        val data = PlayerEchoData[player]
        val actions: List<ActionButton> = data.echo.mapIndexed { index, it ->
            if (it == null) {
                empty
            } else {
                val echo = EchoRegistry[it.key]
                val sonata = SonataRegistry[it.sonataEffect]
                if (echo == null || sonata == null) {
                    empty
                } else {
                    ActionButton.builder(
                        Component.text()
                            .append(it, echo, sonata)
                            .build()
                    )
                        .action(DialogAction.customClick({ _, _ ->
                            GuiEnhanceOption.openSync(player, it) {
                                data.echo[index] = it
                                data.update()
                                PlayerEchoData[player] = data
                                data.apply(player)
                            }
                        }, DEFAULT_CALLBACK_OPTION))
                        .tooltip(Component.text { builder ->
                            for (pair in it.attributes) {
                                builder.appendNewline().append(EchoItems.formatAttribute(pair))
                            }
                        })
                        .build()
                }
            }
        }

        val dialog = Dialog.create {
            it.empty()
                .base(base)
                .type(DialogType.multiAction(actions)
                    .build())
        }
        player.runOnScheduler {
            player.showDialog(dialog)
        }
    }
}
