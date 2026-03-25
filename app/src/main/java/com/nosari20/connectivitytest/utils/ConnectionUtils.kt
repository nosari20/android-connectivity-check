package com.nosari20.connectivitytest.utils

import android.security.KeyChain
import android.content.Context
import android.util.Log
import com.nosari20.connectivitytest.ConnectivityTest
import java.net.Socket
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.text.SimpleDateFormat
import java.util.*
import javax.net.ssl.*

// Enhanced data class to store detailed connection information
data class ConnectionDetails(
    var ipAddress: String = "",
    var responseTimeMs: Long = 0,
    var protocol: String = "",
    var cipherSuite: String = "",
    var peerHost: String = "",
    var peerPort: Int = 0,
    var serverCertificates: List<CertificateInfo> = emptyList(),
    var clientCertificate: CertificateInfo? = null,
    var hostnameVerified: Boolean = false,
    var hostnameVerificationDetails: String = "",
    var crlChecked: Boolean = false,
    var crlStatus: String = "",
    var error: String? = null
)

data class CertificateInfo(
    val subjectDN: String,
    val issuerDN: String,
    val serialNumber: String,
    val validFrom: Date,
    val validTo: Date,
    val signatureAlgorithm: String,
    val publicKeyAlgorithm: String,
    val version: Int,
    val subjectAlternativeNames: List<String> = emptyList(),
    val keyUsage: List<String> = emptyList(),
    val extendedKeyUsage: List<String> = emptyList(),
    val isSelfSigned: Boolean = false,
    val publicKeyHash: String = "",
    val crlDistributionPoints: List<String> = emptyList(),
    val isRevoked: Boolean? = null,
    val revocationReason: String? = null
)

// Custom TrustManager that captures certificate chain even on validation failure
// Uses X509ExtendedTrustManager for hostname-aware checking (required by domain-specific config)
class CertificateCapturingTrustManager : X509ExtendedTrustManager() {
    var capturedChain: Array<X509Certificate>? = null
    private val defaultTrustManager: X509ExtendedTrustManager
    private val tag = "TRUST_MGR"

    init {
        Log.d(tag, "Initializing CertificateCapturingTrustManager")
        val trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
        trustManagerFactory.init(null as java.security.KeyStore?)
        defaultTrustManager = trustManagerFactory.trustManagers
            .filterIsInstance<X509ExtendedTrustManager>()
            .first()
        Log.d(tag, "Default trust manager initialized")
    }

    override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) {
        Log.d(tag, "checkClientTrusted called (authType: $authType)")
        defaultTrustManager.checkClientTrusted(chain, authType)
    }

    override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {
        Log.d(tag, "checkServerTrusted called (authType: $authType, chain length: ${chain.size})")
        // Capture the chain BEFORE validation
        capturedChain = chain
        Log.d(tag, "Captured certificate chain (${chain.size} certificates)")
        chain.forEachIndexed { index, cert ->
            Log.d(tag, "  Chain[$index]: ${cert.subjectX500Principal.name}")
        }

        try {
            // Now attempt validation (may throw exception)
            defaultTrustManager.checkServerTrusted(chain, authType)
            Log.d(tag, "✓ Certificate chain validation PASSED")
        } catch (e: Exception) {
            Log.e(tag, "✗ Certificate chain validation FAILED: ${e.javaClass.simpleName} - ${e.message}")
            throw e
        }
    }

    // Hostname-aware methods (required for domain-specific network security config)
    override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String, socket: Socket) {
        Log.d(tag, "checkClientTrusted (socket-aware) called")
        capturedChain = chain
        defaultTrustManager.checkClientTrusted(chain, authType, socket)
    }

    override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String, engine: SSLEngine) {
        Log.d(tag, "checkClientTrusted (engine-aware) called")
        capturedChain = chain
        defaultTrustManager.checkClientTrusted(chain, authType, engine)
    }

    override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String, socket: Socket) {
        Log.d(tag, "checkServerTrusted (socket-aware) called (chain length: ${chain.size})")
        // Capture the chain BEFORE validation
        capturedChain = chain
        Log.d(tag, "Captured certificate chain (${chain.size} certificates)")

        try {
            // Now attempt validation (may throw exception)
            defaultTrustManager.checkServerTrusted(chain, authType, socket)
            Log.d(tag, "✓ Certificate chain validation PASSED (socket-aware)")
        } catch (e: Exception) {
            Log.e(tag, "✗ Certificate chain validation FAILED (socket-aware): ${e.javaClass.simpleName} - ${e.message}")
            throw e
        }
    }

    override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String, engine: SSLEngine) {
        Log.d(tag, "checkServerTrusted (engine-aware) called (chain length: ${chain.size})")
        // Capture the chain BEFORE validation
        capturedChain = chain
        Log.d(tag, "Captured certificate chain (${chain.size} certificates)")

        try {
            // Now attempt validation (may throw exception)
            defaultTrustManager.checkServerTrusted(chain, authType, engine)
            Log.d(tag, "✓ Certificate chain validation PASSED (engine-aware)")
        } catch (e: Exception) {
            Log.e(tag, "✗ Certificate chain validation FAILED (engine-aware): ${e.javaClass.simpleName} - ${e.message}")
            throw e
        }
    }

    override fun getAcceptedIssuers(): Array<X509Certificate> {
        return defaultTrustManager.acceptedIssuers
    }
}

