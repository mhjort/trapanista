package app.trapanista.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Settings
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
    onSpeak: (Scenario) -> Unit,
    onType: (Scenario) -> Unit,
    onOpenSettings: () -> Unit,
) {
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
                Text(
                    "Choose a situation",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
            }
            items(scenarios, key = { it.id }) { scenario ->
                Card(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)
                        .clickable { onSpeak(scenario) },
                ) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(scenario.emoji, fontSize = 32.sp)
                        Spacer(Modifier.width(16.dp))
                        Column {
                            Text(scenario.titleIt, style = MaterialTheme.typography.titleMedium)
                            Text(scenario.title, style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary)
                            Text(scenario.description, style = MaterialTheme.typography.bodySmall)
                            Row {
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
                            }
                        }
                    }
                }
            }
        }
    }
}
