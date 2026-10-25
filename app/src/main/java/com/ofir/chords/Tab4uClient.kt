package com.ofir.chords

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.net.URLDecoder
import java.net.URLEncoder

data class SongResult(val title: String, val url: String) {
    override fun toString() = title
}

data class SongLine(val text: String, val isChord: Boolean)

/** Fetches and parses Tab4u pages. Call from a background thread. */
object Tab4uClient {
    private const val BASE = "https://www.tab4u.com/"
    private const val UA =
        "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 " +
            "(KHTML, like Gecko) Chrome/124 Mobile Safari/537.36"

    private fun get(url: String): Document =
        Jsoup.connect(url)
            .userAgent(UA)
            .header("Accept-Language", "he,en;q=0.8")
            .timeout(15_000)
            .get()

    fun search(query: String): List<SongResult> {
        val url = BASE + "resultsSimple?tab=songs&q=" + URLEncoder.encode(query, "UTF-8")
        val doc = get(url)
        val found = LinkedHashMap<String, String>() // url -> title
        for (a in doc.select("a[href*=tabs/songs/]")) {
            val href = a.absUrl("href")
            if (href.isEmpty()) continue
            val text = a.text().trim()
            val existing = found[href]
            if (existing == null || (existing.isEmpty() && text.isNotEmpty())) {
                found[href] = text
            }
        }
        return found.map { (url, title) ->
            SongResult(title.ifEmpty { titleFromUrl(url) }, url)
        }
    }

    /** Returns chord and lyric lines in page order, keeping spacing for alignment. */
    fun fetchSong(url: String): List<SongLine> {
        val doc = get(url)
        val root = doc.getElementById("songContentTPL") ?: doc.body()
        val lines = mutableListOf<SongLine>()
        for (td in root.select("td")) {
            val classes = td.classNames()
            val isChord = classes.any { it.startsWith("chords") }
            val isLyric = "song" in classes
            if (!isChord && !isLyric) continue
            val text = td.wholeText().replace(' ', ' ').trimEnd()
            lines += SongLine(text, isChord)
        }
        if (lines.isEmpty()) {
            // Fallback if the page layout changed: plain text of the song area
            root.select("br").before("\n")
            root.wholeText().replace(' ', ' ').lines()
                .map { it.trimEnd() }
                .filter { it.isNotBlank() }
                .forEach { lines += SongLine(it, false) }
        }
        return lines
    }

    private fun titleFromUrl(url: String): String {
        val file = url.substringAfterLast('/').substringBefore(".html")
        val name = URLDecoder.decode(file, "UTF-8")
        return name.substringAfter('_').replace('_', ' ').trim()
    }
}
