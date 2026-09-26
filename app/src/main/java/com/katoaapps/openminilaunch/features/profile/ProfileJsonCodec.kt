package com.katoaapps.openminilaunch.features.profile

import org.json.JSONArray
import org.json.JSONObject

internal object ProfileJsonCodec {
    private const val MAX_LINKS = 500
    private const val MAX_TEXT = 8_000

    fun encode(card: ProfileCard, links: List<ProfileLink>): ByteArray = JSONObject().apply {
        put("version", 1)
        put("card", JSONObject().apply {
            put("fullName", card.fullName)
            put("organization", card.organization)
            put("jobTitle", card.jobTitle)
            put("note", card.note)
            put("selectedLinkIds", JSONArray(card.selectedLinkIds))
            put("showValuesOnCard", card.showValuesOnCard)
        })
        put("links", JSONArray().apply {
            links.forEach { link ->
                put(JSONObject().apply {
                    put("id", link.id)
                    put("type", link.type.name)
                    put("label", link.label)
                    put("value", link.value)
                })
            }
        })
    }.toString().toByteArray(Charsets.UTF_8)

    fun decode(bytes: ByteArray): Pair<ProfileCard, List<ProfileLink>> {
        val root = JSONObject(bytes.toString(Charsets.UTF_8))
        require(root.optInt("version") == 1) { "Unsupported Profile data" }
        val cardJson = root.getJSONObject("card")
        val linksJson = root.optJSONArray("links") ?: JSONArray()
        val seenIds = mutableSetOf<String>()
        val links = buildList {
            repeat(minOf(linksJson.length(), MAX_LINKS)) { index ->
                val item = linksJson.optJSONObject(index) ?: return@repeat
                val id = item.optString("id").safeId(128)
                val value = item.optString("value").safeText(MAX_TEXT)
                val type = runCatching { ProfileLinkType.valueOf(item.optString("type")) }.getOrNull()
                if (id.isNotBlank() && value.isNotBlank() && type != null && seenIds.add(id)) {
                    add(
                        ProfileLink(
                            id = id,
                            type = type,
                            label = item.optString("label").safeText(120),
                            value = value,
                        ),
                    )
                }
            }
        }
        val linkIds = links.mapTo(hashSetOf(), ProfileLink::id)
        val selected = cardJson.optJSONArray("selectedLinkIds") ?: JSONArray()
        val selectedIds = buildList {
            repeat(minOf(selected.length(), MAX_LINKS)) { index ->
                selected.optString(index).safeId(128)
                    .takeIf { it in linkIds && it !in this }
                    ?.let(::add)
            }
        }
        return ProfileCard(
            fullName = cardJson.optString("fullName").safeText(300),
            organization = cardJson.optString("organization").safeText(300),
            jobTitle = cardJson.optString("jobTitle").safeText(300),
            note = cardJson.optString("note").safeText(MAX_TEXT),
            selectedLinkIds = selectedIds,
            showValuesOnCard = cardJson.optBoolean("showValuesOnCard", false),
        ) to links
    }

    private fun String.safeId(maxLength: Int): String = trim().take(maxLength)

    private fun String.safeText(maxLength: Int): String = take(maxLength)
}
