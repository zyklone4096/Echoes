package dev.zyklone.echoes.config

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.bukkit.Material
import org.bukkit.entity.EntityType
import org.bukkit.inventory.ItemStack

@Serializable
sealed interface Item {
    @Serializable
    @SerialName("Vanilla")
    class Vanilla(val material: Material): Item {
        override fun toItemStack(): ItemStack =
            ItemStack(material)

        override fun match(item: ItemStack): Boolean =
            item.type == this.material
    }

    @Serializable
    @SerialName("BuiltinDefault")
    object BuiltinDefault: Item {
        override fun toItemStack(): ItemStack =
            throw UnsupportedOperationException()

        override fun match(item: ItemStack): Boolean =
            throw UnsupportedOperationException()
    }

    fun toItemStack(): ItemStack
    fun match(item: ItemStack): Boolean
}

@Serializable
sealed interface Entity {
    class Vanilla(val type: EntityType): Entity
}
