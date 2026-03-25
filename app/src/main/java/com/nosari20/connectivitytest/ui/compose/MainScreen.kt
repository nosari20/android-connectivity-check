package com.nosari20.connectivitytest.ui.compose

import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nosari20.connectivitytest.ConnectivityTest
import com.nosari20.connectivitytest.utils.checkSsl
import com.nosari20.connectivitytest.utils.checkTcp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch

data class TestTab(
    val title: String,
    val tests: List<ConnectivityTest>,
    val onClick: ((ConnectivityTest) -> Unit)?,
    val emptyMessage: String? = null  // Optional message to show when list is empty
)

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
@Composable
fun MainScreen(
    tabs: List<TestTab>,
    onAddTestClick: () -> Unit,
    onImport: () -> Unit = {},
    onExport: () -> Unit = {},
    onExportCsv: () -> Unit = {},
    onShareCsv: () -> Unit = {},
    onSaveTabIndex: (Int) -> Unit = {},
    initialTabIndex: Int = 0,
    modifier: Modifier = Modifier
) {
    val pagerState = rememberPagerState(
        initialPage = initialTabIndex,
        pageCount = { tabs.size }
    )
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    // State for expandable FAB
    var fabExpanded by remember { mutableStateOf(false) }

    // Save the current tab index when it changes
    LaunchedEffect(pagerState.currentPage) {
        onSaveTabIndex(pagerState.currentPage)
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Connectivity Tests") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        floatingActionButton = {
            ExpandableFab(
                expanded = fabExpanded,
                onExpandChange = { fabExpanded = it },
                onAddTest = {
                    fabExpanded = false
                    onAddTestClick()
                },
                onImport = {
                    fabExpanded = false
                    onImport()
                },
                onExport = {
                    fabExpanded = false
                    onExport()
                },
                onExportCsv = {
                    fabExpanded = false
                    onExportCsv()
                },
                onShareCsv = {
                    fabExpanded = false
                    onShareCsv()
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Tab Row
            PrimaryTabRow(
                selectedTabIndex = pagerState.currentPage,
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ) {
                tabs.forEachIndexed { index, tab ->
                    Tab(
                        selected = pagerState.currentPage == index,
                        onClick = {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(index)
                            }
                        },
                        text = { Text(tab.title) }
                    )
                }
            }

            // Pager with test lists
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                val tab = tabs[page]
                var isRefreshing by remember { mutableStateOf(false) }

                val pullRefreshState = rememberPullRefreshState(
                    refreshing = isRefreshing,
                    onRefresh = {
                        isRefreshing = true
                        // Set all tests to PENDING first
                        tab.tests.forEach { test ->
                            test.status = ConnectivityTest.Status.PENDING
                        }

                        // Run tests in parallel
                        coroutineScope.launch {
                            val jobs = tab.tests.map { test ->
                                launch(Dispatchers.IO) {
                                    if (test.ssl) {
                                        checkSsl(test, context)
                                    } else {
                                        checkTcp(test)
                                    }
                                }
                            }
                            // Wait for all tests to complete
                            jobs.joinAll()
                            isRefreshing = false
                        }
                    }
                )

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pullRefresh(pullRefreshState)
                ) {
                    // No need for key since ConnectivityTest now uses mutableStateOf
                    ConnectivityTestList(
                        tests = tab.tests,
                        onClick = tab.onClick,
                        emptyMessage = tab.emptyMessage  // Pass empty message to list
                    )

                    PullRefreshIndicator(
                        refreshing = isRefreshing,
                        state = pullRefreshState,
                        modifier = Modifier.align(Alignment.TopCenter)
                    )
                }
            }
        }
    }
}

@Composable
private fun ExpandableFab(
    expanded: Boolean,
    onExpandChange: (Boolean) -> Unit,
    onAddTest: () -> Unit,
    onImport: () -> Unit,
    onExport: () -> Unit,
    onExportCsv: () -> Unit,
    onShareCsv: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Mini FAB options (shown when expanded)
        androidx.compose.animation.AnimatedVisibility(
            visible = expanded,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Add Test
                MiniFabItem(
                    icon = Icons.Default.Add,
                    label = "Add Test",
                    onClick = onAddTest,
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )

                // Import Config
                MiniFabItem(
                    icon = Icons.Default.Upload,
                    label = "Import Config",
                    onClick = onImport,
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer
                )

                // Export Config
                MiniFabItem(
                    icon = Icons.Default.Download,
                    label = "Export Config",
                    onClick = onExport,
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer
                )

                // Export CSV
                MiniFabItem(
                    icon = Icons.Default.Assessment,
                    label = "Export CSV",
                    onClick = onExportCsv,
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )

                // Share CSV
                MiniFabItem(
                    icon = Icons.Default.Share,
                    label = "Share CSV",
                    onClick = onShareCsv,
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            }
        }

        // Main FAB
        FloatingActionButton(
            onClick = { onExpandChange(!expanded) },
            containerColor = MaterialTheme.colorScheme.primary
        ) {
            Icon(
                imageVector = if (expanded) Icons.Default.Close else Icons.Default.Menu,
                contentDescription = if (expanded) "Close" else "Actions",
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
private fun MiniFabItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
    containerColor: androidx.compose.ui.graphics.Color
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Surface(
            shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp,
            shadowElevation = 2.dp
        ) {
            Text(
                text = label,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Medium
            )
        }

        SmallFloatingActionButton(
            onClick = onClick,
            containerColor = containerColor
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}








