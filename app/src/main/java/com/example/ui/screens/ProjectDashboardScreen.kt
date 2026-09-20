package com.example.ui.screens

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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine.ProjectManager
import com.example.model.ProjectTemplate
import com.example.model.ProjectType
import com.example.model.StudioProject
import com.example.ui.components.ExportToAndroidStudioDialog
import com.example.ui.components.InstalledAppsPickerDialog
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectDashboardScreen(
    projectManager: ProjectManager,
    onOpenProject: (StudioProject) -> Unit,
    modifier: Modifier = Modifier
) {
    var projects by remember { mutableStateOf(projectManager.listProjects()) }
    var searchQuery by remember { mutableStateOf("") }

    var showNewProjectDialog by remember { mutableStateOf(false) }
    var showImportApkDialog by remember { mutableStateOf(false) }
    var showInstalledAppsDialog by remember { mutableStateOf(false) }
    var projectToExport by remember { mutableStateOf<StudioProject?>(null) }

    if (projectToExport != null) {
        ExportToAndroidStudioDialog(
            project = projectToExport!!,
            onDismiss = { projectToExport = null }
        )
    }

    val filteredProjects = remember(projects, searchQuery) {
        if (searchQuery.isEmpty()) projects
        else projects.filter { it.name.contains(searchQuery, ignoreCase = true) || it.packageName.contains(searchQuery, ignoreCase = true) }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = StudioDarkBg,
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            color = StudioAccentBlue.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.DeveloperMode, contentDescription = null, tint = StudioAccentBlue)
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("Android App Studio", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = StudioTextPrimary)
                            Text("Mobile IDE & Reverse Engineering Suite", fontSize = 11.sp, color = StudioTextSecondary)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = StudioDarkSurface)
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showNewProjectDialog = true },
                containerColor = StudioAccentBlue,
                contentColor = StudioDarkBg,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("New Project", fontWeight = FontWeight.Bold) }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 14.dp, vertical = 8.dp)
        ) {
            // Hero Action Banners
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Surface(
                    color = StudioDarkCard,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .clickable { showNewProjectDialog = true }
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Icon(Icons.Default.CreateNewFolder, contentDescription = null, tint = StudioAccentGreen, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("New Project", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = StudioTextPrimary)
                        Text("Compose & NDK", fontSize = 9.sp, color = StudioTextSecondary)
                    }
                }

                Surface(
                    color = StudioDarkCard,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .clickable { showInstalledAppsDialog = true }
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Icon(Icons.Default.Apps, contentDescription = null, tint = StudioAccentBlue, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Installed Apps", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = StudioTextPrimary)
                        Text("Select from Device", fontSize = 9.sp, color = StudioTextSecondary)
                    }
                }

                Surface(
                    color = StudioDarkCard,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .clickable { showImportApkDialog = true }
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Icon(Icons.Default.DownloadForOffline, contentDescription = null, tint = StudioAccentOrange, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Import APK", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = StudioTextPrimary)
                        Text("Decompile & Smali", fontSize = 9.sp, color = StudioTextSecondary)
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Search and filter
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search your projects...", fontSize = 12.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth().height(48.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = StudioDarkSurface,
                    unfocusedContainerColor = StudioDarkSurface
                )
            )

            Spacer(modifier = Modifier.height(14.dp))

            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "RECENT PROJECTS (${filteredProjects.size})",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = StudioAccentBlue,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.weight(1f))
                IconButton(
                    onClick = { projects = projectManager.listProjects() },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = StudioTextMuted)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Projects List
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f).fillMaxWidth()
            ) {
                items(filteredProjects) { proj ->
                    ProjectCard(
                        project = proj,
                        onClick = { onOpenProject(proj) },
                        onExport = { projectToExport = proj },
                        onDelete = {
                            proj.rootDir.deleteRecursively()
                            projects = projectManager.listProjects()
                        }
                    )
                }
            }
        }
    }

    // New Project Dialog
    if (showNewProjectDialog) {
        NewProjectTemplateDialog(
            templates = projectManager.getTemplates(),
            onDismiss = { showNewProjectDialog = false },
            onCreate = { template, name, pkg ->
                val newProj = projectManager.createProjectFromTemplate(template, name, pkg)
                projects = projectManager.listProjects()
                showNewProjectDialog = false
                onOpenProject(newProj)
            }
        )
    }

    // Import APK Dialog
    if (showImportApkDialog) {
        ImportApkDialog(
            onDismiss = { showImportApkDialog = false },
            onImport = { apkName, pkgName ->
                val template = projectManager.getTemplates().first { it.id == "template_apk" }
                val newProj = projectManager.createProjectFromTemplate(template, apkName, pkgName)
                projects = projectManager.listProjects()
                showImportApkDialog = false
                onOpenProject(newProj)
            }
        )
    }

    // Installed Apps Picker Dialog
    if (showInstalledAppsDialog) {
        InstalledAppsPickerDialog(
            onDismiss = { showInstalledAppsDialog = false },
            onImportApk = { apkFile, appName ->
                val newProj = projectManager.importApkFile(apkFile, appName)
                projects = projectManager.listProjects()
                showInstalledAppsDialog = false
                onOpenProject(newProj)
            }
        )
    }
}

