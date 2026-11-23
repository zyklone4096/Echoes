package dev.zyklone.echoes.contents.echoes

import dev.zyklone.echoes.contents.echoes.builtin.Skeleton
import dev.zyklone.echoes.contents.echoes.builtin.Wither
import dev.zyklone.echoes.contents.echoes.builtin.Zombie
import org.bukkit.NamespacedKey
import org.bukkit.entity.EntityType
import org.incendo.cloud.bukkit.parser.NamespacedKeyParser.namespacedKeyParser
import org.incendo.cloud.parser.ArgumentParseResult
import org.incendo.cloud.suggestion.SuggestionProvider
import java.util.concurrent.CompletableFuture

object EchoRegistry {
    private val vanilla = HashMap<EntityType, Echo>()

    private val registry = HashMap<NamespacedKey, Echo>()

    fun init() {
        this.add(Skeleton())
        this.add(Wither())
        this.add(Zombie())
    }

    fun add(echo: Echo) {
        this.registry[echo.key] = echo
        when (val drop = echo.drop) {
            is EchoDrop.Vanilla -> vanilla[drop.type] = echo
        }
    }

    operator fun get(key: NamespacedKey) =
        this.registry[key]

    fun getVanilla(type: EntityType) =
        this.vanilla[type]

    fun size() = this.registry.size

    fun keys() = this.registry.keys.toList()

    fun <C> argumentParser() = namespacedKeyParser<C>().parser()
        .flatMapSuccess { _, key ->
            CompletableFuture.completedFuture(
                get(key)?.let(ArgumentParseResult<Echo>::success)
                    ?: ArgumentParseResult.failure(IllegalArgumentException("No such Echo"))
            )
        }

    fun <C> suggestionProvider() = SuggestionProvider.blockingStrings<C> { _, input ->
        val inp = input.readString()
        buildList {
            for (key in keys()) {
                val str = key.toString()
                if (key.value().startsWith(inp) || str.startsWith(inp)) {
                    add(str)
                }
            }
        }
    }
}
