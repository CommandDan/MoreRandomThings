package dk.marcusrokatis.moreRandomThings.events

import dk.marcusrokatis.moreRandomThings.MoreRandomThings
import dk.marcusrokatis.moreRandomThings.Util
import dk.marcusrokatis.moreRandomThings.Util.Companion.isSapling
import dk.marcusrokatis.moreRandomThings.Util.Companion.startsWithTypeOf
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.block.BlockFace
import org.bukkit.block.PistonMoveReaction
import org.bukkit.entity.ElderGuardian
import org.bukkit.entity.Entity
import org.bukkit.entity.Guardian
import org.bukkit.entity.Item
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockFormEvent
import org.bukkit.event.block.BlockPistonExtendEvent
import org.bukkit.event.entity.*
import org.bukkit.event.inventory.InventoryPickupItemEvent
import org.bukkit.event.weather.LightningStrikeEvent
import org.bukkit.inventory.ItemStack
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.LongAdder

class GeneralEvents : Listener {

    @EventHandler
    fun onLightningStrike(event: LightningStrikeEvent) {
        if (!MoreRandomThings.configuration.renewableSponges) return
        event.lightning
            .getNearbyEntities(4.0, 4.0, 4.0) // Radius of 4 pulled from wiki for pigs, villagers, and mooshrooms
            .stream()
            .filter { e: Entity? -> e is Guardian }
            .map { e: Entity -> e as Guardian }
            .forEach { g: Guardian ->
                // Unfortunately no easy way to convert guardian into elder anymore, so have to manually copy data
                val elder: ElderGuardian = g.world.spawn(g.location, ElderGuardian::class.java, CreatureSpawnEvent.SpawnReason.LIGHTNING)
                // Main data that is noticeable.
                elder.customName(g.customName())
                elder.addPotionEffects(g.activePotionEffects)
                g.remove()
            }
    }

    @EventHandler
    fun onEntityChangeBlock(event: EntityChangeBlockEvent) {
        if (!MoreRandomThings.configuration.silverFishDropGravel) return
        val from: Material = event.block.type
        val to: Material = event.to

        if (from.startsWithTypeOf("INFESTED") && to.isAir) { // Silverfish drop gravel
            val stack = ItemStack(Material.GRAVEL)
            event.block.world.dropItem(event.block.location.toCenterLocation(), stack)
        }
    }

    @EventHandler
    fun onBlockForm(event: BlockFormEvent) {
        if (event.newState.type == Material.STONE
            && event.block.getRelative(BlockFace.DOWN).type == Material.ANDESITE
            ) { // Renewable Andesite
            if (!MoreRandomThings.configuration.renewableAndesite) return
            event.newState.type = Material.ANDESITE
            return
        }
        if (event.block.location.blockY < 0) { // Renewable Deepslate
            if (!MoreRandomThings.configuration.renewableDeepslate) return
            val newMaterial: Material = when (event.newState.type) {
                Material.COBBLESTONE -> Material.COBBLED_DEEPSLATE
                Material.STONE -> Material.DEEPSLATE
                else -> return
            }

            event.newState.type = newMaterial
        }
    }

    @EventHandler
    fun onPistonPush(event: BlockPistonExtendEvent) { // TODO: Implement retraction
        if (event.isCancelled) return

        if (!MoreRandomThings.configuration.movableAmethyst) return

        val blocksSet: Set<Block> = event.blocks.toSet()
        val dir: BlockFace = event.direction

        /* Handle Movable Amethyst */

        // Re-calculate push limit to account for amethyst
        val pushSize: Long = blocksSet
            .stream()
            .filter { f: Block -> f.pistonMoveReaction != PistonMoveReaction.BREAK || f.type == Material.BUDDING_AMETHYST }
            .count()
        if (pushSize > 12) {
            event.isCancelled = true
            return
        }

        // Check if the amethyst can be moved -- if not, let the event happen and "crush" the amethyst.
        for (block: Block in event.blocks) {
            val dest: Block = block.getRelative(dir)
            if (!blocksSet.contains(dest) && dest.pistonMoveReaction != PistonMoveReaction.BREAK && !dest.type.isAir) return
        }

        // Move the amethyst - FIXME: Breaks when multiple in row
        val blocks: List<Pair<Material, Block>> = event.blocks.stream().map { b: Block -> Pair(b.type, b) }.toList()
        Bukkit.getScheduler().runTaskLater(MoreRandomThings.INSTANCE, Runnable {
            for ((key, value) in blocks) {
                val dest: Block = value.getRelative(dir)
                if (key == Material.BUDDING_AMETHYST) dest.type = Material.BUDDING_AMETHYST
            }
        }, 1)
    }

    @EventHandler
    fun onItemDrop(event: ItemSpawnEvent) {
        if (event.isCancelled || !MoreRandomThings.configuration.autoSaplings) return

        val item: Item = event.entity
        if (!item.itemStack.type.isSapling) return
        val timer: IntArray = intArrayOf(0)
        timer[0] = Bukkit.getScheduler().scheduleSyncRepeatingTask(MoreRandomThings.INSTANCE, {
            if (Util.isOnSaplingBlock(item)) {
                val la = LongAdder()
                la.add(Util.randInt(5 * 20, 15 * 20).toLong())
                TO_BE_PLANTED[item.uniqueId] = la
                Bukkit.getScheduler().cancelTask(timer[0])
            }
        }, 0, 10)
        Bukkit.getScheduler().runTaskLater(MoreRandomThings.INSTANCE, Runnable { Bukkit.getScheduler().cancelTask(timer[0]) }, 20 * 10)
    }

    @EventHandler
    fun onItemPickup(event: ItemDespawnEvent) {
        TO_BE_PLANTED.remove(event.entity.uniqueId)
    }

    @EventHandler
    fun onItemPickup(event: EntityPickupItemEvent) {
        TO_BE_PLANTED.remove(event.item.uniqueId)
    }

    @EventHandler
    fun onItemPickup(event: InventoryPickupItemEvent) {
        TO_BE_PLANTED.remove(event.item.uniqueId)
    }

    companion object {

        @JvmStatic
        val TO_BE_PLANTED: MutableMap<UUID, LongAdder> = ConcurrentHashMap()
    }
}