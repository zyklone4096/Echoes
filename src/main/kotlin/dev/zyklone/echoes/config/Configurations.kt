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

    // custom
    val useCustomAttributes: Boolean = false,
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

    // Sealed tubes
    val basicSealedTube: Item = Item.BuiltinDefault,
    val mediumSealedTube: Item = Item.BuiltinDefault,
    val advancedSealedTube: Item = Item.BuiltinDefault,
    val premiumSealedTube: Item = Item.BuiltinDefault,

    // Tuners
    val basicTuner: Item = Item.BuiltinDefault,
    val mediumTuner: Item = Item.BuiltinDefault,
    val advancedTuner: Item = Item.BuiltinDefault,
    val premiumTuner: Item = Item.BuiltinDefault,

    // Modifiers
    val modifier: Item = Item.BuiltinDefault,
    val transducer: Item = Item.BuiltinDefault,
)

@Serializable
internal data class GamePlay(
    val dataBankLevelIncrement: Int = 100,
    val echoLevelIncrement: Int = 500,
) {
    init {
        require(dataBankLevelIncrement > 0) { "Data bank level experience increment must be positive" }
        require(echoLevelIncrement> 0) { "Echo level experience increment must be positive" }
    }
}
