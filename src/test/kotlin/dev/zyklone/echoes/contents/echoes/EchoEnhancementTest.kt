package dev.zyklone.echoes.contents.echoes

import dev.zyklone.echoes.config.Configurations
import dev.zyklone.echoes.contents.attributes.EchoAttributes
import dev.zyklone.echoes.contents.items.SealedTubeType
import org.bukkit.NamespacedKey
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class EchoEnhancementTest {
    private val echoKey = NamespacedKey("minecraft", "zombie")
    private val sonataKey = NamespacedKey("echoes", "lingering_tunes")
    private val healthKey = NamespacedKey("echoes", "health")
    private val damageKey = NamespacedKey("echoes", "damage")
    private val defenseKey = NamespacedKey("echoes", "defense")

    private fun data(level: Int = 0, exp: Int = 0, rarity: Byte = 4) =
        EchoData(echoKey, level, exp, rarity = rarity, sonataEffect = sonataKey)

    private fun material(slot: Int, type: SealedTubeType, amount: Int) =
        EchoEnhancement.UpgradeMaterial(slot, type, amount)

    @Test
    fun `insufficient materials reach the highest available level and preserve experience`() {
        val original = data()
        val result = EchoEnhancement.upgrade(original, 25, 500, listOf(material(0, SealedTubeType.PREMIUM, 2)))

        assertEquals(5, result.data.level)
        assertEquals(2500, result.data.exp)
        assertEquals(mapOf(0 to 2), result.consumed)
        assertEquals(3, result.data.attributes.size)
        assertNull(result.data.attributes[2])
        assertEquals(0, original.level)
        assertEquals(0, original.exp)
        assertEquals(2, original.attributes.size)
    }

    @Test
    fun `target stops leveling and consuming materials while retaining overflow`() {
        val result = EchoEnhancement.upgrade(data(), 1, 500, listOf(material(3, SealedTubeType.PREMIUM, 64)))

        assertEquals(1, result.data.level)
        assertEquals(4500, result.data.exp)
        assertEquals(mapOf(3 to 1), result.consumed)
    }

    @Test
    fun `mixed stacks are consumed by increasing tube experience`() {
        val result = EchoEnhancement.upgrade(data(), 4, 500, listOf(
            material(1, SealedTubeType.PREMIUM, 3),
            material(4, SealedTubeType.MEDIUM, 1),
            material(5, SealedTubeType.BASIC, 1),
        ))

        assertEquals(4, result.data.level)
        assertEquals(1500, result.data.exp)
        assertEquals(listOf(5, 4, 1), result.consumed.keys.toList())
        assertEquals(mapOf(5 to 1, 4 to 1, 1 to 1), result.consumed)
    }

    @Test
    fun `existing experience is used and unlocked slots remain empty`() {
        val result = EchoEnhancement.upgrade(data(level = 4, exp = 2400), 5, 500,
            listOf(material(0, SealedTubeType.BASIC, 1)))

        assertEquals(5, result.data.level)
        assertEquals(400, result.data.exp)
        assertEquals(mapOf(0 to 1), result.consumed)
        assertNull(result.data.attributes[2])
    }

    @Test
    fun `experience sufficient for the target requires no materials`() {
        val result = EchoEnhancement.upgrade(data(exp = 1500), 2, 500,
            listOf(material(0, SealedTubeType.PREMIUM, 64)))

        assertEquals(2, result.data.level)
        assertEquals(0, result.data.exp)
        assertTrue(result.consumed.isEmpty())
    }

    @Test
    fun `partial experience is preserved even if no level is gained`() {
        val result = EchoEnhancement.upgrade(data(level = 5), 10, 500,
            listOf(material(0, SealedTubeType.BASIC, 3)))

        assertEquals(5, result.data.level)
        assertEquals(1500, result.data.exp)
        assertEquals(mapOf(0 to 3), result.consumed)
    }

    @Test
    fun `no materials and unchanged target do not consume or reset progress`() {
        val original = data(level = 5, exp = 100)
        val missing = EchoEnhancement.upgrade(original, 10, 500, emptyList())
        assertEquals(5, missing.data.level)
        assertEquals(100, missing.data.exp)
        assertTrue(missing.consumed.isEmpty())

        val unchanged = EchoEnhancement.upgrade(original, 5, 500,
            listOf(material(0, SealedTubeType.PREMIUM, 64)))
        assertSame(original, unchanged.data)
        assertTrue(unchanged.consumed.isEmpty())
    }

    @Test
    fun `rarity caps the requested level and further upgrades consume nothing`() {
        for (rarity in 0..4) {
            val original = data(rarity = rarity.toByte())
            assertEquals((rarity + 1) * 5, original.maxLevel)
            val materials = listOf(material(0, SealedTubeType.PREMIUM, 64))
            val result = EchoEnhancement.upgrade(original, 25, 500, materials)
            assertEquals(original.maxLevel, result.data.level)
            assertTrue(result.data.exp < 5000)
            assertTrue(EchoEnhancement.upgrade(result.data, 25, 500, materials).consumed.isEmpty())
        }
    }

    @Test
    fun `large experience requirements do not overflow and invalid increments are rejected`() {
        val result = EchoEnhancement.upgrade(data(level = 24), 25, Int.MAX_VALUE,
            listOf(material(0, SealedTubeType.ADVANCED, 1)))
        assertEquals(24, result.data.level)
        assertEquals(2000, result.data.exp)
        for (increment in listOf(0, -1)) {
            assertFailsWith<IllegalArgumentException> {
                EchoEnhancement.upgrade(data(), 1, increment, emptyList())
            }
        }
    }

    @Test
    fun `main attributes use fixed range increments without rerolling secondary attributes`() {
        EchoAttributes.init(Configurations(), Path.of("."))
        val original = EchoData(echoKey, level = 5, attributes = arrayOf(
            healthKey to 0.08, damageKey to 0.09, defenseKey to 0.1,
        ), rarity = 4, sonataEffect = sonataKey)
        val materials = listOf(material(0, SealedTubeType.PREMIUM, 20))

        val direct = EchoEnhancement.upgrade(original, 10, 500, materials).data
        val intermediate = EchoEnhancement.upgrade(original, 7, 500, materials).data
        val incremental = EchoEnhancement.upgrade(intermediate, 10, 500, materials).data
        assertEquals(0.0904, direct.attributes[0]!!.second as Double, 1e-12)
        assertEquals(0.1004, direct.attributes[1]!!.second as Double, 1e-12)
        assertEquals(direct.attributes[0]!!.second as Double, incremental.attributes[0]!!.second as Double, 1e-12)
        assertEquals(direct.attributes[1]!!.second as Double, incremental.attributes[1]!!.second as Double, 1e-12)
        assertEquals(healthKey, direct.attributes[0]!!.first)
        assertEquals(original.attributes[2], direct.attributes[2])
        assertNull(direct.attributes[3])
        assertEquals(0.08, original.attributes[0]!!.second)
    }

    @Test
    fun `main attributes advance only for the levels actually reached`() {
        EchoAttributes.init(Configurations(), Path.of("."))
        val original = EchoData(echoKey, attributes = arrayOf(healthKey to 0.064),
            rarity = 4, sonataEffect = sonataKey)
        val result = EchoEnhancement.upgrade(original, 25, 500,
            listOf(material(0, SealedTubeType.PREMIUM, 2)))

        assertEquals(5, result.data.level)
        assertEquals(0.0744, result.data.attributes[0]!!.second as Double, 1e-12)
        assertEquals(0.064, original.attributes[0]!!.second)

        val unchanged = EchoEnhancement.upgrade(result.data, 25, 500, emptyList())
        assertEquals(5, unchanged.data.level)
        assertEquals(result.data.attributes[0], unchanged.data.attributes[0])
    }

    @Test
    fun `main attributes never exceed their base maximum during batch upgrades`() {
        EchoAttributes.init(Configurations(), Path.of("."))
        val original = EchoData(echoKey, attributes = arrayOf(healthKey to 0.115),
            rarity = 4, sonataEffect = sonataKey)
        val result = EchoEnhancement.upgrade(original, 25, 500,
            listOf(material(0, SealedTubeType.PREMIUM, 64)))

        assertEquals(25, result.data.level)
        assertEquals(0.116, result.data.attributes[0]!!.second as Double)
    }

    @Test
    fun `tuning costs ten per empty slot and never rerolls an existing slot`() {
        val original = data(level = 15)
        original.attributes[2] = damageKey to 0.1
        var calls = 0
        val result = EchoEnhancement.tune(original, 19) { blacklist ->
            assertEquals(listOf(damageKey), blacklist)
            calls++
            healthKey to 0.2
        }

        assertEquals(1, calls)
        assertEquals(1, result.tunedSlots)
        assertEquals(10, result.consumed)
        assertEquals(original.attributes[2], result.data.attributes[2])
        assertEquals(healthKey to 0.2, result.data.attributes[3])
        assertNull(result.data.attributes[4])
        assertNull(original.attributes[3])
    }

    @Test
    fun `one star locked slots and too few tuners never generate an attribute`() {
        for ((original, count) in listOf(data(level = 5, rarity = 0) to 100, data() to 100, data(level = 5) to 9)) {
            val result = EchoEnhancement.tune(original, count) { error("Must not generate an attribute") }
            assertSame(original, result.data)
            assertEquals(0, result.consumed)
        }
    }

    @Test
    fun `generated secondary keys are excluded from subsequent rolls but main keys are allowed`() {
        val original = data(level = 10)
        original.attributes[0] = healthKey to 0.6
        val generated = mutableListOf<NamespacedKey>()
        val result = EchoEnhancement.tune(original, 20) { blacklist ->
            assertEquals(generated, blacklist)
            val key = if (generated.isEmpty()) healthKey else damageKey
            generated.add(key)
            key to 0.1
        }

        assertEquals(2, result.tunedSlots)
        assertEquals(20, result.consumed)
        assertEquals(healthKey, result.data.attributes[2]!!.first)
        assertEquals(damageKey, result.data.attributes[3]!!.first)
        val repeated = EchoEnhancement.tune(result.data, 100) { error("Must not reroll a tuned slot") }
        assertSame(result.data, repeated.data)
        assertEquals(0, repeated.consumed)
    }

    @Test
    fun `attribute exhaustion leaves remaining slots empty and consumes only successful rolls`() {
        var calls = 0
        val result = EchoEnhancement.tune(data(level = 15), 30) {
            if (calls++ == 0) healthKey to 0.1 else null
        }

        assertEquals(1, result.tunedSlots)
        assertEquals(10, result.consumed)
        assertNull(result.data.attributes[3])
        assertNull(result.data.attributes[4])
    }

    @Test
    fun `failed rolls leave the source echo unchanged`() {
        val original = data(level = 10)
        assertFailsWith<IllegalStateException> {
            EchoEnhancement.tune(original, 20) { blacklist ->
                if (blacklist.isEmpty()) healthKey to 0.1 else error("Failed roll")
            }
        }
        assertTrue(original.attributes.all { it == null })
    }
}
