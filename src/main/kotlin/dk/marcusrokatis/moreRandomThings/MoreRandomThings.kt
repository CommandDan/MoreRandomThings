package dk.marcusrokatis.moreRandomThings

import dk.marcusrokatis.moreRandomThings.events.DispenserEvents
import dk.marcusrokatis.moreRandomThings.events.GeneralEvents
import dk.marcusrokatis.moreRandomThings.events.PlayerEvents
import org.bukkit.Bukkit
import org.bukkit.Chunk
import org.bukkit.Particle
import org.bukkit.entity.Entity
import org.bukkit.entity.Item
import org.bukkit.event.Listener
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.util.Vector
import java.io.IOException
import java.util.concurrent.ExecutionException

class MoreRandomThings : JavaPlugin() {

    lateinit var dataHandler: DataHandler

    init {
        INSTANCE = this
    }

    override fun onEnable() {
        try {
            Util.isLatestVersion().thenAccept { latest ->
                if (!latest) {
                    logger.warning("MoreRandomThings has an update!")
                    logger.warning("Get it from https://modrinth.com/plugin/morerandomthings")
                }
            }.get()
        } catch (e: InterruptedException) {
            throw RuntimeException(e)
        } catch (e: ExecutionException) {
            throw RuntimeException(e)
        }

        try {
            configuration = PluginConfig(INSTANCE)
        } catch (e: IOException) {
            throw RuntimeException(e)
        }

        dataHandler = DataHandler()
        registerEvents()

        Bukkit.getScheduler().scheduleSyncRepeatingTask(
            this,
            {
                this.dataHandler.data.vacuumHoppers.forEach { h ->
                    val entity: Entity = Bukkit.getEntity(h) ?: return@forEach

                    entity.world.spawnParticle(
                        Particle.PORTAL,
                        entity.location,
                        50,
                        .25,
                        .25,
                        .25,
                        .0
                    )

                    val radius: Int = configuration.vacuumRadius

                    val entities: MutableList<Entity>

                    if (configuration.radiusIsChunks) {
                        entities = ArrayList()
                        val centre: Chunk = entity.chunk
                        for (z: Int in -radius ..radius) {
                            for (x: Int in -radius .. radius) {
                                entities.addAll(centre.world.getChunkAt(centre.x + x, centre.z + z).entities.toList())
                            }
                        }
                    } else {
                        entities = entity.getNearbyEntities(radius.toDouble(), radius.toDouble(), radius.toDouble())
                    }
                    entities.stream().filter { e -> e is Item }.forEach { i ->
                        i.velocity = Vector(.0, .0, .0)
                        i.teleport(entity)
                    }
                }
            },
            0,
            20
        )

        Bukkit.getScheduler().scheduleSyncRepeatingTask(
            this,
            {
                GeneralEvents.TO_BE_PLANTED.forEach { (uuid, la) ->
                    val entity: Item? = Bukkit.getEntity(uuid) as Item?
                    if (la.sum() <= 0) {
                        if (entity != null) {
                            if (Util.isOnSaplingBlock(entity)) {
                                Util.placeBlock(entity)
                                val stack: ItemStack = entity.itemStack
                                stack.amount -= 1
                            }
                        }
                        GeneralEvents.TO_BE_PLANTED.remove(uuid)
                    } else {
                        if (entity == null || !Util.isOnSaplingBlock(entity)) GeneralEvents.TO_BE_PLANTED.remove(uuid)
                        la.add(-10)
                    }
                }
            },
            0,
            10
        )
    }

    override fun onDisable() {
        try {
            dataHandler.saveData()
        } catch (e: IOException) {
            throw RuntimeException(e)
        }
    }

    private fun registerEvents() {

        val eventHandlers: Array<Listener> = arrayOf(
            GeneralEvents(),
            DispenserEvents(),
            PlayerEvents()
        )

        eventHandlers.forEach { server.pluginManager.registerEvents(it, this) }
    }

    companion object {

        @JvmStatic
        lateinit var INSTANCE: MoreRandomThings
        @JvmStatic
        lateinit var configuration: PluginConfig

        fun getDataHandler(): DataHandler = INSTANCE.dataHandler
    }
}
