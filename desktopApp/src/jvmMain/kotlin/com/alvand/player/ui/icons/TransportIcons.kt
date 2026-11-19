package com.alvand.player.ui.icons

/*
 * آیکون‌های کنترل پخش.
 *
 * ## چرا این‌ها اینجا هستند و نه از کتابخانه
 *
 * بستهٔ `material-icons-extended` **۳۶ مگابایت** است، در حالی که کل برنامه فقط
 * ۷ آیکون از آن می‌گیرد — یعنی حدود ۲۲٪ حجم بستهٔ نهایی صرف چند آیکون می‌شد.
 *
 * این pathها **عیناً و بدون هیچ تغییری** از سورس رسمی Material Icons کپی شده‌اند
 * (Android Open Source Project، لایسنس Apache-2.0). یعنی شکل و ضخامت و اندازهٔ
 * آیکون‌ها روی صفحه **دقیقاً همان** چیزی است که در نسخهٔ قبلی دیده می‌شد؛ فقط
 * دیگر کل آن کتابخانه همراه برنامه توزیع نمی‌شود.
 *
 * اگر خواستید آیکون‌ها را عوض کنید، همین فایل تنها جایی است که لازم است تغییر
 * کند. برای ساخت دوباره‌اش از روی سورس رسمی:
 *
 *     .\tools\generate-transport-icons.ps1
 *
 * ## لایسنس
 *
 * Apache License 2.0 — متن کامل در LICENSE-APACHE-2.0.txt کنار همین فایل.
 */

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.materialIcon
import androidx.compose.material.icons.materialPath
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * آیکون Pause از Material Icons (Filled)، کپی‌شده از سورس رسمی.
 */
public val Icons.Filled.Pause: ImageVector
    get() {
        if (_pause != null) {
            return _pause!!
        }
        _pause = materialIcon(name = "Filled.Pause") {
            materialPath {
                moveTo(6.0f, 19.0f)
                horizontalLineToRelative(4.0f)
                lineTo(10.0f, 5.0f)
                lineTo(6.0f, 5.0f)
                verticalLineToRelative(14.0f)
                close()
                moveTo(14.0f, 5.0f)
                verticalLineToRelative(14.0f)
                horizontalLineToRelative(4.0f)
                lineTo(18.0f, 5.0f)
                horizontalLineToRelative(-4.0f)
                close()
            }
        }
        return _pause!!
    }

private var _pause: ImageVector? = null

/**
 * آیکون PlayArrow از Material Icons (Filled)، کپی‌شده از سورس رسمی.
 */
public val Icons.Filled.PlayArrow: ImageVector
    get() {
        if (_playarrow != null) {
            return _playarrow!!
        }
        _playarrow = materialIcon(name = "Filled.PlayArrow") {
            materialPath {
                moveTo(8.0f, 5.0f)
                verticalLineToRelative(14.0f)
                lineToRelative(11.0f, -7.0f)
                close()
            }
        }
        return _playarrow!!
    }

private var _playarrow: ImageVector? = null

/**
 * آیکون Shuffle از Material Icons (Filled)، کپی‌شده از سورس رسمی.
 */
public val Icons.Filled.Shuffle: ImageVector
    get() {
        if (_shuffle != null) {
            return _shuffle!!
        }
        _shuffle = materialIcon(name = "Filled.Shuffle") {
            materialPath {
                moveTo(10.59f, 9.17f)
                lineTo(5.41f, 4.0f)
                lineTo(4.0f, 5.41f)
                lineToRelative(5.17f, 5.17f)
                lineToRelative(1.42f, -1.41f)
                close()
                moveTo(14.5f, 4.0f)
                lineToRelative(2.04f, 2.04f)
                lineTo(4.0f, 18.59f)
                lineTo(5.41f, 20.0f)
                lineTo(17.96f, 7.46f)
                lineTo(20.0f, 9.5f)
                lineTo(20.0f, 4.0f)
                horizontalLineToRelative(-5.5f)
                close()
                moveTo(14.83f, 13.41f)
                lineToRelative(-1.41f, 1.41f)
                lineToRelative(3.13f, 3.13f)
                lineTo(14.5f, 20.0f)
                lineTo(20.0f, 20.0f)
                verticalLineToRelative(-5.5f)
                lineToRelative(-2.04f, 2.04f)
                lineToRelative(-3.13f, -3.13f)
                close()
            }
        }
        return _shuffle!!
    }

private var _shuffle: ImageVector? = null

/**
 * آیکون Repeat از Material Icons (Filled)، کپی‌شده از سورس رسمی.
 */
