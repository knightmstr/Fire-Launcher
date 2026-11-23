package com.knightmstr.onyxtv

import com.knightmstr.onyxtv.update.ReleaseInfo
import com.knightmstr.onyxtv.update.UpdateVersions
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdateVersionsTest {

    @Test
    fun `version codes match the release workflow formula`() {
        // (MAJOR*10000 + MINOR*100 + PATCH) * 1000 + 999 for a stable release.
        assertEquals(10100999, UpdateVersions.versionCodeFromTag("v1.1.0"))
        assertEquals(10203999, UpdateVersions.versionCodeFromTag("v1.2.3"))
        assertEquals(20000999, UpdateVersions.versionCodeFromTag("2.0"))
    }

    @Test
    fun `pre-releases rank below their stable but above the previous version`() {
        val prevStable = UpdateVersions.versionCodeFromTag("v1.5.2")!!
        val beta1 = UpdateVersions.versionCodeFromTag("v1.6.0-beta.1")!!
        val beta2 = UpdateVersions.versionCodeFromTag("v1.6.0-beta.2")!!
        val stable = UpdateVersions.versionCodeFromTag("v1.6.0")!!

        assertEquals(10600001, beta1)
        assertEquals(10600999, stable)
        assertTrue(beta1 < beta2)        // later beta wins
        assertTrue(beta2 < stable)       // final stable promotes beta testers
        assertTrue(prevStable < beta1)   // beta is newer than the last stable
    }

    @Test
    fun `non-semver and malformed pre-release tags are ignored`() {
        assertNull(UpdateVersions.versionCodeFromTag("v1_x"))
        assertNull(UpdateVersions.versionCodeFromTag(""))
        assertNull(UpdateVersions.versionCodeFromTag("v1.100.0"))
        assertNull(UpdateVersions.versionCodeFromTag("v1.2.0-beta1")) // needs the dot: -beta.N
        assertNull(UpdateVersions.versionCodeFromTag("v1.2.0-foo.1")) // unknown pre-release label
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
            $hashA  OnyxTV-v1.2.0-debug.apk
            $hashB  OnyxTV-v1.2.0-release.apk
        """.trimIndent()
        assertEquals("b".repeat(64), UpdateVersions.checksumFor(output, "OnyxTV-v1.2.0-release.apk"))
        assertEquals(hashA, UpdateVersions.checksumFor(output, "OnyxTV-v1.2.0-debug.apk"))
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
        apkName = "OnyxTV-v1.2.0-release.apk", apkUrl = "https://example.com/a.apk",
        apkSizeBytes = 1, checksumsUrl = null
    )
}
