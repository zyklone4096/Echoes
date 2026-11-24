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

    @Suppress("UNCHECKED_CAST")
    fun nextWide(value: Any, maxLevel: Int): Any =
        this.next(value as T, maxLevel)!!

    /** 返回提升一级后的主属性值，由属性实现决定确定性的成长规则。 */
    fun next(value: T, maxLevel: Int): T = value

    abstract class DoubleAttribute(
        val minBaseValue: Double = 0.0,
        val maxBaseValue: Double = minBaseValue,
    ): EchoesAttribute<Double> {
        init {
            require(minBaseValue.isFinite() && maxBaseValue.isFinite() && minBaseValue <= maxBaseValue) {
                "Invalid attribute base value range"
            }
        }

        override fun load(view: ReadAttributes, summary: AttributeSummary) {
            view[this, PersistentDataType.DOUBLE]?.let { summary.add(this.key, it) }
        }

        override fun getValueWide(view: PersistentDataContainerView) =
            requireNotNull(view[this.key, PersistentDataType.DOUBLE])

        override fun setValueWide(pdc: PersistentDataContainer, value: Any) {
            pdc[this.key, PersistentDataType.DOUBLE] = value as Double
        }

        override fun random(player: Player, echo: Echo): Double =
            if (this.minBaseValue == this.maxBaseValue) this.minBaseValue
            else ThreadLocalRandom.current().nextDouble(this.minBaseValue, this.maxBaseValue)

        override fun next(value: Double, maxLevel: Int): Double {
            require(maxLevel > 0) { "Echo maximum level must be positive" }
            // 未提供成长区间的属性保留原值，兼容已有自定义属性。
            if (this.minBaseValue == this.maxBaseValue) return value
            val increment = (this.maxBaseValue - this.minBaseValue) / maxLevel
            return (value + increment).coerceIn(this.minBaseValue, this.maxBaseValue)
        }
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
