package dev.zyklone.echoes.contents.sonata

import dev.zyklone.echoes.contents.echoes.EchoData
import dev.zyklone.echoes.contents.sonata.builtin.FreezingFrost
import dev.zyklone.echoes.contents.sonata.builtin.HavocEclipse
import dev.zyklone.echoes.contents.sonata.builtin.LingeringTunes
import dev.zyklone.echoes.contents.sonata.builtin.MoonlitClouds
import org.bukkit.NamespacedKey
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BuiltinSonataTest {
    private val sonatas = listOf(FreezingFrost(), HavocEclipse(), LingeringTunes(), MoonlitClouds())
    private val firstKey = NamespacedKey("minecraft", "zombie")
    private val secondKey = NamespacedKey("minecraft", "skeleton")
    private val otherEffect = NamespacedKey("echoes", "other")

    @Test
    fun `empty slots and a single matching echo do not activate sonatas`() {
        for (sonata in sonatas) {
            assertFalse(sonata.check(emptyList()), sonata.rawName)
            assertFalse(sonata.check(listOf(null, null)), sonata.rawName)
            assertFalse(
                sonata.check(listOf(null, EchoData(firstKey, sonataEffect = sonata.key), null)),
                sonata.rawName,
            )
        }
    }

    @Test
    fun `duplicate echo IDs count once regardless of level or rarity`() {
        for (sonata in sonatas) {
            val first = EchoData(firstKey, sonataEffect = sonata.key)
            val duplicate = EchoData(firstKey, level = 5, rarity = 4, sonataEffect = sonata.key)
            assertFalse(sonata.check(listOf(first, first)), sonata.rawName)
            assertFalse(sonata.check(listOf(first, duplicate)), sonata.rawName)
        }
    }

    @Test
    fun `two distinct matching echo IDs activate sonatas despite duplicates and empty slots`() {
        for (sonata in sonatas) {
            val first = EchoData(firstKey, sonataEffect = sonata.key)
            val second = EchoData(secondKey, sonataEffect = sonata.key)
            assertTrue(sonata.check(listOf(first, second)), sonata.rawName)
            assertTrue(sonata.check(listOf(null, first, first, second, null)), sonata.rawName)
        }
    }

    @Test
    fun `echoes with other effects do not count even when their ID matches the sonata key`() {
        for (sonata in sonatas) {
            val first = EchoData(firstKey, sonataEffect = sonata.key)
            val other = EchoData(secondKey, sonataEffect = otherEffect)
            val sameKey = EchoData(sonata.key, sonataEffect = otherEffect)
            assertFalse(sonata.check(listOf(first, other)), sonata.rawName)
            assertFalse(sonata.check(listOf(sameKey, sameKey)), sonata.rawName)
        }
    }

    @Test
    fun `nonmatching effects do not hide matching echoes with the same ID`() {
        for (sonata in sonatas) {
            val other = EchoData(firstKey, sonataEffect = otherEffect)
            val first = EchoData(firstKey, sonataEffect = sonata.key)
            val second = EchoData(secondKey, sonataEffect = sonata.key)
            assertTrue(sonata.check(listOf(other, first, second)), sonata.rawName)
            assertTrue(sonata.check(listOf(first, other, second)), sonata.rawName)
        }
    }
}
