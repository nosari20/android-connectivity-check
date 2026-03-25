package com.nosari20.connectivitytest.ui.compose

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nosari20.connectivitytest.ConnectivityTest
import com.nosari20.connectivitytest.ui.theme.StatusError
import com.nosari20.connectivitytest.ui.theme.StatusOk
import com.nosari20.connectivitytest.ui.theme.StatusUnknown

@Composable
fun ConnectivityTestList(
    tests: List<ConnectivityTest>,
    onClick: ((ConnectivityTest) -> Unit)? = null,
    modifier: Modifier = Modifier,
    emptyMessage: String? = null  // Optional empty state message
) {
    if (tests.isEmpty() && emptyMessage != null) {
        // Show empty state with message
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.padding(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(64.dp)
                )
                Text(
                    text = emptyMessage,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
    } else {
        // Show list (even if empty without message)
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            items(tests, key = { test -> System.identityHashCode(test) }) { test ->
                ConnectivityTestItem(
                    test = test,
                    onClick = if (onClick != null) {
                        { onClick(test) }
                    } else null
                )
            }
        }
    }
}

@Composable
fun ConnectivityTestItem(
    test: ConnectivityTest,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        onClick = onClick ?: {}
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Status Icon
            StatusIcon(status = test.status)

            Spacer(modifier = Modifier.width(16.dp))

            // Test Info
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "${test.host}:${test.port}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )

                if (test.info.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = test.info,
                        style = MaterialTheme.typography.bodyMedium,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun StatusIcon(status: ConnectivityTest.Status, modifier: Modifier = Modifier) {
    when (status) {
        ConnectivityTest.Status.OK -> {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Success",
                tint = StatusOk,
                modifier = modifier.size(40.dp)
            )
        }
        ConnectivityTest.Status.KO -> {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Failed",
                tint = StatusError,
                modifier = modifier.size(40.dp)
            )
        }
        ConnectivityTest.Status.PENDING -> {
            val infiniteTransition = rememberInfiniteTransition(label = "loading")
            val rotation by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = 360f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1000, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart
                ),
                label = "rotation"
            )

            CircularProgressIndicator(
                modifier = modifier
                    .size(40.dp)
                    .rotate(rotation),
                strokeWidth = 4.dp
            )
        }
        ConnectivityTest.Status.UNKNOWN -> {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.HelpOutline,
                contentDescription = "Unknown",
                tint = StatusUnknown,
                modifier = modifier.size(40.dp)
            )
        }
    }
}

