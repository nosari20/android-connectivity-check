package com.nosari20.connectivitytest.ui.compose

import androidx.activity.compose.BackHandler
import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nosari20.connectivitytest.ConnectivityTest
import com.nosari20.connectivitytest.ui.theme.StatusError
import com.nosari20.connectivitytest.ui.theme.StatusOk
import com.nosari20.connectivitytest.utils.CertificateInfo
import com.nosari20.connectivitytest.utils.formatDate
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TestDetailsScreen(
    test: ConnectivityTest,
    onBack: () -> Unit,
    onRunTest: (ConnectivityTest) -> Unit
) {
    val details = test.connectionDetails

    // Predictive back gesture - scale animation during swipe
    val scale = remember { Animatable(1f) }

    PredictiveBackHandler { progress ->
        try {
            progress.collect { backEvent ->
                // Scale down as user swipes (1.0 to 0.9)
                scale.snapTo(1f - (backEvent.progress * 0.1f))
            }
            // Animation complete - trigger actual back navigation
            scale.animateTo(
                targetValue = 0.85f,
                animationSpec = tween(durationMillis = 200, easing = LinearEasing)
            )
            onBack()
        } catch (e: Exception) {
            // Gesture cancelled - spring back to normal size
            scale.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 200)
            )
        }
    }

    Scaffold(
        modifier = Modifier.graphicsLayer {
            scaleX = scale.value
            scaleY = scale.value
        },
        topBar = {
            TopAppBar(
                title = { Text("Test Details") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        floatingActionButton = {
            if (test.status == ConnectivityTest.Status.PENDING) {
                FloatingActionButton(
                    onClick = { },
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                }
            } else {
                ExtendedFloatingActionButton(
                    text = { Text("Run Test") },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Run Test"
                        )
                    },
                    onClick = {
                        test.status = ConnectivityTest.Status.PENDING
                        onRunTest(test)
                    },
                    containerColor = MaterialTheme.colorScheme.primary,
                    expanded = true
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Status Card
            StatusCard(test)

            // SSL Error Warnings (prominent display for failures)
            if (test.status == ConnectivityTest.Status.KO && test.ssl) {
                SSLErrorWarnings(test, details)
            }

            // Connection Details Card
            ConnectionDetailsCard(test, details)

            // SSL/TLS Information (only for SSL connections)
            if (test.ssl && details != null) {
                // Protocol & Cipher Suite
                if (details.protocol.isNotEmpty()) {
                    ProtocolCard(details)
                }

                // Server Certificate Chain
                if (details.serverCertificates.isNotEmpty()) {
                    ServerCertificatesCard(details.serverCertificates)
                }

                // Client Certificate (if used)
                details.clientCertificate?.let { clientCert ->
                    ClientCertificateCard(clientCert)
                }
            }

            // Test Results Card
            if (test.info.isNotEmpty()) {
                TestResultsCard(test.info)
            }

            // Error Details (if any)
            details?.error?.let { error ->
                ErrorDetailsCard(error)
            }

            // Technical Information Card
            TechnicalInfoCard(test, details)
        }
    }
}

@Composable
private fun StatusCard(test: ConnectivityTest) {
    val containerColor = when (test.status) {
        ConnectivityTest.Status.OK -> if (isSystemInDarkTheme()) {
            Color(0xFF1B5E20).copy(alpha = 0.3f)
        } else {
            Color(0xFFC8E6C9)
        }
        ConnectivityTest.Status.KO -> if (isSystemInDarkTheme()) {
            Color(0xFFB71C1C).copy(alpha = 0.3f)
        } else {
            Color(0xFFFFCDD2)
        }
        else -> MaterialTheme.colorScheme.surfaceVariant
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = containerColor
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatusIcon(
                status = test.status,
                modifier = Modifier.size(64.dp)
            )
            Text(
                text = when (test.status) {
                    ConnectivityTest.Status.OK -> "Connection Successful"
                    ConnectivityTest.Status.KO -> "Connection Failed"
                    ConnectivityTest.Status.PENDING -> "Testing..."
                    ConnectivityTest.Status.UNKNOWN -> "Not Tested"
                },
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun ConnectionDetailsCard(test: ConnectivityTest, details: com.nosari20.connectivitytest.utils.ConnectionDetails?) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SectionHeader("Connection Details")

            DetailRow(
                icon = Icons.Default.Language,
                label = "Hostname",
                value = test.host
            )

            DetailRow(
                icon = Icons.Default.Settings,
                label = "Port",
                value = test.port.toString()
            )

            details?.ipAddress?.let { ip ->
                if (ip.isNotEmpty()) {
                    DetailRow(
                        icon = Icons.Default.Router,
                        label = "IP Address",
                        value = ip
                    )
                }
            }

            details?.responseTimeMs?.let { time ->
                if (time > 0) {
                    DetailRow(
                        icon = Icons.Default.Timer,
                        label = "Response Time",
                        value = "${time}ms"
                    )
                }
            }

            DetailRow(
                icon = if (test.ssl) Icons.Default.Lock else Icons.Default.LockOpen,
                label = "Protocol",
                value = if (test.ssl) "SSL/TLS" else "TCP"
            )

            // Hostname verification details
            if (test.ssl && details != null) {
                details.hostnameVerificationDetails?.let { verificationDetails ->
                    if (verificationDetails.isNotEmpty()) {
                        DetailRow(
                            icon = if (details.hostnameVerified) Icons.Default.CheckCircle else Icons.Default.Error,
                            label = "Hostname Verification",
                            value = verificationDetails,
                            valueColor = if (details.hostnameVerified)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            // CRL status
            if (test.ssl && details != null && details.crlChecked) {
                details.crlStatus?.let { status ->
                    if (status.isNotEmpty()) {
                        val isRevoked = status.contains("REVOKED", ignoreCase = true)
                        DetailRow(
                            icon = if (isRevoked) Icons.Default.Warning else Icons.Default.Verified,
                            label = "CRL Status",
                            value = status,
                            valueColor = if (isRevoked)
                                MaterialTheme.colorScheme.error
                            else
                                MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            if (test.ssl && test.certAlias.isNotEmpty() && test.certAlias != "null") {
                DetailRow(
                    icon = Icons.Default.Security,
                    label = "Client Certificate Alias",
                    value = test.certAlias
                )
            }
        }
    }
}

@Composable
private fun ProtocolCard(details: com.nosari20.connectivitytest.utils.ConnectionDetails) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SectionHeader("SSL/TLS Protocol Information")

            DetailRow(
                icon = Icons.Default.Shield,
                label = "Protocol Version",
                value = details.protocol
            )

            DetailRow(
                icon = Icons.Default.VpnKey,
                label = "Cipher Suite",
                value = details.cipherSuite
            )

            DetailRow(
                icon = if (details.hostnameVerified) Icons.Default.CheckCircle else Icons.Default.Error,
                label = "Hostname Verification",
                value = if (details.hostnameVerified) "✓ Verified" else "✗ Failed"
            )

            if (details.peerHost.isNotEmpty()) {
                DetailRow(
                    icon = Icons.Default.Dns,
                    label = "Peer Host",
                    value = "${details.peerHost}:${details.peerPort}"
                )
            }
        }
    }
}

@Composable
private fun ServerCertificatesCard(certificates: List<CertificateInfo>) {
    var expandedCert by remember { mutableIntStateOf(-1) }

    // Detect cross-signed certificates
    val crossSignedIndices = remember(certificates) {
        com.nosari20.connectivitytest.utils.detectCrossSignedCertificates(certificates)
    }

    val crossSigningInfo = remember(certificates) {
        com.nosari20.connectivitytest.utils.getCrossSigningInfo(certificates)
    }

    val crossSigningPattern = remember(certificates) {
        com.nosari20.connectivitytest.utils.analyzeCrossSigningPattern(certificates)
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SectionHeader("Server Certificate Chain (${certificates.size} certificates)")

            // Show cross-signing alert if detected
            if (crossSignedIndices.isNotEmpty()) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.tertiaryContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onTertiaryContainer,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "Cross-Signed Certificates Detected",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        }

                        crossSigningPattern?.let { pattern ->
                            Text(
                                text = pattern,
                                style = MaterialTheme.typography.bodyMedium,
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        }

                        Text(
                            text = "This is common during CA transitions and indicates the same certificate is trusted by multiple certificate authorities.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f)
                        )
                    }
                }
            }

            certificates.forEachIndexed { index, cert ->
                val isCrossSigned = index in crossSignedIndices
                val crossSignPartners = crossSigningInfo[index]

                CertificateCard(
                    cert = cert,
                    title = when (index) {
                        0 -> "Server Certificate"
                        certificates.size - 1 -> "Root CA Certificate"
                        else -> "Intermediate CA Certificate #${index}"
                    },
                    isExpanded = expandedCert == index,
                    onToggleExpand = {
                        expandedCert = if (expandedCert == index) -1 else index
                    },
                    isCrossSigned = isCrossSigned,
                    crossSignPartners = crossSignPartners
                )
            }
        }
    }
}

@Composable
private fun ClientCertificateCard(cert: CertificateInfo) {
    var isExpanded by remember { mutableStateOf(false) }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SectionHeader("Client Certificate (mTLS)")

            CertificateCard(
                cert = cert,
                title = "Client Certificate",
                isExpanded = isExpanded,
                onToggleExpand = { isExpanded = !isExpanded }
            )
        }
    }
}

@Composable
private fun CertificateCard(
    cert: CertificateInfo,
    title: String,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    isCrossSigned: Boolean = false,
    crossSignPartners: List<Int>? = null
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header with expand button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    // Show cross-signing badge
                    if (isCrossSigned) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.tertiaryContainer
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Link,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onTertiaryContainer,
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    text = "Cross-Signed (${(crossSignPartners?.size ?: 0) + 1} CAs)",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }

                IconButton(onClick = onToggleExpand) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (isExpanded) "Collapse" else "Expand"
                    )
                }
            }

            // Basic Info (always shown)
            Spacer(modifier = Modifier.height(8.dp))

            MonoText("Subject: ${extractCN(cert.subjectDN)}")
            MonoText("Issuer: ${extractCN(cert.issuerDN)}")

            val now = Date()
            val isValid = now.after(cert.validFrom) && now.before(cert.validTo)
            MonoText(
                "Valid: ${if (isValid) "✓" else "✗"} ${formatDate(cert.validFrom)} to ${formatDate(cert.validTo)}",
                color = if (isValid) StatusOk else StatusError
            )

            // Show REVOKED warning if certificate is revoked
            if (cert.isRevoked == true) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.errorContainer,
                    border = BorderStroke(2.dp, MaterialTheme.colorScheme.error)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(24.dp)
                        )
                        Column {
                            Text(
                                text = "⚠ CERTIFICATE REVOKED",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.error
                            )
                            cert.revocationReason?.let { reason ->
                                Text(
                                    text = "Reason: $reason",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                    }
                }
            }

            // Show self-signed indicator
            if (cert.isSelfSigned) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Self-Signed Certificate",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }
            }

            // Detailed Info (shown when expanded)
            if (isExpanded) {
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(12.dp))

                CertDetailSection("Full Subject DN", cert.subjectDN)
                CertDetailSection("Full Issuer DN", cert.issuerDN)
                CertDetailSection("Serial Number", cert.serialNumber)
                CertDetailSection("Signature Algorithm", cert.signatureAlgorithm)
                CertDetailSection("Public Key Algorithm", cert.publicKeyAlgorithm)
                CertDetailSection("Version", "v${cert.version}")

                // Show public key hash for cross-signing verification
                if (cert.publicKeyHash.isNotEmpty() && isCrossSigned) {
                    CertDetailSection(
                        "Public Key Hash (SHA-256)",
                        cert.publicKeyHash.chunked(64).joinToString("\n")
                    )
                }

                if (cert.subjectAlternativeNames.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Subject Alternative Names:",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                    cert.subjectAlternativeNames.forEach { san ->
                        MonoText("  • $san")
                    }
                }

                // CRL Distribution Points and Revocation Status
                if (cert.crlDistributionPoints.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "CRL Distribution Points:",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                    cert.crlDistributionPoints.forEach { url ->
                        MonoText("  • $url")
                    }

                    // Show revocation status if checked
                    if (cert.isRevoked != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (cert.isRevoked == true) Icons.Default.Warning else Icons.Default.Verified,
                                contentDescription = null,
                                tint = if (cert.isRevoked == true) StatusError else StatusOk,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = if (cert.isRevoked == true) {
                                    "⚠ REVOKED: ${cert.revocationReason ?: "Unknown reason"}"
                                } else {
                                    "✓ Not Revoked (CRL checked)"
                                },
                                style = MaterialTheme.typography.labelMedium,
                                color = if (cert.isRevoked == true) StatusError else StatusOk,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                if (cert.keyUsage.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Key Usage:",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                    cert.keyUsage.forEach { usage ->
                        MonoText("  • $usage")
                    }
                }

                if (cert.extendedKeyUsage.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Extended Key Usage:",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                    cert.extendedKeyUsage.forEach { usage ->
                        MonoText("  • $usage")
                    }
                }
            }
        }
    }
}

