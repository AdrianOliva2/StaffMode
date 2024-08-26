package org.staffmode.mc.staffMode.helper

import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.staffmode.mc.staffMode.StaffMode

class MessageHelper private constructor(private val plugin: StaffMode) {
    fun playerSendMessage(player: Player?, path: String?, replaces: Map<String, String>?): Boolean {
        if (player == null) return false

        val config = plugin.config
        var message = config.getString(path!!)
        if (!message.isNullOrEmpty()) {
            if (!replaces.isNullOrEmpty()) {
                val iterator = replaces.keys.iterator()
                do {
                    val replaceKey = iterator.next()
                    message = message!!.replace(replaceKey, replaces[replaceKey]!!)
                } while (iterator.hasNext())
            }
            player.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(message!!))
            return true
        }
        return false
    }

    fun consoleSendMessage(path: String?, replaces: Map<String, String>?): Boolean {
        val config = plugin.config
        var message = config.getString(path!!)
        if (!message.isNullOrEmpty()) {
            if (!replaces.isNullOrEmpty()) {
                val iterator = replaces.keys.iterator()
                do {
                    val replaceKey = iterator.next()
                    message = message!!.replace(replaceKey, replaces[replaceKey]!!)
                } while (iterator.hasNext())
            }
            Bukkit.getConsoleSender().sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(message!!))
            return true
        }
        return false
    }

    companion object {
        private var instance: MessageHelper? = null

        @JvmStatic
        fun getInstance(plugin: StaffMode): MessageHelper? {
            if (instance == null) {
                instance = MessageHelper(plugin)
            }
            return instance
        }
    }
}
