package dev.zyklone.echoes.contents.player

import dev.zyklone.echoes.Echoes
import dev.zyklone.echoes.database.DataBankModel
import dev.zyklone.echoes.database.DataBankTable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import org.bukkit.entity.Player
import java.util.UUID
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap

object PlayerManager {
    private val access = ConcurrentHashMap<UUID, PlayerDataAccess>()
    private val loading = ConcurrentHashMap<UUID, CompletableFuture<PlayerDataAccess>>()

    private suspend fun load(player: Player): PlayerDataAccess {
        val databank = DataBankTable.get(player.uniqueId)
            ?: DataBankModel()

        return PlayerDataAccess(
            player.uniqueId,
            databank,
        ).apply {
            access[player.uniqueId] = this
        }
    }

    suspend fun push(player: Player) {
        this.access.remove(player.uniqueId)?.let { this.save0(it) }
    }

    suspend fun save(player: Player) {
        this.access[player.uniqueId]?.let { this.save0(it) }
    }

    private suspend fun save0(data: PlayerDataAccess) {
        DataBankTable.update(data.uuid, data.dataBank)
    }

    fun loadAsync(player: Player): CompletableFuture<PlayerDataAccess> {
        val uniqueId = player.uniqueId
        this.access[uniqueId]?.let { return CompletableFuture.completedFuture(it) }

        val future = CompletableFuture<PlayerDataAccess>()
        loading.putIfAbsent(uniqueId, future)?.let { return it }
        CoroutineScope(Echoes.INST.coroutines).launch {
            try {
                future.complete(access[uniqueId] ?: load(player))
            } catch (e: Throwable) {
                future.completeExceptionally(e)
                if (e is CancellationException) throw e
            } finally {
                loading.remove(uniqueId, future)
            }
        }
        return future
    }

    operator fun get(player: Player) = this.access[player.uniqueId]
}
