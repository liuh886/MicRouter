package com.liuh886.microuter.ui.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun glassSurfaceColor(): Color {
    val dark = isSystemInDarkTheme()
    return if (dark) {
        MaterialTheme.colorScheme.surface.copy(alpha = 0.62f)
    } else {
        Color.White.copy(alpha = 0.68f)
    }
}

@Composable
fun glassStrokeColor(): Color {
    val dark = isSystemInDarkTheme()
    return if (dark) Color.White.copy(alpha = 0.10f) else Color.White.copy(alpha = 0.70f)
}

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 20.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(cornerRadius),
        colors = CardDefaults.cardColors(containerColor = glassSurfaceColor()),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(Hairline, glassStrokeColor())
    ) {
        Column(content = content)
    }
}

@Composable
fun AppBackdrop(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val dark = isSystemInDarkTheme()
    val base = MaterialTheme.colorScheme.background
    val blobAlpha = if (dark) 0.32f else 0.45f
    val c1 = MaterialTheme.colorScheme.primary.copy(alpha = blobAlpha)
    val c2 = AppPalette.IndigoLight.copy(alpha = blobAlpha * 0.85f)
    val c3 = AppPalette.GreenLight.copy(alpha = blobAlpha * 0.70f)

    Box(
        modifier
            .fillMaxSize()
            .background(base)
    ) {
        Blob(c1, 420.dp, Alignment.TopStart, -140.dp to -160.dp)
        Blob(c2, 360.dp, Alignment.TopEnd, 110.dp to 150.dp)
        Blob(c3, 320.dp, Alignment.BottomStart, (-80).dp to 50.dp)
        content()
    }
}

@Composable
private fun BoxScope.Blob(
    color: Color,
    diameter: Dp,
    anchor: Alignment,
    shift: Pair<Dp, Dp>
) {
    Box(
        Modifier
            .align(anchor)
            .offset(x = shift.first, y = shift.second)
            .size(diameter)
            .background(color, CircleShape)
            .blur(90.dp)
    )
}
