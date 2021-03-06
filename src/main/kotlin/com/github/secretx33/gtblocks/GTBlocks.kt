package com.github.secretx33.gtblocks

import com.github.secretx33.gtblocks.database.SQLite
import me.mattstudios.msg.bukkit.BukkitMessage
import org.bukkit.plugin.Plugin
import org.bukkit.plugin.java.JavaPlugin
import toothpick.Scope
import toothpick.ktp.KTP
import toothpick.ktp.binding.bind
import toothpick.ktp.binding.module
import toothpick.ktp.extension.getInstance

class GTBlocks : JavaPlugin() {

    private val mod = module {
        bind<Plugin>().toInstance(this@GTBlocks)
        bind<JavaPlugin>().toInstance(this@GTBlocks)
        bind<BukkitMessage>().toInstance(BukkitMessage.create())
    }

    private lateinit var scope: Scope

    // this method is called when the plugin is loaded by the plugin loader
    override fun onEnable() {
        scope = KTP.openScope("GTBlocks").installModules(mod)
        // only used to eagerly instantiate the database class that is currently throwing
        // NoClassDefFoundError
        val sqlite = scope.getInstance<SQLite>()
        // test if database is being queried correctly (if it doesn't throw anything)
        sqlite.getAllCommandBlocks()
    }

    // this method is called when the plugin is unloaded by the plugin loader
    override fun onDisable() {
        scope.getInstance<SQLite>().closeDatabase()
        KTP.closeScope("GTBlocks")
    }
}
