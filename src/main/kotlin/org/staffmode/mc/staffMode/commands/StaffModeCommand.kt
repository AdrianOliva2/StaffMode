package org.staffmode.mc.staffMode.commands

import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.staffmode.mc.staffMode.StaffMode
import org.staffmode.mc.staffMode.helper.MessageHelper
import org.staffmode.mc.staffMode.helper.StaffModeHelper
import org.staffmode.mc.staffMode.manager.DataManager

class StaffModeCommand private constructor(private val plugin: StaffMode) : CommandExecutor {
    private val playerInventoryContentMap: Map<String, Array<ItemStack>>?

    private val dataManager: DataManager? = DataManager.getInstance(plugin)

    private val msgHelper: MessageHelper?
    private val config: FileConfiguration
    private val replaces: MutableMap<String, String>?

    init {
        playerInventoryContentMap = dataManager?.playerInventoryContentMap
        msgHelper = MessageHelper.getInstance(plugin)
        config = plugin.config
        replaces = HashMap()
        if (!plugin.prefix.isNullOrEmpty()) {
            replaces["%prefix%"] = plugin.prefix!!
        }
    }

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<String>): Boolean {
        if (sender !is Player) {
            if (args.size == 1) {
                if (args[0] == "reload") {
                    dataManager!!.reloadfreezedPlayers()
                    dataManager.reloadStaffModePlayers()
                    plugin.reloadConfig()
                    dataManager.saveFreezedPlayers()
                    dataManager.saveStaffModePlayers()
                    plugin.saveConfig()
                    msgHelper!!.consoleSendMessage("Message.plugin-reloaded", replaces)
                } else {
                    val player = Bukkit.getPlayerExact(args[0])
                    if (player != null) {
                        if (dataManager?.staffModeList == null || !dataManager.staffModeList.contains(player.uniqueId)) {
                            replaces?.set("%target%", player.name)
                            msgHelper!!.consoleSendMessage("Message.activate-staffmode-to-player", replaces)
                            replaces?.remove("%target%")
                            activateStaffMode(player)
                        } else {
                            replaces?.set("%target%", player.name)
                            msgHelper!!.consoleSendMessage("Message.deactivate-staffmode-to-player", replaces)
                            replaces?.remove("%target%")
                            deactivateStaffMode(player)
                        }
                    } else {
                        replaces?.set("%target%", args[0])
                        msgHelper!!.consoleSendMessage("Message.user-is-offline", replaces)
                        replaces?.remove("%target%")
                    }
                }
            } else {
                msgHelper!!.consoleSendMessage("Messages.console-use-staffmode", replaces)
            }
        } else {
            if (sender.hasPermission(config.getString("Permission.toogle")!!) || sender.isOp) {
                if (args.isEmpty()) {
                    if (dataManager?.staffModeList == null || !dataManager.staffModeList.contains(sender.uniqueId)) {
                        activateStaffMode(sender)
                    } else {
                        deactivateStaffMode(sender)
                    }
                } else if (args.size == 1) {
                    if (args[0] == "reload") {
                        dataManager!!.reloadfreezedPlayers()
                        dataManager.reloadStaffModePlayers()
                        plugin.reloadConfig()
                        dataManager.saveFreezedPlayers()
                        dataManager.saveStaffModePlayers()
                        plugin.saveConfig()
                        msgHelper!!.playerSendMessage(sender, "Message.plugin-reloaded", replaces)
                    } else {
                        val playerStaff = Bukkit.getPlayerExact(args[0])
                        if (playerStaff != null) {
                            replaces?.set("%target%", sender.name)
                            if (dataManager?.staffModeList == null || !dataManager.staffModeList.contains(playerStaff.uniqueId)) {
                                msgHelper!!.consoleSendMessage("Message.activate-staffmode-to-player", replaces)
                                activateStaffMode(playerStaff)
                            } else {
                                msgHelper!!.consoleSendMessage("Message.deactivate-staffmode-to-player", replaces)
                                deactivateStaffMode(playerStaff)
                            }
                            replaces?.remove("%target%")
                        } else {
                            replaces?.set("%target%", args[0])
                            msgHelper!!.playerSendMessage(sender, "Message.user-is-offline", replaces)
                            replaces?.remove("%target%")
                        }
                    }
                } else {
                    msgHelper!!.playerSendMessage(sender, "Message.player-use-staffmode", replaces)
                }
            } else {
                msgHelper!!.playerSendMessage(sender, "Message.player-not-permission", replaces)
            }
        }
        return false
    }

    private fun activateStaffMode(player: Player) {
        if (dataManager != null && dataManager.frozedPlayerList.contains(player.uniqueId.toString())) dataManager.frozedPlayerList.remove(
            player.uniqueId.toString()
        )

        dataManager?.staffModeList?.add(player.uniqueId)
        val playerInventoryContents = player.inventory.contents.clone().map { it ?: ItemStack(Material.AIR) }
        val staffModePlayersFile = dataManager?.staffModePlayers
        staffModePlayersFile!!["Staffs." + player.uniqueId.toString() + ".activated"] = true
        staffModePlayersFile["Staffs." + player.uniqueId.toString() + ".inventory"] = playerInventoryContents
        dataManager?.saveStaffModePlayers()
        playerInventoryContentMap?.plus(player.uniqueId.toString() to playerInventoryContents)

        StaffModeHelper.givePlayerStaffModeItems(player, config)

        msgHelper!!.playerSendMessage(player, "Message.player-enable-staffmode", replaces)
    }

    fun deactivateStaffMode(player: Player) {
        dataManager?.staffModeList?.remove(player.uniqueId)
        val playerInventoryContent = playerInventoryContentMap?.get(player.uniqueId.toString())
        if (playerInventoryContent != null) {
            player.inventory.contents = playerInventoryContent
            playerInventoryContentMap?.minus(player.uniqueId.toString())
        }
        val staffModePlayersFile = dataManager!!.staffModePlayers
        staffModePlayersFile!!["Staffs." + player.uniqueId.toString() + ".activated"] = false
        staffModePlayersFile["Staffs." + player.uniqueId.toString() + ".inventory"] = null
        dataManager.saveStaffModePlayers()
        player.isInvulnerable = false
        val players = Bukkit.getOnlinePlayers()
        for (p in players) {
            p.showPlayer(plugin, player)
        }
        msgHelper!!.playerSendMessage(player, "Message.player-disable-staffmode", replaces)
    }

    companion object {
        private var instance: StaffModeCommand? = null

        fun getInstance(plugin: StaffMode): StaffModeCommand? {
            if (instance == null) {
                instance = StaffModeCommand(plugin)
            }
            return instance
        }
    }
}
