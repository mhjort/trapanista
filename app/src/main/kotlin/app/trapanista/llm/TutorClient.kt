package app.trapanista.llm

import app.trapanista.data.Level
import app.trapanista.data.Scenario
import app.trapanista.data.TutorModel
import com.anthropic.client.AnthropicClient
import com.anthropic.client.okhttp.AnthropicOkHttpClient
import com.anthropic.core.JsonValue
import com.anthropic.errors.AnthropicIoException
import com.anthropic.errors.AnthropicServiceException
import com.anthropic.errors.PermissionDeniedException
import com.anthropic.errors.RateLimitException
import com.anthropic.errors.UnauthorizedException
import com.anthropic.models.beta.messages.BetaJsonOutputFormat
import com.anthropic.models.beta.messages.BetaMessageParam
import com.anthropic.models.beta.messages.BetaOutputConfig
import com.anthropic.models.beta.messages.BetaStopReason
import com.anthropic.models.beta.messages.MessageCreateParams
import org.json.JSONObject

data class Correction(val original: String, val corrected: String, val note: String)

data class Suggestion(val italian: String, val english: String)

/** One structured tutor turn, parsed from the model's JSON output. */
data class TutorTurn(
    val corrections: List<Correction>,
    val tip: String,
    val reply: String,
    val translation: String,
    val suggestions: List<Suggestion>,
    val sceneComplete: Boolean,
)

class TutorException(message: String, cause: Throwable? = null) : Exception(message, cause)

/**
 * Runs one role-play conversation against the Claude API with the user's own key.
 *
 * The history is append-only: every assistant response is replayed exactly as
 * returned (via [com.anthropic.models.beta.messages.BetaMessage.toParam]) so
 * thinking blocks stay valid across turns.
 */
