package dev.zyklone.echoes.contents.echoes.builtin

import dev.zyklone.echoes.contents.attributes.EchoAttributes
import dev.zyklone.echoes.contents.echoes.EchoData
import dev.zyklone.echoes.contents.echoes.VanillaMobEcho
import dev.zyklone.echoes.contents.sonata.Sonata
import dev.zyklone.echoes.contents.sonata.SonataRegistry
import org.bukkit.entity.Entity
import org.bukkit.entity.EntityType
import org.bukkit.entity.Player
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType

class Chicken: VanillaMobEcho(EntityType.CHICKEN) {
    override val minRarity: Int = 1
    override val cost: Int = 1

    override fun createRandom(
        player: Player,
        from: Entity?,
        sonata: Sonata?,
        rarity: Byte?
    ): EchoData = EchoData(
        this.key,
        sonataEffect = sonata?.key ?: SonataRegistry.BUILTIN_MOONLIT_CLOUDS,
        rarity = rarity ?: this.minRarity.toByte(),
        attributes = EchoAttributes.randomPrimary(player, this)
    )

    private val effect = PotionEffect(
        PotionEffectType.SLOW_FALLING,
        100,
        1,
        false, false, true
    )

    override fun skill(player: Player, data: EchoData) {
        player.addPotionEffect(effect)
    }
}