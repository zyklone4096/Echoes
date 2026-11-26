package dev.zyklone.echoes.config

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.bukkit.NamespacedKey
import org.bukkit.attribute.Attribute
import org.bukkit.attribute.AttributeModifier

@Serializable
internal sealed interface CustomAttribute {
    val key: NamespacedKey

    @Serializable
    @SerialName("")
    data class Double(
        @Serializable(NamespacedKeySerializer::class) override val key: NamespacedKey,
        val displayName: String,

        val max: kotlin.Double,
        val min: kotlin.Double,

        val actions: List<AttributeAction> = emptyList(),
    ): CustomAttribute
}

@Serializable
internal sealed interface AttributeAction {
    @Serializable
    data class ModifyPlayerAttribute(
        @Serializable(AttributeSerializer::class) val attribute: Attribute,
        val operation: AttributeModifier.Operation = AttributeModifier.Operation.ADD_NUMBER,
        ): AttributeAction
}
