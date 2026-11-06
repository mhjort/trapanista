package app.trapanista.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.trapanista.data.Scenario
import java.text.DateFormat
import java.util.Date

/** A 1-5 star score as a row of filled and empty stars. */
@Composable
fun Stars(count: Int, modifier: Modifier = Modifier, size: Dp = 20.dp) {
    Row(modifier.semantics(mergeDescendants = true) { contentDescription = "$count of 5 stars" }) {
        repeat(5) { i ->
            Icon(
                if (i < count) Icons.Filled.Star else Icons.Filled.StarBorder,
                contentDescription = null,
                tint = Gold,
                modifier = Modifier.size(size),
            )
        }
    }
}

private val Gold = Color(0xFFF2A900)

internal fun formatDate(millis: Long): String = DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(millis))

/** An exercise with Speak/Type buttons, optionally its best score and a one-line history. */
@Composable
internal fun ScenarioCard(
    scenario: Scenario,
    onSpeak: (Scenario) -> Unit,
    onType: (Scenario) -> Unit,
    modifier: Modifier = Modifier,
    best: Int? = null,
    history: String? = null,
    onDelete: (() -> Unit)? = null,
) {
    Card(
        modifier
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
                if (best != null) {
                    Row(Modifier.padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Stars(best, size = 16.dp)
                        if (history != null) {
                            Spacer(Modifier.width(8.dp))
                            Text(history, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
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
