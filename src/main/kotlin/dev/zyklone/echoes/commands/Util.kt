package dev.zyklone.echoes.commands

import dev.zyklone.echoes.contents.echoes.Echo
import dev.zyklone.echoes.contents.echoes.EchoData
import dev.zyklone.echoes.contents.sonata.Sonata
import dev.zyklone.echoes.utils.literal
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.TextComponent
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.NamespacedKey
import org.incendo.cloud.Command
import org.incendo.cloud.paper.PaperCommandManager
import org.incendo.cloud.paper.util.sender.PlayerSource
import org.incendo.cloud.paper.util.sender.Source
import org.incendo.cloud.suggestion.SuggestionProvider

internal typealias CommandManager = PaperCommandManager<Source>
internal typealias CommandBuilder = Command.Builder<Source>
internal typealias PlayerCommandBuilder = Command.Builder<PlayerSource>

val MESSAGE_HEADER = Component.text()
    .append("Echoes".literal().decorate(TextDecoration.BOLD))
    .append(" | ".literal(NamedTextColor.GRAY))
    .build()

fun <C> Collection<NamespacedKey>.suggestions() = SuggestionProvider.blockingStrings<C> { context, input ->
    val inp = input.readString()
    buildList {
        for (key in this@suggestions) {
            val str = key.toString()
            if (key.value().startsWith(inp) || str.startsWith(inp)) {
                add(str)
            }
        }
    }
}

fun TextComponent.Builder.append(data: EchoData, echo: Echo, sonata: Sonata) =
    append("${echo.cost} COST ".literal())
        .append(sonata.displayName())
        .append(Component.space())
        .append(echo.displayName(data.rarity))
