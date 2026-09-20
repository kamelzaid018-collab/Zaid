package com.example.ui.components

import android.content.Context
import android.content.Intent
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.example.engine.BuildEngine
import com.example.model.BuildIssue
import com.example.model.BuildResult
import com.example.model.BuildStep
import com.example.model.IssueSeverity
import com.example.model.StepStatus
import com.example.model.StudioProject
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun BuildConsoleView(
    project: StudioProject,
    onNavigateToFileLine: (String, Int) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var isBuilding by remember { mutableStateOf(false) }
    var buildSteps by remember { mutableStateOf<List<BuildStep>>(emptyList()) }
    var buildResult by remember { mutableStateOf<BuildResult?>(null) }
    var activeTab by remember { mutableIntStateOf(0) } // 0: Steps & Logs, 1: Issues
    var showExportDialog by remember { mutableStateOf(false) }

    if (showExportDialog) {
        ExportToAndroidStudioDialog(
            project = project,
            onDismiss = { showExportDialog = false }
        )
    }

    fun startBuild() {
        if (isBuilding) return
        isBuilding = true
        buildResult = null
        coroutineScope.launch {
            val res = BuildEngine.executeBuild(context, project) { steps ->
                buildSteps = steps
            }
            buildResult = res
            isBuilding = false
        }
    }

    Column(modifier = modifier.fillMaxSize().background(StudioDarkBg)) {
        // Build Top Bar
        Surface(color = StudioDarkSurface, tonalElevation = 3.dp, modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Build, contentDescription = null, tint = StudioAccentBlue)
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "Build Pipeline: ${project.name}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = StudioTextPrimary
                    )
                    Text(
                        text = if (isBuilding) "Running build tasks..." else buildResult?.let { if (it.isSuccess) "BUILD SUCCESSFUL in ${it.totalTimeMs}ms" else "BUILD FAILED" } ?: "Ready to assemble APK",
                        fontSize = 11.sp,
                        color = if (isBuilding) StudioAccentBlue else if (buildResult?.isSuccess == true) StudioAccentGreen else if (buildResult?.isSuccess == false) StudioAccentRed else StudioTextSecondary
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                OutlinedButton(
                    onClick = { showExportDialog = true },
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = StudioAccentBlue),
                    modifier = Modifier.height(36.dp)
                ) {
                    Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Export Studio", fontSize = 12.sp)
                }

                Spacer(modifier = Modifier.width(8.dp))

                Button(
                    onClick = { startBuild() },
                    enabled = !isBuilding,
                    colors = ButtonDefaults.buttonColors(containerColor = StudioAccentGreen),
                    modifier = Modifier.height(36.dp)
                ) {
                    if (isBuilding) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Building...", fontSize = 12.sp)
                    } else {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Build APK", fontSize = 12.sp)
                    }
                }
            }
        }

        // Output Status & Tabs
        val issues = buildResult?.issues ?: emptyList()
        TabRow(
            selectedTabIndex = activeTab,
            containerColor = StudioDarkSurface,
            contentColor = StudioAccentBlue
        ) {
            Tab(
                selected = activeTab == 0,
                onClick = { activeTab = 0 },
                text = { Text("Build Tasks & Log", fontSize = 12.sp) }
            )
            Tab(
                selected = activeTab == 1,
                onClick = { activeTab = 1 },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Issues & Diagnostics", fontSize = 12.sp)
                        if (issues.isNotEmpty()) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Badge(containerColor = if (issues.any { it.severity == IssueSeverity.ERROR }) StudioAccentRed else StudioAccentYellow) {
                                Text("${issues.size}", fontSize = 10.sp)
                            }
                        }
                    }
                }
            )
        }

        if (activeTab == 0) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Generated APK Card if available
                buildResult?.apkFile?.let { apk ->
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = StudioDarkCard),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = StudioAccentGreen)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("APK Generated & Signed Successfully", fontWeight = FontWeight.Bold, color = StudioAccentGreen)
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text("File: ${apk.name} (${apk.length() / 1024} KB)", fontSize = 12.sp, color = StudioTextPrimary)
                                Spacer(modifier = Modifier.height(10.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(
                                        onClick = { installApk(context, apk) },
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                        modifier = Modifier.height(34.dp)
                                    ) {
                                        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Install APK", fontSize = 12.sp)
                                    }

                                    OutlinedButton(
                                        onClick = { shareApk(context, apk) },
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                        modifier = Modifier.height(34.dp)
                                    ) {
                                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Share / Export", fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }
                }

                // Step by Step execution
                items(buildSteps) { step ->
                    Surface(
                        color = StudioDarkCard,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                when (step.status) {
                                    StepStatus.PENDING -> Icon(Icons.Default.HourglassEmpty, contentDescription = null, tint = StudioTextMuted, modifier = Modifier.size(18.dp))
                                    StepStatus.RUNNING -> CircularProgressIndicator(modifier = Modifier.size(16.dp), color = StudioAccentBlue, strokeWidth = 2.dp)
                                    StepStatus.SUCCESS -> Icon(Icons.Default.CheckCircle, contentDescription = null, tint = StudioAccentGreen, modifier = Modifier.size(18.dp))
                                    StepStatus.FAILED -> Icon(Icons.Default.Cancel, contentDescription = null, tint = StudioAccentRed, modifier = Modifier.size(18.dp))
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = step.title,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = StudioTextPrimary
                                )
                            }

                            if (step.logs.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(6.dp))
                                step.logs.forEach { log ->
                                    Text(
                                        text = "  > $log",
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = if (step.status == StepStatus.FAILED) StudioAccentRed else StudioTextSecondary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // Issues & Diagnostics Tab
            LazyColumn(modifier = Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (issues.isEmpty()) {
                    item {
                        Surface(color = StudioDarkCard, shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
                            Text("No issues detected in project.", modifier = Modifier.padding(16.dp), color = StudioAccentGreen)
                        }
                    }
                } else {
                    items(issues) { issue ->
                        IssueCard(issue = issue, onNavigate = { onNavigateToFileLine(issue.filePath, issue.line) })
                    }
                }
            }
        }
    }
}

@Composable
private fun IssueCard(issue: BuildIssue, onNavigate: () -> Unit) {
    val color = when (issue.severity) {
        IssueSeverity.ERROR -> StudioAccentRed
        IssueSeverity.WARNING -> StudioAccentOrange
        IssueSeverity.INFO -> StudioAccentBlue
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = StudioDarkCard),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Error, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("${issue.severity.name}: ${issue.fileName}:${issue.line}", fontWeight = FontWeight.Bold, color = color, fontSize = 12.sp)
                Spacer(modifier = Modifier.weight(1f))
                TextButton(
                    onClick = onNavigate,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    modifier = Modifier.height(28.dp)
                ) {
                    Text("Jump to Line", fontSize = 11.sp, color = StudioAccentBlue)
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(issue.message, fontSize = 12.sp, color = StudioTextPrimary)
            issue.possibleFix?.let { fix ->
                Spacer(modifier = Modifier.height(4.dp))
                Text("Fix suggestion: $fix", fontSize = 11.sp, color = StudioAccentGreen)
            }
        }
    }
}

private fun installApk(context: Context, apkFile: File) {
    try {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", apkFile)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

private fun shareApk(context: Context, apkFile: File) {
    try {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", apkFile)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/vnd.android.package-archive"
            putExtra(Intent.EXTRA_STREAM, uri)
            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
        }
        context.startActivity(Intent.createChooser(intent, "Share Signed APK"))
    } catch (e: Exception) {
        e.printStackTrace()
    }
}
