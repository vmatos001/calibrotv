package com.example.calibretv.data.api

import com.example.calibretv.data.model.Book
import com.example.calibretv.data.model.CalibreShelf
import com.example.calibretv.data.opds.SslHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object ApiClient {

    const val BASE_URL = "https://b-blio-tk.duckdns.org/personajes/api"

    private fun resolveCoverUrl(rawUrl: String?): String? {
        if (rawUrl.isNullOrBlank()) return null
        if (rawUrl.startsWith("http://") || rawUrl.startsWith("https://")) return rawUrl
        val domain = "https://b-blio-tk.duckdns.org"
        return if (rawUrl.startsWith("/")) "$domain$rawUrl" else "$domain/$rawUrl"
    }

    suspend fun fetchSummary(): Result<JSONObject> = withContext(Dispatchers.IO) {
        try {
            val url = URL("$BASE_URL/summary")
            val conn = url.openConnection() as HttpURLConnection
            SslHelper.configureHttps(conn)
            conn.connectTimeout = 8000
            conn.readTimeout = 12000
            conn.setRequestProperty("Accept", "application/json")
            conn.connect()

            if (conn.responseCode in 200..299) {
                val jsonStr = conn.inputStream.bufferedReader().use { it.readText() }
                Result.success(JSONObject(jsonStr))
            } else {
                Result.failure(Exception("HTTP ${conn.responseCode}: ${conn.responseMessage}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun fetchShelves(): Result<List<CalibreShelf>> = withContext(Dispatchers.IO) {
        try {
            val url = URL("$BASE_URL/shelves")
            val conn = url.openConnection() as HttpURLConnection
            SslHelper.configureHttps(conn)
            conn.connectTimeout = 8000
            conn.readTimeout = 14000
            conn.setRequestProperty("Accept", "application/json")
            conn.connect()

            if (conn.responseCode in 200..299) {
                val jsonStr = conn.inputStream.bufferedReader().use { it.readText() }
                val root = JSONObject(jsonStr)
                val shelvesArray = root.optJSONArray("shelves") ?: JSONArray()
                val list = mutableListOf<CalibreShelf>()

                for (i in 0 until shelvesArray.length()) {
                    val obj = shelvesArray.getJSONObject(i)
                    val id = obj.optString("id", i.toString())
                    val name = obj.optString("name", "")
                    if (name.isBlank() || name.equals("null", ignoreCase = true)) continue

                    val type = obj.optString("type", "character")
                    val isChar = type == "character"
                    val hasImg = obj.optBoolean("has_image", true) || obj.has("image_url")
                    val rawImg = obj.optString("image_url", "/personajes/api/shelves/$id/image")
                    val imageUrl = if (hasImg) resolveCoverUrl(rawImg) else null

                    list.add(
                        CalibreShelf(
                            id = id,
                            name = name,
                            bookIds = emptyList(),
                            isCharacterShelf = isChar,
                            hasImage = hasImg,
                            imageUrl = imageUrl
                        )
                    )
                }
                Result.success(list)
            } else {
                Result.failure(Exception("HTTP ${conn.responseCode}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun fetchBooks(limit: Int = 500, offset: Int = 0): Result<List<Book>> = withContext(Dispatchers.IO) {
        try {
            val url = URL("$BASE_URL/books?limit=$limit&offset=$offset")
            val conn = url.openConnection() as HttpURLConnection
            SslHelper.configureHttps(conn)
            conn.connectTimeout = 10000
            conn.readTimeout = 20000
            conn.setRequestProperty("Accept", "application/json")
            conn.connect()

            if (conn.responseCode in 200..299) {
                val jsonStr = conn.inputStream.bufferedReader().use { it.readText() }
                val root = JSONObject(jsonStr)
                val booksArray = root.optJSONArray("books") ?: JSONArray()
                val books = mutableListOf<Book>()

                for (i in 0 until booksArray.length()) {
                    val obj = booksArray.getJSONObject(i)
                    val id = obj.optString("id", "")
                    val title = obj.optString("title", "Sin título")
                    val author = obj.optString("author", "Autor Desconocido")
                    val coverRaw = if (obj.has("cover_url") && !obj.isNull("cover_url")) obj.getString("cover_url") else null
                    val coverUrl = resolveCoverUrl(coverRaw)
                    val synopsis = obj.optString("synopsis", "")

                    // Valid Calibre-Web OPDS EPUB Download URL
                    val epubUrl = "https://b-blio-tk.duckdns.org/opds/download/$id/epub/"

                    // Extract shelves names & level tags
                    val shelvesList = mutableListOf<String>()
                    val shelvesArr = obj.optJSONArray("shelves")
                    if (shelvesArr != null) {
                        for (s in 0 until shelvesArr.length()) {
                            val sObj = shelvesArr.getJSONObject(s)
                            val sName = sObj.optString("name", "")
                            if (sName.isNotBlank() && !sName.equals("null", ignoreCase = true)) {
                                shelvesList.add(sName)
                            }
                        }
                    }

                    val charName = obj.optString("character", "")
                    if (charName.isNotBlank() && !charName.equals("null", ignoreCase = true) && !shelvesList.contains(charName)) {
                        shelvesList.add(charName)
                    }

                    val levelName = obj.optString("level", "")
                    if (levelName.isNotBlank() && !levelName.equals("null", ignoreCase = true) && !shelvesList.contains(levelName)) {
                        shelvesList.add(levelName)
                    }

                    books.add(
                        Book(
                            id = id,
                            title = title,
                            author = author,
                            coverUrl = coverUrl,
                            epubUrl = epubUrl,
                            summary = synopsis,
                            category = if (charName.isNotBlank() && !charName.equals("null", ignoreCase = true)) charName else "General",
                            tags = shelvesList.distinct(),
                            shelves = shelvesList.distinct()
                        )
                    )
                }
                Result.success(books)
            } else {
                Result.failure(Exception("HTTP ${conn.responseCode}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
