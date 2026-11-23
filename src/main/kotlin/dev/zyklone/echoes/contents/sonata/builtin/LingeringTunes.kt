package dev.zyklone.echoes.contents.sonata.builtin

import dev.zyklone.echoes.contents.echoes.EchoData
import dev.zyklone.echoes.contents.sonata.Sonata
import dev.zyklone.echoes.contents.sonata.SonataRegistry
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.NamespacedKey

class LingeringTunes: Sonata {
    override val key: NamespacedKey = SonataRegistry.BUILTIN_LINGERING_TUNES
    override val rawName: String = "不绝余音"

    override fun displayName(): Component =
        Component.text(rawName, NamedTextColor.RED)

    override fun check(data: List<EchoData?>): Boolean =
        data.count { it?.sonataEffect == key } >= 2
}
