package dev.zyklone.echoes.contents.attributes

import dev.zyklone.echoes.Echoes
import io.papermc.paper.persistence.PersistentDataContainerView
import io.papermc.paper.persistence.PersistentDataViewHolder
import org.bukkit.NamespacedKey
import org.bukkit.persistence.PersistentDataContainer
import org.bukkit.persistence.PersistentDataHolder
import org.bukkit.persistence.PersistentDataType

private val parent = NamespacedKey(Echoes.NAMESPACE, "attributes")

open class ReadAttributes internal constructor(private val view: PersistentDataContainerView) {
    operator fun <T: Any> get(attr: EchoesAttribute<*>, type: PersistentDataType<out Any, T>): T? =
        if (this.view.has(attr.key, type))
            this.view.get(attr.key, type)
        else null
}

internal class WriteAttributes(internal val pdc: PersistentDataContainer): ReadAttributes(pdc) {
    operator fun <T: Any> set(attr: EchoesAttribute<*>, type: PersistentDataType<out Any, T>, value: T) {
        this.pdc[attr.key, type] = value
    }
}

val PersistentDataViewHolder.attributes: ReadAttributes?
    get() = this.persistentDataContainer[parent, PersistentDataType.TAG_CONTAINER]?.let(::ReadAttributes)

internal var PersistentDataHolder.attributes: WriteAttributes?
    get() = this.persistentDataContainer[parent, PersistentDataType.TAG_CONTAINER]?.let(::WriteAttributes)
    set(value) {
        if (value == null)
            this.persistentDataContainer.remove(parent)
        else this.persistentDataContainer[parent, PersistentDataType.TAG_CONTAINER] = value.pdc
    }
