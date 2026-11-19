package com.alvand.player.i18n

import androidx.compose.runtime.staticCompositionLocalOf

/**
 * رشته‌های رابط کاربری.
 *
 * فقط دو زبان، دقیقاً مثل نسخهٔ اندروید: en و fa.
 *
 * قاعدهٔ نام‌گذاری: هر رشته‌ای که در اندروید هست، **همان نام** و همان متن را دارد
 * (`search_hint`, `no_music`, `nav_library`, …) تا ترجمه و تاریخچه معنادار بماند.
 * رشته‌هایی که مخصوص ویندوز هستند با کامنت `// windows-only` مشخص شده‌اند و در
 * `docs/PORT-DIFFERENCES.md` ثبت شده‌اند.
 */
data class Strings(
    val appName: String,
    val language: String,
    val close: String,

    // ---- تم (پورت‌شده از strings.xml اندروید) ----
    val theme: String,
    val themeSystem: String,
    val themeLight: String,
    val themeDark: String,
    val themeColor: String,
    val accentMono: String,
    val accentRoyal: String,
    val accentSunset: String,
    val accentOcean: String,

    // ---- کتابخانه ----
    val navLibrary: String,        // strings.xml اندروید
    val settings: String,          // windows-only
    val jumpTitle: String,         // windows-only
    val jumpHint: String,          // windows-only
    val jumpGo: String,            // windows-only
    val abLoop: String,            // windows-only
    val abSetA: String,            // windows-only
    val abSetB: String,            // windows-only
    val abClear: String,           // windows-only
    val visualizer: String,        // windows-only
    val searchHint: String,        // strings.xml اندروید
    val noMusic: String,           // strings.xml اندروید
    val playAll: String,           // strings.xml اندروید
    val tabPlayer: String,         // strings.xml اندروید
    val playlist: String,          // strings.xml اندروید
    val playError: String,         // strings.xml اندروید

    // ---- milestone 1: نمایش سیستم طراحی ----
    val designSystem: String,      // windows-only
    val palette: String,           // windows-only
    val surface: String,           // windows-only
    val milestoneNote: String,     // windows-only

    // ---- کتابخانه (ویندوز) ----
    val chooseFolder: String,      // windows-only
    val openFile: String,        // windows-only
    val dropFiles: String,       // windows-only
    val unsupportedFormat: String, // windows-only
    val scan: String,              // windows-only
    val scanning: String,          // windows-only
    val cancel: String,            // windows-only
    val songsFound: String,        // windows-only
    val folders: String,           // windows-only
    val groupSongs: String,        // windows-only
    val groupAlbums: String,       // windows-only
    val groupArtists: String,      // windows-only
    val sortDefault: String,       // windows-only
    val sortTitle: String,         // windows-only
    val sortArtist: String,        // windows-only
    val sortLongest: String,       // windows-only
    val favourites: String,        // windows-only

    // ---- شل دسکتاپ (windows-only) ----
    val navHome: String,
    val navPlaylists: String,
    val navDiscover: String,
    val navRadio: String,
    val navTools: String,
    val navEffects: String,
    val navSettingsSection: String,
    val appTagline: String,
    val greetingMorning: String,
    val greetingAfternoon: String,
    val greetingEvening: String,
    val heroTitle: String,
    val heroSubtitle: String,
    val playYourMix: String,
    val browse: String,
    val cardLibraryTitle: String,
    val cardLibrarySub: String,
    val cardDiscoverTitle: String,
    val cardDiscoverSub: String,
    val cardRadioTitle: String,
    val cardRadioSub: String,
    val cardSettingsTitle: String,
    val cardSettingsSub: String,
    val recentlyPlayed: String,
    val viewAll: String,
    val nowPlaying: String,
    val queueTitle: String,
    val groupGenres: String,
    val newPlaylist: String,
    val playlistNameHint: String,
    val playlistCreate: String,
    val playlistDelete: String,
    val playlistRename: String,
    val forYou: String,
    val charts: String,
    val newReleases: String,
    val featuredPlaylists: String,
    val radioEmpty: String,
    val discoverEmpty: String,
    val searchNoResults: String,
    val volumeLabel: String,
    val setGeneral: String,
    val setGeneralSub: String,
    val setLanguage: String,
    val setLanguageSub: String,
    val setAudio: String,
    val setAudioSub: String,
    val outputDevice: String,
    val outputDeviceDefault: String,
    val setAppearance: String,
    val setAppearanceSub: String,
    val setShortcuts: String,
    val setShortcutsSub: String,
    val setAbout: String,
    val setLyricsOnline: String,
    val setPerformance: String,
    val appVersion: String,

    // ---- امضای اوریجینال الوند (windows-only) ----
    val sleepTimer: String,        // windows-only
    val sleepOff: String,          // windows-only
    val performanceMode: String,   // windows-only
    val queueRestored: String,      // windows-only
    val shortcutsHint: String,      // windows-only

    // ---- اکولایزر (windows-only) ----
    val equalizer: String,         // windows-only
    val eqOn: String,              // windows-only
    val eqOff: String,             // windows-only
    val eqReset: String,           // windows-only
    val eqPresets: String,         // windows-only
    val eqManual: String,          // windows-only
    val eqBands: String,           // windows-only
    val eqTone: String,            // windows-only
    val eqBass: String,            // windows-only
    val eqBoost: String,           // windows-only
    val eqNoise: String,           // windows-only
    val eqNoiseLevel: String,      // windows-only
    val eqReverb: String,          // windows-only
    val eqReverbOff: String,       // windows-only
    val eqReverbSmall: String,     // windows-only
    val eqReverbMedium: String,    // windows-only
    val eqReverbLarge: String,     // windows-only

    // ---- شناسنامهٔ آهنگ (windows-only) ----
    val details: String,           // windows-only
    val propsTags: String,         // windows-only
    val propsTitle: String,        // windows-only
    val propsArtist: String,       // windows-only
    val propsAlbum: String,        // windows-only
    val propsDuration: String,     // windows-only
    val propsFile: String,         // windows-only
    val propsName: String,         // windows-only
    val propsPath: String,         // windows-only
    val propsSize: String,         // windows-only
    val propsFormat: String,       // windows-only
    val propsMissing: String,      // windows-only
    val propsAudio: String,        // windows-only
    val propsSampleRate: String,   // windows-only
    val propsChannels: String,     // windows-only
    val propsMono: String,         // windows-only
    val propsStereo: String,       // windows-only
    val propsBits: String,         // windows-only
    val propsBitrate: String,      // windows-only
    val propsShowInFolder: String,   // windows-only

    // ---- متن آهنگ (windows-only) ----
    val lyrics: String,            // windows-only
    val lyricsEnable: String,      // windows-only
    val lyricsPrivacy: String,     // windows-only
    val lyricsLoading: String,     // windows-only
    val lyricsNotFound: String,    // windows-only
    val lyricsOffline: String,     // windows-only
    val lyricsTimeout: String,     // windows-only
    val lyricsRateLimited: String, // windows-only
    val lyricsServerError: String, // windows-only
    val lyricsError: String,       // windows-only
    val lyricsRetry: String,       // windows-only
    val lyricsUntimed: String,     // windows-only
    val lyricsNoMusic: String       // windows-only
) {
    fun accentName(id: Int): String = when (id) {
        1 -> accentRoyal
        2 -> accentSunset
        3 -> accentOcean
        else -> accentMono
    }

    fun themeName(mode: Int): String = when (mode) {
        1 -> themeLight
        2 -> themeDark
        else -> themeSystem
    }
}