public val Icons.Filled.Repeat: ImageVector
    get() {
        if (_repeat != null) {
            return _repeat!!
        }
        _repeat = materialIcon(name = "Filled.Repeat") {
            materialPath {
                moveTo(7.0f, 7.0f)
                horizontalLineToRelative(10.0f)
                verticalLineToRelative(3.0f)
                lineToRelative(4.0f, -4.0f)
                lineToRelative(-4.0f, -4.0f)
                verticalLineToRelative(3.0f)
                lineTo(5.0f, 5.0f)
                verticalLineToRelative(6.0f)
                horizontalLineToRelative(2.0f)
                lineTo(7.0f, 7.0f)
                close()
                moveTo(17.0f, 17.0f)
                lineTo(7.0f, 17.0f)
                verticalLineToRelative(-3.0f)
                lineToRelative(-4.0f, 4.0f)
                lineToRelative(4.0f, 4.0f)
                verticalLineToRelative(-3.0f)
                horizontalLineToRelative(12.0f)
                verticalLineToRelative(-6.0f)
                horizontalLineToRelative(-2.0f)
                verticalLineToRelative(4.0f)
                close()
            }
        }
        return _repeat!!
    }

private var _repeat: ImageVector? = null

/**
 * آیکون RepeatOne از Material Icons (Filled)، کپی‌شده از سورس رسمی.
 */
public val Icons.Filled.RepeatOne: ImageVector
    get() {
        if (_repeatone != null) {
            return _repeatone!!
        }
        _repeatone = materialIcon(name = "Filled.RepeatOne") {
            materialPath {
                moveTo(7.0f, 7.0f)
                horizontalLineToRelative(10.0f)
                verticalLineToRelative(3.0f)
                lineToRelative(4.0f, -4.0f)
                lineToRelative(-4.0f, -4.0f)
                verticalLineToRelative(3.0f)
                lineTo(5.0f, 5.0f)
                verticalLineToRelative(6.0f)
                horizontalLineToRelative(2.0f)
                lineTo(7.0f, 7.0f)
                close()
                moveTo(17.0f, 17.0f)
                lineTo(7.0f, 17.0f)
                verticalLineToRelative(-3.0f)
                lineToRelative(-4.0f, 4.0f)
                lineToRelative(4.0f, 4.0f)
                verticalLineToRelative(-3.0f)
                horizontalLineToRelative(12.0f)
                verticalLineToRelative(-6.0f)
                horizontalLineToRelative(-2.0f)
                verticalLineToRelative(4.0f)
                close()
                moveTo(13.0f, 15.0f)
                lineTo(13.0f, 9.0f)
                horizontalLineToRelative(-1.0f)
                lineToRelative(-2.0f, 1.0f)
                verticalLineToRelative(1.0f)
                horizontalLineToRelative(1.5f)
                verticalLineToRelative(4.0f)
                lineTo(13.0f, 15.0f)
                close()
            }
        }
        return _repeatone!!
    }

private var _repeatone: ImageVector? = null

/**
 * آیکون SkipNext از Material Icons (Filled)، کپی‌شده از سورس رسمی.
 */
public val Icons.Filled.SkipNext: ImageVector
    get() {
        if (_skipnext != null) {
            return _skipnext!!
        }
        _skipnext = materialIcon(name = "Filled.SkipNext") {
            materialPath {
                moveTo(6.0f, 18.0f)
                lineToRelative(8.5f, -6.0f)
                lineTo(6.0f, 6.0f)
                verticalLineToRelative(12.0f)
                close()
                moveTo(16.0f, 6.0f)
                verticalLineToRelative(12.0f)
                horizontalLineToRelative(2.0f)
                verticalLineTo(6.0f)
                horizontalLineToRelative(-2.0f)
                close()
            }
        }
        return _skipnext!!
    }

private var _skipnext: ImageVector? = null

/**
 * آیکون SkipPrevious از Material Icons (Filled)، کپی‌شده از سورس رسمی.
 */
public val Icons.Filled.SkipPrevious: ImageVector
    get() {
        if (_skipprevious != null) {
            return _skipprevious!!
        }
        _skipprevious = materialIcon(name = "Filled.SkipPrevious") {
            materialPath {
                moveTo(6.0f, 6.0f)
                horizontalLineToRelative(2.0f)
                verticalLineToRelative(12.0f)
                lineTo(6.0f, 18.0f)
                close()
                moveTo(9.5f, 12.0f)
                lineToRelative(8.5f, 6.0f)
                lineTo(18.0f, 6.0f)
                close()
            }
        }
        return _skipprevious!!
    }

