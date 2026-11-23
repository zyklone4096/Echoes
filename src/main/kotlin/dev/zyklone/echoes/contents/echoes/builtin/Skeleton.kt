package dev.zyklone.echoes.contents.echoes.builtin

import dev.zyklone.echoes.contents.attributes.EchoAttributes
import dev.zyklone.echoes.contents.echoes.EchoData
import dev.zyklone.echoes.contents.echoes.VanillaMobEcho
import dev.zyklone.echoes.contents.sonata.Sonata
import dev.zyklone.echoes.contents.sonata.SonataRegistry
import org.bukkit.entity.Arrow
import org.bukkit.entity.Entity
import org.bukkit.entity.EntityType
import org.bukkit.entity.Player
import org.bukkit.entity.WitherSkull

class Skeleton: VanillaMobEcho(EntityType.SKELETON) {
    override val minRarity: Int = 0
    override val cost: Int = 1

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

    override fun skill(player: Player, data: EchoData) {
        val eyeLocation = player.eyeLocation
        val direction = eyeLocation.direction
        val spawnLocation = eyeLocation.add(direction.clone().multiply(0.5))
        player.world.spawn(spawnLocation, Arrow::class.java) { arrow ->
            arrow.shooter = player
            arrow.velocity = direction
        }
    }
}
