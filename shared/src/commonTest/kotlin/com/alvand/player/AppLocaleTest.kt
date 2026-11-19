package com.alvand.player

import com.alvand.player.data.AppLocale
import kotlin.test.Test
import kotlin.test.assertEquals

class AppLocaleTest {

    @Test
    fun `supported locales are english and persian`() {
        assertEquals(listOf("en", "fa"), AppLocale.all.map { it.code })
    }

    @Test
    fun `normalize strips region`() {
        assertEquals("fa", AppLocale.normalize("fa-IR"))
        assertEquals("en", AppLocale.normalize("en-US"))
    }

    @Test
    fun `normalize falls back to en`() {
        assertEquals("en", AppLocale.normalize("xx"))
        assertEquals("en", AppLocale.normalize(""))
        assertEquals("en", AppLocale.normalize("zh-Hans-CN"))
    }

    @Test
    fun `normalize handles underscore separator`() {
        assertEquals("fa", AppLocale.normalize("fa_IR"))
    }

    @Test
    fun `currentTag picks the first supported base tag`() {
        assertEquals("fa", AppLocale.currentTag("fa-IR,en-US"))
        assertEquals("en", AppLocale.currentTag("en-US,fa-IR"))
        assertEquals("en", AppLocale.currentTag(""))
        assertEquals("en", AppLocale.currentTag("de-DE,fr-FR"))
    }
}
