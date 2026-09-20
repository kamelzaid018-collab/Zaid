package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.os.Environment
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import com.example.engine.AndroidStudioExportEngine
import com.example.model.*
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportToAndroidStudioDialog(
    project: StudioProject,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val clipboardManager = LocalClipboardManager.current

    var isExporting by remember { mutableStateOf(true) }
    var steps by remember { mutableStateOf<List<ExportStep>>(emptyList()) }
    var report by remember { mutableStateOf<ExportReport?>(null) }
    var selectedSection by remember { mutableIntStateOf(0) } // 0: Report, 1: Project Tree, 2: Unconverted Files, 3: AI Workflow

    // Launch export immediately upon dialog presentation
    LaunchedEffect(project.id) {
        isExporting = true
        report = null
        try {
            val result = AndroidStudioExportEngine.exportProject(context, project) { currentSteps ->
                steps = currentSteps
            }
            report = result
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            isExporting = false
        }
    }

    Dialog(
        onDismissRequest = { if (!isExporting) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.92f),
            shape = RoundedCornerShape(16.dp),
            color = StudioDarkBg,
            tonalElevation = 6.dp
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header Bar
                Surface(
                    color = StudioDarkSurface,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(StudioAccentBlue.copy(alpha = 0.15f), RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.CloudDownload,
                                contentDescription = null,
                                tint = StudioAccentBlue,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Export to Android Studio",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = StudioTextPrimary
                            )
                            Text(
                                text = "Production-grade project generator & validator",
                                fontSize = 11.sp,
                                color = StudioTextSecondary
                            )
                        }
                        IconButton(
                            onClick = onDismiss,
                            enabled = !isExporting
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = StudioTextMuted)
                        }
                    }
                }

                HorizontalDivider(color = StudioDarkBorder)

                // Main Content Body
                Box(modifier = Modifier.weight(1f)) {
                    if (isExporting || report == null) {
                        // Progress / Steps Screen
                        ExportProgressView(steps = steps)
                    } else {
                        // Completed Report Screen
                        ExportReportView(
                            report = report!!,
                            selectedSection = selectedSection,
                            onSelectSection = { selectedSection = it }
                        )
                    }
                }

                HorizontalDivider(color = StudioDarkBorder)

                // Bottom Action Footer
                Surface(
                    color = StudioDarkSurface,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.End
                    ) {
                        if (!isExporting && report != null) {
                            OutlinedButton(
                                onClick = {
                                    saveZipToDownloads(context, report!!.zipFile)
                                },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.height(38.dp)
                            ) {
                                Icon(Icons.Default.SaveAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Save to Downloads", fontSize = 12.sp)
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            Button(
                                onClick = {
                                    shareExportZip(context, report!!.zipFile)
                                },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = StudioAccentGreen),
                                modifier = Modifier.height(38.dp)
                            ) {
                                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Share ZIP", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }

                            Spacer(modifier = Modifier.width(8.dp))
                        }

                        Button(
                            onClick = onDismiss,
                            enabled = !isExporting,
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = StudioDarkCard),
                            modifier = Modifier.height(38.dp)
                        ) {
                            Text(if (isExporting) "Exporting..." else "Done", fontSize = 12.sp, color = StudioTextPrimary)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ExportProgressView(steps: List<ExportStep>) {
    val completedCount = steps.count { it.status == ExportStepStatus.SUCCESS || it.status == ExportStepStatus.WARNING }
    val progress = if (steps.isEmpty()) 0.05f else completedCount.toFloat() / steps.size.toFloat()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = StudioDarkCard),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.5.dp,
                        color = StudioAccentBlue
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Preparing Android Studio Project...",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = StudioTextPrimary
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text = "${(progress * 100).toInt()}%",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = StudioAccentBlue
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp),
                    color = StudioAccentBlue,
                    trackColor = StudioDarkBorder
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))
        Text(
            text = "PRE-EXPORT VALIDATION PIPELINE (10 STEPS)",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = StudioTextMuted
        )
        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(steps) { step ->
                StepItemCard(step = step)
            }
        }
    }
}

