package com.simplesoftware.browser.browser

import android.util.Log
import com.simplesoftware.browser.Config
import com.simplesoftware.browser.model.FavoriteItem
import com.simplesoftware.browser.model.HomePageLink
import com.simplesoftware.browser.data.FavoritesRepository
import com.simplesoftware.browser.data.HistoryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.json.JSONArray
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Links shown on the built-in home page: bookmarks, latest or most visited history items.
 * The bookmarks are the ones of the Home page folder, in its order: a link's order is its place
 * on the page, and the page leaves out those it has no room for.
 */
class HomePageLinks(
    private val config: Config,
    private val history: HistoryRepository,
    private val favorites: FavoritesRepository
) {
    companion object {
        private const val TAG = "HomePageLinks"
    }

    private val _links = MutableStateFlow<List<HomePageLink>>(emptyList())
    /** Can be read from any thread (home page scripts ask for it on a background thread) */
    val links: StateFlow<List<HomePageLink>> = _links.asStateFlow()

    suspend fun load() {
        Log.d(TAG, "load")
        if (config.homePageMode != Config.HomePageMode.HOME_PAGE) return
        when (config.homePageLinksMode) {
            Config.HomePageLinksMode.MOST_VISITED -> {
                _links.value = history.mostVisited()
                    .map { HomePageLink.fromHistoryItem(it) }
            }
            Config.HomePageLinksMode.LATEST_HISTORY -> {
                _links.value = history.last(8)
                    .map { HomePageLink.fromHistoryItem(it) }
            }
            Config.HomePageLinksMode.BOOKMARKS -> {
                val bookmarks = favorites.homePageBookmarks()
                if (bookmarks.isEmpty() && !config.initialBookmarksSuggestionsLoaded) {
                    val suggestions = initialBookmarksSuggestions() ?: return
                    for (s in suggestions) {
                        favorites.save(s)
                    }
                    config.initialBookmarksSuggestionsLoaded = true
                    _links.value = suggestions.mapIndexed { i, item -> HomePageLink.fromBookmarkItem(item, i) }
                } else {
                    _links.value = bookmarks.mapIndexed { i, item -> HomePageLink.fromBookmarkItem(item, i) }
                }
            }
        }
    }

    /** The links that follow the removed one move up a place */
    suspend fun remove(link: HomePageLink) {
        _links.update { it - link }
        link.favoriteId?.let {
            favorites.delete(it)
        }
        load()
    }

    /**
     * Saves the bookmark edited (or created, if its id is 0: it goes after the others) by the user and
     * updates the links.
     */
    suspend fun onBookmarkEdited(item: FavoriteItem) {
        if (item.id == 0L) {
            item.homePageBookmark = true
            item.order = favorites.nextHomePageOrder()
        }
        favorites.save(item)
        load()
    }

    suspend fun markRecommendationAsUseful(bookmarkOrder: Int) {
        val link = _links.value.find { it.order == bookmarkOrder } ?: return
        val favoriteId = link.favoriteId ?: return
        if (link.validUntil == null) return
        favorites.markAsUseful(favoriteId)
    }

    private fun initialBookmarksSuggestions(): List<FavoriteItem>? {
        //There was loading of default bookmarks from app's home page (with affiliate links).
        // Now its just hardcoded. I left them as json array just for convenience to copy the same
        // data used in corresponding home html page where them useful for debug
        return try {
            val jsonArray = JSONArray("""[
        {"title": "Reddit - Dive into anything", "url":"https://www.reddit.com", "favicon": "https://www.redditstatic.com/desktop2x/img/favicon/apple-icon-120x120.png"},
        {"title": "My Tuner", "url":"https://mytuner-radio.com/", "favicon": "https://static2.mytuner.mobi/static/icons/apple-touch-icon-120x120-precomposed.png"},
        {"title": "Mises Institute", "url":"https://mises.org", "favicon": "https://cdn.mises.org/favicon.ico"},
        {"title": "X", "url":"https://x.com", "favicon": "https://abs.twimg.com/favicons/twitter.2.ico"},
        {"title": "YouTube", "url":"https://www.youtube.com", "favicon": "https://www.youtube.com/favicon.ico"},
        {"title": "Medium", "url":"https://medium.com", "favicon": "https://t2.gstatic.com/faviconV2?client=SOCIAL&type=FAVICON&fallback_opts=TYPE,SIZE,URL&size=64&url=https://medium.com"}
    ]""")
            val result = mutableListOf<FavoriteItem>()
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            for (i in 0 until jsonArray.length()) {
                val jsonObject = jsonArray.getJSONObject(i)
                val favorite = FavoriteItem()
                favorite.title = jsonObject.getString("title")
                favorite.url = jsonObject.getString("url")
                favorite.favicon = jsonObject.opt("favicon") as String?
                favorite.destUrl = jsonObject.opt("dest_url") as String?
                favorite.description = jsonObject.opt("description") as String?
                favorite.order = i
                favorite.homePageBookmark = true
                val validUntil = jsonObject.opt("valid_until") as String?
                if (validUntil != null) {
                    favorite.validUntil = dateFormat.parse(validUntil)
                }
                result.add(favorite)
            }
            result
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
