package dev.zyklone.echoes.contents.sonata.builtin

import dev.zyklone.echoes.contents.echoes.EchoData
import dev.zyklone.echoes.contents.sonata.Sonata
import dev.zyklone.echoes.contents.sonata.SonataRegistry
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.NamespacedKey

class HavocEclipse: Sonata {
    override val key: NamespacedKey = SonataRegistry.BUILTIN_HAVOC_ECLIPSE
    override val rawName: String = "沉日劫明"

    override fun displayName(): Component = Component.text(this.rawName, NamedTextColor.LIGHT_PURPLE)

    override fun check(data: List<EchoData?>): Boolean =
        data.filter { it?.sonataEffect == this.key }.distinctBy { it?.key }.size >= 2
}