private var _skipprevious: ImageVector? = null

/**
 * آیکون Favorite از Material Icons (Filled)، کپی‌شده از سورس رسمی.
 */
public val Icons.Filled.Favorite: ImageVector
    get() {
        if (_favorite != null) {
            return _favorite!!
        }
        _favorite = materialIcon(name = "Filled.Favorite") {
            materialPath {
                moveTo(12.0f, 21.35f)
                lineToRelative(-1.45f, -1.32f)
                curveTo(5.4f, 15.36f, 2.0f, 12.28f, 2.0f, 8.5f)
                curveTo(2.0f, 5.42f, 4.42f, 3.0f, 7.5f, 3.0f)
                curveToRelative(1.74f, 0.0f, 3.41f, 0.81f, 4.5f, 2.09f)
                curveTo(13.09f, 3.81f, 14.76f, 3.0f, 16.5f, 3.0f)
                curveTo(19.58f, 3.0f, 22.0f, 5.42f, 22.0f, 8.5f)
                curveToRelative(0.0f, 3.78f, -3.4f, 6.86f, -8.55f, 11.54f)
                lineTo(12.0f, 21.35f)
                close()
            }
        }
        return _favorite!!
    }

private var _favorite: ImageVector? = null

/**
 * آیکون FavoriteBorder از Material Icons (Filled)، کپی‌شده از سورس رسمی.
 */
public val Icons.Filled.FavoriteBorder: ImageVector
    get() {
        if (_favoriteborder != null) {
            return _favoriteborder!!
        }
        _favoriteborder = materialIcon(name = "Filled.FavoriteBorder") {
            materialPath {
                moveTo(16.5f, 3.0f)
                curveToRelative(-1.74f, 0.0f, -3.41f, 0.81f, -4.5f, 2.09f)
                curveTo(10.91f, 3.81f, 9.24f, 3.0f, 7.5f, 3.0f)
                curveTo(4.42f, 3.0f, 2.0f, 5.42f, 2.0f, 8.5f)
                curveToRelative(0.0f, 3.78f, 3.4f, 6.86f, 8.55f, 11.54f)
                lineTo(12.0f, 21.35f)
                lineToRelative(1.45f, -1.32f)
                curveTo(18.6f, 15.36f, 22.0f, 12.28f, 22.0f, 8.5f)
                curveTo(22.0f, 5.42f, 19.58f, 3.0f, 16.5f, 3.0f)
                close()
                moveTo(12.1f, 18.55f)
                lineToRelative(-0.1f, 0.1f)
                lineToRelative(-0.1f, -0.1f)
                curveTo(7.14f, 14.24f, 4.0f, 11.39f, 4.0f, 8.5f)
                curveTo(4.0f, 6.5f, 5.5f, 5.0f, 7.5f, 5.0f)
                curveToRelative(1.54f, 0.0f, 3.04f, 0.99f, 3.57f, 2.36f)
                horizontalLineToRelative(1.87f)
                curveTo(13.46f, 5.99f, 14.96f, 5.0f, 16.5f, 5.0f)
                curveToRelative(2.0f, 0.0f, 3.5f, 1.5f, 3.5f, 3.5f)
                curveToRelative(0.0f, 2.89f, -3.14f, 5.74f, -7.9f, 10.05f)
                close()
            }
        }
        return _favoriteborder!!
    }

private var _favoriteborder: ImageVector? = null

/**
 * آیکون Home از Material Icons (Filled)، کپی‌شده از سورس رسمی.
 */
public val Icons.Filled.Home: ImageVector
    get() {
        if (_home != null) {
            return _home!!
        }
        _home = materialIcon(name = "Filled.Home") {
            materialPath {
                moveTo(10.0f, 20.0f)
                verticalLineToRelative(-6.0f)
                horizontalLineToRelative(4.0f)
                verticalLineToRelative(6.0f)
                horizontalLineToRelative(5.0f)
                verticalLineToRelative(-8.0f)
                horizontalLineToRelative(3.0f)
                lineTo(12.0f, 3.0f)
                lineTo(2.0f, 12.0f)
                horizontalLineToRelative(3.0f)
                verticalLineToRelative(8.0f)
                close()
            }
        }
        return _home!!
    }

private var _home: ImageVector? = null

/**
 * آیکون Settings از Material Icons (Filled)، کپی‌شده از سورس رسمی.
 */
