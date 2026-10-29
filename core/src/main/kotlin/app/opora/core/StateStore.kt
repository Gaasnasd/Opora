package app.opora.core

import app.opora.core.model.AppState
import kotlinx.serialization.json.Json
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption

object StateCodec {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun encode(state: AppState): String = json.encodeToString(AppState.serializer(), state)

    fun decode(text: String): AppState = json.decodeFromString(AppState.serializer(), text)
}

/** Файл состояния `dir/state.json`. Запись атомарная: временный файл, затем переименование. */
class StateStore(private val dir: File, private val clock: () -> Long) {
    data class LoadResult(val state: AppState, val brokenBackup: File?)

    private val file = File(dir, "state.json")
    private val temp = File(dir, "state.json.tmp")

    fun load(): LoadResult {
        if (!file.exists()) return LoadResult(AppState(), null)
        return try {
            LoadResult(StateCodec.decode(file.readText()), null)
        } catch (e: Exception) {
            // Не затираем то, что не смогли прочитать: откладываем в сторону.
            val backup = File(dir, "state.broken-${clock()}.json")
            file.renameTo(backup)
            LoadResult(AppState(), backup)
        }
    }

    fun save(state: AppState) {
        dir.mkdirs()
        temp.writeText(StateCodec.encode(state))
        Files.move(
            temp.toPath(),
            file.toPath(),
            StandardCopyOption.ATOMIC_MOVE,
            StandardCopyOption.REPLACE_EXISTING,
        )
    }
}
