package dev.zyklone.echoes.contents.sonata.builtin

import dev.zyklone.echoes.contents.echoes.EchoData
import dev.zyklone.echoes.contents.sonata.Sonata
import dev.zyklone.echoes.contents.sonata.SonataRegistry
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.NamespacedKey

class MoonlitClouds: Sonata {
    override val key: NamespacedKey = SonataRegistry.BUILTIN_MOONLIT_CLOUDS
    override val rawName: String = "轻云出月"

    override fun displayName(): Component =
        Component.text(rawName, NamedTextColor.GRAY)

    override fun check(data: List<EchoData?>): Boolean =
        data.filter { it?.sonataEffect == this.key }.distinctBy { it?.key }.size >= 2
}
