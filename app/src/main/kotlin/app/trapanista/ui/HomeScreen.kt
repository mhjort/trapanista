package app.trapanista.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.DocumentScanner
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import app.trapanista.R
import app.trapanista.data.Scenario

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    hasApiKey: Boolean,
    /** Null once every exercise has 5 stars. */
    recommended: Scenario?,
    /** The recommended exercise's best score so far, if it has been done. */
    recommendedBest: Int?,
    onSpeak: (Scenario) -> Unit,
    onType: (Scenario) -> Unit,
    onScan: () -> Unit,
    onOpenExercises: () -> Unit,
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
                    IconButton(onClick = onOpenExercises) {
                        Icon(Icons.AutoMirrored.Filled.List, "All exercises")
                    }
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
                    "Recommended for you",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
            }
            item {
                if (recommended != null) {
                    ScenarioCard(
                        recommended,
                        onSpeak,
                        onType,
                        best = recommendedBest,
                        history = recommendedBest?.let { "best so far" },
                    )
                } else {
                    Card(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                        Column(Modifier.padding(16.dp)) {
                            Text("Tutto fatto! 🎉", style = MaterialTheme.typography.titleMedium)
                            Text(
                                "Every exercise has 5 stars. Scan a new one from your book, or practise any exercise again.",
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                }
            }
            item {
                NavCard(
                    Icons.AutoMirrored.Filled.List,
                    "All exercises & results",
                    "See your past scores and try any exercise.",
                    onOpenExercises,
                    Modifier.padding(top = 16.dp),
                )
            }
            item {
                NavCard(
                    Icons.Default.DocumentScanner,
                    "Scan an exercise",
                    "Turn a pair exercise from your book into a conversation.",
                    onScan,
                    Modifier.padding(top = 12.dp),
                )
            }
        }
    }
}

@Composable
private fun NavCard(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit, modifier: Modifier) {
    Card(modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, Modifier.size(32.dp))
            Spacer(Modifier.width(16.dp))
            Column {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(subtitle, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
