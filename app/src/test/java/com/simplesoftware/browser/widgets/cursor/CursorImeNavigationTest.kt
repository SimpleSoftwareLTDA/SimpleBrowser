package com.simplesoftware.browser.widgets.cursor

import android.view.KeyEvent
import com.simplesoftware.browser.AppContext
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * While software keyboard is requested or active, D-pad navigation keys are not consumed
 * by the cursor adapter, so the on-screen keyboard receives them.
 */
@RunWith(RobolectricTestRunner::class)
class CursorImeNavigationTest {
    private val layout = CursorLayout(AppContext.get())

    @Test
    fun dpadKeysAreYieldedWhenSoftInputIsRequested() {
        val upEvent = KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_UP)

        // Normally, the cursor consumes D-pad navigation keys
        val handledInitially = layout.dispatchKeyEvent(upEvent)
        assertTrue(handledInitially)

        // When soft input is requested, D-pad keys must be yielded to the IME
        layout.notifySoftInputRequested()
        val handledDuringIme = layout.dispatchKeyEvent(upEvent)
        assertFalse(handledDuringIme)

        // When input is blurred, normal cursor navigation resumes
        layout.notifySoftInputBlurred()
        val handledAfterBlur = layout.dispatchKeyEvent(upEvent)
        assertTrue(handledAfterBlur)
    }

    @Test
    fun backKeyResetsSoftInputRequestedState() {
        val upEvent = KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_UP)
        val backEvent = KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_BACK)

        layout.notifySoftInputRequested()
        assertFalse(layout.dispatchKeyEvent(upEvent))

        // Back key dismisses IME and resets soft input requested state
        layout.dispatchKeyEvent(backEvent)
        assertTrue(layout.dispatchKeyEvent(upEvent))
    }
}
