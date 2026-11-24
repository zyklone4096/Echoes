package dev.zyklone.echoes.gui

import dev.zyklone.echoes.Echoes
import dev.zyklone.echoes.Echoes.Companion.runOnScheduler
import dev.zyklone.echoes.commands.MESSAGE_HEADER
import dev.zyklone.echoes.commands.append
import dev.zyklone.echoes.contents.attributes.EchoAttributes
import dev.zyklone.echoes.contents.echoes.EchoData
import dev.zyklone.echoes.contents.echoes.EchoEnhancement
import dev.zyklone.echoes.contents.echoes.EchoRegistry
import dev.zyklone.echoes.contents.items.EchoItems
import dev.zyklone.echoes.contents.items.TunerType
import dev.zyklone.echoes.contents.sonata.SonataRegistry
import dev.zyklone.echoes.utils.literal
import io.papermc.paper.dialog.Dialog
import io.papermc.paper.registry.data.dialog.ActionButton
import io.papermc.paper.registry.data.dialog.DialogBase
import io.papermc.paper.registry.data.dialog.action.DialogAction
import io.papermc.paper.registry.data.dialog.input.DialogInput
import io.papermc.paper.registry.data.dialog.type.DialogType
import net.kyori.adventure.text.Component
import org.bukkit.entity.Player

object GuiUpgrade {
    fun openAsync(player: Player, data: EchoData, callback: EchoEnhanceCallback) {
        player.runOnScheduler {
            this.openSync(player, data, callback)
        }
    }

    fun openSync(player: Player, data: EchoData, callback: EchoEnhanceCallback) {
        player.showDialog(this.ui(player, data, callback) ?: return)
    }

    fun openTuneSync(player: Player, data: EchoData, callback: EchoEnhanceCallback) {
        player.showDialog(this.ui(player, data, callback, tuneOnly = true) ?: return)
    }

    private val targetLevel = "目标等级".literal()
    private val autoTune = "自动调谐".literal()
    private val badData = Component.text()
        .append(MESSAGE_HEADER)
        .append("数据错误，请重试".literal())
        .build()

    private fun ui(player: Player, data: EchoData, callback: EchoEnhanceCallback, tuneOnly: Boolean = false): Dialog? {
        val echo = EchoRegistry[data.key]
        val sonata = SonataRegistry[data.sonataEffect]
        if (echo == null || sonata == null || data.level > data.maxLevel) {
            player.sendMessage(GuiEnhanceOption.invalid)
            return null
        }

        return Dialog.create {
            it.empty()
                .base(DialogBase.builder(Component.text()
                    .append(if (tuneOnly) "调谐 ".literal() else "升级 ".literal())
                    .append(data, echo, sonata)
                    .build())
                    .inputs(buildList {
                        if (!tuneOnly) {
                            if (data.level < data.maxLevel) {
                                add(DialogInput.numberRange("target", targetLevel, data.level.toFloat(), data.maxLevel.toFloat())
                                    .step(1f)
                                    .initial(data.level.toFloat())
                                    .build())
                            }
                            add(DialogInput.bool("auto_tune", autoTune).build())
                        }
                    })
                    .build())
                .type(DialogType.confirmation(
                    ActionButton.builder(if (tuneOnly) "调谐".literal() else "升级".literal())
                        .tooltip("材料不足时升级至可达最高等级；每槽调谐消耗 10 个对应品质调谐器".literal())
                        .action(DialogAction.customClick({ resp, _ ->
                            val target = if (tuneOnly || data.level >= data.maxLevel) data.level.toFloat() else resp.getFloat("target")
                            val autoTune = if (tuneOnly) true else resp.getBoolean("auto_tune")

                            if (target == null || !target.isFinite() || target % 1f != 0f ||
                                target < data.level || target > data.maxLevel || autoTune == null) {
                                player.sendMessage(badData)
                                return@customClick
                            }
                            player.runOnScheduler {
                                this.enhance(player, data, target.toInt(), autoTune, callback)
                            }
                        }, DEFAULT_CALLBACK_OPTION))
                        .build(),
                    ActionButton.builder("取消".literal()).build(),
                ))
        }
    }

    private fun enhance(player: Player, data: EchoData, target: Int, autoTune: Boolean, callback: EchoEnhanceCallback) {
        val echo = EchoRegistry[data.key]
        if (echo == null || SonataRegistry[data.sonataEffect] == null) {
            player.sendMessage(GuiEnhanceOption.invalid)
            return
        }

        val inventory = player.inventory
        val contents = inventory.storageContents
        val materials = contents.mapIndexedNotNull { slot, item ->
            if (item == null || item.isEmpty) return@mapIndexedNotNull null
            val type = EchoItems.getSealedTubeType(item) ?: return@mapIndexedNotNull null
            EchoEnhancement.UpgradeMaterial(slot, type, item.amount)
        }
        val upgrade = EchoEnhancement.upgrade(data, target, Echoes.INST.config.gameplay.echoLevelIncrement, materials)
        val tunerType = TunerType.entries.getOrNull(data.rarity.toInt() - 1)
        val tuners = if (autoTune && tunerType != null) contents.mapIndexedNotNull { slot, item ->
            if (item == null || item.isEmpty || slot in upgrade.consumed || EchoItems.getTunerType(item) != tunerType) null
            else slot to item.amount
        } else emptyList()
        val tunerCount = tuners.sumOf { it.second.toLong() }.coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
        val tune = EchoEnhancement.tune(upgrade.data, tunerCount) { blacklist ->
            if (blacklist.count { EchoAttributes[it] != null } >= EchoAttributes.size()) null
            else EchoAttributes.random(player, echo, blacklist.toMutableList())
        }

        val consumed = upgrade.consumed.toMutableMap()
        var remainingTuners = tune.consumed
        for ((slot, amount) in tuners) {
            if (remainingTuners == 0) break
            val count = minOf(amount, remainingTuners)
            consumed[slot] = count
            remainingTuners -= count
        }
        // storageContents 只包含背包与快捷栏，不使用盔甲或副手中的物品。
        for ((slot, amount) in consumed) {
            val item = requireNotNull(contents[slot]).clone()
            item.amount -= amount
            inventory.setItem(slot, if (item.isEmpty) null else item)
        }
        val updated = tune.data
        if (updated.level != data.level || updated.exp != data.exp || tune.tunedSlots > 0) {
            callback(updated)
        }
        player.sendMessage(Component.text()
            .append(MESSAGE_HEADER)
            .append("声骸等级：${updated.level}/${data.maxLevel}，剩余经验：${updated.exp}，本次调谐：${tune.tunedSlots} 个槽位".literal())
            .append(if (updated.level < target) "（密音筒不足，已升级至可达最高等级）".literal() else Component.empty())
            .build())
        if (autoTune && tunerType == null) {
            player.sendMessage(MESSAGE_HEADER.append("一星声骸无法调谐".literal()))
        } else if (autoTune && updated.attributes.drop(EchoData.MAIN_ATTRIBUTE_COUNT).any { it == null }) {
            player.sendMessage(MESSAGE_HEADER.append("调谐器不足或无可用副属性，未完成的槽位已保留".literal()))
        }
    }
}
