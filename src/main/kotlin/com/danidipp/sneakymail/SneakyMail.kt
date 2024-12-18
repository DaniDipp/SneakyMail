package com.danidipp.sneakymail

import kotlinx.coroutines.*
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Bukkit
import org.bukkit.NamespacedKey
import org.bukkit.plugin.java.JavaPlugin
import java.util.concurrent.ConcurrentHashMap

class SneakyMail : JavaPlugin() {
    lateinit var pbHandler: PocketbaseHandler
    lateinit var pbShutdown: Function<Unit>
    val mailKey = NamespacedKey(this, "mail")
    private val commands = listOf(
        MailCommand(),
        MailcheckCommand()
    )
    private val eventListeners = listOf(
        NewMailEvent.listener,
        MailGUI.listener
    )

    @OptIn(DelicateCoroutinesApi::class)
    override fun onLoad() {
        instance = this

        saveDefaultConfig()
        val pbProtocol = config.getString("pocketbase.protocol", "http")!!
        val pbHost = config.getString("pocketbase.host")
        val pbUser = config.getString("pocketbase.user")
        val pbPassword = config.getString("pocketbase.password")

        if (pbHost.isNullOrEmpty() || pbUser.isNullOrEmpty() || pbPassword.isNullOrEmpty()) {
            logger.severe("Missing Pocketbase configuration")
            server.pluginManager.disablePlugin(this)
            return
        }
        pbHandler = PocketbaseHandler(logger, pbProtocol, pbHost, pbUser, pbPassword)
    }
    @OptIn(DelicateCoroutinesApi::class)
    override fun onEnable() {
        Bukkit.getServer().commandMap.registerAll(IDENTIFIER, commands)
        eventListeners.forEach {
            Bukkit.getServer().pluginManager.registerEvents(it, this)
        }

        Bukkit.getScheduler().runTaskAsynchronously(this, Runnable {
            pbHandler.runRealtime()
        })

        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            Placeholders().register();
        }
    }

    override fun onDisable() {
        logger.warning("Disabling SneakyMail")

        // Close open GUIs
        MailGUI.openInventories.forEach { it.close() }

        logger.warning("Shutting down Pocketbase")
        runBlocking {
            logger.warning("Disconnecting from Pocketbase Realtime")
            pbHandler.pocketbase.realtime.disconnect()
        }
    }

    companion object {
        const val IDENTIFIER = "sneakymail"
        const val AUTHORS = "Team Sneakymouse"
        const val VERSION = "1.0"
        private lateinit var instance: SneakyMail
        val mail: ConcurrentHashMap<String, MailRecord> = ConcurrentHashMap()

        fun getInstance(): SneakyMail {
            return instance
        }
    }
}