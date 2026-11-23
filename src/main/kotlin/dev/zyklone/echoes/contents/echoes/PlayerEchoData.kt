package dev.zyklone.echoes.contents.echoes

import dev.zyklone.echoes.Echoes
import dev.zyklone.echoes.contents.attributes.EchoAttributes
import dev.zyklone.echoes.contents.sonata.Sonata
import dev.zyklone.echoes.contents.sonata.SonataRegistry
import org.bukkit.NamespacedKey
import org.bukkit.entity.Player
import org.bukkit.persistence.PersistentDataType

class PlayerEchoData private constructor(
    val echo: MutableList<EchoData?> = arrayListOf(null, null, null, null, null),
    var sonata: MutableSet<Sonata> = HashSet(),

    private var applied: Array<EchoData>? = null,
    private var appliedSonata: Array<Sonata.Applied>? = null
) {
    companion object {
        private val nkData = NamespacedKey(Echoes.NAMESPACE, "echo")
        private val nkApplied = NamespacedKey(Echoes.NAMESPACE, "applied_echo")
        private val nkSonata = NamespacedKey(Echoes.NAMESPACE, "sonata")
        private val nkAppliedSonata = NamespacedKey(Echoes.NAMESPACE, "applied_sonata")

        operator fun get(player: Player): PlayerEchoData {
            val pdc = player.persistentDataContainer
            val echoes = arrayListOf<EchoData?>(null, null, null, null, null)
            pdc[nkData, PersistentDataType.LIST.dataContainers()]
                ?.map {
                    it.echoData
                }?.forEachIndexed { index, data ->
                    if (index < 5) {
                        echoes[index] = data
                    }
                }
            val applied = pdc[nkApplied, PersistentDataType.LIST.dataContainers()]
                ?.mapNotNull {
                    it.echoData
                }
                ?: emptyList()
            val sonata = pdc[nkSonata, PersistentDataType.LIST.strings()]
                ?.mapNotNull { NamespacedKey.fromString(it)?.let(SonataRegistry::get) }
                ?.toHashSet()
                ?: HashSet()
            val appliedSonata = pdc[nkSonata, PersistentDataType.LIST.strings()]
                ?.mapNotNull { NamespacedKey.fromString(it)?.let(SonataRegistry::get) as? Sonata.Applied }
                ?.toHashSet()
                ?: HashSet()

            return PlayerEchoData(
                echoes,
                sonata,

                applied.toTypedArray(),
                appliedSonata.toTypedArray(),
            )
        }

        operator fun set(player: Player, data: PlayerEchoData) {
            val pdc = player.persistentDataContainer
            val ctx = pdc.adapterContext
            pdc[nkData, PersistentDataType.LIST.dataContainers()] = data.echo.map {
                ctx.newPersistentDataContainer().apply {
                    if (it != null)
                        this.echoData = it
                }
            }
            pdc[nkApplied, PersistentDataType.LIST.dataContainers()] = data.applied?.map {
                ctx.newPersistentDataContainer().apply { this.echoData = it }
            } ?: emptyList()
            pdc[nkSonata, PersistentDataType.LIST.strings()] =
                data.sonata.map { it.toString() }
        }

        fun getSonata(player: Player) =
            player.persistentDataContainer[nkSonata, PersistentDataType.LIST.strings()]
                ?.mapNotNull { NamespacedKey.fromString(it)?.let(SonataRegistry::get) }

        fun getFirst(player: Player) =
            player.persistentDataContainer[nkData, PersistentDataType.LIST.dataContainers()]
                ?.getOrNull(0)
                ?.echoData
    }

    fun update() {
        this.sonata.clear()
        this.sonata.addAll(SonataRegistry[this.echo])
    }

    fun apply(player: Player) {
        player.scheduler.run(Echoes.INST, { _ ->
            // echoes
            val pdc = player.persistentDataContainer
            val previous = this.applied?.asList()
                ?: pdc[nkApplied, PersistentDataType.LIST.dataContainers()]?.mapNotNull { it.echoData }
                ?: emptyList()
            val previousSummary = EchoAttributes.summarizeAttributes(player, previous)
            EchoAttributes.appliedAttributes(previous).forEach { it.clear(player, previousSummary) }

            val summary = EchoAttributes.update(player, this)
            EchoAttributes.appliedAttributes(this.echo).forEach { it.apply(player, summary) }
            this.applied = this.echo.filterNotNull().map {
                EchoData(it.key, it.level, it.exp, it.attributes, it.rarity, it.sonataEffect)
            }.toTypedArray()

            // sonata
            this.appliedSonata?.forEach { it.clear(player, this.echo) }
            this.appliedSonata = buildList {
                for (item in sonata) {
                    if (item is Sonata.Applied) {
                        item.apply(player, echo)
                        add(item)
                    }
                }
            }.toTypedArray()
            PlayerEchoData[player] = this
        }, null)
    }

    fun doSkill(player: Player) {
        val data = this.echo[0] ?: return
        EchoRegistry[data.key]?.skill(player, data)
    }
}
