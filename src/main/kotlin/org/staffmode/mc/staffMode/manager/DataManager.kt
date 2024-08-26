package org.staffmode.mc.staffMode.manager

import com.google.common.base.Charsets
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.inventory.ItemStack
import org.staffmode.mc.staffMode.StaffMode
import java.io.File
import java.io.IOException
import java.io.InputStreamReader
import java.util.*
import java.util.logging.Level

class DataManager private constructor(private val plugin: StaffMode) {
    private var freezedPlayersFile: File? = null
    private var freezedPlayersFileConfiguration: FileConfiguration? = null
    private var staffModePlayersFile: File? = null
    private var staffModePlayersFileConfiguration: FileConfiguration? = null

    @JvmField
    var frozedPlayers: MutableList<String> = mutableListOf()

    @JvmField
    var staffModeList: MutableList<UUID> = mutableListOf()

    @JvmField
    val playerInventoryContentMap: MutableMap<String, Array<ItemStack>> = HashMap()

    fun reloadfreezedPlayers() {
        if (this.freezedPlayersFile == null) this.freezedPlayersFile = File(
            plugin.dataFolder, "freezedPlayers.yml"
        )
        this.freezedPlayersFileConfiguration = YamlConfiguration.loadConfiguration(
            freezedPlayersFile!!
        )
        val defStream = plugin.getResource("freezedPlayers.yml")
        if (defStream != null) {
            val defFreezedPlayers = YamlConfiguration.loadConfiguration(InputStreamReader(defStream, Charsets.UTF_8))
            (freezedPlayersFileConfiguration as YamlConfiguration).setDefaults(defFreezedPlayers)
        }
        (freezedPlayersFileConfiguration as YamlConfiguration).getConfigurationSection("FreezedPlayers")?.getKeys(false)
            ?.forEach { key ->
                val uuid = UUID.fromString(key)
                frozedPlayers.add(uuid.toString())
            }
    }

    val freezedPlayers: FileConfiguration?
        get() {
            if (this.freezedPlayersFileConfiguration == null) reloadfreezedPlayers()
            return this.freezedPlayersFileConfiguration
        }

    fun saveFreezedPlayers() {
        if (this.freezedPlayersFileConfiguration == null || this.freezedPlayersFile == null) return
        try {
            freezedPlayers!!.save(freezedPlayersFile!!)
            reloadfreezedPlayers()
        } catch (e: IOException) {
            plugin.logger.log(Level.SEVERE, "Could not save config to" + this.freezedPlayersFile, e)
        }
    }

    fun saveDefaultFreezedPlayers() {
        if (this.freezedPlayersFile == null) this.freezedPlayersFile = File(
            plugin.dataFolder, "freezedPlayers.yml"
        )
        if (!freezedPlayersFile!!.exists()) plugin.saveResource("freezedPlayers.yml", false)
    }

    fun reloadStaffModePlayers() {
        if (this.staffModePlayersFile == null) this.staffModePlayersFile = File(
            plugin.dataFolder, "staffModePlayers.yml"
        )
        this.staffModePlayersFileConfiguration = YamlConfiguration.loadConfiguration(
            staffModePlayersFile!!
        )
        val defStream = plugin.getResource("staffModePlayers.yml")
        if (defStream != null) {
            val defStaffModePlayers = YamlConfiguration.loadConfiguration(InputStreamReader(defStream, Charsets.UTF_8))
            (staffModePlayersFileConfiguration as YamlConfiguration).setDefaults(defStaffModePlayers)
        }
        (staffModePlayersFileConfiguration as YamlConfiguration).getConfigurationSection("Staffs")?.getKeys(false)
            ?.forEach { key ->
                val uuid = UUID.fromString(key)
                (staffModeList as MutableList<UUID>).add(uuid)
            }
        (staffModePlayersFileConfiguration as YamlConfiguration).getConfigurationSection("Staffs")?.getKeys(false)
            ?.forEach { key ->
                (staffModePlayersFileConfiguration as YamlConfiguration).getConfigurationSection("Staffs.$key")
                    ?.getKeys(false)?.forEach { _ ->
                    val uuid = UUID.fromString(key)
                    val itemList =
                        (staffModePlayersFileConfiguration as YamlConfiguration).getList("Staffs.$key.inventory") as List<ItemStack>?
                    if (itemList != null) playerInventoryContentMap[uuid.toString()] = itemList.toTypedArray()
                }
            }
    }

    val staffModePlayers: FileConfiguration?
        get() {
            if (this.staffModePlayersFileConfiguration == null) reloadStaffModePlayers()
            return this.staffModePlayersFileConfiguration
        }

    fun saveStaffModePlayers() {
        if (this.staffModePlayersFileConfiguration == null || this.staffModePlayersFile == null) return
        try {
            staffModePlayers!!.save(staffModePlayersFile!!)
            reloadStaffModePlayers()
        } catch (e: IOException) {
            plugin.logger.log(Level.SEVERE, "Could not save config to" + this.staffModePlayersFile, e)
        }
    }

    fun saveDefaultStaffModePlayers() {
        if (this.staffModePlayersFile == null) this.staffModePlayersFile = File(
            plugin.dataFolder, "staffModePlayers.yml"
        )
        if (!staffModePlayersFile!!.exists()) plugin.saveResource("staffModePlayers.yml", false)
    }

    fun setFrozedPlayers(players: MutableList<String>?) {
        if (players == null) return
        this.frozedPlayers = players
        this.freezedPlayers?.set("Players", players)
        saveFreezedPlayers()
        reloadfreezedPlayers()
    }

    companion object {
        private var instance: DataManager? = null

        @JvmStatic
        fun getInstance(plugin: StaffMode): DataManager? {
            if (instance == null) {
                instance = DataManager(plugin)
            }
            return instance
        }
    }
}
