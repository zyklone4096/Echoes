package dev.zyklone.echoes.listeners

import dev.zyklone.echoes.Echoes
import dev.zyklone.echoes.commands.MESSAGE_HEADER
import dev.zyklone.echoes.commands.append
import dev.zyklone.echoes.contents.echoes.Echo
import dev.zyklone.echoes.contents.echoes.EchoRegistry
import dev.zyklone.echoes.contents.player.PlayerManager
import dev.zyklone.echoes.contents.sonata.SonataRegistry
import dev.zyklone.echoes.utils.literal
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import net.kyori.adventure.text.Component
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDeathEvent
import org.bukkit.inventory.ItemStack
import java.util.concurrent.ThreadLocalRandom

internal class CombatListener: Listener {
    @EventHandler
    fun onEntityDeath(event: EntityDeathEvent) {
        val player = event.entity.killer
            ?: return

        // vanilla drops
        EchoRegistry.getVanilla(event.entityType)?.let {
            drop(player, event.entity, it)
                ?.let { item -> event.drops += item }
        }
    }

    private fun drop(player: Player, from: LivingEntity, echo: Echo): ItemStack? {
        val data = PlayerManager[player]

        if (data == null) {
            PlayerManager.loadAsync(player)
            return null
        }

        if (ThreadLocalRandom.current().nextDouble(0.0, 1.0) <= data.dataBank.dropRate) {
            val created = echo.createRandom(player, from, rarity = data.dataBank.maxRarity)
            val sonata = requireNotNull(SonataRegistry[created.sonataEffect]) { "Unknown Sonata effect ${created.key}" }
            CoroutineScope(Echoes.INST.coroutines).launch {
                data.setCollected(echo, sonata, created.rarity)
            }
            player.sendMessage(Component.text()
                .append(MESSAGE_HEADER)
                .append("击败 ".literal())
                .append(from.customName() ?: Component.translatable(from.type.translationKey()))
                .append(" 获得 ".literal())
                .append(created, echo, sonata)
                )
            return echo.asItem(created)
        }
        return null
    }

    @EventHandler
    fun onEntityDamageByEntity() {

    }
}
