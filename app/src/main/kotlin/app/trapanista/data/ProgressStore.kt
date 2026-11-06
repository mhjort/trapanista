package app.trapanista.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

enum class Mode(val label: String) { VOICE("voice"), TEXT("text") }

/** One finished, scored run of an exercise. */
data class Attempt(val scenarioId: String, val mode: Mode, val stars: Int, val at: Long)

/** The learner's scored attempts, kept as a JSON array in SharedPreferences. */
class ProgressStore(context: Context) {
    private val prefs = context.getSharedPreferences("progress", Context.MODE_PRIVATE)

    /** Newest first. */
    fun attempts(): List<Attempt> {
        val stored = prefs.getString(KEY_ATTEMPTS, null) ?: return emptyList()
        val arr = runCatching { JSONArray(stored) }.getOrNull() ?: return emptyList()
        return (0 until arr.length()).mapNotNull { runCatching { fromJson(arr.getJSONObject(it)) }.getOrNull() }
    }

    fun record(scenarioId: String, mode: Mode, stars: Int) {
        val attempt = Attempt(scenarioId, mode, stars, System.currentTimeMillis())
        val arr = JSONArray().apply { (listOf(attempt) + attempts()).forEach { put(toJson(it)) } }
        prefs.edit().putString(KEY_ATTEMPTS, arr.toString()).apply()
    }

    private fun toJson(a: Attempt) = JSONObject()
        .put("scenarioId", a.scenarioId)
        .put("mode", a.mode.name)
        .put("stars", a.stars)
        .put("at", a.at)

    private fun fromJson(o: JSONObject) = Attempt(
        scenarioId = o.getString("scenarioId"),
        mode = Mode.valueOf(o.getString("mode")),
        stars = o.getInt("stars"),
        at = o.getLong("at"),
    )

    private companion object {
        const val KEY_ATTEMPTS = "attempts"
    }
}

fun List<Attempt>.bestById(): Map<String, Int> =
    groupBy { it.scenarioId }.mapValues { (_, list) -> list.maxOf { it.stars } }

/**
 * The exercise to suggest next: the first one never finished, otherwise the one with the lowest
 * best score below 5 stars. Null once every exercise has 5 stars.
 */
fun recommend(all: List<Scenario>, attempts: List<Attempt>): Scenario? {
    val best = attempts.bestById()
    all.firstOrNull { it.id !in best }?.let { return it }
    return all.filter { best.getValue(it.id) < 5 }.minByOrNull { best.getValue(it.id) }
}
