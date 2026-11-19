package com.alvand.player

import com.alvand.player.library.Sha256
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * بردارهای آزمون رسمی FIPS 180-2 / NIST برای SHA-256.
 *
 * این تست عمداً وجود دارد چون پیاده‌سازی SHA-256 دستی است: اگر کسی الگوریتم را
 * خراب کند، آیدی همهٔ آهنگ‌ها عوض می‌شود و علاقه‌مندی‌ها و پلی‌لیست‌های کاربر
 * بی‌صدا از کار می‌افتند. پس باید دقیق بودنش قفل شود.
 */
class Sha256Test {

    @Test
    fun `empty string`() {
        assertEquals(
            "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
            Sha256.digestHex("".encodeToByteArray())
        )
    }

    @Test
    fun `abc`() {
        assertEquals(
            "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad",
            Sha256.digestHex("abc".encodeToByteArray())
        )
    }

    @Test
    fun `two block message`() {
        assertEquals(
            "248d6a61d20638b8e5c026930c3e6039a33ce45964ff2167f6ecedd419db06c1",
            Sha256.digestHex(
                "abcdbcdecdefdefgefghfghighijhijkijkljklmklmnlmnomnopnopq".encodeToByteArray()
            )
        )
    }

    @Test
    fun `one million a characters`() {
        val million = ByteArray(1_000_000) { 'a'.code.toByte() }
        assertEquals(
            "cdc76e5c9914fb9281a1c7e284d73e67f1809a48a497200e046d39ccc7112cd0",
            Sha256.digestHex(million)
        )
    }

    @Test
    fun `digest is 32 bytes`() {
        assertEquals(32, Sha256.digest("x".encodeToByteArray()).size)
    }
}
