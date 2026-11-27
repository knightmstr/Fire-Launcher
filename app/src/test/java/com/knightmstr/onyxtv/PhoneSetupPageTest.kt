package com.knightmstr.onyxtv

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * The phone page shows app names, which an installed app (or the rename box) controls. Guard
 * against it ever building HTML from that data: no HTML-injecting DOM APIs, no eval, no outside
 * scripts or styles.
 */
class PhoneSetupPageTest {

    private val page: String = File("src/main/assets/phone-setup.html").readText()

    @Test
    fun `page never builds HTML from strings`() {
        for (sink in listOf("innerHTML", "outerHTML", "insertAdjacentHTML", "document.write", "eval(", "new Function")) {
            assertFalse("page uses $sink", page.contains(sink))
        }
    }

    @Test
    fun `page loads nothing from other sites`() {
        assertFalse(Regex("""(src|href)\s*=\s*["']?(https?:)?//""").containsMatchIn(page))
        assertFalse(page.contains("@import"))
    }

    @Test
    fun `page sends every call with the session token`() {
        assertTrue(page.contains("'t=' + encodeURIComponent(token)"))
        assertTrue(page.contains("name=\"referrer\" content=\"no-referrer\""))
    }
}
