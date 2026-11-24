package dev.zyklone.echoes.contents.items

import dev.zyklone.echoes.Echoes
import dev.zyklone.echoes.config.Item
import dev.zyklone.echoes.utils.literal
import net.kyori.adventure.text.Component
import org.bukkit.inventory.ItemRarity
import org.bukkit.persistence.PersistentDataAdapterContext
import org.bukkit.persistence.PersistentDataType

enum class TunerType(val displayName: Component, val rarity: ItemRarity, val provider: () -> Item) {
    BASIC("初级调谐器".literal(), ItemRarity.COMMON, { Echoes.INST.config.items.basicTuner }),
    MEDIUM("中级调谐器".literal(), ItemRarity.UNCOMMON, { Echoes.INST.config.items.mediumTuner }),
    ADVANCED("高级调谐器".literal(), ItemRarity.RARE, { Echoes.INST.config.items.advancedTuner }),
    PREMIUM("特级调谐器".literal(), ItemRarity.EPIC, { Echoes.INST.config.items.premiumTuner }),
}

object TunerPDT: PersistentDataType<Byte, TunerType> {
    override fun getPrimitiveType(): Class<Byte> =
        Byte::class.javaObjectType

    override fun getComplexType(): Class<TunerType> =
        TunerType::class.java

    override fun toPrimitive(
        complex: TunerType,
        context: PersistentDataAdapterContext
    ): Byte =
        complex.ordinal.toByte()

    override fun fromPrimitive(
        primitive: Byte,
        context: PersistentDataAdapterContext
    ): TunerType =
        TunerType.entries[primitive.toInt()]
}
