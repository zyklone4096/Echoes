package dev.zyklone.echoes.contents.attributes

import dev.zyklone.echoes.Echoes
import dev.zyklone.echoes.utils.formatPercent
import dev.zyklone.echoes.utils.getOrRegisterAttribute
import dev.zyklone.echoes.utils.scaleToString
import dev.zyklone.echoes.utils.styleless
import net.kyori.adventure.text.Component
import org.bukkit.NamespacedKey
import org.bukkit.attribute.Attribute
import org.bukkit.attribute.AttributeModifier
import org.bukkit.entity.Player

class HealthAttribute: EchoesAttribute.DoubleAttribute(0.064, 0.116), EchoesAttribute.AppliedAttribute, EchoesAttribute.AutoRender<Double> {
    override val key: NamespacedKey =
        NamespacedKey(Echoes.NAMESPACE, "health")

    override fun apply(player: Player, summary: AttributeSummary) {
        summary.getDouble(this.key)?.let {
            player.getOrRegisterAttribute(Attribute.MAX_HEALTH)
                .addTransientModifier(AttributeModifier(this.key, it, AttributeModifier.Operation.ADD_SCALAR))
        }
    }

    override fun clear(player: Player, summary: AttributeSummary) {
        player.getOrRegisterAttribute(Attribute.MAX_HEALTH)
            .removeModifier(this.key)
    }

    override val header: Component = "生命值：".styleless()

    override fun format(value: Double): String =
        value.formatPercent()
}

class HealthValueAttribute: EchoesAttribute.DoubleAttribute(3.2, 5.8), EchoesAttribute.AppliedAttribute, EchoesAttribute.AutoRender<Double> {
    override val key: NamespacedKey = NamespacedKey(Echoes.NAMESPACE, "health_value")

    override fun apply(
        player: Player,
        summary: AttributeSummary
    ) {
        summary.getDouble(this.key)?.let {
            player.getOrRegisterAttribute(Attribute.MAX_HEALTH)
                .addTransientModifier(AttributeModifier(this.key, it, AttributeModifier.Operation.ADD_NUMBER))
        }
    }

    override fun clear(
        player: Player,
        summary: AttributeSummary
    ) {
        player.getOrRegisterAttribute(Attribute.MAX_HEALTH)
            .removeModifier(this.key)
    }

    override val header: Component = "生命值：".styleless()

    override fun format(value: Double): String =
        value.scaleToString()
}
