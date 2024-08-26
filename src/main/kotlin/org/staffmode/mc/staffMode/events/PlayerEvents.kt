package org.staffmode.mc.staffMode.events

import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.EntityType
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.enchantment.EnchantItemEvent
import org.bukkit.event.enchantment.PrepareItemEnchantEvent
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.entity.EntityPickupItemEvent
import org.bukkit.event.entity.FoodLevelChangeEvent
import org.bukkit.event.inventory.*
import org.bukkit.event.player.*
import org.bukkit.inventory.EquipmentSlot
import org.staffmode.mc.staffMode.StaffMode
import org.staffmode.mc.staffMode.commands.StaffModeCommand
import org.staffmode.mc.staffMode.helper.MessageHelper
import org.staffmode.mc.staffMode.manager.DataManager
import org.staffmode.mc.staffMode.manager.InventoryManager
import java.util.*

@Suppress("unchecked_cast")
class PlayerEvents(private val plugin: StaffMode) : Listener {
    private val dataManager = DataManager.getInstance(plugin)

    private val frozedPlayers: MutableList<String>? = dataManager?.frozedPlayers?.toMutableList()

    private val smc: StaffModeCommand? = StaffModeCommand.getInstance(plugin)

    private val staffModeList: List<UUID>? = smc?.staffModeList

    private val config = plugin.config
    private val msgHelper = MessageHelper.getInstance(plugin)
    private val replaces: MutableMap<String, String> = HashMap()

    init {
        if (plugin.prefix != null) replaces["%prefix%"] = plugin.prefix!!
    }

    private fun toggleVanish(player: Player) {
        replaces["%player%"] = player.name
        if (!player.isInvisible) {
            player.isInvisible = true
            val players = Bukkit.getOnlinePlayers()
            if (players.isNotEmpty()) {
                for (p in players) {
                    val permissionToggle = config.getString("Permission.toogle")
                    if (permissionToggle == null || !p.hasPermission(permissionToggle) && !p.isOp) {
                        msgHelper?.playerSendMessage(p, "Message.player-fake-quit", replaces)
                        p.hidePlayer(plugin, player)
                    }
                }
            }
            msgHelper?.playerSendMessage(player, "Message.player-enable-vanish", replaces)
        } else {
            player.isInvisible = false
            val players = Bukkit.getOnlinePlayers()
            if (players.isNotEmpty()) {
                for (p in players) {
                    val permissionToggle = config.getString("Permission.toogle")
                    if (permissionToggle == null || !p.hasPermission(permissionToggle) && !p.isOp) {
                        msgHelper?.playerSendMessage(p, "Message.player-fake-join", replaces)
                        p.showPlayer(plugin, player)
                    }
                }
            }
            msgHelper?.playerSendMessage(player, "Message.player-disable-vanish", replaces)
        }
        replaces.remove("%player%")
    }

    @EventHandler
    fun onPlayerMove(event: PlayerMoveEvent) {
        val player = event.player
        if (frozedPlayers != null && frozedPlayers.contains(player.uniqueId.toString())) {
            event.isCancelled = true
        }
    }

    @EventHandler
    fun onClickInventory(event: InventoryClickEvent) {
        val inventory = event.clickedInventory
        if (inventory != null && inventory.type == InventoryType.PLAYER) {
            val player = inventory.holder as Player?
            if (player != null && (staffModeList != null && staffModeList.contains(player.uniqueId) || (frozedPlayers != null && frozedPlayers.contains(
                    player.uniqueId.toString()
                ))
                        )
            ) {
                event.isCancelled = true
            }
        }
    }

    @EventHandler
    fun onChangeItemHand(event: PlayerSwapHandItemsEvent) {
        val player = event.player
        if (staffModeList != null && staffModeList.contains(player.uniqueId) || (frozedPlayers != null && frozedPlayers.contains(
                player.uniqueId.toString()
            ))
        ) {
            event.isCancelled = true
        }
    }

