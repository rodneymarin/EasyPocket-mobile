package com.easypocket.mobile.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.easypocket.mobile.AppViewModel
import com.easypocket.mobile.ui.lists.ItemFormScreen
import com.easypocket.mobile.ui.lists.ListDetailScreen

@Composable
fun AppNavHost(navController: NavHostController, vm: AppViewModel) {
    NavHost(navController = navController, startDestination = "home") {
        composable("home") { HomePagerScreen(vm = vm, navController = navController) }
        composable("listDetail/{listId}") { backStackEntry ->
            val listId = backStackEntry.arguments?.getString("listId") ?: return@composable
            ListDetailScreen(navController = navController, listId = listId)
        }
        composable("itemForm/{listId}/{itemId}") { backStackEntry ->
            val listId = backStackEntry.arguments?.getString("listId") ?: return@composable
            val itemId = backStackEntry.arguments?.getString("itemId")?.toLongOrNull() ?: return@composable
            ItemFormScreen(navController = navController, listId = listId, itemId = itemId)
        }
        composable("productForm/{productId}") { }
        composable("storeForm/{storeId}") { }
    }
}
