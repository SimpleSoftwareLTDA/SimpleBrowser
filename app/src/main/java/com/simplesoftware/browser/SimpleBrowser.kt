package com.simplesoftware.browser

import android.app.Activity
import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatDelegate
import com.simplesoftware.browser.browser.ProxySetup
import com.simplesoftware.browser.browser.adblock.AppEngineAdBlocker
import com.simplesoftware.browser.webengine.WebEngineFactory
import com.simplesoftware.browser.activity.main.MainActivity
import com.simplesoftware.browser.singleton.FaviconsPool
import com.simplesoftware.browser.utils.UpdateChecker
import com.simplesoftware.browser.utils.activemodel.ActiveModelsRepository
import com.simplesoftware.browser.webengine.webview.WebViewWebEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.net.CookieHandler
import java.net.CookieManager
import kotlin.system.exitProcess

/**
 * Created by PDT on 09.09.2016.
 */
class SimpleBrowser : Application(), Application.ActivityLifecycleCallbacks {
    companion object {
        lateinit var instance: SimpleBrowser
            private set

        /** Application-wide objects of this process */
        val container: AppContainer
            get() = instance.container

        /**
         * Exits the process (after the pending tab writes) when MainActivity is destroyed next time,
         * and starts it again if [restartMainActivity].
         */
        fun exitProcessAfterMainActivityFinish(restartMainActivity: Boolean = false) {
            instance.needToExitProcessAfterMainActivityFinish = true
            instance.needRestartMainActivityAfterExitingProcess = restartMainActivity
        }

        const val CHANNEL_ID_DOWNLOADS: String = "downloads"
        const val MAIN_PREFS_NAME = "main.xml"
        val TAG = SimpleBrowser::class.simpleName
    }

    lateinit var container: AppContainer
        private set

    private var needToExitProcessAfterMainActivityFinish = false
    private var needRestartMainActivityAfterExitingProcess = false
    override fun onCreate() {
        Log.i(TAG, "onCreate")
        super.onCreate()

        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
            //we need this since when targetSdkVersion >= 33 then
            //deprecated WebSettingsCompat.setForceDark stops working on android 9 and below
            applicationInfo.targetSdkVersion = 32
        }

        instance = this
        val process = AppProcesses.currentKind(this)
        if (process == AppProcesses.Kind.OTHER) {
            //a process of GeckoView: nothing of the app runs there (an isolated one may not even read prefs),
            //and each of them would load its own ad blocker and update its lists
            Log.i(TAG, "onCreate: not a process of the app, nothing to initialize")
            return
        }
        container = AppContainer(this)

        AppContext.init(this, Config(getSharedPreferences(MAIN_PREFS_NAME, MODE_MULTI_PROCESS),
            getSharedPreferences(Config.CREDENTIALS_PREFS_NAME, MODE_MULTI_PROCESS)))

        val downloadsProcess = process == AppProcesses.Kind.DOWNLOADS
        ProxySetup.start(AppContext.provideConfig(), browserProcess = !downloadsProcess)

        if (downloadsProcess) {
            //the process of DownloadService needs the database and the notification channel only
            initNotificationChannels()
            return
        }

        initWebEngineStuff()
        initAdBlocking()

        clearUpdateFilesAfterAppUpdate()

        initNotificationChannels()

        ActiveModelsRepository.init(this)

        when (AppContext.provideConfig().theme.value) {
            Config.Theme.BLACK -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            Config.Theme.WHITE -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            else -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        }

        registerActivityLifecycleCallbacks(this)
    }

    private fun clearUpdateFilesAfterAppUpdate() {
        val config = AppContext.provideConfig()
        if (config.appVersionCodeMark == BuildConfig.VERSION_CODE) return
        Log.i(TAG, "App version code changed from ${config.appVersionCodeMark} to ${BuildConfig.VERSION_CODE}")
        config.appVersionCodeMark = BuildConfig.VERSION_CODE
        container.appScope.launch(Dispatchers.IO) {
            UpdateChecker.clearTempFilesIfAny(this@SimpleBrowser)
        }
    }

    /** The engines of all tabs ask the ad blocker; the lists are not updated in unit tests (Robolectric) */
    private fun initAdBlocking() {
        val config = AppContext.provideConfig()
        container.adBlock.start(scheduleUpdates = Build.FINGERPRINT != "robolectric")
        WebEngineFactory.adBlocker = AppEngineAdBlocker(container.adBlock, container.data.hostConfigs, config)
    }

    private fun initWebEngineStuff() {
        Log.i(TAG, "initWebEngineStuff")

        //the engine of this variant of the app registers itself in WebEngineFactory
        try {
            Class.forName(BuildConfig.WEB_ENGINE_CLASS)
        } catch (e: ClassNotFoundException) {
            throw AssertionError(e)
        }

        val cookieManager = CookieManager()
        CookieHandler.setDefault(cookieManager)
        FaviconsPool.databaseDelegate = object : FaviconsPool.DatabaseDelegate {
            override suspend fun findFavicon(host: String): String? =
                container.data.hostConfigs.find(host)?.favicon

            override suspend fun setFavicon(host: String, fileName: String) {
                container.data.hostConfigs.setFavicon(host, fileName)
            }
        }
    }

    private fun initNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = getString(R.string.downloads)
            val descriptionText = getString(R.string.downloads_notifications_description)
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID_DOWNLOADS, name, importance)
            channel.description = descriptionText
            val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
    override fun onActivityStarted(activity: Activity) {}
    override fun onActivityResumed(activity: Activity) {}
    override fun onActivityPaused(activity: Activity) {}
    override fun onActivityStopped(activity: Activity) {}
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}

    override fun onActivityDestroyed(activity: Activity) {
        Log.i(TAG, "onActivityDestroyed: " + activity.javaClass.simpleName)
        if (needToExitProcessAfterMainActivityFinish && activity is MainActivity) {
            container.appScope.launch {
                //tabs state saved on activity pause should not be lost
                container.tabWrites.awaitPending()
                Log.i(TAG, "onActivityDestroyed: exiting process")
                if (needRestartMainActivityAfterExitingProcess) {
                    Log.i(TAG, "onActivityDestroyed: restarting main activity")
                    val intent = Intent(this@SimpleBrowser, MainActivity::class.java)
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                    startActivity(intent)
                }
                exitProcess(0)
            }
        }
    }
}
