package org.staffmode.mc.staffMode.manager

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.EntityType
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.event.inventory.InventoryType
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemFlag
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.SkullMeta
import org.staffmode.mc.staffMode.StaffMode
import kotlin.math.floor

@Suppress("unchecked_cast")
class InventoryManager(private val plugin: StaffMode) : Listener {
    private val actualPlayersPage: MutableMap<Player, Int> = HashMap()

    private val config = plugin.config
    private var changed = false

    private fun createHeads(players: Collection<Player>): List<ItemStack> {
        val heads: MutableList<ItemStack> = ArrayList()
        for (p in players) {
            if (p.location.y <= 25 && !p.isOp && !p.hasPermission("staffmode.toogle") && !p.isDead) {
                val item = ItemStack(Material.PLAYER_HEAD, 1)
                val meta = item.itemMeta as SkullMeta
                meta.owningPlayer = p
                meta.displayName(p.name())
                val lore: MutableList<Component> = ArrayList()
                lore.add(LegacyComponentSerializer.legacyAmpersand().deserialize("&a&lMundo: &7 ${p.world.name}"))
                lore.add(LegacyComponentSerializer.legacyAmpersand().deserialize("&a&lX: &7 ${floor(p.location.x)}"))
                lore.add(LegacyComponentSerializer.legacyAmpersand().deserialize("&a&lY: &7 ${floor(p.location.y)}"))
                lore.add(LegacyComponentSerializer.legacyAmpersand().deserialize("&a&lZ: &7 ${floor(p.location.z)}"))
                meta.lore(lore)
                item.setItemMeta(meta)
                heads.add(item)
            }
        }
        return heads
    }

    private fun fillBorders(inventory: Inventory) {
        val material = config.getString("X-Ray-Finder-Menu.item.borderItem.type")!!
        val borderItem = ItemStack(Material.getMaterial(material)!!)
        val meta = borderItem.itemMeta
        val displayName = config.getString("X-Ray-Finder-Menu.item.borderItem.display-name")!!
        meta.displayName(LegacyComponentSerializer.legacyAmpersand().deserialize(displayName))
        val lore = config.getStringList("X-Ray-Finder-Menu.item.borderItem.lore")
        val loreComponents: MutableList<Component> = mutableListOf()
        lore.forEach {
            if (it.isNotEmpty()) {
                loreComponents.add(LegacyComponentSerializer.legacyAmpersand().deserialize(it))
            }
        }
        meta.lore(loreComponents)
        meta.addItemFlags(
            ItemFlag.HIDE_ATTRIBUTES,
            ItemFlag.HIDE_UNBREAKABLE,
            ItemFlag.HIDE_DESTROYS,
            ItemFlag.HIDE_DYE,
            ItemFlag.HIDE_ENCHANTS,
            ItemFlag.HIDE_PLACED_ON
        )
        borderItem.setItemMeta(meta)
        for (i in 0..8) inventory.setItem(i, borderItem)
        for (i in inventory.size - 9 until inventory.size) inventory.setItem(i, borderItem)
        val rows = inventory.size / 9
        for (i in 1 until rows) {
            inventory.setItem(((i * 9)), borderItem)
            inventory.setItem(((i * 9) + 8), borderItem)
        }
    }

