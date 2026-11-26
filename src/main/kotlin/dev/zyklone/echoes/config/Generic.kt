package dev.zyklone.echoes.config

import io.th0rgal.oraxen.api.OraxenItems
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.momirealms.craftengine.core.util.Key
import org.bukkit.Material
import org.bukkit.entity.EntityType
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import pers.neige.neigeitems.manager.ItemManager
import kotlin.jvm.optionals.getOrNull

@Serializable
sealed interface Item {
    val modifyDisplay: Boolean
    val modifyIdentifier: Boolean

    @Serializable
    @SerialName("Vanilla")
    class Vanilla(
        val material: Material,
        override val modifyDisplay: Boolean = true,
        override val modifyIdentifier: Boolean = true,
    ): Item {
        override fun toItemStack(player: Player?): ItemStack =
            ItemStack(material)

        override fun match(item: ItemStack): Boolean =
            item.type == this.material
    }

    @Serializable
    @SerialName("BuiltinDefault")
    object BuiltinDefault: Item {
        override val modifyDisplay: Boolean = true
        override val modifyIdentifier: Boolean = true

        override fun toItemStack(player: Player?): ItemStack =
            throw UnsupportedOperationException()

        override fun match(item: ItemStack): Boolean =
            throw UnsupportedOperationException()
    }

    @Serializable
    @SerialName("Neige")
    data class Neige(
        val id: String,
        override val modifyDisplay: Boolean = false,
        override val modifyIdentifier: Boolean = true,
    ): Item {
        override fun toItemStack(player: Player?): ItemStack =
            requireNotNull(ItemManager.getItemStack(this.id, player)) { "Unable to create item $id" }

        override fun match(item: ItemStack): Boolean =
            ItemManager.getItemId(item) == this.id
    }

    @Serializable
    @SerialName("Oraxen")
    data class Oraxen(
        val id: String,
        override val modifyDisplay: Boolean = false,
        override val modifyIdentifier: Boolean = true,
    ): Item {
        override fun toItemStack(player: Player?): ItemStack =
            requireNotNull(OraxenItems.getItemById(this.id)) { "Unable to create item $id" }
                .build()

        override fun match(item: ItemStack): Boolean =
            OraxenItems.getIdByItem(item) == this.id
    }

    @Serializable
    @SerialName("CraftEngine")
    data class CraftEngine(
        @Serializable(CEKeySerializer::class) val key: Key,
        override val modifyDisplay: Boolean = false,
        override val modifyIdentifier: Boolean = true,
    ): Item {
        override fun toItemStack(player: Player?): ItemStack {
            val ce = net.momirealms.craftengine.core.plugin.CraftEngine.instance()
            val platform = ce.platform()
            return ce.itemManager()
                .getBuildableItem(this.key)
                .getOrNull()
                ?.buildItem(player?.let { platform.getPlayer(it.uniqueId) })
                ?.platformItem() as ItemStack
        }

        // 乡巴佬用不起这玩意，没有进一步 支持/测试
        // 在多数情况下仍可通过内置物品识别处理
        override fun match(item: ItemStack): Boolean =
            throw UnsupportedOperationException("Not supported for CraftEngine, use another item provider type")
    }

    fun toItemStack(player: Player?): ItemStack
    fun match(item: ItemStack): Boolean
}

@Serializable
sealed interface Entity {
    class Vanilla(val type: EntityType): Entity
}
