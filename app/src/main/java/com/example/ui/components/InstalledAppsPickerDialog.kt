package com.example.ui.components

import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

data class InstalledAppInfo(
    val appName: String,
    val packageName: String,
    val versionName: String,
    val sourceDir: String,
    val icon: Drawable?,
    val isSystem: Boolean,
    val apkSize: Long
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InstalledAppsPickerDialog(
    onDismiss: () -> Unit,
    onImportApk: (File, String) -> Unit
) {
    val context = LocalContext.current
    var apps by remember { mutableStateOf<List<InstalledAppInfo>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var searchQuery by remember { mutableStateOf("") }
    var showSystemApps by remember { mutableStateOf(false) }
    var selectedApp by remember { mutableStateOf<InstalledAppInfo?>(null) }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val pm = context.packageManager
            val packages = pm.getInstalledPackages(PackageManager.GET_META_DATA)
            val list = mutableListOf<InstalledAppInfo>()
            for (pkg in packages) {
                val appInfo = pkg.applicationInfo ?: continue
                val appName = try {
                    pm.getApplicationLabel(appInfo).toString()
                } catch (e: Exception) {
                    pkg.packageName
                }
                val packageName = pkg.packageName ?: continue
                val versionName = pkg.versionName ?: "1.0"
                val sourceDir = appInfo.sourceDir ?: continue
                val isSystem = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                val icon = try {
                    pm.getApplicationIcon(packageName)
                } catch (e: Exception) {
                    null
                }
                val apkFile = File(sourceDir)
                val size = if (apkFile.exists()) apkFile.length() else 0L

                list.add(
                    InstalledAppInfo(
                        appName = appName,
                        packageName = packageName,
                        versionName = versionName,
                        sourceDir = sourceDir,
                        icon = icon,
                        isSystem = isSystem,
                        apkSize = size
                    )
                )
            }
            apps = list.sortedBy { it.appName.lowercase() }
            isLoading = false
        }
    }

    val filteredApps = remember(apps, searchQuery, showSystemApps) {
        apps.filter { app ->
            (showSystemApps || !app.isSystem) &&
            (app.appName.contains(searchQuery, ignoreCase = true) || app.packageName.contains(searchQuery, ignoreCase = true))
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Apps, contentDescription = null, tint = StudioAccentBlue)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Select Installed App", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = StudioTextPrimary)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(420.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    "Choose an application installed on your device to decompile, analyze, and modify in APK Feature Studio.",
                    fontSize = 12.sp,
                    color = StudioTextSecondary
                )

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search installed apps...", fontSize = 12.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = StudioDarkSurface,
                        unfocusedContainerColor = StudioDarkSurface
                    )
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Checkbox(
                        checked = showSystemApps,
                        onCheckedChange = { showSystemApps = it }
                    )
                    Text("Show System Apps", fontSize = 12.sp, color = StudioTextSecondary)
                    Spacer(modifier = Modifier.weight(1f))
                    Text("${filteredApps.size} apps", fontSize = 11.sp, color = StudioTextMuted)
                }

                Divider(color = StudioDarkBorder)

                if (isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = StudioAccentBlue)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Loading installed applications...", fontSize = 12.sp, color = StudioTextSecondary)
                        }
                    }
                } else if (filteredApps.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No applications found matching query.", fontSize = 12.sp, color = StudioTextSecondary)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(filteredApps) { app ->
                            val isSelected = selectedApp?.packageName == app.packageName
                            Surface(
                                color = if (isSelected) StudioAccentBlue.copy(alpha = 0.15f) else StudioDarkSurface,
                                shape = RoundedCornerShape(8.dp),
                                border = if (isSelected) BorderStroke(1.dp, StudioAccentBlue) else null,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedApp = app }
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // App Icon
                                    Surface(
                                        color = StudioDarkCard,
                                        shape = RoundedCornerShape(6.dp),
                                        modifier = Modifier.size(40.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            if (app.icon != null) {
                                                val bitmap = remember(app.icon) {
                                                    val drawable = app.icon
                                                    if (drawable is BitmapDrawable && drawable.bitmap != null) {
                                                        drawable.bitmap
                                                    } else {
                                                        val w = if (drawable.intrinsicWidth > 0) drawable.intrinsicWidth else 96
                                                        val h = if (drawable.intrinsicHeight > 0) drawable.intrinsicHeight else 96
                                                        val bmp = android.graphics.Bitmap.createBitmap(w, h, android.graphics.Bitmap.Config.ARGB_8888)
                                                        val canvas = android.graphics.Canvas(bmp)
                                                        drawable.setBounds(0, 0, canvas.width, canvas.height)
                                                        drawable.draw(canvas)
                                                        bmp
                                                    }
                                                }
                                                Image(
                                                    bitmap = bitmap.asImageBitmap(),
                                                    contentDescription = null,
                                                    modifier = Modifier.size(32.dp).clip(RoundedCornerShape(4.dp))
                                                )
                                            } else {
                                                Icon(Icons.Default.Android, contentDescription = null, tint = StudioAccentOrange, modifier = Modifier.size(24.dp))
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(10.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = app.appName,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                color = StudioTextPrimary,
                                                maxLines = 1
                                            )
                                            if (app.isSystem) {
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Badge(containerColor = StudioTextMuted) {
                                                    Text("System", fontSize = 8.sp, color = Color.White, modifier = Modifier.padding(horizontal = 3.dp, vertical = 1.dp))
                                                }
                                            }
                                        }
                                        Text(
                                            text = app.packageName,
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 10.sp,
                                            color = StudioTextSecondary,
                                            maxLines = 1
                                        )
                                        Text(
                                            text = "v${app.versionName} • ${formatSize(app.apkSize)}",
                                            fontSize = 10.sp,
                                            color = StudioTextMuted
                                        )
                                    }

                                    RadioButton(
                                        selected = isSelected,
                                        onClick = { selectedApp = app }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    selectedApp?.let { app ->
                        val apkFile = File(app.sourceDir)
                        if (apkFile.exists()) {
                            onImportApk(apkFile, app.appName.replace(Regex("[^a-zA-Z0-9_]"), ""))
                        }
                    }
                },
                enabled = selectedApp != null
            ) {
                Text("Decompile & Modify")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        containerColor = StudioDarkBg
    )
}

private fun formatSize(bytes: Long): String {
    if (bytes <= 0) return "0 KB"
    val kb = bytes / 1024.0
    val mb = kb / 1024.0
    return if (mb >= 1.0) String.format(java.util.Locale.US, "%.1f MB", mb)
    else String.format(java.util.Locale.US, "%.1f KB", kb)
}
