package com.example.model

import java.io.File

enum class ProjectType(val displayName: String) {
    SOURCE_PROJECT("Android Source Project"),
    APK_PROJECT("APK Mod Project")
}

data class StudioProject(
    val id: String,
    val name: String,
    val type: ProjectType,
    val rootDir: File,
    val packageName: String,
    val versionName: String = "1.0.0",
    val versionCode: Int = 1,
    val minSdk: Int = 24,
    val targetSdk: Int = 34,
    val compileSdk: Int = 34,
    val isCompose: Boolean = true,
    val hasNativeCode: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val lastModified: Long = System.currentTimeMillis(),
    val buildStatus: String = "Not Built"
)

data class ProjectFile(
    val file: File,
    val name: String,
    val relativePath: String,
    val isDirectory: Boolean,
    val extension: String,
    val sizeBytes: Long = 0,
    val children: List<ProjectFile> = emptyList()
) {
    val isKotlin: Boolean get() = extension.equals("kt", ignoreCase = true)
    val isJava: Boolean get() = extension.equals("java", ignoreCase = true)
    val isXml: Boolean get() = extension.equals("xml", ignoreCase = true)
    val isGradle: Boolean get() = extension.equals("gradle", ignoreCase = true) || name.endsWith(".gradle.kts")
    val isSmali: Boolean get() = extension.equals("smali", ignoreCase = true)
    val isDex: Boolean get() = extension.equals("dex", ignoreCase = true)
    val isCpp: Boolean get() = extension.equals("cpp", ignoreCase = true) || extension.equals("c", ignoreCase = true) || extension.equals("h", ignoreCase = true)
    val isJson: Boolean get() = extension.equals("json", ignoreCase = true)
    val isProperties: Boolean get() = extension.equals("properties", ignoreCase = true)
    val isImage: Boolean get() = extension in listOf("png", "jpg", "jpeg", "webp", "gif")
}

data class EditorTab(
    val projectFile: ProjectFile,
    var content: String,
    val originalContent: String = content,
    var isDirty: Boolean = false,
    var cursorPosition: Int = 0,
    var selectedLine: Int = 1
)

enum class EditorViewMode(val title: String) {
    CODE("Code"),
    VISUAL_UI("UI Designer"),
    MANIFEST("Manifest"),
    RESOURCES("Resources"),
    DEX_SMALI("DEX / Smali"),
    APK_ANALYZER("APK Analyzer"),
    GRADLE_DEPS("Dependencies"),
    DIFF("Diff"),
    IMAGE("Image")
}

enum class StudioBottomPanel(val title: String) {
    NONE("Hide"),
    BUILD("Build"),
    TERMINAL("Terminal"),
    GIT("Git"),
    PROBLEMS("Problems"),
    BACKUPS("Backups")
}

data class ProjectTemplate(
    val id: String,
    val name: String,
    val description: String,
    val iconName: String,
    val type: ProjectType,
    val isCompose: Boolean = true,
    val hasNative: Boolean = false
)
