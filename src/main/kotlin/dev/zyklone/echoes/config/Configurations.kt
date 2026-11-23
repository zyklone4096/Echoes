package dev.zyklone.echoes.config

import kotlinx.serialization.Serializable
import org.bukkit.Material

@Serializable
internal data class Configurations(
    val database: DatabaseConfig = DatabaseConfig(),

    // contents
    val builtinAttributes: BuiltinAttributes = BuiltinAttributes(),
    val items: Items = Items(),
    val gameplay: GamePlay = GamePlay(),
)

@Serializable
internal data class DatabaseConfig(
    val type: Type = Type.SQLITE,
    val username: String = "echoes",
    val password: String = "echoes",
    val database: String = "echoes",

    val host: String = "127.0.0.1",
    val port: String = "3306",
) {
    enum class Type {
        MYSQL, SQLITE
    }
}

@Serializable
internal data class BuiltinAttributes(
    val health: Boolean = true,
    val healthValue: Boolean = true,
    val damage: Boolean = true,
    val damageValue: Boolean = true,
    val defense: Boolean = true,
    val defenseValue: Boolean = true,
    val healing: Boolean = true,
)

@Serializable
internal data class Items(
    val defaultEchoItem: Item = Item.Vanilla(Material.IRON_GOLEM_SPAWN_EGG),
)

@Serializable
internal data class GamePlay(
    val dataBankLevelIncrement: Int = 100,
) {
    init {
        require(dataBankLevelIncrement > 0) { "Data bank level experience increment must be positive" }
    }
}