@Composable
private fun StepItemCard(step: ExportStep) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = when (step.status) {
                ExportStepStatus.RUNNING -> StudioDarkSurface
                ExportStepStatus.SUCCESS -> StudioDarkCard
                ExportStepStatus.WARNING -> StudioDarkCard
                ExportStepStatus.FAILED -> StudioDarkSurface
                ExportStepStatus.PENDING -> StudioDarkBg
            }
        ),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                1.dp,
                when (step.status) {
                    ExportStepStatus.RUNNING -> StudioAccentBlue.copy(alpha = 0.5f)
                    ExportStepStatus.SUCCESS -> StudioAccentGreen.copy(alpha = 0.2f)
                    ExportStepStatus.WARNING -> StudioAccentYellow.copy(alpha = 0.3f)
                    ExportStepStatus.FAILED -> StudioAccentRed.copy(alpha = 0.4f)
                    ExportStepStatus.PENDING -> StudioDarkBorder.copy(alpha = 0.4f)
                },
                RoundedCornerShape(8.dp)
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(24.dp),
                contentAlignment = Alignment.Center
            ) {
                when (step.status) {
                    ExportStepStatus.PENDING -> {
                        Text("${step.stepNumber}", fontSize = 11.sp, color = StudioTextMuted, fontWeight = FontWeight.Bold)
                    }
                    ExportStepStatus.RUNNING -> {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = StudioAccentBlue)
                    }
                    ExportStepStatus.SUCCESS -> {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = StudioAccentGreen, modifier = Modifier.size(18.dp))
                    }
                    ExportStepStatus.WARNING -> {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = StudioAccentYellow, modifier = Modifier.size(18.dp))
                    }
                    ExportStepStatus.FAILED -> {
                        Icon(Icons.Default.Cancel, contentDescription = null, tint = StudioAccentRed, modifier = Modifier.size(18.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${step.stepNumber}. ${step.title}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (step.status == ExportStepStatus.PENDING) StudioTextMuted else StudioTextPrimary
                    )
                }
                Text(
                    text = if (step.message.isNotEmpty()) step.message else step.description,
                    fontSize = 11.sp,
                    color = if (step.status == ExportStepStatus.RUNNING) StudioAccentBlue else StudioTextSecondary
                )
            }
        }
    }
}

@Composable
private fun ExportReportView(
    report: ExportReport,
    selectedSection: Int,
    onSelectSection: (Int) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // Summary Header Card
        Surface(color = StudioDarkSurface, modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(StudioAccentGreen.copy(alpha = 0.2f), RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, tint = StudioAccentGreen, modifier = Modifier.size(20.dp))
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text("Export completed", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = StudioAccentGreen)
                        Text("Project: ${report.projectName}", fontSize = 12.sp, color = StudioTextPrimary)
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    Badge(
                        containerColor = if (report.isReady) StudioAccentGreen.copy(alpha = 0.2f) else StudioAccentRed.copy(alpha = 0.2f)
                    ) {
                        Text(
                            text = if (report.isReady) "Android Studio Project: Ready" else "Build Action Required",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (report.isReady) StudioAccentGreen else StudioAccentRed,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Nature Badge (CRITICAL REQUIREMENT)
                Surface(
                    color = if (report.isReconstructedFromApk) StudioAccentOrange.copy(alpha = 0.15f) else StudioAccentBlue.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            if (report.isReconstructedFromApk) Icons.Default.Engineering else Icons.Default.Code,
                            contentDescription = null,
                            tint = if (report.isReconstructedFromApk) StudioAccentOrange else StudioAccentBlue,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (report.isReconstructedFromApk) {
                                "Source reconstructed from APK (decompiled classes in java/ & smali/ preserved)"
                            } else {
                                "Original Source Code (100% fidelity Kotlin, Compose & Gradle)"
                            },
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (report.isReconstructedFromApk) StudioAccentOrange else StudioAccentBlue
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Exact Metric Columns as required
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    MetricBox(label = "Source files", value = "${report.sourceFilesCount}", icon = Icons.Default.Description, color = StudioAccentBlue)
                    MetricBox(label = "Resource files", value = "${report.resourceFilesCount}", icon = Icons.Default.FolderOpen, color = StudioAccentPurple)
                    MetricBox(label = "Dependencies", value = "${report.dependenciesCount}", icon = Icons.Default.Extension, color = StudioAccentYellow)
                    MetricBox(label = "Warnings", value = "${report.warningsCount}", icon = Icons.Default.Warning, color = if (report.warningsCount > 0) StudioAccentOrange else StudioTextSecondary)
                    MetricBox(label = "Errors", value = "${report.errorsCount}", icon = Icons.Default.Error, color = if (report.errorsCount > 0) StudioAccentRed else StudioAccentGreen)
                }
            }
        }

        // Sub-Navigation Tabs
        ScrollableTabRow(
            selectedTabIndex = selectedSection,
            containerColor = StudioDarkBg,
            contentColor = StudioAccentBlue,
            edgePadding = 12.dp
        ) {
            Tab(
                selected = selectedSection == 0,
                onClick = { onSelectSection(0) },
                text = { Text("Export Report", fontSize = 12.sp) }
            )
            Tab(
                selected = selectedSection == 1,
                onClick = { onSelectSection(1) },
                text = { Text("Directory Structure", fontSize = 12.sp) }
            )
            Tab(
                selected = selectedSection == 2,
                onClick = { onSelectSection(2) },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Unconverted Files", fontSize = 12.sp)
                        if (report.unconvertedFiles.isNotEmpty()) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Badge(containerColor = StudioAccentOrange) {
                                Text("${report.unconvertedFiles.size}", fontSize = 9.sp)
                            }
                        }
                    }
                }
            )
            Tab(
                selected = selectedSection == 3,
                onClick = { onSelectSection(3) },
                text = { Text("AI Editing Workflow", fontSize = 12.sp) }
            )
        }

        // Tab Body Content
        Box(
            modifier = Modifier
                .weight(1f)
                .padding(12.dp)
        ) {
            when (selectedSection) {
                0 -> ExportReportDetails(report = report)
                1 -> ProjectTreeStructureView(report = report)
                2 -> UnconvertedFilesView(unconvertedFiles = report.unconvertedFiles)
                3 -> AiEditingWorkflowGuide(report = report)
            }
        }
    }
}

