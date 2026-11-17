package com.peercall.telephony

import android.content.Context
import android.telephony.CellInfo
import android.telephony.CellInfoCdma
import android.telephony.CellInfoGsm
import android.telephony.CellInfoLte
import android.telephony.CellInfoWcdma
import android.telephony.TelephonyManager

data class SignalSnapshot(
    val dbm: Int,
    val bars: Int,
    val label: String,
    val isWeak: Boolean,
    val warning: String?
)

object SignalQuality {
    fun classify(dbm: Int): SignalSnapshot {
        val bars = when {
            dbm >= -70 -> 4
            dbm >= -85 -> 3
            dbm >= -100 -> 2
            dbm >= -110 -> 1
            else -> 0
        }

        val label = when {
            dbm >= -70 -> "Strong"
            dbm >= -85 -> "Good"
            dbm >= -100 -> "Fair"
            dbm >= -110 -> "Weak"
            else -> "No signal"
        }

        val warning = if (bars <= 1) "About to get choppy" else null

        return SignalSnapshot(
            dbm = dbm,
            bars = bars,
            label = label,
            isWeak = bars <= 1,
            warning = warning
        )
    }
}

class SignalMonitor(private val context: Context) {
    fun current(): SignalSnapshot {
        val telephony = context.getSystemService(TelephonyManager::class.java) ?: return emptySignal()
        val values = telephony.allCellInfo
            ?.mapNotNull { cellInfo -> dbmFromCellInfo(cellInfo) }
            .orEmpty()

        if (values.isEmpty()) return emptySignal()

        val strongest = values.maxOrNull() ?: 0
        return SignalQuality.classify(strongest)
    }

    private fun emptySignal(): SignalSnapshot = SignalSnapshot(
        dbm = Int.MIN_VALUE,
        bars = 0,
        label = "No signal",
        isWeak = true,
        warning = "About to get choppy"
    )

    private fun dbmFromCellInfo(cellInfo: CellInfo): Int? = when (cellInfo) {
        is CellInfoGsm -> cellInfo.cellSignalStrength.dbm
        is CellInfoLte -> cellInfo.cellSignalStrength.dbm
        is CellInfoCdma -> cellInfo.cellSignalStrength.dbm
        is CellInfoWcdma -> cellInfo.cellSignalStrength.dbm
        else -> null
    }
}
