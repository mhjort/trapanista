package app.trapanista.llm

import app.trapanista.data.ClaudeModel
import app.trapanista.data.Level
import app.trapanista.data.Scenario
import com.anthropic.models.beta.messages.BetaMessageParam
import com.anthropic.models.beta.messages.BetaOutputConfig
import org.json.JSONObject

enum class Speaker { LEARNER, PARTNER }

data class TranscriptLine(val speaker: Speaker, val text: String)

/** One learner line from the voice conversation, with Claude's corrections for it. */
data class ReviewedLine(val text: String, val corrections: List<Correction>)

data class Review(
    val summary: String,
    val wentWell: List<String>,
    val lines: List<ReviewedLine>,
    val usefulPhrases: List<Suggestion>,
)

/** Asks Claude to review the transcript of a finished voice conversation. */
class ReviewClient(apiKey: String, model: ClaudeModel) : AutoCloseable {

    private val claude = Claude(apiKey, model)

    /** Blocking; call from a background dispatcher. */
    fun review(scenario: Scenario, level: Level, transcript: List<TranscriptLine>): Review {
        val learnerLines = transcript.filter { it.speaker == Speaker.LEARNER }
        val numbered = buildString {
            var n = 0
            transcript.forEach { line ->
                when (line.speaker) {
                    Speaker.PARTNER -> appendLine("Partner: ${line.text}")
                    Speaker.LEARNER -> appendLine("Learner [${n++}]: ${line.text}")
                }
            }
        }
        val (_, json) = claude.call(
            systemPrompt(scenario, level),
            listOf(BetaMessageParam.ofUser("<transcript>\n$numbered</transcript>")),
            SCHEMA,
            BetaOutputConfig.Effort.MEDIUM,
        )
        return try {
            val o = JSONObject(json)
            val byIndex = o.getJSONArray("line_feedback").let { arr ->
                (0 until arr.length()).associate { i ->
                    val f = arr.getJSONObject(i)
                    f.getInt("line") to parseCorrections(f.getJSONArray("corrections"))
                }
            }
            Review(
                summary = o.getString("summary"),
                wentWell = o.getJSONArray("went_well").let { a -> (0 until a.length()).map(a::getString) },
                lines = learnerLines.mapIndexed { i, line -> ReviewedLine(line.text, byIndex[i].orEmpty()) },
                usefulPhrases = parsePhrases(o.getJSONArray("useful_phrases")),
            )
        } catch (e: Exception) {
            throw ClaudeException("Couldn't read the review.", e)
        }
    }

    override fun close() = claude.close()

    private companion object {
        val SCHEMA = Claude.schema(
            Claude.obj(
                mapOf(
                    "summary" to Claude.str("2-3 encouraging sentences in English on how the conversation went and the main thing to work on."),
                    "went_well" to mapOf(
                        "type" to "array",
                        "description" to "1-3 specific things the learner did well, in English.",
                        "items" to mapOf("type" to "string"),
                    ),
                    "line_feedback" to Claude.array(
                        "Corrections for learner lines that had mistakes. Omit lines without mistakes.",
                        Claude.obj(
                            mapOf(
                                "line" to mapOf("type" to "integer", "description" to "The learner line number from the transcript."),
                                "corrections" to Claude.array("Mistakes in that line.", Claude.CORRECTION_SCHEMA),
                            )
                        ),
                    ),
                    "useful_phrases" to Claude.array(
                        "3-5 natural phrases for this situation the learner didn't use but would benefit from, at their level.",
                        Claude.PHRASE_SCHEMA,
                    ),
                )
            )
        )

        fun systemPrompt(scenario: Scenario, level: Level) = """
            You are an Italian teacher reviewing a spoken role-play your student just finished with a
            conversation partner.

            ${scenario.promptBlock(level)}

            The transcript comes from automatic speech recognition, so ignore spelling, punctuation,
            capitalisation and accents, and give the benefit of the doubt where a word looks like a
            likely mis-transcription. Focus on grammar, word choice, agreement, verb forms and register
            (Lei vs tu). Don't correct things that are just a different valid phrasing. Only review the
            learner's lines. Be specific and encouraging, and pitch explanations at ${level.label}.
        """.trimIndent()
    }
}
