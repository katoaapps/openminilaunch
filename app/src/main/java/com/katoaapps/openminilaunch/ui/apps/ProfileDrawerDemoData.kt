package com.katoaapps.openminilaunch.ui.apps

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.katoaapps.openminilaunch.ui.theme.Dimens

internal data class ProfileDrawerDemoApp(
    val label: String,
    val packageName: String,
    val icon: ImageVector,
)

internal object ProfileDrawerDemoData {
    val workApps = listOf(
        ProfileDrawerDemoApp("Work Mail", "com.example.workmail", Icons.Default.Email),
        ProfileDrawerDemoApp("Team Directory", "com.example.directory", Icons.Default.Contacts),
        ProfileDrawerDemoApp("Work Documents", "com.example.documents", Icons.Default.Folder),
        ProfileDrawerDemoApp("Company Portal", "com.example.portal", Icons.Default.Work),
    )

    val privateApps = listOf(
        ProfileDrawerDemoApp(
            "Private Photos",
            "com.example.privatephotos",
            Icons.Default.PhotoLibrary,
        ),
        ProfileDrawerDemoApp("Private Mail", "com.example.privatemail", Icons.Default.Email),
        ProfileDrawerDemoApp("Private Browser", "com.example.privatebrowser", Icons.Default.Public),
        ProfileDrawerDemoApp("Private Files", "com.example.privatefiles", Icons.Default.Folder),
    )
}

@Composable
internal fun ProfileDrawerDemoAppRow(app: ProfileDrawerDemoApp) {
    ProfileDrawerAppRow(
        icon = {
            Surface(
                modifier = Modifier.size(Dimens.dp40),
                shape = RoundedCornerShape(Dimens.dp10),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(app.icon, contentDescription = null, modifier = Modifier.size(Dimens.dp24))
                }
            }
        },
        label = app.label,
        supportingText = app.packageName,
        onClick = {},
    )
}
