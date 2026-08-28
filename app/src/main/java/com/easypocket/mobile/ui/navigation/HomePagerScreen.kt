package com.easypocket.mobile.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.easypocket.mobile.AppViewModel
import com.easypocket.mobile.i18n.t
import com.easypocket.mobile.ui.components.ScreenHeader
import com.easypocket.mobile.ui.theme.LocalAppColors
import kotlinx.coroutines.launch

private const val PAGE_COUNT = 3

@Composable
fun HomePagerScreen(vm: AppViewModel, navController: NavController) {
    val appColors = LocalAppColors.current
    val language by vm.language.collectAsStateWithLifecycle()
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
            Column(Modifier.fillMaxSize()) {
                ScreenHeader(
                    title = labels[page],
                    onMenuClick = { vm.openMenu() },
                )
                Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                    Text(
                        labels[page],
                        color = appColors.text,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}
