package app.trapanista.llm

import app.trapanista.data.ClaudeModel
import com.anthropic.client.AnthropicClient
import com.anthropic.client.okhttp.AnthropicOkHttpClient
import com.anthropic.core.JsonValue
import com.anthropic.errors.AnthropicIoException
import com.anthropic.errors.AnthropicServiceException
import com.anthropic.errors.PermissionDeniedException
import com.anthropic.errors.RateLimitException
import com.anthropic.errors.UnauthorizedException
import com.anthropic.models.beta.messages.BetaJsonOutputFormat
import com.anthropic.models.beta.messages.BetaMessage
import com.anthropic.models.beta.messages.BetaMessageParam
import com.anthropic.models.beta.messages.BetaOutputConfig
import com.anthropic.models.beta.messages.BetaStopReason
import com.anthropic.models.beta.messages.MessageCreateParams

class ClaudeException(message: String, cause: Throwable? = null) : Exception(message, cause)

/** Thin wrapper around the Anthropic client for structured-JSON requests with the user's own key. */
internal class Claude(apiKey: String, private val model: ClaudeModel) : AutoCloseable {

    private val client: AnthropicClient = AnthropicOkHttpClient.builder()
        .apiKey(apiKey)
        .build()

    /** Blocking; call from a background dispatcher. Returns the full message and its JSON text. */
    fun call(
        system: String,
        messages: List<BetaMessageParam>,
        schema: BetaJsonOutputFormat.Schema,
        effort: BetaOutputConfig.Effort,
    ): Pair<BetaMessage, String> {
        val builder = MessageCreateParams.builder()
            .model(model.id)
            .maxTokens(16000L)
            .system(system)
            .outputConfig(
                BetaOutputConfig.builder()
                    .effort(effort)
                    .format(BetaJsonOutputFormat.builder().schema(schema).build())
                    .build()
            )
            .messages(messages)
        if (model.supportsFallbacks) {
            // If a safety classifier declines, let the API retry on a fallback model.
            builder.addBeta("server-side-fallback-2026-07-01").fallbacksDefault()
        }

        val response = try {
            client.beta().messages().create(builder.build())
        } catch (e: UnauthorizedException) {
            throw ClaudeException("Your Anthropic API key was rejected. Check it in Settings.", e)
        } catch (e: PermissionDeniedException) {
            throw ClaudeException("Your API key doesn't have access to ${model.label}.", e)
        } catch (e: RateLimitException) {
            throw ClaudeException("Rate limited by the API. Wait a moment and try again.", e)
        } catch (e: AnthropicServiceException) {
            throw ClaudeException("API error (${e.statusCode()}): ${e.message}", e)
        } catch (e: AnthropicIoException) {
            throw ClaudeException("Network error. Check your connection and try again.", e)
        }

        when (response.stopReason().orElse(null)) {
            BetaStopReason.REFUSAL -> throw ClaudeException("The model declined to respond.")
            BetaStopReason.MAX_TOKENS -> throw ClaudeException("The response was cut off. Please try again.")
            else -> Unit
        }

        val text = response.content()
            .mapNotNull { block -> block.text().map { it.text() }.orElse(null) }
            .joinToString("")
        return response to text
    }

    override fun close() = client.close()

    companion object {
        fun str(description: String) = mapOf("type" to "string", "description" to description)

        /** A strict JSON-schema object: every property required, no extras. */
        fun obj(properties: Map<String, Any>) = mapOf(
            "type" to "object",
            "properties" to properties,
            "required" to properties.keys.toList(),
            "additionalProperties" to false,
        )

        fun array(description: String, items: Map<String, Any>) =
            mapOf("type" to "array", "description" to description, "items" to items)

        fun schema(root: Map<String, Any>): BetaJsonOutputFormat.Schema =
            BetaJsonOutputFormat.Schema.builder()
                .apply { root.forEach { (k, v) -> putAdditionalProperty(k, JsonValue.from(v)) } }
                .build()

        val CORRECTION_SCHEMA = obj(
            mapOf(
                "original" to str("The learner's incorrect fragment, verbatim."),
                "corrected" to str("The corrected Italian fragment."),
                "note" to str("A one-sentence explanation in English."),
            )
        )

        val PHRASE_SCHEMA = obj(
            mapOf(
                "italian" to str("Phrase in Italian."),
                "english" to str("English translation."),
            )
        )
    }
}
