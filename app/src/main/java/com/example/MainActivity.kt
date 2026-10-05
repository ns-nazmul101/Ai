package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.SmartToy
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.example.ui.components.DestructiveConfirmDialog
import com.example.ui.screens.AssistantScreen
import com.example.ui.screens.PermissionScreen
import com.example.ui.screens.PhoneToolsScreen
import com.example.ui.screens.RootConsoleScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.DevilAiTheme
import com.example.ui.theme.DevilCyan
import com.example.ui.theme.DevilRed

enum class DevilScreen(
    val labelEn: String,
    val labelBn: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    ASSISTANT("Assistant", "সহকারী", Icons.Filled.SmartToy, Icons.Outlined.SmartToy),
    PHONE_TOOLS("Tools", "টুলস", Icons.Filled.Build, Icons.Outlined.Build),
    ROOT_CONSOLE("Root", "রুট", Icons.Filled.Terminal, Icons.Filled.Terminal),
    PERMISSIONS("Permissions", "অনুমতি", Icons.Filled.Security, Icons.Outlined.Security),
    SETTINGS("Settings", "সেটিংস", Icons.Filled.Settings, Icons.Outlined.Settings)
}

class MainActivity : ComponentActivity() {

    private val viewModel: DevilViewModel by viewModels()

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            DevilAiTheme {
                var currentScreen by remember { mutableStateOf(DevilScreen.ASSISTANT) }
                val currentLanguage by viewModel.currentLanguage.collectAsStateWithLifecycle()
                val pendingDestructive by viewModel.pendingDestructive.collectAsStateWithLifecycle()
                val isBengali = currentLanguage.startsWith("bn", ignoreCase = true)

                // Handle back button on sub-screens
                if (currentScreen != DevilScreen.ASSISTANT) {
                    BackHandler {
                        currentScreen = DevilScreen.ASSISTANT
                    }
                }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    topBar = {
                        CenterAlignedTopAppBar(
                            title = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(CircleShape)
                                            .background(DevilRed),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "D",
                                            fontWeight = FontWeight.Black,
                                            color = Color.White,
                                            fontSize = 14.sp
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "[Devil] AI",
                                        style = MaterialTheme.typography.titleLarge.copy(
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = 0.5.sp
                                        ),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            },
                            actions = {
                                // Language switch button in top app bar
                                Box(
                                    modifier = Modifier
                                        .padding(end = 12.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                                        .clickable {
                                            viewModel.setLanguage(if (isBengali) "en-US" else "bn-BD")
                                        }
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                        .testTag("language_toggle_button")
                                ) {
                                    Text(
                                        text = if (isBengali) "বাংলা" else "EN",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = DevilCyan
                                    )
                                }
                            },
                            colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            ),
                            modifier = Modifier.statusBarsPadding()
                        )
                    },
                    bottomBar = {
                        NavigationBar(
                            containerColor = MaterialTheme.colorScheme.surface,
                            tonalElevation = 6.dp
                        ) {
                            DevilScreen.entries.forEach { screen ->
                                val isSelected = currentScreen == screen
                                NavigationBarItem(
                                    selected = isSelected,
                                    onClick = { currentScreen = screen },
                                    icon = {
                                        Icon(
                                            imageVector = if (isSelected) screen.selectedIcon else screen.unselectedIcon,
                                            contentDescription = screen.labelEn
                                        )
                                    },
                                    label = {
                                        Text(
                                            text = if (isBengali) screen.labelBn else screen.labelEn,
                                            fontSize = 11.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = DevilRed,
                                        selectedTextColor = DevilRed,
                                        indicatorColor = DevilRed.copy(alpha = 0.15f),
                                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                    ),
                                    modifier = Modifier.testTag("nav_${screen.name.lowercase()}")
                                )
                            }
                        }
                    }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        when (currentScreen) {
                            DevilScreen.ASSISTANT -> AssistantScreen(viewModel = viewModel)
                            DevilScreen.PHONE_TOOLS -> PhoneToolsScreen(viewModel = viewModel)
                            DevilScreen.ROOT_CONSOLE -> RootConsoleScreen(viewModel = viewModel)
                            DevilScreen.PERMISSIONS -> PermissionScreen(viewModel = viewModel)
                            DevilScreen.SETTINGS -> SettingsScreen(viewModel = viewModel)
                        }

                        // Destructive Operation Confirmation Dialog
                        if (pendingDestructive != null) {
                            DestructiveConfirmDialog(
                                promptMessage = pendingDestructive!!.second,
                                onConfirm = { viewModel.confirmDestructiveAction() },
                                onDismiss = { viewModel.cancelDestructiveAction() },
                                isBengali = isBengali
                            )
                        }
                    }
                }
            }
        }
    }
}
