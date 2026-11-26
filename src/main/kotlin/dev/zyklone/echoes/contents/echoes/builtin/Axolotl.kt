package dev.zyklone.echoes.contents.echoes.builtin

import dev.zyklone.echoes.Echoes
import dev.zyklone.echoes.contents.attributes.EchoAttributes
import dev.zyklone.echoes.contents.echoes.EchoData
import dev.zyklone.echoes.contents.echoes.VanillaMobEcho
import dev.zyklone.echoes.contents.sonata.Sonata
import dev.zyklone.echoes.contents.sonata.SonataRegistry
import dev.zyklone.echoes.utils.getOrRegisterAttribute
import org.bukkit.attribute.Attribute
import org.bukkit.attribute.AttributeModifier
import org.bukkit.entity.Entity
import org.bukkit.entity.EntityType
import org.bukkit.entity.Player
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType

class Axolotl: VanillaMobEcho(EntityType.AXOLOTL) {
    override val minRarity: Int = 2
    override val cost: Int = 3

    private val effects = arrayOf(
        SonataRegistry.BUILTIN_CELESTIAL_LIGHT,
        SonataRegistry.BUILTIN_REJUVENATING_GLOW,
    )

    override fun createRandom(
        player: Player,
        from: Entity?,
        sonata: Sonata?,
        rarity: Byte?
    ): EchoData = EchoData(
        this.key,
        sonataEffect = sonata?.key ?: this.effects.random(),
        rarity = rarity ?: this.minRarity.toByte(),
        attributes = EchoAttributes.randomPrimary(player, this)
    )

    private val stopMove = AttributeModifier(
        this.key,
        -1000.0,
        AttributeModifier.Operation.ADD_NUMBER,
    )
    private val regen = PotionEffect(
        PotionEffectType.REGENERATION,
        200,
        1,
        false,
        false,
        true,
    )

    override fun skill(player: Player, data: EchoData) {
        player.getOrRegisterAttribute(Attribute.MOVEMENT_SPEED)
            .addTransientModifier(this.stopMove) // disable movement
        player.addPotionEffect(regen)

        player.scheduler.runDelayed(Echoes.INST, {
            player.getOrRegisterAttribute(Attribute.MOVEMENT_SPEED)
                .removeModifier(this.key)
        }, null, 100)
    }
}