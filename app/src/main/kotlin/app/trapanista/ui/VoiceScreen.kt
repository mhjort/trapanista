package app.trapanista.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.trapanista.data.Scenario
import app.trapanista.llm.Review
import app.trapanista.llm.Speaker
import app.trapanista.llm.TranscriptLine

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceScreen(scenario: Scenario, viewModel: VoiceViewModel, onBack: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val listState = rememberLazyListState()

    val requestMic = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            @Suppress("MissingPermission")
            viewModel.start()
        }
    }
    fun startWithPermission() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED
        ) {
            viewModel.start()
        } else {
            requestMic.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    LaunchedEffect(state.transcript.size, state.phase) {
        val count = listState.layoutInfo.totalItemsCount
        if (count > 0) listState.animateScrollToItem(count - 1)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(scenario.titleIt)
                        Text("${scenario.title} · voice", style = MaterialTheme.typography.labelMedium)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { viewModel.finish(); onBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item {
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(Modifier.padding(12.dp)) {
                            Text("${scenario.emoji} Your goal", fontWeight = FontWeight.Bold)
                            Text(scenario.description, style = MaterialTheme.typography.bodyMedium)
                            Text(
                                "Speak Italian out loud. When you finish, you'll get feedback on what you said.",
                                style = MaterialTheme.typography.bodySmall,
                                fontStyle = FontStyle.Italic,
                            )
                        }
                    }
                }
                items(state.transcript) { TranscriptBubble(it) }
                if (state.sceneComplete && state.phase == VoicePhase.LIVE) {
                    item { Text("Bravo! 🎉 Scene complete. Tap stop to get your feedback.", fontWeight = FontWeight.Bold) }
                }
                if (state.phase == VoicePhase.REVIEWING) {
                    item {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(Modifier.size(20.dp))
                            Spacer(Modifier.width(12.dp))
                            Text("Reviewing your Italian…")
                        }
                    }
                }
                state.review?.let { review -> item { ReviewCard(review) } }
                state.error?.let { error ->
                    item {
                        Column {
                            Text(error, color = MaterialTheme.colorScheme.error)
                            if (state.phase == VoicePhase.REVIEWED && state.review == null) {
                                TextButton(onClick = viewModel::review) { Text("Try review again") }
                            }
                        }
                    }
                }
            }

            Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                when (state.phase) {
                    VoicePhase.IDLE, VoicePhase.REVIEWED -> Button(onClick = ::startWithPermission) {
                        Icon(Icons.Default.Mic, null)
                        Spacer(Modifier.width(8.dp))
                        Text(if (state.phase == VoicePhase.IDLE) "Start speaking" else "Practise again")
                    }
                    VoicePhase.CONNECTING -> CircularProgressIndicator()
                    VoicePhase.LIVE -> Button(
                        onClick = viewModel::finish,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    ) {
                        Icon(Icons.Default.Stop, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Stop & get feedback")
                    }
                    VoicePhase.REVIEWING -> Unit
                }
            }
        }
    }
}

@Composable
private fun TranscriptBubble(line: TranscriptLine) {
    val learner = line.speaker == Speaker.LEARNER
    Column(Modifier.fillMaxWidth(), horizontalAlignment = if (learner) Alignment.End else Alignment.Start) {
        Surface(
            color = if (learner) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
            shape = if (learner) RoundedCornerShape(16.dp, 16.dp, 4.dp, 16.dp) else RoundedCornerShape(16.dp, 16.dp, 16.dp, 4.dp),
            modifier = Modifier.widthIn(max = 320.dp),
        ) {
            Text(line.text, Modifier.padding(12.dp), style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Composable
private fun ReviewCard(review: Review) {
    Surface(
        color = MaterialTheme.colorScheme.tertiaryContainer,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Feedback", style = MaterialTheme.typography.titleMedium)
            Text(review.summary)
            review.wentWell.forEach { Text("✅ $it", style = MaterialTheme.typography.bodyMedium) }

            val corrected = review.lines.filter { it.corrections.isNotEmpty() }
            if (corrected.isNotEmpty()) {
                Text("Corrections", style = MaterialTheme.typography.titleSmall)
                corrected.forEach { line ->
                    Text("“${line.text}”", fontStyle = FontStyle.Italic, style = MaterialTheme.typography.bodyMedium)
                    line.corrections.forEach { CorrectionRow(it, alignEnd = false) }
                }
            }

            if (review.usefulPhrases.isNotEmpty()) {
                Text("Useful phrases", style = MaterialTheme.typography.titleSmall)
                review.usefulPhrases.forEach {
                    Column {
                        Text(it.italian, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                        Text(it.english, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}
