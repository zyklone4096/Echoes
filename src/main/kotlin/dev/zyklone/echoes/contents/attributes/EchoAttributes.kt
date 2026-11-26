package dev.zyklone.echoes.contents.attributes

import com.typesafe.config.ConfigFactory
import dev.zyklone.echoes.Echoes
import dev.zyklone.echoes.config.Configurations
import dev.zyklone.echoes.config.CustomAttribute
import dev.zyklone.echoes.contents.echoes.Echo
import dev.zyklone.echoes.contents.echoes.EchoData
import dev.zyklone.echoes.contents.echoes.PlayerEchoData
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.hocon.decodeFromConfig
import org.bukkit.NamespacedKey
import org.bukkit.entity.Player
import java.nio.file.Path
import java.util.UUID
import java.util.concurrent.ThreadLocalRandom
import kotlin.io.path.createDirectories
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.reader
import kotlin.random.asKotlinRandom

object EchoAttributes {
    private val registry = HashMap<NamespacedKey, EchoesAttribute<*>>()

    internal fun init(config: Configurations, dp: Path) {
        val cfg = config.builtinAttributes
        if (cfg.health) this.add0(HealthAttribute())
        if (cfg.healthValue) this.add0(HealthValueAttribute())
        if (cfg.damage) this.add0(DamageAttribute())
        if (cfg.damageValue) this.add0(DamageValueAttribute())
        if (cfg.defense) this.add0(DefenseAttribute())
        if (cfg.defenseValue) this.add0(DefenseValueAttribute())
        if (cfg.healing) this.add0(HealingAttribute())

        // custom attributes
        if (config.useCustomAttributes) {
            val log = Echoes.INST.slF4JLogger
            log.info("Loading custom attributes")
            val attrDir = dp.resolve("attributes")
            attrDir.createDirectories()
            @OptIn(ExperimentalSerializationApi::class)
            for (path in attrDir.listDirectoryEntries("**/*.{hocon,conf}")) {
                try {
                    val config =
                        Echoes.HOCON.decodeFromConfig<CustomAttribute>(path.reader().use(ConfigFactory::parseReader))
                    when (config) {
                        is CustomAttribute.Double ->
                            this.add(ConfiguredDoubleAttribute(config))
                    }
                } catch (e: Exception) {
                    log.error("Error loading custom attribute from $path", e)
                }
            }
        }
    }

    private fun add0(attr: EchoesAttribute<*>) {
        this.registry[attr.key] = attr
    }

    fun add(attribute: EchoesAttribute<*>) {
        if (attribute.key.namespace == Echoes.NAMESPACE)
            throw IllegalArgumentException("Attempted to register with internal namespace")
        if (attribute.key in this.registry)
            throw IllegalStateException("Attempted to register with duplicate key ${attribute.key}")
        this.add0(attribute)
    }

    fun size() = this.registry.size

    operator fun get(nk: NamespacedKey) = this.registry[nk]

    private val cache = HashMap<UUID, AttributeSummary>()

    fun update(player: Player) =
        this.update(player, PlayerEchoData[player])

    fun update(player: Player, data: PlayerEchoData): AttributeSummary {
        val summary = this.summarizeAttributes(player, data.echo)
        this.cache[player.uniqueId] = summary
        return summary
    }

    internal fun summarizeAttributes(player: Player, echoes: Iterable<EchoData?>): AttributeSummary {
        val summary = AttributeSummary()
        for (data in echoes) {
            val values = data?.attributes ?: continue
            for (value in values) {
                val (key, amount) = value ?: continue
                val attribute = this[key] ?: continue
                val container = player.persistentDataContainer.adapterContext.newPersistentDataContainer()
                attribute.setValueWide(container, amount)
                attribute.load(ReadAttributes(container), summary)
            }
        }
        return summary
    }

    internal fun appliedAttributes(echoes: Iterable<EchoData?>): Set<EchoesAttribute.AppliedAttribute> =
        echoes.asSequence()
            .flatMap { it?.attributes?.asSequence() ?: emptySequence() }
            .mapNotNull { value -> value?.first?.let { this[it] as? EchoesAttribute.AppliedAttribute } }
            .toSet()

    fun invalidate(player: Player) {
        this.cache.remove(player.uniqueId)
    }

    operator fun get(player: Player): AttributeSummary =
        this.cache[player.uniqueId] ?: this.update(player)

    fun random(player: Player, echo: Echo, blacklist: MutableList<NamespacedKey>? = null): Pair<NamespacedKey, Any> {
        val tlr = ThreadLocalRandom.current()
        val attr = registry.values.filter {
            blacklist == null || it.key !in blacklist
        }.toSet().random(tlr.asKotlinRandom())
        blacklist?.add(attr.key)
        return attr.key to attr.random(player, echo)!!
    }

    fun randomPrimary(player: Player, echo: Echo): Array<Pair<NamespacedKey, Any>?> {
        val bl = arrayListOf<NamespacedKey>()
        return arrayOf(
            random(player, echo, bl),
            random(player, echo, bl),
        )
    }
}
