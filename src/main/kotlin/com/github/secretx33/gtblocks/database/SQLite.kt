package com.github.secretx33.gtblocks.database

import com.cryptomorin.xseries.XMaterial
import com.github.secretx33.gtblocks.model.CommandBlock
import com.github.secretx33.gtblocks.model.CommandBlocks
import com.github.secretx33.gtblocks.model.PurgeResult
import com.github.secretx33.gtblocks.util.extension.toUuid
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.plugin.Plugin
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.Op
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.SqlExpressionBuilder
import org.jetbrains.exposed.sql.Transaction
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import toothpick.InjectConstructor
import java.io.IOException
import java.lang.reflect.Type
import java.sql.SQLException
import javax.inject.Singleton

@Singleton
@InjectConstructor
class SQLite(plugin: Plugin) {

    private val log = plugin.logger
    private val dbFile = plugin.dataFolder.absoluteFile.resolve("database.db")
    private val lock = Semaphore(1)

    init {
        try {
            dbFile.parentFile?.mkdirs()
        } catch (e: IOException) {
            log.severe("ERROR: Could not create folder ${dbFile.parent} for ${dbFile.name} file\n${e.stackTraceToString()}")
            Bukkit.getPluginManager().disablePlugin(plugin)
        }
    }

    private val url = "jdbc:sqlite:${dbFile.absoluteFile}"
    private val ds = HikariDataSource(hikariConfig.apply { jdbcUrl = url })

    init {
        try {
            Database.connect(ds)
            transaction { SchemaUtils.create(CommandBlocks) }
            log.info("DB initiated")
        } catch (e: SQLException) {
            log.severe("ERROR: An exception occurred while trying to connect to the database and create the tables\n${e.stackTraceToString()}")
            throw e
        }
    }

    fun getAllCommandBlocks(): Set<CommandBlock> = blockingTransaction {
        val blocks = CommandBlocks.selectAll().mapTo(hashSetOf()) { it.toCommandBlock() }
        // filter to only blocks that have their world loaded & match their stored material
        // (didn't get replaced when the plugin was disabled or something)
        blocks.filterTo(hashSetOf()) {
            Bukkit.getWorld(it.worldUuid)?.getBlockAt(it.x, it.y, it.z)?.type == it.material
        }
    }

    /**
     * Remove entries from worlds that are not currently loaded, and blocks that don't match
     * their original, stored material anymore. Used to keep database integrity and remove entries
     * from deleted worlds, for example.
     *
     * @return PurgeResult An object containing the results of this purge operation. More information
     * can be seen by looking at the class documentation.
     */
    suspend fun purgeInvalidEntriesFromDatabase(): PurgeResult = suspendTransaction {
        val allBlocks = CommandBlocks.selectAll().mapTo(hashSetOf()) { it.toCommandBlock() }

        // unloaded worlds
        val worldsToRemove = allBlocks
            .groupingBy { it.worldUuid }
            .eachCount()
            .filter { Bukkit.getWorld(it.key) == null }

        worldsToRemove.keys.forEach { worldUuid ->
            CommandBlocks.deleteWhere { CommandBlocks.worldUuid eq worldUuid.toString() }
        }

        // loaded world, but block is from a different material
        val blocksToRemove = allBlocks
            .filter {
                val world = Bukkit.getWorld(it.worldUuid)
                world != null && world.getBlockAt(it.x, it.y, it.z).type != it.material
            }.groupBy { it.worldUuid }

        blocksToRemove.flatMap { it.value }
            .forEach { CommandBlocks.deleteWhere(op = filterByLocation(it)) }

        commit()
        PurgeResult(removedWorlds = worldsToRemove, removedBlocks = blocksToRemove.mapValues { it.value.size })
    }

    fun insertCommandBlock(block: CommandBlock) = suspendTransactionFromSync {
        CommandBlocks.insert {
            it[worldUuid] = block.worldUuid.toString()
            it[x] = block.x
            it[y] = block.y
            it[z] = block.z
            it[material] = block.material.toString()
            it[leftClickCommands] = gson.toJson(block.leftClickCommands, stringListType)
            it[rightClickCommands] = gson.toJson(block.rightClickCommands, stringListType)
        }
        commit()
    }

    fun updateCommandsFromBlock(block: CommandBlock) = suspendTransactionFromSync {
        CommandBlocks.update(filterByLocation(block)) {
            it[leftClickCommands] = gson.toJson(block.leftClickCommands, stringListType)
            it[rightClickCommands] = gson.toJson(block.rightClickCommands, stringListType)
        }
        commit()
    }

    fun removeCommandBlock(block: CommandBlock) = suspendTransactionFromSync {
        CommandBlocks.deleteWhere(op = filterByLocation(block))
        commit()
    }

    private fun filterByLocation(block: CommandBlock): SqlExpressionBuilder.() -> Op<Boolean> = {
        (CommandBlocks.x eq block.x)
            .and((CommandBlocks.y eq block.y))
            .and((CommandBlocks.z eq block.z))
            .and(CommandBlocks.worldUuid eq block.worldUuid.toString())
    }

    private fun ResultRow.toCommandBlock(): CommandBlock =
        CommandBlock(
           worldUuid = get(CommandBlocks.worldUuid).toUuid(),
           x = get(CommandBlocks.x),
           y = get(CommandBlocks.y),
           z = get(CommandBlocks.z),
           material = XMaterial.matchXMaterial(get(CommandBlocks.material))
               .map { it.parseMaterial() }
               .orElseThrow { IllegalStateException("Material '${get(CommandBlocks.material)}' stored in database could not be parsed back to Material") }!!,
           leftClickCommands = gson.fromJson(get(CommandBlocks.leftClickCommands), stringListType),
           rightClickCommands = gson.fromJson(get(CommandBlocks.rightClickCommands), stringListType),
        )

    fun closeDatabase() = runBlocking {
        lock.withPermit { runCatching { ds.close() } }
    }

    private fun <T> blockingTransaction(statement: Transaction.() -> T): T =
        runBlocking {
            lock.withPermit {
                transaction(statement = statement)
            }
        }

    private fun suspendTransactionFromSync(statement: suspend Transaction.() -> Unit) =
        CoroutineScope(Dispatchers.IO).launch {
            suspendTransaction(statement)
        }

    private suspend fun <T> suspendTransaction(statement: suspend Transaction.() -> T) =
        withContext(Dispatchers.IO) {
            lock.withPermit {
                newSuspendedTransaction(statement = statement)
            }
        }

    private companion object {
        val gson: Gson = GsonBuilder()
            .registerTypeAdapter(Location::class.java, LocationAdapter())
            .create()

        val stringListType: Type = object : TypeToken<List<String>>() {}.type

        val hikariConfig = HikariConfig().apply {
            maximumPoolSize = 10
            isAutoCommit = false
            addDataSourceProperty("reWriteBatchedInserts", "true")
            addDataSourceProperty("characterEncoding", "utf8")
            addDataSourceProperty("useLegacyDatetimeCode", "false")
        }
    }
}
