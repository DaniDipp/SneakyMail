package com.danidipp.sneakymail

import com.danidipp.sneakymail.SneakyMail.Companion
import io.github.agrevster.pocketbaseKotlin.PocketbaseClient
import io.github.agrevster.pocketbaseKotlin.dsl.login
import io.github.agrevster.pocketbaseKotlin.dsl.query.Filter
import io.github.agrevster.pocketbaseKotlin.dsl.query.SortFields
import io.github.agrevster.pocketbaseKotlin.services.RealtimeService
import io.github.agrevster.pocketbaseKotlin.services.RealtimeService.RealtimeActionType.*
import io.github.agrevster.pocketbaseKotlin.models.Record
import io.ktor.http.*
import kotlinx.coroutines.*
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import java.util.logging.Level
import java.util.logging.Logger
import kotlin.math.log

class PocketbaseHandler {
    val pocketbase: PocketbaseClient
    val logger: Logger
    private lateinit var authWait: Deferred<Unit>

    @OptIn(DelicateCoroutinesApi::class)
    constructor(logger: Logger,
                pbProtocol: String,
                pbHost: String,
                pbUser: String,
                pbPassword: String) {
        this.logger = logger
        pocketbase = PocketbaseClient({
            this.protocol = URLProtocol.byName[pbProtocol]!!
            this.host = pbHost
        })
        authWait = GlobalScope.async {
            val token = pocketbase.admins.authWithPassword(pbUser, pbPassword).token
            pocketbase.login { this.token = token }
        }
    }

    fun runRealtime() {
        runBlocking {
            runCatching {
                authWait.await()
            }.onFailure {
                logger.severe("Failed to authenticate with Pocketbase")
                logger.severe(it.stackTraceToString())
                Bukkit.getPluginManager().disablePlugin(SneakyMail.getInstance())
                return@runBlocking // Abort realtime service
            }

            launch(CoroutineName("Connect")) {
                runCatching {
                    pocketbase.realtime.connect()
                }.onFailure {
                    logger.severe("Failed to connect to Pocketbase Realtime")
                    logger.severe(it.stackTraceToString())
                    Bukkit.getPluginManager().disablePlugin(SneakyMail.getInstance())
                }
            }
            launch(CoroutineName("Fetch Current")) {
                val mailList = pocketbase.records.getFullList<MailRecord>(
                    "lom2_mail",
                    500,
                    SortFields(),
                    Filter("available=true")
                ).map { it.checkUuid(); it }

                SneakyMail.mail.putAll(mailList.associateBy { it.id!! })
            }
            launch(CoroutineName("Subscribe")) {
                pocketbase.realtime.subscribe("lom2_mail")
            }
            launch(CoroutineName("Listen")) {
                delay(1000)
                kotlin.runCatching {
                    pocketbase.realtime.listen {
                        when (action) {
                            CREATE -> callbackCreate(parseRecord<MailRecord>())
                            UPDATE -> callbackUpdate(parseRecord<MailRecord>())
                            DELETE -> callbackDelete(parseRecord<MailRecord>())
                            CONNECT -> logger.log(Level.FINE, "Connected to Pocketbase Realtime")
                        }
                    }
                }.onFailure {
                    logger.severe("Failed to listen to Pocketbase Realtime")
                    logger.severe(it.stackTraceToString())
                    Bukkit.getPluginManager().disablePlugin(SneakyMail.getInstance())
                }
            }

        }
    }

    private fun callbackCreate(record: Record) {
        val mailRecord = record as MailRecord
        mailRecord.checkUuid()
        SneakyMail.mail[mailRecord.id!!] = mailRecord
        Bukkit.getScheduler().runTask(SneakyMail.getInstance(), Runnable {
            val event = NewMailEvent(mailRecord)
            Bukkit.getPluginManager().callEvent(event)
        })
    }
    private fun callbackUpdate(record: Record) {
        val mailRecord = record as MailRecord
        mailRecord.checkUuid()
        if (!SneakyMail.mail.containsKey(mailRecord.id!!)) {
            callbackCreate(mailRecord)
        } else {
            val oldRecord = SneakyMail.mail[mailRecord.id!!]!!
            SneakyMail.mail[mailRecord.id!!] = mailRecord
            if (!oldRecord.available && mailRecord.available) {
                Bukkit.getScheduler().runTask(SneakyMail.getInstance(), Runnable {
                    val event = NewMailEvent(mailRecord)
                    Bukkit.getPluginManager().callEvent(event)
                })
            }
        }
    }
    private fun callbackDelete(record: Record) {
        val mailRecord = record as MailRecord
        SneakyMail.mail.remove(mailRecord.id!!)
    }


    companion object {
        fun init() {
            // Pocketbase.init("localhost", 27017, "sneakymail")
        }
    }
}