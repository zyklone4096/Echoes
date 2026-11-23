package dev.zyklone.echoes.contents.attributes

import dev.zyklone.echoes.contents.echoes.Echo
import io.papermc.paper.persistence.PersistentDataContainerView
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.TextComponent
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.NamespacedKey
import org.bukkit.entity.Player
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.persistence.PersistentDataContainer
import org.bukkit.persistence.PersistentDataType
import java.util.concurrent.ThreadLocalRandom

sealed interface EchoesAttribute<T> {
    val key: NamespacedKey

    fun load(view: ReadAttributes, summary: AttributeSummary)
    
    fun getValueWide(view: PersistentDataContainerView): Any
    fun setValueWide(pdc: PersistentDataContainer, value: Any)

    @Suppress("UNCHECKED_CAST")
    fun renderWide(value: Any) = this.render(value as T)
    fun render(value: T) = Component.empty()

    fun random(player: Player, echo: Echo): T

    abstract class DoubleAttribute: EchoesAttribute<Double> {
        override fun load(view: ReadAttributes, summary: AttributeSummary) {
            view[this, PersistentDataType.DOUBLE]?.let { summary.add(this.key, it) }
        }

        override fun getValueWide(view: PersistentDataContainerView) =
            requireNotNull(view[this.key, PersistentDataType.DOUBLE])

        override fun setValueWide(pdc: PersistentDataContainer, value: Any) {
            pdc[this.key, PersistentDataType.DOUBLE] = value as Double
        }

        override fun random(player: Player, echo: Echo): Double = 0.0
    }

    interface AutoRender<T>: EchoesAttribute<T> {
        val header: Component

        override fun render(value: T): TextComponent =
            Component.text()
                .append(this.header)
                .append(Component.text(this.format(value), NamedTextColor.GREEN))
                .build()

        fun format(value: T): String
    }

    interface AppliedAttribute {
        fun apply(player: Player, summary: AttributeSummary)

        fun clear(player: Player, summary: AttributeSummary)
    }

    interface CombatAttribute {
        fun accept(event: EntityDamageByEntityEvent)
    }
}
