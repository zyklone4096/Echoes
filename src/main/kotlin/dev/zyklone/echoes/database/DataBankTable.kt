package dev.zyklone.echoes.database

import dev.zyklone.echoes.Echoes
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.java.UUIDTable
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.dao.java.UUIDEntity
import org.jetbrains.exposed.v1.dao.java.UUIDEntityClass
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.upsert
import java.util.UUID

object DataBankTable: UUIDTable("echoes_data_bank", columnName = "player") {
    val exp = uinteger("exp")

    suspend fun get(player: UUID) =
        Echoes.INST.db.query {
            select(exp)
                .where { DataBankTable.id eq player }
                .singleOrNull()
                ?.let {
                    DataBankModel(
                        it[exp]
                    )
                }
        }

    suspend fun update(player: UUID, model: DataBankModel) =
        Echoes.INST.db.query {
            upsert {
                it[id] = player
                it[exp] = model.exp
            }
        }
}

data class DataBankModel(
    val exp: UInt = 0u,
)
