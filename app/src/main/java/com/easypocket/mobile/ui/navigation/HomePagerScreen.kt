package com.easypocket.mobile.ui.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.easypocket.mobile.AppViewModel
import com.easypocket.mobile.ui.history.HistoryScreen
import com.easypocket.mobile.ui.lists.ListsScreen
import com.easypocket.mobile.ui.products.ProductsScreen
import com.easypocket.mobile.ui.stores.StoresScreen
import kotlinx.coroutines.flow.drop

private const val PAGE_COUNT = 4

@Composable
fun HomePagerScreen(
    vm: AppViewModel,
    navController: NavController,
    selectedPage: Int,
    onPageChanged: (Int) -> Unit,
) {
    val language by vm.language.collectAsStateWithLifecycle()
    val refreshTick by vm.refreshTick.collectAsStateWithLifecycle()

    val pagerState = rememberPagerState(initialPage = selectedPage, pageCount = { PAGE_COUNT })

    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.settledPage }.drop(1).collect { onPageChanged(it) }
    }

    LaunchedEffect(selectedPage) {
        if (pagerState.currentPage != selectedPage) {
            pagerState.animateScrollToPage(selectedPage)
        }
    }

    HorizontalPager(
        state = pagerState,
        modifier = Modifier.fillMaxSize(),
    ) { page ->
        when (page) {
            0 -> ListsScreen(
                navController = navController,
                onMenuClick = { vm.openMenu() },
                refreshTick = refreshTick,
            )
            1 -> ProductsScreen(
                navController = navController,
                onMenuClick = { vm.openMenu() },
                refreshTick = refreshTick,
            )
            2 -> StoresScreen(
                navController = navController,
                onMenuClick = { vm.openMenu() },
                refreshTick = refreshTick,
            )
            3 -> HistoryScreen(
                onMenuClick = { vm.openMenu() },
            )
        }
    }
}