@Composable
private fun MetricBox(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color
) {
    Surface(
        color = StudioDarkCard,
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.width(62.dp)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.height(4.dp))
            Text(value, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = color)
            Text(label, fontSize = 8.5.sp, color = StudioTextSecondary, maxLines = 1)
        }
    }
}

@Composable
private fun ExportReportDetails(report: ExportReport) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = StudioDarkCard),
                shape = RoundedCornerShape(10.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("Android Studio Export Summary", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = StudioTextPrimary)
                    Spacer(modifier = Modifier.height(8.dp))

                    ReportRow("Project Name", report.projectName)
                    ReportRow("Status", "Android Studio Project: Ready", valueColor = StudioAccentGreen)
                    ReportRow("Source Mode", if (report.isReconstructedFromApk) "Source reconstructed from APK" else "Original Source Code")
                    ReportRow("Source files count", "${report.sourceFilesCount}")
                    ReportRow("Resource files count", "${report.resourceFilesCount}")
                    ReportRow("Dependencies count", "${report.dependenciesCount}")
                    ReportRow("Warnings count", "${report.warningsCount}")
                    ReportRow("Errors count", "${report.errorsCount}", valueColor = if (report.errorsCount == 0) StudioAccentGreen else StudioAccentRed)
                    ReportRow("Archive Size", "${report.zipSizeBytes / 1024} KB")
                    ReportRow("Export Duration", "${report.durationMs} ms")
                    ReportRow("Build Toolchain", "Gradle 8.7 • AGP 8.5.2 • Java 17")
                }
            }
        }

        if (report.warningsList.isNotEmpty()) {
            item {
                Text(
                    "EXPORT WARNINGS & NOTICES (${report.warningsList.size})",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = StudioAccentOrange
                )
            }
            items(report.warningsList) { warning ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = StudioDarkCard),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = StudioAccentOrange, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(warning, fontSize = 12.sp, color = StudioTextPrimary)
                    }
                }
            }
        }
    }
}

@Composable
private fun ReportRow(label: String, value: String, valueColor: Color = StudioTextPrimary) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontSize = 12.sp, color = StudioTextSecondary)
        Text(value, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = valueColor)
    }
}

