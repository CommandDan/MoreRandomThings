package dk.marcusrokatis.moreRandomThings

import com.google.gson.JsonArray
import com.google.gson.JsonParser
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.block.BlockFace
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.Entity
import org.bukkit.entity.Item
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemFlag
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.Damageable
import org.bukkit.inventory.meta.ItemMeta
import org.bukkit.persistence.PersistentDataContainer
import org.bukkit.persistence.PersistentDataType
import org.bukkit.util.Vector
import java.io.IOException
import java.io.InputStreamReader
import java.net.URI
import java.net.URL
import java.util.concurrent.CompletableFuture
import kotlin.random.Random

class Util {

    companion object {

        @JvmStatic
        val ADJACENT_FACES: Array<BlockFace> = arrayOf(
            BlockFace.DOWN, BlockFace.UP,
            BlockFace.WEST, BlockFace.EAST,
            BlockFace.NORTH, BlockFace.SOUTH
        )

        @JvmStatic
        val PLANTS: Set<Material> = setOf(
            Material.WHEAT,
            Material.CARROTS,
            Material.POTATOES,
            Material.BEETROOTS,
            Material.NETHER_WART
        )

        @JvmStatic
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

        @JvmStatic
        val RNG: Random = Random

        @JvmStatic
        fun randInt(min: Int, max: Int): Int = RNG.nextInt(max - min) + min

        @JvmStatic
        fun applyDamage(item: ItemStack, amount: Int = 1) {
            if (item.itemMeta !is Damageable) return
            val dmg: Damageable = item.itemMeta as Damageable
            repeat(amount) {
                val chance: Double = 1 / (item.getEnchantmentLevel(Enchantment.UNBREAKING) + 1).toDouble()
                if (RNG.nextDouble() < chance) dmg.damage += 1
            }
            item.setItemMeta(dmg)
            if (dmg.damage >= item.type.maxDurability) item.amount = 0
        }

        @JvmStatic
        fun getKey(key: String): NamespacedKey = NamespacedKey(MoreRandomThings.INSTANCE, key)

        @JvmStatic
        fun newMagicMirror(): ItemStack {
            val stack = ItemStack(Material.ENDER_PEARL)
            val meta: ItemMeta = stack.itemMeta

            meta.displayName(MiniMessage.miniMessage().deserialize("<bold><rainbow>Magic Mirror</rainbow></bold>"))
            meta.lore(listOf(
                Component.text("Throw this to return to your respawn location.", NamedTextColor.GRAY)
            ))
            meta.persistentDataContainer[getKey("magic_mirror"), PersistentDataType.BYTE] = 1

            stack.setItemMeta(meta)

            stack.addUnsafeEnchantment(Enchantment.POWER, 1)
            stack.addItemFlags(ItemFlag.HIDE_ENCHANTS)

            return stack
        }

        @JvmStatic
        fun isMagicMirror(stack: ItemStack): Boolean {
            val pdc: PersistentDataContainer = stack.itemMeta.persistentDataContainer
            val magicMirror: Byte = pdc.getOrDefault(getKey("magic_mirror"), PersistentDataType.BYTE, 0)
            return magicMirror == 1.toByte()
        }

        @JvmStatic
        fun teleport(player: Player, dest: Location) {
            player.teleport(dest)
            player.fallDistance = 0f
            player.velocity = Vector(0f, .3f, 0f)
        }

        @JvmStatic
        fun placeBlock(entity: Item) {
            if (!entity.itemStack.type.isBlock) return

            entity.world.getBlockAt(entity.location).type = entity.itemStack.type
        }

        @JvmStatic
        fun String.endsWithTypeOf(type: String, allowPlainForm: Boolean = false, ignoreCase: Boolean = true): Boolean = endsWith(if (allowPlainForm) type else "_$type", ignoreCase)

        @JvmStatic
        fun Material.endsWithTypeOf(type: String, allowPlainForm: Boolean = false, ignoreCase: Boolean = true): Boolean = name.endsWithTypeOf(type, allowPlainForm, ignoreCase)

        @JvmStatic
        fun String.startsWithTypeOf(type: String, allowPlainForm: Boolean = false, ignoreCase: Boolean = true): Boolean = startsWith(if (allowPlainForm) type else "${type}_", ignoreCase)

        @JvmStatic
        fun Material.startsWithTypeOf(type: String, allowPlainForm: Boolean = false, ignoreCase: Boolean = true): Boolean = name.startsWithTypeOf(type, allowPlainForm, ignoreCase)

        @JvmStatic
        fun String.containsTypeOf(type: String, allowPlainForm: Boolean = false, ignoreCase: Boolean = true): Boolean = contains(if (allowPlainForm) type else "_${type}_", ignoreCase)

        @JvmStatic
        fun Material.containsTypeOf(type: String, allowPlainForm: Boolean = false, ignoreCase: Boolean = true): Boolean = name.containsTypeOf(type, allowPlainForm, ignoreCase)

        @JvmStatic
        val Material.isSapling: Boolean get() = endsWithTypeOf("SAPLING") || this == Material.MANGROVE_PROPAGULE

        @JvmStatic
        fun isOnSaplingBlock(entity: Entity): Boolean {
            val blockUnder: Location = entity.location.clone().add(0.0, -1.0, 0.0)
            return VALID_SAPLING_BLOCKS.contains(entity.world.getBlockAt(blockUnder).type)
        }

        @JvmStatic
        fun isLatestVersion(): CompletableFuture<Boolean> {

            if (MoreRandomThings.IS_DEVBUILD) return CompletableFuture.completedFuture(true)

            val serverVersion: Int = Integer.parseInt(
                MoreRandomThings.INSTANCE
                    .pluginMeta
                    .version
                    .replace(Regex("\\.|-SNAPSHOT|v"), "")
            )


            return CompletableFuture.supplyAsync {

                try {
                    var url: URL = URI.create("https://api.modrinth.com/v2/project/K9JIhdio").toURL()
                    var reader = InputStreamReader(url.openStream())
                    val versions: JsonArray = JsonParser.parseReader(reader).asJsonObject.getAsJsonArray("versions")
                    val version: String = versions[versions.size() - 1].asString

                    url = URI.create("https://api.modrinth.com/v2/version/$version").toURL()
                    reader = InputStreamReader(url.openStream())
                    val latestVersion: Int = Integer.parseInt(
                        JsonParser.parseReader(reader)
                            .asJsonObject["version_number"]
                            .asString
                            .replace(Regex("\\.|-SNAPSHOT|v"), "")
                    )
                    MoreRandomThings.INSTANCE.logger.info("Latest Version: $latestVersion")

                    return@supplyAsync latestVersion <= serverVersion
                } catch (e: IOException) {
                    throw RuntimeException(e)
                }
            }
        }
    }
}