fun checkTcp(test: ConnectivityTest) {
    val tag = "TCP_CHECK"
    Log.d(tag, "========================================")
    Log.d(tag, "Starting TCP connection test")
    Log.d(tag, "Host: ${test.host}")
    Log.d(tag, "Port: ${test.port}")
    Log.d(tag, "========================================")

    try {
        Log.d(tag, "Attempting to create socket connection...")
        val before = System.currentTimeMillis()
        val client = Socket(test.host, test.port)
        val after = System.currentTimeMillis()

        val responseTime = after - before
        val ipAddress = client.inetAddress.hostAddress ?: ""
        val peerHost = client.inetAddress.hostName
        val peerPort = client.port

        Log.d(tag, "✓ Socket connection successful!")
        Log.d(tag, "IP Address: $ipAddress")
        Log.d(tag, "Peer Host: $peerHost")
        Log.d(tag, "Peer Port: $peerPort")
        Log.d(tag, "Response Time: ${responseTime}ms")

        val details = ConnectionDetails(
            ipAddress = ipAddress,
            responseTimeMs = responseTime,
            peerHost = peerHost,
            peerPort = peerPort
        )

        client.close()
        Log.d(tag, "Socket closed successfully")

        test.status = ConnectivityTest.Status.OK
        test.info = "Time: ${details.responseTimeMs}ms (${details.ipAddress})"
        test.connectionDetails = details

        Log.d(tag, "✓ TCP test PASSED")
        Log.d(tag, "========================================")
    } catch (e: Exception) {
        Log.e(tag, "✗ TCP connection FAILED", e)
        Log.e(tag, "Exception Type: ${e.javaClass.simpleName}")
        Log.e(tag, "Exception Message: ${e.localizedMessage}")
        Log.e(tag, "Stack trace:", e)

        test.status = ConnectivityTest.Status.KO
        test.info = e.localizedMessage ?: "Unknown error"
        test.connectionDetails = ConnectionDetails(error = e.toString())

        Log.d(tag, "========================================")
    }
}

