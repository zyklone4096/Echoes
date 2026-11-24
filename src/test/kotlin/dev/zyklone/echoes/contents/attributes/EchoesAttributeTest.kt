package dev.zyklone.echoes.contents.attributes

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class EchoesAttributeTest {
    private data class Range(
        val attribute: EchoesAttribute.DoubleAttribute,
        val min: Double,
        val max: Double,
    )

    private val ranges = listOf(
        Range(HealthAttribute(), 0.064, 0.116),
        Range(HealthValueAttribute(), 3.2, 5.8),
        Range(DamageAttribute(), 0.064, 0.116),
        Range(DamageValueAttribute(), 1.0, 1.7),
        Range(DefenseAttribute(), 0.081, 0.147),
        Range(DefenseValueAttribute(), 1.3, 2.0),
    )

    @Test
    fun `built in attributes expose their original base value ranges`() {
        for ((attribute, min, max) in ranges) {
            assertEquals(min, attribute.minBaseValue)
            assertEquals(max, attribute.maxBaseValue)
        }
    }

    @Test
    fun `next value grows by a fixed increment based on the rarity level cap`() {
        for ((attribute, min, max) in ranges) {
            for (maxLevel in listOf(5, 10, 15, 20, 25)) {
                val expected = min + (max - min) / maxLevel
                assertEquals(expected, attribute.next(min, maxLevel), 1e-12)
                assertEquals(expected, attribute.nextWide(min, maxLevel) as Double, 1e-12)
            }
        }
    }

    @Test
    fun `repeated increments stay within the base range and reach its maximum`() {
        for ((attribute, min, max) in ranges) {
            var value = min
            repeat(25) {
                val next = attribute.next(value, 25)
                assertTrue(next >= value)
                assertTrue(next in min..max)
                value = next
            }
            assertEquals(max, value, 1e-12)
            assertEquals(max, attribute.next(max, 25))
            assertEquals(max, attribute.next(max + 1.0, 25))
        }
    }

    @Test
    fun `attributes without a base range retain their existing values`() {
        val attribute = HealingAttribute()
        assertEquals(0.0, attribute.next(0.0, 25))
        assertEquals(0.1, attribute.next(0.1, 25))
    }

    @Test
    fun `invalid maximum levels are rejected`() {
        for (range in ranges) {
            val attribute = range.attribute
            for (maxLevel in listOf(0, -1)) {
                assertFailsWith<IllegalArgumentException> { attribute.next(attribute.minBaseValue, maxLevel) }
            }
        }
    }
}
