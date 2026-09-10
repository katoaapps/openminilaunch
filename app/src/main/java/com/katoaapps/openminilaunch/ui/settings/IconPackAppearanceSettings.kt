package com.katoaapps.openminilaunch.ui.settings

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Palette
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.katoaapps.openminilaunch.R
import com.katoaapps.openminilaunch.data.LauncherStore
import com.katoaapps.openminilaunch.features.iconpacks.IconPackRepository
import com.katoaapps.openminilaunch.features.iconpacks.InstalledIconPack
import com.katoaapps.openminilaunch.model.IconSource
import com.katoaapps.openminilaunch.ui.components.SettingsRow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val ICON_SELECTION_ANIMATION_MILLIS = 280L
private const val ICON_APPLYING_FEEDBACK_MILLIS = 1000L

@Composable
internal fun IconPackAppearanceSettings(
    store: LauncherStore,
    onApplied: () -> Unit,
) {
    val context = LocalContext.current
    val repository = remember(context) { IconPackRepository.get(context) }
    val repositoryRevision by repository.revision.collectAsState()
    val packs by produceState<List<InstalledIconPack>?>(null, repositoryRevision) {
        value = withContext(Dispatchers.IO) { repository.installedPacks() }
    }
    var showPicker by remember { mutableStateOf(false) }
    var selectionInProgress by remember { mutableStateOf(false) }
    var showApplyingFeedback by remember { mutableStateOf(false) }
    var selectionFeedbackId by remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()
    val selectedPack = packs?.firstOrNull {
        it.packageName == store.iconAppearance.iconPackPackage
    }
    val selectionLabel = when (store.iconAppearance.source) {
        IconSource.MINK -> stringResource(R.string.mink_icons)
        IconSource.SYSTEM -> stringResource(R.string.system_icons)
        IconSource.ICON_PACK -> selectedPack?.label ?: stringResource(R.string.icon_pack_unavailable)
    }

    SettingsRow(
        title = stringResource(R.string.icon_style),
        subtitle = selectionLabel,
        icon = Icons.Default.Palette,
        onClick = { showPicker = true },
    )
    if (store.iconAppearance.source == IconSource.MINK) {
        MinkIconColorSetting(store)
    }

    if (showPicker) {
        fun selectIconStyle(updateSelection: () -> Unit) {
            if (selectionInProgress) return
            val feedbackId = selectionFeedbackId + 1
            selectionFeedbackId = feedbackId
            updateSelection()
            selectionInProgress = true
            scope.launch {
                delay(ICON_SELECTION_ANIMATION_MILLIS)
                if (!selectionInProgress || selectionFeedbackId != feedbackId) return@launch
                showApplyingFeedback = true
                delay(ICON_APPLYING_FEEDBACK_MILLIS)
                if (!selectionInProgress || selectionFeedbackId != feedbackId) return@launch
                showPicker = false
                selectionInProgress = false
                showApplyingFeedback = false
                onApplied()
            }
        }

        IconPackPickerDialog(
            selectedSource = store.iconAppearance.source,
            selectedPackage = store.iconAppearance.iconPackPackage,
            packs = packs,
            selectionEnabled = !selectionInProgress,
            showApplyingFeedback = showApplyingFeedback,
            applyingStyleLabel = selectionLabel,
            onMinkIcons = { selectIconStyle(store::useMinkIcons) },
            onSystemIcons = { selectIconStyle(store::useSystemIcons) },
            onIconPack = { packageName -> selectIconStyle { store.useIconPack(packageName) } },
            onDismiss = {
                selectionFeedbackId += 1
                showPicker = false
                selectionInProgress = false
                showApplyingFeedback = false
            },
        )
    }
}