@Composable
private fun CertDetailSection(label: String, value: String) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(
            text = "$label:",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold
        )
        MonoText(value)
    }
}

@Composable
private fun TestResultsCard(info: String) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SectionHeader("Test Results")

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Text(
                    text = info,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(16.dp),
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

@Composable
private fun ErrorDetailsCard(error: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = StatusError.copy(alpha = 0.1f)
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SectionHeader("Error Details")

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.errorContainer
            ) {
                Text(
                    text = error,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(16.dp),
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }
    }
}

@Composable
private fun TechnicalInfoCard(test: ConnectivityTest, details: com.nosari20.connectivitytest.utils.ConnectionDetails?) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SectionHeader("Technical Information")

            DetailRow(
                icon = Icons.Default.Info,
                label = "Connection URL",
                value = "${if (test.ssl) "https" else "http"}://${test.host}:${test.port}"
            )

            DetailRow(
                icon = Icons.Default.Check,
                label = "Verification",
                value = if (test.ssl) "Certificate validation enabled" else "No encryption"
            )

            if (test.ssl) {
                DetailRow(
                    icon = Icons.Default.Shield,
                    label = "Security Level",
                    value = if (test.certAlias.isNotEmpty() && test.certAlias != "null") {
                        "Mutual TLS (mTLS)"
                    } else {
                        "Server Authentication"
                    }
                )
            }
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary
    )
}