    fun createInventory(player: Player): Inventory {
        var material = config.getString("X-Ray-Finder-Menu.item.previousPage.type")
        val previousPageItem = ItemStack(Material.getMaterial(material!!)!!)
        material = config.getString("X-Ray-Finder-Menu.item.nextPage.type")
        val nextPageItem = ItemStack(Material.getMaterial(material!!)!!)
        val players = Bukkit.getOnlinePlayers()
        val heads = createHeads(players)
        val inventoryTitle = config.getString("X-Ray-Finder-Menu.title")!!
        val inventory =
            Bukkit.createInventory(null, 54, LegacyComponentSerializer.legacyAmpersand().deserialize(inventoryTitle))
        val actualPlayerPage: Int?
        if (actualPlayersPage.containsKey(player)) actualPlayerPage = actualPlayersPage[player]
        else {
            actualPlayerPage = 1
            actualPlayersPage[player] = 1
        }
        player.closeInventory()
        fillBorders(inventory)
        val rows = inventory.size / 9
        val numItemsBorder = ((rows - 2) * 2) + (9 * 2)
        val maxSkullsPerPage = inventory.size - numItemsBorder
        val maxPages = if (heads.size % maxSkullsPerPage == 0) {
            if (heads.size / maxSkullsPerPage == 0) {
                1
            } else {
                heads.size / maxSkullsPerPage
            }
        } else {
            heads.size / maxSkullsPerPage + 1
        }
        material = config.getString("X-Ray-Finder-Menu.item.closeMenu.type")
        var item = ItemStack(Material.getMaterial(material!!)!!, 1)
        var meta = item.itemMeta
        var displayName = config.getString("X-Ray-Finder-Menu.item.closeMenu.display-name")!!
        meta.displayName(LegacyComponentSerializer.legacyAmpersand().deserialize(displayName))
        var lore = config.getList("X-Ray-Finder-Menu.item.closeMenu.lore") as List<String?>?
        var loreComponents: MutableList<Component?> = mutableListOf()
        lore?.forEach {
            if (!it.isNullOrEmpty()) {
                loreComponents.add(LegacyComponentSerializer.legacyAmpersand().deserialize(it))
            }
        }
        meta.lore(loreComponents)
        item.setItemMeta(meta)
        inventory.setItem((inventory.size - 1), item)
        if (actualPlayerPage!! < maxPages) {
            meta = nextPageItem.itemMeta
            displayName = config.getString("X-Ray-Finder-Menu.item.nextPage.display-name")!!
            meta.displayName(LegacyComponentSerializer.legacyAmpersand().deserialize(displayName))
            lore = config.getList("X-Ray-Finder-Menu.item.nextPage.lore") as List<String?>?
            loreComponents = mutableListOf()
            lore?.forEach {
                if (!it.isNullOrEmpty()) {
                    loreComponents.add(LegacyComponentSerializer.legacyAmpersand().deserialize(it))
                }
            }
            meta.lore(loreComponents)
            nextPageItem.setItemMeta(meta)
            inventory.setItem((inventory.size - 3), nextPageItem)
        }
        material = config.getString("X-Ray-Finder-Menu.item.actualPage.type")
        item = ItemStack(Material.getMaterial(material!!)!!, actualPlayerPage)
        meta = item.itemMeta
        displayName = config.getString("X-Ray-Finder-Menu.item.actualPage.display-name")!!
        displayName = displayName.replace("%actualPlayerPage%".toRegex(), actualPlayerPage.toString())
        displayName = displayName.replace("%maxPage%".toRegex(), maxPages.toString())
        meta.displayName(LegacyComponentSerializer.legacyAmpersand().deserialize(displayName))
        lore = config.getList("X-Ray-Finder-Menu.item.actualPage.lore") as List<String?>?
        loreComponents = mutableListOf()
        lore?.forEach {
            if (!it.isNullOrEmpty()) {
                loreComponents.add(LegacyComponentSerializer.legacyAmpersand().deserialize(it))
            }
        }
        meta.lore(loreComponents)
        item.setItemMeta(meta)
        inventory.setItem((inventory.size - 5), item)
        if (actualPlayerPage > 1) {
            meta = previousPageItem.itemMeta
            displayName = config.getString("X-Ray-Finder-Menu.item.previousPage.display-name")!!
            meta.displayName(LegacyComponentSerializer.legacyAmpersand().deserialize(displayName))
            lore = config.getList("X-Ray-Finder-Menu.item.previousPage.lore") as List<String?>?
            loreComponents = mutableListOf()
            lore?.forEach {
                if (!it.isNullOrEmpty()) {
                    loreComponents.add(LegacyComponentSerializer.legacyAmpersand().deserialize(it))
                }
            }
            meta.lore(loreComponents)
            previousPageItem.setItemMeta(meta)
            inventory.setItem((inventory.size - 7), previousPageItem)
        }

        var i = ((actualPlayerPage * maxSkullsPerPage) - maxSkullsPerPage)
        while (i < (actualPlayerPage * maxSkullsPerPage) && i < heads.size) {
            inventory.addItem(heads[i])
            i++
        }

        changed = false

        return inventory
    }

