package com.alvand.player.system

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class MediaKeyMapTest {

    @Test
    fun `vk values match the Microsoft WinUser header`() {
        assertEquals(0xB0, MediaKeyMap.VK_MEDIA_NEXT_TRACK)
        assertEquals(0xB1, MediaKeyMap.VK_MEDIA_PREV_TRACK)
        assertEquals(0xB2, MediaKeyMap.VK_MEDIA_STOP)
        assertEquals(0xB3, MediaKeyMap.VK_MEDIA_PLAY_PAUSE)
        assertEquals(0x0312, MediaKeyMap.WM_HOTKEY)
    }

    @Test
    fun `mapping is a bijection`() {
        for (action in MediaKeyMap.Action.values()) {
            val vk = MediaKeyMap.vkFor(action)
            assertEquals(action, MediaKeyMap.actionFor(vk))
        }
    }

    @Test
    fun `unknown vk maps to null`() {
        assertNull(MediaKeyMap.actionFor(0x41))
        assertNull(MediaKeyMap.actionFor(-1))
    }

    @Test
    fun `hotkey id roundtrip is stable`() {
        for (vk in MediaKeyMap.allKeys) {
            assertEquals(vk, MediaKeyMap.hotkeyIdToVk(MediaKeyMap.hotkeyIdOf(vk)))
        }
        // هیچ دو کلیدی آیدی مشترک ندارد — وگرنه دکمهٔ اشتباهی اکشن می‌گیرد
        val ids = MediaKeyMap.allKeys.map { MediaKeyMap.hotkeyIdOf(it) }
        assertEquals(ids.size, ids.distinct().size)
    }
}
