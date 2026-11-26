package dev.zyklone.echoes.contents.attributes

import dev.zyklone.echoes.config.AttributeAction
import dev.zyklone.echoes.config.CustomAttribute
import dev.zyklone.echoes.utils.getOrRegisterAttribute
import org.bukkit.NamespacedKey
import org.bukkit.attribute.AttributeModifier
import org.bukkit.entity.Player

internal class ConfiguredDoubleAttribute(private val config: CustomAttribute.Double): EchoesAttribute.DoubleAttribute(
    config.min,
    config.max
), EchoesAttribute.AppliedAttribute {
    override val key: NamespacedKey = config.key

    override fun apply(
        player: Player,
        summary: AttributeSummary
    ) {
        val value = summary.getDouble(this.key) ?: return

        for (action in config.actions) {
            if (action is AttributeAction.ModifyPlayerAttribute) {
                player.getOrRegisterAttribute(action.attribute)
                    .addTransientModifier(AttributeModifier(
                        this.key,
                        value,
                        action.operation
                    ))
            }
        }
    }

    override fun clear(
        player: Player,
        summary: AttributeSummary
    ) {
        for (action in this.config.actions) {
            if (action is AttributeAction.ModifyPlayerAttribute) {
                player.getOrRegisterAttribute(action.attribute)
                    .removeModifier(this.key)
            }
        }
    }
}
