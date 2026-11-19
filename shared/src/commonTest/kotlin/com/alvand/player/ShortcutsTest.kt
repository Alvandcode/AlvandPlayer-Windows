package com.alvand.player

import com.alvand.player.player.ShortcutAction
import com.alvand.player.player.Shortcuts
import kotlin.test.Test
import kotlin.test.assertEquals

class ShortcutsTest {

    @Test
    fun `basic playback keys`() {
        assertEquals(ShortcutAction.TOGGLE_PLAY, Shortcuts.resolve(" "))
        assertEquals(ShortcutAction.NEXT, Shortcuts.resolve("arrowright"))
        assertEquals(ShortcutAction.PREVIOUS, Shortcuts.resolve("arrowleft"))
        assertEquals(ShortcutAction.SEEK_BACK, Shortcuts.resolve("arrowleft", ctrl = true))
        assertEquals(ShortcutAction.SEEK_FORWARD, Shortcuts.resolve("arrowright", ctrl = true))
    }

    @Test
    fun `library and modes`() {
        assertEquals(ShortcutAction.TOGGLE_SHUFFLE, Shortcuts.resolve("s"))
        assertEquals(ShortcutAction.CYCLE_REPEAT, Shortcuts.resolve("r"))
        assertEquals(ShortcutAction.TOGGLE_MUTE, Shortcuts.resolve("m"))
        assertEquals(ShortcutAction.TOGGLE_FAVORITE, Shortcuts.resolve("l"))
        assertEquals(ShortcutAction.OPEN_LIBRARY_SEARCH, Shortcuts.resolve("k", ctrl = true))
        assertEquals(ShortcutAction.CYCLE_SLEEP_TIMER, Shortcuts.resolve("t"))
        assertEquals(ShortcutAction.OPEN_EQUALIZER, Shortcuts.resolve("e"))
        assertEquals(ShortcutAction.SHOW_PROPERTIES, Shortcuts.resolve("i"))
        assertEquals(ShortcutAction.JUMP_TO_TIME, Shortcuts.resolve("j"))
        assertEquals(ShortcutAction.CYCLE_AB_LOOP, Shortcuts.resolve("b"))
        assertEquals(ShortcutAction.TOGGLE_VISUALIZER, Shortcuts.resolve("v"))
    }

    @Test
    fun `typing swallows everything except ctrl-k`() {
        assertEquals(ShortcutAction.NONE, Shortcuts.resolve(" ", isTyping = true))
        assertEquals(ShortcutAction.NONE, Shortcuts.resolve("s", isTyping = true))
        assertEquals(
            ShortcutAction.OPEN_LIBRARY_SEARCH,
            Shortcuts.resolve("k", ctrl = true, isTyping = true)
        )
    }

    @Test
    fun `unknown keys do nothing`() {
        assertEquals(ShortcutAction.NONE, Shortcuts.resolve("f1"))
        assertEquals(ShortcutAction.NONE, Shortcuts.resolve("z", ctrl = true))
    }
}