fun checkSsl(test: ConnectivityTest, context: Context? = null) {
    val tag = "SSL_CHECK"
    Log.d(tag, "========================================")
    Log.d(tag, "Starting SSL/TLS connection test")
    Log.d(tag, "Host: ${test.host}")
    Log.d(tag, "Port: ${test.port}")
    Log.d(tag, "Client Auth Alias: ${test.certAlias}")
    Log.d(tag, "CRL Check Enabled: ${test.enableCrlCheck}")
    Log.d(tag, "========================================")

    val clientAuth = test.certAlias.isNotEmpty() && test.certAlias != "null"
    Log.d(tag, "Client Authentication Required: $clientAuth")

    // Create custom trust manager to capture certificates even on failure
    Log.d(tag, "Creating custom certificate capturing trust manager...")
    val capturingTrustManager = CertificateCapturingTrustManager()
    val sslContext = SSLContext.getInstance("TLS")
    sslContext.init(null, arrayOf(capturingTrustManager), null)
    val sslFactory = sslContext.socketFactory
    Log.d(tag, "SSL context initialized successfully")

    try {
        Log.d(tag, "Creating SSL socket connection...")
        val client = sslFactory.createSocket(test.host, test.port) as SSLSocket
        Log.d(tag, "SSL socket created, starting handshake...")

        val before = System.currentTimeMillis()
        client.startHandshake()
        val after = System.currentTimeMillis()

        val responseTime = after - before
        Log.d(tag, "✓ SSL handshake successful! (${responseTime}ms)")

        val sslSession = client.session
        Log.d(tag, "SSL Session Info:")
        Log.d(tag, "  Protocol: ${sslSession.protocol}")
        Log.d(tag, "  Cipher Suite: ${sslSession.cipherSuite}")
        Log.d(tag, "  Peer Host: ${sslSession.peerHost}")
        Log.d(tag, "  Peer Port: ${sslSession.peerPort}")

        val details = ConnectionDetails(
            ipAddress = client.inetAddress.hostAddress ?: "",
            responseTimeMs = responseTime,
            protocol = sslSession.protocol,
            cipherSuite = sslSession.cipherSuite,
            peerHost = sslSession.peerHost,
            peerPort = sslSession.peerPort
        )
        Log.d(tag, "  IP Address: ${details.ipAddress}")

        // Extract server certificate chain - use captured chain
        try {
            Log.d(tag, "Extracting server certificate chain...")
            val serverCerts = capturingTrustManager.capturedChain
            if (serverCerts != null) {
                Log.d(tag, "Found ${serverCerts.size} certificates in chain")
                serverCerts.forEachIndexed { index, cert ->
                    Log.d(tag, "  Cert $index: ${cert.subjectX500Principal.name}")
                }

                details.serverCertificates = serverCerts.map { cert ->
                    extractCertificateInfo(cert, test.enableCrlCheck)
                }
                Log.d(tag, "✓ Certificate chain extracted successfully")
            } else {
                Log.w(tag, "⚠ No certificates captured by trust manager")
            }
        } catch (e: Exception) {
            Log.e(tag, "✗ Failed to extract server certificates", e)
        }

        // Extract client certificate if used
        if (clientAuth && context != null) {
            try {
                Log.d(tag, "Extracting client certificate for alias: ${test.certAlias}")
                val clientCerts = KeyChain.getCertificateChain(context, test.certAlias)
                if (clientCerts != null && clientCerts.isNotEmpty()) {
                    Log.d(tag, "Found ${clientCerts.size} client certificates")
                    details.clientCertificate = extractCertificateInfo(clientCerts[0], test.enableCrlCheck)
                    Log.d(tag, "✓ Client certificate extracted")
                } else {
                    Log.w(tag, "⚠ No client certificates found for alias")
                }
            } catch (e: Exception) {
                Log.e(tag, "✗ Failed to extract client certificate", e)
            }
        }

        // Improved hostname verification with detailed feedback
        Log.d(tag, "Performing hostname verification...")
        val hostnameCheckResult = verifyHostname(test.host, capturingTrustManager.capturedChain)
        details.hostnameVerified = hostnameCheckResult.verified
        details.hostnameVerificationDetails = hostnameCheckResult.details
        Log.d(tag, "Hostname Verification: ${if (hostnameCheckResult.verified) "✓ PASSED" else "✗ FAILED"}")
        Log.d(tag, "Details: ${hostnameCheckResult.details}")

        // Check CRL if enabled and certificates are available
        if (test.enableCrlCheck && details.serverCertificates.isNotEmpty()) {
            Log.d(tag, "CRL checking is ENABLED for test: ${test.host}")
            Log.d(tag, "Number of certificates to check: ${details.serverCertificates.size}")

            val crlResult = checkCertificateRevocation(details.serverCertificates)
            details.crlChecked = crlResult.checked
            details.crlStatus = crlResult.status

            Log.d(tag, "CRL check completed. Status: ${details.crlStatus}")

            // If CRL check is enabled and any certificate is revoked, fail the connection
            if (details.serverCertificates.any { it.isRevoked == true }) {
                Log.w(tag, "⚠ CERTIFICATE REVOKED - Failing test")
                test.status = ConnectivityTest.Status.KO
                test.info = "Certificate revoked: ${details.crlStatus}"
                test.connectionDetails = details
                client.close()
                Log.d(tag, "========================================")
                return
            } else {
                Log.d(tag, "✓ No revoked certificates found")
            }
        } else {
            if (!test.enableCrlCheck) {
                Log.d(tag, "CRL checking is DISABLED for test: ${test.host}")
            } else {
                Log.w(tag, "⚠ No certificates available for CRL check")
            }
        }

        if (!details.hostnameVerified) {
            Log.w(tag, "✗ Test FAILED: Hostname verification failed")
            test.status = ConnectivityTest.Status.KO
            test.info = "Hostname verification failed: ${details.hostnameVerificationDetails}"
        } else {
            Log.d(tag, "✓ SSL test PASSED")
            test.status = ConnectivityTest.Status.OK
            test.info = "Time: ${details.responseTimeMs}ms (${details.ipAddress})\nCertificate : OK, Protocol : ${details.protocol}${if (clientAuth) " (mutual)" else ""}"
        }

        test.connectionDetails = details
        client.close()
        Log.d(tag, "SSL socket closed")
        Log.d(tag, "========================================")
    } catch (e: SSLHandshakeException) {
        Log.e(tag, "✗ SSL HANDSHAKE FAILED", e)
        Log.e(tag, "Exception Type: SSLHandshakeException")
        Log.e(tag, "Exception Message: ${e.localizedMessage}")
        Log.e(tag, "Cause: ${e.cause?.javaClass?.simpleName} - ${e.cause?.message}")

        test.status = ConnectivityTest.Status.KO

        // Provide more specific error messages based on the cause
        val errorMessage = when {
            e.cause is java.io.EOFException -> {
                Log.e(tag, "⚠ Server closed connection unexpectedly during handshake")
                Log.e(tag, "Possible causes:")
                Log.e(tag, "  1. Server requires client certificate authentication")
                Log.e(tag, "  2. TLS version or cipher suite mismatch")
                Log.e(tag, "  3. Server-side configuration issue")
                "SSL handshake failed: Server closed connection\nPossible causes:\n• Server requires client certificate\n• TLS/cipher mismatch\n• Server configuration issue"
            }
            e.message?.contains("certificate", ignoreCase = true) == true -> {
                "SSL handshake failed: Certificate issue\n${e.localizedMessage}"
            }
            e.message?.contains("protocol", ignoreCase = true) == true -> {
                "SSL handshake failed: Protocol issue\n${e.localizedMessage}"
            }
            else -> {
                "SSL handshake failed: ${e.localizedMessage}"
            }
        }

        test.info = errorMessage

        // Create connection details with captured certificate chain
        val details = ConnectionDetails(error = e.toString())

        // Use the captured certificate chain from our custom trust manager
        try {
            Log.d(tag, "Attempting to extract certificate chain from failed handshake...")
            val capturedCerts = capturingTrustManager.capturedChain
            if (capturedCerts != null && capturedCerts.isNotEmpty()) {
                Log.d(tag, "Found ${capturedCerts.size} certificates from failed handshake")

                // Perform hostname verification on captured certificates even though handshake failed
                val hostnameCheckResult = verifyHostname(test.host, capturedCerts)
                details.hostnameVerified = hostnameCheckResult.verified
                details.hostnameVerificationDetails = hostnameCheckResult.details
                Log.d(tag, "Hostname verification on captured certs: ${if (hostnameCheckResult.verified) "✓ PASSED" else "✗ FAILED"}")
                Log.d(tag, "Details: ${hostnameCheckResult.details}")

                details.serverCertificates = capturedCerts.map { cert ->
                    extractCertificateInfo(cert, test.enableCrlCheck)
                }
                Log.d(tag, "✓ Certificate chain extracted from failed handshake")
            } else {
                Log.w(tag, "⚠ No certificates captured during failed handshake")
            }
        } catch (certError: Exception) {
            Log.e(tag, "✗ Failed to extract certificates from failed handshake", certError)
        }

        // Try to get basic connection info
        try {
            Log.d(tag, "Attempting to get basic connection info...")
            val client = sslFactory.createSocket(test.host, test.port) as SSLSocket
            details.ipAddress = client.inetAddress.hostAddress ?: ""
            details.peerHost = client.inetAddress.hostName
            details.peerPort = client.port
            Log.d(tag, "✓ Connection info: ${details.ipAddress}, ${details.peerHost}:${details.peerPort}")
            client.close()
        } catch (captureError: Exception) {
            Log.e(tag, "✗ Failed to capture connection info", captureError)
        }

        test.connectionDetails = details
        Log.d(tag, "========================================")
    } catch (e: Exception) {
        Log.e(tag, "✗ SSL CONNECTION FAILED", e)
        Log.e(tag, "Exception Type: ${e.javaClass.simpleName}")
        Log.e(tag, "Exception Message: ${e.localizedMessage}")
        Log.e(tag, "Stack trace:", e)

        test.status = ConnectivityTest.Status.KO

        // Provide more specific error messages
        val errorMessage = when (e) {
            is java.net.UnknownHostException -> "SSL connection failed: Unknown host\n${test.host} could not be resolved"
            is java.net.ConnectException -> "SSL connection failed: Connection refused\nServer at ${test.host}:${test.port} refused connection"
            is java.net.SocketTimeoutException -> "SSL connection failed: Connection timeout\nServer at ${test.host}:${test.port} did not respond"
            is javax.net.ssl.SSLProtocolException -> "SSL connection failed: Protocol error\n${e.localizedMessage}"
            is javax.net.ssl.SSLPeerUnverifiedException -> "SSL connection failed: Peer verification failed\n${e.localizedMessage}"
            else -> "SSL connection failed: ${e.localizedMessage ?: e.javaClass.simpleName}"
        }

        test.info = errorMessage

        // Create connection details with captured certificate chain
        val details = ConnectionDetails(error = e.toString())

        // Use the captured certificate chain from our custom trust manager
        try {
            Log.d(tag, "Attempting to extract certificate chain from failed connection...")
            val capturedCerts = capturingTrustManager.capturedChain
            if (capturedCerts != null && capturedCerts.isNotEmpty()) {
                Log.d(tag, "Found ${capturedCerts.size} certificates from failed connection")

                // Perform hostname verification on captured certificates
                val hostnameCheckResult = verifyHostname(test.host, capturedCerts)
                details.hostnameVerified = hostnameCheckResult.verified
                details.hostnameVerificationDetails = hostnameCheckResult.details
                Log.d(tag, "Hostname verification on captured certs: ${if (hostnameCheckResult.verified) "✓ PASSED" else "✗ FAILED"}")
                Log.d(tag, "Details: ${hostnameCheckResult.details}")

                details.serverCertificates = capturedCerts.map { cert ->
                    extractCertificateInfo(cert, test.enableCrlCheck)
                }
                Log.d(tag, "✓ Certificate chain extracted from failed connection")
            } else {
                Log.w(tag, "⚠ No certificates captured during failed connection")
            }
        } catch (certError: Exception) {
            Log.e(tag, "✗ Failed to extract certificates from failed connection", certError)
        }

        // Try to get basic connection info
        try {
            Log.d(tag, "Attempting to get basic connection info...")
            val client = sslFactory.createSocket(test.host, test.port) as SSLSocket
            details.ipAddress = client.inetAddress.hostAddress ?: ""
            details.peerHost = client.inetAddress.hostName
            details.peerPort = client.port
            Log.d(tag, "✓ Connection info: ${details.ipAddress}, ${details.peerHost}:${details.peerPort}")
            client.close()
        } catch (captureError: Exception) {
            Log.e(tag, "✗ Failed to capture connection info", captureError)
        }

        test.connectionDetails = details
        Log.d(tag, "========================================")
    }
}

