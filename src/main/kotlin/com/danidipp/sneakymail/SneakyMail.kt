package com.danidipp.sneakymail

import org.bukkit.plugin.java.JavaPlugin

class SneakyMail : JavaPlugin() {

    override fun onLoad() {
        instance = this
    }
    override fun onEnable() {
        saveDefaultConfig()
    }

    companion object {
        const val IDENTIFIER = "sneakymail"
        const val AUTHORS = "Team Sneakymouse"
        const val VERSION = "1.0"
        private lateinit var instance: SneakyMail

        fun getInstance(): SneakyMail {
            return instance
        }
    }
}
