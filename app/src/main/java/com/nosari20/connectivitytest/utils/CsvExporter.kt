package com.nosari20.connectivitytest.utils

import android.content.Context
import android.net.Uri
import com.nosari20.connectivitytest.ConnectivityTest
import java.text.SimpleDateFormat
import java.util.*

/**
 * Exports connectivity test results to CSV format
 */
object CsvExporter {

    /**
     * Generates CSV content from test results
     */
    fun generateCsv(
        customTests: List<ConnectivityTest>,
        appConfigTests: List<ConnectivityTest>,
        androidTests: List<ConnectivityTest>
    ): String {
        val csvBuilder = StringBuilder()

        // CSV Header
        csvBuilder.appendLine("Category,Host,Port,Protocol,Status,Response Time (ms),IP Address,Certificate Info,Error Details,Test Date")

        // Export Custom Tests
        customTests.forEach { test ->
            csvBuilder.appendLine(formatTestToCsv("Custom", test))
        }

        // Export AppConfig Tests
        appConfigTests.forEach { test ->
            csvBuilder.appendLine(formatTestToCsv("AppConfig", test))
        }

        // Export Android Tests
        androidTests.forEach { test ->
            csvBuilder.appendLine(formatTestToCsv("Android", test))
        }

        return csvBuilder.toString()
    }

    /**
     * Formats a single test result to CSV row
     */
    private fun formatTestToCsv(category: String, test: ConnectivityTest): String {
        val details = test.connectionDetails
        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())

        val status = when (test.status) {
            ConnectivityTest.Status.OK -> "SUCCESS"
            ConnectivityTest.Status.KO -> "FAILED"
            ConnectivityTest.Status.PENDING -> "TESTING"
            ConnectivityTest.Status.UNKNOWN -> "NOT_TESTED"
        }

        val protocol = if (test.ssl) {
            if (test.certAlias.isNotEmpty() && test.certAlias != "null") {
                "SSL/TLS (mTLS)"
            } else {
                "SSL/TLS"
            }
        } else {
            "TCP"
        }

        val responseTime = details?.responseTimeMs?.toString() ?: ""
        val ipAddress = details?.ipAddress ?: ""

        val certInfo = if (test.ssl && details != null) {
            buildString {
                if (details.protocol.isNotEmpty()) {
                    append("Protocol: ${details.protocol}; ")
                }
                if (details.cipherSuite.isNotEmpty()) {
                    append("Cipher: ${details.cipherSuite}; ")
                }
                if (details.serverCertificates.isNotEmpty()) {
                    val serverCert = details.serverCertificates.first()
                    append("Server: ${extractCN(serverCert.subjectDN)}; ")
                    append("Valid: ${if (isValidCert(serverCert)) "Yes" else "No"}; ")
                }
                val clientCert = details.clientCertificate
                if (clientCert != null) {
                    append("Client Cert: ${extractCN(clientCert.subjectDN)}; ")
                }
            }
        } else {
            ""
        }

        val errorDetails = if (test.status == ConnectivityTest.Status.KO) {
            test.info.replace("\"", "\"\"").replace("\n", " | ")
        } else {
            ""
        }

        // Escape and format fields
        return buildString {
            append(escapeField(category))
            append(",")
            append(escapeField(test.host))
            append(",")
            append(test.port)
            append(",")
            append(escapeField(protocol))
            append(",")
            append(status)
            append(",")
            append(responseTime)
            append(",")
            append(escapeField(ipAddress))
            append(",")
            append(escapeField(certInfo))
            append(",")
            append(escapeField(errorDetails))
            append(",")
            append(timestamp)
        }
    }

    /**
     * Escapes CSV field (handles quotes and commas)
     */
    private fun escapeField(field: String): String {
        return if (field.contains(",") || field.contains("\"") || field.contains("\n")) {
            "\"${field.replace("\"", "\"\"")}\""
        } else {
            field
        }
    }

    /**
     * Extracts Common Name from Distinguished Name
     */
    private fun extractCN(dn: String): String {
        return dn.split(",")
            .find { it.trim().startsWith("CN=") }
            ?.substringAfter("CN=")
            ?.trim()
            ?: dn.take(50)
    }

    /**
     * Checks if certificate is currently valid
     */
    private fun isValidCert(cert: CertificateInfo): Boolean {
        val now = Date()
        return now.after(cert.validFrom) && now.before(cert.validTo)
    }

    /**
     * Exports CSV to file
     */
    fun exportToFile(
        context: Context,
        uri: Uri,
        csvContent: String
    ): Boolean {
        return try {
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                outputStream.write(csvContent.toByteArray())
                outputStream.flush()
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Generates filename with timestamp
     */
    fun generateFilename(): String {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        return "connectivity_tests_$timestamp.csv"
    }

    /**
     * Creates a temporary CSV file for sharing
     * Returns the file Uri for use with share intent
     */
    fun createShareableCsv(
        context: Context,
        csvContent: String
    ): Uri? {
        return try {
            // Create temp file in cache directory
            val cacheDir = context.cacheDir
            val tempFile = java.io.File(cacheDir, generateFilename())

            // Write CSV content
            tempFile.writeText(csvContent)

            // Create content URI using FileProvider
            androidx.core.content.FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                tempFile
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}




