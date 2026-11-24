package dev.zyklone.echoes.gui

import dev.zyklone.echoes.Echoes
import dev.zyklone.echoes.commands.MESSAGE_HEADER
import dev.zyklone.echoes.commands.append
import dev.zyklone.echoes.contents.echoes.EchoData
import dev.zyklone.echoes.contents.echoes.EchoRegistry
import dev.zyklone.echoes.contents.echoes.PlayerEchoData
import dev.zyklone.echoes.contents.echoes.echoData
import dev.zyklone.echoes.contents.items.EchoItems
import dev.zyklone.echoes.contents.player.PlayerDataAccess
import dev.zyklone.echoes.contents.player.PlayerManager
import dev.zyklone.echoes.contents.sonata.SonataRegistry
import dev.zyklone.echoes.utils.literal
import io.papermc.paper.dialog.Dialog
import io.papermc.paper.registry.data.dialog.ActionButton
import io.papermc.paper.registry.data.dialog.DialogBase
import io.papermc.paper.registry.data.dialog.action.DialogAction
import io.papermc.paper.registry.data.dialog.body.DialogBody
import io.papermc.paper.registry.data.dialog.type.DialogType
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.entity.Player

object GuiEquip {
    private val none = Component.text()
        .append(MESSAGE_HEADER)
        .append("当前物品不是有效声骸".literal())
        .build()

    private val exit = ActionButton.builder("取消".literal())
        .build()
    val empty = "空".literal(NamedTextColor.GRAY)
    private val badOld = "由于先前装备的声骸存在问题，无法生成物品".literal()
    private val noEmptySlot = "背包没有剩余空间，无法完成替换".literal()
    private val changedItem = "手持物品已改变，请重新打开装备界面".literal()
    private val loadFailed = "数据坞加载失败，请稍后重试".literal(NamedTextColor.RED)

    internal fun openEquipAsync(player: Player) {
        PlayerManager.loadAsync(player).whenComplete { data, error ->
            if (error != null)
                Echoes.INST.slF4JLogger.error("Failed to load data bank for ${player.uniqueId}", error)
            player.scheduler.run(Echoes.INST, {
                if (error != null) player.sendMessage(loadFailed) else openEquip(player, data)
            }, null)
        }
    }

    private fun openEquip(player: Player, playerData: PlayerDataAccess) {
        val heldItem = player.inventory.itemInMainHand.clone()
        val current = if (heldItem.type.isAir) null else
            heldItem.persistentDataContainer.echoData ?: return player.sendMessage(this.none)
        val echo = current?.let {
            EchoRegistry[it.key] ?: return player.sendMessage(this.none)
        }
        val sonata = current?.let {
            SonataRegistry[it.sonataEffect] ?: return player.sendMessage(this.none)
        }

        val equippedData = PlayerEchoData[player]
        val actions = equippedData.echo.mapIndexed { index, data ->
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
                    player.scheduler.run(Echoes.INST, {
                        val ped = PlayerEchoData[player]
                        if (current != null && !player.inventory.itemInMainHand.isSimilar(heldItem))
                            return@run player.sendMessage(changedItem)
                        if (current != null && echo != null) {
                            val remainingCost = totalCost(ped.echo, index)
                                ?: return@run player.sendMessage(badOld)
                            val nextCost = remainingCost + echo.cost
                            val maxCost = playerData.dataBank.maxCost
                            if (nextCost > maxCost)
                                return@run player.sendMessage(Component.text()
                                    .append(MESSAGE_HEADER)
                                    .append("装备后 COST 为 $nextCost，超过数据坞上限 $maxCost".literal(NamedTextColor.RED))
                                    .build())
                        }
                        val old = ped.echo[index]
                        if (old != null && player.inventory.firstEmpty() < 0)
                            return@run player.sendMessage(noEmptySlot)
                        val oldItem = old?.let {
                            val oldEcho = EchoRegistry[it.key]
                            if (oldEcho == null || SonataRegistry[it.sonataEffect] == null)
                                return@run player.sendMessage(badOld)
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
                    }, null)
                }, DEFAULT_CALLBACK_OPTION))
            }
            if (data != null) {
                button.tooltip(Component.text {
                    for (pair in data.attributes) {
                        it.appendNewline()
                            .append(EchoItems.formatAttribute(pair))
                    }
                })
            }
            button.build()
        }

        val cost = totalCost(equippedData.echo)?.toString() ?: "未知"
        val maxCost = playerData.dataBank.maxCost
        val base = DialogBase.builder("装备声骸".literal())
            .canCloseWithEscape(true)
            .body(listOf(DialogBody.plainMessage("COST：$cost / $maxCost".literal())))
            .build()
        player.showDialog(Dialog.create {
            it.empty()
                .base(base)
                .type(DialogType.multiAction(
                    actions,
                    exit,
                    1,
                ))
        })
    }

    private fun totalCost(echoes: List<EchoData?>, excludedIndex: Int = -1): Int? {
        var cost = 0
        for ((index, data) in echoes.withIndex()) {
            if (index == excludedIndex || data == null) continue
            cost += EchoRegistry[data.key]?.cost ?: return null
        }
        return cost
    }
}
