package com.katoaapps.openminilaunch.features.profile

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.AtomicFile
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.UUID

class ProfileRepository(context: Context) {
    private val directory = File(context.applicationContext.filesDir, "profile")
    private val profileFile = File(directory, "profile.enc")
    private val portraitFile = File(directory, "portrait.enc")
    private val encryption = ProfileEncryption()

    var state by mutableStateOf<ProfileState>(ProfileState.Empty)
        private set
    var portraitRevision by mutableIntStateOf(0)
        private set

    init {
        reload()
    }

    fun reload() {
        state = if (!profileFile.isFile) {
            ProfileState.Empty
        } else {
            val decrypted = runCatching { encryption.decrypt(profileFile.readBytes()) }
            decrypted.fold(
                onSuccess = { bytes ->
                    runCatching {
                        val (card, links) = ProfileJsonCodec.decode(bytes)
                        ProfileState.Ready(card.copy(hasPortrait = portraitFile.isFile), links)
                    }.getOrElse { ProfileState.Invalid(it.message) }
                },
                onFailure = { ProfileState.Unreadable(it.message) },
            )
        }
    }

    fun saveIdentity(fullName: String, organization: String, jobTitle: String, note: String) {
        val (card, links) = editableData()
        persist(
            card.copy(
                fullName = fullName,
                organization = organization,
                jobTitle = jobTitle,
                note = note,
            ),
            links,
        )
    }

    fun upsertLink(id: String?, type: ProfileLinkType, label: String, value: String): String {
        val (card, links) = editableData()
        val linkId = id?.takeIf(String::isNotBlank) ?: UUID.randomUUID().toString()
        val updated = ProfileLink(linkId, type, label, value)
        val newLinks = links.toMutableList().apply {
            val index = indexOfFirst { it.id == linkId }
            if (index >= 0) set(index, updated) else add(updated)
        }
        persist(card, newLinks)
        return linkId
    }

    fun deleteLink(id: String) {
        val (card, links) = editableData()
        persist(
            card.copy(selectedLinkIds = card.selectedLinkIds - id),
            links.filterNot { it.id == id },
        )
    }

    fun setLinkSelected(id: String, selected: Boolean) {
        val (card, links) = editableData()
        if (links.none { it.id == id }) return
        val order = if (selected) (card.selectedLinkIds + id).distinct() else card.selectedLinkIds - id
        persist(card.copy(selectedLinkIds = order), links)
    }

    fun setSelectedLinkOrder(ids: List<String>) {
        val (card, links) = editableData()
        val validIds = links.mapTo(hashSetOf(), ProfileLink::id)
        val order = ids.filter { it in validIds }.distinct()
        persist(card.copy(selectedLinkIds = order), links)
    }

    fun setShowValuesOnCard(show: Boolean) {
        val (card, links) = editableData()
        persist(card.copy(showValuesOnCard = show), links)
    }

    fun savePortrait(bitmap: Bitmap) {
        val bytes = ByteArrayOutputStream().use { output ->
            check(bitmap.compress(Bitmap.CompressFormat.JPEG, 92, output))
            output.toByteArray()
        }
        writeEncrypted(portraitFile, bytes)
        portraitRevision++
        val ready = state as? ProfileState.Ready
        if (ready != null) state = ready.copy(card = ready.card.copy(hasPortrait = true))
    }

    fun loadPortrait(): Bitmap? {
        if (!portraitFile.isFile) return null
        return runCatching {
            val bytes = encryption.decrypt(portraitFile.readBytes())
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        }.getOrNull()
    }

    fun removePortrait() {
        portraitFile.delete()
        portraitRevision++
        val ready = state as? ProfileState.Ready
        if (ready != null) state = ready.copy(card = ready.card.copy(hasPortrait = false))
    }

    fun replaceFromBackup(card: ProfileCard?, links: List<ProfileLink>) {
        removePortrait()
        if (card == null) {
            profileFile.delete()
            state = ProfileState.Empty
        } else {
            persist(card.copy(hasPortrait = false), links)
        }
    }

    fun reset() {
        profileFile.delete()
        portraitFile.delete()
        runCatching(encryption::resetKey)
        portraitRevision++
        state = ProfileState.Empty
    }

    private fun editableData(): Pair<ProfileCard, List<ProfileLink>> = when (val current = state) {
        ProfileState.Empty -> ProfileCard() to emptyList()
        is ProfileState.Ready -> current.card to current.links
        is ProfileState.Invalid -> error("Invalid Profile must be reset before it can be changed")
        is ProfileState.Unreadable -> error("Profile must be reset before it can be changed")
    }

    private fun persist(card: ProfileCard, links: List<ProfileLink>) {
        runCatching {
            writeEncrypted(profileFile, ProfileJsonCodec.encode(card, links))
        }.onSuccess {
            state = ProfileState.Ready(card.copy(hasPortrait = portraitFile.isFile), links)
        }.onFailure {
            state = ProfileState.Unreadable(it.message)
        }
    }

    private fun writeEncrypted(destination: File, plainText: ByteArray) {
        directory.mkdirs()
        val atomicFile = AtomicFile(destination)
        val output = atomicFile.startWrite()
        try {
            output.write(encryption.encrypt(plainText))
            atomicFile.finishWrite(output)
        } catch (failure: Throwable) {
            atomicFile.failWrite(output)
            throw failure
        }
    }
}
