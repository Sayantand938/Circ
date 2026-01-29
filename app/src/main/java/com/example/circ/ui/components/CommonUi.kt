package com.example.circ.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.circ.ui.theme.TextLowEmphasis

/**
 * The Top Header of the app containing the Menu trigger and the App Brand name.
 */
@Composable
fun Header(onMenuClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .padding(horizontal = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        IconButton(
            onClick = onMenuClick,
            modifier = Modifier.align(Alignment.CenterStart)
        ) {
            Icon(
                imageVector = Icons.Default.Menu,
                contentDescription = "Menu",
                // Uses high contrast text color (White in Dark theme, Dark in Light theme)
                tint = MaterialTheme.colorScheme.onBackground
            )
        }

        Text(
            text = "CIRC",
            style = MaterialTheme.typography.labelLarge,
            letterSpacing = 12.sp,
            // Uses high contrast text color
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}

/**
 * A toggleable button used for switching modes (e.g., WORK vs BREAK).
 * Updates automatically based on the theme's Primary color.
 */
@Composable
fun ModeButton(label: String, active: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(25.dp))
            // Active: BrandPrimary | Inactive: Invisible
            .background(if (active) MaterialTheme.colorScheme.primary else Color.Transparent)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null // Removes the ripple for a cleaner minimal look
            ) { onClick() }
            .padding(horizontal = 32.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            // Active: BrandBackground | Inactive: Muted Gray
            color = if (active) MaterialTheme.colorScheme.onPrimary else TextLowEmphasis,
            fontWeight = FontWeight.Bold
        )
    }
}