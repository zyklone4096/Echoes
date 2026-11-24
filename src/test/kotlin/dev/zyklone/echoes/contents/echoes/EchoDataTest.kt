package dev.zyklone.echoes.contents.echoes

import org.bukkit.NamespacedKey
import org.bukkit.persistence.PersistentDataContainer
import java.lang.reflect.Proxy
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class EchoDataTest {
    @Test
    fun `experience is read from its own persistent data key rather than the level key`() {
        // 仅模拟键值读写，验证所选 PDC 键；不替代 Paper 的序列化集成测试。
        val values = mutableMapOf<NamespacedKey, Any>()
        val container = Proxy.newProxyInstance(
            PersistentDataContainer::class.java.classLoader,
            arrayOf(PersistentDataContainer::class.java),
        ) { _, method, args ->
            when (method.name) {
                "set" -> {
                    values[args!![0] as NamespacedKey] = args[2]
                    null
                }
                "get" -> values[args!![0] as NamespacedKey]
                else -> error("Unexpected PDC method ${method.name}")
            }
        } as PersistentDataContainer
        val original = EchoData(
            NamespacedKey("minecraft", "zombie"), level = 5, exp = 2400,
            rarity = 4, sonataEffect = NamespacedKey("echoes", "lingering_tunes"),
        )

        container.echoData = original
        val restored = assertNotNull(container.echoData)
        assertEquals(original.level, restored.level)
        assertEquals(original.exp, restored.exp)
        assertEquals(original.key, restored.key)
        assertEquals(original.sonataEffect, restored.sonataEffect)
        assertEquals(original.attributes.toList(), restored.attributes.toList())
    }
}
