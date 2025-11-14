package dk.marcusrokatis.moreRandomThings

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.nio.file.Files

class DataHandler {

    val gson: Gson = GsonBuilder().create()
    val dataFile: File = File(MoreRandomThings.INSTANCE.dataFolder, "data.json")
    lateinit var data: PluginData

    init {
        try {
            loadData()
        } catch (e: IOException) {
            throw RuntimeException(e)
        }
    }

    fun loadData() {
        if (!dataFile.exists()) {
            MoreRandomThings.INSTANCE.logger.info("No data file found.  Using blank data...")
            data = PluginData()
        } else {
            MoreRandomThings.INSTANCE.logger.info("Loading from data file...")
            val content: String = Files.readString(dataFile.toPath())
            data = gson.fromJson(content, PluginData::class.java)
        }
    }

    fun saveData() {
        MoreRandomThings.INSTANCE.logger.info("Saving data file...")
        val json: String = gson.toJson(data)
        if (!dataFile.parentFile.exists()) dataFile.parentFile.mkdir().let { if (!it) throw IOException("Failed to create directory") }
        if (!dataFile.exists()) dataFile.createNewFile()
        val fw = FileWriter(dataFile)
        fw.write(json)
        fw.close()
        MoreRandomThings.INSTANCE.logger.info("Done saving data.")
    }
}