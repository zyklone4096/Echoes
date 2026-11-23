package dev.zyklone.echoes.listeners

import dev.zyklone.echoes.contents.echoes.EchoRegistry
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDeathEvent

internal class CombatListener: Listener {
    @EventHandler
    fun onEntityDeath(event: EntityDeathEvent) {
        val player = event.entity.killer
            ?: return

        // vanilla drops
        EchoRegistry.getVanilla(event.entityType)?.let {
            event.drops += it.asItem(it.createRandom(player, event.entity))
        }
    }

    @EventHandler
    fun onEntityDamageByEntity() {

    }
}
