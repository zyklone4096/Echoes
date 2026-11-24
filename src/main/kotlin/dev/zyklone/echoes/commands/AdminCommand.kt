package dev.zyklone.echoes.commands

import dev.zyklone.echoes.Echoes
import dev.zyklone.echoes.contents.attributes.EchoAttributes
import dev.zyklone.echoes.contents.echoes.Echo
import dev.zyklone.echoes.contents.echoes.EchoRegistry
import dev.zyklone.echoes.contents.items.EchoItems
import dev.zyklone.echoes.contents.items.SealedTubeType
import dev.zyklone.echoes.contents.items.TunerType
import dev.zyklone.echoes.contents.player.PlayerManager
import dev.zyklone.echoes.contents.sonata.Sonata
import dev.zyklone.echoes.contents.sonata.SonataRegistry
import dev.zyklone.echoes.database.DataBankModel
import dev.zyklone.echoes.utils.literal
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.future.await
import kotlinx.coroutines.launch
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.incendo.cloud.Command
import org.incendo.cloud.bukkit.data.SinglePlayerSelector
import org.incendo.cloud.bukkit.parser.selector.SinglePlayerSelectorParser
import org.incendo.cloud.paper.util.sender.PlayerSource
import org.incendo.cloud.paper.util.sender.Source
import org.incendo.cloud.parser.standard.IntegerParser
import kotlin.jvm.optionals.getOrNull

object AdminCommand {
    fun register(mgr: CommandManager) {
        val root = mgr.commandBuilder("echoes-admin")
            .permission("echoes.admin")

        mgr.command(status(root))
        mgr.command(give(mgr, root))
        dataBank(mgr, root)
    }

    private fun status(root: CommandBuilder) = root.literal("status")
        .handler {
            it.sender().source().sendMessage(Component.text { builder ->
                builder.append(MESSAGE_HEADER)
                    .append("可用属性数量：".literal())
                    .append(Component.text(EchoAttributes.size(), NamedTextColor.GREEN))
                    .appendNewline()
                builder.append(MESSAGE_HEADER)
                    .append("可用声骸数量：".literal())
                    .append(Component.text(EchoRegistry.size(), NamedTextColor.GREEN))
                    .appendNewline()
                builder.append(MESSAGE_HEADER)
                    .append("合鸣效果数量".literal())
                    .append(Component.text(SonataRegistry.size(), NamedTextColor.GREEN))
            })
        }

    private fun give(mgr: CommandManager, root: CommandBuilder): Command.Builder<Source> {
        val give = root.literal("give")
        val pcb = give.senderType(PlayerSource::class.java)
        mgr.command(giveEcho(mgr, pcb))
        mgr.command(giveSealedTube(mgr, pcb))
        mgr.command(giveTuner(mgr, pcb))
        return give
    }

    private fun giveEcho(mgr: CommandManager, root: PlayerCommandBuilder) = root.literal("echo")
        .argument(mgr.componentBuilder(Echo::class.java, "echo")
            .parser(EchoRegistry.argumentParser())
            .suggestionProvider(EchoRegistry.suggestionProvider()))
        .optional(mgr.componentBuilder(Sonata::class.java, "sonata")
            .parser(SonataRegistry.argumentParser())
            .suggestionProvider(SonataRegistry.suggestionProvider()))
        .optional("rarity", IntegerParser.integerParser(0, 4))
        .handler {
            val player = it.sender().source()

            val echo = it.get<Echo>("echo")
            val sonata = it.optional<Sonata>("sonata").getOrNull()
            val rarity = it.optional<Int>("rarity").getOrNull()?.toByte()

            val data = echo.createRandom(player, sonata = sonata, rarity = rarity)
            val created = echo.asItem(data)

            give0(player, created) {
                player.sendMessage(Component.text()
                    .append(MESSAGE_HEADER)
                    .append("已添加 ".literal())
                    .append(echo.displayName(data.rarity)))
            }
        }

    private fun giveSealedTube(mgr: CommandManager, root: PlayerCommandBuilder) = root.literal("sealed-tube")
        .argument(EnumArgumentParser.component("type", SealedTubeType::class.java))
        .optional(mgr.componentBuilder(Int::class.java, "amount"))
        .optional(SinglePlayerSelectorParser.singlePlayerSelectorComponent<PlayerSource>()
            .name("player"))
        .handler {
            val type: SealedTubeType = it["type"]
            val amount = it.optional<Int>("amount")
            val player = it.optional<Player>("player").orElseGet { it.sender().source() }

            val created = EchoItems.newSealedTube(type)
            created.amount = amount.orElse(1)!!

            give0(player, created) {
                player.sendMessage(Component.text()
                    .append(MESSAGE_HEADER)
                    .append("已添加 ".literal())
                    .append(created.displayName()))
            }
        }

    private fun giveTuner(mgr: CommandManager, root: PlayerCommandBuilder) = root.literal("tuner")
        .argument(EnumArgumentParser.component("type", TunerType::class.java))
        .optional(mgr.componentBuilder(Int::class.java, "amount"))
        .optional(SinglePlayerSelectorParser.singlePlayerSelectorComponent<PlayerSource>()
            .name("player"))
        .handler {
            val type: TunerType = it["type"]
            val amount = it.optional<Int>("amount")
            val player = it.optional<Player>("player").orElseGet { it.sender().source() }

            val created = EchoItems.newTuner(type)
            created.amount = amount.orElse(1)!!

            give0(player, created) {
                player.sendMessage(Component.text()
                    .append(MESSAGE_HEADER)
                    .append("已添加 ".literal())
                    .append(created.displayName()))
            }
        }

    private fun give0(player: Player, item: ItemStack, then: (() -> Unit)?) {
        player.scheduler.run(Echoes.INST, {
            if (player.inventory.firstEmpty() < 0)
                return@run player.sendMessage(Component.text()
                    .append(MESSAGE_HEADER)
                    .append("物品栏中没有空槽".literal())
                    .build())

            player.inventory.addItem(item)
            then?.invoke()
        }, null)
    }

    private fun dataBank(mgr: CommandManager, root: CommandBuilder) {
        val parent = root.literal("databank")
            .argument(SinglePlayerSelectorParser.singlePlayerSelectorComponent<Source>()
                .name("target"))
        mgr.command(dataBankAddLevel(parent))
    }

    private fun dataBankAddLevel(root: CommandBuilder) = root
        .literal("add-level")
        .required("value", IntegerParser.integerParser(1))
        .handler {
            val sender = it.sender().source()
            val player = it.get<SinglePlayerSelector>("target").single()
            val value = it.get<Int>("value")
            CoroutineScope(Echoes.INST.coroutines).launch {
                try {
                    val target = PlayerManager[player] ?: PlayerManager.loadAsync(player).await()
                    val updated = target.addDataBankLevel(value)
                    sender.sendMessage(Component.text()
                        .append(MESSAGE_HEADER)
                        .append("${player.name} 的数据坞等级已更新为 ".literal())
                        .append(Component.text(updated.level, NamedTextColor.GREEN))
                        .append(" 级（上限 ${DataBankModel.MAX_LEVEL} 级）".literal())
                        .build())
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Echoes.INST.slF4JLogger.error("Failed to add data bank levels for ${player.uniqueId}", e)
                    sender.sendMessage(Component.text()
                        .append(MESSAGE_HEADER)
                        .append("数据坞等级更新失败，请查看服务器日志".literal(NamedTextColor.RED))
                        .build())
                }
            }
        }
}