public val Icons.Filled.Settings: ImageVector
    get() {
        if (_settings != null) {
            return _settings!!
        }
        _settings = materialIcon(name = "Filled.Settings") {
            materialPath {
                moveTo(19.14f, 12.94f)
                curveToRelative(0.04f, -0.3f, 0.06f, -0.61f, 0.06f, -0.94f)
                curveToRelative(0.0f, -0.32f, -0.02f, -0.64f, -0.07f, -0.94f)
                lineToRelative(2.03f, -1.58f)
                curveToRelative(0.18f, -0.14f, 0.23f, -0.41f, 0.12f, -0.61f)
                lineToRelative(-1.92f, -3.32f)
                curveToRelative(-0.12f, -0.22f, -0.37f, -0.29f, -0.59f, -0.22f)
                lineToRelative(-2.39f, 0.96f)
                curveToRelative(-0.5f, -0.38f, -1.03f, -0.7f, -1.62f, -0.94f)
                lineTo(14.4f, 2.81f)
                curveToRelative(-0.04f, -0.24f, -0.24f, -0.41f, -0.48f, -0.41f)
                horizontalLineToRelative(-3.84f)
                curveToRelative(-0.24f, 0.0f, -0.43f, 0.17f, -0.47f, 0.41f)
                lineTo(9.25f, 5.35f)
                curveTo(8.66f, 5.59f, 8.12f, 5.92f, 7.63f, 6.29f)
                lineTo(5.24f, 5.33f)
                curveToRelative(-0.22f, -0.08f, -0.47f, 0.0f, -0.59f, 0.22f)
                lineTo(2.74f, 8.87f)
                curveTo(2.62f, 9.08f, 2.66f, 9.34f, 2.86f, 9.48f)
                lineToRelative(2.03f, 1.58f)
                curveTo(4.84f, 11.36f, 4.8f, 11.69f, 4.8f, 12.0f)
                reflectiveCurveToRelative(0.02f, 0.64f, 0.07f, 0.94f)
                lineToRelative(-2.03f, 1.58f)
                curveToRelative(-0.18f, 0.14f, -0.23f, 0.41f, -0.12f, 0.61f)
                lineToRelative(1.92f, 3.32f)
                curveToRelative(0.12f, 0.22f, 0.37f, 0.29f, 0.59f, 0.22f)
                lineToRelative(2.39f, -0.96f)
                curveToRelative(0.5f, 0.38f, 1.03f, 0.7f, 1.62f, 0.94f)
                lineToRelative(0.36f, 2.54f)
                curveToRelative(0.05f, 0.24f, 0.24f, 0.41f, 0.48f, 0.41f)
                horizontalLineToRelative(3.84f)
                curveToRelative(0.24f, 0.0f, 0.44f, -0.17f, 0.47f, -0.41f)
                lineToRelative(0.36f, -2.54f)
                curveToRelative(0.59f, -0.24f, 1.13f, -0.56f, 1.62f, -0.94f)
                lineToRelative(2.39f, 0.96f)
                curveToRelative(0.22f, 0.08f, 0.47f, 0.0f, 0.59f, -0.22f)
                lineToRelative(1.92f, -3.32f)
                curveToRelative(0.12f, -0.22f, 0.07f, -0.47f, -0.12f, -0.61f)
                lineTo(19.14f, 12.94f)
                close()
                moveTo(12.0f, 15.6f)
                curveToRelative(-1.98f, 0.0f, -3.6f, -1.62f, -3.6f, -3.6f)
                reflectiveCurveToRelative(1.62f, -3.6f, 3.6f, -3.6f)
                reflectiveCurveToRelative(3.6f, 1.62f, 3.6f, 3.6f)
                reflectiveCurveTo(13.98f, 15.6f, 12.0f, 15.6f)
                close()
            }
        }
        return _settings!!
    }

private var _settings: ImageVector? = null

/**
 * آیکون Search از Material Icons (Filled)، کپی‌شده از سورس رسمی.
 */
