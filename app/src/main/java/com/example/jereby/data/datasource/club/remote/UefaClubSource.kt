package com.example.jereby.data.datasource.club.remote

import android.util.Log
import com.example.jereby.data.datasource.club.ClubSource
import com.example.jereby.data.model.Club
import com.example.jereby.network.RawService
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import okhttp3.ResponseBody
import org.json.JSONArray
import org.json.JSONObject
import org.jsoup.Jsoup
import retrofit2.Response
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UefaClubSource @Inject constructor(
    private val raw: RawService,
) : ClubSource {

    override suspend fun load(limit: Int?): List<Club> {
        val indexUrl = "https://www.uefa.com/uefachampionsleague/clubs/"
        val resp: Response<ResponseBody> = raw.fetch(indexUrl)
        if (!resp.isSuccessful) {
            Log.w("Jereby", "UEFA fetch failed: code=${resp.code()}")
            return emptyList()
        }
        val html = resp.body()?.string().orEmpty()
        val doc = Jsoup.parse(html)

        // 1) Try Next.js JSON blob
        doc.selectFirst("#__NEXT_DATA__")?.data()?.let { json ->
            val clubs = parseNextData(json, limit)
            if (clubs.isNotEmpty()) {
                Log.d("Jereby", "Parsed ${clubs.size} clubs from __NEXT_DATA__")
                return evenCount(clubs)
            }
        }

        // 2) Fallback: anchors (older markup)
        val entries = doc.select("a[href*=/uefachampionsleague/clubs/]").mapNotNull { a ->
            val name = a.text().trim().ifBlank { a.attr("aria-label").trim() }
            val href = a.attr("href").trim()
            if (name.isBlank() || href.isBlank()) null else name to absolute(href)
        }.distinctBy { it.second }

        val picked = limit?.let { entries.take(it) } ?: entries
        if (picked.isEmpty()) return emptyList()

        val gate = Semaphore(6)
        val result = coroutineScope {
            picked.map { (name, link) ->
                async {
                    gate.withPermit {
                        val crest = tryGetLogo(link)
                        Club(id = slug(name), name = name, logoUrl = crest)
                    }
                }
            }.awaitAll()
        }
        return evenCount(result)
    }

    // -------- helpers --------

    private fun evenCount(list: List<Club>): List<Club> =
        if (list.size % 2 == 0) list else list.dropLast(1)

    private fun slug(name: String) =
        name.lowercase().replace("[^a-z0-9]+".toRegex(), "-").trim('-')

    private fun absolute(url: String): String {
        val u = url.trim()
        return when {
            u.startsWith("http://") || u.startsWith("https://") -> u
            u.startsWith("//") -> "https:$u"
            u.startsWith("/") -> "https://www.uefa.com$u"
            else -> "https://www.uefa.com/$u"
        }
    }

    private fun parseNextData(json: String, limit: Int?): List<Club> = runCatching {
        val found = mutableListOf<Club>()
        fun walk(node: Any?) {
            when (node) {
                is JSONObject -> {
                    val name = node.optString("name", "")
                    // Slug or URL hints it’s a club object
                    val slug = node.optString("slug", "")
                    val url = node.optString("url", "")
                    val image = node.optString("image", node.optString("logo", ""))
                    val looksLikeClub = name.isNotBlank() && (slug.isNotBlank() || url.contains("/clubs/"))
                    if (looksLikeClub) {
                        val id = if (slug.isNotBlank()) slug else slug(name)
                        val logo = image.takeIf { it.isNotBlank() }?.let(::absolute)
                        found += Club(id = id, name = name, logoUrl = logo)
                    }
                    node.keys().forEachRemaining { key -> walk(node.opt(key)) }
                }
                is JSONArray -> {
                    for (i in 0 until node.length()) walk(node.opt(i))
                }
            }
        }
        walk(JSONObject(json))
        // Deduplicate by id
        val dedup = found.distinctBy { it.id.lowercase() }.sortedBy { it.name }
        if (limit != null) dedup.take(limit) else dedup
    }.getOrElse {
        Log.w("Jereby", "parseNextData error: ${it.message}")
        emptyList()
    }

    private suspend fun tryGetLogo(clubUrl: String): String? = runCatching {
        val resp = raw.fetch(clubUrl)
        if (!resp.isSuccessful) return@runCatching null
        val html = resp.body()?.string().orEmpty()
        val d = Jsoup.parse(html)

        // Prefer og:image
        d.selectFirst("meta[property=og:image]")?.attr("content")?.takeIf { it.isNotBlank() }?.let { return absolute(it) }

        // Common fallbacks
        listOf(
            "img[alt*='logo']", "img[alt*='crest']",
            "img[src*='logo']", "img[src*='crest']",
            "img[data-src*='logo']", "img[data-src*='crest']"
        ).forEach { sel ->
            d.selectFirst(sel)?.let { el ->
                val src = el.attr("src").ifBlank { el.attr("data-src") }
                if (src.isNotBlank()) return absolute(src)
            }
        }
        null
    }.getOrNull()
}

