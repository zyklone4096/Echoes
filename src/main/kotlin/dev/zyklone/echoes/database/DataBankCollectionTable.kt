package dev.zyklone.echoes.database

import dev.zyklone.echoes.Echoes
import net.kyori.adventure.key.Key
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.java.javaUUID
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.upsert
import java.util.UUID

object DataBankCollectionTable: Table("echoes_data_bank_collection") {
    val key = javaUUID("id")
    val item = varchar("item", 255)
    val sonata = varchar("sonata", 255)
    /** 与 EchoData 一致，0-4 分别表示一至五星；保存最高已收集品质。 */
    val rarity = ubyte("level").default(0u)

    override val primaryKey: PrimaryKey = PrimaryKey(key, item, sonata)

    suspend fun isCollected(player: UUID, nk: Key, st: Key) = Echoes.INST.db.query {
        collectedRarityInTransaction(player, nk, st) != null
    }

    suspend fun addCollected(player: UUID, nk: Key, st: Key, rarity: Byte): DataBankCollectionResult {
        require(rarity in 0..4) { "Echo rarity must be between 0 and 4" }
        val levelIncrement = Echoes.INST.config.gameplay.dataBankLevelIncrement
        return Echoes.INST.db.query {
            addCollectedInTransaction(player, nk, st, rarity, levelIncrement)
        }
    }

    internal fun collectedRarityInTransaction(player: UUID, nk: Key, st: Key): UByte? =
        select(rarity)
            .where { (key eq player) and (item eq nk.toString()) and (sonata eq st.toString()) }
            .singleOrNull()
            ?.get(rarity)

    internal fun addCollectedInTransaction(
        player: UUID,
        nk: Key,
        st: Key,
        rarity: Byte,
        levelIncrement: Int,
    ): DataBankCollectionResult {
        require(rarity in 0..4) { "Echo rarity must be between 0 and 4" }
        require(levelIncrement > 0) { "Data bank level experience increment must be positive" }
        val bank = DataBankTable.getOrCreateInTransaction(player)
        val previous = collectedRarityInTransaction(player, nk, st)
        if (previous != null && previous.toInt() >= rarity) {
            return DataBankCollectionResult(0u, bank)
        }

        // 二至五星各档奖励 10、10、15、20 点，首次高品质收集也解锁较低档位。
        val previousExp = previous?.let { collectionExp(it.toByte()) } ?: 0u
        val awardedExp = collectionExp(rarity) - previousExp
        val updatedBank = bank.addExp(awardedExp, levelIncrement)
        upsert {
            it[key] = player
            it[item] = nk.toString()
            it[sonata] = st.toString()
            it[this@DataBankCollectionTable.rarity] = rarity.toUByte()
        }
        DataBankTable.updateInTransaction(player, updatedBank)
        return DataBankCollectionResult(awardedExp, updatedBank)
    }

    private fun collectionExp(rarity: Byte): UInt = when (rarity.toInt()) {
        0 -> 0u
        1 -> 10u
        2 -> 20u
        3 -> 35u
        4 -> 55u
        else -> error("Invalid echo rarity: $rarity")
    }
}

data class DataBankCollectionResult(
    val awardedExp: UInt,
    val dataBank: DataBankModel,
)
