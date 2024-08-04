package dk.marcusrokatis.moreRandomThings.events

import com.destroystokyo.paper.event.player.PlayerJumpEvent
import com.destroystokyo.paper.event.player.PlayerLaunchProjectileEvent
import dk.marcusrokatis.moreRandomThings.MoreRandomThings
import dk.marcusrokatis.moreRandomThings.Util
import dk.marcusrokatis.moreRandomThings.Util.Companion.endsWithTypeOf
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.block.Block
import org.bukkit.block.BlockFace
import org.bukkit.block.data.Ageable
import org.bukkit.entity.Entity
import org.bukkit.entity.Marker
import org.bukkit.entity.Player
import org.bukkit.event.Event
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.inventory.PrepareAnvilEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerTeleportEvent
import org.bukkit.event.player.PlayerToggleSneakEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.util.Vector
import java.util.*
import kotlin.math.max
import kotlin.math.min

class PlayerEvents : Listener {

    @EventHandler
    fun onPlayerSneak(event: PlayerToggleSneakEvent) {
        if (event.isSneaking) { // Twerk Bonemeal
            if (MoreRandomThings.configuration.twerkBonemeal) doBonemeal(event.player)
            if (MoreRandomThings.configuration.elevators) {
                val currentBlock: Block = event.player.location.world.getBlockAt(event.player.location)
                val under: Block = currentBlock.getRelative(BlockFace.DOWN)
                if (currentBlock.type == Material.HEAVY_WEIGHTED_PRESSURE_PLATE && under.type.endsWithTypeOf("WOOL")) {
                    val maxY: Int = -(max(event.player.world.minHeight, currentBlock.y - 21) - currentBlock.y)
                    for (y: Int in 2..maxY) {
                        val block: Block = currentBlock.getRelative(BlockFace.DOWN, y)
                        if (teleportByElevator(event.player, block, under)) return
                    }
                }
            }
        }
    }

    @EventHandler
    fun onPlayerJump(event: PlayerJumpEvent) {
        if (event.isCancelled || !MoreRandomThings.configuration.elevators) return
        val currentBlock: Block = event.from.block
        val under: Block = currentBlock.getRelative(BlockFace.DOWN)
        if (currentBlock.type == Material.HEAVY_WEIGHTED_PRESSURE_PLATE && under.type.endsWithTypeOf("WOOL")) {
            val maxY: Int = min(event.player.world.maxHeight, currentBlock.y + 20) - currentBlock.y
            for (y: Int in 0..maxY) {
                val block: Block = currentBlock.getRelative(BlockFace.UP, y)
                if (teleportByElevator(event.player, block, under)) return
            }
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
    fun onBlockPlace(event: BlockPlaceEvent) {
        if (!MoreRandomThings.configuration.vacuumHoppers) return
        val l: Location = event.block.location.add(.0, -1.0, .0)
        val hopperBlock: Block = l.world.getBlockAt(l)
        if (hopperBlock.type != Material.HOPPER || event.blockPlaced.type != Material.PURPLE_CARPET) return

        // Create vacuum hopper
        val e: Marker = l.world.spawn(l.add(.5, 1.0, .5), Marker::class.java)
        e.setGravity(false)
        MoreRandomThings.getDataHandler().data.vacuumHoppers.add(e.uniqueId)
    }

    @EventHandler
    fun onBlockBreak(event: BlockBreakEvent) {
        if (!MoreRandomThings.configuration.vacuumHoppers) return
        if (event.block.type == Material.HOPPER || event.block.type == Material.PURPLE_CARPET) {
            val asLoc: Location = event.block.location.add(.5, .0, .5)
            if (event.block.type == Material.HOPPER) asLoc.add(.0, 1.0, .0)

            // Remove vacuum hopper
            val stand: Entity? = MoreRandomThings.getDataHandler().data.vacuumHoppers
                .stream()
                .map { uuid: UUID? -> Bukkit.getEntity(uuid!!) }
                .filter { e -> e != null && e.location.distanceSquared(asLoc) < .2 }
                .findFirst()
                .orElse(null)

            if (stand == null) return

            stand.remove()
            MoreRandomThings.getDataHandler().data.vacuumHoppers.remove(stand.uniqueId)
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

        @JvmStatic
        fun teleportByElevator(player: Player, checkBlock: Block, elevatorColor: Block): Boolean {
            if (checkBlock.type == elevatorColor.type
                && checkBlock.getRelative(BlockFace.UP).type == Material.HEAVY_WEIGHTED_PRESSURE_PLATE
                && !checkBlock.getRelative(BlockFace.UP, 2).type.isCollidable
            ) {
                val dest: Location = checkBlock.getRelative(BlockFace.UP).location.toCenterLocation()
                val playerLoc: Location = player.location
                dest.yaw = playerLoc.yaw
                dest.pitch = playerLoc.pitch

                player.teleport(dest, PlayerTeleportEvent.TeleportCause.PLUGIN)
                player.playSound(
                    dest,
                    Sound.ENTITY_ENDER_DRAGON_SHOOT,
                    1F,
                    2F
                )
                Bukkit.getScheduler().runTaskLater(MoreRandomThings.INSTANCE, Runnable { player.velocity = Vector(.0, -.3, .0) }, 1)
                return true
            }
            return false
        }
    }
}