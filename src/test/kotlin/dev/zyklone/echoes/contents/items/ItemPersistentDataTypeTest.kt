package dev.zyklone.echoes.contents.items

import org.bukkit.persistence.PersistentDataType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ItemPersistentDataTypeTest {
    private val types = listOf<PersistentDataType<Byte, *>>(SealedTubePDT, TunerPDT)

    @Test
    fun `material primitive types match the standard boxed byte type`() {
        for (type in types) {
            assertEquals(PersistentDataType.BYTE.primitiveType, type.primitiveType)
            assertFalse(type.primitiveType.isPrimitive)
        }
    }

    @Test
    fun `material primitive types accept boxed bytes returned by Paper`() {
        for (type in types) {
            for (value in 0..3) {
                assertTrue(type.primitiveType.isInstance(value.toByte()))
            }
        }
    }
}
