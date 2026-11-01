package app.trapanista.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/** Situations scanned from the learner's book, kept as a JSON array in SharedPreferences. */
class ScenarioStore(context: Context) {
    private val prefs = context.getSharedPreferences("custom_scenarios", Context.MODE_PRIVATE)

    /** Newest first. */
    fun saved(): List<Scenario> {
        val stored = prefs.getString(KEY_SCENARIOS, null) ?: return emptyList()
        val arr = runCatching { JSONArray(stored) }.getOrNull() ?: return emptyList()
        return (0 until arr.length()).map { fromJson(arr.getJSONObject(it)) }
    }

    fun add(scenario: Scenario) = write(listOf(scenario) + saved())

    fun delete(id: String) = write(saved().filterNot { it.id == id })

    /** Finds a built-in or saved scenario. */
    fun find(id: String): Scenario? = (scenarios + saved()).firstOrNull { it.id == id }

    private fun write(list: List<Scenario>) {
        val arr = JSONArray().apply { list.forEach { put(toJson(it)) } }
        prefs.edit().putString(KEY_SCENARIOS, arr.toString()).apply()
    }

    private fun toJson(s: Scenario) = JSONObject()
        .put("id", s.id)
        .put("emoji", s.emoji)
        .put("title", s.title)
        .put("titleIt", s.titleIt)
        .put("description", s.description)
        .put("partnerRole", s.partnerRole)
        .put("learnerGoal", s.learnerGoal)
        .put("bookDetails", s.bookDetails)

    private fun fromJson(o: JSONObject) = Scenario(
        id = o.getString("id"),
        emoji = o.getString("emoji"),
        title = o.getString("title"),
        titleIt = o.getString("titleIt"),
        description = o.getString("description"),
        partnerRole = o.getString("partnerRole"),
        learnerGoal = o.getString("learnerGoal"),
        bookDetails = o.getString("bookDetails"),
    )

    private companion object {
        const val KEY_SCENARIOS = "scenarios"
    }
}
