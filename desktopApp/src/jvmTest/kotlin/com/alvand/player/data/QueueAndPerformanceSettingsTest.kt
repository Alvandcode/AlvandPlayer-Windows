package com.alvand.player.data

import kotlinx.coroutines.test.runTest
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class QueueAndPerformanceSettingsTest {

    private lateinit var dir: java.nio.file.Path
    private lateinit var file: java.nio.file.Path

    @BeforeTest
    fun setUp() {
        dir = createTempDirectory("alvand-queue-test")
        file = dir.resolve("alvand_settings.properties")
    }

    @AfterTest
    fun tearDown() {
        runCatching { dir.toFile().deleteRecursively() }
    }

    @Test
    fun `queue snapshot and performance survive restart`() = runTest {
        val first = SettingsStore(file)
        assertEquals("", first.current.queueSnapshot)
        assertFalse(first.current.performanceMode)

        first.setQueueSnapshot("v1|0|1|C:/m/a.mp3")
        first.setPerformanceMode(true)
        first.flush()

        val second = SettingsStore(file)
        assertEquals("v1|0|1|C:/m/a.mp3", second.current.queueSnapshot)
        assertTrue(second.current.performanceMode)
    }

    @Test
    fun `corrupt queue snapshot does not break other settings`() = runTest {
        val first = SettingsStore(file)
        first.setThemeMode(2)
        first.setQueueSnapshot("v1|0|1|C:/m/a.mp3")
        first.flush()

        // دستکاری دستی فایل — queue_snapshot خراب ولی بقیه سالم
        val text = file.toFile().readText()
        file.toFile().writeText(text.replace("v1|0|1", "v9|xx|yy"))
        val second = SettingsStore(file)
        // تم باید سالم مانده باشد (برگشت کامل به پیش‌فرض فقط وقتی کل فایل خراب است)
        assertEquals(2, second.current.themeMode)
    }
}
