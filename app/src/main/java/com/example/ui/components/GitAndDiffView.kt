package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
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
import com.example.engine.CodeDiffEngine
import com.example.engine.GitEngine
import com.example.model.DiffLine
import com.example.model.DiffType
import com.example.model.GitCommitItem
import com.example.model.GitStatusItem
import com.example.ui.theme.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun GitAndDiffView(
    projectRoot: File,
    modifier: Modifier = Modifier
) {
    val git = remember(projectRoot) { GitEngine(projectRoot) }
    var commits by remember { mutableStateOf(git.getCommits()) }
    var changedFiles by remember { mutableStateOf(git.getStatus()) }
    var selectedTab by remember { mutableIntStateOf(0) } // 0: History, 1: Diff, 2: Commit

    var commitMessage by remember { mutableStateOf("") }
    var commitAuthor by remember { mutableStateOf("Android Studio Dev <developer@studio.local>") }

    // Sample diff demo state if needed
    var selectedFileForDiff by remember { mutableStateOf(changedFiles.firstOrNull()?.path ?: "MainActivity.kt") }

    fun refreshGit() {
        commits = git.getCommits()
        changedFiles = git.getStatus()
    }

    Column(modifier = modifier.fillMaxSize().background(StudioDarkBg)) {
        // Git Header Bar
        Surface(color = StudioDarkSurface, tonalElevation = 3.dp, modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.AccountTree, contentDescription = null, tint = StudioAccentPurple)
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text("Git Version Control", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = StudioTextPrimary)
                    Text("Branch: main • ${commits.size} commits • ${changedFiles.size} modified", fontSize = 11.sp, color = StudioTextSecondary)
                }
                Spacer(modifier = Modifier.weight(1f))
                IconButton(onClick = { refreshGit() }) {
                    Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = StudioAccentBlue)
                }
            }
        }

        // Sub tabs
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = StudioDarkSurface,
            contentColor = StudioAccentBlue
        ) {
            Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Commits Log", fontSize = 12.sp) })
            Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Diff Viewer", fontSize = 12.sp) })
            Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }, text = { Text("Stage & Commit", fontSize = 12.sp) })
        }

        when (selectedTab) {
            0 -> CommitsLogTab(commits)
            1 -> DiffViewerTab(projectRoot, selectedFileForDiff, changedFiles) { selectedFileForDiff = it }
            2 -> CommitFormTab(
                commitMessage = commitMessage,
                onMessageChange = { commitMessage = it },
                author = commitAuthor,
                onAuthorChange = { commitAuthor = it },
                changedFiles = changedFiles,
                onCommit = {
                    if (commitMessage.isNotBlank()) {
                        git.commit(commitMessage.trim(), commitAuthor.trim())
                        commitMessage = ""
                        refreshGit()
                        selectedTab = 0
                    }
                }
            )
        }
    }
}

@Composable
private fun CommitsLogTab(commits: List<GitCommitItem>) {
    val sdf = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()) }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(commits) { c ->
            Card(
                colors = CardDefaults.cardColors(containerColor = StudioDarkCard),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = c.message,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = StudioTextPrimary,
                            modifier = Modifier.weight(1f)
                        )
                        Badge(containerColor = StudioDarkSurface) {
                            Text(c.hash.take(7), fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = StudioAccentBlue)
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Author: ${c.author}", fontSize = 11.sp, color = StudioTextSecondary)
                    Text("Date: ${sdf.format(Date(c.timestamp))} • ${c.changedFilesCount} files changed", fontSize = 11.sp, color = StudioTextMuted)
                }
            }
        }
    }
}

