package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
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
import com.example.engine.*
import com.example.model.*
import com.example.ui.components.*
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import java.io.File

enum class WorkspaceTool(val title: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    EDITOR("Editor", Icons.Default.Code),
    AI_FEATURE("AI Feature", Icons.Default.AutoAwesome),
    DESIGNER("UI Designer", Icons.Default.DesignServices),
    MANIFEST("Manifest", Icons.Default.Security),
    ANALYZER("APK Analyzer", Icons.Default.Assessment),
    BUILD("Build & APK", Icons.Default.Build),
    TERMINAL("Terminal", Icons.Default.Terminal),
    GIT("Git & Diff", Icons.Default.AccountTree)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudioWorkspaceScreen(
    project: StudioProject,
    projectManager: ProjectManager,
    onBackToDashboard: () -> Unit,
    modifier: Modifier = Modifier
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()

    var activeTool by remember { mutableStateOf(WorkspaceTool.EDITOR) }
    var fileTree by remember { mutableStateOf(projectManager.getProjectFileTree(project.rootDir)) }

    // Open editor tabs
    val openTabs = remember { mutableStateListOf<EditorTab>() }
    var activeTabPath by remember { mutableStateOf<String?>(null) }

    // Search dialog
    var showGlobalSearchDialog by remember { mutableStateOf(false) }
    var globalSearchQuery by remember { mutableStateOf("") }
    var globalSearchResults by remember { mutableStateOf<List<ProjectManager.SearchResult>>(emptyList()) }

    // Export to Android Studio dialog
    var showExportDialog by remember { mutableStateOf(false) }

    if (showExportDialog) {
        ExportToAndroidStudioDialog(
            project = project,
            onDismiss = { showExportDialog = false }
        )
    }

    // APK Analysis info for APK projects
    val apkAnalysis = remember(project.id) {
        val dummyApk = File(project.rootDir, "${project.name}.apk")
        if (!dummyApk.exists()) {
            ApkEngine.buildApkFromProject(project.rootDir, dummyApk)
        }
        ApkEngine.analyzeApk(dummyApk)
    }

    // Helper to open file
    fun openFile(fileNode: ProjectFile) {
        if (fileNode.isDirectory) return
        val existing = openTabs.find { it.projectFile.relativePath == fileNode.relativePath }
        if (existing == null) {
            val content = try { fileNode.file.readText() } catch (e: Exception) { "// Binary file" }
            openTabs.add(EditorTab(fileNode, content))
        }
        activeTabPath = fileNode.relativePath
        activeTool = WorkspaceTool.EDITOR
        coroutineScope.launch { drawerState.close() }
    }

    // Initialize with main file open
    LaunchedEffect(project.id) {
        val defaultFile = project.rootDir.walkTopDown().firstOrNull {
            it.name == "MainActivity.kt" || it.name == "MainActivity.java" || it.name == "AndroidManifest.xml"
        }
        if (defaultFile != null) {
            val rel = defaultFile.relativeTo(project.rootDir).path.replace('\\', '/')
            openFile(ProjectFile(defaultFile, defaultFile.name, rel, false, defaultFile.extension))
        }
    }

    fun saveActiveTab() {
        val current = openTabs.find { it.projectFile.relativePath == activeTabPath } ?: return
        try {
            current.projectFile.file.writeText(current.content)
            val idx = openTabs.indexOf(current)
            if (idx != -1) {
                openTabs[idx] = current.copy(isDirty = false)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = StudioDarkSurface,
                modifier = Modifier.width(300.dp)
            ) {
                ProjectTreeView(
                    rootNode = fileTree,
                    selectedFilePath = activeTabPath,
                    onFileClick = { openFile(it) },
                    onCreateFile = { parent, name ->
                        File(parent, name).createNewFile()
                        fileTree = projectManager.getProjectFileTree(project.rootDir)
                    },
                    onCreateFolder = { parent, name ->
                        File(parent, name).mkdirs()
                        fileTree = projectManager.getProjectFileTree(project.rootDir)
                    },
                    onDeleteFile = { target ->
                        if (target.isDirectory) target.deleteRecursively() else target.delete()
                        openTabs.removeAll { it.projectFile.file == target }
                        fileTree = projectManager.getProjectFileTree(project.rootDir)
                    },
                    onRenameFile = { target, newName ->
                        val dest = File(target.parentFile, newName)
                        target.renameTo(dest)
                        fileTree = projectManager.getProjectFileTree(project.rootDir)
                    }
                )
            }
        }
    ) {
        Scaffold(
            containerColor = StudioDarkBg,
            topBar = {
                TopAppBar(
                    navigationIcon = {
                        IconButton(onClick = { coroutineScope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "Explorer Drawer", tint = StudioTextPrimary)
                        }
                    },
                    title = {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(project.name, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = StudioTextPrimary)
                                Spacer(modifier = Modifier.width(6.dp))
                                Badge(containerColor = if (project.type == ProjectType.SOURCE_PROJECT) StudioAccentBlue else StudioAccentOrange) {
                                    Text(if (project.type == ProjectType.SOURCE_PROJECT) "SRC" else "APK", fontSize = 9.sp)
                                }
                            }
                            Text(project.packageName, fontSize = 10.sp, color = StudioTextSecondary)
                        }
                    },
                    actions = {
                        IconButton(onClick = { activeTool = WorkspaceTool.AI_FEATURE }) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = "AI Feature Studio", tint = StudioAccentPurple)
                        }
                        IconButton(onClick = { showGlobalSearchDialog = true }) {
                            Icon(Icons.Default.Search, contentDescription = "Find in Files", tint = StudioTextSecondary)
                        }
                        IconButton(onClick = { saveActiveTab() }) {
                            Icon(Icons.Default.Save, contentDescription = "Save File", tint = StudioAccentGreen)
                        }
                        IconButton(onClick = { showExportDialog = true }) {
                            Icon(Icons.Default.CloudDownload, contentDescription = "Export to Android Studio", tint = StudioAccentBlue)
                        }
                        IconButton(onClick = { activeTool = WorkspaceTool.BUILD }) {
                            Icon(Icons.Default.PlayArrow, contentDescription = "Build Project", tint = StudioAccentGreen)
                        }
                        IconButton(onClick = onBackToDashboard) {
                            Icon(Icons.Default.Home, contentDescription = "Dashboard", tint = StudioTextSecondary)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = StudioDarkSurface)
                )
            },
            bottomBar = {
                Surface(
                    color = StudioDarkSurface,
                    modifier = Modifier.fillMaxWidth().height(60.dp)
                ) {
                    LazyRow(
                        modifier = Modifier.fillMaxSize().padding(horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(WorkspaceTool.values()) { tool ->
                            val isSelected = activeTool == tool
                            Surface(
                                color = if (isSelected) StudioDarkCard else Color.Transparent,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .clickable { activeTool = tool }
                                    .padding(vertical = 4.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        tool.icon,
                                        contentDescription = tool.title,
                                        modifier = Modifier.size(20.dp),
                                        tint = if (isSelected) StudioAccentBlue else StudioTextMuted
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        tool.title,
                                        fontSize = 10.sp,
                                        color = if (isSelected) StudioAccentBlue else StudioTextMuted,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            }
                        }
                    }
                }
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                // Active Editor Tab Strip when Editor is selected
                if (activeTool == WorkspaceTool.EDITOR && openTabs.isNotEmpty()) {
                    Surface(color = StudioDarkCard, modifier = Modifier.fillMaxWidth().height(36.dp)) {
                        LazyRow(
                            modifier = Modifier.fillMaxSize().padding(horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            items(openTabs) { tab ->
                                val isActive = tab.projectFile.relativePath == activeTabPath
                                Surface(
                                    color = if (isActive) StudioDarkSurface else StudioDarkBg,
                                    shape = RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp),
                                    modifier = Modifier
                                        .height(30.dp)
                                        .clickable { activeTabPath = tab.projectFile.relativePath }
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 8.dp)
                                    ) {
                                        Text(
                                            text = tab.projectFile.name + if (tab.isDirty) " *" else "",
                                            fontSize = 11.sp,
                                            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isActive) StudioAccentBlue else StudioTextSecondary
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        IconButton(
                                            onClick = {
                                                val idx = openTabs.indexOf(tab)
                                                openTabs.remove(tab)
                                                if (activeTabPath == tab.projectFile.relativePath) {
                                                    activeTabPath = openTabs.getOrNull(maxOf(0, idx - 1))?.projectFile?.relativePath
                                                }
                                            },
                                            modifier = Modifier.size(16.dp)
                                        ) {
                                            Icon(Icons.Default.Close, contentDescription = "Close Tab", tint = StudioTextMuted, modifier = Modifier.size(12.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Active Workspace Tool Body
                val activeTab = openTabs.find { it.projectFile.relativePath == activeTabPath }
                when (activeTool) {
                    WorkspaceTool.EDITOR -> {
                        if (activeTab != null) {
                            CodeEditorView(
                                tab = activeTab,
                                onContentChange = { newContent ->
                                    val idx = openTabs.indexOf(activeTab)
                                    if (idx != -1) {
                                        openTabs[idx] = activeTab.copy(content = newContent, isDirty = true)
                                    }
                                },
                                onOrganizeImports = {
                                    val organized = RefactoringEngine.organizeImports(activeTab.projectFile.file)
                                    val idx = openTabs.indexOf(activeTab)
                                    if (idx != -1) {
                                        openTabs[idx] = activeTab.copy(content = organized, isDirty = false)
                                    }
                                },
                                onRenameSymbol = { old, new ->
                                    RefactoringEngine.renameSymbol(project, old, new)
                                    // Refresh active tab
                                    val refreshed = activeTab.projectFile.file.readText()
                                    val idx = openTabs.indexOf(activeTab)
                                    if (idx != -1) {
                                        openTabs[idx] = activeTab.copy(content = refreshed, isDirty = false)
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            )
                        } else {
                            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Default.FolderOpen, contentDescription = null, tint = StudioTextMuted, modifier = Modifier.size(48.dp))
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("No file open. Select a file from Project Explorer.", color = StudioTextSecondary, fontSize = 13.sp)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Button(onClick = { coroutineScope.launch { drawerState.open() } }) {
                                        Text("Open Explorer")
                                    }
                                }
                            }
                        }
                    }
                    WorkspaceTool.AI_FEATURE -> {
                        AiFeatureEngineView(
                            project = project,
                            onFeatureApplied = {
                                fileTree = projectManager.getProjectFileTree(project.rootDir)
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    WorkspaceTool.DESIGNER -> {
                        VisualUiDesigner(
                            isComposeProject = project.isCompose,
                            onExportCode = { code, isCompose ->
                                // Insert into active tab or create new layout file
                                if (activeTab != null) {
                                    val newContent = activeTab.content + "\n\n" + code
                                    val idx = openTabs.indexOf(activeTab)
                                    if (idx != -1) {
                                        openTabs[idx] = activeTab.copy(content = newContent, isDirty = true)
                                    }
                                    activeTool = WorkspaceTool.EDITOR
                                }
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    WorkspaceTool.MANIFEST -> {
                        val manifestFile = project.rootDir.walkTopDown().firstOrNull { it.name == "AndroidManifest.xml" }
                        val manifestContent = manifestFile?.readText() ?: BinaryXmlParser.fallbackManifest(project.packageName)
                        ManifestEditorView(
                            manifestContent = manifestContent,
                            onManifestChange = { newContent ->
                                manifestFile?.writeText(newContent)
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    WorkspaceTool.ANALYZER -> {
                        ApkAnalyzerView(
                            analysis = apkAnalysis,
                            onOpenSmaliClass = { className ->
                                val smaliFile = project.rootDir.walkTopDown().firstOrNull {
                                    it.name == "${className.substringAfterLast('.')}.smali"
                                }
                                if (smaliFile != null) {
                                    val rel = smaliFile.relativeTo(project.rootDir).path
                                    openFile(ProjectFile(smaliFile, smaliFile.name, rel, false, "smali"))
                                }
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    WorkspaceTool.BUILD -> {
                        BuildConsoleView(
                            project = project,
                            onNavigateToFileLine = { filePath, line ->
                                val file = File(filePath)
                                if (file.exists()) {
                                    val rel = file.relativeTo(project.rootDir).path
                                    openFile(ProjectFile(file, file.name, rel, false, file.extension))
                                }
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    WorkspaceTool.TERMINAL -> {
                        TerminalView(
                            projectRoot = project.rootDir,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    WorkspaceTool.GIT -> {
                        GitAndDiffView(
                            projectRoot = project.rootDir,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }

    // Global Search in Files Dialog
    if (showGlobalSearchDialog) {
        AlertDialog(
            onDismissRequest = { showGlobalSearchDialog = false },
            title = { Text("Find in Files Across Project") },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = globalSearchQuery,
                            onValueChange = {
                                globalSearchQuery = it
                                globalSearchResults = projectManager.searchInProject(project.rootDir, it)
                            },
                            placeholder = { Text("Search text, class, symbol...") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Text("Results (${globalSearchResults.size})", fontSize = 11.sp, color = StudioTextSecondary)

                    LazyColumn(modifier = Modifier.weight(1f)) {
                        items(globalSearchResults) { item ->
                            Surface(
                                color = StudioDarkCard,
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp)
                                    .clickable {
                                        openFile(item.file)
                                        showGlobalSearchDialog = false
                                    }
                            ) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    Text(
                                        "${item.file.name}:${item.line}",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = StudioAccentBlue
                                    )
                                    Text(
                                        item.lineContent,
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = StudioTextPrimary
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showGlobalSearchDialog = false }) { Text("Close") }
            }
        )
    }
}
