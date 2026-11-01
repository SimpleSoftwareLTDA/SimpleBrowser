package com.simplesoftware.browser.browser

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import com.simplesoftware.browser.model.BrowserTab
import com.simplesoftware.browser.webengine.EngineUiHost
import java.lang.reflect.Proxy

class FakeTabViewHost(context: Context) : BrowserController.TabViewHost {
    override val tabsContainer: ViewGroup = FrameLayout(context)
    var canCreateViews = true
    /** Calls of [EngineUiHost] methods by name */
    val uiHostCalls = ArrayList<String>()

    override fun createTabView(tab: BrowserTab): View? =
        if (canCreateViews) tab.webEngine.getOrCreateView(tabsContainer.context) else null

    override fun uiHost(tab: BrowserTab): EngineUiHost =
        Proxy.newProxyInstance(javaClass.classLoader, arrayOf(EngineUiHost::class.java)) { _, method, _ ->
            uiHostCalls += method.name
            null
        } as EngineUiHost
}
