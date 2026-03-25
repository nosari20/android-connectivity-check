package com.nosari20.connectivitytest.utils

import android.content.Context
import android.net.Uri
import com.nosari20.connectivitytest.Configuration
import com.nosari20.connectivitytest.ConnectivityTest
import java.io.*

/**
 * Utility class for importing and exporting connectivity test configurations
 */
object ConfigImportExport {

    /**
     * Import configuration from a file URI
     * @param context Android context
     * @param uri URI of the file to import
     * @return Result with success flag and message
     */
    fun importConfig(context: Context, uri: Uri): ImportResult {
        try {
            val pfd = context.contentResolver.openFileDescriptor(uri, "r")
                ?: return ImportResult(
                    success = false,
                    message = "Could not open file descriptor"
                )

            pfd.use {
                FileInputStream(it.fileDescriptor).use { fis ->
                    val stringBuilder = StringBuilder()
                    val lineList = mutableListOf<String>()

                    fis.bufferedReader().forEachLine { line ->
                        lineList.add(line)
                    }
                    lineList.forEach { stringBuilder.append(it) }

                    return try {
                        Configuration.loadSerializedConfigurations(stringBuilder.toString())
                        ImportResult(
                            success = true,
                            message = "Configuration imported successfully",
                            testsImported = parseTestCount(stringBuilder.toString())
                        )
                    } catch (e: NumberFormatException) {
                        ImportResult(
                            success = false,
                            message = "Bad format. Expected format: hostname,port,ssl,certAlias,enableCrlCheck",
                            error = e
                        )
                    } catch (e: Exception) {
                        ImportResult(
                            success = false,
                            message = "Unknown error: ${e.message}",
                            error = e
                        )
                    }
                }
            }
        } catch (e: FileNotFoundException) {
            return ImportResult(
                success = false,
                message = "File not found: ${uri.path}",
                error = e
            )
        } catch (e: IOException) {
            return ImportResult(
                success = false,
                message = "IO Exception: ${e.message}",
                error = e
            )
        }
    }

    /**
     * Export configuration to a file URI
     * @param context Android context
     * @param uri URI of the file to export to
     * @return Result with success flag and message
     */
    fun exportConfig(context: Context, uri: Uri): ExportResult {
        try {
            val configString = Configuration.serializeConfig()

            if (configString.isEmpty()) {
                return ExportResult(
                    success = false,
                    message = "No tests to export"
                )
            }

            val pfd = context.contentResolver.openFileDescriptor(uri, "w")
                ?: return ExportResult(
                    success = false,
                    message = "Could not open file descriptor"
                )

            pfd.use {
                FileOutputStream(it.fileDescriptor).use { fos ->
                    fos.write(configString.toByteArray())
                    return ExportResult(
                        success = true,
                        message = "Configuration saved to ${uri.path}",
                        testsExported = parseTestCount(configString)
                    )
                }
            }
        } catch (e: FileNotFoundException) {
            return ExportResult(
                success = false,
                message = "File not found: ${uri.path}",
                error = e
            )
        } catch (e: IOException) {
            return ExportResult(
                success = false,
                message = "IO Exception: ${e.message}",
                error = e
            )
        }
    }

    /**
     * Serialize tests to string format
     * @param tests List of connectivity tests
     * @return Serialized string in CSV-like format
     */
    fun serializeTests(tests: List<ConnectivityTest>): String {
        val stringBuilder = StringBuilder()
        tests.forEach { test ->
            stringBuilder.append("${test.host},${test.port},${test.ssl},${test.certAlias},${test.enableCrlCheck};")
        }
        return stringBuilder.toString()
    }

    /**
     * Deserialize tests from string format
     * @param configString Serialized configuration string
     * @return List of connectivity tests
     * @throws NumberFormatException if format is invalid
     */
    fun deserializeTests(configString: String): List<ConnectivityTest> {
        val configList = arrayListOf<ConnectivityTest>()

        if (configString.isEmpty()) {
            return configList
        }

        val testList = configString.split(";")
        for (testString in testList) {
            if (testString.trim().isEmpty()) continue

            val test = testString.split(",")

            when (test.size) {
                4 -> {
                    // Old format without enableCrlCheck (backward compatibility)
                    configList.add(
                        ConnectivityTest(
                            host = test[0],
                            port = test[1].toInt(),
                            ssl = test[2].equals("true", ignoreCase = true),
                            certAlias = test[3],
                            enableCrlCheck = false  // Default to false for old configs
                        )
                    )
                }
                5 -> {
                    // New format with enableCrlCheck
                    configList.add(
                        ConnectivityTest(
                            host = test[0],
                            port = test[1].toInt(),
                            ssl = test[2].equals("true", ignoreCase = true),
                            certAlias = test[3],
                            enableCrlCheck = test[4].equals("true", ignoreCase = true)
                        )
                    )
                }
                else -> {
                    // Skip invalid entries
                    continue
                }
            }
        }

        return configList
    }

    /**
     * Parse test count from config string
     */
    private fun parseTestCount(configString: String): Int {
        if (configString.isEmpty()) return 0
        return configString.split(";")
            .filter { it.trim().isNotEmpty() }
            .count()
    }

    /**
     * Validate config string format
     * @param configString Configuration string to validate
     * @return ValidationResult with success flag and details
     */
    fun validateConfig(configString: String): ValidationResult {
        if (configString.isEmpty()) {
            return ValidationResult(
                isValid = false,
                message = "Configuration is empty"
            )
        }

        try {
            val tests = deserializeTests(configString)
            return ValidationResult(
                isValid = true,
                message = "Configuration is valid",
                testCount = tests.size
            )
        } catch (e: Exception) {
            return ValidationResult(
                isValid = false,
                message = "Invalid configuration format: ${e.message}",
                error = e
            )
        }
    }

    /**
     * Get configuration info without loading it
     * @param context Android context
     * @param uri URI of the file to read
     * @return ConfigInfo with details about the configuration
     */
    fun getConfigInfo(context: Context, uri: Uri): ConfigInfo {
        try {
            val pfd = context.contentResolver.openFileDescriptor(uri, "r")
                ?: return ConfigInfo(
                    isValid = false,
                    message = "Could not read file"
                )

            pfd.use {
                FileInputStream(it.fileDescriptor).use { fis ->
                    val configString = fis.bufferedReader().use { it.readText() }
                    val validation = validateConfig(configString)

                    return ConfigInfo(
                        isValid = validation.isValid,
                        testCount = validation.testCount,
                        fileSize = configString.length.toLong(),
                        message = validation.message
                    )
                }
            }
        } catch (e: Exception) {
            return ConfigInfo(
                isValid = false,
                message = "Error reading file: ${e.message}"
            )
        }
    }
}

/**
 * Result of an import operation
 */
data class ImportResult(
    val success: Boolean,
    val message: String,
    val testsImported: Int = 0,
    val error: Throwable? = null
)

/**
 * Result of an export operation
 */
data class ExportResult(
    val success: Boolean,
    val message: String,
    val testsExported: Int = 0,
    val error: Throwable? = null
)

/**
 * Result of a validation operation
 */
data class ValidationResult(
    val isValid: Boolean,
    val message: String,
    val testCount: Int = 0,
    val error: Throwable? = null
)

/**
 * Information about a configuration file
 */
data class ConfigInfo(
    val isValid: Boolean,
    val testCount: Int = 0,
    val fileSize: Long = 0,
    val message: String
)

