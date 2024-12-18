package com.danidipp.sneakymail

import me.clip.placeholderapi.expansion.PlaceholderExpansion
import org.bukkit.entity.Player
import org.jetbrains.annotations.NotNull
import java.util.*

class Placeholders : PlaceholderExpansion() {
    override fun getIdentifier(): @NotNull String {
        return SneakyMail.IDENTIFIER
    }

    override fun getAuthor(): @NotNull String {
        return SneakyMail.AUTHORS
    }

    override fun getVersion(): String {
        return SneakyMail.VERSION
    }

    override fun onPlaceholderRequest(player: Player, identifier: String): String? {
        return when (identifier) {
            "unread_count" -> {
                val mailCount = SneakyMail.mail.values.count {
                    player.uniqueId == UUID.fromString(it.recipient_uuid)
                    && it.available
                }
                mailCount.toString()
            }
            else -> null
        }
    }
}