package org.staffmode.mc.staffMode

import org.bukkit.Bukkit
import org.bukkit.plugin.java.JavaPlugin
import org.staffmode.mc.staffMode.commands.StaffModeCommand
import org.staffmode.mc.staffMode.events.PlayerEvents
import org.staffmode.mc.staffMode.helper.MessageHelper
import org.staffmode.mc.staffMode.manager.DataManager
import org.staffmode.mc.staffMode.manager.InventoryManager

@Suppress("unchecked_cast")
class StaffMode : JavaPlugin() {
    var prefix: String? = null
        private set
    private val dataManager: DataManager? = DataManager.getInstance(this)
    private val msgHelper: MessageHelper? = MessageHelper.getInstance(this)

    override fun onEnable() {
        this.saveDefaultConfig()
        dataManager?.saveDefaultFreezedPlayers()
        dataManager?.saveDefaultStaffModePlayers()

        val freezedPlayersFileConfiguration = dataManager?.freezedPlayers
        val frozedPlayersConfig = freezedPlayersFileConfiguration?.getStringList("Players")
        if (frozedPlayersConfig != null) dataManager?.frozedPlayerList = frozedPlayersConfig

        /*val staffModePlayersConfiguration = dataManager?.staffModePlayers
        staffModePlayersConfiguration?.getConfigurationSection("Staffs")?.getKeys(false)
            ?.forEach { uuidKey ->
                staffModePlayersConfiguration.getConfigurationSection("Staffs.$uuidKey")
                    ?.getKeys(false)?.forEach { _ ->
                        val uuid = UUID.fromString(uuidKey)
                        val activated = staffModePlayersConfiguration.getBoolean("Staffs.$uuidKey.activated")
                        if (activated && dataManager?.staffModeList?.contains(uuid) == false) {
                            dataManager.staffModeList.add(uuid)
                            val itemList =
                                staffModePlayersConfiguration.getList("Staffs.$uuidKey.inventory") as List<ItemStack>?
                            if (itemList != null) dataManager.playerInventoryContentMap[uuid.toString()] =
                                itemList.toTypedArray()
                        }
                    }
            }*/

        prefix = config.getString("Message.prefix")
        registerCommands()
        registerEvents()
        val replaces: MutableMap<String, String> = HashMap()
        replaces["%prefix%"] = prefix!!
        msgHelper?.consoleSendMessage("Message.enable-plugin", replaces)
    }

    private fun registerCommands() {
        getCommand("staffmode")!!.setExecutor(StaffModeCommand.getInstance(this))
    }

    private fun registerEvents() {
        val pluginManager = server.pluginManager
        pluginManager.registerEvents(PlayerEvents(this), this)
        pluginManager.registerEvents(InventoryManager(this), this)
    }

    override fun onDisable() {
        val frozedPlayers = dataManager?.frozedPlayerList
        val freezedPlayersFileConfiguration = dataManager?.freezedPlayers
        if (!frozedPlayers.isNullOrEmpty()) {
            val frozedPlayersConfig = freezedPlayersFileConfiguration?.getList("Players") as MutableList<String>
            for (uuid in frozedPlayers) {
                if (!frozedPlayersConfig.contains(uuid)) frozedPlayersConfig.add(uuid)
            }

            val players = Bukkit.getOnlinePlayers()
            for (p in players) {
                if (!frozedPlayers.contains(p.uniqueId.toString()) && frozedPlayersConfig.contains(p.uniqueId.toString())) frozedPlayersConfig.remove(
                    p.uniqueId.toString()
                )
            }
            freezedPlayersFileConfiguration.set("Players", frozedPlayersConfig)
            dataManager?.saveFreezedPlayers()
        } else {
            freezedPlayersFileConfiguration?.set("Players", ArrayList<String>())
            dataManager?.saveFreezedPlayers()
        }
        /* if (dataManager?.staffModeList?.isNotEmpty() == true) {
            val smc = StaffModeCommand.getInstance(this)
            var i = 0
            while (i < dataManager.staffModeList.size) {
                val uuid = dataManager.staffModeList[i]
                val player = Bukkit.getPlayer(uuid)
                i++
                if (player != null) {
                    smc?.deactivateStaffMode(player)
                    if ((player.openInventory.title() == LegacyComponentSerializer.legacyAmpersand()
                            .deserialize("&4&lX-Ray Finder"))
                    ) player.closeInventory()
                    i--
                }
            }
        }*/
        dataManager?.saveStaffModePlayers()
        val replaces: MutableMap<String, String> = HashMap()
        if (!prefix.isNullOrEmpty()) {
            replaces["%prefix%"] = prefix!!
        }
        msgHelper?.consoleSendMessage("Message.disable-plugin", replaces)
    }
}
