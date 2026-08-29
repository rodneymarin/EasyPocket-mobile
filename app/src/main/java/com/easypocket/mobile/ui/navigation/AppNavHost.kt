package com.easypocket.mobile.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import com.easypocket.mobile.AppViewModel
import com.easypocket.mobile.i18n.t
import com.easypocket.mobile.ui.lists.ItemFormScreen
import com.easypocket.mobile.ui.lists.ListDetailScreen
import com.easypocket.mobile.ui.products.ProductFormScreen
import com.easypocket.mobile.ui.stores.StoreFormScreen

private const val PAGE_COUNT = 4

@Composable
fun AppNavHost(navController: NavHostController, vm: AppViewModel) {
    val language by vm.language.collectAsStateWithLifecycle()
    val labels = listOf(
        t("tab.lists", language),
        t("tab.products", language),
        t("tab.stores", language),
        t("tab.history", language),
    )
    var selectedPage by rememberSaveable { mutableIntStateOf(0) }
    val backStackEntry by navController.currentBackStackEntryAsState()
    val isHome = backStackEntry?.destination?.route == "home"

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            BottomBar(
                pageCount = PAGE_COUNT,
                currentPage = selectedPage,
                onPageSelected = { target ->
                    selectedPage = target
                    if (!isHome) {
                        navController.popBackStack("home", inclusive = false)
                    }
                },
                labels = labels,
            )
        },
    ) { paddingValues ->
        Box(Modifier.fillMaxSize().padding(paddingValues)) {
            NavHost(
                navController = navController,
                startDestination = "home",
                enterTransition = { slideInHorizontally { it } },
                exitTransition = { slideOutHorizontally { -it } },
                popEnterTransition = { slideInHorizontally { -it } },
                popExitTransition = { slideOutHorizontally { it } },
            ) {
                composable("home") {
                    HomePagerScreen(
                        vm = vm,
                        navController = navController,
                        selectedPage = selectedPage,
                        onPageChanged = { selectedPage = it },
                    )
                }
                composable("listDetail/{listId}") { backStackEntry ->
                    val listId = backStackEntry.arguments?.getString("listId") ?: return@composable
                    ListDetailScreen(navController = navController, listId = listId)
                }
                composable("itemForm/{listId}/{itemId}") { backStackEntry ->
                    val listId = backStackEntry.arguments?.getString("listId") ?: return@composable
                    val itemId = backStackEntry.arguments?.getString("itemId")?.toLongOrNull() ?: return@composable
                    ItemFormScreen(navController = navController, listId = listId, itemId = itemId)
                }
                composable("productForm/{productId}") { backStackEntry ->
                    val productId = backStackEntry.arguments?.getString("productId") ?: return@composable
                    ProductFormScreen(navController = navController, productId = productId)
                }
                composable("storeForm/{storeId}") { backStackEntry ->
                    val storeId = backStackEntry.arguments?.getString("storeId") ?: return@composable
                    StoreFormScreen(navController = navController, storeId = storeId)
                }
            }
        }
    }
}