val StringsEn = Strings(
    appName = "Alvand Player",
    language = "Language",
    close = "Close",

    theme = "Theme",
    themeSystem = "System default",
    themeLight = "Light",
    themeDark = "Dark",
    themeColor = "Color theme",
    accentMono = "Minimal",
    accentRoyal = "Royal",
    accentSunset = "Sunset",
    accentOcean = "Ocean",

    navLibrary = "Library",
    settings = "Settings",
    jumpTitle = "Jump to time",
    jumpHint = "mm:ss",
    jumpGo = "Go",
    abLoop = "Loop A–B",
    abSetA = "Set A",
    abSetB = "Set B",
    abClear = "Clear",
    visualizer = "Visualizer",
    searchHint = "Search songs, artists, playlists.",
    noMusic = "No music was found on this device. Scan again or open a file from the menu.",
    playAll = "Play All",
    tabPlayer = "Player",
    playlist = "Playlist",
    playError = "This file cannot be played (format or access problem).",

    designSystem = "Design system",
    palette = "Palette",
    surface = "Glass surfaces",
    milestoneNote = "Milestone 1 — tokens, window, tray, single instance",

    chooseFolder = "Choose folder",
    openFile = "Open file",
    dropFiles = "Drop audio files or folders here",
    unsupportedFormat = "That audio format is not supported yet.",
    scan = "Scan",
    scanning = "Scanning",
    cancel = "Cancel",
    songsFound = "songs found",
    folders = "folders",
    groupSongs = "Songs",
    groupAlbums = "Albums",
    groupArtists = "Artists",
    sortDefault = "Recent",
    sortTitle = "Title",
    sortArtist = "Artist",
    sortLongest = "Longest",
    favourites = "Favourites",

    navHome = "Home",
    navPlaylists = "Playlists",
    navDiscover = "Discover",
    navRadio = "Radio",
    navTools = "Tools",
    navEffects = "Effects",
    navSettingsSection = "Settings",
    appTagline = "Feel the Music",
    greetingMorning = "Good Morning",
    greetingAfternoon = "Good Afternoon",
    greetingEvening = "Good Evening",
    heroTitle = "Music Makes Life Better",
    heroSubtitle = "Your mood. Your music. Your Alvand.",
    playYourMix = "Play Your Mix",
    browse = "Browse",
    cardLibraryTitle = "Your Library",
    cardLibrarySub = "Your songs & playlists",
    cardDiscoverTitle = "Discover",
    cardDiscoverSub = "New music, new vibes",
    cardRadioTitle = "Radio",
    cardRadioSub = "Stations for you",
    cardSettingsTitle = "Settings",
    cardSettingsSub = "Customize your experience",
    recentlyPlayed = "Recently Played",
    viewAll = "View All",
    nowPlaying = "Now Playing",
    queueTitle = "Queue",
    groupGenres = "Genres",
    newPlaylist = "New Playlist",
    playlistNameHint = "Playlist name",
    playlistCreate = "Create",
    playlistDelete = "Delete",
    playlistRename = "Rename",
    forYou = "For You",
    charts = "Charts",
    newReleases = "New Releases",
    featuredPlaylists = "Featured Playlists",
    radioEmpty = "Radio needs streaming, and this player only plays your own files — so there are no stations here yet.",
    discoverEmpty = "Charts need an online service, which is not connected.",
    searchNoResults = "No results",
    volumeLabel = "Volume",
    setGeneral = "General",
    setGeneralSub = "Startup, updates",
    setLanguage = "Language",
    setLanguageSub = "App language",
    setAudio = "Audio",
    setAudioSub = "Output, quality, device",
    outputDevice = "Output device",
    outputDeviceDefault = "Windows default",
    setAppearance = "Appearance",
    setAppearanceSub = "Theme, layout, colors",
    setShortcuts = "Shortcuts",
    setShortcutsSub = "Keyboard shortcuts",
    setAbout = "About",
    setLyricsOnline = "Online lyrics",
    setPerformance = "Performance mode",
    appVersion = "1.1.0",

    sleepTimer = "Sleep",
    sleepOff = "Off",
    performanceMode = "Performance",
    queueRestored = "Queue restored",
    shortcutsHint = "Space play • ←/→ prev/next • S shuffle • R repeat • T sleep • E EQ • I details",

    equalizer = "Equalizer",
    eqOn = "On",
    eqOff = "Off",
    eqReset = "Reset",
    eqPresets = "Presets",
    eqManual = "Manual",
    eqBands = "Bands",
    eqTone = "Tone",
    eqBass = "Bass",
    eqBoost = "Loudness",
    eqNoise = "Noise reduction",
    eqNoiseLevel = "Strength",
    eqReverb = "Room",
    eqReverbOff = "Off",
    eqReverbSmall = "Small",
    eqReverbMedium = "Medium",
    eqReverbLarge = "Large",

    details = "Details",
    propsTags = "Tags",
    propsTitle = "Title",
    propsArtist = "Artist",
    propsAlbum = "Album",
    propsDuration = "Duration",
    propsFile = "File",
    propsName = "Name",
    propsPath = "Path",
    propsSize = "Size",
    propsFormat = "Format",
    propsMissing = "This file is no longer on disk.",
    propsAudio = "Audio",
    propsSampleRate = "Sample rate",
    propsChannels = "Channels",
    propsMono = "Mono",
    propsStereo = "Stereo",
    propsBits = "Depth",
    propsBitrate = "Bitrate (est.)",
    propsShowInFolder = "Show in folder",

    lyrics = "Lyrics",
    lyricsEnable = "Enable online search",
    lyricsPrivacy = "Online lyrics search is off. Only your artist + title are sent to a free lyrics service when you enable it — your files never leave this device.",
    lyricsLoading = "Searching…",
    lyricsNotFound = "No lyrics found for this song.",
    lyricsOffline = "No internet connection.",
    lyricsTimeout = "The service did not answer in time.",
    lyricsRateLimited = "Rate limited — try again later.",
    lyricsServerError = "The lyrics service is currently unavailable.",
    lyricsError = "Lyrics lookup failed.",
    lyricsRetry = "Retry",
    lyricsUntimed = "Plain text — no timing",
    lyricsNoMusic = "Nothing is playing right now."
)

