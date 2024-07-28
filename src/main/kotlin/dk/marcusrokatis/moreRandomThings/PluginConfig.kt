package dk.marcusrokatis.moreRandomThings

import com.tchristofferson.configupdater.ConfigUpdater
import org.bukkit.configuration.file.FileConfiguration
import java.io.File

class PluginConfig(plugin: MoreRandomThings) {

    private val configFile: File = File(plugin.dataFolder, "config.yml")

    var vacuumHoppers: Boolean = false
    var vacuumRadius: Int = 0
    var radiusIsChunks: Boolean = false
    var renewableBlackstone: Boolean = false
    var renewableDeepslate: Boolean = false
    var renewableAndesite: Boolean = false
    var renewableSponges: Boolean = false
    var silverFishDropGravel: Boolean = false
    var rightClickHarvestCrops: Boolean = false
    var twerkBonemeal: Boolean = false
    var dispenserBreakBlocks: Boolean = false
    var dispenserTillBlocks: Boolean = false
    var dispenserCauldrons: Boolean = false
    var movableAmethyst: Boolean = false
    var magicMirror: Boolean = false
    var autoSaplings: Boolean = false
    var elevators: Boolean = false

    init {
        plugin.saveDefaultConfig()
        update()

        val config: FileConfiguration = plugin.config

        vacuumHoppers = config.getBoolean("vacuum-hoppers")
        vacuumRadius = config.getInt("vacuum-radius", 5)
        radiusIsChunks = config.getBoolean("radius-is-chunks", false)
        renewableBlackstone = config.getBoolean("renewable-blackstone")
        renewableDeepslate = config.getBoolean("renewable-deepslate")
        renewableAndesite = config.getBoolean("renewable-andesite")
        renewableSponges = config.getBoolean("renewable-sponges")
        silverFishDropGravel = config.getBoolean("silver-fish-drop-gravel")
        rightClickHarvestCrops = config.getBoolean("right-click-harvest-crops")
        twerkBonemeal = config.getBoolean("twerk-bonemeal")
        dispenserBreakBlocks = config.getBoolean("dispenser-break-blocks")
        dispenserTillBlocks = config.getBoolean("dispenser-till-blocks")
        dispenserCauldrons = config.getBoolean("dispenser-cauldrons")
        movableAmethyst = config.getBoolean("movable-amethyst")
        magicMirror = config.getBoolean("magic-mirror")
        autoSaplings = config.getBoolean("auto-saplings")
        elevators = config.getBoolean("elevators")
    }

    fun update() = ConfigUpdater.update(MoreRandomThings.INSTANCE, "config.yml", configFile)
}