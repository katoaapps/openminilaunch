package com.katoaapps.openminilaunch.ui.minkpad

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.KeyEvent
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import com.katoaapps.openminilaunch.data.LauncherStore
import com.katoaapps.openminilaunch.features.minkpad.MinkPadRepository
import com.katoaapps.openminilaunch.features.minkpad.MinkPadStatus
import com.katoaapps.openminilaunch.features.minkpad.input.MinkPadHardwareInput
import com.katoaapps.openminilaunch.ui.theme.MinkLauncherTheme

class MinkPadActivity : ComponentActivity() {
    private val repository by lazy { MinkPadRepository.get(this) }
    private val hardware by lazy { MinkPadHardwareInput(repository) }
    private var captureHardwareInput = false
    private val permissionRequest = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { grants ->
        if (repository.permissionsGranted()) startSession()
        else if (Build.VERSION.SDK_INT >= 31 && grants.any { (permission, granted) -> !granted && !shouldShowRequestPermissionRationale(permission) }) {
            startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName")))
        }
    }
    private val notificationRequest = registerForActivityResult(ActivityResultContracts.RequestPermission()) { repository.start() }
    private val enableBluetooth = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (repository.bluetoothEnabled()) startSession()
    }
    private val makeDiscoverable = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode > 0) repository.beginClassicPairing(result.resultCode)
        else repository.pairingDeclined()
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val store = LauncherStore.get(this)
        setContent {
            MinkLauncherTheme(store) {
                MinkPadScreen(repository, onStart = ::startSession,
                    onDiscover = ::requestPairing, onClose = ::finish,
                    onHardwareCaptureChanged = {
                        captureHardwareInput = it
                        if (!it) hardware.reset()
                    })
            }
        }
    }

    private fun requestPairing() {
        makeDiscoverable.launch(
            Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE)
                .putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, PAIRING_SECONDS),
        )
    }

    private fun startSession() {
        if (!repository.supported()) { repository.start(); return }
        if (!repository.permissionsGranted()) {
            if (Build.VERSION.SDK_INT >= 31) permissionRequest.launch(arrayOf(Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_ADVERTISE))
            return
        }
        if (!repository.bluetoothEnabled()) {
            enableBluetooth.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)); return
        }
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            notificationRequest.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else repository.start()
    }
    override fun onResume() { super.onResume(); repository.refreshHosts() }
    override fun onPause() {
        hardware.reset()
        repository.releaseInput()
        super.onPause()
    }
    // Framework Activity's public dispatch hook; this AndroidX version adds a library annotation.
    @android.annotation.SuppressLint("RestrictedApi")
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (captureHardwareInput && repository.state.value.keyboardReady && repository.state.value.status == MinkPadStatus.CONNECTED && hardware.handle(event)) return true
        return super.dispatchKeyEvent(event)
    }

    private companion object { const val PAIRING_SECONDS = 120 }
}
