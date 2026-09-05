package app.panelrelay.core

import io.ktor.http.Parameters
import io.ktor.http.formUrlEncode

class MangaInUaProvider(private val http: NativeHttpTransport) : MangaProvider {
    override val id = "manga-in-ua"
    private val baseUrl = "https://manga.in.ua"
    private val ajaxUrl = "$baseUrl/engine/ajax/controller.php"

    override fun recognizes(url: String) =
        url.startsWith("https://manga.in.ua/mangas/") || url.startsWith("https://manga.in.ua/chapters/")

    override suspend fun loadSeries(url: String): MangaSeries {
        require(recognizes(url)) { "Paste an HTTPS manga.in.ua series or chapter URL." }
        val html = http.request(url).requireSuccess("Series page").text()
        val hash = capture(html, """var\s+site_login_hash\s*=\s*["']([^"']+)["']""", "guest hash")
        val seriesTag = Regex("""<[^>]+id=["']linkstocomics["'][^>]*>""", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
            .find(html)?.value ?: error("The page did not contain series metadata.")
        val seriesId = attribute(seriesTag, "data-news_id") ?: error("The page did not contain a series ID.")
        val category = attribute(seriesTag, "data-news_category") ?: error("The page did not contain a category.")
        val title = Regex("""<span\s+class=["']UAname["'][^>]*>(.*?)</span>""", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
            .find(html)?.groupValues?.get(1)?.let(::cleanText)
            ?: Regex("""<meta\s+name=["']description["']\s+content=["']([^"']+)["']""", RegexOption.IGNORE_CASE)
                .find(html)?.groupValues?.get(1)?.let(::decodeEntities)?.substringBefore(" - Том:")
            ?: "Imported series"
        val cover = Regex("""<meta\s+property=["']og:image["']\s+content=["']([^"']+)""", RegexOption.IGNORE_CASE)
            .find(html)?.groupValues?.get(1)?.let(::decodeEntities)?.let(::absoluteUrl)
        val form = Parameters.build {
            append("action", "show")
            append("news_id", seriesId)
            append("news_category", category)
            append("this_link", if (category == "54") url else "")
            append("user_hash", hash)
        }.formUrlEncode()
        val catalog = http.request(
            "$ajaxUrl?mod=load_chapters",
            method = "POST",
            body = form,
            headers = mapOf(
                "Content-Type" to "application/x-www-form-urlencoded; charset=UTF-8",
                "X-Requested-With" to "XMLHttpRequest",
            ),
        ).requireSuccess("Chapter catalog").text()
        return MangaSeries(id, seriesId, title, url, category, cover, parseChapters(catalog))
    }

    override suspend fun loadPages(chapter: MangaChapter): List<MangaPage> {
        val html = http.request(chapter.url).requireSuccess("Chapter page").text()
        val hash = capture(html, """var\s+site_login_hash\s*=\s*["']([^"']+)["']""", "guest hash")
        val chapterTag = Regex("""<[^>]+id=["']comics["'][^>]*>""", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
            .find(html)?.value ?: error("The chapter did not contain its ID.")
        val chapterId = attribute(chapterTag, "data-news_id") ?: error("The chapter did not contain its ID.")
        val endpoint = "$ajaxUrl?mod=load_chapters_image&news_id=$chapterId&action=show&user_hash=$hash"
        val pageHtml = http.request(endpoint, headers = mapOf("X-Requested-With" to "XMLHttpRequest"))
            .requireSuccess("Page catalog").text()
        val pages = Regex("""<img\b[^>]*>""", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
            .findAll(pageHtml)
            .mapNotNull { attribute(it.value, "data-src") ?: attribute(it.value, "src") }
            .map(::absoluteUrl)
            .filter { !it.contains("imagebg.jpg") && !it.startsWith("data:") && Regex("""\.(jpe?g|png|webp)(\?.*)?$""", RegexOption.IGNORE_CASE).containsMatchIn(it) }
            .distinct()
            .mapIndexed { index, pageUrl -> MangaPage(index + 1, pageUrl) }
            .toList()
        check(pages.isNotEmpty()) { "The source returned no page images." }
        return pages
    }

    internal fun parseChapters(html: String): List<MangaChapter> {
        val anchors = Regex("""<a\b([^>]*)>(.*?)</a>""", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
            .findAll(html).mapNotNull { match ->
                val attrs = match.groupValues[1]
                if (!attribute(attrs, "class").orEmpty().split(Regex("\\s+")).contains("chapterscalc")) return@mapNotNull null
                chapter(attribute(attrs, "href") ?: return@mapNotNull null, cleanText(match.groupValues[2]), attrs)
            }.toList()
        val options = Regex("""<option\b([^>]*)>(.*?)</option>""", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
            .findAll(html).mapNotNull { match ->
                val attrs = match.groupValues[1]
                chapter(attribute(attrs, "value") ?: return@mapNotNull null, cleanText(match.groupValues[2]), attrs)
            }.toList()
        val chapters = (if (anchors.isNotEmpty()) anchors else options).distinctBy { it.url }
            .sortedWith(compareBy({ it.volume?.toDoubleOrNull() ?: 0.0 }, { it.number ?: 0.0 }))
        check(chapters.isNotEmpty()) { "The source returned no chapters." }
        return chapters
    }

    private fun chapter(rawUrl: String, label: String, attrs: String): MangaChapter {
        val parts = Regex("""Том\s*([0-9]+(?:[.,][0-9]+)?).*?Розділ\s*([0-9]+(?:[.,][0-9]+)?)""", RegexOption.IGNORE_CASE).find(label)
        val url = absoluteUrl(rawUrl)
        return MangaChapter(
            Regex("""/chapters/([0-9]+)-""").find(url)?.groupValues?.get(1) ?: url,
            label,
            url,
            attribute(attrs, "data-chapter")?.toDoubleOrNull() ?: parts?.groupValues?.get(2)?.replace(',', '.')?.toDoubleOrNull(),
            attribute(attrs, "data-volume") ?: parts?.groupValues?.get(1)?.replace(',', '.'),
        )
    }

    private fun capture(input: String, pattern: String, description: String) =
        Regex(pattern, setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)).find(input)
            ?.groupValues?.get(1)?.let(::decodeEntities) ?: error("The page did not contain $description.")

    private fun attribute(input: String, name: String): String? {
        val match = Regex("""\b${Regex.escape(name)}\s*=\s*(?:["']([^"']*)["']|([^\s>]+))""", RegexOption.IGNORE_CASE).find(input) ?: return null
        return decodeEntities(match.groupValues[1].ifEmpty { match.groupValues[2] })
    }

    private fun cleanText(value: String) = decodeEntities(value.replace(Regex("<[^>]+>"), "")).trim().replace(Regex("\\s+"), " ")
    private fun decodeEntities(value: String) = value.replace("&amp;", "&").replace("&quot;", "\"").replace("&#039;", "'").replace("&lt;", "<").replace("&gt;", ">")
    private fun absoluteUrl(value: String) = when {
        value.startsWith("//") -> "https:$value"
        value.startsWith("/") -> "$baseUrl$value"
        else -> value
    }
}
