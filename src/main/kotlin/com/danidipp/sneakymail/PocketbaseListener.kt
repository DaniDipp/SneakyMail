package com.danidipp.sneakymail

import com.danidipp.sneakypocketbase.AsyncPocketbaseEvent
import com.danidipp.sneakypocketbase.PBRunnable
import com.danidipp.sneakypocketbase.SneakyPocketbase
import io.github.agrevster.pocketbaseKotlin.dsl.query.Filter
import io.github.agrevster.pocketbaseKotlin.dsl.query.SortFields
import io.github.agrevster.pocketbaseKotlin.services.RealtimeService.RealtimeActionType.*
import org.bukkit.Bukkit
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import java.util.logging.Logger

class PocketbaseListener : Listener {
    private val logger: Logger
    constructor(logger: Logger, sneakyPB: SneakyPocketbase) {
        this.logger = logger
        logger.warning("Registering callback for when Pocketbase is loaded")
        sneakyPB.onPocketbaseLoaded {
            logger.warning("Pocketbase is loaded. Now subscribing to mail events")
            sneakyPB.subscribeAsync("lom2_mail")

            logger.warning("Fetching current mail records")
            Bukkit.getScheduler().runTaskAsynchronously(SneakyMail.getInstance(), PBRunnable {

                val mailList = sneakyPB.pb().records.getFullList<MailRecord>(
                    "lom2_mail",
                    500,
                    SortFields(),
                    Filter("available=true")
                )
                mailList.forEach {
                    it.checkUuid()
                    SneakyMail.mail[it.id!!] = it
                }
                logger.warning("Fetched ${mailList.size} mail records")
            })
        }
    }

    @EventHandler
    fun onMailEvent(event: AsyncPocketbaseEvent) {
        if (event.collectionName != "lom2_mail") return
        logger.warning("Received mail event: ${event.action}")
        val mailRecord = event.data.parseRecord<MailRecord>()

        when (event.action) {
            CREATE ->{
                mailRecord.checkUuid()
                SneakyMail.mail[mailRecord.id!!] = mailRecord
                Bukkit.getScheduler().runTask(SneakyMail.getInstance(), Runnable {
                    val mailEvent = NewMailEvent(mailRecord)
                    Bukkit.getPluginManager().callEvent(mailEvent)
                })
            }
            UPDATE -> {
                mailRecord.checkUuid()
                if (!SneakyMail.mail.containsKey(mailRecord.id)) {
                    event.action = CREATE
                    onMailEvent(event)
                    return
                }

                val oldRecord = SneakyMail.mail[mailRecord.id]!!
                if (!oldRecord.available && mailRecord.available) {
                    // Mail has been marked as available again
                    Bukkit.getScheduler().runTask(SneakyMail.getInstance(), Runnable {
                        val mailEvent = NewMailEvent(mailRecord)
                        Bukkit.getPluginManager().callEvent(mailEvent)
                    })
                } else {
                    SneakyMail.mail[mailRecord.id!!] = mailRecord
                }
            }
            DELETE -> {
                SneakyMail.mail.remove(mailRecord.id)
            }
            else -> {
                throw IllegalStateException("${event.action} can never occur. (${event.data.toString()}")
            }
        }

    }
}