package app.trapanista.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AssistChip
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.trapanista.data.Scenario
import app.trapanista.llm.Correction

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(scenario: Scenario, viewModel: ChatViewModel, onBack: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var input by rememberSaveable { mutableStateOf("") }
    val listState = rememberLazyListState()

    LaunchedEffect(state.items.size, state.loading) {
        val count = listState.layoutInfo.totalItemsCount
        if (count > 0) listState.animateScrollToItem(count - 1)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(scenario.titleIt)
                        Text(scenario.title, style = MaterialTheme.typography.labelMedium)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
                },
                actions = {
                    IconButton(onClick = viewModel::restart) { Icon(Icons.Default.Refresh, "Restart scene") }
                },
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).imePadding()) {
            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item { GoalCard(scenario) }
                itemsIndexed(state.items) { _, item ->
                    when (item) {
                        is ChatItem.Partner -> PartnerBubble(item)
                        is ChatItem.Learner -> LearnerBubble(item)
                    }
                }
                if (state.loading) {
                    item {
                        Box(Modifier.fillMaxWidth().padding(8.dp)) {
                            CircularProgressIndicator(Modifier.size(24.dp))
                        }
                    }
                }
                state.error?.let { error ->
                    item {
                        Column {
                            Text(error, color = MaterialTheme.colorScheme.error)
                            TextButton(onClick = viewModel::retry) { Text("Try again") }
                        }
                    }
                }
                if (state.sceneComplete) {
                    item {
                        Surface(
                            color = MaterialTheme.colorScheme.tertiaryContainer,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Column(Modifier.padding(16.dp)) {
                                Text("Bravo! 🎉 Scene complete.", fontWeight = FontWeight.Bold)
                                TextButton(onClick = viewModel::restart) { Text("Practise again") }
                            }
                        }
                    }
                }
            }

            if (state.suggestions.isNotEmpty()) {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(state.suggestions) { s ->
                        AssistChip(
                            onClick = { input = s.italian },
                            label = {
                                Column(Modifier.padding(vertical = 4.dp)) {
                                    Text(s.italian)
                                    Text(s.english, style = MaterialTheme.typography.labelSmall)
                                }
                            },
                        )
                    }
                }
            }

            Row(
                Modifier.fillMaxWidth().padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    placeholder = { Text("Scrivi in italiano…") },
                    modifier = Modifier.weight(1f),
                    enabled = !state.sceneComplete,
                    maxLines = 4,
                )
                IconButton(
                    onClick = { viewModel.send(input); input = "" },
                    enabled = input.isNotBlank() && !state.loading && !state.sceneComplete,
                ) { Icon(Icons.AutoMirrored.Filled.Send, "Send") }
            }
        }
    }
}

@Composable
private fun GoalCard(scenario: Scenario) {
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(12.dp)) {
            Text("${scenario.emoji} Your goal", fontWeight = FontWeight.Bold)
            Text(scenario.description, style = MaterialTheme.typography.bodyMedium)
            Text(
                "Tap the tutor's message to see the translation.",
                style = MaterialTheme.typography.bodySmall,
                fontStyle = FontStyle.Italic,
            )
        }
    }
}

@Composable
private fun PartnerBubble(item: ChatItem.Partner) {
    var showTranslation by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxWidth()) {
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(16.dp, 16.dp, 16.dp, 4.dp),
            modifier = Modifier.widthIn(max = 320.dp).clickable { showTranslation = !showTranslation },
        ) {
            Column(Modifier.padding(12.dp)) {
                Text(item.italian, style = MaterialTheme.typography.bodyLarge)
                if (showTranslation) {
                    Text(
                        item.english,
                        style = MaterialTheme.typography.bodySmall,
                        fontStyle = FontStyle.Italic,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }
        }
        if (item.tip.isNotBlank()) {
            Text(
                "💡 ${item.tip}",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.widthIn(max = 320.dp).padding(start = 8.dp, top = 4.dp),
            )
        }
    }
}

@Composable
private fun LearnerBubble(item: ChatItem.Learner) {
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.End) {
        Surface(
            color = MaterialTheme.colorScheme.primaryContainer,
            shape = RoundedCornerShape(16.dp, 16.dp, 4.dp, 16.dp),
            modifier = Modifier.widthIn(max = 320.dp),
        ) {
            Text(item.text, Modifier.padding(12.dp), style = MaterialTheme.typography.bodyLarge)
        }
        item.corrections.forEach { CorrectionRow(it) }
    }
}

@Composable
private fun CorrectionRow(c: Correction) {
    Column(Modifier.widthIn(max = 320.dp).padding(top = 4.dp, end = 8.dp), horizontalAlignment = Alignment.End) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                c.original,
                style = MaterialTheme.typography.bodySmall,
                textDecoration = TextDecoration.LineThrough,
                color = MaterialTheme.colorScheme.error,
            )
            Text("→ ${c.corrected}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
        }
        Text(c.note, style = MaterialTheme.typography.bodySmall, fontStyle = FontStyle.Italic)
    }
}
