package dev.zyklone.echoes.contents.attributes

import dev.zyklone.echoes.Echoes
import dev.zyklone.echoes.contents.echoes.Echo
import dev.zyklone.echoes.utils.formatPercent
import dev.zyklone.echoes.utils.getOrRegisterAttribute
import dev.zyklone.echoes.utils.scaleToString
import dev.zyklone.echoes.utils.styleless
import net.kyori.adventure.text.Component
import org.bukkit.NamespacedKey
import org.bukkit.attribute.Attribute
import org.bukkit.attribute.AttributeModifier
import org.bukkit.entity.Player
import java.util.concurrent.ThreadLocalRandom

class DamageAttribute: EchoesAttribute.DoubleAttribute(), EchoesAttribute.AppliedAttribute, EchoesAttribute.AutoRender<Double> {
    override val key: NamespacedKey = NamespacedKey(Echoes.NAMESPACE, "damage")

    override fun apply(player: Player, summary: AttributeSummary) {
        summary.getDouble(this.key)?.let {
            player.getOrRegisterAttribute(Attribute.ATTACK_DAMAGE)
                .addTransientModifier(AttributeModifier(this.key, it, AttributeModifier.Operation.ADD_SCALAR))
        }
    }

    override fun clear(player: Player, summary: AttributeSummary) {
        player.getOrRegisterAttribute(Attribute.ATTACK_DAMAGE)
            .removeModifier(this.key)
    }

    override val header: Component = "攻击力：".styleless()

    override fun format(value: Double): String =
        value.formatPercent()

    override fun random(player: Player, echo: Echo): Double =
        ThreadLocalRandom.current().nextDouble(0.064, 0.116)
}

class DamageValueAttribute: EchoesAttribute.DoubleAttribute(), EchoesAttribute.AppliedAttribute, EchoesAttribute.AutoRender<Double> {
    override val key: NamespacedKey = NamespacedKey(Echoes.NAMESPACE, "damage_value")

    override fun apply(player: Player, summary: AttributeSummary) {
        summary.getDouble(this.key)?.let {
            player.getOrRegisterAttribute(Attribute.ATTACK_DAMAGE)
                .addTransientModifier(AttributeModifier(this.key, it, AttributeModifier.Operation.ADD_NUMBER))
        }
    }

    override fun clear(player: Player, summary: AttributeSummary) {
        player.getOrRegisterAttribute(Attribute.ATTACK_DAMAGE)
            .removeModifier(this.key)
    }

    override val header: Component = "攻击力：".styleless()

    override fun format(value: Double): String =
        value.scaleToString()

    override fun random(player: Player, echo: Echo): Double =
        ThreadLocalRandom.current().nextDouble(1.0, 1.7)
}