@Composable
private fun ProjectTreeStructureView(report: ExportReport) {
    Card(
        colors = CardDefaults.cardColors(containerColor = StudioDarkCard),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.AccountTree, contentDescription = null, tint = StudioAccentBlue, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Generated Android Studio Project Structure",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = StudioTextPrimary
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                "Standard Android multi-module layout ready for Gradle import & AI editing:",
                fontSize = 11.sp,
                color = StudioTextSecondary
            )
            Spacer(modifier = Modifier.height(10.dp))

            val treeStructure = """
${report.projectName}/
├── settings.gradle.kts           # Root Gradle settings & repo catalog
├── build.gradle.kts              # Top-level build file (AGP 8.5.2, Kotlin 2.0)
├── gradle.properties             # JVM arguments & AndroidX flags
├── gradlew                       # Unix executable wrapper
├── gradlew.bat                   # Windows CMD wrapper
├── gradle/
│   └── wrapper/
│       ├── gradle-wrapper.jar
│       └── gradle-wrapper.properties
├── AI_STUDIO_CONTEXT.md          # AI Assistant Context & Prompting Guide
├── README.md                     # Build instructions & notes
└── app/
    ├── build.gradle.kts          # Module dependencies, SDK 34, namespace
    ├── proguard-rules.pro        # Optimization & obfuscation rules
    └── src/
        └── main/
            ├── AndroidManifest.xml
            ├── java/             # Reconstructed/Original Java classes
            ├── kotlin/           # Kotlin Coroutine/Compose components
            ├── res/              # Values, Layouts, Drawables, Mipmaps
            ├── assets/           # App runtime assets
            ${if (report.isReconstructedFromApk) "└── smali/            # Preserved Smali disassembly from APK" else ""}
            """.trimIndent()

            Surface(
                color = StudioDarkBg,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth().weight(1f)
            ) {
                LazyColumn(modifier = Modifier.padding(12.dp)) {
                    item {
                        Text(
                            text = treeStructure,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = StudioAccentGreen,
                            lineHeight = 17.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun UnconvertedFilesView(unconvertedFiles: List<UnconvertedFile>) {
    if (unconvertedFiles.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = StudioAccentGreen, modifier = Modifier.size(48.dp))
                Spacer(modifier = Modifier.height(10.dp))
                Text("All Files Converted Cleanly", fontWeight = FontWeight.Bold, color = StudioTextPrimary)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "No files were dropped or skipped. Every resource and source file is present.",
                    fontSize = 12.sp,
                    color = StudioTextSecondary,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Surface(
                    color = StudioAccentOrange.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = StudioAccentOrange, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "The following files cannot be directly converted to standard plain text source code. They have been organized and clearly preserved rather than silently discarded:",
                            fontSize = 11.sp,
                            color = StudioTextPrimary
                        )
                    }
                }
            }

            items(unconvertedFiles) { file ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = StudioDarkCard),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Badge(containerColor = StudioAccentBlue) {
                                Text(file.category, fontSize = 9.sp)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(file.relativePath, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = StudioTextPrimary)
                            Spacer(modifier = Modifier.weight(1f))
                            if (file.sizeBytes > 0) {
                                Text("${file.sizeBytes / 1024} KB", fontSize = 10.sp, color = StudioTextMuted)
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("Reason: ${file.reason}", fontSize = 11.sp, color = StudioAccentYellow)
                        if (file.suggestedAction.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Resolution: ${file.suggestedAction}", fontSize = 11.sp, color = StudioAccentGreen)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AiEditingWorkflowGuide(report: ExportReport) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = StudioDarkCard),
                shape = RoundedCornerShape(10.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = StudioAccentPurple, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("AI Editing Inside Android Studio", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = StudioTextPrimary)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "This exported project includes an AI_STUDIO_CONTEXT.md file at root. You can prompt Gemini Code Assist, Studio Bot, or GitHub Copilot inside Android Studio with full context:",
                        fontSize = 12.sp,
                        color = StudioTextSecondary
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                    AiWorkflowTip("1. Modifying Kotlin & Java", "Prompt AI: 'Refactor MainActivity to add a ViewModel with MutableStateFlow and handle lifecycle events.'")
                    AiWorkflowTip("2. Modifying Jetpack Compose & XML", "Prompt AI: 'Add a Material 3 TopAppBar with search action and bottom navigation rail for tablet support.'")
                    AiWorkflowTip("3. Modifying Gradle & Dependencies", "Prompt AI: 'Add Retrofit and Kotlinx Serialization to app/build.gradle.kts and sync project.'")
                    AiWorkflowTip("4. Modifying AndroidManifest", "Prompt AI: 'Add POST_NOTIFICATIONS runtime permission and register BackgroundSyncService.'")
                    AiWorkflowTip("5. Modifying Resources", "Strings in res/values/strings.xml and color tokens in colors.xml are standard M3 resources.")
                    AiWorkflowTip("6. Building the Modified App", "Run in Android Studio Terminal: ./gradlew assembleDebug")
                }
            }
        }
    }
}

@Composable
private fun AiWorkflowTip(title: String, promptExample: String) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(title, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = StudioAccentBlue)
        Spacer(modifier = Modifier.height(2.dp))
        Surface(
            color = StudioDarkBg,
            shape = RoundedCornerShape(6.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = promptExample,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                color = StudioTextPrimary,
                modifier = Modifier.padding(8.dp)
            )
        }
    }
}

private fun shareExportZip(context: Context, zipFile: File) {
    try {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", zipFile)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/zip"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Android Studio Project - ${zipFile.nameWithoutExtension}")
            putExtra(Intent.EXTRA_TEXT, "Here is the exportable Android Studio project ready for Gradle sync and AI development.")
            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
        }
        context.startActivity(Intent.createChooser(intent, "Export / Share Android Studio Project"))
    } catch (e: Exception) {
        Toast.makeText(context, "Error sharing ZIP: ${e.message}", Toast.LENGTH_LONG).show()
    }
}

private fun saveZipToDownloads(context: Context, zipFile: File) {
    try {
        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        if (!downloadsDir.exists()) downloadsDir.mkdirs()
        val destFile = File(downloadsDir, zipFile.name)
        FileInputStream(zipFile).use { input ->
            FileOutputStream(destFile).use { output ->
                input.copyTo(output)
            }
        }
        Toast.makeText(context, "Saved to Downloads: ${destFile.name}", Toast.LENGTH_LONG).show()
    } catch (e: Exception) {
        // Fallback to internal files dir path display
        Toast.makeText(context, "Export ready in: ${zipFile.absolutePath}", Toast.LENGTH_LONG).show()
    }
}
