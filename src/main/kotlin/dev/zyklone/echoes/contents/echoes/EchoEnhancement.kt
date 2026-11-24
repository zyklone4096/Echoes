package dev.zyklone.echoes.contents.echoes

import dev.zyklone.echoes.contents.attributes.EchoAttributes
import dev.zyklone.echoes.contents.items.SealedTubeType
import org.bukkit.NamespacedKey

/** 先计算强化结果与材料消耗，再由 GUI 提交，避免计算失败时扣除材料。 */
internal object EchoEnhancement {
    const val TUNERS_PER_SLOT = 10

    data class UpgradeMaterial(val slot: Int, val type: SealedTubeType, val amount: Int)
    data class UpgradeResult(val data: EchoData, val consumed: Map<Int, Int>)
    data class TuneResult(val data: EchoData, val tunedSlots: Int) {
        val consumed: Int
            get() = this.tunedSlots * TUNERS_PER_SLOT
    }

    fun upgrade(
        data: EchoData,
        targetLevel: Int,
        levelIncrement: Int,
        materials: List<UpgradeMaterial>,
    ): UpgradeResult {
        require(levelIncrement > 0) { "Echo level experience increment must be positive" }
        val target = targetLevel.coerceAtMost(data.maxLevel).coerceAtLeast(data.level)
        if (target == data.level) return UpgradeResult(data, emptyMap())

        var neededExp = (data.level until target).sumOf { levelIncrement.toLong() * (it + 1) } - data.exp
        var remainingExp = data.exp.toLong()
        val consumed = linkedMapOf<Int, Int>()
        // 优先使用低经验密音筒，达到目标后不再消耗，单个道具的溢出经验保留。
        for (material in materials.sortedWith(compareBy({ it.type.exp }, { it.slot }))) {
            if (neededExp <= 0) break
            require(material.amount >= 0) { "Material amount must not be negative" }
            val count = minOf(material.amount.toLong(), (neededExp + material.type.exp - 1) / material.type.exp).toInt()
            if (count == 0) continue
            val gainedExp = count.toLong() * material.type.exp
            consumed[material.slot] = count
            remainingExp += gainedExp
            neededExp -= gainedExp
        }

        var level = data.level
        while (level < target) {
            val requiredExp = levelIncrement.toLong() * (level + 1)
            if (remainingExp < requiredExp) break
            remainingExp -= requiredExp
            level++
        }
        require(remainingExp <= Int.MAX_VALUE) { "Echo experience exceeds Int.MAX_VALUE" }

        val attributes = data.attributes.copyOf()
        if (level != data.level) {
            for (index in 0 until EchoData.MAIN_ATTRIBUTE_COUNT) {
                val (key, value) = attributes[index] ?: continue
                val attribute = requireNotNull(EchoAttributes[key]) { "Unknown attribute $key" }
                var nextValue = value
                repeat(level - data.level) {
                    nextValue = attribute.nextWide(nextValue, data.maxLevel)
                }
                attributes[index] = key to nextValue
            }
        }
        return UpgradeResult(
            EchoData(data.key, level, remainingExp.toInt(), attributes, data.rarity, data.sonataEffect),
            consumed,
        )
    }

    fun tune(
        data: EchoData,
        tunerCount: Int,
        random: (List<NamespacedKey>) -> Pair<NamespacedKey, Any>?,
    ): TuneResult {
        require(tunerCount >= 0) { "Tuner amount must not be negative" }
        if (data.rarity.toInt() == 0 || tunerCount < TUNERS_PER_SLOT) return TuneResult(data, 0)

        val attributes = data.attributes.copyOf()
        // 副属性不能彼此重复，但可以与主属性相同。
        val blacklist = attributes.drop(EchoData.MAIN_ATTRIBUTE_COUNT).mapNotNull { it?.first }.distinct().toMutableList()
        var tuned = 0
        for (index in EchoData.MAIN_ATTRIBUTE_COUNT until attributes.size) {
            if (attributes[index] != null) continue
            if (tuned >= tunerCount / TUNERS_PER_SLOT) break
            val attribute = random(blacklist.toList()) ?: break
            require(attribute.first !in blacklist) { "Duplicate sub attribute ${attribute.first}" }
            attributes[index] = attribute
            blacklist.add(attribute.first)
            tuned++
        }
        if (tuned == 0) return TuneResult(data, 0)
        return TuneResult(
            EchoData(data.key, data.level, data.exp, attributes, data.rarity, data.sonataEffect),
            tuned,
        )
    }
}