private fun extractCertificateInfo(cert: X509Certificate, enableCrlCheck: Boolean = false): CertificateInfo {
    val tag = "CERT_EXTRACT"
    val certSubject = cert.subjectX500Principal.name
    Log.d(tag, "========================================")
    Log.d(tag, "Extracting certificate information")
    Log.d(tag, "Subject: $certSubject")
    Log.d(tag, "Issuer: ${cert.issuerX500Principal.name}")
    Log.d(tag, "Serial: ${cert.serialNumber.toString(16).uppercase()}")
    Log.d(tag, "Valid From: ${cert.notBefore}")
    Log.d(tag, "Valid To: ${cert.notAfter}")
    Log.d(tag, "========================================")

    val subjectAltNames = mutableListOf<String>()
    try {
        val sans = cert.subjectAlternativeNames
        if (sans != null) {
            Log.d(tag, "Extracting Subject Alternative Names...")
            sans.forEach { san ->
                when (san[0]) {
                    2 -> {
                        val dnsName = "DNS: ${san[1]}"
                        subjectAltNames.add(dnsName)
                        Log.d(tag, "  $dnsName")
                    }
                    7 -> {
                        val ipName = "IP: ${san[1]}"
                        subjectAltNames.add(ipName)
                        Log.d(tag, "  $ipName")
                    }
                    6 -> {
                        val uriName = "URI: ${san[1]}"
                        subjectAltNames.add(uriName)
                        Log.d(tag, "  $uriName")
                    }
                    1 -> {
                        val emailName = "Email: ${san[1]}"
                        subjectAltNames.add(emailName)
                        Log.d(tag, "  $emailName")
                    }
                }
            }
            Log.d(tag, "✓ Found ${subjectAltNames.size} SANs")
        } else {
            Log.d(tag, "No Subject Alternative Names found")
        }
    } catch (e: Exception) {
        Log.e(tag, "Failed to extract SANs", e)
    }

    val keyUsages = mutableListOf<String>()
    try {
        val keyUsageArray = cert.keyUsage
        if (keyUsageArray != null) {
            Log.d(tag, "Extracting Key Usage...")
            keyUsageArray.forEachIndexed { index, used ->
                if (used) {
                    val usage = when (index) {
                        0 -> "Digital Signature"
                        1 -> "Non Repudiation"
                        2 -> "Key Encipherment"
                        3 -> "Data Encipherment"
                        4 -> "Key Agreement"
                        5 -> "Key Cert Sign"
                        6 -> "CRL Sign"
                        7 -> "Encipher Only"
                        8 -> "Decipher Only"
                        else -> "Unknown"
                    }
                    keyUsages.add(usage)
                    Log.d(tag, "  $usage")
                }
            }
            Log.d(tag, "✓ Found ${keyUsages.size} key usages")
        } else {
            Log.d(tag, "No Key Usage extension found")
        }
    } catch (e: Exception) {
        Log.e(tag, "Failed to extract key usage", e)
    }

    val extKeyUsages = mutableListOf<String>()
    try {
        val extKeyUsageList = cert.extendedKeyUsage
        if (extKeyUsageList != null) {
            Log.d(tag, "Extracting Extended Key Usage...")
            extKeyUsageList.forEach { oid ->
                val usage = when (oid) {
                    "1.3.6.1.5.5.7.3.1" -> "Server Authentication"
                    "1.3.6.1.5.5.7.3.2" -> "Client Authentication"
                    "1.3.6.1.5.5.7.3.3" -> "Code Signing"
                    "1.3.6.1.5.5.7.3.4" -> "Email Protection"
                    "1.3.6.1.5.5.7.3.8" -> "Time Stamping"
                    "1.3.6.1.5.5.7.3.9" -> "OCSP Signing"
                    else -> oid
                }
                extKeyUsages.add(usage)
                Log.d(tag, "  $usage")
            }
            Log.d(tag, "✓ Found ${extKeyUsages.size} extended key usages")
        } else {
            Log.d(tag, "No Extended Key Usage extension found")
        }
    } catch (e: Exception) {
        Log.e(tag, "Failed to extract extended key usage", e)
    }

    // Check if certificate is self-signed
    val isSelfSigned = cert.subjectX500Principal.equals(cert.issuerX500Principal)
    Log.d(tag, "Self-Signed: $isSelfSigned")

    // Compute public key hash for cross-signing detection
    val publicKeyHash = try {
        Log.d(tag, "Computing public key hash...")
        val digest = java.security.MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(cert.publicKey.encoded)
        val hashString = hash.joinToString("") { "%02x".format(it) }
        Log.d(tag, "✓ Public key hash: ${hashString.take(32)}...")
        hashString
    } catch (e: Exception) {
        Log.e(tag, "Failed to compute public key hash", e)
        ""
    }

    // Extract CRL Distribution Points
    val crlDistributionPoints = mutableListOf<String>()
    try {
        Log.d(tag, "Extracting CRL Distribution Points...")
        val crlDPExtension = cert.getExtensionValue("2.5.29.31") // CRL Distribution Points OID
        if (crlDPExtension != null) {
            // Parse the extension to extract URLs (simplified)
            val crlDPString = String(crlDPExtension)
            val httpMatches = Regex("http[s]?://[^\\s\"'<>]+").findAll(crlDPString)
            httpMatches.forEach { match ->
                crlDistributionPoints.add(match.value)
                Log.d(tag, "  ${match.value}")
            }
            Log.d(tag, "✓ Found ${crlDistributionPoints.size} CRL URLs")
        } else {
            Log.d(tag, "No CRL Distribution Points extension found")
        }
    } catch (e: Exception) {
        Log.e(tag, "Failed to extract CRL distribution points", e)
    }

    // Check revocation status if CRL URLs are available AND CRL check is enabled
    var isRevoked: Boolean? = null
    var revocationReason: String? = null

    if (enableCrlCheck && crlDistributionPoints.isNotEmpty()) {
        Log.d(tag, "CRL check ENABLED - Checking revocation status...")
        Log.d(tag, "Certificate: $certSubject")
        Log.d(tag, "CRL URLs: ${crlDistributionPoints.joinToString()}")

        try {
            val revocationResult = checkCRLRevocation(cert, crlDistributionPoints)
            isRevoked = revocationResult.first
            revocationReason = revocationResult.second

            Log.d(tag, "CRL check result: isRevoked=$isRevoked, reason=$revocationReason")
            if (isRevoked == true) {
                Log.w(tag, "⚠ CERTIFICATE IS REVOKED! Reason: $revocationReason")
            } else if (isRevoked == false) {
                Log.d(tag, "✓ Certificate is NOT revoked")
            } else {
                Log.w(tag, "⚠ CRL check inconclusive (CRL server may be unreachable)")
            }
        } catch (e: Exception) {
            Log.e(tag, "CRL check failed with exception", e)
        }
    } else if (!enableCrlCheck) {
        Log.d(tag, "CRL check DISABLED")
    } else {
        Log.d(tag, "No CRL URLs available - cannot check revocation")
    }

    Log.d(tag, "✓ Certificate extraction complete")
    Log.d(tag, "========================================")

    return CertificateInfo(
        subjectDN = cert.subjectX500Principal.name,
        issuerDN = cert.issuerX500Principal.name,
        serialNumber = cert.serialNumber.toString(16).uppercase(),
        validFrom = cert.notBefore,
        validTo = cert.notAfter,
        signatureAlgorithm = cert.sigAlgName,
        publicKeyAlgorithm = cert.publicKey.algorithm,
        version = cert.version,
        subjectAlternativeNames = subjectAltNames,
        keyUsage = keyUsages,
        extendedKeyUsage = extKeyUsages,
        isSelfSigned = isSelfSigned,
        publicKeyHash = publicKeyHash,
        crlDistributionPoints = crlDistributionPoints,
        isRevoked = isRevoked,
        revocationReason = revocationReason
    )
}

