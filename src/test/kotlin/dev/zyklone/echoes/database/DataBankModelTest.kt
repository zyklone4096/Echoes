package dev.zyklone.echoes.database

import dev.zyklone.echoes.config.GamePlay
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class DataBankModelTest {
    @Test
    fun `upgrade requirements increase by the configured increment`() {
        val before = DataBankModel().addExp(99u, 100)
        assertEquals(DataBankModel(exp = 99u, level = 0), before)
        val first = before.addExp(1u, 100)
        assertEquals(DataBankModel(exp = 0u, level = 1), first)
        assertEquals(DataBankModel(exp = 199u, level = 1), first.addExp(199u, 100))
        val second = first.addExp(200u, 100)
        assertEquals(DataBankModel(exp = 0u, level = 2), second)
        assertEquals(DataBankModel(exp = 0u, level = 3), second.addExp(300u, 100))
    }

    @Test
    fun `single reward can advance multiple levels and retain excess experience`() {
        assertEquals(DataBankModel(exp = 25u, level = 3), DataBankModel().addExp(625u, 100))
    }

    @Test
    fun `zero reward does not recalculate existing level after configuration changes`() {
        val stored = DataBankModel(exp = 90u, level = 2)
        assertEquals(stored, stored.addExp(0u, 1))
        assertEquals(DataBankModel(exp = 91u, level = 2), stored.addExp(1u, 100))
    }

    @Test
    fun `level is capped at thirty and excess experience is preserved`() {
        val capped = DataBankModel().addExp(46501u, 100)
        assertEquals(DataBankModel(exp = 1u, level = 30), capped)
        assertEquals(DataBankModel(exp = 56u, level = 30), capped.addExp(55u, 1))
    }

    @Test
    fun `large increment does not overflow the upgrade requirement`() {
        assertEquals(
            DataBankModel(exp = UInt.MAX_VALUE, level = 29),
            DataBankModel(level = 29).addExp(UInt.MAX_VALUE, Int.MAX_VALUE),
        )
    }

    @Test
    fun `invalid increments and levels are rejected`() {
        for (increment in listOf(0, -1)) {
            assertFailsWith<IllegalArgumentException> { GamePlay(dataBankLevelIncrement = increment) }
            assertFailsWith<IllegalArgumentException> { DataBankModel().addExp(1u, increment) }
        }
        for (level in listOf(-1, 31)) {
            assertFailsWith<IllegalArgumentException> { DataBankModel(level = level) }
        }
    }

    @Test
    fun `direct level increments preserve remaining experience`() {
        assertEquals(DataBankModel(exp = 90u, level = 7), DataBankModel(exp = 90u, level = 2).addLevel(5))
    }

    @Test
    fun `direct level increments cap at thirty without overflowing`() {
        val stored = DataBankModel(exp = 90u, level = 29)
        val capped = DataBankModel(exp = 90u, level = 30)
        assertEquals(capped, stored.addLevel(5))
        assertEquals(capped, stored.addLevel(Int.MAX_VALUE))
        assertEquals(capped, capped.addLevel(1))
    }

    @Test
    fun `nonpositive direct level increments are rejected`() {
        for (amount in listOf(0, -1, Int.MIN_VALUE)) {
            assertFailsWith<IllegalArgumentException> { DataBankModel().addLevel(amount) }
        }
    }
}
