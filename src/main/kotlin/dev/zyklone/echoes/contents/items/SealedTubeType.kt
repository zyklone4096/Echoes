package dev.zyklone.echoes.contents.items

import dev.zyklone.echoes.Echoes
import dev.zyklone.echoes.config.Item
import dev.zyklone.echoes.utils.literal
import net.kyori.adventure.text.Component
import org.bukkit.inventory.ItemRarity
import org.bukkit.persistence.PersistentDataAdapterContext
import org.bukkit.persistence.PersistentDataType

enum class SealedTubeType(val displayName: Component, val rarity: ItemRarity, val exp: Int, val provider: () -> Item) {
    BASIC("初级密音筒".literal(), ItemRarity.COMMON, 500, { Echoes.INST.config.items.basicSealedTube }),
    MEDIUM("中级密音筒".literal(), ItemRarity.UNCOMMON, 1000, { Echoes.INST.config.items.mediumSealedTube }),
    ADVANCED("高级密音筒".literal(), ItemRarity.RARE, 2000, { Echoes.INST.config.items.advancedSealedTube }),
    PREMIUM("特级密音筒".literal(), ItemRarity.EPIC, 5000, { Echoes.INST.config.items.premiumSealedTube }),
}

object SealedTubePDT: PersistentDataType<Byte, SealedTubeType> {
    override fun getPrimitiveType(): Class<Byte> =
        Byte::class.javaObjectType

    override fun getComplexType(): Class<SealedTubeType> =
        SealedTubeType::class.java

    override fun toPrimitive(
        complex: SealedTubeType,
        context: PersistentDataAdapterContext
    ): Byte =
        complex.ordinal.toByte()

    override fun fromPrimitive(
        primitive: Byte,
        context: PersistentDataAdapterContext
    ): SealedTubeType =
        SealedTubeType.entries[primitive.toInt()]
}