    @EventHandler
    fun onBreakBlock(event: BlockBreakEvent) {
        val player = event.player
        if (staffModeList != null && staffModeList.contains(player.uniqueId) || (frozedPlayers != null && frozedPlayers.contains(
                player.uniqueId.toString()
            ))
        ) {
            event.isCancelled = true
            val material = config.getString("Item.x-ray-finder.type")!!
            if (player.inventory.itemInMainHand.type == Material.getMaterial(material)) {
                val inventoryManager = InventoryManager(plugin)
                val inventory = inventoryManager.createInventory(player)
                player.openInventory(inventory)
            }
        }
    }

    @EventHandler
    fun onPickupItem(event: EntityPickupItemEvent) {
        if (event.entity.type != EntityType.PLAYER) return
        val player = event.entity as Player
        if (staffModeList != null && staffModeList.contains(player.uniqueId) || (frozedPlayers != null && frozedPlayers.contains(
                player.uniqueId.toString()
            ))
        ) {
            event.isCancelled = true
        }
    }

    @EventHandler
    fun onPickupArrow(event: PlayerPickupArrowEvent) {
        val player = event.player
        if (staffModeList != null && staffModeList.contains(player.uniqueId) || (frozedPlayers != null && frozedPlayers.contains(
                player.uniqueId.toString()
            ))
        ) {
            event.isCancelled = true
        }
    }

    @EventHandler
    fun onQuit(event: PlayerQuitEvent) {
        val player = event.player
        val freezedPlayersFileConfiguration = dataManager!!.freezedPlayers
        var users = freezedPlayersFileConfiguration!!.getList("Players") as MutableList<String?>?
        if (frozedPlayers != null && frozedPlayers.contains(player.uniqueId.toString())) {
            if (users == null) users = ArrayList()

            if (!users.contains(player.uniqueId.toString())) users.add(player.uniqueId.toString())

            val players = Bukkit.getOnlinePlayers()
            for (p in players) {
                if (p.hasPermission(config.getString("Permission.toogle")!!) || p.isOp) {
                    replaces["%player%"] = player.name
                    msgHelper!!.playerSendMessage(p, "Message.player-quit-while-are-freezed", replaces)
                    replaces.remove("%player%")
                }
            }
        } else {
            if (users != null && users.contains(player.uniqueId.toString())) users.remove(player.uniqueId.toString())
        }

        if (!staffModeList.isNullOrEmpty()) {
            if (staffModeList.contains(player.uniqueId)) {
                smc?.deactivateStaffMode(player)
            }
        }

        freezedPlayersFileConfiguration["Players"] = users
        dataManager.saveFreezedPlayers()
    }

    @EventHandler
    fun onEnter(event: PlayerJoinEvent) {
        val player = event.player
        val freezedPlayersFileConfiguration = dataManager!!.freezedPlayers
        dataManager.staffModePlayers
        val users = freezedPlayersFileConfiguration!!.getList("Players") as List<String>?
        if (users != null && users.contains(player.uniqueId.toString())) {
            if (frozedPlayers != null && !frozedPlayers.contains(player.uniqueId.toString())) frozedPlayers.add(player.uniqueId.toString())

            val players = Bukkit.getOnlinePlayers()
            for (p in players) {
                if (p.hasPermission(config.getString("Permission.toogle")!!) || p.isOp) {
                    replaces["%player%"] = player.name
                    msgHelper!!.playerSendMessage(p, "Message.player-join-while-are-freezed", replaces)
                    replaces.remove("%player%")
                }
            }
        }
    }

    @EventHandler
    fun onBlockPlace(event: BlockPlaceEvent) {
        val player = event.player
        val block = event.block
        if (staffModeList != null && staffModeList.contains(player.uniqueId) || (frozedPlayers != null && frozedPlayers.contains(
                player.uniqueId.toString()
            ))
        ) {
            event.isCancelled = true
            val materialVanish = config.getString("Item.vanish.type")!!
            val materialStaffMode = config.getString("Item.deactivate-staffmode.type")!!
            if (block.type == Material.getMaterial(materialVanish)) //toggleVanish(player);
                player.chat("/vanish")
            else if (block.type == Material.getMaterial(materialStaffMode)) smc?.deactivateStaffMode(player)
        }
    }

