package com.liuh886.microuter.widget

import android.content.Context
import android.media.AudioDeviceInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.liuh886.microuter.MicRouterApp
import com.liuh886.microuter.core.model.AudioSessionState

class MicStatusWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val app = context.applicationContext as MicRouterApp
        app.audioRepository.start()
        provideContent {
            val state by app.audioRepository.state.collectAsState()
            GlanceTheme {
                Content(state)
            }
        }
    }

    @Composable
    private fun Content(state: AudioSessionState) {
        val micName = state.communicationDevice?.name
            ?: state.inputs.firstOrNull { it.type == AudioDeviceInfo.TYPE_BUILTIN_MIC }?.name
            ?: "Built-in mic"
        val active = state.btMicLinkUp || state.communicationDevice != null
        Row(
            modifier = GlanceModifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = GlanceModifier
                    .size(10.dp)
                    .background(
                        ColorProvider(
                            if (active) Color(0xFF30D158) else Color(0xFF8E8E93)
                        )
                    )
            ) {}
            Spacer(modifier = GlanceModifier.width(10.dp))
            Column(modifier = GlanceModifier.defaultWeight()) {
                Text(
                    "CURRENT MIC",
                    style = TextStyle(
                        color = ColorProvider(Color(0xFF8E8E93)),
                        fontSize = 10.sp
                    )
                )
                Spacer(modifier = GlanceModifier.height(2.dp))
                Text(
                    micName,
                    maxLines = 1,
                    style = TextStyle(
                        color = ColorProvider(Color(0xFF1C1C1E)),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
            }
            Text(
                state.modeLabel.substringBefore(' ') +
                    if (state.btMicLinkUp) "\nBT up" else "",
                style = TextStyle(
                    color = ColorProvider(Color(0xFF8E8E93)),
                    fontSize = 11.sp,
                    textAlign = TextAlign.End
                )
            )
        }
    }
}

class MicStatusWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = MicStatusWidget()
}
