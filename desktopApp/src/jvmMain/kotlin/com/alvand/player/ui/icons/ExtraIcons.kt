package com.alvand.player.ui.icons

/*
 * آیکون‌های تکمیلی رابط دسکتاپ.
 *
 * ## چرا این‌ها اینجا هستند و نه از کتابخانه
 *
 * همان دلیلی که در `TransportIcons.kt` آمده: بستهٔ `material-icons-extended` ۳۶ مگابایت
 * است و برنامه فقط چند آیکون از آن می‌گیرد. این pathها **عیناً و بدون هیچ تغییری**
 * از سورس رسمی Material Icons کپی شده‌اند (Android Open Source Project، لایسنس
 * Apache-2.0) — یعنی ظاهرشان دقیقاً همان چیزی است که در کتابخانه می‌دید.
 *
 * اگر خواستید این آیکون‌ها را عوض کنید، همین فایل تنها جایی است که لازم است تغییر
 * کند.
 *
 * ## لایسنس
 *
 * Apache License 2.0 — متن کامل در LICENSE-APACHE-2.0.txt کنار `TransportIcons.kt`.
 */

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.materialIcon
import androidx.compose.material.icons.materialPath
import androidx.compose.ui.graphics.vector.ImageVector

/** آیکون نت‌نویس (Material Icons، Filled) */
public val Icons.Filled.MusicNote: ImageVector
    get() {
        if (_musicnote != null) {
            return _musicnote!!
        }
        _musicnote = materialIcon(name = "Filled.MusicNote") {
            materialPath {
                moveTo(12.0f, 3.0f)
                verticalLineToRelative(10.55f)
                curveToRelative(-0.59f, -0.34f, -1.27f, -0.55f, -2.0f, -0.55f)
                curveToRelative(-2.21f, 0.0f, -4.0f, 1.79f, -4.0f, 4.0f)
                reflectiveCurveToRelative(1.79f, 4.0f, 4.0f, 4.0f)
                reflectiveCurveToRelative(4.0f, -1.79f, 4.0f, -4.0f)
                verticalLineTo(7.0f)
                horizontalLineToRelative(4.0f)
                verticalLineTo(3.0f)
                horizontalLineToRelative(-6.0f)
                close()
            }
        }
        return _musicnote!!
    }

private var _musicnote: ImageVector? = null

/** آیکون دیسک/آلبوم (Material Icons، Filled) */
public val Icons.Filled.Album: ImageVector
    get() {
        if (_album != null) {
            return _album!!
        }
        _album = materialIcon(name = "Filled.Album") {
            materialPath {
                moveTo(12.0f, 2.0f)
                curveTo(6.48f, 2.0f, 2.0f, 6.48f, 2.0f, 12.0f)
                reflectiveCurveToRelative(4.48f, 10.0f, 10.0f, 10.0f)
                reflectiveCurveToRelative(10.0f, -4.48f, 10.0f, -10.0f)
                reflectiveCurveTo(17.52f, 2.0f, 12.0f, 2.0f)
                close()
                moveTo(12.0f, 16.5f)
                curveToRelative(-2.49f, 0.0f, -4.5f, -2.01f, -4.5f, -4.5f)
                reflectiveCurveTo(9.51f, 7.5f, 12.0f, 7.5f)
                reflectiveCurveToRelative(4.5f, 2.01f, 4.5f, 4.5f)
                reflectiveCurveToRelative(-2.01f, 4.5f, -4.5f, 4.5f)
                close()
                moveTo(12.0f, 11.0f)
                curveToRelative(-0.55f, 0.0f, -1.0f, 0.45f, -1.0f, 1.0f)
                reflectiveCurveToRelative(0.45f, 1.0f, 1.0f, 1.0f)
                reflectiveCurveToRelative(1.0f, -0.45f, 1.0f, -1.0f)
                reflectiveCurveToRelative(-0.45f, -1.0f, -1.0f, -1.0f)
                close()
            }
        }
        return _album!!
    }

