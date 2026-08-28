package com.easypocket.mobile.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.easypocket.mobile.AppViewModel

@Composable
fun AppNavHost(navController: NavHostController, vm: AppViewModel) {
    NavHost(navController = navController, startDestination = "home") {
        composable("home") { HomePagerScreen(vm = vm, navController = navController) }
        composable("listDetail/{listId}") { }
        composable("itemForm/{listId}/{itemId}") { }
        composable("productForm/{productId}") { }
        composable("storeForm/{storeId}") { }
    }
}
