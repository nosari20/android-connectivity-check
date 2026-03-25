package com.nosari20.connectivitytest

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.nosari20.connectivitytest.utils.ConnectionDetails

class ConnectivityTest(
    val host: String,
    val port: Int,
    val ssl: Boolean,
    var certAlias: String = "",
    var enableCrlCheck: Boolean = false  // CRL checking disabled by default
) {
    enum class Status {
        PENDING, KO, OK, UNKNOWN
    }

    var status: Status by mutableStateOf(Status.UNKNOWN)
    var info: String by mutableStateOf("")
    var connectionDetails: ConnectionDetails? by mutableStateOf(null)
}