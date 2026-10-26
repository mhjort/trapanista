package app.trapanista.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import app.trapanista.data.Level
import app.trapanista.data.SettingsStore
import app.trapanista.data.TutorModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(settings: SettingsStore, onBack: () -> Unit) {
    val savedKey = remember { settings.apiKey }
    var keyInput by remember { mutableStateOf("") }
    var hasKey by remember { mutableStateOf(savedKey != null) }
    var level by remember { mutableStateOf(settings.level) }
    var model by remember { mutableStateOf(settings.model) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Anthropic API key", style = MaterialTheme.typography.titleMedium)
            Text(
                if (hasKey) "A key is saved (encrypted on this device). Enter a new one to replace it."
                else "Get a key from console.anthropic.com. It is stored encrypted on this device and only sent to Anthropic.",
                style = MaterialTheme.typography.bodySmall,
            )
            OutlinedTextField(
                value = keyInput,
                onValueChange = { keyInput = it },
                label = { Text("sk-ant-…") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
            )
            Button(
                onClick = {
                    settings.apiKey = keyInput
                    hasKey = keyInput.isNotBlank()
                    keyInput = ""
                },
                enabled = keyInput.isNotBlank(),
            ) { Text("Save key") }
            if (hasKey) {
                Button(onClick = { settings.apiKey = null; hasKey = false }) { Text("Remove key") }
            }

            Text("Your level", style = MaterialTheme.typography.titleMedium)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Level.entries.forEach { l ->
                    FilterChip(
                        selected = level == l,
                        onClick = { level = l; settings.level = l },
                        label = { Text("${l.label} · ${l.description}") },
                    )
                }
            }

            Text("Model", style = MaterialTheme.typography.titleMedium)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TutorModel.entries.forEach { m ->
                    FilterChip(
                        selected = model == m,
                        onClick = { model = m; settings.model = m },
                        label = { Text(m.label) },
                    )
                }
            }
            Text(
                "Opus gives the best feedback; Haiku is the cheapest and fastest.",
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}
