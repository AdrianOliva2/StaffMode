package org.staffmode.mc.staffMode.commands

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.entity.Player
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import org.staffmode.mc.staffMode.StaffMode
import org.staffmode.mc.staffMode.helper.MessageHelper
import org.staffmode.mc.staffMode.manager.DataManager
import java.util.*

class StaffModeCommand private constructor(private val plugin: StaffMode) : CommandExecutor {
    private val playerInventoryContentMap: Map<String, Array<ItemStack>>?

    private val dataManager: DataManager? = DataManager.getInstance(plugin)

    val staffModeList: MutableList<UUID>?

    private val msgHelper: MessageHelper?
    private val config: FileConfiguration
    private val replaces: MutableMap<String, String>?

    init {
        playerInventoryContentMap = dataManager?.playerInventoryContentMap
        staffModeList = dataManager?.staffModeList?.toMutableList()
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
                        if (staffModeList == null || !staffModeList.contains(player.uniqueId)) {
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
                    if (staffModeList == null || !staffModeList.contains(sender.uniqueId)) {
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
                            if (staffModeList == null || !staffModeList.contains(playerStaff.uniqueId)) {
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
        if (dataManager != null && dataManager.frozedPlayers.contains(player.uniqueId.toString())) dataManager.frozedPlayers.remove(
            player.uniqueId.toString()
        )

        staffModeList?.add(player.uniqueId)
        val playerInventory: Inventory = player.inventory
        val playerInventoryContents = player.inventory.contents.clone()
        val staffModePlayersFile = dataManager?.staffModePlayers
        staffModePlayersFile!!["Staffs." + player.uniqueId.toString() + ".activated"] = true
        staffModePlayersFile["Staffs." + player.uniqueId.toString() + ".inventory"] = playerInventoryContents
        dataManager?.saveStaffModePlayers()
        playerInventoryContentMap?.plus(player.uniqueId.toString() to playerInventoryContents)
        playerInventory.clear()
        player.isInvulnerable = true
        player.foodLevel = 20
        player.health = 20.0

        //Item vanish
        var material = config.getString("Item.vanish.type")!!.uppercase(Locale.getDefault())
        var item = ItemStack(Material.getMaterial(material)!!, 1)
        var itemMeta = item.itemMeta
        var displayName = config.getString("Item.vanish.display-name")
        if (displayName != null) itemMeta.displayName(
            LegacyComponentSerializer.legacyAmpersand().deserialize(displayName)
        )
        var loreList = config.getStringList("Item.vanish.lore")
        val loreComponents: MutableList<Component> = mutableListOf()
        if (loreList.size > 0) {
            for (i in loreList.indices) {
                loreComponents.add(LegacyComponentSerializer.legacyAmpersand().deserialize(loreList[i]))
            }
        }
        itemMeta.lore(loreComponents)
        item.setItemMeta(itemMeta)
        playerInventory.setItem(0, item)

        //Item rtp player
        material = config.getString("Item.player-rtp.type")!!.uppercase(Locale.getDefault())
        item = ItemStack(Material.getMaterial(material)!!, 1)
        itemMeta = item.itemMeta
        displayName = config.getString("Item.player-rtp.display-name")
        if (displayName != null) itemMeta.displayName(
            LegacyComponentSerializer.legacyAmpersand().deserialize(displayName)
        )
        loreList = config.getStringList("Item.player-rtp.lore")
        loreComponents.clear()
        if (loreList.isNotEmpty()) {
            for (i in loreList.indices) {
                loreComponents.add(LegacyComponentSerializer.legacyAmpersand().deserialize(loreList[i]))
            }
        }
        itemMeta.lore(loreComponents)
        item.setItemMeta(itemMeta)
        playerInventory.setItem(1, item)

        //Item froze player
        material = config.getString("Item.player-freeze.type")!!.uppercase(Locale.getDefault())
        item = ItemStack(Material.getMaterial(material)!!, 1)
        itemMeta = item.itemMeta
        displayName = config.getString("Item.player-freeze.display-name")
        if (displayName != null) itemMeta.displayName(
            LegacyComponentSerializer.legacyAmpersand().deserialize(displayName)
        )
        loreList = config.getStringList("Item.player-freeze.lore")
        if (loreList.size > 0) {
            for (i in loreList.indices) {
                loreComponents.add(LegacyComponentSerializer.legacyAmpersand().deserialize(loreList[i]))
            }
        }
        itemMeta.lore(loreComponents)
        item.setItemMeta(itemMeta)
        playerInventory.setItem(2, item)

        //Item X-Ray Finder
        material = config.getString("Item.x-ray-finder.type")!!.uppercase(Locale.getDefault())
        item = ItemStack(Material.getMaterial(material)!!, 1)
        itemMeta = item.itemMeta
        displayName = config.getString("Item.x-ray-finder.display-name")
        if (displayName != null) itemMeta.displayName(
            LegacyComponentSerializer.legacyAmpersand().deserialize(displayName)
        )
        loreList = config.getStringList("Item.x-ray-finder.lore")
        if (loreList.isNotEmpty()) {
            for (i in loreList.indices) {
                loreComponents.add(LegacyComponentSerializer.legacyAmpersand().deserialize(loreList[i]))
            }
        }
        itemMeta.lore(loreComponents)
        item.setItemMeta(itemMeta)
        playerInventory.setItem(3, item)

        //Item deactivate staffmode
        material = config.getString("Item.deactivate-staffmode.type")!!.uppercase(Locale.getDefault())
        item = ItemStack(Material.getMaterial(material)!!, 1)
        itemMeta = item.itemMeta
        displayName = config.getString("Item.deactivate-staffmode.display-name")
        if (displayName != null) itemMeta.displayName(
            LegacyComponentSerializer.legacyAmpersand().deserialize(displayName)
        )
        loreList = config.getStringList("Item.deactivate-staffmode.lore")
        if (loreList.isNotEmpty()) {
            for (i in loreList.indices) {
                loreComponents.add(LegacyComponentSerializer.legacyAmpersand().deserialize(loreList[i]))
            }
        }
        itemMeta.lore(loreComponents)
        item.setItemMeta(itemMeta)
        playerInventory.setItem(8, item)
        msgHelper!!.playerSendMessage(player, "Message.player-enable-staffmode", replaces)
    }

    fun deactivateStaffMode(player: Player) {
        staffModeList?.remove(player.uniqueId)
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
