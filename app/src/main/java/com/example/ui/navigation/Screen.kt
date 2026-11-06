package com.example.ui.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Warning
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.R

sealed class Screen(
    val route: String,
    @StringRes val titleRes: Int,
    val icon: ImageVector
) {
    data object Dashboard : Screen("dashboard", R.string.nav_dashboard, Icons.Default.Dashboard)
    data object Sos : Screen("sos", R.string.nav_sos, Icons.Default.Warning)
    data object Messages : Screen("messages", R.string.nav_messages, Icons.Default.Chat)
    data object Radar : Screen("radar", R.string.nav_map, Icons.Default.Radar)
    data object Hardware : Screen("hardware", R.string.nav_nearby, Icons.Default.Devices)

    companion object {
        val bottomNavItems: List<Screen>
            get() = listOf(Dashboard, Sos, Messages, Radar, Hardware)
    }
}
