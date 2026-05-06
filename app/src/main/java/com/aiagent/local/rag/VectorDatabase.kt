package com.aiagent.local.rag

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.math.sqrt

/**
 * SQLite-based vector store with brute-force cosine similarity search.
 * Stores: (id, chunk_text, embedding_json, source_file, chunk_index)
 *
 * Optimized from Off-Grid's pattern: they use op-sqlite with a vector JSON column
 * and cosine similarity scoring. We use plain SQLite for zero-dependency approach.
 */
class VectorDatabase(context: Context) : SQLiteOpenHelper(
    context, "rag_vectors.db", null, 2
) {
    private val json = Json

    companion object {
        const val TABLE_CHUNKS = "chunks"
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE $TABLE_CHUNKS (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                chunk_text TEXT NOT NULL,
                embedding_json TEXT NOT NULL,
                source_file TEXT NOT NULL,
                chunk_index INTEGER NOT NULL
            )
        """.trimIndent())
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_CHUNKS")
        onCreate(db)
    }

    fun insertChunk(
        text: String,
        embedding: FloatArray,
        sourceFile: String,
        chunkIndex: Int
    ) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put("chunk_text", text)
            put("embedding_json", json.encodeToString(embedding.toList()))
            put("source_file", sourceFile)
            put("chunk_index", chunkIndex)
        }
        db.insert(TABLE_CHUNKS, null, values)
    }

    fun searchSimilar(
        queryEmbedding: FloatArray,
        topK: Int = 3,
        minScore: Float = 0.3f
    ): List<Pair<String, Float>> {
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT chunk_text, embedding_json FROM $TABLE_CHUNKS", null)
        val results = mutableListOf<Triple<String, Float, Float>>()

        cursor.use {
            while (it.moveToNext()) {
                val text = it.getString(0)
                val embJson = it.getString(1)
                val storedEmbedding = json.decodeFromString<List<Float>>(embJson).toFloatArray()
                val score = cosineSimilarity(queryEmbedding, storedEmbedding)
                if (score >= minScore) {
                    results.add(Triple(text, embJson.length.toFloat(), score))
                }
            }
        }

        return results
            .sortedByDescending { it.third }
            .take(topK)
            .map { Pair(it.first, it.third) }
    }

    fun deleteBySource(sourceFile: String) {
        writableDatabase.delete(TABLE_CHUNKS, "source_file = ?", arrayOf(sourceFile))
    }

    fun clearAll() {
        writableDatabase.delete(TABLE_CHUNKS, null, null)
    }

    fun getChunkCount(): Int {
        val cursor = readableDatabase.rawQuery(
            "SELECT COUNT(*) FROM $TABLE_CHUNKS", null
        )
        cursor.use {
            return if (it.moveToFirst()) it.getInt(0) else 0
        }
    }

    private fun cosineSimilarity(a: FloatArray, b: FloatArray): Float {
        if (a.size != b.size) return 0f
        var dot = 0f
        var normA = 0f
        var normB = 0f
        for (i in a.indices) {
            dot += a[i] * b[i]
            normA += a[i] * a[i]
            normB += b[i] * b[i]
        }
        val denominator = sqrt(normA) * sqrt(normB)
        return if (denominator == 0f) 0f else dot / denominator
    }

    private fun List<Float>.toFloatArray(): FloatArray {
        return FloatArray(size) { this[it] }
    }
}