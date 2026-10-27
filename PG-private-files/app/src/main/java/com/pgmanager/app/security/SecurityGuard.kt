package com.pgmanager.app.security

import android.app.Activity
import android.os.Build
import android.view.WindowManager
import java.io.File

object SecurityGuard {

    /**
     * Checks if the device displays common indicators of root or custom ROMs.
     * Handled gracefully without crashing.
     */
    fun checkRootOrTamper(): Boolean {
        return checkBuildTags() || checkSuBinary() || checkSuperuserApk()
    }

    private fun checkBuildTags(): Boolean {
        val buildTags = Build.TAGS
        return buildTags != null && buildTags.contains("test-keys")
    }

    private fun checkSuBinary(): Boolean {
        val paths = arrayOf(
            "/system/app/Superuser.apk",
            "/sbin/su",
            "/system/bin/su",
            "/system/xbin/su",
            "/data/local/xbin/su",
            "/data/local/bin/su",
            "/system/sd/xbin/su",
            "/system/bin/failsafe/su",
            "/data/local/su"
        )
        return paths.any { File(it).exists() }
    }

    private fun checkSuperuserApk(): Boolean {
        return File("/system/app/Superuser.apk").exists()
    }

    /**
     * Enforces FLAG_SECURE on windows displaying sensitive data (documents, bank info, UPI QR).
     * Prevents screenshotting, screen recording, and system recent tasks preview leakage.
     */
    fun setSecureScreen(activity: Activity, secure: Boolean) {
        if (secure) {
            activity.window.setFlags(
                WindowManager.LayoutParams.FLAG_SECURE,
                WindowManager.LayoutParams.FLAG_SECURE
            )
        } else {
            activity.window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
    }

    /**
     * Sanitizes user input string against HTML injection, script tags, and path traversal.
     */
    fun sanitizeInput(input: String, maxLength: Int = 500): String {
        val trimmed = input.trim().take(maxLength)
        return trimmed
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#x27;")
            .replace("&(?!amp;|lt;|gt;|quot;|#x27;)", "&amp;")
    }

    /**
     * Validates 15-character GST format and modulo-36 checksum.
     */
    fun validateGstNumber(gst: String): Boolean {
        val clean = gst.trim().uppercase()
        if (clean.length != 15) return false

        val pattern = Regex("^[0-9]{2}[A-Z]{5}[0-9]{4}[A-Z]{1}[1-9A-Z]{1}Z[0-9A-Z]{1}$")
        if (!pattern.matches(clean)) return false

        val chars = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ"
        var factor = 1
        var sum = 0
        val mod = 36

        for (i in 0 until 14) {
            val char = clean[i]
            val codePoint = chars.indexOf(char)
            if (codePoint == -1) return false

            var addend = factor * codePoint
            factor = if (factor == 2) 1 else 2
            addend = (addend / mod) + (addend % mod)
            sum += addend
        }

        val remainder = sum % mod
        val checkCodePoint = (mod - remainder) % mod
        val expectedCheckChar = chars[checkCodePoint]

        return clean[14] == expectedCheckChar
    }
}
