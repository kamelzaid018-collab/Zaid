package com.example.ui.components

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ProjectFile
import com.example.ui.theme.*
import java.io.File

@Composable
fun ProjectTreeView(
    rootNode: ProjectFile,
    selectedFilePath: String?,
    onFileClick: (ProjectFile) -> Unit,
    onCreateFile: (File, String) -> Unit,
    onCreateFolder: (File, String) -> Unit,
    onDeleteFile: (File) -> Unit,
    onRenameFile: (File, String) -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    val expandedPaths = remember { mutableStateMapOf<String, Boolean>() }

    // Dialogs state
    var showNewFileDialog by remember { mutableStateOf(false) }
    var showNewFolderDialog by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var targetFolderForCreation by remember { mutableStateOf(rootNode.file) }
    var fileToRename by remember { mutableStateOf<File?>(null) }
    var inputNameText by remember { mutableStateOf("") }

    // Flatten tree items for display based on expanded states
    fun flattenTree(node: ProjectFile, depth: Int, list: MutableList<Pair<ProjectFile, Int>>) {
        if (node.relativePath.isNotEmpty()) {
            if (searchQuery.isEmpty() || node.name.contains(searchQuery, ignoreCase = true)) {
                list.add(Pair(node, depth))
            }
        }
        val isExpanded = expandedPaths[node.relativePath] ?: (depth < 2)
        if (node.isDirectory && (isExpanded || searchQuery.isNotEmpty())) {
            node.children.forEach { child ->
                flattenTree(child, depth + 1, list)
            }
        }
    }

    val flattened = remember(rootNode, expandedPaths.toMap(), searchQuery) {
        val list = mutableListOf<Pair<ProjectFile, Int>>()
        rootNode.children.forEach { flattenTree(it, 0, list) }
        list
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(StudioDarkSurface)
    ) {
        // Tree Top Bar with Search & Actions
        Surface(
            color = StudioDarkCard,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "PROJECT EXPLORER",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = StudioAccentBlue,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.weight(1f))

                    IconButton(
                        onClick = {
                            targetFolderForCreation = rootNode.file
                            inputNameText = ""
                            showNewFileDialog = true
                        },
                        modifier = Modifier.size(26.dp)
                    ) {
                        Icon(Icons.Default.NoteAdd, contentDescription = "New File", tint = StudioAccentGreen)
                    }

                    IconButton(
                        onClick = {
                            targetFolderForCreation = rootNode.file
                            inputNameText = ""
                            showNewFolderDialog = true
                        },
                        modifier = Modifier.size(26.dp)
                    ) {
                        Icon(Icons.Default.CreateNewFolder, contentDescription = "New Folder", tint = StudioAccentYellow)
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Filter files...", fontSize = 11.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(16.dp)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().height(42.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = StudioDarkBg,
                        unfocusedContainerColor = StudioDarkBg
                    )
                )
            }
        }

        // Tree List
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(vertical = 4.dp)
        ) {
            items(flattened) { (item, depth) ->
                val isSelected = item.relativePath == selectedFilePath
                val isExpanded = expandedPaths[item.relativePath] ?: (depth < 2)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(if (isSelected) StudioDarkCard else Color.Transparent)
                        .clickable {
                            if (item.isDirectory) {
                                expandedPaths[item.relativePath] = !isExpanded
                            } else {
                                onFileClick(item)
                            }
                        }
                        .padding(horizontal = 8.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Spacer(modifier = Modifier.width((depth * 14).dp))

                    // Folder expand/collapse caret
                    if (item.isDirectory) {
                        Icon(
                            if (isExpanded) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowRight,
                            contentDescription = null,
                            tint = StudioTextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    } else {
                        Spacer(modifier = Modifier.width(16.dp))
                    }

                    // File / Folder Icon
                    val (icon, tint) = getFileIconAndColor(item)
                    Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(18.dp))

                    Spacer(modifier = Modifier.width(6.dp))

                    Text(
                        text = item.name,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) StudioAccentBlue else StudioTextPrimary,
                        modifier = Modifier.weight(1f)
                    )

                    // Quick contextual options
                    var showItemMenu by remember { mutableStateOf(false) }
                    Box {
                        IconButton(
                            onClick = { showItemMenu = true },
                            modifier = Modifier.size(20.dp)
                        ) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Menu", tint = StudioTextMuted, modifier = Modifier.size(14.dp))
                        }

                        DropdownMenu(
                            expanded = showItemMenu,
                            onDismissRequest = { showItemMenu = false }
                        ) {
                            if (item.isDirectory) {
                                DropdownMenuItem(
                                    text = { Text("New File Here") },
                                    onClick = {
                                        showItemMenu = false
                                        targetFolderForCreation = item.file
                                        inputNameText = ""
                                        showNewFileDialog = true
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("New Folder Here") },
                                    onClick = {
                                        showItemMenu = false
                                        targetFolderForCreation = item.file
                                        inputNameText = ""
                                        showNewFolderDialog = true
                                    }
                                )
                            }
                            DropdownMenuItem(
                                text = { Text("Rename") },
                                onClick = {
                                    showItemMenu = false
                                    fileToRename = item.file
                                    inputNameText = item.name
                                    showRenameDialog = true
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Delete", color = StudioAccentRed) },
                                onClick = {
                                    showItemMenu = false
                                    onDeleteFile(item.file)
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // New File Dialog
    if (showNewFileDialog) {
        AlertDialog(
            onDismissRequest = { showNewFileDialog = false },
            title = { Text("Create New File in ${targetFolderForCreation.name}") },
            text = {
                OutlinedTextField(
                    value = inputNameText,
                    onValueChange = { inputNameText = it },
                    label = { Text("File Name (e.g. MyScreen.kt, layout.xml)") },
                    singleLine = true
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (inputNameText.isNotBlank()) {
                            onCreateFile(targetFolderForCreation, inputNameText.trim())
                            showNewFileDialog = false
                        }
                    }
                ) { Text("Create") }
            },
            dismissButton = {
                TextButton(onClick = { showNewFileDialog = false }) { Text("Cancel") }
            }
        )
    }

    // New Folder Dialog
    if (showNewFolderDialog) {
        AlertDialog(
            onDismissRequest = { showNewFolderDialog = false },
            title = { Text("Create New Folder in ${targetFolderForCreation.name}") },
            text = {
                OutlinedTextField(
                    value = inputNameText,
                    onValueChange = { inputNameText = it },
                    label = { Text("Folder Name") },
                    singleLine = true
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (inputNameText.isNotBlank()) {
                            onCreateFolder(targetFolderForCreation, inputNameText.trim())
                            showNewFolderDialog = false
                        }
                    }
                ) { Text("Create") }
            },
            dismissButton = {
                TextButton(onClick = { showNewFolderDialog = false }) { Text("Cancel") }
            }
        )
    }

    // Rename Dialog
    if (showRenameDialog && fileToRename != null) {
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = { Text("Rename ${fileToRename?.name}") },
            text = {
                OutlinedTextField(
                    value = inputNameText,
                    onValueChange = { inputNameText = it },
                    label = { Text("New Name") },
                    singleLine = true
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (inputNameText.isNotBlank() && fileToRename != null) {
                            onRenameFile(fileToRename!!, inputNameText.trim())
                            showRenameDialog = false
                        }
                    }
                ) { Text("Rename") }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = false }) { Text("Cancel") }
            }
        )
    }
}

private fun getFileIconAndColor(item: ProjectFile): Pair<ImageVector, Color> {
    if (item.isDirectory) {
        return Pair(Icons.Default.Folder, StudioAccentYellow)
    }
    return when (item.extension.lowercase()) {
        "kt" -> Pair(Icons.Default.Code, StudioAccentPurple)
        "java" -> Pair(Icons.Default.Coffee, StudioAccentOrange)
        "xml" -> Pair(Icons.Default.DataObject, StudioAccentBlue)
        "gradle", "kts" -> Pair(Icons.Default.SettingsApplications, StudioAccentGreen)
        "smali" -> Pair(Icons.Default.Terminal, StudioAccentOrange)
        "cpp", "c", "h" -> Pair(Icons.Default.Memory, StudioAccentBlue)
        "json" -> Pair(Icons.Default.DataArray, StudioAccentYellow)
        "png", "jpg", "webp", "svg" -> Pair(Icons.Default.Image, StudioAccentGreen)
        "so" -> Pair(Icons.Default.SettingsEthernet, StudioAccentRed)
        else -> Pair(Icons.Default.Description, StudioTextSecondary)
    }
}