val StringsFa = Strings(
    appName = "Alvand Player",
    language = "زبان",
    close = "بستن",

    theme = "تم ظاهری",
    themeSystem = "پیش‌فرض سیستم",
    themeLight = "روشن",
    themeDark = "تیره",
    themeColor = "تم رنگی",
    accentMono = "مینیمال",
    accentRoyal = "سلطنتی",
    accentSunset = "غروب",
    accentOcean = "اقیانوس",

    navLibrary = "کتابخانه",
    settings = "تنظیمات",
    jumpTitle = "پرش به زمان",
    jumpHint = "دقیقه:ثانیه",
    jumpGo = "برو",
    abLoop = "تکرار A–B",
    abSetA = "نقطهٔ A",
    abSetB = "نقطهٔ B",
    abClear = "پاک کردن",
    visualizer = "ویژوالایزر",
    searchHint = "جستجوی آهنگ، خواننده، پلی‌لیست…",
    noMusic = "آهنگی پیدا نشد. پوشهٔ موسیقی را انتخاب کنید یا دوباره اسکن کنید.",
    playAll = "پخش همه",
    tabPlayer = "پخش‌کننده",
    playlist = "پلی‌لیست",
    playError = "پخش این فایل ممکن نیست (مشکل فرمت یا دسترسی).",

    designSystem = "سیستم طراحی",
    palette = "پالت رنگ",
    surface = "سطوح شیشه‌ای",
    milestoneNote = "نقطهٔ ۱ — رنگ‌ها، پنجره، آیکون سیستم، تک‌نمونه‌سازی",

    chooseFolder = "انتخاب پوشه",
    openFile = "باز کردن فایل",
    dropFiles = "فایل‌ها یا پوشه‌های صوتی را اینجا رها کنید",
    unsupportedFormat = "این فرمت صوتی هنوز پشتیبانی نمی‌شود.",
    scan = "اسکن",
    scanning = "در حال اسکن",
    cancel = "لغو",
    songsFound = "آهنگ پیدا شد",
    folders = "پوشه",
    groupSongs = "آهنگ‌ها",
    groupAlbums = "آلبوم‌ها",
    groupArtists = "خواننده‌ها",
    sortDefault = "جدیدترین",
    sortTitle = "عنوان",
    sortArtist = "خواننده",
    sortLongest = "بلندترین",
    favourites = "علاقه‌مندی‌ها",

    navHome = "خانه",
    navPlaylists = "پلی‌لیست‌ها",
    navDiscover = "اکتشاف",
    navRadio = "رادیو",
    navTools = "ابزارها",
    navEffects = "افکت‌ها",
    navSettingsSection = "تنظیمات",
    appTagline = "حس موسیقی",
    greetingMorning = "صبح بخیر",
    greetingAfternoon = "ظهر بخیر",
    greetingEvening = "عصر بخیر",
    heroTitle = "موزیک زندگی را بهتر می‌کند",
    heroSubtitle = "حال تو. موزیک تو. الوند تو.",
    playYourMix = "میکس من را پخش کن",
    browse = "مرور",
    cardLibraryTitle = "کتابخانهٔ شما",
    cardLibrarySub = "آهنگ‌ها و پلی‌لیست‌های شما",
    cardDiscoverTitle = "اکتشاف",
    cardDiscoverSub = "موزیک تازه، حس تازه",
    cardRadioTitle = "رادیو",
    cardRadioSub = "ایستگاه‌های شما",
    cardSettingsTitle = "تنظیمات",
    cardSettingsSub = "تجربه‌تان را شخصی کنید",
    recentlyPlayed = "اخیراً پخش‌شده",
    viewAll = "مشاهدهٔ همه",
    nowPlaying = "در حال پخش",
    queueTitle = "صف",
    groupGenres = "ژانرها",
    newPlaylist = "پلی‌لیست تازه",
    playlistNameHint = "نام پلی‌لیست",
    playlistCreate = "ساختن",
    playlistDelete = "حذف",
    playlistRename = "تغییر نام",
    forYou = "برای شما",
    charts = "چارت‌ها",
    newReleases = "تازه‌ها",
    featuredPlaylists = "پلی‌لیست‌های منتخب",
    radioEmpty = "رادیو به استریم نیاز دارد و این پخش‌کننده فقط فایل‌های خودتان را پخش می‌کند — پس هنوز ایستگاهی اینجا نیست.",
    discoverEmpty = "چارت‌ها به سرویس آنلاین نیاز دارند که وصل نیست.",
    searchNoResults = "نتیجه‌ای نیست",
    volumeLabel = "ولوم",
    setGeneral = "عمومی",
    setGeneralSub = "شروع، به‌روزرسانی",
    setLanguage = "زبان",
    setLanguageSub = "زبان برنامه",
    setAudio = "صدا",
    setAudioSub = "خروجی، کیفیت، دستگاه",
    outputDevice = "دستگاه خروجی",
    outputDeviceDefault = "پیش‌فرض ویندوز",
    setAppearance = "ظاهر",
    setAppearanceSub = "تم، چیدمان، رنگ‌ها",
    setShortcuts = "میانبرها",
    setShortcutsSub = "میانبرهای کیبورد",
    setAbout = "درباره",
    setLyricsOnline = "متن آنلاین",
    setPerformance = "حالت سبک",
    appVersion = "1.1.0",

    sleepTimer = "خواب",
    sleepOff = "خاموش",
    performanceMode = "حالت سبک",
    queueRestored = "صف برگشت",
    shortcutsHint = "Space پخش • →/← بعدی/قبلی • S شافل • R تکرار • T خواب • E اکولایزر • I جزئیات",

    equalizer = "اکولایزر",
    eqOn = "روشن",
    eqOff = "خاموش",
    eqReset = "بازنشانی",
    eqPresets = "پریست‌ها",
    eqManual = "دستی",
    eqBands = "باندها",
    eqTone = "لحن صدا",
    eqBass = "باس",
    eqBoost = "بلندی",
    eqNoise = "کاهش نویز",
    eqNoiseLevel = "شدت",
    eqReverb = "اتاق",
    eqReverbOff = "بدون اکو",
    eqReverbSmall = "کوچک",
    eqReverbMedium = "متوسط",
    eqReverbLarge = "بزرگ",

    details = "جزئیات",
    propsTags = "تگ‌ها",
    propsTitle = "عنوان",
    propsArtist = "خواننده",
    propsAlbum = "آلبوم",
    propsDuration = "مدت",
    propsFile = "فایل",
    propsName = "نام",
    propsPath = "مسیر",
    propsSize = "حجم",
    propsFormat = "فرمت",
    propsMissing = "این فایل دیگر روی دیسک نیست.",
    propsAudio = "صدا",
    propsSampleRate = "نرخ نمونه",
    propsChannels = "کانال",
    propsMono = "تک‌کاناله",
    propsStereo = "استریو",
    propsBits = "عمق بیت",
    propsBitrate = "بیت‌ریت (تخمینی)",
    propsShowInFolder = "نمایش در پوشه",

    lyrics = "متن آهنگ",
    lyricsEnable = "روشن کردن جستجوی آنلاین",
    lyricsPrivacy = "جستجوی آنلاین متن آهنگ خاموش است. فقط با روشن کردن آن، نام خواننده و عنوان برای یک سرویس رایگان فرستاده می‌شود — فایل‌های شما هرگز از این دستگاه بیرون نمی‌روند.",
    lyricsLoading = "در حال جستجو…",
    lyricsNotFound = "متنی برای این آهنگ پیدا نشد.",
    lyricsOffline = "اینترنت وصل نیست.",
    lyricsTimeout = "سرویس به‌موقع جواب نداد.",
    lyricsRateLimited = "محدودیت سرویس — کمی بعد تلاش کنید.",
    lyricsServerError = "سرویس متن آهنگ فعلاً در دسترس نیست.",
    lyricsError = "جستجوی متن ناموفق بود.",
    lyricsRetry = "تلاش دوباره",
    lyricsUntimed = "متن ساده — بدون زمان‌بندی",
    lyricsNoMusic = "الان چیزی در حال پخش نیست."
)

val LocalStrings = staticCompositionLocalOf { StringsEn }
