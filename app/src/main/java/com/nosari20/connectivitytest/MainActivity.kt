package com.nosari20.connectivitytest

import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import com.nosari20.connectivitytest.ui.compose.AddTestDialog
import com.nosari20.connectivitytest.ui.compose.MainScreen
import com.nosari20.connectivitytest.ui.compose.TestActionsBottomSheet
import com.nosari20.connectivitytest.ui.compose.TestDetailsScreen
import com.nosari20.connectivitytest.ui.compose.TestTab
import com.nosari20.connectivitytest.ui.theme.ConnectivityTestTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.util.*

class MainActivity : ComponentActivity() {

    private val testList = Configuration
    private var localTests by mutableStateOf<List<ConnectivityTest>>(emptyList())
    private var managedTests by mutableStateOf<List<ConnectivityTest>>(emptyList())
    private var googleTests by mutableStateOf<List<ConnectivityTest>>(emptyList())
    private var showAddDialog by mutableStateOf(false)
    private var editingTest by mutableStateOf<ConnectivityTest?>(null)  // Test being edited
    private var editingTestIndex by mutableStateOf(-1)  // Index of test being edited
    private var selectedTest by mutableStateOf<ConnectivityTest?>(null)
    private var showTestActions by mutableStateOf(false)
    private var canDeleteSelectedTest by mutableStateOf(false)
    private var showTestDetails by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        testList.loadManagedConfigurations(applicationContext)
        testList.loadLocalConfigurations(this)

        // Load initial data
        loadTestLists()

        // Import config launcher
        val importConfigResultLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
            if (uri == null) return@registerForActivityResult

