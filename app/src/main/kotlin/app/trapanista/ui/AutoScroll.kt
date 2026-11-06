package app.trapanista.ui

import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect

/**
 * Keeps the list scrolled to the very bottom whenever any of [keys] changes. Scrolling to the
 * last item alone isn't enough: it aligns the item's top, so a long or growing bubble runs off-screen.
 */
@Composable
internal fun AutoScrollToBottom(listState: LazyListState, vararg keys: Any?) {
    LaunchedEffect(*keys) {
        val count = listState.layoutInfo.totalItemsCount
        if (count == 0) return@LaunchedEffect
        listState.scrollToItem(count - 1)
        // The list clamps this to however far it can actually scroll.
        listState.scrollBy(100_000f)
    }
}
