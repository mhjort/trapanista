package app.trapanista

import android.media.AudioManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import app.trapanista.data.ProgressStore
import app.trapanista.data.ScenarioStore
import app.trapanista.data.SettingsStore
import app.trapanista.data.bestById
import app.trapanista.data.recommend
import app.trapanista.data.scenarios
import app.trapanista.ui.ChatScreen
import app.trapanista.ui.ChatViewModel
import app.trapanista.ui.ExercisesScreen
import app.trapanista.ui.HomeScreen
import app.trapanista.ui.ScanScreen
import app.trapanista.ui.ScanViewModel
import app.trapanista.ui.SettingsScreen
import app.trapanista.ui.TrapanistaTheme
import app.trapanista.ui.VoiceScreen
import app.trapanista.ui.VoiceViewModel

private const val HOME = "home"
private const val SETTINGS = "settings"
private const val SCAN = "scan"
private const val EXERCISES = "exercises"
private const val CHAT = "chat:"
private const val VOICE = "voice:"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.Theme_Trapanista)
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val settings = SettingsStore(applicationContext)
        val store = ScenarioStore(applicationContext)
        val progress = ProgressStore(applicationContext)
        val firebaseReady = (application as TrapanistaApp).firebaseReady
        val audioManager = applicationContext.getSystemService(AudioManager::class.java)
        setContent {
            TrapanistaTheme {
                // HOME, SETTINGS, SCAN, EXERCISES, "chat:<scenario id>" or "voice:<scenario id>".
                var screen by rememberSaveable { mutableStateOf(HOME) }
                var deletions by remember { mutableIntStateOf(0) }
                BackHandler(enabled = screen != HOME) { screen = HOME }
                when {
                    screen == HOME -> {
                        // Book exercises first (newest first), so a fresh scan is suggested next.
                        val recommended = remember(screen) { recommend(store.saved() + scenarios, progress.attempts()) }
                        HomeScreen(
                            hasApiKey = remember(screen) { settings.apiKey != null },
                            recommended = recommended,
                            recommendedBest = remember(recommended) {
                                recommended?.let { progress.attempts().bestById()[it.id] }
                            },
                            onSpeak = { screen = VOICE + it.id },
                            onType = { screen = CHAT + it.id },
                            onScan = { screen = SCAN },
                            onOpenExercises = { screen = EXERCISES },
                            onOpenSettings = { screen = SETTINGS },
                        )
                    }
                    screen == EXERCISES -> ExercisesScreen(
                        saved = remember(screen, deletions) { store.saved() },
                        attempts = remember(screen) { progress.attempts() },
                        onSpeak = { screen = VOICE + it.id },
                        onType = { screen = CHAT + it.id },
                        onDelete = { store.delete(it.id); deletions++ },
                        onBack = { screen = HOME },
                    )
                    screen == SETTINGS -> SettingsScreen(settings, onBack = { screen = HOME })
                    screen == SCAN -> {
                        val vm = viewModel(key = screen) {
                            ScanViewModel(settings, store, applicationContext.contentResolver)
                        }
                        ScanScreen(vm, onBack = { screen = HOME }, onStart = { screen = VOICE + it.id })
                    }
                    screen.startsWith(CHAT) -> {
                        val scenario = remember(screen) { store.find(screen.removePrefix(CHAT)) }
                        if (scenario == null) {
                            LaunchedEffect(Unit) { screen = HOME }
                        } else {
                            val vm = viewModel(key = screen) { ChatViewModel(scenario, settings, progress) }
                            ChatScreen(scenario, vm, onBack = { screen = HOME })
                        }
                    }
                    screen.startsWith(VOICE) -> {
                        val scenario = remember(screen) { store.find(screen.removePrefix(VOICE)) }
                        if (scenario == null) {
                            LaunchedEffect(Unit) { screen = HOME }
                        } else {
                            val vm = viewModel(key = screen) {
                                VoiceViewModel(scenario, settings, progress, firebaseReady, audioManager)
                            }
                            VoiceScreen(scenario, vm, onBack = { screen = HOME })
                        }
                    }
                }
            }
        }
    }
}