    @EventHandler
    fun onInventoryClick(event: InventoryClickEvent) {
        val item = event.currentItem
        val inventory = event.clickedInventory
        val playerClicked = event.whoClicked as Player
        if (item != null && item.type != Material.AIR && inventory!!.type != InventoryType.PLAYER) {
            val menuTitle = config.getString("X-Ray-Finder-Menu.title")!!
            if (event.view.title() == LegacyComponentSerializer.legacyAmpersand().deserialize(menuTitle)) {
                val materialCloseItem = config.getString("X-Ray-Finder-Menu.item.closeMenu.type")!!
                val materialBorderItems = config.getString("X-Ray-Finder-Menu.item.borderItem.type")!!
                val materialPreviousPage = config.getString("X-Ray-Finder-Menu.item.previousPage.type")!!
                val materialActualPage = config.getString("X-Ray-Finder-Menu.item.actualPage.type")!!
                val materialNextPage = config.getString("X-Ray-Finder-Menu.item.nextPage.type")!!
                if (item.type == Material.getMaterial(materialCloseItem) || item.type == Material.getMaterial(
                        materialBorderItems
                    ) || item.type == Material.getMaterial(
                        materialPreviousPage
                    ) || item.type == Material.getMaterial(materialActualPage) || item.type == Material.getMaterial(
                        materialNextPage
                    ) || item.type == Material.PLAYER_HEAD
                ) {
                    event.isCancelled = true
                    val meta = item.itemMeta
                    if (item.type == Material.getMaterial(materialPreviousPage) || item.type == Material.getMaterial(
                            materialNextPage
                        )
                    ) {
                        var actualPage = actualPlayersPage[playerClicked]
                        if (actualPage == null) {
                            actualPage = 1
                        }
                        actualPlayersPage.remove(playerClicked)
                        if (item.type == Material.valueOf(materialNextPage)) {
                            actualPlayersPage[playerClicked] = actualPage + 1
                        } else {
                            actualPlayersPage[playerClicked] = actualPage - 1
                        }
                        changed = true
                        playerClicked.openInventory(createInventory(playerClicked))
                        return
                    }
                    if (item.type == Material.PLAYER_HEAD) {
                        val displayName: Component? = meta.displayName()
                        if (displayName != null) {
                            val target = Bukkit.getPlayerExact(
                                LegacyComponentSerializer.legacyAmpersand().serialize(displayName)
                            )
                            if (target != null) {
                                playerClicked.teleport(target)
                            } else {
                                playerClicked.sendMessage(
                                    LegacyComponentSerializer.legacyAmpersand()
                                        .deserialize("${plugin.prefix} &cEl jugador se ha desconectado o no estaba conectado")
                                )
                            }
                        }
                        return
                    }
                    if (item.type == Material.getMaterial(materialCloseItem)) {
                        playerClicked.closeInventory()
                        if (playerClicked.inventory.contains(item)) playerClicked.inventory.remove(item)
                    }
                }
            }
        }
    }

    @EventHandler
    fun onCloseInventoy(event: InventoryCloseEvent) {
        if (event.player.type == EntityType.PLAYER) {
            val player = event.player as Player
            if (actualPlayersPage.containsKey(player)) {
                if (!changed) {
                    actualPlayersPage.remove(event.player)
                }
            }
        }
    }
}
