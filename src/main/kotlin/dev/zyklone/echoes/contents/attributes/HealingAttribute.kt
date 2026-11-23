package dev.zyklone.echoes.contents.attributes

import dev.zyklone.echoes.Echoes
import dev.zyklone.echoes.contents.echoes.Echo
import dev.zyklone.echoes.utils.formatPercent
import dev.zyklone.echoes.utils.styleless
import net.kyori.adventure.text.Component
import org.bukkit.NamespacedKey
import org.bukkit.entity.Player
import java.util.WeakHashMap
import java.util.concurrent.ThreadLocalRandom
import kotlin.math.roundToInt

class HealingAttribute: EchoesAttribute.DoubleAttribute(), EchoesAttribute.AppliedAttribute, EchoesAttribute.AutoRender<Double> {
    override val key: NamespacedKey = NamespacedKey(Echoes.NAMESPACE, "healing")

    private val originalRegenRates = WeakHashMap<Player, RegenRates>()

    override fun apply(player: Player, summary: AttributeSummary) {
        val bonus = summary.getDouble(this.key) ?: return
        require(bonus.isFinite() && bonus > -1.0) {
            "Healing bonus must be finite and greater than -1.0"
        }

        val original = this.originalRegenRates.getOrPut(player) {
            RegenRates(player.saturatedRegenRate, player.unsaturatedRegenRate)
        }

        // A bonus of 0.5 increases regeneration frequency by 50%.
        player.saturatedRegenRate = (original.saturated / (1.0 + bonus)).roundToInt().coerceAtLeast(1)
        player.unsaturatedRegenRate = (original.unsaturated / (1.0 + bonus)).roundToInt().coerceAtLeast(1)
    }

    override fun clear(player: Player, summary: AttributeSummary) {
        val original = this.originalRegenRates.remove(player) ?: return
        player.saturatedRegenRate = original.saturated
        player.unsaturatedRegenRate = original.unsaturated
    }

    override val header: Component = "治疗效果加成：".styleless()

    override fun format(value: Double): String =
        value.formatPercent()

    private data class RegenRates(val saturated: Int, val unsaturated: Int)
}
