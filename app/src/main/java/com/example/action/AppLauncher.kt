package com.example.action

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.net.Uri
import java.util.Locale

data class InstalledApp(
    val name: String,
    val packageName: String
)

class AppLauncher(private val context: Context) {

    private val packageManager: PackageManager = context.packageManager

    // Commonly referenced aliases
    private val appAliases = mapOf(
        "youtube" to listOf("com.google.android.youtube"),
        "chrome" to listOf("com.android.chrome"),
        "camera" to listOf("com.google.android.GoogleCamera", "com.android.camera", "com.sec.android.app.camera"),
        "settings" to listOf("com.android.settings"),
        "maps" to listOf("com.google.android.apps.maps"),
        "calculator" to listOf("com.google.android.calculator", "com.android.calculator2"),
        "clock" to listOf("com.google.android.deskclock", "com.android.deskclock"),
        "whatsapp" to listOf("com.whatsapp"),
        "telegram" to listOf("org.telegram.messenger"),
        "spotify" to listOf("com.spotify.music"),
        "gmail" to listOf("com.google.android.gm"),
        "photos" to listOf("com.google.android.apps.photos"),
        "play store" to listOf("com.android.vending"),
        "playstore" to listOf("com.android.vending")
    )

    fun getInstalledLaunchableApps(): List<InstalledApp> {
        val intent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val resolveInfos = packageManager.queryIntentActivities(intent, 0)
        return resolveInfos.mapNotNull { ri ->
            val pkg = ri.activityInfo?.packageName ?: return@mapNotNull null
            val name = ri.loadLabel(packageManager).toString()
            InstalledApp(name, pkg)
        }.distinctBy { it.packageName }.sortedBy { it.name.lowercase(Locale.ROOT) }
    }

    fun openApp(query: String): Pair<Boolean, String> {
        val q = query.trim().lowercase(Locale.ROOT)

        // 1. Check direct aliases
        val aliasPackages = appAliases[q]
        if (aliasPackages != null) {
            for (pkg in aliasPackages) {
                val launchIntent = packageManager.getLaunchIntentForPackage(pkg)
                if (launchIntent != null) {
                    launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(launchIntent)
                    return Pair(true, "Opening $query")
                }
            }
        }

        // 2. Query installed apps and match label or package
        val apps = getInstalledLaunchableApps()
        val exactMatch = apps.find { it.name.lowercase(Locale.ROOT) == q }
            ?: apps.find { it.name.lowercase(Locale.ROOT).contains(q) }
            ?: apps.find { it.packageName.lowercase(Locale.ROOT).contains(q) }

        if (exactMatch != null) {
            val launchIntent = packageManager.getLaunchIntentForPackage(exactMatch.packageName)
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(launchIntent)
                return Pair(true, "Opening ${exactMatch.name}")
            }
        }

        // 3. Fallback for web or generic intent
        return Pair(false, "Could not find installed application matching '$query'")
    }

    fun openAppSettings(packageName: String): Intent {
        return Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", packageName, null)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
    }
}