            try {
                contentResolver.openFileDescriptor(uri, "r")?.use {
                    FileInputStream(it.fileDescriptor).use { fis ->
                        val stringBuilder = StringBuilder()
                        val lineList = mutableListOf<String>()
                        fis.bufferedReader().forEachLine { line ->
                            lineList.add(line)
                        }
                        lineList.forEach { stringBuilder.append(it) }

                        try {
                            testList.loadSerializedConfigurations(stringBuilder.toString())
                            Toast.makeText(applicationContext, "Configuration imported successfully", Toast.LENGTH_SHORT).show()
                            loadTestLists()
                            testList.saveLocalConfigurations(this)
                            showAddDialog = false
                        } catch (e: NumberFormatException) {
                            Toast.makeText(applicationContext, "Bad format.", Toast.LENGTH_SHORT).show()
                        } catch (e: Exception) {
                            Toast.makeText(applicationContext, "Unknown error.", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            } catch (e: FileNotFoundException) {
                e.printStackTrace()
                Toast.makeText(applicationContext, "File not found " + uri.path.toString(), Toast.LENGTH_LONG).show()
            } catch (e: IOException) {
                e.printStackTrace()
                Toast.makeText(applicationContext, "IO Exception " + uri.path.toString(), Toast.LENGTH_LONG).show()
            }
        }

        // Export config launcher
        val exportConfigResultLauncher = registerForActivityResult(ActivityResultContracts.CreateDocument("text/plain")) { uri: Uri? ->
            if (uri == null) return@registerForActivityResult

            val result = com.nosari20.connectivitytest.utils.ConfigImportExport.exportConfig(
                context = applicationContext,
                uri = uri
            )

            if (result.success) {
                Toast.makeText(
                    applicationContext,
                    "${result.message} (${result.testsExported} tests)",
                    Toast.LENGTH_LONG
                ).show()
            } else {
                Toast.makeText(
                    applicationContext,
                    result.message,
                    Toast.LENGTH_LONG
                ).show()
                result.error?.printStackTrace()
            }

            showAddDialog = false
        }

        // CSV export launcher
        val exportCsvResultLauncher = registerForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri: Uri? ->
            if (uri == null) return@registerForActivityResult

            val csvContent = com.nosari20.connectivitytest.utils.CsvExporter.generateCsv(
                customTests = localTests,
                appConfigTests = managedTests,
                androidTests = googleTests
            )

            val success = com.nosari20.connectivitytest.utils.CsvExporter.exportToFile(
                context = applicationContext,
                uri = uri,
                csvContent = csvContent
            )

            if (success) {
                Toast.makeText(applicationContext, "CSV exported successfully", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(applicationContext, "Failed to export CSV", Toast.LENGTH_SHORT).show()
            }
            showAddDialog = false
        }

        // Function to share CSV via share sheet
        fun shareCsvResults() {
            val csvContent = com.nosari20.connectivitytest.utils.CsvExporter.generateCsv(
                customTests = localTests,
                appConfigTests = managedTests,
                androidTests = googleTests
            )

            val uri = com.nosari20.connectivitytest.utils.CsvExporter.createShareableCsv(
                context = applicationContext,
                csvContent = csvContent
            )

            if (uri != null) {
                val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                    type = "text/csv"
                    putExtra(android.content.Intent.EXTRA_STREAM, uri)
                    putExtra(android.content.Intent.EXTRA_SUBJECT, "Connectivity Test Results")
                    putExtra(android.content.Intent.EXTRA_TEXT, "Please find attached the connectivity test results in CSV format.")
                    addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }

                startActivity(android.content.Intent.createChooser(shareIntent, "Share CSV Results"))
                showAddDialog = false
            } else {
                Toast.makeText(applicationContext, "Failed to create shareable CSV", Toast.LENGTH_SHORT).show()
            }
        }

        setContent {
            ConnectivityTestTheme {
                // Use rememberSaveable to preserve tab index across configuration changes and process death
                var previousTabIndex by rememberSaveable { mutableIntStateOf(0) }

                // Surface to prevent flashing between screens
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AnimatedContent(
                    targetState = showTestDetails && selectedTest != null,
                    contentKey = { it }, // Use boolean as content key to prevent flashing
                    transitionSpec = {
                        if (targetState) {
                            // Opening details screen - slide in from right
                            slideInHorizontally(
                                initialOffsetX = { fullWidth -> fullWidth },
                                animationSpec = tween(300)
                            ) + fadeIn(animationSpec = tween(300)) togetherWith
                            slideOutHorizontally(
                                targetOffsetX = { fullWidth -> -fullWidth / 3 },
                                animationSpec = tween(300)
                            ) + fadeOut(animationSpec = tween(300))
                        } else {
                            // Closing details screen - slide out to right
                            slideInHorizontally(
                                initialOffsetX = { fullWidth -> -fullWidth / 3 },
                                animationSpec = tween(300)
                            ) + fadeIn(animationSpec = tween(300)) togetherWith
                            slideOutHorizontally(
                                targetOffsetX = { fullWidth -> fullWidth },
                                animationSpec = tween(300)
                            ) + fadeOut(animationSpec = tween(300))
                        }
                    },
                    label = "screen_transition"
                ) { showDetails ->
                    if (showDetails && selectedTest != null) {
                        // Show details screen
                        TestDetailsScreen(
                            test = selectedTest!!,
                            onBack = {
                                showTestDetails = false
                                selectedTest = null
                            },
                            onRunTest = { test ->
                                // Run the test in background
                                CoroutineScope(Dispatchers.IO).launch {
                                    if (test.ssl) {
                                        com.nosari20.connectivitytest.utils.checkSsl(test, applicationContext)
                                    } else {
                                        com.nosari20.connectivitytest.utils.checkTcp(test)
                                    }
                                }
                            }
                        )
                    } else {
                        // Show main screen
                        MainActivityContent(
                            localTests = localTests,
                            managedTests = managedTests,
                            googleTests = googleTests,
                            showAddDialog = showAddDialog,
                            editingTest = editingTest,  // Pass editing test
                            onAddTestClick = {
                                editingTest = null
                                editingTestIndex = -1
                                showAddDialog = true
                            },
                            onDismissDialog = {
                                showAddDialog = false
                                editingTest = null
                                editingTestIndex = -1
                            },
                            onAddTest = { hostname, port, ssl, certAlias, enableCrlCheck ->
                                if (editingTest != null && editingTestIndex >= 0) {
                                    // Edit mode - update existing test
                                    updateTest(editingTestIndex, hostname, port, ssl, certAlias, enableCrlCheck)
                                } else {
                                    // Add mode - create new test
                                    addNewTest(hostname, port, ssl, certAlias, enableCrlCheck)
                                }
                                showAddDialog = false
                                editingTest = null
                                editingTestIndex = -1
                            },
                            onImport = {
                                importConfigResultLauncher.launch(arrayOf("text/*"))
                            },
                            onExport = {
                                exportConfigResultLauncher.launch("connectivity-tests-${UUID.randomUUID().toString().substring(0, 8)}.txt")
                            },
                            onExportCsv = {
                                val filename = com.nosari20.connectivitytest.utils.CsvExporter.generateFilename()
                                exportCsvResultLauncher.launch(filename)
                            },
                            onShareCsv = {
                                shareCsvResults()
                            },
                            onTestPress = { test, canDelete ->
                                selectedTest = test
                                canDeleteSelectedTest = canDelete
                                showTestActions = true
                            },
                            onSaveTabIndex = { index ->
                                previousTabIndex = index
                            },
                            previousTabIndex = previousTabIndex
                        )

                        // Bottom sheet for test actions
                        if (showTestActions && selectedTest != null) {
                            TestActionsBottomSheet(
                                test = selectedTest!!,
                                canDelete = canDeleteSelectedTest,
                                onDismiss = {
                                    showTestActions = false
                                },
                                onViewDetails = {
                                    showTestActions = false
                                    showTestDetails = true
                                },
                                onEdit = if (canDeleteSelectedTest) {
                                    {
                                        // Find test index for editing
                                        val index = localTests.indexOf(selectedTest)
                                        if (index >= 0) {
                                            editingTest = selectedTest
                                            editingTestIndex = index
                                            showAddDialog = true
                                        }
                                        showTestActions = false
                                    }
                                } else null,  // Only custom tests can be edited
                                onDelete = {
                                    // Find and remove the test
                                    val index = localTests.indexOf(selectedTest)
                                    if (index >= 0) {
                                        removeTest(index)
                                    }
                                    showTestActions = false
                                    selectedTest = null
                                }
                            )
                        }
                    }
                }
            }
        }
    }
    }

