package app.trapanista.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.trapanista.data.Scenario
import app.trapanista.data.SettingsStore
import app.trapanista.llm.Correction
import app.trapanista.llm.Suggestion
import app.trapanista.llm.TutorClient
import app.trapanista.llm.TutorException
import app.trapanista.llm.TutorTurn
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed interface ChatItem {
    data class Partner(val italian: String, val english: String, val tip: String) : ChatItem
    data class Learner(val text: String, val corrections: List<Correction> = emptyList()) : ChatItem
}

data class ChatUiState(
    val items: List<ChatItem> = emptyList(),
    val suggestions: List<Suggestion> = emptyList(),
    val loading: Boolean = false,
    val error: String? = null,
    val sceneComplete: Boolean = false,
)

class ChatViewModel(
    private val scenario: Scenario,
    private val settings: SettingsStore,
) : ViewModel() {

    private val _state = MutableStateFlow(ChatUiState())
    val state: StateFlow<ChatUiState> = _state

    private var tutor: TutorClient? = null
    private var retry: (() -> Unit)? = null

    init {
        restart()
    }

    fun restart() {
        tutor?.close()
        val key = settings.apiKey
        if (key == null) {
            _state.value = ChatUiState(error = "Add your Anthropic API key in Settings first.")
            return
        }
        tutor = TutorClient(key, settings.model, settings.level, scenario)
        _state.value = ChatUiState()
        run { it.start() }
    }

    fun send(text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty() || _state.value.loading) return
        _state.update { it.copy(items = it.items + ChatItem.Learner(trimmed), suggestions = emptyList()) }
        run { it.reply(trimmed) }
    }

    fun retry() {
        retry?.invoke()
    }

    private fun run(call: (TutorClient) -> TutorTurn) {
        val client = tutor ?: return
        retry = { run(call) }
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            try {
                val turn = withContext(Dispatchers.IO) { call(client) }
                retry = null
                _state.update { it.withTurn(turn) }
            } catch (e: TutorException) {
                _state.update { it.copy(loading = false, error = e.message) }
            }
        }
    }

    private fun ChatUiState.withTurn(turn: TutorTurn): ChatUiState {
        // Attach corrections to the learner message they refer to.
        val updated = items.toMutableList()
        val lastIndex = updated.indexOfLast { it is ChatItem.Learner }
        if (lastIndex >= 0 && turn.corrections.isNotEmpty()) {
            updated[lastIndex] = (updated[lastIndex] as ChatItem.Learner).copy(corrections = turn.corrections)
        }
        updated += ChatItem.Partner(turn.reply, turn.translation, turn.tip)
        return copy(
            items = updated,
            suggestions = if (turn.sceneComplete) emptyList() else turn.suggestions,
            loading = false,
            error = null,
            sceneComplete = turn.sceneComplete,
        )
    }

    override fun onCleared() {
        tutor?.close()
    }
}
