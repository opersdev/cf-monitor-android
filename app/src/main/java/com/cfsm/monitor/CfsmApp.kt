package com.cfsm.monitor

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.cfsm.monitor.ui.DetailScreen
import com.cfsm.monitor.ui.HomeScreen
import com.cfsm.monitor.ui.SettingsScreen
import com.cfsm.monitor.ui.SetupScreen
import com.cfsm.monitor.ui.theme.CfsmTheme

@Composable
fun CfsmApp(vm: MonitorViewModel) {
    CfsmTheme(themeMode = vm.themeMode) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background,
        ) {
            val screen = vm.screen
            BackHandler(enabled = screen is Screen.Detail || screen is Screen.Settings) {
                vm.goHome()
            }
            AppLifecycle(vm)

            when (screen) {
                is Screen.Home -> HomeScreen(vm)
                is Screen.Setup -> SetupScreen(vm)
                is Screen.Settings -> SettingsScreen(vm)
                is Screen.Detail -> DetailScreen(vm)
            }
        }
    }
}

@Composable
private fun AppLifecycle(vm: MonitorViewModel) {
    val owner = LocalLifecycleOwner.current
    DisposableEffect(owner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> vm.onAppForeground()
                Lifecycle.Event.ON_STOP -> vm.onAppBackground()
                else -> Unit
            }
        }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer) }
    }
}
