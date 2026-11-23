package dev.zyklone.echoes.contents.sonata

import dev.zyklone.echoes.Echoes
import dev.zyklone.echoes.contents.echoes.EchoData
import dev.zyklone.echoes.contents.sonata.builtin.FreezingFrost
import dev.zyklone.echoes.contents.sonata.builtin.HavocEclipse
import dev.zyklone.echoes.contents.sonata.builtin.LingeringTunes
import dev.zyklone.echoes.contents.sonata.builtin.MoonlitClouds
import org.bukkit.NamespacedKey
import org.incendo.cloud.bukkit.parser.NamespacedKeyParser.namespacedKeyParser
import org.incendo.cloud.parser.ArgumentParseResult
import org.incendo.cloud.suggestion.SuggestionProvider
import java.util.concurrent.CompletableFuture

object SonataRegistry {
    private val registry = HashMap<NamespacedKey, Sonata>()

    val BUILTIN_FREEZING_FROST = NamespacedKey(Echoes.NAMESPACE, "freezing_frost")
    val BUILTIN_LINGERING_TUNES = NamespacedKey(Echoes.NAMESPACE, "lingering_tunes")
    val BUILTIN_HAVOC_ECLIPSE = NamespacedKey(Echoes.NAMESPACE, "havoc_eclipse")
    val BUILTIN_MOONLIT_CLOUDS = NamespacedKey(Echoes.NAMESPACE, "moonlit_clouds")

    fun init() {
        this.add(FreezingFrost())
        this.add(HavocEclipse())
        this.add(LingeringTunes())
        this.add(MoonlitClouds())
    }

    fun add(sonata: Sonata) {
        require(sonata.key !in registry) { "Attempted to register Sonata with duplicate key ${sonata.key}" }

        this.registry[sonata.key] = sonata
    }

    operator fun get(nk: NamespacedKey) = this.registry[nk]

    operator fun get(echoes: List<EchoData?>) =
        registry.values.filter { it.check(echoes) }

    fun size() = this.registry.size

    fun keys() = this.registry.keys.toList()

    fun <C> argumentParser() = namespacedKeyParser<C>(false, Echoes.NAMESPACE).parser()
        .flatMapSuccess { _, key ->
            CompletableFuture.completedFuture(
                get(key)?.let(ArgumentParseResult<Sonata>::success)
                    ?: ArgumentParseResult.failure(IllegalArgumentException("No such Sonata"))
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
