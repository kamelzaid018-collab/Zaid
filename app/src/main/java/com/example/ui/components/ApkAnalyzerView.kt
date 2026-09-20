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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ApkAnalysis
import com.example.ui.theme.*

@Composable
fun ApkAnalyzerView(
    analysis: ApkAnalysis,
    onOpenSmaliClass: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Overview", "DEX (${analysis.dexFiles.size})", "Manifest & Perms", "Native Libs (${analysis.nativeLibs.size})", "Signature")

    Column(modifier = modifier.fillMaxSize().background(StudioDarkBg)) {
        // App Header Info Banner
        Surface(color = StudioDarkSurface, tonalElevation = 3.dp, modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = StudioDarkCard,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.size(52.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Android, contentDescription = null, tint = StudioAccentGreen, modifier = Modifier.size(32.dp))
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = analysis.packageName,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = StudioTextPrimary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("v${analysis.versionName} (${analysis.versionCode})", fontSize = 12.sp, color = StudioAccentBlue)
                        Text("•", fontSize = 12.sp, color = StudioTextMuted)
                        Text("minSdk ${analysis.minSdk}", fontSize = 12.sp, color = StudioTextSecondary)
                        Text("•", fontSize = 12.sp, color = StudioTextMuted)
                        Text("targetSdk ${analysis.targetSdk}", fontSize = 12.sp, color = StudioAccentPurple)
                    }
                }
            }
        }

        // Sub-tabs
        ScrollableTabRow(
            selectedTabIndex = selectedTab,
            containerColor = StudioDarkSurface,
            contentColor = StudioAccentBlue,
            edgePadding = 8.dp
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = { Text(title, fontSize = 12.sp) }
                )
            }
        }

        when (selectedTab) {
            0 -> OverviewTab(analysis)
            1 -> DexAnalysisTab(analysis, onOpenSmaliClass)
            2 -> ManifestPermsTab(analysis)
            3 -> NativeLibsTab(analysis)
            4 -> SignatureTab(analysis)
        }
    }
}

@Composable
private fun OverviewTab(analysis: ApkAnalysis) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Text("APK BREAKDOWN SUMMARY", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = StudioAccentBlue, letterSpacing = 1.sp)
        }

        // Size cards
        item {
            val totalMethods = analysis.dexFiles.sumOf { it.totalMethods }
            val totalClasses = analysis.dexFiles.sumOf { it.totalClasses }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MetricCard("DEX Classes", "$totalClasses", StudioAccentBlue, Modifier.weight(1f))
                MetricCard("DEX Methods", "$totalMethods", StudioAccentPurple, Modifier.weight(1f))
                MetricCard("Resources", "${analysis.totalResourcesCount}", StudioAccentOrange, Modifier.weight(1f))
            }
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = StudioDarkCard),
                shape = RoundedCornerShape(10.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Package Components Breakdown", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = StudioTextPrimary)
                    ComponentRow("Activities", "${analysis.activities.size}", StudioAccentGreen)
                    ComponentRow("Services", "${analysis.services.size}", StudioAccentBlue)
                    ComponentRow("Broadcast Receivers", "${analysis.receivers.size}", StudioAccentPurple)
                    ComponentRow("Content Providers", "${analysis.providers.size}", StudioAccentOrange)
                    ComponentRow("Permissions Declared", "${analysis.permissions.size}", StudioAccentRed)
                    ComponentRow("Assets Files", "${analysis.totalAssetsCount}", StudioTextSecondary)
                }
            }
        }
    }
}

@Composable
private fun MetricCard(label: String, value: String, accent: Color, modifier: Modifier = Modifier) {
    Surface(
        color = StudioDarkCard,
        shape = RoundedCornerShape(10.dp),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(label, fontSize = 11.sp, color = StudioTextSecondary)
            Spacer(modifier = Modifier.height(4.dp))
            Text(value, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = accent)
        }
    }
}

@Composable
private fun ComponentRow(name: String, count: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Text(name, color = StudioTextPrimary, fontSize = 13.sp, modifier = Modifier.weight(1f))
        Text(count, color = color, fontWeight = FontWeight.Bold, fontSize = 13.sp)
    }
}

