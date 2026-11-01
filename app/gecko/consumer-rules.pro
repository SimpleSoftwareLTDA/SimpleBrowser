-keep class com.simplesoftware.browser.webengine.gecko.GeckoWebEngine { *; }

-keepclassmembers class org.mozilla.geckoview.** {
    *** mDisplay;
    *** sTreatMouseAsTouch;
}
