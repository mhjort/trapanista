package app.trapanista

import android.media.AudioManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import app.trapanista.data.SettingsStore
import app.trapanista.data.scenarios
import app.trapanista.ui.ChatScreen
import app.trapanista.ui.ChatViewModel
import app.trapanista.ui.HomeScreen
import app.trapanista.ui.SettingsScreen
import app.trapanista.ui.TrapanistaTheme
import app.trapanista.ui.VoiceScreen
import app.trapanista.ui.VoiceViewModel

private const val HOME = "home"
private const val SETTINGS = "settings"
private const val CHAT = "chat:"
private const val VOICE = "voice:"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val settings = SettingsStore(applicationContext)
        val firebaseReady = (application as TrapanistaApp).firebaseReady
        val audioManager = applicationContext.getSystemService(AudioManager::class.java)
        setContent {
            TrapanistaTheme {
                // HOME, SETTINGS, "chat:<scenario id>" or "voice:<scenario id>".
                var screen by rememberSaveable { mutableStateOf(HOME) }
                BackHandler(enabled = screen != HOME) { screen = HOME }
                when {
                    screen == HOME -> HomeScreen(
                        hasApiKey = remember(screen) { settings.apiKey != null },
                        onSpeak = { screen = VOICE + it.id },
                        onType = { screen = CHAT + it.id },
                        onOpenSettings = { screen = SETTINGS },
                    )
                    screen == SETTINGS -> SettingsScreen(settings, onBack = { screen = HOME })
                    screen.startsWith(CHAT) -> {
                        val scenario = scenarios.first { it.id == screen.removePrefix(CHAT) }
                        val vm = viewModel(key = screen) { ChatViewModel(scenario, settings) }
                        ChatScreen(scenario, vm, onBack = { screen = HOME })
                    }
                    screen.startsWith(VOICE) -> {
                        val scenario = scenarios.first { it.id == screen.removePrefix(VOICE) }
                        val vm = viewModel(key = screen) { VoiceViewModel(scenario, settings, firebaseReady, audioManager) }
                        VoiceScreen(scenario, vm, onBack = { screen = HOME })
                    }
                }
            }
        }
    }
}
