package dev.zyklone.echoes

import com.typesafe.config.ConfigFactory
import dev.zyklone.echoes.commands.AdminCommand
import dev.zyklone.echoes.commands.MainCommand
import dev.zyklone.echoes.config.Configurations
import dev.zyklone.echoes.contents.attributes.EchoAttributes
import dev.zyklone.echoes.contents.echoes.EchoRegistry
import dev.zyklone.echoes.contents.items.EchoItems
import dev.zyklone.echoes.contents.sonata.SonataRegistry
import dev.zyklone.echoes.database.DatabaseManager
import dev.zyklone.echoes.listeners.CombatListener
import dev.zyklone.echoes.listeners.PlayerListener
import io.papermc.paper.threadedregions.scheduler.ScheduledTask
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.hocon.Hocon
import kotlinx.serialization.hocon.decodeFromConfig
import kotlinx.serialization.hocon.encodeToConfig
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin
import org.incendo.cloud.execution.ExecutionCoordinator
import org.incendo.cloud.paper.PaperCommandManager
import org.incendo.cloud.paper.util.sender.PaperSimpleSenderMapper
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.io.path.createDirectories
import kotlin.io.path.exists
import kotlin.io.path.reader
import kotlin.io.path.writeText

class Echoes : JavaPlugin() {
    companion object {
        lateinit var INST: Echoes
            private set

        @OptIn(ExperimentalSerializationApi::class)
        val HOCON by lazy { Hocon {
            encodeDefaults = true
        } }

        const val NAMESPACE = "echoes"

        fun Player.runOnScheduler(retired: Runnable? = null, task: (ScheduledTask) -> Unit) =
            this.scheduler.run(INST, task, retired)
    }

    init {
        INST = this
    }

    private var load = false

    internal lateinit var config: Configurations
        private set

    internal lateinit var threads: ExecutorService
        private set
    internal lateinit var coroutines: CoroutineDispatcher
        private set
    private val log = this.slF4JLogger

    internal lateinit var db: DatabaseManager
        private set

    override fun onLoad() {
        val dp = this.dataPath
        dp.createDirectories()

        this.log.info("Loading configurations")
        try {
            val configFile = dp.resolve("config.hocon")
            @OptIn(ExperimentalSerializationApi::class)
            this.config = if (configFile.exists()) {
                configFile.reader().use {
                    HOCON.decodeFromConfig<Configurations>(ConfigFactory.parseReader(it))
                }
            } else {
                val new = Configurations()
                configFile.writeText(HOCON.encodeToConfig(new).root().render())
                new
            }
        } catch (e: Exception) {
            throw ExceptionInInitializerError(e)
        }

        load = true
    }

    override fun onEnable() {
        require(load)

        this.threads = Executors.newCachedThreadPool()
        this.coroutines = this.threads.asCoroutineDispatcher()

        val dp = this.dataPath
        this.log.info("Initializing contents")
        EchoItems.init()
        EchoAttributes.init(this.config, dp)
        EchoRegistry.init()
        SonataRegistry.init()

        this.log.info("Setting up database")
        this.db = DatabaseManager(this.config.database)

        val pm = Bukkit.getPluginManager()
        pm.registerEvents(PlayerListener(), this)
        pm.registerEvents(CombatListener(), this)

        val cm = PaperCommandManager
            .builder(PaperSimpleSenderMapper.simpleSenderMapper())
            .executionCoordinator(ExecutionCoordinator.asyncCoordinator())
            .buildOnEnable(this)
        AdminCommand.register(cm)
        MainCommand.register(cm)
    }

    override fun onDisable() {
        this.threads.shutdownNow()
        this.db.close()
    }
}