    @EventHandler
    fun onDropItem(event: PlayerDropItemEvent) {
        val player = event.player
        if (staffModeList != null && staffModeList.contains(player.uniqueId) || (frozedPlayers != null && frozedPlayers.contains(
                player.uniqueId.toString()
            ))
        ) {
            event.isCancelled = true
        }
    }

    @EventHandler
    fun onInteractPlayer(event: PlayerInteractAtEntityEvent) {
        val player = event.player
        if (frozedPlayers != null && frozedPlayers.contains(player.uniqueId.toString())) {
            event.isCancelled = true
        } else {
            if (event.hand == EquipmentSlot.HAND) {
                val entity = event.rightClicked
                if (entity.type == EntityType.PLAYER) {
                    event.player.inventory.itemInMainHand
                    config.getString("Item.player-freeze.type")!!
                    if (staffModeList != null && staffModeList.contains(player.uniqueId) || (frozedPlayers != null && frozedPlayers.contains(
                            player.uniqueId.toString()
                        ))
                    ) {
                        val target = entity as Player
                        event.isCancelled = true
                        if (target.hasPermission(config.getString("Permission.toogle")!!) || target.isOp) {
                            msgHelper!!.playerSendMessage(player, "Message.cant-freeze-staff", replaces)
                        } else if (!target.isOnline) {
                            msgHelper!!.playerSendMessage(player, "Message.cant-freeze-offline-player", replaces)
                        } else {
                            if (frozedPlayers != null && !frozedPlayers.contains(target.uniqueId.toString())) {
                                replaces["%target%"] = target.name
                                msgHelper!!.playerSendMessage(player, "Message.player-freeze-target", replaces)
                                replaces.remove("%target%")
                                frozedPlayers.add(target.uniqueId.toString())
                                msgHelper.playerSendMessage(target, "Message.player-been-freezed", replaces)
                            } else {
                                replaces["%target%"] = target.name
                                msgHelper!!.playerSendMessage(player, "Message.player-unfreeze-target", replaces)
                                replaces.remove("%target%")
                                frozedPlayers?.remove(target.uniqueId.toString())
                                msgHelper.playerSendMessage(target, "Message.player-been-unfreezed", replaces)
                            }
                            dataManager?.setFrozedPlayers(frozedPlayers)
                        }
                    }
                }
            }
        }
    }

    @EventHandler
    fun onUseItem(event: PlayerInteractEvent) {
        val player = event.player
        if (frozedPlayers != null && frozedPlayers.contains(player.uniqueId.toString())) {
            event.isCancelled = true
            return
        } else {
            val item = event.item
            if (staffModeList != null && staffModeList.contains(player.uniqueId) && event.action == Action.RIGHT_CLICK_AIR) {
                if (item != null) {
                    val materialVanish = config.getString("Item.vanish.type")!!
                    val materialRtp = config.getString("Item.player-rtp.type")!!
                    val materialXRay = config.getString("Item.x-ray-finder.type")!!
                    val materialStaffMode = config.getString("Item.deactivate-staffmode.type")!!
                    if (item.type == Material.getMaterial(materialVanish)) {
                        event.isCancelled = true
                        toggleVanish(player)
                    } else if (item.type == Material.getMaterial(materialRtp)) {
                        val players: MutableList<Player> = Bukkit.getOnlinePlayers().toMutableList()
                        for (i in 0..players.size) {
                            val p = players.elementAtOrNull(i)
                            if (p != null && staffModeList.contains(p.uniqueId)) {
                                players.remove(p)
                            }
                        }
                        if (players.isNotEmpty()) {
                            val rnd = Random()
                            val rndNum = rnd.nextInt(players.size)
                            val target = players.elementAt(rndNum)
                            player.teleport(target)
                        } else {
                            msgHelper!!.playerSendMessage(player, "Message.no-players-for-rtp", replaces)
                        }
                    } else if (item.type == Material.getMaterial(materialXRay)) {
                        event.isCancelled = true
                        val inventoryManager = InventoryManager(plugin)
                        val inventory = inventoryManager.createInventory(player)
                        player.openInventory(inventory)
                    } else if (item.type == Material.getMaterial(materialStaffMode)) {
                        event.isCancelled = true
                        smc?.deactivateStaffMode(player)
                    }
                }
            }
        }
    }