fun formatDate(date: Date): String {
    val format = SimpleDateFormat("MMM dd, yyyy HH:mm:ss z", Locale.getDefault())
    return format.format(date)
}

/**
 * Detects cross-signed certificates in a certificate chain.
 * Cross-signing occurs when the same public key is signed by different issuers.
 * This is common during CA transitions (e.g., Let's Encrypt switching root CAs).
 *
 * @param certificates List of certificates in the chain
 * @return List of indices that contain cross-signed certificates
 */
fun detectCrossSignedCertificates(certificates: List<CertificateInfo>): List<Int> {
    val crossSignedIndices = mutableListOf<Int>()

    // Group certificates by their public key hash (same key = potential cross-sign)
    val publicKeyMap = mutableMapOf<String, MutableList<Int>>()

    certificates.forEachIndexed { index, cert ->
        if (cert.publicKeyHash.isNotEmpty()) {
            publicKeyMap.getOrPut(cert.publicKeyHash) { mutableListOf() }.add(index)
        }
    }

    // Find keys that appear multiple times with different issuers
    publicKeyMap.forEach { (publicKeyHash, indices) ->
        if (indices.size > 1) {
            // Check if they have different issuers (true cross-signing)
            val issuers = indices.map { certificates[it].issuerDN }.toSet()
            if (issuers.size > 1) {
                crossSignedIndices.addAll(indices)
            }
        }
    }

    return crossSignedIndices.sorted()
}