@Composable
private fun DiffViewerTab(
    projectRoot: File,
    selectedFile: String,
    changedFiles: List<GitStatusItem>,
    onSelectFile: (String) -> Unit
) {
    // Generate original vs modified text for visualization
    val fileObj = File(projectRoot, selectedFile)
    val modifiedText = if (fileObj.exists()) fileObj.readText() else "class Example {\n    fun start() {}\n}"
    val originalText = remember(selectedFile, modifiedText) {
        // synthesize original base version
        modifiedText.lines().filterIndexed { idx, _ -> idx % 4 != 0 }.joinToString("\n")
    }

    val diffLines = remember(originalText, modifiedText) {
        CodeDiffEngine.computeDiff(originalText, modifiedText)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // File selection header
        Surface(color = StudioDarkCard, modifier = Modifier.fillMaxWidth().padding(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Viewing Diff: ", fontSize = 12.sp, color = StudioTextSecondary)
                Text(selectedFile, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = StudioAccentBlue)
                Spacer(modifier = Modifier.weight(1f))
                Badge(containerColor = StudioAccentGreen) {
                    Text("+${diffLines.count { it.type == DiffType.ADDED }}", fontSize = 10.sp)
                }
                Spacer(modifier = Modifier.width(4.dp))
                Badge(containerColor = StudioAccentRed) {
                    Text("-${diffLines.count { it.type == DiffType.REMOVED }}", fontSize = 10.sp)
                }
            }
        }

        // Diff line-by-line renderer
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(StudioDarkBg)
        ) {
            items(diffLines) { line ->
                val (bgColor, textColor, prefix) = when (line.type) {
                    DiffType.ADDED -> Triple(StudioDiffAdded, StudioAccentGreen, "+")
                    DiffType.REMOVED -> Triple(StudioDiffRemoved, StudioAccentRed, "-")
                    DiffType.UNCHANGED -> Triple(StudioDarkBg, StudioTextPrimary, " ")
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(bgColor)
                        .horizontalScroll(rememberScrollState())
                        .padding(vertical = 1.dp, horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${line.lineNumberOriginal ?: " "}".padStart(3),
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = StudioLineNumber,
                        modifier = Modifier.width(28.dp)
                    )
                    Text(
                        text = "${line.lineNumberModified ?: " "}".padStart(3),
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = StudioLineNumber,
                        modifier = Modifier.width(28.dp)
                    )
                    Text(
                        text = " $prefix ",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = textColor,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = line.text,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = textColor
                    )
                }
            }
        }
    }
}

@Composable
private fun CommitFormTab(
    commitMessage: String,
    onMessageChange: (String) -> Unit,
    author: String,
    onAuthorChange: (String) -> Unit,
    changedFiles: List<GitStatusItem>,
    onCommit: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("CREATE GIT COMMIT", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = StudioAccentBlue, letterSpacing = 1.sp)

        OutlinedTextField(
            value = commitMessage,
            onValueChange = onMessageChange,
            label = { Text("Commit Message") },
            placeholder = { Text("e.g. Refactor MainActivity and add Jetpack Compose layout") },
            modifier = Modifier.fillMaxWidth().height(80.dp)
        )

        OutlinedTextField(
            value = author,
            onValueChange = onAuthorChange,
            label = { Text("Author Signature") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Button(
            onClick = onCommit,
            enabled = commitMessage.isNotBlank(),
            modifier = Modifier.fillMaxWidth().height(42.dp),
            colors = ButtonDefaults.buttonColors(containerColor = StudioAccentGreen)
        ) {
            Icon(Icons.Default.Check, contentDescription = null)
            Spacer(modifier = Modifier.width(6.dp))
            Text("Commit Changes")
        }

        Spacer(modifier = Modifier.height(6.dp))
        Text("Changed files in workspace (${changedFiles.size})", fontSize = 12.sp, color = StudioTextSecondary)

        LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            items(changedFiles) { f ->
                Surface(color = StudioDarkCard, shape = RoundedCornerShape(6.dp), modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("M", color = StudioAccentYellow, fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.width(20.dp))
                        Text(f.path, color = StudioTextPrimary, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                    }
                }
            }
        }
    }
}
