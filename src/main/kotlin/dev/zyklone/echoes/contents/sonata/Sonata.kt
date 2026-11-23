package dev.zyklone.echoes.contents.sonata

import dev.zyklone.echoes.contents.echoes.EchoData
import dev.zyklone.echoes.utils.styleless
import net.kyori.adventure.text.Component
import org.bukkit.NamespacedKey
import org.bukkit.entity.Player
import org.bukkit.event.entity.EntityDamageByEntityEvent

interface Sonata {
    val key: NamespacedKey

    val rawName: String
    fun displayName(): Component

    fun check(data: List<EchoData?>): Boolean

    interface Applied: Sonata {
        fun apply(player: Player, data: List<EchoData?>)
        fun clear(player: Player, data: List<EchoData?>)
    }

    interface Combat: Sonata {
        fun accept(event: EntityDamageByEntityEvent, damager: Player, data: List<EchoData?>)
    }

    interface AutoDisplayName: Sonata {
        override fun displayName(): Component =
            this.rawName.styleless()
    }
}
