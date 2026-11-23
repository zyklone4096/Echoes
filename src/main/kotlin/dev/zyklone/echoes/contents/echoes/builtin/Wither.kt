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
import org.bukkit.entity.WitherSkull

class Wither: Echo, AutoItem {
    override val key: NamespacedKey = EntityType.WITHER.key
    override val drop: EchoDrop = EchoDrop.Vanilla(EntityType.WITHER)
    override val minRarity: Int = 2
    override val cost: Int = 4

    private val effects = listOf(
        SonataRegistry.BUILTIN_LINGERING_TUNES,
        SonataRegistry.BUILTIN_HAVOC_ECLIPSE
    )

    override fun createRandom(
        player: Player,
        from: Entity?,
        sonata: Sonata?,
        rarity: Byte?
    ): EchoData = EchoData(
        this.key,
        rarity = rarity ?: 2,
        attributes = EchoAttributes.randomPrimary(player, this),
        sonataEffect = sonata?.key ?: effects.random(),
    )

    override fun displayName(rarity: Byte): Component =
        Component.translatable(EntityType.WITHER.translationKey(), rarity.asRarityColor())

    override fun skill(player: Player, data: EchoData) {
        val eyeLocation = player.eyeLocation
        val direction = eyeLocation.direction
        val spawnLocation = eyeLocation.add(direction.clone().multiply(0.5))
        player.world.spawn(spawnLocation, WitherSkull::class.java) { skull ->
            skull.shooter = player
            skull.velocity = direction
            skull.direction = direction
        }
    }
}