/**
 * Gets information about cross-signing in the certificate chain.
 *
 * @param certificates List of certificates in the chain
 * @return Map of certificate index to list of cross-sign partner indices
 */
fun getCrossSigningInfo(certificates: List<CertificateInfo>): Map<Int, List<Int>> {
    val crossSignMap = mutableMapOf<Int, MutableList<Int>>()

    // Group by public key hash
    val publicKeyGroups = mutableMapOf<String, MutableList<Int>>()
    certificates.forEachIndexed { index, cert ->
        if (cert.publicKeyHash.isNotEmpty()) {
            publicKeyGroups.getOrPut(cert.publicKeyHash) { mutableListOf() }.add(index)
        }
    }

    // Build cross-signing relationships
    publicKeyGroups.forEach { (_, indices) ->
        if (indices.size > 1) {
            // Each cert in this group is cross-signed with the others
            indices.forEach { index ->
                crossSignMap[index] = indices.filter { it != index }.toMutableList()
            }
        }
    }

    return crossSignMap
}

/**
 * Analyzes the certificate chain for cross-signing patterns.
 *
 * @param certificates List of certificates in the chain
 * @return Human-readable description of cross-signing detected
 */
fun analyzeCrossSigningPattern(certificates: List<CertificateInfo>): String? {
    val crossSignInfo = getCrossSigningInfo(certificates)

    if (crossSignInfo.isEmpty()) {
        return null
    }

    val descriptions = mutableListOf<String>()
    val processed = mutableSetOf<Int>()

    crossSignInfo.forEach { (index, partners) ->
        if (index !in processed) {
            val allInGroup = listOf(index) + partners
            processed.addAll(allInGroup)

            val cert = certificates[index]
            val partnerIssuers = partners.map {
                extractCN(certificates[it].issuerDN)
            }.distinct()

            val description = buildString {
                append("Certificate at position ${index + 1}")
                append(" (${extractCN(cert.subjectDN)})")
                append(" is cross-signed by ${partnerIssuers.size + 1} different CAs:\n")
                append("  • ${extractCN(cert.issuerDN)}\n")
                partnerIssuers.forEach { issuer ->
                    append("  • $issuer\n")
                }
            }

            descriptions.add(description)
        }
    }

    return if (descriptions.isNotEmpty()) {
        descriptions.joinToString("\n")
    } else {
        null
    }
}

private fun extractCN(dn: String): String {
    return dn.split(",").find { it.trim().startsWith("CN=") }?.substringAfter("CN=")?.trim() ?: dn.take(50)
}

// Data classes for verification results
data class HostnameVerificationResult(
    val verified: Boolean,
    val details: String
)

data class CRLCheckResult(
    val checked: Boolean,
    val status: String
)

