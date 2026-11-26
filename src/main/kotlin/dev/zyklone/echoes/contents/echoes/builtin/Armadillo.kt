package dev.zyklone.echoes.contents.echoes.builtin

import dev.zyklone.echoes.contents.attributes.EchoAttributes
import dev.zyklone.echoes.contents.echoes.EchoData
import dev.zyklone.echoes.contents.echoes.VanillaMobEcho
import dev.zyklone.echoes.contents.sonata.Sonata
import dev.zyklone.echoes.contents.sonata.SonataRegistry
import dev.zyklone.echoes.listeners.SkillListener
import org.bukkit.entity.Entity
import org.bukkit.entity.EntityType
import org.bukkit.entity.Player

class Armadillo: VanillaMobEcho(EntityType.ARMADILLO) {
    override val minRarity: Int = 1
    override val cost: Int = 3

    private val effects = arrayOf(
        SonataRegistry.BUILTIN_MOONLIT_CLOUDS,
        SonataRegistry.BUILTIN_LINGERING_TUNES,
    )

    override fun createRandom(
        player: Player,
        from: Entity?,
        sonata: Sonata?,
        rarity: Byte?
    ): EchoData = EchoData(
        this.key,
        rarity = rarity ?: this.minRarity.toByte(),
        sonataEffect = sonata?.key ?: this.effects.random(),
        attributes = EchoAttributes.randomPrimary(player, this)
    )

    override fun skill(player: Player, data: EchoData) {
        SkillListener.armadillo += player.uniqueId
    }
}