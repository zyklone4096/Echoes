package dev.zyklone.echoes.contents.echoes

import dev.zyklone.echoes.contents.items.EchoItems
import dev.zyklone.echoes.contents.sonata.Sonata
import dev.zyklone.echoes.utils.asRarityColor
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.NamespacedKey
import org.bukkit.entity.Entity
import org.bukkit.entity.EntityType
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

interface Echo {
    val key: NamespacedKey
    val drop: EchoDrop
    val minRarity: Int  // 0-4
    val cost: Int   // 1-4

    fun createRandom(player: Player, from: Entity? = null, sonata: Sonata? = null, rarity: Byte? = null): EchoData

    fun asItem(data: EchoData): ItemStack

    fun skill(player: Player, data: EchoData) {

    }

    fun displayName(rarity: Byte): Component
}

abstract class VanillaMobEcho(val type: EntityType): Echo, AutoItem {
    override val key: NamespacedKey = type.key
    override val drop: EchoDrop = EchoDrop.Vanilla(type)

    override fun displayName(rarity: Byte): Component =
        Component.translatable(this.type.translationKey(), rarity.asRarityColor())
}

interface AutoDisplayName: Echo {
    val name: String

    override fun displayName(rarity: Byte): Component =
        Component.text(name, when (rarity) {
            0.toByte() -> NamedTextColor.WHITE
            1.toByte() -> NamedTextColor.GREEN
            2.toByte() -> NamedTextColor.BLUE
            3.toByte() -> NamedTextColor.LIGHT_PURPLE
            else -> NamedTextColor.GOLD
        }).decoration(TextDecoration.ITALIC, false)
}

interface AutoItem: Echo {
    override fun asItem(data: EchoData): ItemStack =
        EchoItems.defaultEchoItem(this, data)
}

sealed interface EchoDrop {
    class Vanilla(val type: EntityType): EchoDrop
}
