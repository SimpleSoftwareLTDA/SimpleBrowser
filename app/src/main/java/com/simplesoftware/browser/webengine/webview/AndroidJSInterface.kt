package com.simplesoftware.browser.webengine.webview

import android.content.Context
import android.view.inputmethod.InputMethodManager
import android.webkit.JavascriptInterface
import com.simplesoftware.browser.AppContext
import com.simplesoftware.browser.Config
import com.simplesoftware.browser.SimpleBrowser
import com.simplesoftware.browser.utils.DownloadUtils
import com.simplesoftware.browser.webengine.DownloadRequest
import com.simplesoftware.browser.widgets.cursor.CursorLayout
import org.json.JSONArray
import org.json.JSONObject


class AndroidJSInterface(private val webEngine: WebViewWebEngine) {
    companion object {
        /** A page's blob data is held in memory by the page already: keep the bridge from doubling it without limit */
        private const val MAX_BLOB_BASE64_LENGTH = 256 * 1024 * 1024
    }
    @JavascriptInterface
    fun reloadWithSslTrust() {
        val callback = webEngine.callback ?: return
        if ((webEngine.getView() as WebViewEx).currentOriginalUrl?.scheme != "file") return
        callback.getActivity().runOnUiThread {
            val webview = webEngine.getView() as? WebViewEx ?: return@runOnUiThread
            webview.trustSsl = true
            webEngine.tab.url.apply { webEngine.loadUrl(this) }
        }
    }

    @JavascriptInterface
    fun startVoiceSearch() {
        if (!isHomePage()) return
        val callback = webEngine.callback ?: return
        callback.getActivity().runOnUiThread { callback.initiateVoiceSearch() }
    }

    @JavascriptInterface
    fun setSearchEngine(engine: String, customSearchEngineURL: String) {
        if (!isHomePage()) return
        AppContext.provideConfig().searchEngineURL.value = customSearchEngineURL
    }

    @JavascriptInterface
    fun onEditBookmark(index: Int) {
        if (!isHomePage()) return
        val callback = webEngine.callback ?: return
        callback.getActivity().runOnUiThread { callback.onEditHomePageBookmarkSelected(index) }
    }

    /** The page reports where its search bar is (device pixels of the view) */
    @JavascriptInterface
    fun onHomePageLoaded(searchBarX: Int, searchBarY: Int) {
        onHomePageLoaded()
        if (!isHomePage()) return
        val callback = webEngine.callback ?: return
        callback.getActivity().runOnUiThread { callback.onHomePageSearchBarLocated(searchBarX, searchBarY) }
    }

    @JavascriptInterface
    fun onHomePageLoaded() {
        if (!isHomePage()) return
        val callback = webEngine.callback ?: return
        callback.getActivity().runOnUiThread {
            val cfg = AppContext.provideConfig()
            val jsArr = JSONArray()
            for (item in callback.getHomePageLinks()) {
                jsArr.put(item.toJsonObj())
            }
            var links = jsArr.toString()
            links = links.replace("'", "\\'")
            webEngine.evaluateJavascript("renderLinks('${cfg.homePageLinksMode.name}', $links)")
            webEngine.evaluateJavascript(
                "applySearchEngine(${JSONObject.quote(cfg.guessSearchEngineName())}, ${JSONObject.quote(cfg.searchEngineURL.value)})")
        }
    }

    @JavascriptInterface
    fun takeBlobDownloadData(base64BlobData: String, fileName: String?, url: String, mimetype: String) {
        //the interface is bound to every frame of every page: only a page of a site may start a download
        //(a file:, content: or about: page must not), and the data is bounded so a page cannot exhaust memory
        val pageUrl = webEngine.tab.url
        if (!pageUrl.startsWith("http://") && !pageUrl.startsWith("https://")) return
        if (base64BlobData.length > MAX_BLOB_BASE64_LENGTH) return
        val callback = webEngine.callback ?: return
        val finalFileName = DownloadUtils.safeFileName(fileName ?: DownloadUtils.guessFileName(url, null, mimetype))
        callback.onDownloadRequested(DownloadRequest(url = url, referer = webEngine.tab.url, userAgent = "Simple Browser",
            fileName = finalFileName, mimeType = mimetype, base64BlobData = base64BlobData))
    }

    @JavascriptInterface
    fun markBookmarkRecommendationAsUseful(bookmarkOrder: Int) {
        if (!isHomePage()) return
        val callback = webEngine.callback ?: return
        callback.getActivity().runOnUiThread { callback.markBookmarkRecommendationAsUseful(bookmarkOrder) }
    }

    @JavascriptInterface
    fun requestSoftInput() {
        val callback = webEngine.callback ?: return
        val activity = callback.getActivity()
        activity.runOnUiThread {
            val view = webEngine.getView()
            val webView = (view as? CursorLayout)?.let {
                it.notifySoftInputRequested()
                (0 until it.childCount).map { i -> it.getChildAt(i) }.filterIsInstance<WebViewEx>().firstOrNull()
            } ?: view as? WebViewEx
            webView?.requestFocus()
            val imm = activity.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
            if (webView != null) {
                imm?.showSoftInput(webView, 0)
            }
        }
    }

    @JavascriptInterface
    fun onInputBlurred() {
        val callback = webEngine.callback ?: return
        val activity = callback.getActivity()
        activity.runOnUiThread {
            val view = webEngine.getView()
            (view as? CursorLayout)?.notifySoftInputBlurred()
        }
    }

    private fun isHomePage(): Boolean {
        return webEngine.tab.url == Config.HOME_PAGE_URL || webEngine.tab.url == Config.HOME_URL_ALIAS
    }
}
