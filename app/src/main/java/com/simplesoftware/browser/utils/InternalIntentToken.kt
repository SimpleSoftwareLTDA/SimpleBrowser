package com.simplesoftware.browser.utils

import android.content.Context
import java.io.File
import java.security.SecureRandom
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * Signs the values the app puts in the intents it sends to itself, so an app of another UID cannot forge
 * them. [MainActivity] uses it for the process it is asked to kill: the activity is exported (it is the
 * browser) and an extra it trusted from anyone would let any app kill the app's own processes.
 *
 * The secret is made on first use in `noBackupFilesDir`: a backup restored on another device carries no
 * secret, so an old link is not valid there.
 */
object InternalIntentToken {
    private const val SECRET_FILE = "internal_intents.key"
    private const val SECRET_SIZE = 32

    /** The signature of [value] for [purpose] */
    fun token(context: Context, purpose: String, value: Long): String {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(secret(context), "HmacSHA256"))
        return mac.doFinal("${context.packageName}:$purpose:$value".toByteArray())
            .joinToString("") { "%02x".format(it) }
    }

    /** Whether [token] is the signature of [value] for [purpose] */
    fun isValid(context: Context, purpose: String, value: Long, token: String?): Boolean {
        if (token == null) return false
        val expected = token(context, purpose, value)
        return expected.length == token.length && expected == token
    }

    /**
     * The secret, made on first use. A file and not a pref: the processes of the app keep their prefs in
     * memory, and one of them could make a second secret.
     */
    @Synchronized
    private fun secret(context: Context): ByteArray {
        val file = File(context.noBackupFilesDir, SECRET_FILE)
        if (file.length() == SECRET_SIZE.toLong()) return file.readBytes()
        val temp = File(context.noBackupFilesDir, "$SECRET_FILE.${android.os.Process.myPid()}.tmp")
        temp.writeBytes(ByteArray(SECRET_SIZE).also { SecureRandom().nextBytes(it) })
        //another process may have made it meanwhile: the first one stays
        if (file.length() != SECRET_SIZE.toLong()) temp.renameTo(file) else temp.delete()
        return file.readBytes()
    }
}
