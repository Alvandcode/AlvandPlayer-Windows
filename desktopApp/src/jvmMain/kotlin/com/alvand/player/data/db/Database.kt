package com.alvand.player.data.db

import java.nio.file.Path
import java.sql.Connection
import java.sql.DriverManager
import java.sql.SQLException

/**
 * دیتابیس SQLite — جایگزین Room.
 *
 * Room فقط روی Android کار می‌کند، پس اینجا JDBC خام با **مهاجرت‌های فقط‌رو‌به‌جلو**.
 * هر مهاجرت در یک تراکنش اجرا می‌شود؛ اگر وسط کار خراب شود، دیتابیس در نسخهٔ قبلی
 * سالم می‌ماند.
 *
 * سیاست مهم: خرابی فایل تنظیمات یعنی برگشت به پیش‌فرض، ولی **خرابی دیتابیس** یعنی خطا.
 * چون دیتابیس پلی‌لیست‌ها و تاریخچهٔ کاربر است و نباید بی‌صدا پاک شود.
 */
class Database private constructor(
    private val connection: Connection,
    val file: Path
) : AutoCloseable {

    /** برای تست و مهاجرت روی فایل موقت */
    constructor(file: Path) : this(openConnection(file), file)

    init {
        connection.createStatement().use { st ->
            // SQLite به‌صورت پیش‌فرض کلید خارجی را غیرفعال می‌کند؛ بدون این، CASCADE کار نمی‌کند
            st.execute("PRAGMA foreign_keys = ON")
            st.execute("PRAGMA journal_mode = WAL")
        }
    }

    val version: Int get() = userVersion()

    /**
     * مهاجرت تا آخرین نسخه. اگر دیتابیس خراب باشد Exception می‌دهد —
     * عمداً catch نمی‌کنیم.
     */
    @Synchronized
    fun migrate() {
        val from = userVersion()
        if (from > Migrations.LATEST_VERSION) {
            throw SQLException(
                "Database is newer than this build (db=$from, app=${Migrations.LATEST_VERSION}). " +
                    "Refusing to downgrade, because that would lose data."
            )
        }
        for (m in Migrations.all()) {
            if (m.version <= from) continue
            connection.autoCommit = false
            try {
                connection.createStatement().use { st ->
                    m.statements().forEach { st.execute(it) }
                }
                connection.commit()
            } catch (t: Throwable) {
                runCatching { connection.rollback() }
                throw SQLException(
                    "Migration ${m.version} (${m.name}) failed: ${t.message}",
                    t
                )
            } finally {
                connection.autoCommit = true
            }
            setUserVersion(m.version)
        }
    }

    @Synchronized
    private fun userVersion(): Int =
        connection.createStatement().use { st ->
            st.executeQuery("PRAGMA user_version").use { rs -> if (rs.next()) rs.getInt(1) else 0 }
        }

    @Synchronized
    private fun setUserVersion(v: Int) {
        connection.createStatement().use { st -> st.execute("PRAGMA user_version = $v") }
    }

    /** کل دیتابیس در یک تراکنش — برای نوشتن دسته‌ای هزاران آهنگ */
    @Synchronized
    fun <T> transaction(block: () -> T): T {
        connection.autoCommit = false
        return try {
            val result = block()
            connection.commit()
            result
        } catch (t: Throwable) {
            runCatching { connection.rollback() }
            throw t
        } finally {
            connection.autoCommit = true
        }
    }

    @Synchronized
    fun <T> read(block: (Connection) -> T): T = block(connection)

    override fun close() {
        runCatching { connection.close() }
    }

    private companion object {
        fun openConnection(file: Path): Connection {
            file.parent?.toFile()?.mkdirs()
            return DriverManager.getConnection("jdbc:sqlite:${file.toAbsolutePath()}")
        }
    }
}
