package dev.zyklone.echoes.listeners

import dev.zyklone.echoes.Echoes
import dev.zyklone.echoes.contents.player.PlayerManager
import dev.zyklone.echoes.contents.attributes.EchoAttributes
import dev.zyklone.echoes.contents.echoes.isEchoData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.event.world.WorldSaveEvent

internal class PlayerListener: Listener {
    @EventHandler
    fun onPlayerJoin(event: PlayerJoinEvent) {
        EchoAttributes.invalidate(event.player)
        event.player.scheduler.runDelayed(Echoes.INST, {
            PlayerManager.loadAsync(event.player)
        }, null, 20)
    }

    @EventHandler
    fun onSave(event: WorldSaveEvent) {
        val players = event.world.players.toList()
        CoroutineScope(Echoes.INST.coroutines).launch {
            for (player in players) {
                if (player.isOnline) {
                    try {
                        PlayerManager.save(player)
                    } catch (e: Exception) {

                    }
                }
            }
        }
    }

    @EventHandler
    fun onPlayerQuit(event: PlayerQuitEvent) {
        EchoAttributes.invalidate(event.player)
        CoroutineScope(Echoes.INST.coroutines).launch {
            PlayerManager.push(event.player)
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    fun onInteract(event: PlayerInteractEvent) {
        if (event.item?.persistentDataContainer?.isEchoData == true) {  // disable interact for Echo items
            event.isCancelled = true
        }
    }
}
