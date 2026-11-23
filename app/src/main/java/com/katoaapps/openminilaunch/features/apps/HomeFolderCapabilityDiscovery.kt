package com.katoaapps.openminilaunch.features.apps

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.MediaStore

/** Finds strong exported capabilities that can refine broad Android app categories. */
internal class HomeFolderCapabilityDiscovery(
    private val packageManager: PackageManager,
) {
    fun purposesByPackage(): Map<String, Set<HomeFolderPurpose>> {
        val purposesByPackage = mutableMapOf<String, MutableSet<HomeFolderPurpose>>()

        fun collect(purpose: HomeFolderPurpose, intents: List<Intent>) {
            intents.asSequence()
                .flatMap(::packagesHandling)
                .forEach { packageName ->
                    purposesByPackage.getOrPut(packageName, ::mutableSetOf).add(purpose)
                }
        }

        collect(
            HomeFolderPurpose.CONNECT,
            listOf(
                Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:+15551234567")),
                Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:mink@example.com")),
                Intent(Intent.ACTION_DIAL, Uri.parse("tel:+15551234567")),
            ),
        )
        collect(
            HomeFolderPurpose.COMPOSE,
            listOf(
                editableContentIntent("image/*"),
                editableContentIntent("text/plain"),
                editableContentIntent("application/pdf"),
                editableContentIntent("application/msword"),
                editableContentIntent(
                    "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                ),
                Intent("android.intent.action.CREATE_NOTE"),
                Intent("com.google.android.gms.actions.CREATE_NOTE"),
                Intent(MediaStore.ACTION_IMAGE_CAPTURE),
            ),
        )
        collect(HomeFolderPurpose.BROWSE, listOf(Intent(Intent.ACTION_WEB_SEARCH)))
        collect(
            HomeFolderPurpose.MEDIA,
            listOf(
                viewContentIntent("audio/mpeg"),
                viewContentIntent("video/mp4"),
            ),
        )
        return purposesByPackage.mapValues { (_, purposes) -> purposes.toSet() }
    }

    private fun packagesHandling(intent: Intent): Sequence<String> =
        runCatching {
            packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)
        }.getOrDefault(emptyList())
            .asSequence()
            .mapNotNull { result -> result.activityInfo?.packageName }
            .filter(String::isNotBlank)
            .distinct()

    private fun editableContentIntent(mimeType: String): Intent =
        Intent(Intent.ACTION_EDIT).setDataAndType(SAMPLE_CONTENT_URI, mimeType)

    private fun viewContentIntent(mimeType: String): Intent =
        Intent(Intent.ACTION_VIEW).setDataAndType(SAMPLE_CONTENT_URI, mimeType)

    private companion object {
        val SAMPLE_CONTENT_URI: Uri = Uri.parse(
            "content://com.katoaapps.openminilaunch/home-folder-classification",
        )
    }
}
