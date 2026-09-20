package com.example.ui.components

import androidx.compose.foundation.background
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

@Composable
fun ManifestEditorView(
    manifestContent: String,
    onManifestChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var isVisualMode by remember { mutableStateOf(true) }

    // Parse values from manifest XML
    var packageName by remember(manifestContent) {
        val match = Regex("package=\"([^\"]+)\"").find(manifestContent)
        mutableStateOf(match?.groupValues?.get(1) ?: "com.example.app")
    }

    var appLabel by remember(manifestContent) {
        val match = Regex("android:label=\"([^\"]+)\"").find(manifestContent)
        mutableStateOf(match?.groupValues?.get(1) ?: "@string/app_name")
    }

    var appTheme by remember(manifestContent) {
        val match = Regex("android:theme=\"([^\"]+)\"").find(manifestContent)
        mutableStateOf(match?.groupValues?.get(1) ?: "@style/Theme.MyApplication")
    }

    val permissionsList = remember(manifestContent) {
        val list = mutableStateListOf<String>()
        Regex("<uses-permission[^>]+android:name=\"([^\"]+)\"").findAll(manifestContent).forEach {
            list.add(it.groupValues[1])
        }
        if (list.isEmpty()) {
            list.add("android.permission.INTERNET")
        }
        list
    }

    val activitiesList = remember(manifestContent) {
        val list = mutableStateListOf<Triple<String, Boolean, String>>() // name, exported, orientation
        Regex("<activity[^>]+android:name=\"([^\"]+)\"([^>]*)>").findAll(manifestContent).forEach { m ->
            val name = m.groupValues[1]
            val rest = m.groupValues[2]
            val exported = rest.contains("android:exported=\"true\"")
            val orientation = if (rest.contains("android:screenOrientation=\"portrait\"")) "portrait" else "unspecified"
            list.add(Triple(name, exported, orientation))
        }
        if (list.isEmpty()) {
            list.add(Triple(".MainActivity", true, "unspecified"))
        }
        list
    }

    var newPermissionInput by remember { mutableStateOf("") }
    var newActivityInput by remember { mutableStateOf("") }
    var showAddActivityDialog by remember { mutableStateOf(false) }

    fun syncFormToXml() {
        val permsXml = permissionsList.joinToString("\n    ") {
            "<uses-permission android:name=\"$it\" />"
        }

        val actsXml = activitiesList.joinToString("\n\n        ") { (name, exported, orientation) ->
            val orientAttr = if (orientation != "unspecified") "\n            android:screenOrientation=\"$orientation\"" else ""
            val isMain = name.contains("Main")
            val intentFilter = if (isMain) {
                """
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>"""
            } else ""

            """<activity
            android:name="$name"
            android:exported="$exported"$orientAttr>$intentFilter
        </activity>"""
        }

        val generated = """
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    package="$packageName">

    $permsXml

    <application
        android:allowBackup="true"
        android:icon="@mipmap/ic_launcher"
        android:label="$appLabel"
        android:roundIcon="@mipmap/ic_launcher_round"
        android:supportsRtl="true"
        android:theme="$appTheme">

        $actsXml
    </application>
</manifest>
        """.trimIndent()
        onManifestChange(generated)
    }

    Column(modifier = modifier.fillMaxSize().background(StudioDarkBg)) {
        // Mode Switcher Bar
        Surface(color = StudioDarkSurface, tonalElevation = 2.dp, modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Security, contentDescription = null, tint = StudioAccentBlue)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "AndroidManifest.xml Editor",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = StudioTextPrimary
                )
                Spacer(modifier = Modifier.weight(1f))

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(
                        selected = isVisualMode,
                        onClick = { isVisualMode = true },
                        label = { Text("Visual Form", fontSize = 11.sp) }
                    )
                    FilterChip(
                        selected = !isVisualMode,
                        onClick = {
                            syncFormToXml()
                            isVisualMode = false
                        },
                        label = { Text("Raw XML", fontSize = 11.sp) }
                    )
                }
            }
        }

        if (isVisualMode) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Section 1: Package & App Metadata
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = StudioDarkCard),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "PACKAGE & APPLICATION IDENTIFIERS",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = StudioAccentBlue,
                                letterSpacing = 1.sp
                            )

                            OutlinedTextField(
                                value = packageName,
                                onValueChange = {
                                    packageName = it
                                    syncFormToXml()
                                },
                                label = { Text("Package Name") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )

                            OutlinedTextField(
                                value = appLabel,
                                onValueChange = {
                                    appLabel = it
                                    syncFormToXml()
                                },
                                label = { Text("Application Label") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )

                            OutlinedTextField(
                                value = appTheme,
                                onValueChange = {
                                    appTheme = it
                                    syncFormToXml()
                                },
                                label = { Text("Application Theme") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                        }
                    }
                }

                // Section 2: Permissions Checklist & Custom Permissions
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = StudioDarkCard),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "PERMISSIONS (${permissionsList.size})",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = StudioAccentGreen,
                                    letterSpacing = 1.sp
                                )
                                Spacer(modifier = Modifier.weight(1f))
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Standard common permissions toggles
                            val commonPermissions = listOf(
                                "android.permission.INTERNET",
                                "android.permission.ACCESS_NETWORK_STATE",
                                "android.permission.VIBRATE",
                                "android.permission.CAMERA",
                                "android.permission.RECORD_AUDIO",
                                "android.permission.ACCESS_FINE_LOCATION",
                                "android.permission.POST_NOTIFICATIONS",
                                "android.permission.BLUETOOTH"
                            )

                            commonPermissions.forEach { perm ->
                                val has = permissionsList.contains(perm)
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
                                ) {
                                    Checkbox(
                                        checked = has,
                                        onCheckedChange = { checked ->
                                            if (checked) {
                                                if (!permissionsList.contains(perm)) permissionsList.add(perm)
                                            } else {
                                                permissionsList.remove(perm)
                                            }
                                            syncFormToXml()
                                        }
                                    )
                                    Text(
                                        text = perm.removePrefix("android.permission."),
                                        fontSize = 13.sp,
                                        color = StudioTextPrimary
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Add custom permission input
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                OutlinedTextField(
                                    value = newPermissionInput,
                                    onValueChange = { newPermissionInput = it },
                                    placeholder = { Text("Custom permission e.g. com.app.PERMISSION") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Button(
                                    onClick = {
                                        if (newPermissionInput.isNotBlank()) {
                                            permissionsList.add(newPermissionInput.trim())
                                            newPermissionInput = ""
                                            syncFormToXml()
                                        }
                                    }
                                ) {
                                    Text("Add")
                                }
                            }
                        }
                    }
                }

                // Section 3: Activities & Components
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = StudioDarkCard),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "ACTIVITIES & COMPONENTS",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = StudioAccentPurple,
                                    letterSpacing = 1.sp
                                )
                                Spacer(modifier = Modifier.weight(1f))
                                IconButton(onClick = { showAddActivityDialog = true }) {
                                    Icon(Icons.Default.Add, contentDescription = "Add Activity", tint = StudioAccentBlue)
                                }
                            }

                            activitiesList.forEachIndexed { index, (name, exported, orientation) ->
                                Surface(
                                    color = StudioDarkSurface,
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = name,
                                                fontWeight = FontWeight.SemiBold,
                                                fontSize = 13.sp,
                                                color = StudioTextPrimary
                                            )
                                            Spacer(modifier = Modifier.weight(1f))
                                            IconButton(
                                                onClick = {
                                                    activitiesList.removeAt(index)
                                                    syncFormToXml()
                                                },
                                                modifier = Modifier.size(24.dp)
                                            ) {
                                                Icon(Icons.Default.Close, contentDescription = "Remove", tint = StudioAccentRed)
                                            }
                                        }

                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text("Exported", fontSize = 11.sp, color = StudioTextSecondary)
                                                Switch(
                                                    checked = exported,
                                                    onCheckedChange = { chk ->
                                                        activitiesList[index] = Triple(name, chk, orientation)
                                                        syncFormToXml()
                                                    }
                                                )
                                            }

                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text("Orientation: $orientation", fontSize = 11.sp, color = StudioAccentYellow)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // Raw XML Editor view
            OutlinedTextField(
                value = manifestContent,
                onValueChange = onManifestChange,
                textStyle = LocalTextStyle.current.copy(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    color = StudioTextPrimary
                ),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp)
            )
        }
    }

    if (showAddActivityDialog) {
        AlertDialog(
            onDismissRequest = { showAddActivityDialog = false },
            title = { Text("Add Activity") },
            text = {
                OutlinedTextField(
                    value = newActivityInput,
                    onValueChange = { newActivityInput = it },
                    label = { Text("Activity name (e.g. .DetailActivity)") },
                    singleLine = true
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newActivityInput.isNotBlank()) {
                            val actName = if (!newActivityInput.startsWith(".")) ".$newActivityInput" else newActivityInput
                            activitiesList.add(Triple(actName, false, "unspecified"))
                            newActivityInput = ""
                            syncFormToXml()
                            showAddActivityDialog = false
                        }
                    }
                ) {
                    Text("Add")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddActivityDialog = false }) { Text("Cancel") }
            }
        )
    }
}
