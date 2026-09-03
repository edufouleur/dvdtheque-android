package fr.dvdtheque.app.data

import android.content.Context
import java.io.File
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class BackupManager(
    context: Context,
    private val repository: MovieRepository
) {
    private val folder = File(context.filesDir, "Sauvegarde").apply { mkdirs() }
    private val dayFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    private val timeFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss")

    suspend fun ensureDailyBackup() {
        val file = File(folder, "Reelio_Quotidienne_${LocalDate.now().format(dayFormat)}.json")
        if (!file.exists()) {
            folder.listFiles { f -> f.name.startsWith("Reelio_Quotidienne_") && f.extension == "json" }
                ?.forEach { it.delete() }
            write(file)
        }
    }

    suspend fun createActiveBackup() {
        val stamp = LocalDateTime.now().format(timeFormat)
        write(File(folder, "Reelio_Auto_$stamp.json"))
        folder.listFiles { f -> f.name.startsWith("Reelio_Auto_") && f.extension == "json" }
            ?.sortedByDescending { it.lastModified() }
            ?.drop(3)
            ?.forEach { it.delete() }
    }

    fun dailyFile(): File? = folder.listFiles { f -> f.name.startsWith("Reelio_Quotidienne_") && f.extension == "json" }
        ?.maxByOrNull { it.lastModified() }

    fun activeFiles(): List<File> = folder.listFiles { f -> f.name.startsWith("Reelio_Auto_") && f.extension == "json" }
        ?.sortedByDescending { it.lastModified() }
        .orEmpty()

    fun folderPath(): String = folder.absolutePath

    fun allBackupNames(): List<String> = folder.listFiles { f -> f.extension == "json" }
        ?.sortedByDescending { it.lastModified() }
        ?.map { it.name }
        .orEmpty()

    fun read(name: String): List<Movie> {
        val safe = File(folder, name)
        require(safe.parentFile?.canonicalPath == folder.canonicalPath && safe.exists())
        return MovieBackupCodec.decode(safe.readText())
    }

    private suspend fun write(file: File) {
        file.writeText(MovieBackupCodec.encode(repository.snapshot()))
    }
}
