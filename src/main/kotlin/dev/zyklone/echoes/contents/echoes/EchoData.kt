package dev.zyklone.echoes.contents.echoes

import dev.zyklone.echoes.Echoes
import dev.zyklone.echoes.contents.attributes.EchoAttributes
import io.papermc.paper.persistence.PersistentDataContainerView
import org.bukkit.NamespacedKey
import org.bukkit.entity.Player
import org.bukkit.persistence.PersistentDataAdapterContext
import org.bukkit.persistence.PersistentDataContainer
import org.bukkit.persistence.PersistentDataType

class EchoData(
    /** 声骸种类标识，用于查找 Echo 定义中的 COST 和技能。 */
    val key: NamespacedKey,
    level: Int = 0,
    exp: Int = 0,
    attributes: Array<Pair<NamespacedKey, Any>?> = emptyArray(),
    /** 与 Echo.minRarity 一致，0-4 分别表示一至五星。 */
    val rarity: Byte = 0,
    /** 此件声骸的合鸣效果。 */
    val sonataEffect: NamespacedKey,
) {
    companion object {
        const val MAIN_ATTRIBUTE_COUNT = 2
    }

    init {
        require(rarity in 0..4) { "Echo rarity must be between 0 and 4" }
        require(level >= 0) { "Echo level must not be negative" }
        require(exp >= 0) { "Echo experience must not be negative" }
        require(attributes.size <= MAIN_ATTRIBUTE_COUNT + level / 5) {
            "Attributes exceed the slots unlocked at this level"
        }
    }

    var level: Int = level
        set(value) {
            require(value >= 0) { "Echo level must not be negative" }
            val slotCount = MAIN_ATTRIBUTE_COUNT + value / 5
            if (slotCount != this.attributes.size) {
                this.attributes = this.attributes.copyOf(slotCount)
            }
            field = value
        }

    /** 当前等级内已积累的强化经验。 */
    var exp: Int = exp
        set(value) {
            require(value >= 0) { "Echo experience must not be negative" }
            field = value
        }

    /**
     * 前两个位置为有序主属性，首条可用于主音属性重构；其余为已解锁的副属性槽。
     * 每条属性保存属性键与实际数值，副属性槽为 null 表示尚未调谐。
     */
    var attributes: Array<Pair<NamespacedKey, Any>?> = attributes.copyOf(MAIN_ATTRIBUTE_COUNT + level / 5)
        private set

    /** 可调谐槽位数量由强化等级计算，无需重复保存。 */
    val subAttributeSlotCount: Int
        get() = this.level / 5
}

private val nkKey = NamespacedKey(Echoes.NAMESPACE, "key")
private val nkLevel = NamespacedKey(Echoes.NAMESPACE, "level")
private val nkExp = NamespacedKey(Echoes.NAMESPACE, "exp")
private val nkAttributes = NamespacedKey(Echoes.NAMESPACE, "attributes")
private class EchoAttributeSlot(val attribute: Pair<NamespacedKey, Any>?)

private val attrType = PersistentDataType.LIST.listTypeFrom(object: PersistentDataType<PersistentDataContainer, EchoAttributeSlot> {
    override fun getPrimitiveType(): Class<PersistentDataContainer> =
        PersistentDataContainer::class.java

    override fun getComplexType(): Class<EchoAttributeSlot> =
        EchoAttributeSlot::class.java

    override fun toPrimitive(
        complex: EchoAttributeSlot,
        context: PersistentDataAdapterContext
    ): PersistentDataContainer = context.newPersistentDataContainer().apply {
        complex.attribute?.let { (nk, va) ->
            this[nkValue, PersistentDataType.STRING] = nk.toString()
            EchoAttributes.get(nk)?.setValueWide(this, va)
                ?: throw IllegalArgumentException("Unknown")
        }
    }

    override fun fromPrimitive(
        primitive: PersistentDataContainer,
        context: PersistentDataAdapterContext
    ): EchoAttributeSlot {
        if (primitive.isEmpty) return EchoAttributeSlot(null)
        val nk = requireNotNull(primitive[nkValue, PersistentDataType.STRING]?.let(NamespacedKey::fromString))
        return EchoAttributeSlot(nk to requireNotNull(EchoAttributes.get(nk)).getValueWide(primitive))
    }
})
private val nkValue = NamespacedKey(Echoes.NAMESPACE, "value")
private val nkRarity = NamespacedKey(Echoes.NAMESPACE, "rarity")
private val nkEffect = NamespacedKey(Echoes.NAMESPACE, "effect")

val PersistentDataContainerView.isEchoData: Boolean
    get() = this.has(nkKey)

val PersistentDataContainerView.echoData: EchoData?
    get() {
        val key = this[nkKey, PersistentDataType.STRING]
            ?.let(NamespacedKey::fromString)
            ?: return null
        val level = this[nkLevel, PersistentDataType.INTEGER]
            ?: return null
        val exp = this[nkLevel, PersistentDataType.INTEGER]
            ?: return null
        val attr = this[nkAttributes, attrType]
            ?: return null
        val rarity = this[nkRarity, PersistentDataType.BYTE]
            ?: return null
        val effect = this[nkEffect, PersistentDataType.STRING]
            ?.let(NamespacedKey::fromString)
            ?: return null

        return EchoData(
            key,
            level,
            exp,
            attr.map { it.attribute }.toTypedArray(),
            rarity,
            effect
        )
    }

var PersistentDataContainer.echoData: EchoData?
    get() = (this as PersistentDataContainerView).echoData
    set(value) {
        if (value != null) {
            this[nkKey, PersistentDataType.STRING] = value.key.toString()
            this[nkLevel, PersistentDataType.INTEGER] = value.level
            this[nkExp, PersistentDataType.INTEGER] = value.exp
            this[nkAttributes, attrType] = value.attributes.map(::EchoAttributeSlot)
            this[nkRarity, PersistentDataType.BYTE] = value.rarity
            this[nkEffect, PersistentDataType.STRING] = value.sonataEffect.toString()
        }
    }
