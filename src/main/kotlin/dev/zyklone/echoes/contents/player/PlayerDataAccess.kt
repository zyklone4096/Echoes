package dev.zyklone.echoes.contents.player

import dev.zyklone.echoes.contents.echoes.Echo
import dev.zyklone.echoes.contents.sonata.Sonata
import dev.zyklone.echoes.database.DataBankCollectionTable
import dev.zyklone.echoes.database.DataBankModel
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import java.util.UUID
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

class PlayerDataAccess internal constructor(
    val uuid: UUID,
    val dataBank: DataBankModel
) {
    val player: Player?
        get() = Bukkit.getPlayer(this.uuid)

    fun available() = this.player != null

    private val lock = ReentrantReadWriteLock()

    suspend fun setCollected(echo: Echo, sonata: Sonata) {
        lock.write {
            DataBankCollectionTable.addCollected(
                this.uuid,
                echo.key,
                sonata.key
            )
        }
    }

    suspend fun isCollected(echo: Echo, sonata: Sonata) {
        this.lock.read {
            DataBankCollectionTable.isCollected(
                this.uuid,
                echo.key,
                sonata.key
            )
            // TODO player exp increment
        }
    }
}
