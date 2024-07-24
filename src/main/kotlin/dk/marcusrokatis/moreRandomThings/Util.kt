package dk.marcusrokatis.moreRandomThings

import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.block.BlockFace
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.Entity
import org.bukkit.entity.Item
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.Damageable
import org.bukkit.util.Vector
import kotlin.random.Random

class Util {

    val ADJACENT_FACES: Array<BlockFace> = arrayOf(
        BlockFace.DOWN, BlockFace.UP,
        BlockFace.WEST, BlockFace.EAST,
        BlockFace.NORTH, BlockFace.SOUTH
    )

    val PLANTS: Set<Material> = setOf(
        Material.WHEAT,
        Material.CARROTS,
        Material.POTATOES,
        Material.BEETROOTS,
        Material.NETHER_WART
    )

    val VALID_SAPLING_BLOCKS: Set<Material> = setOf(
        Material.GRASS_BLOCK,
        Material.PODZOL,
        Material.MYCELIUM,
        Material.DIRT,
        Material.COARSE_DIRT,
        Material.ROOTED_DIRT,
        Material.FARMLAND,
        Material.MUD,
        Material.MOSS_BLOCK
    )

    val RNG: Random = Random

    fun randInt(min: Int, max: Int): Int = RNG.nextInt(max - min) + min

    fun applyDamage(item: ItemStack, amount: Int = 1) {
        if (item.itemMeta !is Damageable) return
        val dmg: Damageable = item.itemMeta as Damageable
        for (i in 1..amount) {
            val chance: Double = 1 / (item.getEnchantmentLevel(Enchantment.UNBREAKING) + 1).toDouble()
            if (RNG.nextDouble() < chance) dmg.damage += 1
        }
        item.setItemMeta(dmg)
        if (dmg.damage >= item.type.maxDurability) item.amount = 0
    }

    fun getKey(key: String): NamespacedKey = NamespacedKey(MoreRandomThings().INSTANCE, key)

    fun newMagicMirror(): ItemStack {
        // @TODO Implement this
        return ItemStack(Material.IRON_INGOT)
    }

    fun isMagicMirror(stack: ItemStack): Boolean {
        // @TODO Implement this too
        return false
    }

    fun teleport(player: Player, dest: Location) {
        player.teleport(dest)
        player.fallDistance = 0f
        player.velocity = Vector(0f, .3f, 0f)
    }

    fun placeBlock(entity: Item) {
        if (!entity.itemStack.type.isBlock) return

        entity.world.getBlockAt(entity.location).type = entity.itemStack.type
    }

    fun String.isTypeOf(type: String, ignoreCase: Boolean = true, allowPlainForm: Boolean = false): Boolean = endsWith(if (allowPlainForm) type else "_$type", ignoreCase)

    fun Material.isTypeOf(type: String, ignoreCase: Boolean = true, allowPlainForm: Boolean = false): Boolean = name.isTypeOf(type, ignoreCase, allowPlainForm)

    fun Material.isSapling(): Boolean = isTypeOf("SAPLING") || this == Material.MANGROVE_PROPAGULE

    fun isOnSaplingBlock(entity: Entity): Boolean {
        val blockUnder: Location = entity.location.clone().add(0.0, -1.0, 0.0)
        return VALID_SAPLING_BLOCKS.contains(entity.world.getBlockAt(blockUnder).type)
    }
}