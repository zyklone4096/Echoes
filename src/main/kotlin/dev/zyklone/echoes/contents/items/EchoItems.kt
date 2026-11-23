package dev.zyklone.echoes.contents.items

import dev.zyklone.echoes.Echoes
import dev.zyklone.echoes.config.Items
import dev.zyklone.echoes.contents.attributes.EchoAttributes
import dev.zyklone.echoes.contents.echoes.Echo
import dev.zyklone.echoes.contents.echoes.EchoData
import dev.zyklone.echoes.contents.echoes.echoData
import dev.zyklone.echoes.contents.sonata.SonataRegistry
import dev.zyklone.echoes.utils.styleless
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.inventory.ItemStack

object EchoItems {
    private var config: Items = Items()
    private var defaultEchoItem: ItemStack = config.defaultEchoItem.toItemStack()

    val tick = " - ".styleless()

    fun init() {
        this.config = Echoes.INST.config.items
        this.defaultEchoItem = this.config.defaultEchoItem.toItemStack()
    }

    val emptyAttribute = Component.text()
        .styleless()
        .append(tick)
        .append(Component.text("（空）", NamedTextColor.GRAY))
        .build()
    private val costHeader = Component.text()
        .styleless()
        .append(Component.text("COST：", NamedTextColor.GOLD))
        .build()
    private val sonataHeader = Component.text()
        .styleless()
        .append(Component.text("合鸣效果：", NamedTextColor.GOLD))
        .build()

    fun defaultEchoItem(echo: Echo, data: EchoData): ItemStack {
        val it = this.defaultEchoItem.clone()
        val meta = it.itemMeta

        // write data
        val pdc = meta.persistentDataContainer
        pdc.echoData = data

        // displays
        meta.displayName(echo.displayName(data.rarity))
        val lore = if (meta.hasLore()) meta.lore() ?: arrayListOf() else arrayListOf()
        lore.add(costHeader.toBuilder()
            .append(Component.text(echo.cost))
            .build())
        lore.add(sonataHeader.toBuilder()
            .append(requireNotNull(SonataRegistry[data.sonataEffect]) { "Unknown sonata effect ${data.sonataEffect}" }.displayName())
            .build())
        lore.add(Component.empty())
        for (pair in data.attributes) {
            if (pair == null) {
                lore.add(emptyAttribute)
            } else {
                val (key, value) = pair
                lore.add(Component.text()
                    .styleless()
                    .append(tick)
                    .append(requireNotNull(EchoAttributes[key]) { "Unknown attribute $key" }.renderWide(value))
                    .build())
            }
        }
        meta.lore(lore)

        it.itemMeta = meta
        return it
    }
}
