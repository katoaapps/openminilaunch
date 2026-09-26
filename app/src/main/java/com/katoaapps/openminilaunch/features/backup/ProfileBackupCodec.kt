package com.katoaapps.openminilaunch.features.backup

import com.katoaapps.openminilaunch.features.profile.ProfileCard
import com.katoaapps.openminilaunch.features.profile.ProfileLink
import com.katoaapps.openminilaunch.features.profile.ProfileLinkType
import org.json.JSONArray
import org.json.JSONObject

/** Readable portable-backup representation of encrypted local vCard data. */
internal object ProfileBackupCodec {
    private const val MAX_LINKS = 500
    private const val MAX_TEXT_LENGTH = 8_000

    fun encode(card: ProfileCard, links: List<ProfileLink>) = JSONObject().apply {
        put("fullName", card.fullName)
        put("organization", card.organization)
        put("jobTitle", card.jobTitle)
        put("note", card.note)
        put("selectedLinkIds", JSONArray(card.selectedLinkIds))
        put("showValuesOnCard", card.showValuesOnCard)
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
    }

    fun decode(json: JSONObject): Pair<ProfileCard, List<ProfileLink>> {
        val source = json.optJSONArray("links") ?: JSONArray()
        val seenIds = mutableSetOf<String>()
        val links = buildList {
            repeat(minOf(source.length(), MAX_LINKS)) { index ->
                val item = source.optJSONObject(index) ?: return@repeat
                val id = item.optString("id").trim().take(128)
                val type = runCatching { ProfileLinkType.valueOf(item.optString("type")) }.getOrNull()
                val value = item.optString("value").take(MAX_TEXT_LENGTH)
                if (id.isNotBlank() && type != null && value.isNotBlank() && seenIds.add(id)) {
                    add(ProfileLink(id, type, item.optString("label").take(120), value))
                }
            }
        }
        val validIds = links.mapTo(hashSetOf(), ProfileLink::id)
        val selectedSource = json.optJSONArray("selectedLinkIds") ?: JSONArray()
        val selected = buildList {
            repeat(minOf(selectedSource.length(), MAX_LINKS)) { index ->
                selectedSource.optString(index).trim().take(128)
                    .takeIf { it in validIds && it !in this }
                    ?.let(::add)
            }
        }
        return ProfileCard(
            fullName = json.optString("fullName").take(300),
            organization = json.optString("organization").take(300),
            jobTitle = json.optString("jobTitle").take(300),
            note = json.optString("note").take(MAX_TEXT_LENGTH),
            selectedLinkIds = selected,
            showValuesOnCard = json.optBoolean("showValuesOnCard", false),
            hasPortrait = false,
        ) to links
    }
}
