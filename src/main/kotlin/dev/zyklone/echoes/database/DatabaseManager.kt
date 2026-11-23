package dev.zyklone.echoes.database

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import dev.zyklone.echoes.Echoes
import dev.zyklone.echoes.config.DatabaseConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.v1.core.Transaction
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.v1.jdbc.transactions.suspendTransaction
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.migration.jdbc.MigrationUtils
import kotlin.io.path.absolutePathString

internal class DatabaseManager(config: DatabaseConfig) {
    private val source: HikariDataSource
    private val database: Database

    init {
        val hc = HikariConfig()
        hc.jdbcUrl = when (config.type) {
            DatabaseConfig.Type.MYSQL -> "jdbc:mysql://${config.host}:${config.port}/${config.database}"
            DatabaseConfig.Type.SQLITE -> "jdbc:sqlite:${Echoes.INST.dataPath.resolve("database.db").absolutePathString()}"
        }
        hc.username = config.username
        hc.password = config.password

        this.source = HikariDataSource(hc)
        this.database = Database.connect(this.source)

        transaction(database) {
            val tables = arrayOf(
                DataBankTable,
                DataBankCollectionTable
            )

            SchemaUtils.create(*tables)
            MigrationUtils.statementsRequiredForDatabaseMigration(*tables).forEach {
                if (it.isNotEmpty())
                    exec(it)
            }
        }
    }

    suspend fun <T> query(stmt: suspend Transaction.() -> T) =
        withContext(Echoes.INST.coroutines) {
            suspendTransaction(database, statement = stmt)
        }

    fun close() {
        this.source.close()
    }
}
