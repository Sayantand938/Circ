package com.example.circ.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.circ.data.TimerConstants
import com.example.circ.ui.theme.TextDisabled
import com.example.circ.ui.theme.TextLowEmphasis

@Composable
fun AppDrawer(navController: NavHostController, onClose: () -> Unit) {
    val currentBackStack by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStack?.destination?.route

    Column(
        modifier = Modifier
            .fillMaxHeight()
            .fillMaxWidth(0.75f)
            .background(MaterialTheme.colorScheme.surface)
            .statusBarsPadding()
            .padding(24.dp)
    ) {
        Text(
            text = "CIRC",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 8.sp,
            modifier = Modifier.padding(top = 20.dp, bottom = 16.dp)
        )

        HorizontalDivider(
            modifier = Modifier.padding(bottom = 24.dp),
            thickness = 1.dp,
            color = MaterialTheme.colorScheme.outline
        )

        val menuItems = listOf(
            TimerConstants.ROUTE_TIMER,
            TimerConstants.ROUTE_DASHBOARD,
            TimerConstants.ROUTE_HISTORY,
            TimerConstants.ROUTE_SETTINGS
        )

        menuItems.forEach { route ->
            val isSelected = currentRoute == route

            Text(
                text = route.uppercase(),
                color = if (isSelected) MaterialTheme.colorScheme.primary else TextLowEmphasis,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        onClose()
                        if (currentRoute != route) {
                            navController.navigate(route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    }
                    .padding(vertical = 18.dp),
                style = MaterialTheme.typography.labelLarge,
                fontSize = 14.sp,
                letterSpacing = 2.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            )
        }

        Spacer(Modifier.weight(1f))

        Text(
            text = "v1.0.0",
            color = TextDisabled,
            fontSize = 10.sp,
            letterSpacing = 1.sp
        )
    }
}