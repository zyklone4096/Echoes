package dev.zyklone.echoes.gui

import dev.zyklone.echoes.Echoes
import dev.zyklone.echoes.commands.MESSAGE_HEADER
import dev.zyklone.echoes.commands.append
import dev.zyklone.echoes.contents.attributes.EchoAttributes
import dev.zyklone.echoes.contents.echoes.EchoRegistry
import dev.zyklone.echoes.contents.echoes.PlayerEchoData
import dev.zyklone.echoes.contents.echoes.echoData
import dev.zyklone.echoes.contents.items.EchoItems
import dev.zyklone.echoes.contents.sonata.SonataRegistry
import dev.zyklone.echoes.utils.literal
import dev.zyklone.echoes.utils.styleless
import io.papermc.paper.dialog.Dialog
import io.papermc.paper.registry.data.dialog.ActionButton
import io.papermc.paper.registry.data.dialog.DialogBase
import io.papermc.paper.registry.data.dialog.action.DialogAction
import io.papermc.paper.registry.data.dialog.type.DialogType
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.entity.Player

object GuiEquip {
    private val none = Component.text()
        .append(MESSAGE_HEADER)
        .append("当前物品不是有效声骸".literal())
        .build()

    private val base = DialogBase.builder("装备声骸".literal())
        .canCloseWithEscape(true)
        .build()
    private val exit = ActionButton.builder("取消".literal())
        .build()
    private val empty = "空".literal(NamedTextColor.GRAY)
    private val badOld = "由于先前装备的声骸存在问题，无法生成物品".literal()
    private val noEmptySlot = "背包没有剩余空间，无法完成替换".literal()

    internal fun openEquipAsync(player: Player) {
        val heldItem = player.inventory.itemInMainHand
        val current = if (heldItem.type.isAir) null else
            heldItem.persistentDataContainer.echoData ?: return player.sendMessage(this.none)
        val echo = current?.let {
            EchoRegistry[it.key] ?: return player.sendMessage(this.none)
        }
        val sonata = current?.let {
            SonataRegistry[it.sonataEffect] ?: return player.sendMessage(this.none)
        }

        val ped = PlayerEchoData[player]
        val actions = ped.echo.mapIndexed { index, data ->
            val label = if (data == null) {
                empty
            } else {
                val equippedEcho = EchoRegistry[data.key]
                val equippedSonata = SonataRegistry[data.sonataEffect]
                if (equippedEcho == null || equippedSonata == null) badOld else
                    Component.text().append(data, equippedEcho, equippedSonata).build()
            }
            val button = ActionButton.builder(label)
            if (current != null || data != null) {
                button.action(DialogAction.customClick({ _, _ ->
                    val old = ped.echo[index]
                    if (old != null && player.inventory.firstEmpty() < 0)
                        return@customClick player.sendMessage(noEmptySlot)
                    val oldItem = old?.let {
                        val oldEcho = EchoRegistry[it.key]
                        if (oldEcho == null || SonataRegistry[it.sonataEffect] == null)
                            return@customClick player.sendMessage(badOld)
                        oldEcho.asItem(it)
                    }

                    if (current != null)
                        player.inventory.itemInMainHand.amount = 0
                    ped.echo[index] = current
                    ped.update()
                    ped.apply(player)

                    if (current != null && echo != null && sonata != null) {
                        player.sendMessage(Component.text()
                            .append(MESSAGE_HEADER)
                            .append("已装备 ".literal())
                            .append(current, echo, sonata)
                            .build())
                    } else {
                        player.sendMessage(Component.text()
                            .append(MESSAGE_HEADER)
                            .append("已卸下声骸".literal())
                            .build())
                    }

                    if (oldItem != null)
                        player.inventory.addItem(oldItem)

                    PlayerEchoData[player] = ped
                }, DEFAULT_CALLBACK_OPTION))
            }
            if (data != null) {
                button.tooltip(Component.text {
                    for (pair in data.attributes) {
                        it.appendNewline()
                        if (pair == null) {
                            it.append(EchoItems.emptyAttribute)
                        } else {
                            val (key, value) = pair
                            it.append(Component.text()
                                .styleless()
                                .append(EchoItems.tick)
                                .append(requireNotNull(EchoAttributes[key]) { "Unknown attribute $key" }.renderWide(value))
                                .build())
                        }
                    }
                })
            }
            button.build()
        }

        player.scheduler.run(Echoes.INST, {
            player.showDialog(Dialog.create {
                it.empty()
                    .base(this.base)
                    .type(DialogType.multiAction(
                        actions,
                        exit,
                        1,
                    ))
            })
        }, null)
    }
}
