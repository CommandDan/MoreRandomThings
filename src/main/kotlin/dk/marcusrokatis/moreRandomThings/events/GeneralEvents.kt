package dk.marcusrokatis.moreRandomThings.events

import dk.marcusrokatis.moreRandomThings.MoreRandomThings
import dk.marcusrokatis.moreRandomThings.Util.Companion.startsWithTypeOf
import org.bukkit.Material
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityChangeBlockEvent
import org.bukkit.inventory.ItemStack

class GeneralEvents : Listener {

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
}