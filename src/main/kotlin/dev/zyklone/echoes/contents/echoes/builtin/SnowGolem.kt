package dev.zyklone.echoes.contents.echoes.builtin

import dev.zyklone.echoes.contents.attributes.EchoAttributes
import dev.zyklone.echoes.contents.echoes.EchoData
import dev.zyklone.echoes.contents.echoes.VanillaMobEcho
import dev.zyklone.echoes.contents.sonata.Sonata
import dev.zyklone.echoes.contents.sonata.SonataRegistry
import org.bukkit.entity.Entity
import org.bukkit.entity.EntityType
import org.bukkit.entity.Player
import org.bukkit.entity.Snowball

class SnowGolem: VanillaMobEcho(EntityType.SNOW_GOLEM) {
    override val minRarity: Int = 1
    override val cost: Int = 3

    override fun createRandom(
        player: Player,
        from: Entity?,
        sonata: Sonata?,
        rarity: Byte?
    ): EchoData = EchoData(
        this.key,
        rarity = rarity ?: this.minRarity.toByte(),
        attributes = EchoAttributes.randomPrimary(player, this),
        sonataEffect = sonata?.key ?: SonataRegistry.BUILTIN_FREEZING_FROST,
    )

    override fun skill(player: Player, data: EchoData) {
        val eyeLocation = player.eyeLocation
        val direction = eyeLocation.direction
        val spawnLocation = eyeLocation.add(direction.clone().multiply(0.5))
        player.world.spawn(spawnLocation, Snowball::class.java) { snowball ->
            snowball.shooter = player
            snowball.velocity = direction
        }
    }
}