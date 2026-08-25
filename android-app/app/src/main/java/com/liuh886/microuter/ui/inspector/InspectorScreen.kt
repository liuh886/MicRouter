package com.liuh886.microuter.ui.inspector

import android.content.ClipData
import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewModelScope
import com.liuh886.microuter.core.model.RouteEvent
import com.liuh886.microuter.data.AudioRepository
import com.liuh886.microuter.ui.components.EmptyHint
import com.liuh886.microuter.ui.theme.GlassCard
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class InspectorViewModel(private val repository: AudioRepository) : ViewModel() {

    private val _events = MutableStateFlow(repository.events.value)
    val events = _events.asStateFlow()

    init {
        viewModelScope.launch {
            repository.events.collect { _events.value = it }
        }
    }
}

@Composable
fun InspectorScreen(repository: AudioRepository, onBack: () -> Unit = {}) {
    val viewModel: InspectorViewModel = viewModel { InspectorViewModel(repository) }
    val events by viewModel.events.collectAsStateWithLifecycle()
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current

    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            IconButton(onClick = onBack, modifier = Modifier.padding(end = 4.dp)) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
            Text(
                "Route Log",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = {
                clipboard.setText(AnnotatedString(repository.diagnosticReport()))
            }) { Text("Copy") }
            TextButton(onClick = { shareLog(context, repository.diagnosticReport()) }) {
                Text("Share")
            }
        }
        if (events.isEmpty()) {
            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                EmptyHint("No route events yet.\nConnect a device or start a call to see live routing.")
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                items(events, key = { it.seq }) { event ->
                    EventCard(event)
                }
            }
        }
    }
}

@Composable
private fun EventCard(event: RouteEvent) {
    GlassCard(cornerRadius = 16.dp) {
        Column(Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(event.kindLabel, style = MaterialTheme.typography.titleSmall)
                Text(
                    timeFormat.format(Date(event.timestampMs)),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.padding(top = 2.dp))
            Text(
                event.message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private val timeFormat = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault())

private fun shareLog(context: android.content.Context, text: String) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
        clipData = ClipData.newPlainText("route log", text)
    }
    context.startActivity(Intent.createChooser(intent, "Share route log"))
}
