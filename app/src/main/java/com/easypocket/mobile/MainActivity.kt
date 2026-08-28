package com.easypocket.mobile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.easypocket.mobile.ui.navigation.AppRoot
import com.easypocket.mobile.ui.theme.EasyPocketTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val vm: AppViewModel = hiltViewModel()
            EasyPocketTheme(themeMode = vm.themeMode.collectAsStateWithLifecycle().value) {
                AppRoot(vm)
            }
        }
    }
}
