package app.trapanista.llm

import android.Manifest
import androidx.annotation.RequiresPermission
import app.trapanista.data.GeminiModel
import app.trapanista.data.Level
import app.trapanista.data.Scenario
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.AudioTranscriptionConfig
import com.google.firebase.ai.type.FunctionCallPart
import com.google.firebase.ai.type.FunctionDeclaration
import com.google.firebase.ai.type.FunctionResponsePart
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.LiveSession
import com.google.firebase.ai.type.PublicPreviewAPI
import com.google.firebase.ai.type.ResponseModality
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.Tool
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.liveAudioConversationConfig
import com.google.firebase.ai.type.liveGenerationConfig
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/**
 * A spoken role-play with Gemini Live through Firebase AI Logic. The SDK owns the
 * microphone and speaker; we only receive transcripts and the end-of-scene signal.
 * Callbacks may arrive on background threads.
 */
@OptIn(PublicPreviewAPI::class)
class VoicePartner(
    private val model: GeminiModel,
    private val scenario: Scenario,
    private val level: Level,
    private val onTranscript: (Speaker, String) -> Unit,
    private val onSceneComplete: () -> Unit,
    private val onDisconnected: () -> Unit,
) {
    private var session: LiveSession? = null

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    suspend fun start() {
        val liveModel = Firebase.ai(backend = GenerativeBackend.googleAI()).liveModel(
            modelName = model.id,
            generationConfig = liveGenerationConfig {
                responseModality = ResponseModality.AUDIO
                inputAudioTranscription = AudioTranscriptionConfig()
                outputAudioTranscription = AudioTranscriptionConfig()
            },
            tools = listOf(Tool.functionDeclarations(listOf(END_SCENE))),
            systemInstruction = content { text(systemPrompt(scenario, level)) },
        )
        val live = liveModel.connect()
        session = live
        live.startAudioConversation(
            liveAudioConversationConfig {
                functionCallHandler = ::handleFunctionCall
                transcriptHandler = { input, output ->
                    input?.text?.takeIf { it.isNotEmpty() }?.let { onTranscript(Speaker.LEARNER, it) }
                    output?.text?.takeIf { it.isNotEmpty() }?.let { onTranscript(Speaker.PARTNER, it) }
                }
                goAwayHandler = { onDisconnected() }
                enableInterruptions = true
            }
        )
        live.send(START_MESSAGE)
    }

    suspend fun stop() {
        session?.let {
            it.stopAudioConversation()
            it.close()
        }
        session = null
    }

    private fun handleFunctionCall(call: FunctionCallPart): FunctionResponsePart {
        val response = when (call.name) {
            END_SCENE_NAME -> {
                onSceneComplete()
                JsonObject(mapOf("success" to JsonPrimitive(true)))
            }
            else -> JsonObject(mapOf("error" to JsonPrimitive("Unknown function: ${call.name}")))
        }
        return FunctionResponsePart(call.name, response, call.id)
    }

    private companion object {
        const val START_MESSAGE = "(The learner has arrived. Start the scene with your opening line.)"
        const val END_SCENE_NAME = "end_scene"

        val END_SCENE = FunctionDeclaration(
            name = END_SCENE_NAME,
            description = "Call after you have said goodbye, once the learner has achieved their goal and the scene is over.",
            parameters = mapOf("reason" to Schema.string("One short English sentence on how the scene ended.")),
        )

        val FunctionDeclaration.name get() = "end_scene"

        fun systemPrompt(scenario: Scenario, level: Level) = """
            You are an Italian conversation partner in a spoken language-learning app. The learner
            practises Italian by role-playing everyday situations with you out loud. You play the
            partner role below.

            ${scenario.promptBlock(level)}

            - Speak only Italian, in character, clearly and a little slower than normal, with a natural
              Italian accent. Keep each turn short (1-2 sentences) so the learner gets to talk.
            - Pitch your Italian at ${level.label}: simple words and present tense for A1/A2, more
              variety for B1/B2. Use the polite "Lei" form unless the situation is informal.
            - Move the scene forward so the learner has to work towards their goal. Ask one thing at a time.
            - Don't correct the learner or teach during the scene; a teacher reviews the transcript
              afterwards. If they're stuck or speak English, rephrase more simply in Italian or offer
              two options to choose from.
            - When the goal is achieved, say goodbye in character and then call end_scene.
        """.trimIndent()
    }
}
