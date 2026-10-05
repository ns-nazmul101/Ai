package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.DevilViewModel
import com.example.ui.theme.DevilAmber
import com.example.ui.theme.DevilCyan
import com.example.ui.theme.DevilGreen
import com.example.ui.theme.DevilRed
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun RootConsoleScreen(
    viewModel: DevilViewModel,
    modifier: Modifier = Modifier
) {
    val actionLogs by viewModel.actionLogs.collectAsStateWithLifecycle()
    val currentLanguage by viewModel.currentLanguage.collectAsStateWithLifecycle()
    val isBengali = currentLanguage.startsWith("bn", ignoreCase = true)

    var commandInput by remember { mutableStateOf("") }
    var executeAsRoot by remember { mutableStateOf(viewModel.rootExecutor.isRootBinaryPresent()) }
    var rootStatusMessage by remember { mutableStateOf<String?>(null) }
    var isCheckingRoot by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    val clipboardManager = LocalClipboardManager.current

    val quickPresets = listOf(
        "id",
        "getprop ro.build.version.release",
        "df -h /data",
        "free -m",
        "uptime",
        "uname -a",
        "pm list packages -3 | head -n 10"
    )

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(6.dp))
            // Title Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Terminal, contentDescription = "Terminal", tint = DevilRed)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isBengali) "রুট ও শেল কনসোল" else "Root & Shell Terminal",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                }

                if (actionLogs.isNotEmpty()) {
                    IconButton(
                        onClick = { viewModel.clearLogs() },
                        modifier = Modifier.testTag("clear_logs_button")
                    ) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = "Clear Logs", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        // 1. Root Status Card
        item {
            val rootPresent = viewModel.rootExecutor.isRootBinaryPresent()
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("root_status_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, (if (rootPresent) DevilGreen else DevilAmber).copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background((if (rootPresent) DevilGreen else DevilAmber).copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (rootPresent) Icons.Default.CheckCircle else Icons.Default.Security,
                                    contentDescription = "Status",
                                    tint = if (rootPresent) DevilGreen else DevilAmber,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = if (rootPresent) "Root Binary (su) Detected" else "Standard Shell (Non-Root)",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = if (rootPresent) DevilGreen else DevilAmber
                                )
                                Text(
                                    text = if (rootPresent) "Privileged commands supported with confirmation." else "Standard Linux sh sandbox active.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    if (!rootStatusMessage.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF070B12))
                                .padding(8.dp)
                        ) {
                            Text(
                                text = rootStatusMessage!!,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                color = DevilCyan
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = {
                            isCheckingRoot = true
                            scope.launch {
                                val (ok, msg) = viewModel.rootExecutor.checkRootAccess()
                                rootStatusMessage = msg
                                isCheckingRoot = false
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("test_root_access_button"),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (rootPresent) DevilGreen else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (rootPresent) Color.Black else MaterialTheme.colorScheme.onSurface
                        )
                    ) {
                        Text(
                            text = if (isCheckingRoot) "Requesting su authorization..." else "Verify / Request Root Access (su)",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // 2. Terminal Input & Presets
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("terminal_input_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = if (isBengali) "কমান্ড রানার" else "Command Executor",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    // Preset chips
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(quickPresets) { preset ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .clickable { commandInput = preset }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = preset,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    color = DevilCyan
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = commandInput,
                        onValueChange = { commandInput = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("terminal_command_input"),
                        placeholder = { Text("e.g. id, getprop, df -h") },
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Go),
                        keyboardActions = KeyboardActions(
                            onGo = {
                                if (commandInput.isNotBlank()) {
                                    viewModel.executeTerminalCommand(commandInput, executeAsRoot)
                                    commandInput = ""
                                }
                            }
                        )
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = executeAsRoot,
                                onCheckedChange = { executeAsRoot = it },
                                colors = CheckboxDefaults.colors(checkedColor = DevilRed)
                            )
                            Text(
                                text = "Run with root (su)",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }

                        Button(
                            onClick = {
                                if (commandInput.isNotBlank()) {
                                    viewModel.executeTerminalCommand(commandInput, executeAsRoot)
                                    commandInput = ""
                                }
                            },
                            modifier = Modifier.testTag("run_command_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = DevilRed)
                        ) {
                            Icon(imageVector = Icons.Default.PlayArrow, contentDescription = "Run", modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Execute")
                        }
                    }
                }
            }
        }

        // 3. Execution History & Logs
        item {
            Text(
                text = if (isBengali) "কমান্ড লগ ইতিহাস" else "Execution History & Logs (${actionLogs.size})",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        if (actionLogs.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (isBengali) "কোন কমান্ড হিস্টোরি পাওয়া যায়নি।" else "No terminal commands executed yet.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            items(actionLogs, key = { it.id }) { log ->
                val dateStr = remember(log.timestamp) {
                    SimpleDateFormat("MMM dd HH:mm:ss", Locale.getDefault()).format(Date(log.timestamp))
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(if (log.isSuccess) DevilGreen else DevilRed)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = log.command,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            IconButton(
                                onClick = { clipboardManager.setText(AnnotatedString(log.output)) },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = "Copy output",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = "$dateStr • Type: ${log.actionType} • ${if (log.isRoot) "Root" else "Standard"}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFF090C12))
                                .padding(8.dp)
                        ) {
                            Text(
                                text = log.output.take(800),
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                color = if (log.isSuccess) Color(0xFF64D2FF) else Color(0xFFFF6B6B),
                                lineHeight = 16.sp
                            )
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}