@Composable
private fun DexAnalysisTab(analysis: ApkAnalysis, onOpenSmaliClass: (String) -> Unit) {
    LazyColumn(modifier = Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        analysis.dexFiles.forEach { dex ->
            item {
                Card(colors = CardDefaults.cardColors(containerColor = StudioDarkCard), shape = RoundedCornerShape(10.dp)) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.DataObject, contentDescription = null, tint = StudioAccentBlue)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(dex.dexFileName, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = StudioTextPrimary)
                            Spacer(modifier = Modifier.weight(1f))
                            Badge(containerColor = StudioAccentGreen) {
                                Text("DEX Bytecode", fontSize = 10.sp)
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Classes: ${dex.totalClasses}  •  Methods: ${dex.totalMethods}  •  Fields: ${dex.totalFields}  •  Strings: ${dex.totalStrings}", fontSize = 12.sp, color = StudioTextSecondary)
                    }
                }
            }

            item {
                Text("DEX CLASS HIERARCHY (${dex.classes.size})", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = StudioAccentBlue)
            }

            items(dex.classes) { cls ->
                Surface(
                    color = StudioDarkSurface,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(cls.className, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = StudioAccentGreen)
                            Spacer(modifier = Modifier.weight(1f))
                            OutlinedButton(
                                onClick = { onOpenSmaliClass(cls.className) },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                modifier = Modifier.height(28.dp)
                            ) {
                                Text("Smali", fontSize = 11.sp)
                            }
                        }
                        Text("extends ${cls.superClassName}", fontSize = 11.sp, color = StudioTextMuted)
                        Spacer(modifier = Modifier.height(4.dp))
                        cls.methods.take(3).forEach { m ->
                            Text("  • ${m.name}(${m.parameterTypes.joinToString(", ")}): ${m.returnType}", fontSize = 11.sp, color = StudioTextSecondary, fontFamily = FontFamily.Monospace)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ManifestPermsTab(analysis: ApkAnalysis) {
    LazyColumn(modifier = Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Text("DECLARED PERMISSIONS (${analysis.permissions.size})", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = StudioAccentOrange)
        }
        items(analysis.permissions) { perm ->
            Surface(
                color = StudioDarkCard,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Lock, contentDescription = null, tint = StudioAccentOrange, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(perm, fontSize = 12.sp, color = StudioTextPrimary, fontFamily = FontFamily.Monospace)
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(8.dp))
            Text("ACTIVITIES (${analysis.activities.size})", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = StudioAccentBlue)
        }
        items(analysis.activities) { act ->
            Surface(
                color = StudioDarkSurface,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.SmartDisplay, contentDescription = null, tint = StudioAccentBlue, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(act, fontSize = 12.sp, color = StudioTextPrimary)
                }
            }
        }
    }
}

@Composable
private fun NativeLibsTab(analysis: ApkAnalysis) {
    LazyColumn(modifier = Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            Text("NATIVE SHARED LIBRARIES (.so)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = StudioAccentPurple)
        }
        if (analysis.nativeLibs.isEmpty()) {
            item {
                Surface(color = StudioDarkCard, shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
                    Text("No native libraries found in APK (pure Java/Kotlin bytecode).", modifier = Modifier.padding(16.dp), color = StudioTextSecondary, fontSize = 13.sp)
                }
            }
        } else {
            items(analysis.nativeLibs) { lib ->
                Surface(color = StudioDarkCard, shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Memory, contentDescription = null, tint = StudioAccentPurple)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(lib.fileName, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = StudioTextPrimary)
                            Text("ABI: ${lib.arch}", fontSize = 11.sp, color = StudioAccentBlue)
                        }
                        Text("${lib.sizeBytes / 1024} KB", fontSize = 12.sp, color = StudioTextSecondary)
                    }
                }
            }
        }
    }
}

@Composable
private fun SignatureTab(analysis: ApkAnalysis) {
    LazyColumn(modifier = Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Text("SIGNATURE & CERTIFICATES", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = StudioAccentGreen)
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = StudioDarkCard), shape = RoundedCornerShape(10.dp)) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Verified, contentDescription = null, tint = StudioAccentGreen)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (analysis.hasV1Signature) "V1 Scheme (JAR) Signature Valid" else "V1 Scheme Unsigned", fontWeight = FontWeight.Bold, color = StudioTextPrimary)
                    }
                    Text("V2 Scheme: ${if (analysis.hasV2Signature) "Present" else "Not present"}", fontSize = 12.sp, color = StudioTextSecondary)
                    Divider(color = StudioDarkBorder)
                    Text("Certificate SHA-256 Fingerprint:", fontSize = 11.sp, color = StudioTextSecondary)
                    Text(analysis.certificateSha256, fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = StudioAccentYellow)
                }
            }
        }
    }
}
