package dev.zyklone.echoes.contents.player

import dev.zyklone.echoes.contents.echoes.Echo
import dev.zyklone.echoes.contents.sonata.Sonata
import dev.zyklone.echoes.database.DataBankCollectionTable
import dev.zyklone.echoes.database.DataBankModel
import dev.zyklone.echoes.database.DataBankTable
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import java.util.UUID

class PlayerDataAccess internal constructor(
    val uuid: UUID,
    dataBank: DataBankModel
) {
    @Volatile
    var dataBank: DataBankModel = dataBank
        private set

    val player: Player?
        get() = Bukkit.getPlayer(this.uuid)

    fun available() = this.player != null

    private val lock = Mutex()

    suspend fun addDataBankLevel(amount: Int): DataBankModel =
        lock.withLock {
            DataBankTable.addLevel(uuid, amount).also { dataBank = it }
        }

    /** rarity 使用实际获得的声骸品质，0-4 分别表示一至五星。 */
    suspend fun setCollected(echo: Echo, sonata: Sonata, rarity: Byte): UInt =
        lock.withLock {
            val result = DataBankCollectionTable.addCollected(
                this.uuid,
                echo.key,
                sonata.key,
                rarity,
            )
            dataBank = result.dataBank
            result.awardedExp
        }

    suspend fun isCollected(echo: Echo, sonata: Sonata): Boolean =
        DataBankCollectionTable.isCollected(
            this.uuid,
            echo.key,
            sonata.key
        )
}
