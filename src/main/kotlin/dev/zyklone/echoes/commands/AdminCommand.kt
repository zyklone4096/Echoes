package dev.zyklone.echoes.commands

import dev.zyklone.echoes.Echoes
import dev.zyklone.echoes.contents.attributes.EchoAttributes
import dev.zyklone.echoes.contents.echoes.Echo
import dev.zyklone.echoes.contents.echoes.EchoRegistry
import dev.zyklone.echoes.contents.sonata.Sonata
import dev.zyklone.echoes.contents.sonata.SonataRegistry
import dev.zyklone.echoes.utils.literal
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.incendo.cloud.Command
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

    private fun give(mgr: CommandManager, root: CommandBuilder) = root.literal("give")
        .senderType(PlayerSource::class.java)
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

            val data = echo.createRandom(player, sonata = sonata)
            val created = echo.asItem(data)
            player.scheduler.run(Echoes.INST, {
                if (player.inventory.firstEmpty() < 0)
                    return@run player.sendMessage(Component.text()
                        .append(MESSAGE_HEADER)
                        .append("物品栏中没有空槽".literal())
                        .build())

                player.inventory.addItem(created)
                player.sendMessage(Component.text()
                    .append(MESSAGE_HEADER)
                    .append("已添加 ".literal())
                    .append(echo.displayName(data.rarity)))
            }, null)
        }
}
