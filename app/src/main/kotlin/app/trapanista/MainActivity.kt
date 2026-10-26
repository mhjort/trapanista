package app.trapanista

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

private const val HOME = "home"
private const val SETTINGS = "settings"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val settings = SettingsStore(applicationContext)
        setContent {
            TrapanistaTheme {
                // HOME, SETTINGS, or a scenario id.
                var screen by rememberSaveable { mutableStateOf(HOME) }
                BackHandler(enabled = screen != HOME) { screen = HOME }
                when (screen) {
                    HOME -> HomeScreen(
                        hasApiKey = remember(screen) { settings.apiKey != null },
                        onOpenScenario = { screen = it.id },
                        onOpenSettings = { screen = SETTINGS },
                    )
                    SETTINGS -> SettingsScreen(settings, onBack = { screen = HOME })
                    else -> {
                        val scenario = scenarios.first { it.id == screen }
                        val vm = viewModel(key = scenario.id) { ChatViewModel(scenario, settings) }
                        ChatScreen(scenario, vm, onBack = { screen = HOME })
                    }
                }
            }
        }
    }
}