@Composable
private fun DetailRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = valueColor
            )
        }
    }
}

@Composable
private fun MonoText(text: String, color: Color = Color.Unspecified) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        fontFamily = FontFamily.Monospace,
        color = color
    )
}

private fun extractCN(dn: String): String {
    return dn.split(",").find { it.trim().startsWith("CN=") }?.substringAfter("CN=")?.trim() ?: dn.take(50)
}

@Composable
private fun SSLErrorWarnings(test: ConnectivityTest, details: com.nosari20.connectivitytest.utils.ConnectionDetails?) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Hostname Verification Failure
        if (details?.hostnameVerified == false) {
            SSLErrorWarningBox(
                icon = Icons.Default.Error,
                title = "Hostname Verification Failed",
                message = details.hostnameVerificationDetails ?: "Certificate hostname does not match",
                errorType = "HOSTNAME_MISMATCH"
            )
        }

        // Certificate Revoked
        if (details?.serverCertificates?.any { it.isRevoked == true } == true) {
            val revokedCert = details.serverCertificates.firstOrNull { it.isRevoked == true }
            SSLErrorWarningBox(
                icon = Icons.Default.Warning,
                title = "Certificate Revoked",
                message = revokedCert?.revocationReason?.let { "Reason: $it" } ?: "Certificate has been revoked by the CA",
                errorType = "REVOKED"
            )
        }

        // Untrusted Certificate / Certificate Chain Issues
        val errorInfo = test.info.lowercase()
        when {
            errorInfo.contains("trust anchor") || errorInfo.contains("untrusted") -> {
                SSLErrorWarningBox(
                    icon = Icons.Default.Block,
                    title = "Untrusted Certificate",
                    message = "Certificate is not trusted by the system. It may be self-signed or issued by an unknown CA.",
                    errorType = "UNTRUSTED"
                )
            }
            errorInfo.contains("expired") -> {
                SSLErrorWarningBox(
                    icon = Icons.Default.EventBusy,
                    title = "Certificate Expired",
                    message = "The certificate validity period has ended.",
                    errorType = "EXPIRED"
                )
            }
            errorInfo.contains("handshake") && !errorInfo.contains("hostname") && !errorInfo.contains("revoked") -> {
                SSLErrorWarningBox(
                    icon = Icons.Default.Error,
                    title = "SSL Handshake Failed",
                    message = "Failed to establish a secure connection. Check cipher suites and protocol versions.",
                    errorType = "HANDSHAKE"
                )
            }
            errorInfo.contains("certificate") && details?.serverCertificates?.isEmpty() != false -> {
                SSLErrorWarningBox(
                    icon = Icons.Default.Warning,
                    title = "Certificate Error",
                    message = test.info,
                    errorType = "CERTIFICATE"
                )
            }
        }
    }
}

@Composable
private fun SSLErrorWarningBox(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    message: String,
    errorType: String
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.errorContainer,
        border = BorderStroke(2.dp, MaterialTheme.colorScheme.error),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(28.dp)
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "⚠",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.error
                    )
                    Text(
                        text = title.uppercase(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }
    }
}
