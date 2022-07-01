package bot.inker.onemirror.traffic

import org.slf4j.LoggerFactory
import java.io.IOException
import java.io.OutputStream
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardOpenOption
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

object LoggerWriter {
    private val logger = LoggerFactory.getLogger("logger-writer")
    private val LINE_SEPARATOR = "\n".toByteArray()
    fun log(name:String, line:String){
        val fileOut = file(name)
        try {
            fileOut.lock.withLock {
                fileOut.out.write(line.toByteArray())
                fileOut.out.write(LINE_SEPARATOR)
            }
        }catch (e:Exception){
            logger.warn("Failed to write log", e)
            filePoolLock.withLock {
                filePool.remove(name)
            }
        }
    }

    private val filePoolLock = ReentrantLock()
    private val filePool = HashMap<String,FileOut>()
    private fun file(name: String):FileOut{
        val fileOut = filePoolLock.withLock { filePool.computeIfAbsent(name){
            FileOut(0, OutputStream.nullOutputStream())
        } }
        val date = (System.currentTimeMillis() / (24 * 60 * 60 * 1000))
        fileOut.lock.withLock {
            if (fileOut.date != date) {
                try {
                    fileOut.out.close()
                }catch (e:IOException){
                    //
                }
                val path = Paths.get("logs",name,"$date.log")
                Files.createDirectories(path.parent)
                fileOut.out = Files.newOutputStream(
                    path,
                    StandardOpenOption.APPEND,
                    StandardOpenOption.CREATE_NEW
                )
                fileOut.date = date
            }
        }
        return fileOut
    }
    data class FileOut(
        var date: Long,
        var out: OutputStream
    ){
        val lock = ReentrantLock()
    }
}