package com.easypocket.mobile.ui.navigation

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.easypocket.mobile.AppViewModel
import com.easypocket.mobile.i18n.t
import com.easypocket.mobile.ui.lists.ListsScreen
import com.easypocket.mobile.ui.products.ProductsScreen
import com.easypocket.mobile.ui.stores.StoresScreen
import kotlinx.coroutines.launch

private const val PAGE_COUNT = 3

@Composable
fun HomePagerScreen(vm: AppViewModel, navController: NavController) {
    val language by vm.language.collectAsStateWithLifecycle()
    val refreshTick by vm.refreshTick.collectAsStateWithLifecycle()
    val labels = listOf(
        t("tab.lists", language),
        t("tab.products", language),
        t("tab.stores", language),
    )

    var currentPage by rememberSaveable { mutableIntStateOf(0) }
    val pagerState = rememberPagerState(initialPage = currentPage, pageCount = { PAGE_COUNT })
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }.collect { currentPage = it }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            BottomBar(
                pageCount = PAGE_COUNT,
                currentPage = currentPage,
                onPageSelected = { target ->
                    currentPage = target
                    coroutineScope.launch { pagerState.animateScrollToPage(target) }
                },
                labels = labels,
            )
        },
    ) { paddingValues ->
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize().padding(paddingValues),
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
            }
        }
    }
}
