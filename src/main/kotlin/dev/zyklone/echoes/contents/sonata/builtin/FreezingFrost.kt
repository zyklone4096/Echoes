package dev.zyklone.echoes.contents.sonata.builtin

import dev.zyklone.echoes.contents.echoes.EchoData
import dev.zyklone.echoes.contents.sonata.Sonata
import dev.zyklone.echoes.contents.sonata.SonataRegistry
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.NamespacedKey

class FreezingFrost: Sonata {
    override val key: NamespacedKey = SonataRegistry.BUILTIN_FREEZING_FROST
    override val rawName: String = "凝夜白霜"

    override fun displayName(): Component =
        Component.text(rawName, NamedTextColor.AQUA)

    override fun check(data: List<EchoData?>): Boolean =
        data.count { it?.sonataEffect == this.key } >= 2
}