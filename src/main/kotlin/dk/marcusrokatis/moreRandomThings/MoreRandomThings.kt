package dk.marcusrokatis.moreRandomThings

import org.bukkit.plugin.java.JavaPlugin
import java.io.IOException
import java.util.concurrent.ExecutionException

class MoreRandomThings : JavaPlugin() {

    val INSTANCE: MoreRandomThings = this
    lateinit var config: PluginConfig

    lateinit var dataHandler: DataHandler

    override fun onEnable() {
        try {
            Util().isLatestVersion().thenAccept { latest ->
                if (!latest) {
                    logger.warning("RandomThings has an update!")
                    logger.warning("Get it from https://modrinth.com/plugin/randomthings")
                }
            }.get()
        } catch (e: InterruptedException) {
            throw RuntimeException(e)
        } catch (e: ExecutionException) {
            throw RuntimeException(e)
        }

        try {
            config = PluginConfig(INSTANCE)
        } catch (e: IOException) {
            throw RuntimeException(e)
        }

        dataHandler = DataHandler()
    }

    override fun onDisable() {
        try {
            dataHandler.saveData()
        } catch (e: IOException) {
            throw RuntimeException(e)
        }
    }
}
