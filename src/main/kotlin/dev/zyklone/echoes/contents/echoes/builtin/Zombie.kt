package dev.zyklone.echoes.contents.echoes.builtin

import dev.zyklone.echoes.contents.attributes.EchoAttributes
import dev.zyklone.echoes.contents.echoes.AutoItem
import dev.zyklone.echoes.contents.echoes.Echo
import dev.zyklone.echoes.contents.echoes.EchoData
import dev.zyklone.echoes.contents.echoes.EchoDrop
import dev.zyklone.echoes.contents.sonata.Sonata
import dev.zyklone.echoes.contents.sonata.SonataRegistry
import dev.zyklone.echoes.utils.asRarityColor
import net.kyori.adventure.text.Component
import org.bukkit.NamespacedKey
import org.bukkit.entity.Entity
import org.bukkit.entity.EntityType
import org.bukkit.entity.Player

class Zombie: Echo, AutoItem {
    override val key: NamespacedKey = EntityType.ZOMBIE.key
    override val drop: EchoDrop = EchoDrop.Vanilla(EntityType.ZOMBIE)
    override val minRarity: Int = 0
    override val cost: Int = 1

    override fun createRandom(
        player: Player,
        from: Entity?,
        sonata: Sonata?,
        rarity: Byte?
    ): EchoData =
        EchoData(
            this.key,
            rarity = rarity ?: 0,
            attributes = EchoAttributes.randomPrimary(player, this),
            sonataEffect = sonata?.key ?: SonataRegistry.BUILTIN_LINGERING_TUNES
        )

    override fun displayName(rarity: Byte): Component =
        Component.translatable(EntityType.ZOMBIE.translationKey(), rarity.asRarityColor())
}