@Composable
fun ProjectCard(
    project: StudioProject,
    onClick: () -> Unit,
    onExport: () -> Unit,
    onDelete: () -> Unit
) {
    val sdf = remember { SimpleDateFormat("MMM d, yyyy • HH:mm", Locale.getDefault()) }

    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = StudioDarkSurface),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Icon
                Surface(
                    color = if (project.type == ProjectType.SOURCE_PROJECT) StudioAccentPurple.copy(alpha = 0.2f) else StudioAccentOrange.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            if (project.type == ProjectType.SOURCE_PROJECT) Icons.Default.Source else Icons.Default.Android,
                            contentDescription = null,
                            tint = if (project.type == ProjectType.SOURCE_PROJECT) StudioAccentPurple else StudioAccentOrange
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = project.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = StudioTextPrimary
                    )
                    Text(
                        text = project.packageName,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = StudioTextSecondary
                    )
                }

                Badge(
                    containerColor = if (project.type == ProjectType.SOURCE_PROJECT) StudioAccentBlue else StudioAccentOrange
                ) {
                    Text(
                        if (project.type == ProjectType.SOURCE_PROJECT) "SOURCE" else "APK MOD",
                        fontSize = 9.sp,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (project.isCompose) {
                    AssistChip(
                        onClick = {},
                        label = { Text("Compose", fontSize = 10.sp) },
                        modifier = Modifier.height(24.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                }
                if (project.hasNativeCode) {
                    AssistChip(
                        onClick = {},
                        label = { Text("C++ NDK", fontSize = 10.sp) },
                        modifier = Modifier.height(24.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                }

                Spacer(modifier = Modifier.weight(1f))

                Text(
                    text = sdf.format(Date(project.lastModified)),
                    fontSize = 11.sp,
                    color = StudioTextMuted
                )

                Spacer(modifier = Modifier.width(6.dp))

                IconButton(
                    onClick = onExport,
                    modifier = Modifier.size(26.dp)
                ) {
                    Icon(
                        Icons.Default.CloudDownload,
                        contentDescription = "Export to Android Studio",
                        tint = StudioAccentBlue,
                        modifier = Modifier.size(17.dp)
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(26.dp)
                ) {
                    Icon(
                        Icons.Default.DeleteOutline,
                        contentDescription = "Delete",
                        tint = StudioTextMuted,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun NewProjectTemplateDialog(
    templates: List<ProjectTemplate>,
    onDismiss: () -> Unit,
    onCreate: (ProjectTemplate, String, String) -> Unit
) {
    var selectedTemplate by remember { mutableStateOf(templates.first()) }
    var projectName by remember { mutableStateOf("MyApplication") }
    var packageName by remember { mutableStateOf("com.example.myapplication") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create New Android Project", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Select Project Architecture & Template:", fontSize = 12.sp, color = StudioTextSecondary)

                templates.forEach { tmpl ->
                    val isSelected = selectedTemplate.id == tmpl.id
                    Surface(
                        color = if (isSelected) StudioDarkCard else StudioDarkBg,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                selectedTemplate = tmpl
                                if (tmpl.type == ProjectType.APK_PROJECT) {
                                    projectName = "ModdedApp"
                                    packageName = "com.example.modapp"
                                }
                            }
                    ) {
                        Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = isSelected, onClick = { selectedTemplate = tmpl })
                            Spacer(modifier = Modifier.width(6.dp))
                            Column {
                                Text(tmpl.name, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = StudioTextPrimary)
                                Text(tmpl.description, fontSize = 11.sp, color = StudioTextSecondary)
                            }
                        }
                    }
                }

                Divider(color = StudioDarkBorder)

                OutlinedTextField(
                    value = projectName,
                    onValueChange = {
                        projectName = it
                        packageName = "com.example." + it.lowercase().replace(Regex("[^a-z0-9]"), "")
                    },
                    label = { Text("Project Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = packageName,
                    onValueChange = { packageName = it },
                    label = { Text("Package Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (projectName.isNotBlank() && packageName.isNotBlank()) {
                        onCreate(selectedTemplate, projectName.trim(), packageName.trim())
                    }
                }
            ) { Text("Create Project") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun ImportApkDialog(
    onDismiss: () -> Unit,
    onImport: (String, String) -> Unit
) {
    var apkName by remember { mutableStateOf("TargetModdedApp") }
    var packageName by remember { mutableStateOf("com.target.application") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Import APK for Reverse Engineering") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "Import an APK file to decompile classes into Smali, inspect DEX headers, edit AndroidManifest.xml, and prepare for re-signing.",
                    fontSize = 12.sp,
                    color = StudioTextSecondary
                )

                OutlinedTextField(
                    value = apkName,
                    onValueChange = { apkName = it },
                    label = { Text("Project Label") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = packageName,
                    onValueChange = { packageName = it },
                    label = { Text("Target Package Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = { onImport(apkName.trim(), packageName.trim()) }) {
                Text("Decompile & Open")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
