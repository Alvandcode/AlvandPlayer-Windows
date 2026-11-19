package com.alvand.player

import com.alvand.player.audio.DspUiLogic
import com.alvand.player.audio.DspSpec
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DspUiLogicTest {

    @Test
    fun `preset bands have five entries in range`() {
        val bands = DspUiLogic.bandsForPreset(0)
        assertEquals(5, bands!!.size)
        assertTrue(bands.all { it in DspSpec.MIN_MB..DspSpec.MAX_MB })
    }

    @Test
    fun `manual preset keeps current bands`() {
        assertNull(DspUiLogic.bandsForPreset(-1))
        assertNull(DspUiLogic.bandsForPreset(99))
    }

    @Test
    fun `rock lifts bass and treble`() {
        // rock = [4,3,-1,2,4]dB → باند اول و آخر مثبت‌اند
        val bands = DspUiLogic.bandsForPreset(0)!!
        assertTrue(bands.first() > 0)
        assertTrue(bands.last() > 0)
    }

    @Test
    fun `applyPreset enables eq and sets bands`() {
        val out = DspUiLogic.applyPreset(DspSpec(eqEnabled = false), 5)
        assertTrue(out.eqEnabled)
        assertEquals(5, out.preset)
        assertEquals(5, out.bandLevels.size)
    }

    @Test
    fun ` moving a band switches to manual`() {
        val out = DspUiLogic.withBand(DspSpec(preset = 0), 2, 300)
        assertEquals(-1, out.preset)
        assertEquals(300, out.bandLevels[2])
        assertTrue(out.eqEnabled)
    }

    @Test
    fun `formats carry sign`() {
        assertEquals("+3.0 dB", DspUiLogic.formatBandDb(300))
        assertEquals("-1.5 dB", DspUiLogic.formatBandDb(-150))
        assertEquals("0 dB", DspUiLogic.formatBoostDb(0))
        assertEquals("20Hz", DspUiLogic.formatBandFreq(20.0))
        assertEquals("14.0kHz", DspUiLogic.formatBandFreq(14000.0))
    }
}
