package com.vidacotidiana.app.core.data

import android.content.Context
import com.vidacotidiana.app.core.network.VisionBoardDto
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * La última foto conocida del tablero, guardada en disco.
 *
 * POR QUÉ: abrir el Vision Board pedía la lista de tableros, luego el
 * tablero, y solo entonces empezaba a pintar. Son dos viajes de red antes
 * de que aparezca nada — con una conexión lenta, segundos mirando un hueco
 * vacío, y el tablero que se va a ver es casi siempre el mismo de la última
 * vez.
 *
 * Así que se guarda tal cual llegó del servidor y en la siguiente apertura
 * se pinta ANTES de preguntar nada. La red sigue yendo detrás y, cuando
 * contesta, reemplaza lo pintado con lo que hay de verdad. Si cambió algo
 * desde otro dispositivo, se ve el cambio un instante después; lo que no
 * se ve nunca es una pantalla en blanco.
 *
 * No es una base de datos ni lo pretende: es UN fichero con el último
 * tablero. Si se corrompe o cambia el formato, se ignora y se carga de la
 * red como siempre — por eso todas las lecturas devuelven `null` en vez de
 * fallar.
 */
@Singleton
class VisionBoardSnapshot @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val file: File get() = File(context.filesDir, "vision-board-last.json")

    suspend fun save(board: VisionBoardDto) = withContext(Dispatchers.IO) {
        runCatching { file.writeText(json.encodeToString(VisionBoardDto.serializer(), board)) }
        Unit
    }

    suspend fun read(): VisionBoardDto? = withContext(Dispatchers.IO) {
        runCatching {
            if (!file.exists()) null else json.decodeFromString(VisionBoardDto.serializer(), file.readText())
        }.getOrNull()
    }

    suspend fun clear() = withContext(Dispatchers.IO) {
        runCatching { file.delete() }
        Unit
    }
}
