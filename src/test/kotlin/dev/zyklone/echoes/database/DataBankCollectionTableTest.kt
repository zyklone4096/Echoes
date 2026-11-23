package dev.zyklone.echoes.database

import net.kyori.adventure.key.Key
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.dao.id.java.UUIDTable
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.migration.jdbc.MigrationUtils
import java.nio.file.Files
import java.util.UUID
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class DataBankCollectionTableTest {
    private val echo = Key.key("minecraft", "zombie")
    private val sonata = Key.key("echoes", "lingering_tunes")

    private fun collect(player: UUID, echo: Key, sonata: Key, rarity: Byte, increment: Int = 100) =
        DataBankCollectionTable.addCollectedInTransaction(player, echo, sonata, rarity, increment)

    private fun withDatabase(createTables: Boolean = true, test: (Database) -> Unit) {
        val file = Files.createTempFile("echoes-data-bank-", ".db")
        try {
            val database = Database.connect("jdbc:sqlite:$file", driver = "org.sqlite.JDBC")
            if (createTables) {
                transaction(database) {
                    SchemaUtils.create(DataBankTable, DataBankCollectionTable)
                }
            }
            test(database)
        } finally {
            Files.deleteIfExists(file)
        }
    }

    private fun storedExp(player: UUID): UInt = DataBankTable
        .select(DataBankTable.exp)
        .where { DataBankTable.id eq player }
        .single()[DataBankTable.exp]

    @Test
    fun `each new quality awards only its newly unlocked experience`() = withDatabase { database ->
        val player = UUID.randomUUID()
        transaction(database) {
            listOf(0u, 10u, 10u, 15u, 20u).forEachIndexed { rarity, expected ->
                val result = collect(player, echo, sonata, rarity.toByte())
                assertEquals(expected, result.awardedExp)
                assertEquals(result.dataBank.exp, storedExp(player))
                assertEquals(rarity.toUByte(), DataBankCollectionTable.collectedRarityInTransaction(player, echo, sonata))
            }
            assertEquals(55u, storedExp(player))
        }
    }

    @Test
    fun `first five star collection and skipped qualities grant cumulative experience`() = withDatabase { database ->
        transaction(database) {
            val first = UUID.randomUUID()
            assertEquals(55u, collect(first, echo, sonata, 4).awardedExp)
            val upgraded = UUID.randomUUID()
            assertEquals(10u, collect(upgraded, echo, sonata, 1).awardedExp)
            assertEquals(45u, collect(upgraded, echo, sonata, 4).awardedExp)
            assertEquals(storedExp(first), storedExp(upgraded))
        }
    }

    @Test
    fun `duplicates and lower qualities preserve highest quality and experience`() = withDatabase { database ->
        val player = UUID.randomUUID()
        transaction(database) { collect(player, echo, sonata, 4) }
        transaction(database) {
            for (rarity in 0..4) {
                assertEquals(0u, collect(player, echo, sonata, rarity.toByte()).awardedExp)
            }
            assertEquals(4.toUByte(), DataBankCollectionTable.collectedRarityInTransaction(player, echo, sonata))
            assertEquals(55u, storedExp(player))
        }
    }

    @Test
    fun `collection keys distinguish players echoes and sonatas`() = withDatabase { database ->
        val player = UUID.randomUUID()
        val otherPlayer = UUID.randomUUID()
        val otherEcho = Key.key("minecraft", "skeleton")
        val otherSonata = Key.key("echoes", "havoc_eclipse")
        transaction(database) {
            collect(player, echo, sonata, 1)
            assertNull(DataBankCollectionTable.collectedRarityInTransaction(player, echo, otherSonata))
            assertNull(DataBankCollectionTable.collectedRarityInTransaction(player, otherEcho, sonata))
            assertNull(DataBankCollectionTable.collectedRarityInTransaction(otherPlayer, echo, sonata))
            assertEquals(10u, collect(player, echo, otherSonata, 1).awardedExp)
            assertEquals(10u, collect(player, otherEcho, sonata, 1).awardedExp)
            assertEquals(10u, collect(otherPlayer, echo, sonata, 1).awardedExp)
            assertEquals(30u, storedExp(player))
            assertEquals(10u, storedExp(otherPlayer))
        }
    }

    @Test
    fun `collection and experience roll back together`() = withDatabase { database ->
        val player = UUID.randomUUID()
        transaction(database) { collect(player, echo, sonata, 1) }
        assertFailsWith<IllegalStateException> {
            transaction(database) {
                collect(player, echo, sonata, 4)
                error("Abort reward transaction")
            }
        }
        transaction(database) {
            assertEquals(1.toUByte(), DataBankCollectionTable.collectedRarityInTransaction(player, echo, sonata))
            assertEquals(10u, storedExp(player))
        }
    }

    @Test
    fun `invalid qualities and experience overflow leave collection unchanged`() = withDatabase { database ->
        val player = UUID.randomUUID()
        transaction(database) {
            for (rarity in listOf((-1).toByte(), 5.toByte())) {
                assertFailsWith<IllegalArgumentException> {
                    collect(player, echo, sonata, rarity)
                }
            }
            DataBankTable.updateInTransaction(player, DataBankModel(UInt.MAX_VALUE))
        }
        assertFailsWith<IllegalArgumentException> {
            transaction(database) { collect(player, echo, sonata, 1) }
        }
        transaction(database) {
            assertNull(DataBankCollectionTable.collectedRarityInTransaction(player, echo, sonata))
            assertEquals(UInt.MAX_VALUE, storedExp(player))
        }
    }

    @Test
    fun `concurrent duplicate collections grant experience once`() = withDatabase { database ->
        val player = UUID.randomUUID()
        val executor = Executors.newFixedThreadPool(4)
        try {
            val results = executor.invokeAll(List(8) {
                Callable {
                    transaction(database) {
                        collect(player, echo, sonata, 4).awardedExp
                    }
                }
            }).map { it.get() }
            assertEquals(55u, results.fold(0u, UInt::plus))
            transaction(database) { assertEquals(55u, storedExp(player)) }
        } finally {
            executor.shutdownNow()
        }
    }

    @Test
    fun `level and remaining experience persist and continue upgrading after reload`() = withDatabase { database ->
        val player = UUID.randomUUID()
        transaction(database) {
            val result = collect(player, echo, sonata, 4, increment = 10)
            assertEquals(DataBankModel(exp = 25u, level = 2), result.dataBank)
        }
        transaction(database) {
            assertEquals(DataBankModel(exp = 25u, level = 2), DataBankTable.getInTransaction(player))
            val result = collect(player, echo, Key.key("echoes", "other"), 1, increment = 10)
            assertEquals(DataBankModel(exp = 5u, level = 3), result.dataBank)
        }
        transaction(database) {
            assertEquals(DataBankModel(exp = 5u, level = 3), DataBankTable.getInTransaction(player))
        }
    }

    @Test
    fun `changed increment preserves stored level until experience is awarded`() = withDatabase { database ->
        val player = UUID.randomUUID()
        val initial = transaction(database) { collect(player, echo, sonata, 4, increment = 10).dataBank }
        transaction(database) {
            DataBankTable.saveInTransaction(player, initial)
            assertEquals(initial, DataBankTable.getInTransaction(player))
            assertEquals(initial, collect(player, echo, sonata, 4, increment = 1).dataBank)
            val higherIncrement = collect(player, echo, Key.key("echoes", "other"), 1, increment = 100)
            assertEquals(DataBankModel(exp = 35u, level = 2), higherIncrement.dataBank)
        }
        transaction(database) {
            val lowerIncrement = collect(player, Key.key("minecraft", "skeleton"), sonata, 1, increment = 1)
            assertEquals(DataBankModel(exp = 3u, level = 9), lowerIncrement.dataBank)
        }
    }

    @Test
    fun `stale cache cannot replace higher level with larger remaining experience`() = withDatabase { database ->
        val player = UUID.randomUUID()
        transaction(database) { collect(player, echo, sonata, 4, increment = 10) }
        transaction(database) {
            DataBankTable.saveInTransaction(player, DataBankModel(exp = 55u, level = 0))
            DataBankTable.saveInTransaction(player, DataBankModel(exp = 10u, level = 2))
            assertEquals(DataBankModel(exp = 25u, level = 2), DataBankTable.getInTransaction(player))
        }
    }

    @Test
    fun `collection level and remaining experience roll back together`() = withDatabase { database ->
        val player = UUID.randomUUID()
        transaction(database) { collect(player, echo, sonata, 1, increment = 10) }
        assertFailsWith<IllegalStateException> {
            transaction(database) {
                collect(player, echo, sonata, 4, increment = 10)
                error("Abort level upgrade")
            }
        }
        transaction(database) {
            assertEquals(DataBankModel(exp = 0u, level = 1), DataBankTable.getInTransaction(player))
            assertEquals(1.toUByte(), DataBankCollectionTable.collectedRarityInTransaction(player, echo, sonata))
        }
    }

    @Test
    fun `concurrent distinct rewards preserve all level progress`() = withDatabase { database ->
        val player = UUID.randomUUID()
        val executor = Executors.newFixedThreadPool(4)
        try {
            val results = executor.invokeAll(List(8) { index ->
                Callable {
                    transaction(database) {
                        collect(player, echo, Key.key("echoes", "sonata_$index"), 4, increment = 10).awardedExp
                    }
                }
            }).map { it.get() }
            assertEquals(440u, results.fold(0u, UInt::plus))
            transaction(database) {
                assertEquals(DataBankModel(exp = 80u, level = 8), DataBankTable.getInTransaction(player))
            }
        } finally {
            executor.shutdownNow()
        }
    }

    @Test
    fun `migration adds a level without recalculating existing experience`() = withDatabase(createTables = false) { database ->
        val player = UUID.randomUUID()
        val legacy = object : UUIDTable("echoes_data_bank", columnName = "player") {
            val exp = uinteger("exp").default(0u)
        }
        transaction(database) {
            SchemaUtils.create(legacy)
            legacy.insert {
                it[legacy.id] = player
                it[legacy.exp] = 300u
            }
        }
        transaction(database) {
            val tables = arrayOf(DataBankTable, DataBankCollectionTable)
            SchemaUtils.create(*tables)
            MigrationUtils.statementsRequiredForDatabaseMigration(*tables).forEach { statement ->
                if (statement.isNotEmpty()) exec(statement)
            }
        }
        transaction(database) {
            assertEquals(DataBankModel(exp = 300u, level = 0), DataBankTable.getInTransaction(player))
            DataBankTable.saveInTransaction(player, DataBankModel())
            assertEquals(DataBankModel(exp = 300u, level = 0), DataBankTable.getInTransaction(player))
            assertEquals(DataBankModel(exp = 10u, level = 2), collect(player, echo, sonata, 1).dataBank)
        }
    }

    @Test
    fun `direct level increment persists and survives a stale cache save`() = withDatabase { database ->
        val player = UUID.randomUUID()
        val before = DataBankModel(exp = 90u, level = 2)
        transaction(database) { DataBankTable.updateInTransaction(player, before) }
        transaction(database) {
            assertEquals(DataBankModel(exp = 90u, level = 7), DataBankTable.addLevelInTransaction(player, 5))
        }
        transaction(database) {
            DataBankTable.saveInTransaction(player, before)
            assertEquals(DataBankModel(exp = 90u, level = 7), DataBankTable.getInTransaction(player))
            assertEquals(DataBankModel(exp = 90u, level = 30), DataBankTable.addLevelInTransaction(player, Int.MAX_VALUE))
        }
    }

    @Test
    fun `direct level increment creates a new player and rolls back on failure`() = withDatabase { database ->
        val player = UUID.randomUUID()
        assertFailsWith<IllegalStateException> {
            transaction(database) {
                DataBankTable.addLevelInTransaction(player, 5)
                error("Abort direct level increment")
            }
        }
        transaction(database) {
            assertNull(DataBankTable.getInTransaction(player))
            assertEquals(DataBankModel(level = 5), DataBankTable.addLevelInTransaction(player, 5))
        }
    }

    @Test
    fun `concurrent direct level increments are all applied`() = withDatabase { database ->
        val player = UUID.randomUUID()
        val executor = Executors.newFixedThreadPool(4)
        try {
            executor.invokeAll(List(8) {
                Callable { transaction(database) { DataBankTable.addLevelInTransaction(player, 1) } }
            }).forEach { it.get() }
            transaction(database) { assertEquals(DataBankModel(level = 8), DataBankTable.getInTransaction(player)) }
        } finally {
            executor.shutdownNow()
        }
    }
}