private var _album: ImageVector? = null

/** آیکون نمودار میله‌ای (Material Icons، Filled) */
public val Icons.Filled.BarChart: ImageVector
    get() {
        if (_barchart != null) {
            return _barchart!!
        }
        _barchart = materialIcon(name = "Filled.BarChart") {
            materialPath {
                moveTo(4.0f, 9.0f)
                horizontalLineToRelative(4.0f)
                verticalLineToRelative(11.0f)
                horizontalLineToRelative(-4.0f)
                close()
            }
            materialPath {
                moveTo(16.0f, 13.0f)
                horizontalLineToRelative(4.0f)
                verticalLineToRelative(7.0f)
                horizontalLineToRelative(-4.0f)
                close()
            }
            materialPath {
                moveTo(10.0f, 4.0f)
                horizontalLineToRelative(4.0f)
                verticalLineToRelative(16.0f)
                horizontalLineToRelative(-4.0f)
                close()
            }
        }
        return _barchart!!
    }

private var _barchart: ImageVector? = null

/** آیکون تازه‌ها (Material Icons، Filled) */
public val Icons.Filled.NewReleases: ImageVector
    get() {
        if (_newreleases != null) {
            return _newreleases!!
        }
        _newreleases = materialIcon(name = "Filled.NewReleases") {
            materialPath {
                moveTo(23.0f, 12.0f)
                lineToRelative(-2.44f, -2.78f)
                lineToRelative(0.34f, -3.68f)
                lineToRelative(-3.61f, -0.82f)
                lineToRelative(-1.89f, -3.18f)
                lineTo(12.0f, 3.0f)
                lineTo(8.6f, 1.54f)
                lineTo(6.71f, 4.72f)
                lineToRelative(-3.61f, 0.81f)
                lineToRelative(0.34f, 3.68f)
                lineTo(1.0f, 12.0f)
                lineToRelative(2.44f, 2.78f)
                lineToRelative(-0.34f, 3.69f)
                lineToRelative(3.61f, 0.82f)
                lineToRelative(1.89f, 3.18f)
                lineTo(12.0f, 21.0f)
                lineToRelative(3.4f, 1.46f)
                lineToRelative(1.89f, -3.18f)
                lineToRelative(3.61f, -0.82f)
                lineToRelative(-0.34f, -3.68f)
                lineTo(23.0f, 12.0f)
                close()
                moveTo(13.0f, 17.0f)
                horizontalLineToRelative(-2.0f)
                verticalLineToRelative(-2.0f)
                horizontalLineToRelative(2.0f)
                verticalLineToRelative(2.0f)
                close()
                moveTo(13.0f, 13.0f)
                horizontalLineToRelative(-2.0f)
                lineTo(11.0f, 7.0f)
                horizontalLineToRelative(2.0f)
                verticalLineToRelative(6.0f)
                close()
            }
        }
        return _newreleases!!
    }

private var _newreleases: ImageVector? = null

/** آیکون دسته‌بندی (Material Icons، Filled) */
public val Icons.Filled.Category: ImageVector
    get() {
        if (_category != null) {
            return _category!!
        }
        _category = materialIcon(name = "Filled.Category") {
            materialPath {
                moveTo(12.0f, 2.0f)
                lineToRelative(-5.5f, 9.0f)
                horizontalLineToRelative(11.0f)
                close()
            }
            materialPath {
                moveTo(17.5f, 17.5f)
                moveToRelative(-4.5f, 0.0f)
                arcToRelative(4.5f, 4.5f, 0.0f, true, true, 9.0f, 0.0f)
                arcToRelative(4.5f, 4.5f, 0.0f, true, true, -9.0f, 0.0f)
            }
            materialPath {
                moveTo(3.0f, 13.5f)
                horizontalLineToRelative(8.0f)
                verticalLineToRelative(8.0f)
                horizontalLineTo(3.0f)
                close()
            }
        }
        return _category!!
    }

