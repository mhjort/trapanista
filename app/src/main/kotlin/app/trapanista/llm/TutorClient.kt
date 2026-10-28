package app.trapanista.llm

import app.trapanista.data.ClaudeModel
import app.trapanista.data.Level
import app.trapanista.data.Scenario
import com.anthropic.models.beta.messages.BetaMessageParam
import com.anthropic.models.beta.messages.BetaOutputConfig
import org.json.JSONArray
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

/**
 * Runs one text role-play conversation against the Claude API with the user's own key.
 *
 * The history is append-only: every assistant response is replayed exactly as
 * returned (via [com.anthropic.models.beta.messages.BetaMessage.toParam]) so
 * thinking blocks stay valid across turns.
 */
class TutorClient(
    apiKey: String,
    model: ClaudeModel,
    private val level: Level,
    private val scenario: Scenario,
) : AutoCloseable {

    private val claude = Claude(apiKey, model)
    private val history = mutableListOf<BetaMessageParam>()

    /** Blocking; call from a background dispatcher. */
    fun start(): TutorTurn = send(START_MESSAGE)

    /** Blocking; call from a background dispatcher. */
    fun reply(learnerText: String): TutorTurn = send(learnerText)

    private fun send(userText: String): TutorTurn {
        val userMessage = BetaMessageParam.ofUser(userText)
        // Short conversational turns don't need deep reasoning.
        val (response, json) = claude.call(
            systemPrompt(scenario, level), history + userMessage, TURN_SCHEMA, BetaOutputConfig.Effort.LOW,
        )
        val turn = try {
            parseTurn(json)
        } catch (e: Exception) {
            throw ClaudeException("Couldn't read the tutor's response.", e)
        }

        // Only commit the turn once it parsed, so a failed send can be retried cleanly.
        history += userMessage
        history += response.toParam()
        return turn
    }

    override fun close() = claude.close()

    private companion object {
        const val START_MESSAGE = "(The learner has arrived. Start the scene with your opening line.)"

        val TURN_SCHEMA = Claude.schema(
            Claude.obj(
                mapOf(
                    "corrections" to Claude.array(
                        "Mistakes in the learner's latest message. Empty if none or if this is the opening line.",
                        Claude.CORRECTION_SCHEMA,
                    ),
                    "tip" to Claude.str("Optional short English tip about vocabulary, culture or politeness. Empty string if none."),
                    "reply" to Claude.str("Your next line in the scene, in Italian, in character."),
                    "translation" to Claude.str("English translation of reply."),
                    "suggestions" to Claude.array(
                        "2-3 things the learner could plausibly say next, at their level.",
                        Claude.PHRASE_SCHEMA,
                    ),
                    "scene_complete" to mapOf(
                        "type" to "boolean",
                        "description" to "True once the learner has achieved the goal and the scene has naturally ended.",
                    ),
                )
            )
        )

        fun parseTurn(json: String): TutorTurn {
            val o = JSONObject(json)
            return TutorTurn(
                corrections = parseCorrections(o.getJSONArray("corrections")),
                tip = o.getString("tip"),
                reply = o.getString("reply"),
                translation = o.getString("translation"),
                suggestions = parsePhrases(o.getJSONArray("suggestions")),
                sceneComplete = o.getBoolean("scene_complete"),
            )
        }

        fun systemPrompt(scenario: Scenario, level: Level): String = """
            You are an Italian conversation partner in a language-learning app. The learner practises
            Italian by role-playing everyday situations with you. You play the partner role below.

            ${scenario.promptBlock(level)}

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

internal fun parseCorrections(arr: JSONArray): List<Correction> = (0 until arr.length()).map { i ->
    val c = arr.getJSONObject(i)
    Correction(c.getString("original"), c.getString("corrected"), c.getString("note"))
}

internal fun parsePhrases(arr: JSONArray): List<Suggestion> = (0 until arr.length()).map { i ->
    val s = arr.getJSONObject(i)
    Suggestion(s.getString("italian"), s.getString("english"))
}

/** Scene description shared by the text tutor, the voice partner and the reviewer. */
internal fun Scenario.promptBlock(level: Level) = """
    Scene: $titleIt ($title).
    The partner role: $partnerRole.
    The learner's goal: $learnerGoal.
    Learner level: ${level.label} (${level.description}) on the CEFR scale.
""".trimIndent()
