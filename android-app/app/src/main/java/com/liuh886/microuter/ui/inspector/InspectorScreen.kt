package com.liuh886.microuter.ui.inspector

import android.content.ClipData
import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.liuh886.microuter.core.model.RouteEvent
import com.liuh886.microuter.data.AudioRepository
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
fun InspectorScreen(repository: AudioRepository) {
    val viewModel: InspectorViewModel = viewModel { InspectorViewModel(repository) }
    val events by viewModel.events.collectAsStateWithLifecycle()
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current

    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Route timeline", style = MaterialTheme.typography.titleLarge)
            Row {
                TextButton(onClick = { clipboard.setText(AnnotatedString(repository.exportLog())) }) {
                    Text("Copy")
                }
                TextButton(onClick = { shareLog(context, repository.exportLog()) }) {
                    Text("Share")
                }
            }
        }
        if (events.isEmpty()) {
            Text(
                "No route events yet. Connect a device or start a call.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            items(events, key = { it.timestampMs to it.kind }) { event ->
                EventRow(event)
            }
        }
    }
}

@Composable
private fun EventRow(event: RouteEvent) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(timeFormat.format(Date(event.timestampMs)), style = MaterialTheme.typography.labelSmall)
            Text(event.kindLabel, style = MaterialTheme.typography.titleSmall)
            Text(
                event.message,
                style = MaterialTheme.typography.bodyMedium,
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
