package app.trapanista.ui

import android.content.ContentResolver
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.trapanista.data.Scenario
import app.trapanista.data.ScenarioStore
import app.trapanista.data.SettingsStore
import app.trapanista.llm.ClaudeException
import app.trapanista.llm.Exercise
import app.trapanista.llm.ExerciseRole
import app.trapanista.llm.ExerciseScanner
import app.trapanista.llm.prepareImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

sealed interface ScanUiState {
    data object Idle : ScanUiState
    data object Scanning : ScanUiState
    data class Choose(val exercises: List<Exercise>) : ScanUiState
    data class Failed(val message: String) : ScanUiState
}

class ScanViewModel(
    private val settings: SettingsStore,
    private val store: ScenarioStore,
    private val resolver: ContentResolver,
) : ViewModel() {

    private val _state = MutableStateFlow<ScanUiState>(ScanUiState.Idle)
    val state: StateFlow<ScanUiState> = _state

    /** [photo] is our own temporary camera file, removed once it has been read. */
    fun scan(uri: Uri, photo: File? = null) {
        val key = settings.apiKey
        if (key == null) {
            photo?.delete()
            _state.value = ScanUiState.Failed("Add your Anthropic API key in Settings first.")
            return
        }
        _state.value = ScanUiState.Scanning
        val model = settings.model
        val level = settings.level
        viewModelScope.launch {
            _state.value = try {
                val result = withContext(Dispatchers.IO) {
                    val image = try {
                        prepareImage(resolver, uri)
                    } finally {
                        photo?.delete()
                    }
                    ExerciseScanner(key, model).use { it.scan(image, level) }
                }
                if (result.exercises.isEmpty()) {
                    ScanUiState.Failed(result.problem)
                } else {
                    ScanUiState.Choose(result.exercises)
                }
            } catch (e: ClaudeException) {
                ScanUiState.Failed(e.message ?: "Couldn't read the exercise.")
            }
        }
    }

    /** Saves the exercise with the learner in [role] and returns the new situation. */
    fun choose(exercise: Exercise, role: ExerciseRole): Scenario {
        val scenario = exercise.toScenario(role)
        store.add(scenario)
        reset()
        return scenario
    }

    fun reset() {
        _state.value = ScanUiState.Idle
    }
}
