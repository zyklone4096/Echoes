package dev.zyklone.echoes.listeners

import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageByEntityEvent
import java.util.UUID

internal object SkillListener: Listener {
    val armadillo = HashSet<UUID>()

    @EventHandler
    fun onEntityDamageByEntity(event: EntityDamageByEntityEvent) {
        val player = event.entity as? Player
            ?: return

        // Armadillo
        if (armadillo.remove(player.uniqueId)) {
            event.damage = (event.damage - 1.0) / 2.0
        }
    }
}