class TutorClient(
    apiKey: String,
    private val model: TutorModel,
    private val level: Level,
    private val scenario: Scenario,
) : AutoCloseable {

    private val client: AnthropicClient = AnthropicOkHttpClient.builder()
        .apiKey(apiKey)
        .build()

    private val history = mutableListOf<BetaMessageParam>()

    /** Blocking; call from a background dispatcher. */
    fun start(): TutorTurn = send(START_MESSAGE)

    /** Blocking; call from a background dispatcher. */
    fun reply(learnerText: String): TutorTurn = send(learnerText)

    private fun send(userText: String): TutorTurn {
        val userMessage = BetaMessageParam.ofUser(userText)

        val builder = MessageCreateParams.builder()
            .model(model.id)
            .maxTokens(8000L)
            .system(systemPrompt(scenario, level))
            .outputConfig(
                BetaOutputConfig.builder()
                    // Short conversational turns don't need deep reasoning.
                    .effort(BetaOutputConfig.Effort.LOW)
                    .format(BetaJsonOutputFormat.builder().schema(TURN_SCHEMA).build())
                    .build()
            )
            .messages(history + userMessage)
        if (model.supportsFallbacks) {
            // If a safety classifier declines, let the API retry on a fallback model.
            builder.addBeta("server-side-fallback-2026-07-01").fallbacksDefault()
        }

        val response = try {
            client.beta().messages().create(builder.build())
        } catch (e: UnauthorizedException) {
            throw TutorException("Your API key was rejected. Check it in Settings.", e)
        } catch (e: PermissionDeniedException) {
            throw TutorException("Your API key doesn't have access to ${model.label}.", e)
        } catch (e: RateLimitException) {
            throw TutorException("Rate limited by the API. Wait a moment and try again.", e)
        } catch (e: AnthropicServiceException) {
            throw TutorException("API error (${e.statusCode()}): ${e.message}", e)
        } catch (e: AnthropicIoException) {
            throw TutorException("Network error. Check your connection and try again.", e)
        }

        when (response.stopReason().orElse(null)) {
            BetaStopReason.REFUSAL -> throw TutorException("The model declined to continue this conversation.")
            BetaStopReason.MAX_TOKENS -> throw TutorException("The response was cut off. Please try again.")
            else -> Unit
        }

        val json = response.content()
            .mapNotNull { block -> block.text().map { it.text() }.orElse(null) }
            .joinToString("")
        val turn = try {
            parseTurn(json)
        } catch (e: Exception) {
            throw TutorException("Couldn't read the tutor's response.", e)
        }

        // Only commit the turn once it parsed, so a failed send can be retried cleanly.
        history += userMessage
        history += response.toParam()
        return turn
    }

    override fun close() = client.close()

    private companion object {
        const val START_MESSAGE = "(The learner has arrived. Start the scene with your opening line.)"

        val TURN_SCHEMA: BetaJsonOutputFormat.Schema = run {
            fun str(description: String) = mapOf("type" to "string", "description" to description)
            fun obj(properties: Map<String, Any>) = mapOf(
                "type" to "object",
                "properties" to properties,
                "required" to properties.keys.toList(),
                "additionalProperties" to false,
            )
            val schema = obj(
                mapOf(
                    "corrections" to mapOf(
                        "type" to "array",
                        "description" to "Mistakes in the learner's latest message. Empty if none or if this is the opening line.",
                        "items" to obj(
                            mapOf(
                                "original" to str("The learner's incorrect fragment, verbatim."),
                                "corrected" to str("The corrected Italian fragment."),
                                "note" to str("A one-sentence explanation in English."),
                            )
                        ),
                    ),
                    "tip" to str("Optional short English tip about vocabulary, culture or politeness. Empty string if none."),
                    "reply" to str("Your next line in the scene, in Italian, in character."),
                    "translation" to str("English translation of reply."),
                    "suggestions" to mapOf(
                        "type" to "array",
                        "description" to "2-3 things the learner could plausibly say next, at their level.",
                        "items" to obj(
                            mapOf(
                                "italian" to str("Suggested reply in Italian."),
                                "english" to str("English translation."),
                            )
                        ),
                    ),
                    "scene_complete" to mapOf(
                        "type" to "boolean",
                        "description" to "True once the learner has achieved the goal and the scene has naturally ended.",
                    ),
                )
            )
            BetaJsonOutputFormat.Schema.builder()
                .apply { schema.forEach { (k, v) -> putAdditionalProperty(k, JsonValue.from(v)) } }
                .build()
        }

        fun parseTurn(json: String): TutorTurn {
            val o = JSONObject(json)
            val corrections = o.getJSONArray("corrections").let { arr ->
                (0 until arr.length()).map { i ->
                    val c = arr.getJSONObject(i)
                    Correction(c.getString("original"), c.getString("corrected"), c.getString("note"))
                }
            }
            val suggestions = o.getJSONArray("suggestions").let { arr ->
                (0 until arr.length()).map { i ->
                    val s = arr.getJSONObject(i)
                    Suggestion(s.getString("italian"), s.getString("english"))
                }
            }
            return TutorTurn(
                corrections = corrections,
                tip = o.getString("tip"),
                reply = o.getString("reply"),
                translation = o.getString("translation"),
                suggestions = suggestions,
                sceneComplete = o.getBoolean("scene_complete"),
            )
        }

        fun systemPrompt(scenario: Scenario, level: Level): String = """
            You are an Italian conversation partner in a language-learning app. The learner practises
            Italian by role-playing everyday situations with you.

            Scene: ${scenario.titleIt} (${scenario.title}).
            You play ${scenario.partnerRole}.
            The learner's goal: ${scenario.learnerGoal}.
            Learner level: ${level.label} (${level.description}) on the CEFR scale.

            How to play the scene:
            - Stay in character and speak only Italian in "reply". Keep each line short (1-3 sentences),
              natural, and pitched at ${level.label}: simple vocabulary and present tense for A1/A2,
              more variety for B1/B2. Use the polite "Lei" form unless the situation is informal.
            - Move the scene forward so the learner has to work towards their goal. Ask one thing at a time.
            - If the learner writes in English or gets stuck, stay in character, respond simply, and make
              the suggestions show how to say it in Italian.
            - When the goal is achieved, close the scene naturally in character and set scene_complete to true.

            How to give feedback:
            - In "corrections", list real mistakes in the learner's latest message (grammar, word choice,
              spelling that changes meaning, wrong register). Ignore missing accents and capitalisation
              unless they change meaning. Don't correct things that are just a different valid phrasing.
            - Be encouraging; the corrections are shown beside the learner's message, not spoken in the scene.
            - "suggestions" should be things the learner could actually say next, at their level,
              that move towards their goal.
        """.trimIndent()
    }
}