public val Icons.Filled.Search: ImageVector
    get() {
        if (_search != null) {
            return _search!!
        }
        _search = materialIcon(name = "Filled.Search") {
            materialPath {
                moveTo(15.5f, 14.0f)
                horizontalLineToRelative(-0.79f)
                lineToRelative(-0.28f, -0.27f)
                curveTo(15.41f, 12.59f, 16.0f, 11.11f, 16.0f, 9.5f)
                curveTo(16.0f, 5.91f, 13.09f, 3.0f, 9.5f, 3.0f)
                reflectiveCurveTo(3.0f, 5.91f, 3.0f, 9.5f)
                reflectiveCurveTo(5.91f, 16.0f, 9.5f, 16.0f)
                curveToRelative(1.61f, 0.0f, 3.09f, -0.59f, 4.23f, -1.57f)
                lineToRelative(0.27f, 0.28f)
                verticalLineToRelative(0.79f)
                lineToRelative(5.0f, 4.99f)
                lineTo(20.49f, 19.0f)
                lineToRelative(-4.99f, -5.0f)
                close()
                moveTo(9.5f, 14.0f)
                curveTo(7.01f, 14.0f, 5.0f, 11.99f, 5.0f, 9.5f)
                reflectiveCurveTo(7.01f, 5.0f, 9.5f, 5.0f)
                reflectiveCurveTo(14.0f, 7.01f, 14.0f, 9.5f)
                reflectiveCurveTo(11.99f, 14.0f, 9.5f, 14.0f)
                close()
            }
        }
        return _search!!
    }

private var _search: ImageVector? = null

/**
 * آیکون KeyboardArrowRight از Material Icons (Filled)، کپی‌شده از سورس رسمی.
 */
public val Icons.Filled.KeyboardArrowRight: ImageVector
    get() {
        if (_keyboardarrowright != null) {
            return _keyboardarrowright!!
        }
        _keyboardarrowright = materialIcon(name = "Filled.KeyboardArrowRight") {
            materialPath {
                moveTo(8.59f, 16.59f)
                lineTo(13.17f, 12.0f)
                lineTo(8.59f, 7.41f)
                lineTo(10.0f, 6.0f)
                lineToRelative(6.0f, 6.0f)
                lineToRelative(-6.0f, 6.0f)
                lineToRelative(-1.41f, -1.41f)
                close()
            }
        }
        return _keyboardarrowright!!
    }

private var _keyboardarrowright: ImageVector? = null

/**
 * آیکون LibraryMusic از Material Icons (Filled)، کپی‌شده از سورس رسمی.
 */
public val Icons.Filled.LibraryMusic: ImageVector
    get() {
        if (_librarymusic != null) {
            return _librarymusic!!
        }
        _librarymusic = materialIcon(name = "Filled.LibraryMusic") {
            materialPath {
                moveTo(20.0f, 2.0f)
                lineTo(8.0f, 2.0f)
                curveToRelative(-1.1f, 0.0f, -2.0f, 0.9f, -2.0f, 2.0f)
                verticalLineToRelative(12.0f)
                curveToRelative(0.0f, 1.1f, 0.9f, 2.0f, 2.0f, 2.0f)
                horizontalLineToRelative(12.0f)
                curveToRelative(1.1f, 0.0f, 2.0f, -0.9f, 2.0f, -2.0f)
                lineTo(22.0f, 4.0f)
                curveToRelative(0.0f, -1.1f, -0.9f, -2.0f, -2.0f, -2.0f)
                close()
                moveTo(18.0f, 7.0f)
                horizontalLineToRelative(-3.0f)
                verticalLineToRelative(5.5f)
                curveToRelative(0.0f, 1.38f, -1.12f, 2.5f, -2.5f, 2.5f)
                reflectiveCurveTo(10.0f, 13.88f, 10.0f, 12.5f)
                reflectiveCurveToRelative(1.12f, -2.5f, 2.5f, -2.5f)
                curveToRelative(0.57f, 0.0f, 1.08f, 0.19f, 1.5f, 0.51f)
                lineTo(14.0f, 5.0f)
                horizontalLineToRelative(4.0f)
                verticalLineToRelative(2.0f)
                close()
                moveTo(4.0f, 6.0f)
                lineTo(2.0f, 6.0f)
                verticalLineToRelative(14.0f)
                curveToRelative(0.0f, 1.1f, 0.9f, 2.0f, 2.0f, 2.0f)
                horizontalLineToRelative(14.0f)
                verticalLineToRelative(-2.0f)
                lineTo(4.0f, 20.0f)
                lineTo(4.0f, 6.0f)
                close()
            }
        }
        return _librarymusic!!
    }

private var _librarymusic: ImageVector? = null

/**
 * آیکون QueueMusic از Material Icons (Filled)، کپی‌شده از سورس رسمی.
 */