    private fun loadTestLists() {
        localTests = testList.all()["local"] ?: emptyList()
        managedTests = testList.all()["managed"] ?: emptyList()
        googleTests = testList.all()["google"] ?: emptyList()
    }

    private fun addNewTest(hostname: String, port: Int, ssl: Boolean, certAlias: String, enableCrlCheck: Boolean) {
        val localTestsMutable = testList.all()["local"]?.toMutableList()
        if (localTestsMutable != null) {
            val test = ConnectivityTest(hostname, port, ssl, certAlias, enableCrlCheck)
            localTestsMutable.add(test)
            testList.update("local", localTestsMutable as ArrayList<ConnectivityTest>)
            testList.saveLocalConfigurations(this)
            loadTestLists()
            Toast.makeText(applicationContext, "New test added.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateTest(index: Int, hostname: String, port: Int, ssl: Boolean, certAlias: String, enableCrlCheck: Boolean) {
        val localTestsMutable = testList.all()["local"]?.toMutableList()
        if (localTestsMutable != null && index >= 0 && index < localTestsMutable.size) {
            val updatedTest = ConnectivityTest(hostname, port, ssl, certAlias, enableCrlCheck)
            localTestsMutable[index] = updatedTest
            testList.update("local", localTestsMutable as ArrayList<ConnectivityTest>)
            testList.saveLocalConfigurations(this)
            loadTestLists()
            Toast.makeText(applicationContext, "Test updated.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun removeTest(pos: Int) {
        val localTestsMutable = testList.all()["local"]?.toMutableList()
        localTestsMutable?.removeAt(pos)
        testList.update("local", localTestsMutable as ArrayList<ConnectivityTest>)
        testList.saveLocalConfigurations(this)
        loadTestLists()
    }
}

@Composable
fun MainActivityContent(
    localTests: List<ConnectivityTest>,
    managedTests: List<ConnectivityTest>,
    googleTests: List<ConnectivityTest>,
    showAddDialog: Boolean,
    editingTest: ConnectivityTest? = null,  // Add editing test parameter
    onAddTestClick: () -> Unit,
    onDismissDialog: () -> Unit,
    onAddTest: (String, Int, Boolean, String, Boolean) -> Unit,
    onImport: () -> Unit,
    onExport: () -> Unit,
    onExportCsv: () -> Unit,
    onShareCsv: () -> Unit,
    onTestPress: (ConnectivityTest, Boolean) -> Unit,
    onSaveTabIndex: (Int) -> Unit,
    previousTabIndex: Int
) {
    val tabs = remember(localTests, managedTests, googleTests) {
        listOf(
            TestTab(
                title = "Custom",
                tests = localTests,
                onClick = { test -> onTestPress(test, true) },
                emptyMessage = "No custom tests added yet.\n\nTap the + button below to add your first connectivity test."
            ),
            TestTab(
                title = "AppConfig",
                tests = managedTests,
                onClick = { test -> onTestPress(test, false) },
                emptyMessage = "No managed tests configured.\n\nTests can be deployed through Mobile Device Management (MDM) solutions using Android App Configuration."
            ),
            TestTab(
                title = "Android",
                tests = googleTests,
                onClick = { test -> onTestPress(test, false) }
            )
        )
    }

    MainScreen(
        tabs = tabs,
        onAddTestClick = onAddTestClick,
        onImport = onImport,
        onExport = onExport,
        onExportCsv = onExportCsv,
        onShareCsv = onShareCsv,
        onSaveTabIndex = onSaveTabIndex,
        initialTabIndex = previousTabIndex
    )

    if (showAddDialog) {
        AddTestDialog(
            onDismiss = onDismissDialog,
            onAddTest = onAddTest,
            editTest = editingTest  // Pass the test being edited
        )
    }
}
