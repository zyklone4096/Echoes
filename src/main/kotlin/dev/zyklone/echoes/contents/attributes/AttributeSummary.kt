package dev.zyklone.echoes.contents.attributes

import org.bukkit.NamespacedKey

class AttributeSummary {
    private val doubles: MutableMap<NamespacedKey, Double> = HashMap()

    fun add(key: NamespacedKey, value: Double) {
        doubles[key] = (doubles[key] ?: 0.0) + value
    }

    fun getDouble(key: NamespacedKey): Double? =
        this.doubles[key]
}
