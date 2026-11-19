package com.alvand.player.data

import com.alvand.player.data.db.Database
import com.alvand.player.data.db.Migrations
import kotlinx.coroutines.test.runTest
import java.nio.file.Files
import java.sql.SQLException
import java.nio.file.Path
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * دیتابیس و مهاجرت‌ها.
 *
 * سیاست مهم: مهاجرت فقط‌رو‌به‌جلو است. اگر فایل دیتابیس از نسخهٔ این برنامه جدیدتر
 * باشد، نباید باز شود (otherwise داده‌های کاربر بی‌صدا از بین می‌رود).
 */
class DatabaseTest {

    private lateinit var dir: Path

    @BeforeTest
    fun setUp() {
        dir = createTempDirectory("alvand-db-test")
    }

    @AfterTest
    fun tearDown() {
        runCatching { dir.toFile().deleteRecursively() }
    }

    @Test
    fun `fresh database migrates to the latest version`() {
        Database(dir.resolve("a.db")).use { db ->
            assertEquals(0, db.version, "new database should start at version 0")
            db.migrate()
            assertEquals(Migrations.LATEST_VERSION, db.version)
        }
    }

    @Test
    fun `migration creates all three tables`() {
        Database(dir.resolve("b.db")).use { db ->
            db.migrate()
            val names = db.read { c ->
                c.createStatement().use { st ->
                    st.executeQuery(
                        "SELECT name FROM sqlite_master WHERE type='table' ORDER BY name"
                    ).use { rs ->
                        buildList { while (rs.next()) add(rs.getString(1)) }
                    }
                }
            }
            assertTrue(names.contains("playlists"), "missing playlists table: $names")
            assertTrue(names.contains("playlist_songs"), "missing playlist_songs table: $names")
            assertTrue(names.contains("play_history"), "missing play_history table: $names")
        }
    }

    @Test
    fun `migration is idempotent`() {
        val file = dir.resolve("c.db")
        Database(file).use { db ->
            db.migrate()
            db.migrate()
            db.migrate()
            assertEquals(Migrations.LATEST_VERSION, db.version)
        }
    }

    @Test
    fun `older database is migrated forward and keeps its data`() {
        val file = dir.resolve("d.db")

        // شبیه‌سازی دیتابیسی که فقط جدول playlists را دارد (مثل نسخهٔ خیلی قدیمی)
        Database(file).use { db ->
            db.transaction {
                db.read { c ->
                    c.createStatement().use { st ->
                        st.executeUpdate(
                            """
                            CREATE TABLE playlists (
                                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                                name TEXT NOT NULL,
                                createdAt INTEGER NOT NULL
                            )
                            """.trimIndent()
                        )
                        st.executeUpdate(
                            "INSERT INTO playlists (name, createdAt) VALUES ('Old list', 1000)"
                        )
                    }
                }
            }
        }

        // حالا نسخه را دستی عقب می‌بریم تا مسیر مهاجرت واقعاً اجرا شود
        Database(file).use { db ->
            db.read { c ->
                c.createStatement().use { it.execute("PRAGMA user_version = 0") }
            }
            db.migrate()

            assertEquals(Migrations.LATEST_VERSION, db.version)
            val names = db.read { c ->
                c.createStatement().use { st ->
                    st.executeQuery("SELECT name FROM playlists").use { rs ->
                        buildList { while (rs.next()) add(rs.getString(1)) }
                    }
                }
            }
            assertEquals(listOf("Old list"), names, "migration must not lose existing rows")
        }
    }

    @Test
    fun `database from a newer build is refused instead of downgraded`() {
        val file = dir.resolve("e.db")
        Database(file).use { db ->
            db.migrate()
            db.read { c ->
                c.createStatement().use { it.execute("PRAGMA user_version = 999") }
            }
        }
        Database(file).use { db ->
            val error = assertFailsWith<SQLException> { db.migrate() }
            assertTrue(
                error.message?.contains("newer") == true,
                "error should explain the version problem: ${error.message}"
            )
        }
    }

    @Test
    fun `foreign keys are enabled so playlist deletion cascades`() {
        Database(dir.resolve("f.db")).use { db ->
            db.migrate()
            val enabled = db.read { c ->
                c.createStatement().use { st ->
                    st.executeQuery("PRAGMA foreign_keys").use { rs -> rs.getInt(1) }
                }
            }
            assertEquals(1, enabled, "PRAGMA foreign_keys must be ON")
        }
    }
}
