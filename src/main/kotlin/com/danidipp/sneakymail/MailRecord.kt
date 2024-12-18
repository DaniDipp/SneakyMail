package com.danidipp.sneakymail

import io.github.agrevster.pocketbaseKotlin.models.Record
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import java.util.UUID

@Serializable
sealed class MailReward {
    @Serializable
    @SerialName("item")
    data class ItemReward(
        val item: String,
        val amount: Int
    ): MailReward()

    @Serializable
    @SerialName("command")
    data class CommandReward(
        val command: String,
    ): MailReward()
}

@Serializable
data class MailRecord(
    var sender_uuid: String,
    var sender_name: String,
    var recipient_uuid: String,
    var recipient_name: String,
    val rewards: List<MailReward>?,
    var available: Boolean,
    var note: String
): Record() {
    fun checkUuid() {
        if (recipient_uuid.isEmpty()) {
            if ( recipient_name.isNotEmpty()) {
                val player = Bukkit.getOfflinePlayerIfCached(recipient_name)
                if (player != null)
                    recipient_uuid = player.uniqueId.toString()
            }
        } else if (!recipient_uuid.contains('-')) {
            recipient_uuid = recipient_uuid.replaceFirst(
                "(.{8})(.{4})(.{4})(.{4})(.{12})".toRegex(),
                "$1-$2-$3-$4-$5"
            )
        }
    }

    fun redeem(player: Player): Boolean {
        if (!available) {
            player.sendMessage("You have already collected this mail")
            return false
        }
        available = false
        val id = this.id!!
        val data = Json.encodeToString(serializer(), this)
        Bukkit.getScheduler().runTaskAsynchronously(SneakyMail.getInstance(), Runnable {
            runBlocking { launch {
                SneakyMail.getInstance().pbHandler.pocketbase.records.update<MailRecord>("lom2_mail", id, data)
            } }
        })
        if (rewards == null) {
            player.sendMessage(Component.text("This mail has no rewards", NamedTextColor.RED))
            return false
        }
        for (reward in rewards) {
            when (reward) {
                is MailReward.ItemReward -> {
                    player.sendMessage(Component.text("Item rewards are not implemented yet", NamedTextColor.RED))
                    return false
                }
                is MailReward.CommandReward -> {
                    val command = reward.command.replace("{player}", player.name)
                    SneakyMail.getInstance().logger.info("Redeeming ${player.name}'s mail $id with command: $command")
                    Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), command)
                }
            }
        }
        return true
    }
}

