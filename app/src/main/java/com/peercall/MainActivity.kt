package com.peercall

import android.app.role.RoleManager
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.telecom.TelecomManager
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.peercall.data.QuotaRepository
import com.peercall.esim.EsimProfileManager
import com.peercall.telephony.SignalMonitor
import com.peercall.telephony.TelecomCallLauncher
import com.peercall.ui.DialerScreen
import com.peercall.ui.DialerViewModel

class MainActivity : ComponentActivity() {
    private val viewModel: DialerViewModel by viewModels {
        object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val repository = QuotaRepository(applicationContext)
                val signalMonitor = SignalMonitor(applicationContext)
                val profileManager = EsimProfileManager(applicationContext)
                @Suppress("UNCHECKED_CAST")
                return DialerViewModel(repository, TelecomCallLauncher(applicationContext), signalMonitor, profileManager) as T
            }
        }
    }

    private val dialerRoleRequest = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        if (isDefaultDialer()) {
            requestCallPermission()
        } else {
            viewModel.onCallSetupUnavailable("Set Peercall as the default phone app to track calls")
        }
    }

    private val callPermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            viewModel.onCallClicked()
        } else {
            viewModel.onCallSetupUnavailable("Phone permission is required to place a call")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        intent.data?.schemeSpecificPart?.let(viewModel::onNumberChanged)

        setContent {
            PeerCallTheme {
                Surface(
                    modifier = androidx.compose.ui.Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val uiState by viewModel.uiState.collectAsState()
                    DialerScreen(uiState, viewModel, ::requestCall)
                }
            }
        }
    }

    private fun requestCall() {
        if (!isDefaultDialer()) {
            val roleIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val roleManager = getSystemService(RoleManager::class.java)
                if (roleManager == null || !roleManager.isRoleAvailable(RoleManager.ROLE_DIALER)) {
                    viewModel.onCallSetupUnavailable("Default phone role is unavailable on this device")
                    return
                }
                roleManager.createRequestRoleIntent(RoleManager.ROLE_DIALER)
            } else {
                Intent(TelecomManager.ACTION_CHANGE_DEFAULT_DIALER).apply {
                    putExtra(TelecomManager.EXTRA_CHANGE_DEFAULT_DIALER_PACKAGE_NAME, packageName)
                }
            }
            dialerRoleRequest.launch(roleIntent)
            return
        }

        requestCallPermission()
    }

    private fun requestCallPermission() {
        if (checkSelfPermission(Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED) {
            viewModel.onCallClicked()
        } else {
            callPermissionRequest.launch(Manifest.permission.CALL_PHONE)
        }
    }

    private fun isDefaultDialer(): Boolean =
        getSystemService(TelecomManager::class.java)?.defaultDialerPackage == packageName

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        intent.data?.schemeSpecificPart?.let(viewModel::onNumberChanged)
    }

}

@Composable
fun PeerCallTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(),
        content = content
    )
}
