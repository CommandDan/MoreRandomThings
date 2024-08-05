package dk.marcusrokatis.moreRandomThings.events

import dk.marcusrokatis.moreRandomThings.MoreRandomThings
import dk.marcusrokatis.moreRandomThings.Util
import dk.marcusrokatis.moreRandomThings.Util.Companion.endsWithTypeOf
import io.papermc.paper.event.block.BlockBreakBlockEvent
import io.papermc.paper.event.block.BlockPreDispenseEvent
import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.block.BlockFace
import org.bukkit.block.Container
import org.bukkit.block.data.Directional
import org.bukkit.block.data.Levelled
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.inventory.ItemStack

class DispenserEvents : Listener {

    @EventHandler
    fun onPreDispense(event: BlockPreDispenseEvent) {
        if (event.isCancelled) return
        val item: ItemStack = event.itemStack // The ItemStack that gets used
        val facingDirection: BlockFace = (event.block.blockData as Directional).facing // The direction in which the dispenser is facing
        val facing: Block = event.block.getRelative(facingDirection) // The block that the dispenser is facing

        if (item.type.endsWithTypeOf("PICKAXE")
            || item.type.endsWithTypeOf("SHOVEL")
            || item.type.endsWithTypeOf("AXE")
            || item.type == Material.SHEARS
            ) { // Dispenser Block Breaker
            if (!MoreRandomThings.configuration.dispenserBreakBlocks) return
            event.isCancelled = true
            if (facing.type.hardness == -1F) return

            val bbbEvent = BlockBreakBlockEvent(facing, event.block, facing.getDrops(item).toList())
            if (!bbbEvent.callEvent()) return
            val broken: Boolean = facing.breakNaturally(event.itemStack, true)
            if (broken) Util.applyDamage(item)

        } else if (item.type.endsWithTypeOf("HOE")) { // Dispenser Till Block
            if (!MoreRandomThings.configuration.dispenserTillBlocks) return
            event.isCancelled = true
            val newType: Material = when (facing.type) {
                Material.DIRT, Material.GRASS_BLOCK -> Material.FARMLAND
                Material.COARSE_DIRT -> Material.DIRT
                else -> return
            }

            facing.type = newType
            Util.applyDamage(item)

        } else if (item.type.endsWithTypeOf("BUCKET", true)) { // Dispenser Cauldrons
            if (!MoreRandomThings.configuration.dispenserCauldrons) return

            if (!facing.type.endsWithTypeOf("CAULDRON", true)) return

            when (item.type) {
                Material.BUCKET, Material.WATER_BUCKET, Material.LAVA_BUCKET, Material.POWDER_SNOW_BUCKET -> event.isCancelled = true
                else -> return
            }

            val c: Container = event.block.state as Container
            if (item.type == Material.BUCKET && facing.type != Material.CAULDRON) { // Bucket from cauldron
                if (facing.blockData is Levelled) {
                    val l: Levelled = facing.blockData as Levelled
                    if (l.level != l.maximumLevel) return
                }

                item.amount -= 1
                val overflow: HashMap<Int, ItemStack> = c.inventory.addItem(ItemStack(when (facing.type) {
                    Material.WATER_CAULDRON -> Material.WATER_BUCKET
                    Material.LAVA_CAULDRON -> Material.LAVA_BUCKET
                    Material.POWDER_SNOW_CAULDRON -> Material.POWDER_SNOW_BUCKET
                    else -> Material.AIR
                }))
                facing.type = Material.CAULDRON

                if (overflow.isNotEmpty()) {
                    event.block.world.dropItemNaturally(
                        event.block.location.clone().add(facingDirection.direction),
                        ItemStack(overflow[0]!!.type)
                    )
                }

            } else if (item.type != Material.BUCKET) { // Bucket into cauldron
                // Always replaces, no matter content (just like when right-clicking with a bucket)
                facing.type = when (item.type) {
                    Material.WATER_BUCKET -> Material.WATER_CAULDRON
                    Material.LAVA_BUCKET -> Material.LAVA_CAULDRON
                    Material.POWDER_SNOW_BUCKET -> Material.POWDER_SNOW_CAULDRON
                    else -> Material.AIR
                }

                if (facing.blockData is Levelled) {
                    val l: Levelled = facing.blockData as Levelled
                    l.level = l.maximumLevel
                    facing.blockData = l
                }

                item.amount -= 1
                val overflow: HashMap<Int, ItemStack> = c.inventory.addItem(ItemStack(Material.BUCKET))

                if (overflow.isNotEmpty()) { // If the item can't fit into the inventory, drop it in world
                    event.block.world.dropItemNaturally(
                        event.block.location.clone().add(facingDirection.direction),
                        ItemStack(overflow[0]!!.type)
                    )
                }
            }
        }
    }
}