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
    }

    fun toItemStack(): ItemStack
}

@Serializable
sealed interface Entity {
    class Vanilla(val type: EntityType): Entity
}