private var _category: ImageVector? = null

/** آیکون ترجمه/زبان (Material Icons، Filled) */
public val Icons.Filled.Translate: ImageVector
    get() {
        if (_translate != null) {
            return _translate!!
        }
        _translate = materialIcon(name = "Filled.Translate") {
            materialPath {
                moveTo(12.87f, 15.07f)
                lineToRelative(-2.54f, -2.51f)
                lineToRelative(0.03f, -0.03f)
                curveToRelative(1.74f, -1.94f, 2.98f, -4.17f, 3.71f, -6.53f)
                lineTo(17.0f, 6.0f)
                lineTo(17.0f, 4.0f)
                horizontalLineToRelative(-7.0f)
                lineTo(10.0f, 2.0f)
                lineTo(8.0f, 2.0f)
                verticalLineToRelative(2.0f)
                lineTo(1.0f, 4.0f)
                verticalLineToRelative(1.99f)
                horizontalLineToRelative(11.17f)
                curveTo(11.5f, 7.92f, 10.44f, 9.75f, 9.0f, 11.35f)
                curveTo(8.07f, 10.32f, 7.3f, 9.19f, 6.69f, 8.0f)
                horizontalLineToRelative(-2.0f)
                curveToRelative(0.73f, 1.63f, 1.73f, 3.17f, 2.98f, 4.56f)
                lineToRelative(-5.09f, 5.02f)
                lineTo(4.0f, 19.0f)
                lineToRelative(5.0f, -5.0f)
                lineToRelative(3.11f, 3.11f)
                lineToRelative(0.76f, -2.04f)
                close()
                moveTo(18.5f, 10.0f)
                horizontalLineToRelative(-2.0f)
                lineTo(12.0f, 22.0f)
                horizontalLineToRelative(2.0f)
                lineToRelative(1.12f, -3.0f)
                horizontalLineToRelative(4.75f)
                lineTo(21.0f, 22.0f)
                horizontalLineToRelative(2.0f)
                lineToRelative(-4.5f, -12.0f)
                close()
                moveTo(15.88f, 17.0f)
                lineToRelative(1.62f, -4.33f)
                lineTo(19.12f, 17.0f)
                horizontalLineToRelative(-3.24f)
                close()
            }
        }
        return _translate!!
    }

private var _translate: ImageVector? = null

