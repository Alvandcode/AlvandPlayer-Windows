package com.alvand.player.ui.shell

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.alvand.player.player.EngineState
import com.alvand.player.ui.theme.LocalAP

/**
 * چیدمان اصلی دسکتاپ — موبه‌مو از روی طرح:
 *
 * ```
 * ┌──────────┬────────────────────────────────┬──────────────┐
 * │          │  نوار جستجو                     │              │
 * │ سایدبار  ├────────────────────────────────┤  در حال پخش  │
 * │  + مینی  │  صفحهٔ فعال                    │  + صف       │
 * │  کارت    │                                │  + متن آهنگ  │
 * └──────────┴────────────────────────────────┴──────────────┘
 * ```
 *
 * وقتی پنجره به‌اندازهٔ کافی پهن نباشد، پنل «در حال پخش» سمت راست جای خود را به
 * **مینی‌پلیر پایین** می‌دهد — همان چیزی که در طرح، پنجره‌های کوچک‌تر نشان داده شده.
 */
@Composable
fun AppShell(
    screen: Screen,
    onNavigate: (Screen) -> Unit,
    accent: Color,

    // سایدبار
    miniTitle: String?,
    miniArtist: String?,
    miniPlaying: Boolean,
    onMiniClick: () -> Unit,

    // نوار جستجو
    query: String,
    onQueryChange: (String) -> Unit,
    searchResults: List<com.alvand.player.model.Song>,
    searchFocusRequester: androidx.compose.ui.focus.FocusRequester,
    onPlaySong: (com.alvand.player.model.Song) -> Unit,

    // محتوای صفحهٔ فعال
    content: @Composable () -> Unit,

    // پنل «در حال پخش»
    engineState: EngineState,
    liked: Boolean,
    onToggleLike: () -> Unit,
    onTogglePlay: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSeek: (Long) -> Unit,
    onToggleShuffle: () -> Unit,
    onCycleRepeat: () -> Unit,
    onSelectQueueIndex: (Int) -> Unit,
    onMoveQueueItem: (Int, Int) -> Unit = { _, _ -> },
    onRemoveFromQueue: (Int) -> Unit = { _ -> },
    onClearQueue: () -> Unit = {},
    onOpenLyrics: () -> Unit,
    onShowProperties: () -> Unit = {},
    onOpenSettings: () -> Unit,
    onOpenMini: () -> Unit,

    // مینی‌پلیر (وقتی پنل راست جا نشود)
    onMiniPlay: () -> Unit = onTogglePlay,
    onMiniSeek: (Long) -> Unit = onSeek,
    modifier: Modifier = Modifier
) {
    val pal = LocalAP.current

    BoxWithConstraints(
        modifier
            .fillMaxSize()
            .background(pal.bg)
    ) {
        // اگر عرض واقعی از این کمتر بود، پنل راست به مینی‌پلیر پایین تبدیل می‌شود.
        val wideEnoughForPanel = maxWidth >= 980.dp

        Row(Modifier.fillMaxSize()) {
            Sidebar(
                current = screen,
                onNavigate = onNavigate,
                accent = accent,
                miniTitle = miniTitle,
                miniArtist = miniArtist,
                miniPlaying = miniPlaying,
                onMiniClick = onMiniClick
            )

            Column(Modifier.weight(1f)) {
                Box(Modifier.weight(1f)) {
                    Column(Modifier.fillMaxSize()) {
                        TopSearchBar(
                            query = query,
                            onQueryChange = onQueryChange,
                            results = searchResults,
                            focusRequester = searchFocusRequester,
                            onPlaySong = onPlaySong,
                            onOpenSettings = onOpenSettings
                        )
                        Box(Modifier.weight(1f)) { content() }
                    }
                }
                if (!wideEnoughForPanel) {
                    MiniPlayerBar(
                        state = engineState,
                        accent = accent,
                        onTogglePlay = onMiniPlay,
                        onNext = onNext,
                        onPrevious = onPrevious,
                        onSeek = onMiniSeek
                    )
                }
            }

            if (wideEnoughForPanel) {
                NowPlayingPanel(
                    state = engineState,
                    accent = accent,
                    liked = liked,
                    onToggleLike = onToggleLike,
                    onTogglePlay = onTogglePlay,
                    onNext = onNext,
                    onPrevious = onPrevious,
                    onSeek = onSeek,
                    onToggleShuffle = onToggleShuffle,
                    onCycleRepeat = onCycleRepeat,
                    onSelectQueueIndex = onSelectQueueIndex,
                    onMoveQueueItem = onMoveQueueItem,
                    onRemoveFromQueue = onRemoveFromQueue,
                    onClearQueue = onClearQueue,
                    onOpenLyrics = onOpenLyrics,
                    onShowProperties = onShowProperties
                )
            }
        }
    }
}