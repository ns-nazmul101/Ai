package com.example.permission

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.core.content.ContextCompat
import com.example.action.RootExecutor
import com.example.service.DevilAccessibilityService

data class PermissionItem(
    val id: String,
    val title: String,
    val description: String,
    val isGranted: Boolean,
    val isRequired: Boolean,
    val category: String,
    val settingsAction: (() -> Intent)?,
    val runtimePermission: String? = null
)

class PermissionManager(
    private val context: Context,
    private val rootExecutor: RootExecutor
) {

    fun getPermissionsState(): List<PermissionItem> {
        val list = mutableListOf<PermissionItem>()

        // 1. Microphone
        val micGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        list.add(
            PermissionItem(
                id = "microphone",
                title = "Microphone",
                description = "Required for real-time speech recognition and voice commands.",
                isGranted = micGranted,
                isRequired = true,
                category = "Core Voice",
                settingsAction = {
                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", context.packageName, null)
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                },
                runtimePermission = Manifest.permission.RECORD_AUDIO
            )
        )

        // 2. Notifications
        val notifGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
        list.add(
            PermissionItem(
                id = "notifications",
                title = "Notifications",
                description = "Alerts for background tasks, command logs, and status updates.",
                isGranted = notifGranted,
                isRequired = false,
                category = "Core",
                settingsAction = {
                    Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                        putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                },
                runtimePermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    Manifest.permission.POST_NOTIFICATIONS
                } else null
            )
        )

        // 3. Accessibility Service
        val accessibilityActive = DevilAccessibilityService.isServiceRunning
        list.add(
            PermissionItem(
                id = "accessibility",
                title = "Accessibility Service",
                description = "Enables hands-free screenshots, Home, Back, and screen lock without root.",
                isGranted = accessibilityActive,
                isRequired = true,
                category = "Phone Control",
                settingsAction = {
                    Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                }
            )
        )

        // 4. Storage / All Files Access
        val storageGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        }
        list.add(
            PermissionItem(
                id = "storage",
                title = "Storage & Files",
                description = "Allows creating folders, searching files, and managing device storage.",
                isGranted = storageGranted,
                isRequired = false,
                category = "Phone Control",
                settingsAction = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                            data = Uri.parse("package:${context.packageName}")
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                    } else {
                        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.fromParts("package", context.packageName, null)
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                    }
                }
            )
        )

        // 5. System Write Settings
        val writeSettingsGranted = Settings.System.canWrite(context)
        list.add(
            PermissionItem(
                id = "write_settings",
                title = "System Write Settings",
                description = "Required to adjust device screen brightness and display settings.",
                isGranted = writeSettingsGranted,
                isRequired = false,
                category = "Phone Control",
                settingsAction = {
                    Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS).apply {
                        data = Uri.parse("package:${context.packageName}")
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                }
            )
        )

        // 6. Root Status
        val rootBinaryExists = rootExecutor.isRootBinaryPresent()
        list.add(
            PermissionItem(
                id = "root_status",
                title = "Root Access",
                description = if (rootBinaryExists) "Root binary (su) detected. Can execute authorized shell and system commands." else "Device is not rooted or su binary is not installed.",
                isGranted = rootBinaryExists,
                isRequired = false,
                category = "Power User",
                settingsAction = null
            )
        )

        return list
    }
}