public val Icons.Filled.QueueMusic: ImageVector
    get() {
        if (_queuemusic != null) {
            return _queuemusic!!
        }
        _queuemusic = materialIcon(name = "Filled.QueueMusic") {
            materialPath {
                moveTo(15.0f, 6.0f)
                horizontalLineTo(3.0f)
                verticalLineToRelative(2.0f)
                horizontalLineToRelative(12.0f)
                verticalLineTo(6.0f)
                close()
                moveTo(15.0f, 10.0f)
                horizontalLineTo(3.0f)
                verticalLineToRelative(2.0f)
                horizontalLineToRelative(12.0f)
                verticalLineTo(10.0f)
                close()
                moveTo(3.0f, 16.0f)
                horizontalLineToRelative(8.0f)
                verticalLineToRelative(-2.0f)
                horizontalLineTo(3.0f)
                verticalLineTo(16.0f)
                close()
                moveTo(17.0f, 6.0f)
                verticalLineToRelative(8.18f)
                curveTo(16.69f, 14.07f, 16.35f, 14.0f, 16.0f, 14.0f)
                curveToRelative(-1.66f, 0.0f, -3.0f, 1.34f, -3.0f, 3.0f)
                reflectiveCurveToRelative(1.34f, 3.0f, 3.0f, 3.0f)
                reflectiveCurveToRelative(3.0f, -1.34f, 3.0f, -3.0f)
                verticalLineTo(8.0f)
                horizontalLineToRelative(3.0f)
                verticalLineTo(6.0f)
                horizontalLineTo(17.0f)
                close()
            }
        }
        return _queuemusic!!
    }

private var _queuemusic: ImageVector? = null

/**
 * آیکون Explore از Material Icons (Filled)، کپی‌شده از سورس رسمی.
 */
public val Icons.Filled.Explore: ImageVector
    get() {
        if (_explore != null) {
            return _explore!!
        }
        _explore = materialIcon(name = "Filled.Explore") {
            materialPath {
                moveTo(12.0f, 10.9f)
                curveToRelative(-0.61f, 0.0f, -1.1f, 0.49f, -1.1f, 1.1f)
                reflectiveCurveToRelative(0.49f, 1.1f, 1.1f, 1.1f)
                curveToRelative(0.61f, 0.0f, 1.1f, -0.49f, 1.1f, -1.1f)
                reflectiveCurveToRelative(-0.49f, -1.1f, -1.1f, -1.1f)
                close()
                moveTo(12.0f, 2.0f)
                curveTo(6.48f, 2.0f, 2.0f, 6.48f, 2.0f, 12.0f)
                reflectiveCurveToRelative(4.48f, 10.0f, 10.0f, 10.0f)
                reflectiveCurveToRelative(10.0f, -4.48f, 10.0f, -10.0f)
                reflectiveCurveTo(17.52f, 2.0f, 12.0f, 2.0f)
                close()
                moveTo(14.19f, 14.19f)
                lineTo(6.0f, 18.0f)
                lineToRelative(3.81f, -8.19f)
                lineTo(18.0f, 6.0f)
                lineToRelative(-3.81f, 8.19f)
                close()
            }
        }
        return _explore!!
    }

private var _explore: ImageVector? = null

/**
 * آیکون Radio از Material Icons (Filled)، کپی‌شده از سورس رسمی.
 */
public val Icons.Filled.Radio: ImageVector
    get() {
        if (_radio != null) {
            return _radio!!
        }
        _radio = materialIcon(name = "Filled.Radio") {
            materialPath {
                moveTo(3.24f, 6.15f)
                curveTo(2.51f, 6.43f, 2.0f, 7.17f, 2.0f, 8.0f)
                verticalLineToRelative(12.0f)
                curveToRelative(0.0f, 1.1f, 0.89f, 2.0f, 2.0f, 2.0f)
                horizontalLineToRelative(16.0f)
                curveToRelative(1.11f, 0.0f, 2.0f, -0.9f, 2.0f, -2.0f)
                lineTo(22.0f, 8.0f)
                curveToRelative(0.0f, -1.11f, -0.89f, -2.0f, -2.0f, -2.0f)
                lineTo(8.3f, 6.0f)
                lineToRelative(8.26f, -3.34f)
                lineTo(15.88f, 1.0f)
                lineTo(3.24f, 6.15f)
                close()
                moveTo(7.0f, 20.0f)
                curveToRelative(-1.66f, 0.0f, -3.0f, -1.34f, -3.0f, -3.0f)
                reflectiveCurveToRelative(1.34f, -3.0f, 3.0f, -3.0f)
                reflectiveCurveToRelative(3.0f, 1.34f, 3.0f, 3.0f)
                reflectiveCurveToRelative(-1.34f, 3.0f, -3.0f, 3.0f)
                close()
                moveTo(20.0f, 12.0f)
                horizontalLineToRelative(-2.0f)
                verticalLineToRelative(-2.0f)
                horizontalLineToRelative(-2.0f)
                verticalLineToRelative(2.0f)
                lineTo(4.0f, 12.0f)
                lineTo(4.0f, 8.0f)
                horizontalLineToRelative(16.0f)
                verticalLineToRelative(4.0f)
                close()
            }
        }
        return _radio!!
    }

