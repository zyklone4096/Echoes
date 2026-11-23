package dev.zyklone.echoes.database

import dev.zyklone.echoes.Echoes
import net.kyori.adventure.key.Key
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.dao.id.java.UUIDTable
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.upsert
import java.util.UUID

object DataBankCollectionTable: UUIDTable("echoes_data_bank_collection") {
    val item = varchar("item", 255)
    val sonata = varchar("sonata", 255)

    suspend fun isCollected(player: UUID, nk: Key, st: Key) = Echoes.INST.db.query {
        selectAll()
            .where { (this@DataBankCollectionTable.id eq player) and (item eq nk.toString()) and (sonata eq sonata.toString()) }
            .count() > 0
    }

    suspend fun addCollected(player: UUID, nk: Key, st: Key) = Echoes.INST.db.query {
        upsert {
            it[id] = player
            it[item] = nk.toString()
            it[sonata] = st.toString()
        }.insertedCount
    }
}
