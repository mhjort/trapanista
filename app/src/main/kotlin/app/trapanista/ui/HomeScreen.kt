package app.trapanista.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.trapanista.data.Scenario
import app.trapanista.R
import app.trapanista.data.scenarios

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    hasApiKey: Boolean,
    saved: List<Scenario>,
    onSpeak: (Scenario) -> Unit,
    onType: (Scenario) -> Unit,
    onScan: () -> Unit,
    onDelete: (Scenario) -> Unit,
    onOpenSettings: () -> Unit,
) {
    var pendingDelete by remember { mutableStateOf<Scenario?>(null) }
    pendingDelete?.let { scenario ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Delete \"${scenario.titleIt}\"?") },
            text = { Text("You can scan the exercise again later.") },
            confirmButton = {
                TextButton(onClick = { onDelete(scenario); pendingDelete = null }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text("Cancel") }
            },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(R.drawable.trapanista_logo),
                            contentDescription = null,
                            modifier = Modifier.size(40.dp),
                        )
                        Spacer(Modifier.width(10.dp))
                        Text("Trapanista")
                    }
                },
                actions = {
                    IconButton(onClick = onOpenSettings) { Icon(Icons.Default.Settings, "Settings") }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
        ) {
            if (!hasApiKey) {
                item {
                    Card(Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                        Column(Modifier.padding(16.dp)) {
                            Text("Benvenuto!", style = MaterialTheme.typography.titleMedium)
                            Text(
                                "Trapanista uses your own Anthropic API key for text practice and for feedback on spoken conversations. Add one in Settings.",
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            TextButton(onClick = onOpenSettings) { Text("Open settings") }
                        }
                    }
                }
            }
            item {
                Card(
                    Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                        .clickable(onClick = onScan),
                ) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.DocumentScanner, null, Modifier.size(32.dp))
                        Spacer(Modifier.width(16.dp))
                        Column {
                            Text("Scan an exercise", style = MaterialTheme.typography.titleMedium)
                            Text(
                                "Turn a pair exercise from your book into a conversation.",
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                }
            }
            if (saved.isNotEmpty()) {
                item {
                    Text(
                        "From your book",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 8.dp),
                    )
                }
                items(saved, key = { it.id }) { scenario ->
                    ScenarioCard(scenario, onSpeak, onType, onDelete = { pendingDelete = scenario })
                }
                item { Spacer(Modifier.height(16.dp)) }
            }
            item {
                Text(
                    "Choose a situation",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
            }
            items(scenarios, key = { it.id }) { scenario ->
                ScenarioCard(scenario, onSpeak, onType)
            }
        }
    }
}

@Composable
private fun ScenarioCard(
    scenario: Scenario,
    onSpeak: (Scenario) -> Unit,
    onType: (Scenario) -> Unit,
    onDelete: (() -> Unit)? = null,
) {
    Card(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clickable { onSpeak(scenario) },
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(scenario.emoji, fontSize = 32.sp)
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(scenario.titleIt, style = MaterialTheme.typography.titleMedium)
                Text(scenario.title, style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary)
                Text(scenario.description, style = MaterialTheme.typography.bodySmall)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = { onSpeak(scenario) }) {
                        Icon(Icons.Default.Mic, null)
                        Spacer(Modifier.width(4.dp))
                        Text("Speak")
                    }
                    TextButton(onClick = { onType(scenario) }) {
                        Icon(Icons.Default.Keyboard, null)
                        Spacer(Modifier.width(4.dp))
                        Text("Type")
                    }
                    if (onDelete != null) {
                        Spacer(Modifier.weight(1f))
                        IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, "Delete") }
                    }
                }
            }
        }
    }
}
