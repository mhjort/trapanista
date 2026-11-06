package app.trapanista.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.trapanista.data.Attempt
import app.trapanista.data.Scenario
import app.trapanista.data.bestById
import app.trapanista.data.scenarios

private const val RECENT_RESULTS = 10

/** Every exercise with its best score, plus the learner's most recent results. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExercisesScreen(
    saved: List<Scenario>,
    attempts: List<Attempt>,
    onSpeak: (Scenario) -> Unit,
    onType: (Scenario) -> Unit,
    onDelete: (Scenario) -> Unit,
    onBack: () -> Unit,
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

    val byId = remember(saved) { (saved + scenarios).associateBy { it.id } }
    // Results of deleted exercises stay stored but aren't shown.
    val visible = remember(attempts, byId) { attempts.filter { it.scenarioId in byId } }
    val best = remember(visible) { visible.bestById() }
    val byScenario = remember(visible) { visible.groupBy { it.scenarioId } }
    fun history(id: String): String? = byScenario[id]?.let { list ->
        val n = list.size
        "$n attempt${if (n == 1) "" else "s"} · last ${formatDate(list.first().at)}"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Exercises & results") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
        ) {
            if (visible.isNotEmpty()) {
                item { SectionTitle("Recent results") }
                item {
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            visible.take(RECENT_RESULTS).forEach { a -> ResultRow(byId.getValue(a.scenarioId), a) }
                        }
                    }
                }
                item { Spacer(Modifier.height(16.dp)) }
            }
            if (saved.isNotEmpty()) {
                item { SectionTitle("From your book") }
                items(saved, key = { it.id }) { scenario ->
                    ScenarioCard(
                        scenario, onSpeak, onType,
                        best = best[scenario.id], history = history(scenario.id),
                        onDelete = { pendingDelete = scenario },
                    )
                }
                item { Spacer(Modifier.height(16.dp)) }
            }
            item { SectionTitle("Situations") }
            items(scenarios, key = { it.id }) { scenario ->
                ScenarioCard(scenario, onSpeak, onType, best = best[scenario.id], history = history(scenario.id))
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 8.dp))
}

@Composable
private fun ResultRow(scenario: Scenario, attempt: Attempt) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(scenario.emoji, fontSize = 20.sp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(scenario.titleIt, style = MaterialTheme.typography.bodyMedium)
            Text(
                "${formatDate(attempt.at)} · ${attempt.mode.label}",
                style = MaterialTheme.typography.labelSmall,
            )
        }
        Stars(attempt.stars, size = 16.dp)
    }
}
