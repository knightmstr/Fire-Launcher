package com.knightmstr.onyxtv

import com.knightmstr.onyxtv.home.Platform
import com.knightmstr.onyxtv.home.Platform.Kind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PlatformTest {

    @Test
    fun `fire tv is detected every way it can be`() {
        assertEquals(Kind.FIRE_TV, Platform.kindFrom("Amazon", false, emptySet()))
        assertEquals(Kind.FIRE_TV, Platform.kindFrom("amazon", false, emptySet()))
        assertEquals(Kind.FIRE_TV, Platform.kindFrom("Other", true, emptySet()))
        assertEquals(Kind.FIRE_TV, Platform.kindFrom(null, false, setOf(Platform.AMAZON_HOME)))
        // A Fire TV wins even if a Google launcher were somehow installed
        assertEquals(Kind.FIRE_TV, Platform.kindFrom("Amazon", false, setOf(Platform.GOOGLE_TV_HOME)))
    }

    @Test
    fun `google tv and android tv`() {
        assertEquals(Kind.GOOGLE_TV, Platform.kindFrom("Sony", false, setOf(Platform.GOOGLE_TV_HOME)))
        assertEquals(Kind.GOOGLE_TV, Platform.kindFrom("Google", false, setOf(Platform.GOOGLE_TV_HOME, "com.knightmstr.onyxtv")))
        assertEquals(Kind.ANDROID_TV, Platform.kindFrom("Sony", false, setOf(Platform.ANDROID_TV_HOME)))
        assertEquals(Kind.ANDROID_TV, Platform.kindFrom(null, false, emptySet()))
    }

    @Test
    fun `fire tv wording is unchanged`() {
        val n = Platform.namesFor(Kind.FIRE_TV)
        assertEquals("Fire TV", n.device)
        assertEquals("Amazon's", n.owner)
        assertTrue(n.developerOptionsHint.contains("My Fire TV"))
    }

    @Test
    fun `google tv wording`() {
        val n = Platform.namesFor(Kind.GOOGLE_TV)
        assertEquals("Google TV", n.device)
        assertEquals("Google's", n.owner)
        assertTrue(!n.developerOptionsHint.contains("Fire"))
    }

    @Test
    fun `the accessibility config lists exactly the stock homes`() {
        val xml = java.io.File("src/main/res/xml/home_watch.xml").readText()
        val listed = Regex("""android:packageNames="([^"]+)"""").find(xml)!!.groupValues[1].split(',').map { it.trim() }.toSet()
        assertEquals(Platform.STOCK_HOMES, listed)
    }

    @Test
    fun `the watcher knows every stock home`() {
        assertEquals(
            setOf("com.amazon.tv.launcher", "com.google.android.apps.tv.launcherx",
                "com.google.android.tvlauncher", "com.google.android.leanbacklauncher"),
            Platform.STOCK_HOMES
        )
    }
}
