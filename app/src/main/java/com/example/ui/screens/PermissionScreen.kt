package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.permission.PermissionItem
import com.example.ui.DevilViewModel
import com.example.ui.theme.DevilAmber
import com.example.ui.theme.DevilCyan
import com.example.ui.theme.DevilGreen
import com.example.ui.theme.DevilRed

@Composable
fun PermissionScreen(
    viewModel: DevilViewModel,
    modifier: Modifier = Modifier
) {
    val permissions by viewModel.permissionsState.collectAsStateWithLifecycle()
    val currentLanguage by viewModel.currentLanguage.collectAsStateWithLifecycle()
    val isBengali = currentLanguage.startsWith("bn", ignoreCase = true)
    val context = LocalContext.current

    // Auto-refresh when entering screen
    LaunchedEffect(Unit) {
        viewModel.refreshPermissions()
    }

    // Permission launcher for runtime permissions
    val runtimeLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) {
        viewModel.refreshPermissions()
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Security, contentDescription = "Security", tint = DevilCyan)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isBengali) "অনুমতি ও নিরাপত্তা ম্যানেজার" else "Permission & Security Manager",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                }

                IconButton(
                    onClick = { viewModel.refreshPermissions() },
                    modifier = Modifier.testTag("refresh_permissions_button")
                ) {
                    Icon(imageVector = Icons.Default.Refresh, contentDescription = "Refresh", tint = DevilCyan)
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = if (isBengali) {
                    "Devil AI এর নির্বিঘ্ন সেবা নিশ্চিত করতে প্রয়োজনীয় অ্যান্ড্রয়েড অনুমতি এবং অ্যাক্সেসিবিলিটি সেটিংস যাচাই করুন।"
                } else {
                    "Manage Android system permissions and Accessibility Service required for phone voice automation."
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        items(permissions, key = { it.id }) { item ->
            PermissionCard(
                item = item,
                isBengali = isBengali,
                onRequest = {
                    if (item.runtimePermission != null && !item.isGranted) {
                        runtimeLauncher.launch(item.runtimePermission)
                    } else if (item.settingsAction != null) {
                        try {
                            val intent = item.settingsAction.invoke()
                            context.startActivity(intent)
                        } catch (_: Exception) {}
                    }
                }
            )
        }

        item {
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
private fun PermissionCard(
    item: PermissionItem,
    isBengali: Boolean,
    onRequest: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("permission_card_${item.id}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (item.isGranted) DevilGreen.copy(alpha = 0.4f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header Row: Title + Badges
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Status Icon
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(
                                if (item.isGranted) DevilGreen.copy(alpha = 0.15f)
                                else DevilRed.copy(alpha = 0.15f)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (item.isGranted) Icons.Default.Check else Icons.Default.Close,
                            contentDescription = if (item.isGranted) "Granted" else "Not Granted",
                            tint = if (item.isGranted) DevilGreen else DevilRed,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                // Badges (Granted / Not Granted & Required / Optional)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    // Required or Optional Badge
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(
                                if (item.isRequired) DevilAmber.copy(alpha = 0.15f)
                                else MaterialTheme.colorScheme.surfaceVariant
                            )
                            .padding(horizontal = 7.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = if (item.isRequired) {
                                if (isBengali) "প্রয়োজনীয়" else "Required"
                            } else {
                                if (isBengali) "ঐচ্ছিক" else "Optional"
                            },
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.SemiBold),
                            color = if (item.isRequired) DevilAmber else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Granted / Not Granted Badge
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(
                                if (item.isGranted) DevilGreen.copy(alpha = 0.15f)
                                else DevilRed.copy(alpha = 0.15f)
                            )
                            .padding(horizontal = 7.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = if (item.isGranted) {
                                if (isBengali) "অনুমোদিত" else "Granted"
                            } else {
                                if (isBengali) "অনুমোদিত নয়" else "Not Granted"
                            },
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                            color = if (item.isGranted) DevilGreen else DevilRed
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = item.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (!item.isGranted && (item.settingsAction != null || item.runtimePermission != null)) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    Button(
                        onClick = onRequest,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (item.isRequired) DevilRed else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (item.isRequired) Color.White else MaterialTheme.colorScheme.onSurface
                        ),
                        modifier = Modifier.testTag("grant_permission_${item.id}")
                    ) {
                        Icon(imageVector = Icons.Default.OpenInNew, contentDescription = "Open Settings", modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (item.runtimePermission != null) {
                                if (isBengali) "অনুমতি দিন" else "Grant Permission"
                            } else {
                                if (isBengali) "সেটিংস খুলুন" else "Open Settings"
                            },
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }
        }
    }
}
