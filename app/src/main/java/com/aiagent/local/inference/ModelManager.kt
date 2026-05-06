package com.aiagent.local.inference

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Manages GGUF model files: resolves URIs to absolute paths,
 * copies from content:// URIs to internal storage for llama.cpp access.
 */
class ModelManager(private val context: Context) {

    companion object {
        private const val MODELS_DIR = "models"
    }

    /**
     * Resolves a content:// or file:// URI to an absolute file path.
     * Content URIs from ACTION_OPEN_DOCUMENT are copied to app-local storage
     * because llama.cpp needs a real file path, not a content:// stream.
     */
    suspend fun resolveModelPath(uri: Uri): String = withContext(Dispatchers.IO) {
        val fileName = getFileName(uri) ?: "model_${System.currentTimeMillis()}.gguf"
        val modelsDir = File(context.filesDir, MODELS_DIR)
        if (!modelsDir.exists()) modelsDir.mkdirs()

        val destFile = File(modelsDir, fileName)

        // If already copied, reuse
        if (destFile.exists() && destFile.length() > 0) {
            return@withContext destFile.absolutePath
        }

        // Copy from content URI to internal storage
        context.contentResolver.openInputStream(uri)?.use { input ->
            destFile.outputStream().use { output ->
                input.copyTo(output)
            }
        } ?: throw IllegalStateException("Cannot open input stream for URI: $uri")

        destFile.absolutePath
    }

    /**
     * Extract file name from URI
     */
    private fun getFileName(uri: Uri): String? {
        val cursor = context.contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val displayNameIndex =
                    it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (displayNameIndex >= 0) {
                    return it.getString(displayNameIndex)
                }
            }
        }
        return uri.lastPathSegment
    }

    /**
     * Returns list of GGUF files already in models directory
     */
    fun getLocalModels(): List<File> {
        val modelsDir = File(context.filesDir, MODELS_DIR)
        if (!modelsDir.exists()) return emptyList()
        return modelsDir.listFiles { file ->
            file.extension.equals("gguf", true)
        }?.toList() ?: emptyList()
    }

    /**
     * Delete a previously imported model
     */
    fun deleteModel(fileName: String): Boolean {
        val file = File(context.filesDir, "$MODELS_DIR/$fileName")
        return file.delete()
    }
}