private var _radio: ImageVector? = null

/**
 * آیکون Equalizer از Material Icons (Filled)، کپی‌شده از سورس رسمی.
 */
public val Icons.Filled.Equalizer: ImageVector
    get() {
        if (_equalizer != null) {
            return _equalizer!!
        }
        _equalizer = materialIcon(name = "Filled.Equalizer") {
            materialPath {
                moveTo(10.0f, 20.0f)
                horizontalLineToRelative(4.0f)
                lineTo(14.0f, 4.0f)
                horizontalLineToRelative(-4.0f)
                verticalLineToRelative(16.0f)
                close()
                moveTo(4.0f, 20.0f)
                horizontalLineToRelative(4.0f)
                verticalLineToRelative(-8.0f)
                lineTo(4.0f, 12.0f)
                verticalLineToRelative(8.0f)
                close()
                moveTo(16.0f, 9.0f)
                verticalLineToRelative(11.0f)
                horizontalLineToRelative(4.0f)
                lineTo(20.0f, 9.0f)
                horizontalLineToRelative(-4.0f)
                close()
            }
        }
        return _equalizer!!
    }

private var _equalizer: ImageVector? = null

/**
 * آیکون GraphicEq از Material Icons (Filled)، کپی‌شده از سورس رسمی.
 */
public val Icons.Filled.GraphicEq: ImageVector
    get() {
        if (_graphiceq != null) {
            return _graphiceq!!
        }
        _graphiceq = materialIcon(name = "Filled.GraphicEq") {
            materialPath {
                moveTo(7.0f, 18.0f)
                horizontalLineToRelative(2.0f)
                lineTo(9.0f, 6.0f)
                lineTo(7.0f, 6.0f)
                verticalLineToRelative(12.0f)
                close()
                moveTo(11.0f, 22.0f)
                horizontalLineToRelative(2.0f)
                lineTo(13.0f, 2.0f)
                horizontalLineToRelative(-2.0f)
                verticalLineToRelative(20.0f)
                close()
                moveTo(3.0f, 14.0f)
                horizontalLineToRelative(2.0f)
                verticalLineToRelative(-4.0f)
                lineTo(3.0f, 10.0f)
                verticalLineToRelative(4.0f)
                close()
                moveTo(15.0f, 18.0f)
                horizontalLineToRelative(2.0f)
                lineTo(17.0f, 6.0f)
                horizontalLineToRelative(-2.0f)
                verticalLineToRelative(12.0f)
                close()
                moveTo(19.0f, 10.0f)
                verticalLineToRelative(4.0f)
                horizontalLineToRelative(2.0f)
                verticalLineToRelative(-4.0f)
                horizontalLineToRelative(-2.0f)
                close()
            }
        }
        return _graphiceq!!
    }

private var _graphiceq: ImageVector? = null

/**
 * آیکون Tune از Material Icons (Filled)، کپی‌شده از سورس رسمی.
 */
public val Icons.Filled.Tune: ImageVector
    get() {
        if (_tune != null) {
            return _tune!!
        }
        _tune = materialIcon(name = "Filled.Tune") {
            materialPath {
                moveTo(3.0f, 17.0f)
                verticalLineToRelative(2.0f)
                horizontalLineToRelative(6.0f)
                verticalLineToRelative(-2.0f)
                lineTo(3.0f, 17.0f)
                close()
                moveTo(3.0f, 5.0f)
                verticalLineToRelative(2.0f)
                horizontalLineToRelative(10.0f)
                lineTo(13.0f, 5.0f)
                lineTo(3.0f, 5.0f)
                close()
                moveTo(13.0f, 21.0f)
                verticalLineToRelative(-2.0f)
                horizontalLineToRelative(8.0f)
                verticalLineToRelative(-2.0f)
                horizontalLineToRelative(-8.0f)
                verticalLineToRelative(-2.0f)
                horizontalLineToRelative(-2.0f)
                verticalLineToRelative(6.0f)
                horizontalLineToRelative(2.0f)
                close()
                moveTo(7.0f, 9.0f)
                verticalLineToRelative(2.0f)
                lineTo(3.0f, 11.0f)
                verticalLineToRelative(2.0f)
                horizontalLineToRelative(4.0f)
                verticalLineToRelative(2.0f)
                horizontalLineToRelative(2.0f)
                lineTo(9.0f, 9.0f)
                lineTo(7.0f, 9.0f)
                close()
                moveTo(21.0f, 13.0f)
                verticalLineToRelative(-2.0f)
                lineTo(11.0f, 11.0f)
                verticalLineToRelative(2.0f)
                horizontalLineToRelative(10.0f)
                close()
                moveTo(15.0f, 9.0f)
                horizontalLineToRelative(2.0f)
                lineTo(17.0f, 7.0f)
                horizontalLineToRelative(4.0f)
                lineTo(21.0f, 5.0f)
                horizontalLineToRelative(-4.0f)
                lineTo(17.0f, 3.0f)
                horizontalLineToRelative(-2.0f)
                verticalLineToRelative(6.0f)
                close()
            }
        }
        return _tune!!
    }

