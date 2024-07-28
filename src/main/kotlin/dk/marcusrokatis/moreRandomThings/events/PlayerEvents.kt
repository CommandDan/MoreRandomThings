package dk.marcusrokatis.moreRandomThings.events

import dk.marcusrokatis.moreRandomThings.MoreRandomThings
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.block.BlockFace
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerToggleSneakEvent

class PlayerEvents : Listener {

    @EventHandler
    fun onPlayerSneak(event: PlayerToggleSneakEvent) {
        if (event.isSneaking) { // Twerk Bonemeal
            if (MoreRandomThings.configuration.twerkBonemeal) doBonemeal(event.player)
        }
    }

    companion object {

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