/** آیکون صفحه‌کلید (Material Icons، Filled) */
public val Icons.Filled.Keyboard: ImageVector
    get() {
        if (_keyboard != null) {
            return _keyboard!!
        }
        _keyboard = materialIcon(name = "Filled.Keyboard") {
            materialPath {
                moveTo(20.0f, 5.0f)
                lineTo(4.0f, 5.0f)
                curveToRelative(-1.1f, 0.0f, -1.99f, 0.9f, -1.99f, 2.0f)
                lineTo(2.0f, 17.0f)
                curveToRelative(0.0f, 1.1f, 0.9f, 2.0f, 2.0f, 2.0f)
                horizontalLineToRelative(16.0f)
                curveToRelative(1.1f, 0.0f, 2.0f, -0.9f, 2.0f, -2.0f)
                lineTo(22.0f, 7.0f)
                curveToRelative(0.0f, -1.1f, -0.9f, -2.0f, -2.0f, -2.0f)
                close()
                moveTo(11.0f, 8.0f)
                horizontalLineToRelative(2.0f)
                verticalLineToRelative(2.0f)
                horizontalLineToRelative(-2.0f)
                lineTo(11.0f, 8.0f)
                close()
                moveTo(11.0f, 11.0f)
                horizontalLineToRelative(2.0f)
                verticalLineToRelative(2.0f)
                horizontalLineToRelative(-2.0f)
                verticalLineToRelative(-2.0f)
                close()
                moveTo(8.0f, 8.0f)
                horizontalLineToRelative(2.0f)
                verticalLineToRelative(2.0f)
                lineTo(8.0f, 10.0f)
                lineTo(8.0f, 8.0f)
                close()
                moveTo(8.0f, 11.0f)
                horizontalLineToRelative(2.0f)
                verticalLineToRelative(2.0f)
                lineTo(8.0f, 13.0f)
                verticalLineToRelative(-2.0f)
                close()
                moveTo(7.0f, 13.0f)
                lineTo(5.0f, 13.0f)
                verticalLineToRelative(-2.0f)
                horizontalLineToRelative(2.0f)
                verticalLineToRelative(2.0f)
                close()
                moveTo(7.0f, 10.0f)
                lineTo(5.0f, 10.0f)
                lineTo(5.0f, 8.0f)
                horizontalLineToRelative(2.0f)
                verticalLineToRelative(2.0f)
                close()
                moveTo(16.0f, 17.0f)
                lineTo(8.0f, 17.0f)
                verticalLineToRelative(-2.0f)
                horizontalLineToRelative(8.0f)
                verticalLineToRelative(2.0f)
                close()
                moveTo(16.0f, 13.0f)
                horizontalLineToRelative(-2.0f)
                verticalLineToRelative(-2.0f)
                horizontalLineToRelative(2.0f)
                verticalLineToRelative(2.0f)
                close()
                moveTo(16.0f, 10.0f)
                horizontalLineToRelative(-2.0f)
                lineTo(14.0f, 8.0f)
                horizontalLineToRelative(2.0f)
                verticalLineToRelative(2.0f)
                close()
                moveTo(19.0f, 13.0f)
                horizontalLineToRelative(-2.0f)
                verticalLineToRelative(-2.0f)
                horizontalLineToRelative(2.0f)
                verticalLineToRelative(2.0f)
                close()
                moveTo(19.0f, 10.0f)
                horizontalLineToRelative(-2.0f)
                lineTo(17.0f, 8.0f)
                horizontalLineToRelative(2.0f)
                verticalLineToRelative(2.0f)
                close()
            }
        }
        return _keyboard!!
    }

private var _keyboard: ImageVector? = null

/** آیکون پالت (Material Icons، Filled) */
public val Icons.Filled.Palette: ImageVector
    get() {
        if (_palette != null) {
            return _palette!!
        }
        _palette = materialIcon(name = "Filled.Palette") {
            materialPath {
                moveTo(12.0f, 2.0f)
                curveTo(6.49f, 2.0f, 2.0f, 6.49f, 2.0f, 12.0f)
                reflectiveCurveToRelative(4.49f, 10.0f, 10.0f, 10.0f)
                curveToRelative(1.38f, 0.0f, 2.5f, -1.12f, 2.5f, -2.5f)
                curveToRelative(0.0f, -0.61f, -0.23f, -1.2f, -0.64f, -1.67f)
                curveToRelative(-0.08f, -0.1f, -0.13f, -0.21f, -0.13f, -0.33f)
                curveToRelative(0.0f, -0.28f, 0.22f, -0.5f, 0.5f, -0.5f)
                horizontalLineTo(16.0f)
                curveToRelative(3.31f, 0.0f, 6.0f, -2.69f, 6.0f, -6.0f)
                curveTo(22.0f, 6.04f, 17.51f, 2.0f, 12.0f, 2.0f)
                close()
                moveTo(17.5f, 13.0f)
                curveToRelative(-0.83f, 0.0f, -1.5f, -0.67f, -1.5f, -1.5f)
                curveToRelative(0.0f, -0.83f, 0.67f, -1.5f, 1.5f, -1.5f)
                reflectiveCurveToRelative(1.5f, 0.67f, 1.5f, 1.5f)
                curveTo(19.0f, 12.33f, 18.33f, 13.0f, 17.5f, 13.0f)
                close()
                moveTo(14.5f, 9.0f)
                curveTo(13.67f, 9.0f, 13.0f, 8.33f, 13.0f, 7.5f)
                curveTo(13.0f, 6.67f, 13.67f, 6.0f, 14.5f, 6.0f)
                reflectiveCurveTo(16.0f, 6.67f, 16.0f, 7.5f)
                curveTo(16.0f, 8.33f, 15.33f, 9.0f, 14.5f, 9.0f)
                close()
                moveTo(5.0f, 11.5f)
                curveTo(5.0f, 10.67f, 5.67f, 10.0f, 6.5f, 10.0f)
                reflectiveCurveTo(8.0f, 10.67f, 8.0f, 11.5f)
                curveTo(8.0f, 12.33f, 7.33f, 13.0f, 6.5f, 13.0f)
                reflectiveCurveTo(5.0f, 12.33f, 5.0f, 11.5f)
                close()
                moveTo(11.0f, 7.5f)
                curveTo(11.0f, 8.33f, 10.33f, 9.0f, 9.5f, 9.0f)
                reflectiveCurveTo(8.0f, 8.33f, 8.0f, 7.5f)
                curveTo(8.0f, 6.67f, 8.67f, 6.0f, 9.5f, 6.0f)
                reflectiveCurveTo(11.0f, 6.67f, 11.0f, 7.5f)
                close()
            }
        }
        return _palette!!
    }

