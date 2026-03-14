package com.jbncode.bibliaapp

import android.content.Context
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Singleton

@Singleton
class DatabaseDownloader(@ApplicationContext private val context: Context) {

    // Callback para informar el progreso (opcional pero muy recomendado para UX)
    interface DownloadProgressListener {
        fun onProgress(progressPercentage: Int)
        fun onCompleted(file: File)
        fun onError(exception: Exception)
    }

    suspend fun downloadDatabase(
        downloadUrl: String, // URL completa del archivo .sqlite3 en el servidor
        destinationFileName: String, // Nombre con el que se guardará localmente (ej: "NVI.sqlite3")
        progressListener: DownloadProgressListener? = null
    ): File? {
        val destinationDir = context.getDatabasePath("any_temp_name.sqlite3").parentFile
        if (destinationDir != null && !destinationDir.exists()) {
            destinationDir.mkdirs()
        }
        val destinationFile = File(destinationDir, destinationFileName)

        var connection: HttpURLConnection? = null
        var inputStream: InputStream? = null
        var outputStream: FileOutputStream? = null

        try {
            val url = URL(downloadUrl)
            connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = 15000 // 15 segundos timeout conexión
            connection.readTimeout = 30000    // 30 segundos timeout lectura
            connection.connect()

            // Verificar respuesta del servidor
            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                val errorMsg = "Error del servidor: ${connection.responseCode} ${connection.responseMessage}"
                Log.e("Downloader", errorMsg)
                progressListener?.onError(IOException(errorMsg))
                return null
            }

            val fileLength = connection.contentLength // -1 si no se conoce

            inputStream = connection.inputStream
            outputStream = FileOutputStream(destinationFile) // Sobrescribe si ya existe

            val data = ByteArray(4096) // Buffer de 4KB
            var totalBytesRead: Long = 0
            var bytesRead: Int

            Log.i("Downloader", "Descargando desde: $downloadUrl -> ${destinationFile.absolutePath}")

            while (inputStream.read(data).also { bytesRead = it } != -1) {
                outputStream.write(data, 0, bytesRead)
                totalBytesRead += bytesRead

                if (fileLength > 0) { // Solo si conocemos el tamaño total
                    val progress = (totalBytesRead * 100 / fileLength).toInt()
                    progressListener?.onProgress(progress)
                }
            }

            outputStream.flush() // Asegura que todos los bytes se escriban
            Log.i("Downloader", "Descarga completada: ${destinationFile.name}")
            progressListener?.onCompleted(destinationFile)
            return destinationFile

        } catch (e: Exception) {
            Log.e("Downloader", "Error durante la descarga de $downloadUrl", e)
            progressListener?.onError(e)
            // Eliminar archivo parcial si la descarga falló
            if (destinationFile.exists()) {
                destinationFile.delete()
            }
            return null
        } finally {
            try {
                outputStream?.close()
                inputStream?.close()
                connection?.disconnect()
            } catch (ioe: IOException) {
                Log.e("Downloader", "Error al cerrar streams/conexión", ioe)
            }
        }
    }
}