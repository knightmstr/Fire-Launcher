package com.knightmstr.firelauncher

import com.knightmstr.firelauncher.update.ReleaseInfo
import com.knightmstr.firelauncher.update.UpdateVersions
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdateVersionsTest {

    @Test
    fun `version codes match the release workflow formula`() {
        assertEquals(10100, UpdateVersions.versionCodeFromTag("v1.1.0"))
        assertEquals(10203, UpdateVersions.versionCodeFromTag("v1.2.3"))
        assertEquals(20000, UpdateVersions.versionCodeFromTag("2.0"))
        assertEquals(10200, UpdateVersions.versionCodeFromTag("v1.2.0-beta1"))
    }

    @Test
    fun `non-semver tags are ignored`() {
        assertNull(UpdateVersions.versionCodeFromTag("v1_x"))
        assertNull(UpdateVersions.versionCodeFromTag(""))
        assertNull(UpdateVersions.versionCodeFromTag("v1.100.0"))
    }

    @Test
    fun `only strictly newer releases are offered`() {
        val release = release(versionCode = 10200)
        assertTrue(UpdateVersions.isNewer(release, 10100))
        assertFalse(UpdateVersions.isNewer(release, 10200))
        assertFalse(UpdateVersions.isNewer(release, 10300))
    }

    @Test
    fun `checksum is found for the right file in sha256sum output`() {
        val hashA = "a".repeat(64)
        val hashB = "B".repeat(64)
        val output = """
            $hashA  FireLauncher-v1.2.0-debug.apk
            $hashB  FireLauncher-v1.2.0-release.apk
        """.trimIndent()
        assertEquals("b".repeat(64), UpdateVersions.checksumFor(output, "FireLauncher-v1.2.0-release.apk"))
        assertEquals(hashA, UpdateVersions.checksumFor(output, "FireLauncher-v1.2.0-debug.apk"))
        assertNull(UpdateVersions.checksumFor(output, "missing.apk"))
        assertNull(UpdateVersions.checksumFor("nothex  x.apk", "x.apk"))
    }

    @Test
    fun `github release notes become plain lines`() {
        val body = """
            ## What's Changed
            * Fix long-press, smaller cards, All Apps grid by @knightmstr in https://github.com/knightmstr/Fire-Launcher/pull/5
            * **Weather** picker works with the [remote](https://example.com)

            ## New Contributors
            * @someone made their first contribution in https://github.com/x/y/pull/1

            **Full Changelog**: https://github.com/knightmstr/Fire-Launcher/compare/v1.2.0...v1.2.1
        """.trimIndent()
        assertEquals(
            "• Fix long-press, smaller cards, All Apps grid\n• Weather picker works with the remote",
            UpdateVersions.cleanNotes(body)
        )
        assertEquals("", UpdateVersions.cleanNotes("**Full Changelog**: https://x"))
    }

    private fun release(versionCode: Int) = ReleaseInfo(
        tag = "v1.2.0", versionName = "1.2.0", versionCode = versionCode, notes = "",
        apkName = "FireLauncher-v1.2.0-release.apk", apkUrl = "https://example.com/a.apk",
        apkSizeBytes = 1, checksumsUrl = null
    )
}