private var _tune: ImageVector? = null

/**
 * آیکون Lyrics از Material Icons (Filled)، کپی‌شده از سورس رسمی.
 */
public val Icons.Filled.Lyrics: ImageVector
    get() {
        if (_lyrics != null) {
            return _lyrics!!
        }
        _lyrics = materialIcon(name = "Filled.Lyrics") {
            materialPath {
                moveTo(14.0f, 9.0f)
                curveToRelative(0.0f, -2.04f, 1.24f, -3.79f, 3.0f, -4.57f)
                verticalLineTo(4.0f)
                curveToRelative(0.0f, -1.1f, -0.9f, -2.0f, -2.0f, -2.0f)
                horizontalLineTo(4.0f)
                curveTo(2.9f, 2.0f, 2.01f, 2.9f, 2.01f, 4.0f)
                lineTo(2.0f, 22.0f)
                lineToRelative(4.0f, -4.0f)
                horizontalLineToRelative(9.0f)
                curveToRelative(1.1f, 0.0f, 2.0f, -0.9f, 2.0f, -2.0f)
                verticalLineToRelative(-2.42f)
                curveTo(15.24f, 12.8f, 14.0f, 11.05f, 14.0f, 9.0f)
                close()
                moveTo(10.0f, 14.0f)
                horizontalLineTo(6.0f)
                verticalLineToRelative(-2.0f)
                horizontalLineToRelative(4.0f)
                verticalLineTo(14.0f)
                close()
                moveTo(13.0f, 11.0f)
                horizontalLineTo(6.0f)
                verticalLineTo(9.0f)
                horizontalLineToRelative(7.0f)
                verticalLineTo(11.0f)
                close()
                moveTo(13.0f, 8.0f)
                horizontalLineTo(6.0f)
                verticalLineTo(6.0f)
                horizontalLineToRelative(7.0f)
                verticalLineTo(8.0f)
                close()
            }
        }
        return _lyrics!!
    }

private var _lyrics: ImageVector? = null

/**
 * آیکون KeyboardArrowUp از Material Icons (Filled)، کپی‌شده از سورس رسمی.
 */
public val Icons.Filled.KeyboardArrowUp: ImageVector
    get() {
        if (_keyboardarrowup != null) {
            return _keyboardarrowup!!
        }
        _keyboardarrowup = materialIcon(name = "Filled.KeyboardArrowUp") {
            materialPath {
                moveTo(7.41f, 15.41f)
                lineTo(12.0f, 10.83f)
                lineToRelative(4.59f, 4.58f)
                lineTo(18.0f, 14.0f)
                lineToRelative(-6.0f, -6.0f)
                lineToRelative(-6.0f, 6.0f)
                close()
            }
        }
        return _keyboardarrowup!!
    }

private var _keyboardarrowup: ImageVector? = null

/**
 * آیکون KeyboardArrowDown از Material Icons (Filled)، کپی‌شده از سورس رسمی.
 */
public val Icons.Filled.KeyboardArrowDown: ImageVector
    get() {
        if (_keyboardarrowdown != null) {
            return _keyboardarrowdown!!
        }
        _keyboardarrowdown = materialIcon(name = "Filled.KeyboardArrowDown") {
            materialPath {
                moveTo(7.41f, 8.59f)
                lineTo(12.0f, 13.17f)
                lineToRelative(4.59f, -4.58f)
                lineTo(18.0f, 10.0f)
                lineToRelative(-6.0f, 6.0f)
                lineToRelative(-6.0f, -6.0f)
                lineToRelative(1.41f, -1.41f)
                close()
            }
        }
        return _keyboardarrowdown!!
    }

private var _keyboardarrowdown: ImageVector? = null