    @EventHandler
    fun onDamageItem(event: PlayerItemDamageEvent) {
        val player = event.player
        if (staffModeList != null && staffModeList.contains(player.uniqueId)) {
            event.isCancelled = true
        }
    }

    @EventHandler
    fun onDamageEntity(event: EntityDamageByEntityEvent) {
        if (event.damager.type == EntityType.PLAYER) {
            val player = event.damager as Player
            if (staffModeList != null && staffModeList.contains(player.uniqueId)) {
                event.isCancelled = true
            }
        }
    }

    @EventHandler
    fun onFoodLevelChange(event: FoodLevelChangeEvent) {
        if (event.entity.type == EntityType.PLAYER) {
            val player = event.entity as Player
            if ((dataManager != null && dataManager.staffModeList.contains(player.uniqueId)) || (frozedPlayers != null && frozedPlayers.contains(
                    player.uniqueId.toString()
                ))
            ) {
                event.isCancelled = true
            }
        }
    }

    @EventHandler
    fun onCraftItem(event: PrepareItemCraftEvent) {
        val player = event.view.player
        if (staffModeList != null && staffModeList.contains(player.uniqueId) || (frozedPlayers != null && frozedPlayers.contains(
                player.uniqueId.toString()
            ))
        ) {
            player.closeInventory();
        }
    }

    @EventHandler
    fun onEnchantItem(event: EnchantItemEvent) {
        val player = event.enchanter
        if (staffModeList != null && staffModeList.contains(player.uniqueId) || (frozedPlayers != null && frozedPlayers.contains(
                player.uniqueId.toString()
            ))
        ) {
            event.isCancelled = true
        }
    }

    @EventHandler
    fun onAnvilUse(event: PrepareAnvilEvent) {
        val player = event.view.player
        if (staffModeList != null && staffModeList.contains(player.uniqueId) || (frozedPlayers != null && frozedPlayers.contains(
                player.uniqueId.toString()
            ))
        ) {
            event.result = null
        }
    }

    @EventHandler
    fun onPrepareEnchant(event: PrepareItemEnchantEvent) {
        val player = event.enchanter
        if (staffModeList != null && staffModeList.contains(player.uniqueId) || (frozedPlayers != null && frozedPlayers.contains(
                player.uniqueId.toString()
            ))
        ) {
            event.isCancelled = true
        }
    }

    @EventHandler
    fun onPrepareSmithing(event: PrepareSmithingEvent) {
        val player = event.view.player
        if (staffModeList != null && staffModeList.contains(player.uniqueId) || (frozedPlayers != null && frozedPlayers.contains(
                player.uniqueId.toString()
            ))
        ) {
            event.result = null
        }
    }

    @EventHandler
    fun onDamagedEntity(event: EntityDamageEvent) {
        if (event.entity.type == EntityType.PLAYER) {
            val player = event.entity as Player
            if (staffModeList != null && staffModeList.contains(player.uniqueId) || (frozedPlayers != null && frozedPlayers.contains(
                    player.uniqueId.toString()
                ))
            ) {
                event.isCancelled = true
            }
        }
    }

    @EventHandler
    fun onEntityDamageEntity(event: EntityDamageByEntityEvent) {
        if (event.damager.type == EntityType.PLAYER) {
            val damager = event.damager as Player
            if (staffModeList != null && staffModeList.contains(damager.uniqueId) || (frozedPlayers != null && frozedPlayers.contains(
                    damager.uniqueId.toString()
                ))
            ) {
                event.isCancelled = true
            }
        }
    }
}
