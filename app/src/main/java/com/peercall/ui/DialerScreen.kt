package com.peercall.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.peercall.telephony.CallState
import com.peercall.telephony.CallStateManager
import com.peercall.telephony.PhoneNumberValidator

@Composable
fun DialerScreen(
    uiState: DialerUiState,
    viewModel: DialerViewModel,
    onCallRequested: () -> Unit
) {
    val numberIsValid = PhoneNumberValidator.normalize(uiState.number) != null
    val callIsInProgress = uiState.callState in setOf(
        CallState.RINGING,
        CallState.DIALING,
        CallState.ACTIVE,
        CallState.HOLDING
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Peercall",
            style = MaterialTheme.typography.headlineLarge
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Account: ${uiState.accountId}",
            style = MaterialTheme.typography.bodyMedium
        )

        Spacer(modifier = Modifier.height(16.dp))

        uiState.profiles.takeIf { it.isNotEmpty() }?.let { profiles ->
            Text(
                text = "Profiles",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            profiles.forEach { profile ->
                Text(
                    text = "${profile.name} · ${profile.carrier} · ${profile.status}",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        OutlinedTextField(
            value = uiState.number,
            onValueChange = { viewModel.onNumberChanged(it) },
            label = { Text("Dial number") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onCallRequested,
            enabled = numberIsValid && !uiState.isStartingCall && !callIsInProgress,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (uiState.isStartingCall) {
                CircularProgressIndicator(
                    modifier = Modifier.height(20.dp),
                    strokeWidth = 2.dp
                )
            } else {
                Text("Call")
            }
        }

        if (callIsInProgress) {
            Spacer(modifier = Modifier.height(8.dp))
            if (uiState.callState == CallState.RINGING) {
                Button(
                    onClick = { CallStateManager.answerCurrentRingingCall() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Answer")
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
            Button(
                onClick = { CallStateManager.disconnectCurrentCall() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("End call")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = uiState.minutesRemaining?.let { "Last confirmed balance: $it min" }
                ?: "Balance unavailable offline",
            style = MaterialTheme.typography.bodyLarge
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Signal: ${uiState.signalLabel} (${uiState.signalBars}/4)",
            style = MaterialTheme.typography.bodyMedium
        )

        uiState.signalWarning?.let { warning ->
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = warning,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.tertiary
            )
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Quality score: ${uiState.qualityScore}/100",
            style = MaterialTheme.typography.bodyMedium
        )

        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "State: ${uiState.callState}",
            style = MaterialTheme.typography.bodyMedium
        )

        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = { viewModel.triggerEmergencyFallback() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Emergency")
        }

        uiState.emergencyNotice?.let { message ->
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        uiState.lockScreenFallbackNotice?.let { message ->
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        uiState.callDestination?.let { destination ->
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Calling: $destination",
                style = MaterialTheme.typography.bodyMedium
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Status: ${uiState.status}",
            style = MaterialTheme.typography.bodyMedium
        )
    }
}