/**
 * Improved hostname verification with detailed feedback
 * Supports:
 * - Exact match (example.com == example.com)
 * - Wildcard match (*.example.com matches sub.example.com)
 * - Multiple wildcards
 * - CN fallback if no SANs
 */
fun verifyHostname(hostname: String, certChain: Array<X509Certificate>?): HostnameVerificationResult {
    val tag = "HOSTNAME_VERIFY"
    Log.d(tag, "========================================")
    Log.d(tag, "Starting hostname verification")
    Log.d(tag, "Hostname to verify: $hostname")

    if (certChain == null || certChain.isEmpty()) {
        Log.e(tag, "✗ No certificate chain available")
        Log.d(tag, "========================================")
        return HostnameVerificationResult(false, "No certificate chain available")
    }

    val cert = certChain[0] // Leaf certificate
    Log.d(tag, "Checking leaf certificate: ${cert.subjectX500Principal.name}")

    val hostnameLC = hostname.lowercase()
    Log.d(tag, "Normalized hostname: $hostnameLC")

    try {
        // First, check Subject Alternative Names (SANs) - RFC 6125 preferred method
        Log.d(tag, "Checking Subject Alternative Names (SANs)...")
        val sans = cert.subjectAlternativeNames
        val dnsNames = mutableListOf<String>()

        if (sans != null) {
            for (san in sans) {
                if (san[0] == 2) { // DNS name type
                    val dnsName = san[1].toString()
                    dnsNames.add(dnsName)
                    Log.d(tag, "  Found SAN: $dnsName")

                    if (matchesHostname(hostnameLC, dnsName.lowercase())) {
                        Log.d(tag, "✓ MATCH! Hostname verified via SAN: $dnsName")
                        Log.d(tag, "========================================")
                        return HostnameVerificationResult(
                            true,
                            "Matched SAN: $dnsName"
                        )
                    } else {
                        Log.d(tag, "  No match for this SAN")
                    }
                }
            }

            if (dnsNames.isNotEmpty()) {
                Log.w(tag, "✗ No matching SAN found")
                Log.w(tag, "Certificate has ${dnsNames.size} SANs: ${dnsNames.joinToString(", ")}")
                Log.d(tag, "========================================")
                return HostnameVerificationResult(
                    false,
                    "No matching SAN. Certificate has: ${dnsNames.joinToString(", ")}"
                )
            } else {
                Log.d(tag, "No DNS SANs found in certificate")
            }
        } else {
            Log.d(tag, "Certificate has no Subject Alternative Names")
        }

        // Fallback to CN in Subject DN (legacy, but still checked)
        Log.d(tag, "Falling back to CN in Subject DN (legacy method)...")
        val subjectDN = cert.subjectX500Principal.name
        Log.d(tag, "Subject DN: $subjectDN")

        val cnMatch = Regex("CN=([^,]+)").find(subjectDN)
        if (cnMatch != null) {
            val cn = cnMatch.groupValues[1]
            Log.d(tag, "Found CN: $cn")

            if (matchesHostname(hostnameLC, cn.lowercase())) {
                Log.d(tag, "✓ MATCH! Hostname verified via CN (legacy): $cn")
                Log.d(tag, "========================================")
                return HostnameVerificationResult(
                    true,
                    "Matched CN (legacy): $cn"
                )
            } else {
                Log.w(tag, "✗ CN does not match hostname")
                Log.d(tag, "========================================")
                return HostnameVerificationResult(
                    false,
                    "No SANs found. CN mismatch: $cn"
                )
            }
        }

        Log.e(tag, "✗ No SANs or CN found in certificate")
        Log.d(tag, "========================================")
        return HostnameVerificationResult(
            false,
            "No SANs or CN found in certificate"
        )
    } catch (e: Exception) {
        Log.e(tag, "✗ Verification error", e)
        Log.d(tag, "========================================")
        return HostnameVerificationResult(
            false,
            "Verification error: ${e.message}"
        )
    }
}

/**
 * Matches hostname against certificate name (supports wildcards)
 */
private fun matchesHostname(hostname: String, certName: String): Boolean {
    val tag = "HOSTNAME_MATCH"
    Log.d(tag, "Matching: hostname='$hostname' vs certName='$certName'")

    // Exact match
    if (hostname == certName) {
        Log.d(tag, "✓ Exact match")
        return true
    }

    // Wildcard match
    if (certName.startsWith("*.")) {
        Log.d(tag, "Wildcard pattern detected: $certName")
        val domain = certName.substring(2)
        Log.d(tag, "Domain part: $domain")

        // hostname must have at least one subdomain
        if (hostname.contains(".")) {
            val hostDomain = hostname.substringAfter(".")
            Log.d(tag, "Hostname domain part: $hostDomain")

            // Match domain part
            if (hostDomain == domain) {
                Log.d(tag, "✓ Wildcard match")
                return true
            } else {
                Log.d(tag, "✗ Wildcard domain mismatch")
            }
        } else {
            Log.d(tag, "✗ Hostname has no subdomain (cannot match wildcard)")
        }
    }

    Log.d(tag, "✗ No match")
    return false
}

/**
 * Check certificate revocation status using CRL
 */
