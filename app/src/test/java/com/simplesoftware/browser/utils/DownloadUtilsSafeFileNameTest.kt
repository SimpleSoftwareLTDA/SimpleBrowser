package com.simplesoftware.browser.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Names come from pages (the `download` attribute of a link, a `Content-Disposition` header), so the
 * sanitiser is what keeps a page from choosing a folder or an odd name for a file of the app.
 */
@RunWith(RobolectricTestRunner::class)
class DownloadUtilsSafeFileNameTest {
    @Test
    fun keepsAPlainName() {
        assertEquals("video.mp4", DownloadUtils.safeFileName("video.mp4"))
    }

    @Test
    fun dropsFolders() {
        assertEquals("passwd", DownloadUtils.safeFileName("../../etc/passwd"))
        assertEquals("file.mp4", DownloadUtils.safeFileName("..\\..\\file.mp4"))
    }

    @Test
    fun dropsControlAndForbiddenCharacters() {
        assertEquals("ab.webp", DownloadUtils.safeFileName("a\u0000b\u001f.webp"))
        assertEquals("name.webp", DownloadUtils.safeFileName("na<me>:\"|?*.webp"))
    }

    @Test
    fun dropsTheLeadingDots() {
        assertEquals("hidden.webp", DownloadUtils.safeFileName("...hidden.webp"))
    }

    @Test
    fun keepsTheExtensionWhenTheNameIsTooLong() {
        val long = "a".repeat(300) + ".webp"
        val name = DownloadUtils.safeFileName(long)
        assertTrue(name.length <= 120)
        assertTrue(name.endsWith(".webp"))
    }

    @Test
    fun fallsBackForAnEmptyName() {
        assertEquals("file", DownloadUtils.safeFileName(null))
        assertEquals("file", DownloadUtils.safeFileName(""))
        assertEquals("file", DownloadUtils.safeFileName("..."))
        assertEquals("file", DownloadUtils.safeFileName("../"))
    }

    @Test
    fun neverReturnsAPathOfAnotherFolder() {
        val name = DownloadUtils.safeFileName("/sdcard/Download/../../data/x.apk")
        assertFalse(name.contains('/'))
        assertFalse(name.startsWith("."))
        assertEquals("x.apk", name)
    }
}
