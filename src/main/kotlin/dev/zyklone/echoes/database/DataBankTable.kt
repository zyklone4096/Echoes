package dev.zyklone.echoes.database

import dev.zyklone.echoes.Echoes
import org.jetbrains.exposed.v1.core.dao.id.java.UUIDTable
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.upsert
import java.util.UUID

object DataBankTable: UUIDTable("echoes_data_bank", columnName = "player") {
    val exp = uinteger("exp").default(0u)
    val level = ubyte("level").default(0u)

    suspend fun get(player: UUID) =
        Echoes.INST.db.query {
            getInTransaction(player)
        }

    suspend fun update(player: UUID, model: DataBankModel) =
        Echoes.INST.db.query {
            saveInTransaction(player, model)
        }

    suspend fun addLevel(player: UUID, amount: Int): DataBankModel {
        require(amount > 0) { "Data bank level increment must be positive" }
        return Echoes.INST.db.query {
            addLevelInTransaction(player, amount)
        }
    }

    internal fun addLevelInTransaction(player: UUID, amount: Int): DataBankModel {
        require(amount > 0) { "Data bank level increment must be positive" }
        val updated = getOrCreateInTransaction(player).addLevel(amount)
        updateInTransaction(player, updated)
        return updated
    }

    internal fun getInTransaction(player: UUID): DataBankModel? =
        select(exp, level)
            .where { id eq player }
            .singleOrNull()
            ?.let { DataBankModel(exp = it[exp], level = it[level].toInt()) }

    /** 在当前事务中创建并锁定玩家的数据坞行，串行化同一玩家的收集奖励。 */
    internal fun getOrCreateInTransaction(player: UUID): DataBankModel {
        upsert(onUpdate = { it[exp] = exp }) {
            it[id] = player
            it[exp] = 0u
            it[level] = 0u
        }
        return requireNotNull(getInTransaction(player))
    }

    internal fun saveInTransaction(player: UUID, model: DataBankModel) {
        val stored = getOrCreateInTransaction(player)
        // 升级会扣除等级内经验，先比较等级，避免旧缓存覆盖更高等级的进度。
        val latest = if (model.level > stored.level || model.level == stored.level && model.exp > stored.exp) {
            model
        } else {
            stored
        }
        updateInTransaction(player, latest)
    }

    internal fun updateInTransaction(player: UUID, model: DataBankModel) {
        upsert {
            it[id] = player
            it[exp] = model.exp
            it[level] = model.level.toUByte()
        }
    }
}

data class DataBankModel(
    /** 当前等级内的剩余经验，升级时扣除本级所需经验。 */
    val exp: UInt = 0u,
    val level: Int = 0,
) {
    companion object {
        const val MAX_LEVEL = 30
    }

    init {
        require(level in 0..MAX_LEVEL) { "Data bank level must be between 0 and $MAX_LEVEL" }
    }

    fun addLevel(amount: Int): DataBankModel {
        require(amount > 0) { "Data bank level increment must be positive" }
        return copy(level = level + minOf(amount, MAX_LEVEL - level))
    }

    fun addExp(amount: UInt, levelIncrement: Int): DataBankModel {
        require(levelIncrement > 0) { "Data bank level experience increment must be positive" }
        // 配置改变和零经验奖励不会重算已有等级。
        if (amount == 0u) return this
        require(amount <= UInt.MAX_VALUE - exp) { "Data bank experience exceeds UInt.MAX_VALUE" }
        var remainingExp = (exp + amount).toULong()
        var nextLevel = level
        while (nextLevel < MAX_LEVEL) {
            val requiredExp = levelIncrement.toULong() * (nextLevel + 1).toULong()
            if (remainingExp < requiredExp) break
            remainingExp -= requiredExp
            nextLevel++
        }
        return copy(exp = remainingExp.toUInt(), level = nextLevel)
    }

    val dropRate: Double
        get() = when {
            this.level >= 10 -> 0.2
            this.level >= 4 -> 0.15
            this.level >= 1 -> 0.1
            else -> 0.6
        }

    val maxRarity: Byte
        get() = when {
            this.level >= 15 -> 5
            this.level >= 8 -> 4
            this.level >= 3 -> 3
            else -> 2
        }

    val maxCost: Int
        get() = when {
            this.level >= 9 -> 12
            this.level >= 2 -> 10
            else -> 8
        }
}