fun checkCertificateRevocation(certificates: List<CertificateInfo>): CRLCheckResult {
    if (certificates.isEmpty()) {
        return CRLCheckResult(false, "No certificates to check")
    }

    val results = mutableListOf<String>()
    var anyRevoked = false

    for ((index, certInfo) in certificates.withIndex()) {
        if (certInfo.crlDistributionPoints.isEmpty()) {
            results.add("Cert $index: No CRL URLs")
            continue
        }

        // Check revocation status
        if (certInfo.isRevoked == true) {
            anyRevoked = true
            val reason = certInfo.revocationReason ?: "Unknown reason"
            results.add("Cert $index: ⚠ REVOKED ($reason)")
        } else if (certInfo.isRevoked == false) {
            results.add("Cert $index: ✓ Not revoked")
        } else {
            results.add("Cert $index: CRL check failed")
        }
    }

    val status = if (anyRevoked) {
        "⚠ Certificate revoked! ${results.joinToString("; ")}"
    } else {
        results.joinToString("; ")
    }

    return CRLCheckResult(true, status)
}

/**
 * Check if a certificate is revoked by downloading and parsing CRL
 * Returns Pair<isRevoked: Boolean?, reason: String?>
 */
private fun checkCRLRevocation(cert: X509Certificate, crlUrls: List<String>): Pair<Boolean?, String?> {
    val tag = "CRL_CHECK"
    Log.d(tag, "========================================")
    Log.d(tag, "Checking CRL revocation status")
    Log.d(tag, "Certificate: ${cert.subjectX500Principal.name}")
    Log.d(tag, "Serial Number: ${cert.serialNumber}")
    Log.d(tag, "Number of CRL URLs: ${crlUrls.size}")
    Log.d(tag, "========================================")

    for ((index, crlUrl) in crlUrls.withIndex()) {
        try {
            Log.d(tag, "Attempting CRL check ${index + 1}/${crlUrls.size}")
            Log.d(tag, "CRL URL: $crlUrl")

            // Download CRL (with timeout)
            val url = java.net.URL(crlUrl)
            val connection = url.openConnection()
            connection.connectTimeout = 10000 // 10 seconds
            connection.readTimeout = 10000
            Log.d(tag, "Connecting to CRL server (timeout: 10s)...")

            connection.getInputStream().use { input ->
                Log.d(tag, "Connection established, downloading CRL...")
                val cf = CertificateFactory.getInstance("X.509")
                val crl = cf.generateCRL(input) as java.security.cert.X509CRL

                Log.d(tag, "✓ CRL downloaded and parsed successfully")
                Log.d(tag, "CRL Issuer: ${crl.issuerX500Principal.name}")
                Log.d(tag, "CRL This Update: ${crl.thisUpdate}")
                Log.d(tag, "CRL Next Update: ${crl.nextUpdate}")

                // Check if certificate is in CRL
                Log.d(tag, "Checking if certificate serial ${cert.serialNumber} is in CRL...")
                val revokedCert = crl.getRevokedCertificate(cert.serialNumber)

                if (revokedCert != null) {
                    val reason = when (revokedCert.revocationReason) {
                        java.security.cert.CRLReason.UNSPECIFIED -> "Unspecified"
                        java.security.cert.CRLReason.KEY_COMPROMISE -> "Key Compromise"
                        java.security.cert.CRLReason.CA_COMPROMISE -> "CA Compromise"
                        java.security.cert.CRLReason.AFFILIATION_CHANGED -> "Affiliation Changed"
                        java.security.cert.CRLReason.SUPERSEDED -> "Superseded"
                        java.security.cert.CRLReason.CESSATION_OF_OPERATION -> "Cessation of Operation"
                        java.security.cert.CRLReason.CERTIFICATE_HOLD -> "Certificate Hold"
                        java.security.cert.CRLReason.REMOVE_FROM_CRL -> "Removed from CRL"
                        java.security.cert.CRLReason.PRIVILEGE_WITHDRAWN -> "Privilege Withdrawn"
                        java.security.cert.CRLReason.AA_COMPROMISE -> "AA Compromise"
                        else -> "Unknown"
                    }
                    Log.w(tag, "⚠⚠⚠ CERTIFICATE IS REVOKED ⚠⚠⚠")
                    Log.w(tag, "Revocation Reason: $reason")
                    Log.w(tag, "Revocation Date: ${revokedCert.revocationDate}")
                    Log.d(tag, "========================================")
                    return Pair(true, reason)
                }

                // Certificate not in CRL - not revoked
                Log.d(tag, "✓ Certificate NOT found in CRL")
                Log.d(tag, "✓ Certificate is NOT revoked")
                Log.d(tag, "========================================")
                return Pair(false, null)
            }
        } catch (e: java.net.SocketTimeoutException) {
            Log.e(tag, "✗ CRL check TIMEOUT from $crlUrl")
            Log.e(tag, "Timeout details: ${e.message}")
        } catch (e: java.io.IOException) {
            Log.e(tag, "✗ CRL check IO ERROR from $crlUrl")
            Log.e(tag, "IO Error: ${e.message}", e)
        } catch (e: Exception) {
            Log.e(tag, "✗ CRL check FAILED from $crlUrl")
            Log.e(tag, "Exception Type: ${e.javaClass.simpleName}")
            Log.e(tag, "Exception Message: ${e.message}", e)
        }

        if (index < crlUrls.size - 1) {
            Log.d(tag, "Trying next CRL URL...")
        }
    }

    // All CRL checks failed
    Log.w(tag, "⚠ All ${crlUrls.size} CRL URLs failed to check")
    Log.w(tag, "Revocation status: UNKNOWN (unable to verify)")
    Log.d(tag, "========================================")
    return Pair(null, null)
}
