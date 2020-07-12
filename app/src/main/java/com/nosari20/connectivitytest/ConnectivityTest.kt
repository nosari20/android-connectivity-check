package com.nosari20.connectivitytest

class ConnectivityTest(val host: String, val port: Int) {
    enum class Status {
        PENDING, KO, OK, UNKNOWN
    }

    var status: Status = Status.UNKNOWN
    var info: String = ""

}