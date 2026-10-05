package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.action.RootExecutor
import com.example.ai.ActionIntent
import com.example.ai.CommandParser
import com.example.ai.GeminiApiClient
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Devil AI", appName)
    }

    @Test
    fun `parse english commands correctly`() = runBlocking {
        val parser = CommandParser(GeminiApiClient())

        val p1 = parser.parseCommand("Open YouTube", "en-US")
        assertTrue(p1.intent is ActionIntent.OpenApp)
        assertEquals("YouTube", (p1.intent as ActionIntent.OpenApp).appName)

        val p2 = parser.parseCommand("Show my device information", "en-US")
        assertTrue(p2.intent is ActionIntent.DeviceInfo)

        val p3 = parser.parseCommand("Turn the brightness down", "en-US")
        assertTrue(p3.intent is ActionIntent.BrightnessControl)

        val p4 = parser.parseCommand("Take a screenshot", "en-US")
        assertTrue(p4.intent is ActionIntent.TakeScreenshot)

        val p5 = parser.parseCommand("Create a folder named Test", "en-US")
        assertTrue(p5.intent is ActionIntent.CreateFolder)
        assertEquals("Test", (p5.intent as ActionIntent.CreateFolder).folderName)
    }

    @Test
    fun `parse bengali commands correctly`() = runBlocking {
        val parser = CommandParser(GeminiApiClient())

        val b1 = parser.parseCommand("ইউটিউব খোলো", "bn-BD")
        assertTrue(b1.intent is ActionIntent.OpenApp)

        val b2 = parser.parseCommand("ব্যাটারি চার্জ কত", "bn-BD")
        assertTrue(b2.intent is ActionIntent.BatteryStatus)

        val b3 = parser.parseCommand("স্ক্রিনশট নাও", "bn-BD")
        assertTrue(b3.intent is ActionIntent.TakeScreenshot)
    }

    @Test
    fun `root destructive command detection`() {
        val rootExecutor = RootExecutor()

        assertTrue(rootExecutor.isDestructiveCommand("rm -rf /"))
        assertTrue(rootExecutor.isDestructiveCommand("reboot"))
        assertTrue(rootExecutor.isDestructiveCommand("shutdown"))
        assertTrue(rootExecutor.isDestructiveCommand("mount -o remount,rw /system"))

        // Safe command
        assertTrue(!rootExecutor.isDestructiveCommand("id"))
        assertTrue(!rootExecutor.isDestructiveCommand("uptime"))
        assertTrue(!rootExecutor.isDestructiveCommand("getprop ro.build.version.release"))
    }
}
