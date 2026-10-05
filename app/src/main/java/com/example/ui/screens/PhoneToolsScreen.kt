package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Brightness6
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.DevilViewModel
import com.example.ui.theme.DevilAmber
import com.example.ui.theme.DevilCyan
import com.example.ui.theme.DevilGreen
import com.example.ui.theme.DevilRed

@Composable
fun PhoneToolsScreen(
    viewModel: DevilViewModel,
    modifier: Modifier = Modifier
) {
    val deviceStatus by viewModel.deviceStatus.collectAsStateWithLifecycle()
    val installedApps by viewModel.installedApps.collectAsStateWithLifecycle()
    val fileList by viewModel.fileList.collectAsStateWithLifecycle()
    val currentLanguage by viewModel.currentLanguage.collectAsStateWithLifecycle()
    val isBengali = currentLanguage.startsWith("bn", ignoreCase = true)

    var appSearchQuery by remember { mutableStateOf("") }
    var fileSearchQuery by remember { mutableStateOf("") }
    var showCreateFolderDialog by remember { mutableStateOf(false) }
    var newFolderName by remember { mutableStateOf("") }

    var localVolume by remember(deviceStatus?.currentVolumePercent) {
        mutableFloatStateOf((deviceStatus?.currentVolumePercent ?: 50).toFloat())
    }
    var localBrightness by remember(deviceStatus?.currentBrightness) {
        mutableFloatStateOf(((deviceStatus?.currentBrightness ?: 128) * 100 / 255).toFloat())
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(6.dp))
            // Section Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isBengali) "ফোন নিয়ন্ত্রণ ও টুলস" else "Phone Control & Diagnostics",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground
                )
                IconButton(
                    onClick = {
                        viewModel.refreshDeviceStatus()
                        viewModel.refreshApps()
                        viewModel.refreshFiles()
                    },
                    modifier = Modifier.testTag("refresh_tools_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh",
                        tint = DevilCyan
                    )
                }
            }
        }

        // 1. Device Diagnostics Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("device_diagnostics_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = if (isBengali) "ডিভাইস স্ট্যাটাস" else "Device Status",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = DevilCyan
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    if (deviceStatus != null) {
                        val status = deviceStatus!!

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            StatusMetricItem(
                                icon = Icons.Default.BatteryChargingFull,
                                label = if (isBengali) "ব্যাটারি" else "Battery",
                                value = "${status.batteryLevel}%",
                                subValue = if (status.isCharging) (if (isBengali) "চার্জিং" else "Charging") else "",
                                tint = if (status.batteryLevel < 20) DevilRed else DevilGreen
                            )
                            StatusMetricItem(
                                icon = Icons.Default.Memory,
                                label = if (isBengali) "র‌্যাম (RAM)" else "RAM",
                                value = "${status.availRamMb} MB",
                                subValue = "of ${status.totalRamMb} MB free",
                                tint = DevilCyan
                            )
                            StatusMetricItem(
                                icon = Icons.Default.Storage,
                                label = if (isBengali) "স্টোরেজ" else "Storage",
                                value = "${status.availStorageGb} GB",
                                subValue = "of ${status.totalStorageGb} GB",
                                tint = DevilAmber
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // RAM usage bar
                        val ramFraction = ((status.totalRamMb - status.availRamMb).toFloat() / status.totalRamMb.coerceAtLeast(1).toFloat()).coerceIn(0f, 1f)
                        Text(
                            text = "RAM Usage (${(ramFraction * 100).toInt()}%)",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        LinearProgressIndicator(
                            progress = { ramFraction },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = DevilCyan,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Device Details summary text
                        Text(
                            text = "${status.manufacturer} ${status.model} • Android ${status.androidVersion} (API ${status.sdkInt}) • Uptime: ${status.uptimeMinutes} min",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // 2. Hardware Sliders (Volume & Brightness)
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("hardware_controls_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    // Volume Slider
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.VolumeUp, contentDescription = "Volume", tint = DevilRed, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = if (isBengali) "মিডিয়া ভলিউম" else "Media Volume", style = MaterialTheme.typography.titleMedium)
                        }
                        Text(text = "${localVolume.toInt()}%", style = MaterialTheme.typography.labelLarge, color = DevilRed)
                    }
                    Slider(
                        value = localVolume,
                        onValueChange = { localVolume = it },
                        onValueChangeFinished = {
                            viewModel.deviceManager.adjustVolume(localVolume.toInt())
                            viewModel.refreshDeviceStatus()
                        },
                        valueRange = 0f..100f,
                        colors = SliderDefaults.colors(thumbColor = DevilRed, activeTrackColor = DevilRed)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Brightness Slider
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Brightness6, contentDescription = "Brightness", tint = DevilAmber, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = if (isBengali) "পর্দার উজ্জ্বলতা" else "Screen Brightness", style = MaterialTheme.typography.titleMedium)
                        }
                        Text(text = "${localBrightness.toInt()}%", style = MaterialTheme.typography.labelLarge, color = DevilAmber)
                    }
                    Slider(
                        value = localBrightness,
                        onValueChange = { localBrightness = it },
                        onValueChangeFinished = {
                            viewModel.deviceManager.adjustBrightness(localBrightness.toInt())
                            viewModel.refreshDeviceStatus()
                        },
                        valueRange = 5f..100f,
                        colors = SliderDefaults.colors(thumbColor = DevilAmber, activeTrackColor = DevilAmber)
                    )
                }
            }
        }

        // 3. System Settings Shortcuts
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = if (isBengali) "সিস্টেম সেটিংস শর্টকাট" else "System Settings Shortcuts",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        item { SettingsTile(Icons.Default.Wifi, "Wi-Fi") { viewModel.sendUserQuery("Open Wi-Fi settings") } }
                        item { SettingsTile(Icons.Default.Bluetooth, "Bluetooth") { viewModel.sendUserQuery("Open Bluetooth settings") } }
                        item { SettingsTile(Icons.Default.Brightness6, "Display") { viewModel.sendUserQuery("Open Display settings") } }
                        item { SettingsTile(Icons.Default.VolumeUp, "Sound") { viewModel.sendUserQuery("Open Sound settings") } }
                        item { SettingsTile(Icons.Default.BatteryChargingFull, "Battery") { viewModel.sendUserQuery("Battery settings") } }
                        item { SettingsTile(Icons.Default.Settings, "General") { viewModel.sendUserQuery("Open settings") } }
                    }
                }
            }
        }

        // 4. App Launcher
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("app_launcher_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Apps, contentDescription = "Apps", tint = DevilCyan)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isBengali) "ইনস্টল করা অ্যাপস" else "Installed Applications",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = appSearchQuery,
                        onValueChange = { appSearchQuery = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("app_search_field"),
                        placeholder = { Text(if (isBengali) "অ্যাপ খুঁজুন..." else "Search apps...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    val filteredApps = remember(installedApps, appSearchQuery) {
                        if (appSearchQuery.isBlank()) installedApps.take(12)
                        else installedApps.filter { it.name.contains(appSearchQuery, ignoreCase = true) }.take(12)
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        filteredApps.forEach { app ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                    .clickable { viewModel.sendUserQuery("Open ${app.name}") }
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = app.name,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (isBengali) "চালু করুন" else "Launch",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = DevilCyan
                                )
                            }
                        }
                    }
                }
            }
        }

        // 5. File Manager Browser
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("file_manager_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Folder, contentDescription = "Files", tint = DevilAmber)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isBengali) "ফাইল ম্যানেজার" else "File Explorer",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                            )
                        }

                        IconButton(onClick = { showCreateFolderDialog = true }) {
                            Icon(imageVector = Icons.Default.CreateNewFolder, contentDescription = "Create Folder", tint = DevilCyan)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = fileSearchQuery,
                        onValueChange = {
                            fileSearchQuery = it
                            if (it.isNotBlank()) {
                                viewModel.fileManager.searchFiles(it)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text(if (isBengali) "ফাইল খুঁজুন..." else "Search files by name...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    val filesToDisplay = remember(fileList, fileSearchQuery) {
                        if (fileSearchQuery.isBlank()) fileList.take(8)
                        else fileList.filter { it.name.contains(fileSearchQuery, ignoreCase = true) }.take(8)
                    }

                    if (filesToDisplay.isEmpty()) {
                        Text(
                            text = if (isBengali) "কোন ফাইল পাওয়া যায়নি।" else "No files in storage directory.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            filesToDisplay.forEach { file ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                                        .padding(horizontal = 10.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = if (file.isDirectory) Icons.Default.Folder else Icons.Default.InsertDriveFile,
                                        contentDescription = "Item",
                                        tint = if (file.isDirectory) DevilAmber else DevilCyan,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(text = file.name, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium))
                                        Text(text = "${file.formattedSize} • ${file.formattedDate}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
        }
    }

    // Create Folder Dialog
    if (showCreateFolderDialog) {
        AlertDialog(
            onDismissRequest = { showCreateFolderDialog = false },
            title = { Text(if (isBengali) "নতুন ফোল্ডার তৈরি করুন" else "Create New Folder") },
            text = {
                OutlinedTextField(
                    value = newFolderName,
                    onValueChange = { newFolderName = it },
                    placeholder = { Text("e.g. Devil_Project") },
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newFolderName.isNotBlank()) {
                            viewModel.sendUserQuery("Create a folder named $newFolderName")
                            newFolderName = ""
                            showCreateFolderDialog = false
                        }
                    }
                ) {
                    Text(if (isBengali) "তৈরি করুন" else "Create")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateFolderDialog = false }) {
                    Text(if (isBengali) "বাতিল" else "Cancel")
                }
            }
        )
    }
}

@Composable
private fun StatusMetricItem(
    icon: ImageVector,
    label: String,
    value: String,
    subValue: String,
    tint: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(horizontal = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(tint.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = label, tint = tint, modifier = Modifier.size(20.dp))
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = value, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = tint)
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface)
        if (subValue.isNotBlank()) {
            Text(text = subValue, style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun SettingsTile(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(imageVector = icon, contentDescription = label, tint = DevilCyan, modifier = Modifier.size(22.dp))
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = label, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium))
    }
}
