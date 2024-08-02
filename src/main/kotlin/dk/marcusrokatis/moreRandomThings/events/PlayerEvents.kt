package dk.marcusrokatis.moreRandomThings.events

import com.destroystokyo.paper.event.player.PlayerLaunchProjectileEvent
import dk.marcusrokatis.moreRandomThings.MoreRandomThings
import dk.marcusrokatis.moreRandomThings.Util
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.block.BlockFace
import org.bukkit.block.data.Ageable
import org.bukkit.entity.Player
import org.bukkit.event.Event
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.PrepareAnvilEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerToggleSneakEvent
import org.bukkit.inventory.ItemStack

class PlayerEvents : Listener {

    @EventHandler
    fun onPlayerSneak(event: PlayerToggleSneakEvent) {
        if (event.isSneaking) { // Twerk Bonemeal
            if (MoreRandomThings.configuration.twerkBonemeal) doBonemeal(event.player)
        }
    }

    @EventHandler
    fun onPlayerInteract(event: PlayerInteractEvent) {
        if (!MoreRandomThings.configuration.rightClickHarvestCrops) return
        if (event.action.isRightClick && event.hasBlock()) { // Right-Click Harvest Crops
            val clicked: Block = event.clickedBlock ?: return

            val type: Material = clicked.type

            if (Util.PLANTS.contains(type)) {

                val data: Ageable = clicked.blockData as Ageable
                if (data.age != data.maximumAge) return

                event.setUseInteractedBlock(Event.Result.DENY)
                event.setUseItemInHand(Event.Result.DENY)

                clicked.breakNaturally(true) // Break it to have the particles and drop the items

                clicked.world.getBlockAt(clicked.location).type = type
                event.player.swingMainHand() // Swing the hand to make it look more natural
                event.isCancelled = true
            }
        }
    }

    @EventHandler
    fun onAnvilPrepare(event: PrepareAnvilEvent) {
        if (!MoreRandomThings.configuration.magicMirror) return

        val left: ItemStack = event.inventory.firstItem ?: return
        val right: ItemStack = event.inventory.secondItem ?: return

        if (left.type == Material.ENDER_PEARL
            && right.type == Material.COMPASS
            && left.amount == right.amount
            ) {
            val newResult: ItemStack = Util.newMagicMirror()
            newResult.amount = left.amount
            event.inventory.repairCost = left.amount * 16
            event.result = newResult
        }
    }

    @EventHandler
    fun onPlayerLaunchProjectile(event: PlayerLaunchProjectileEvent) {
        if (event.isCancelled || !MoreRandomThings.configuration.magicMirror) return

        if (Util.isMagicMirror(event.itemStack)) {
            event.player.sendActionBar(Component.text("Teleporting...", NamedTextColor.LIGHT_PURPLE))
            Bukkit.getScheduler().runTaskLater(MoreRandomThings.INSTANCE, event.projectile::remove, 1)
            event.setShouldConsume(true)
            Bukkit.getScheduler().runTaskLater(MoreRandomThings.INSTANCE, Runnable {
                val dest: Location = event.player.respawnLocation ?: event.player.world.spawnLocation
                Util.teleport(event.player, dest)
            }, 10)
        }
    }

    companion object {

        @JvmStatic
        fun doBonemeal(player: Player) {
            for (attempt: Int in 1..10) {
                var i = 0
                while (i < 10) {
                    ++i

                    val loc: Location = player.location.clone()
                    val radius = 2.5
                    loc.add(
                        Math.random() * radius * 2 - radius,
                        Math.random() * radius * 2 - radius,
                        Math.random() * radius * 2 - radius
                    )
                    val block: Block = player.world.getBlockAt(loc)

                    if (block.type != Material.GRASS_BLOCK) {
                        if (block.applyBoneMeal(BlockFace.UP)) i++
                    }
                }
            }
        }
    }
}