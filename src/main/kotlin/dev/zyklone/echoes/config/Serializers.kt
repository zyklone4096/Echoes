@file:UseSerializers(
    NamespacedKeySerializer::class,
    AttributeSerializer::class,
    CEKeySerializer::class,
)

package dev.zyklone.echoes.config

import kotlinx.serialization.KSerializer
import kotlinx.serialization.UseSerializers
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import net.momirealms.craftengine.core.util.Key
import org.bukkit.NamespacedKey
import org.bukkit.Registry
import org.bukkit.attribute.Attribute

object NamespacedKeySerializer: KSerializer<NamespacedKey> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("org.bukkit.NamespacedKey")

    override fun serialize(encoder: Encoder, value: NamespacedKey) =
        encoder.encodeString(value.toString())

    override fun deserialize(decoder: Decoder): NamespacedKey {
        val key = decoder.decodeString()
        return requireNotNull(NamespacedKey.fromString(key)) { "Invalid key $key" }
    }
}

object AttributeSerializer: KSerializer<Attribute> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("org.bukkit.attribute.Attribute")

    override fun serialize(encoder: Encoder, value: Attribute) =
        encoder.encodeString(value.key.toString())

    override fun deserialize(decoder: Decoder): Attribute {
        val key = decoder.decodeString()
        return requireNotNull(NamespacedKey.fromString(key)?.let(Registry.ATTRIBUTE::get)) {
            "Unknown attribute $key"
        }
    }
}

object CEKeySerializer: KSerializer<Key> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("net.momirealms.craftengine.core.util.Key")

    override fun serialize(
        encoder: Encoder,
        value: Key
    ) = encoder.encodeString(value.asString())

    override fun deserialize(decoder: Decoder): Key =
        Key.of(decoder.decodeString())
}
