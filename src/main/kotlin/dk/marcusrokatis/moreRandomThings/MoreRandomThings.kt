package dk.marcusrokatis.moreRandomThings

import org.bukkit.plugin.java.JavaPlugin
import java.io.IOException

class MoreRandomThings : JavaPlugin() {

    val INSTANCE: MoreRandomThings = this
    lateinit var config: PluginConfig

    lateinit var dataHandler: DataHandler

    override fun onEnable() {
        try {
            config = PluginConfig(INSTANCE)
        } catch (e: IOException) {
            throw RuntimeException(e)
        }
    }

    override fun onDisable() {
        // Plugin shutdown logic
    }
}
