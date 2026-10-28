package app.trapanista.ui

import android.Manifest
import android.util.Log
import androidx.annotation.RequiresPermission
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.trapanista.data.Scenario
import app.trapanista.data.SettingsStore
import app.trapanista.llm.ClaudeException
import app.trapanista.llm.Review
import app.trapanista.llm.ReviewClient
import app.trapanista.llm.Speaker
import app.trapanista.llm.TranscriptLine
import app.trapanista.llm.VoicePartner
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class VoicePhase { IDLE, CONNECTING, LIVE, REVIEWING, REVIEWED }

data class VoiceUiState(
    val phase: VoicePhase = VoicePhase.IDLE,
    val transcript: List<TranscriptLine> = emptyList(),
    val sceneComplete: Boolean = false,
    val review: Review? = null,
    val error: String? = null,
)

class VoiceViewModel(
    private val scenario: Scenario,
    private val settings: SettingsStore,
    private val firebaseReady: Boolean,
) : ViewModel() {

    private val _state = MutableStateFlow(VoiceUiState())
    val state: StateFlow<VoiceUiState> = _state

    private var partner: VoicePartner? = null

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    fun start() {
        if (!firebaseReady) {
            _state.update { it.copy(error = "Voice mode needs Firebase. Add app/google-services.json and rebuild (see README).") }
            return
        }
        val voice = VoicePartner(
            model = settings.geminiModel,
            scenario = scenario,
            level = settings.level,
            onTranscript = ::appendTranscript,
            onSceneComplete = { _state.update { it.copy(sceneComplete = true) } },
            onDisconnected = { viewModelScope.launch { finish() } },
        )
        partner = voice
        _state.value = VoiceUiState(phase = VoicePhase.CONNECTING)
        viewModelScope.launch {
            try {
                voice.start()
                _state.update { it.copy(phase = VoicePhase.LIVE) }
            } catch (e: Exception) {
                Log.w("VoiceViewModel", "Couldn't start Gemini Live", e)
                partner = null
                _state.update { it.copy(phase = VoicePhase.IDLE, error = "Couldn't start the conversation: ${e.message}") }
            }
        }
    }

    /** Ends the conversation and asks Claude to review the transcript. */
    fun finish() {
        val voice = partner ?: return
        partner = null
        viewModelScope.launch {
            runCatching { voice.stop() }
            review()
        }
    }

    fun review() {
        val transcript = _state.value.transcript
        if (transcript.none { it.speaker == Speaker.LEARNER }) {
            _state.update { it.copy(phase = VoicePhase.IDLE) }
            return
        }
        val key = settings.apiKey
        if (key == null) {
            _state.update {
                it.copy(phase = VoicePhase.REVIEWED, error = "Add your Anthropic API key in Settings to get feedback.")
            }
            return
        }
        _state.update { it.copy(phase = VoicePhase.REVIEWING, error = null) }
        viewModelScope.launch {
            try {
                val review = withContext(Dispatchers.IO) {
                    ReviewClient(key, settings.model).use { it.review(scenario, settings.level, transcript) }
                }
                _state.update { it.copy(phase = VoicePhase.REVIEWED, review = review) }
            } catch (e: ClaudeException) {
                _state.update { it.copy(phase = VoicePhase.REVIEWED, error = e.message) }
            }
        }
    }

    /** Transcripts arrive as fragments; merge consecutive fragments from the same speaker. */
    private fun appendTranscript(speaker: Speaker, fragment: String) {
        _state.update { s ->
            val last = s.transcript.lastOrNull()
            val transcript = if (last?.speaker == speaker) {
                s.transcript.dropLast(1) + last.copy(text = last.text + fragment)
            } else {
                s.transcript + TranscriptLine(speaker, fragment.trimStart())
            }
            s.copy(transcript = transcript)
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    override fun onCleared() {
        partner?.let { voice ->
            // viewModelScope is already cancelled here; release the mic and socket anyway.
            GlobalScope.launch(NonCancellable) { runCatching { voice.stop() } }
        }
    }
}
