package org.staffmode.mc.staffMode.helper

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import org.bukkit.Material
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import java.util.*

class StaffModeHelper {

    companion object {
        fun givePlayerStaffModeItems(player: Player, config: FileConfiguration) {
            val playerInventory = player.inventory
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
        }
    }

}