package com.simplesoftware.browser.utils

import org.robolectric.RuntimeEnvironment
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * The token is what keeps another app from making the exported browser activity kill the app's own
 * processes through the extra it reads in onCreate.
 */
@RunWith(RobolectricTestRunner::class)
class InternalIntentTokenTest {
    private val context = RuntimeEnvironment.getApplication()

    @Test
    fun acceptsTheTokenItMade() {
        val token = InternalIntentToken.token(context, "kill_process", 1234L)
        assertTrue(InternalIntentToken.isValid(context, "kill_process", 1234L, token))
    }

    @Test
    fun rejectsAnotherValue() {
        val token = InternalIntentToken.token(context, "kill_process", 1234L)
        assertFalse(InternalIntentToken.isValid(context, "kill_process", 4321L, token))
    }

    @Test
    fun rejectsAnotherPurpose() {
        val token = InternalIntentToken.token(context, "kill_process", 1234L)
        assertFalse(InternalIntentToken.isValid(context, "open_url", 1234L, token))
    }

    @Test
    fun rejectsAMissingOrForgedToken() {
        assertFalse(InternalIntentToken.isValid(context, "kill_process", 1234L, null))
        assertFalse(InternalIntentToken.isValid(context, "kill_process", 1234L, ""))
        assertFalse(InternalIntentToken.isValid(context, "kill_process", 1234L, "0".repeat(64)))
    }

    @Test
    fun theSecretIsKept() {
        val first = InternalIntentToken.token(context, "kill_process", 7L)
        val second = InternalIntentToken.token(context, "kill_process", 7L)
        assertTrue(first == second)
        assertNotEquals(first, InternalIntentToken.token(context, "kill_process", 8L))
    }
}