private var _palette: ImageVector? = null

/** آیکون سرعت‌سنج (Material Icons، Filled) */
public val Icons.Filled.Speed: ImageVector
    get() {
        if (_speed != null) {
            return _speed!!
        }
        _speed = materialIcon(name = "Filled.Speed") {
            materialPath {
                moveTo(20.38f, 8.57f)
                lineToRelative(-1.23f, 1.85f)
                arcToRelative(8.0f, 8.0f, 0.0f, true, true, -0.22f, 7.58f)
                lineTo(5.07f, 18.0f)
                arcTo(8.0f, 8.0f, 0.0f, true, true, 15.58f, 6.85f)
                lineToRelative(1.85f, -1.23f)
                arcTo(10.0f, 10.0f, 0.0f, false, false, 3.35f, 19.0f)
                arcToRelative(2.0f, 2.0f, 0.0f, false, false, 1.72f, 1.0f)
                horizontalLineToRelative(13.85f)
                arcToRelative(2.0f, 2.0f, 0.0f, false, false, 1.74f, -1.0f)
                arcToRelative(10.0f, 10.0f, 0.0f, false, false, -0.27f, -10.44f)
                close()
                moveTo(10.59f, 15.41f)
                arcToRelative(2.0f, 2.0f, 0.0f, false, false, 2.83f, 0.0f)
                lineToRelative(5.66f, -8.49f)
                lineToRelative(-8.49f, 5.66f)
                arcToRelative(2.0f, 2.0f, 0.0f, false, false, 0.0f, 2.83f)
                close()
            }
        }
        return _speed!!
    }

private var _speed: ImageVector? = null

/** آیکون ترک صوتی (Material Icons، Filled) */
public val Icons.Filled.Audiotrack: ImageVector
    get() {
        if (_audiotrack != null) {
            return _audiotrack!!
        }
        _audiotrack = materialIcon(name = "Filled.Audiotrack") {
            materialPath {
                moveTo(12.0f, 3.0f)
                verticalLineToRelative(9.28f)
                curveToRelative(-0.47f, -0.17f, -0.97f, -0.28f, -1.5f, -0.28f)
                curveTo(8.01f, 12.0f, 6.0f, 14.01f, 6.0f, 16.5f)
                reflectiveCurveTo(8.01f, 21.0f, 10.5f, 21.0f)
                curveToRelative(2.31f, 0.0f, 4.2f, -1.75f, 4.45f, -4.0f)
                horizontalLineTo(15.0f)
                verticalLineTo(6.0f)
                horizontalLineToRelative(4.0f)
                verticalLineTo(3.0f)
                horizontalLineToRelative(-7.0f)
                close()
            }
        }
        return _audiotrack!!
    }

private var _audiotrack: ImageVector? = null