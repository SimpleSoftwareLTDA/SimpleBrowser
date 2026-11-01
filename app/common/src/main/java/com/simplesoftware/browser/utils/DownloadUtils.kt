/* -*- Mode: Java; c-basic-offset: 4; tab-width: 20; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * This file incorporates work covered by the following copyright and
 * permission notice:
 *
 *   Copyright (C) 2006 The Android Open Source Project
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License. */

package com.simplesoftware.browser.utils

import android.net.Uri
import android.webkit.MimeTypeMap

import java.io.ByteArrayOutputStream
import java.io.UnsupportedEncodingException
import java.util.regex.Pattern

object DownloadUtils {

    /**
     * Format as defined in RFC 2616 and RFC 5987 / RFC 6266.
     * Both attachment and inline disposition types are supported.
     */
    private val CONTENT_DISPOSITION_PATTERN = Pattern.compile("(?:attachment|inline)\\s*;\\s*filename\\s*=\\s*" +
            "(\"((?:\\\\.|[^\"\\\\])*)\"|[^;]*)\\s*" +
            "(?:;\\s*filename\\*\\s*=\\s*(utf-8|iso-8859-1)'[^']*'(\\S*))?",
            Pattern.CASE_INSENSITIVE)

    /**
     * Definition as per RFC 5987, section 3.2.1. (value-chars)
     */
    private val ENCODED_SYMBOL_PATTERN = Pattern.compile("%[0-9a-f]{2}|[0-9a-z!#$&+-.^_`|~]", Pattern.CASE_INSENSITIVE)

    /**
     * Guess the name of the file that should be downloaded.
     *
     * This method is largely identical to [android.webkit.URLUtil.guessFileName]
     * which unfortunately does not implement RfC 5987.
     */
    fun guessFileName(url: String,
                      contentDisposition: String?,
                      mimeType: String?): String {
        var filename: String? = null
        var extension: String? = null

        // Extract file name from content disposition header field
        if (contentDisposition != null) {
            filename = parseContentDisposition(contentDisposition)
            if (filename != null) {
                val index = filename.lastIndexOf('/') + 1
                if (index > 0) {
                    filename = filename.substring(index)
                }
            }
        }

        // If all the other http-related approaches failed, use the plain uri
        if (filename == null) {
            var decodedUrl: String? = Uri.decode(url)
            if (decodedUrl != null) {
                val queryIndex = decodedUrl.indexOf('?')
                // If there is a query string strip it, same as desktop browsers
                if (queryIndex > 0) {
                    decodedUrl = decodedUrl.substring(0, queryIndex)
                }
                if (!decodedUrl.endsWith("/")) {
                    val index = decodedUrl.lastIndexOf('/') + 1
                    if (index > 0) {
                        filename = decodedUrl.substring(index)
                    }
                }
            }
        }

        // Finally, if couldn't get filename from URI, get a generic filename
        if (filename == null) {
            filename = "downloadfile"
        }

        // Split filename between base and extension
        // Add an extension if filename does not have one
        val dotIndex = filename.indexOf('.')
        if (dotIndex < 0) {
            if (mimeType != null) {
                extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType)
                if (extension != null) {
                    extension = ".$extension"
                }
            }
            if (extension == null) {
                if (mimeType != null && mimeType.lowercase().startsWith("text/")) {
                    if (mimeType.equals("text/html", ignoreCase = true)) {
                        extension = ".html"
                    } else {
                        extension = ".txt"
                    }
                } else {
                    extension = ".bin"
                }
            }
        } else {
            if (mimeType != null && "application/octet-stream" != mimeType) {
                // Compare the last segment of the extension against the mime type.
                // If there's a mismatch, discard the entire extension.
                val lastDotIndex = filename.lastIndexOf('.')
                val typeFromExt = MimeTypeMap.getSingleton().getMimeTypeFromExtension(
                        filename.substring(lastDotIndex + 1))
                if (typeFromExt != null && !typeFromExt.equals(mimeType, ignoreCase = true)) {
                    extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType)
                    if (extension != null) {
                        extension = ".$extension"
                    }
                }
            }
            if (extension == null) {
                extension = filename.substring(dotIndex)
            }
            filename = filename.substring(0, dotIndex)
        }

        return filename + extension
    }

    private fun parseContentDisposition(contentDisposition: String): String? {
        try {
            val m = CONTENT_DISPOSITION_PATTERN.matcher(contentDisposition)

            if (m.find()) {
                // If escaped string is found, decode it using the given encoding.
                val encodedFileName = m.group(4)
                val encoding = m.group(3)

                if (encodedFileName != null) {
                    return decodeHeaderField(encodedFileName, encoding)
                }

                // Return quoted string if available and replace escaped characters.
                val quotedFileName = m.group(2)

                return quotedFileName?.replace("\\\\(.)".toRegex(), "$1") ?: m.group(1)

                // Otherwise try to extract the unquoted file name
            }
        } catch (ex: IllegalStateException) {
            // This function is defined as returning null when it can't parse the header
        } catch (ex: UnsupportedEncodingException) {
        }

        return null
    }

    @Throws(UnsupportedEncodingException::class)
    private fun decodeHeaderField(field: String, encoding: String): String {
        val m = ENCODED_SYMBOL_PATTERN.matcher(field)
        val stream = ByteArrayOutputStream()

        while (m.find()) {
            val symbol = m.group()

            if (symbol.startsWith("%")) {
                stream.write(Integer.parseInt(symbol.substring(1), 16))
            } else {
                stream.write(symbol[0].toInt())
            }
        }

        return stream.toString(encoding)
    }

    private const val MAX_FILE_NAME_BYTES = 120
    private const val DEFAULT_FILE_NAME = "file"

    /**
     * A name safe to be a file name written by the app: no folders (`../`), no control characters,
     * no characters FAT drives refuse, not hidden and not too long (its extension kept). Names come
     * from the page (the `download` attribute of a link, a `Content-Disposition` header), so they are
     * never trusted.
     */
    fun safeFileName(name: String?): String {
        val base = (name ?: "").substringAfterLast('/').substringAfterLast('\\')
        val clean = base.filter { it >= ' ' && it != '\u007f' && it !in "<>:\"|?*" }.trim().trimStart('.').trim()
        if (clean.toByteArray(Charsets.UTF_8).size <= MAX_FILE_NAME_BYTES) return clean.ifEmpty { DEFAULT_FILE_NAME }
        val ext = clean.substringAfterLast('.', "").takeIf { it.isNotEmpty() && it.length <= 16 }
        val suffix = if (ext != null) ".$ext" else ""
        val stem = if (ext != null) clean.substring(0, clean.length - suffix.length) else clean
        val head = takeUtf8Bytes(stem, MAX_FILE_NAME_BYTES - suffix.toByteArray(Charsets.UTF_8).size).trim()
        return (head + suffix).ifEmpty { DEFAULT_FILE_NAME }
    }

    /** The beginning of [text] that has at most [max] bytes of UTF-8, whole code points only */
    private fun takeUtf8Bytes(text: String, max: Int): String {
        var bytes = 0
        var end = 0
        while (end < text.length) {
            val codePoint = text.codePointAt(end)
            val size = when {
                codePoint < 0x80 -> 1
                codePoint < 0x800 -> 2
                codePoint < 0x10000 -> 3
                else -> 4
            }
            if (bytes + size > max) break
            bytes += size
            end += Character.charCount(codePoint)
        }
        return text.substring(0, end)
    }

}
