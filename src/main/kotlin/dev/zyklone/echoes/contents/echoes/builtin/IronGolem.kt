package dev.zyklone.echoes.contents.echoes.builtin

import dev.zyklone.echoes.contents.attributes.EchoAttributes
import dev.zyklone.echoes.contents.echoes.EchoData
import dev.zyklone.echoes.contents.echoes.VanillaMobEcho
import dev.zyklone.echoes.contents.sonata.Sonata
import dev.zyklone.echoes.contents.sonata.SonataRegistry
import org.bukkit.entity.Entity
import org.bukkit.entity.EntityType
import org.bukkit.entity.Player
import org.bukkit.util.Vector

class IronGolem: VanillaMobEcho(EntityType.IRON_GOLEM) {
    override val minRarity: Int = 2
    override val cost: Int = 3

    private val effects = listOf(
        SonataRegistry.BUILTIN_LINGERING_TUNES,
        SonataRegistry.BUILTIN_MOONLIT_CLOUDS
    )

    override fun createRandom(
        player: Player,
        from: Entity?,
        sonata: Sonata?,
        rarity: Byte?
    ): EchoData = EchoData(
        this.key,
        rarity = rarity ?: this.minRarity.toByte(),
        attributes = EchoAttributes.randomPrimary(player, this),
        sonataEffect = sonata?.key ?: this.effects.random(),
    )

    override fun skill(player: Player, data: EchoData) {
        val target = player.getTargetEntity(5) ?: return
        target.velocity = Vector(0.0, 1.0, 0.0)
    }
}