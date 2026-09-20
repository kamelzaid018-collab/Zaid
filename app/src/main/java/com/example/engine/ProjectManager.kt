package com.example.engine

import android.content.Context
import com.example.model.ApkAnalysis
import com.example.model.ProjectFile
import com.example.model.ProjectTemplate
import com.example.model.ProjectType
import com.example.model.StudioProject
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

class ProjectManager(private val context: Context) {

    private val projectsDir = File(context.filesDir, "studio_projects").apply { mkdirs() }
    private val backupsDir = File(context.filesDir, "studio_backups").apply { mkdirs() }

    fun getTemplates(): List<ProjectTemplate> = listOf(
        ProjectTemplate(
            id = "template_compose",
            name = "Jetpack Compose App",
            description = "Modern Kotlin application with Material 3, ViewModel, and Declarative UI",
            iconName = "compose",
            type = ProjectType.SOURCE_PROJECT,
            isCompose = true
        ),
        ProjectTemplate(
            id = "template_classic",
            name = "Classic Views App",
            description = "XML Layouts, ViewBinding, Java/Kotlin Activities and Fragments",
            iconName = "xml",
            type = ProjectType.SOURCE_PROJECT,
            isCompose = false
        ),
        ProjectTemplate(
            id = "template_native",
            name = "Native C/C++ (NDK)",
            description = "High performance C++20 engine with CMakeLists and JNI bindings",
            iconName = "cpp",
            type = ProjectType.SOURCE_PROJECT,
            hasNative = true
        ),
        ProjectTemplate(
            id = "template_apk",
            name = "APK Reverse Project",
            description = "Reverse engineer DEX, edit Smali, decode resources & rebuild signed APK",
            iconName = "apk",
            type = ProjectType.APK_PROJECT
        )
    )

    fun listProjects(): List<StudioProject> {
        val list = mutableListOf<StudioProject>()
        projectsDir.listFiles()?.filter { it.isDirectory }?.forEach { dir ->
            val metaFile = File(dir, "studio-project.json")
            val isApkProject = File(dir, "apk-project.json").exists() || File(dir, "smali").exists()
            val name = dir.name
            val type = if (isApkProject) ProjectType.APK_PROJECT else ProjectType.SOURCE_PROJECT
            val pkgName = parsePackageName(dir)

            list.add(
                StudioProject(
                    id = dir.name,
                    name = name.replace('_', ' '),
                    type = type,
                    rootDir = dir,
                    packageName = pkgName,
                    isCompose = !isApkProject && File(dir, "app/src/main/java").walkTopDown().any { it.name.contains("Theme") },
                    hasNativeCode = File(dir, "app/src/main/cpp").exists(),
                    lastModified = dir.lastModified()
                )
            )
        }

        // If empty, initialize default sample projects so user has immediate rich projects to explore
        if (list.isEmpty()) {
            val defaultCompose = createProjectFromTemplate(getTemplates()[0], "MyComposeApp", "com.example.composeapp")
            val defaultApk = createProjectFromTemplate(getTemplates()[3], "SampleApkMod", "com.example.modapp")
            list.add(defaultCompose)
            list.add(defaultApk)
        }

        return list.sortedByDescending { it.lastModified }
    }

    fun createProjectFromTemplate(
        template: ProjectTemplate,
        projectName: String,
        packageName: String
    ): StudioProject {
        val safeId = projectName.lowercase().replace(Regex("[^a-z0-9_]"), "_") + "_" + UUID.randomUUID().toString().take(6)
        val projectDir = File(projectsDir, safeId).apply { mkdirs() }

        when (template.id) {
            "template_compose" -> generateComposeProject(projectDir, projectName, packageName)
            "template_classic" -> generateClassicProject(projectDir, projectName, packageName)
            "template_native" -> generateNativeProject(projectDir, projectName, packageName)
            "template_apk" -> generateSampleApkProject(projectDir, projectName, packageName)
            else -> generateComposeProject(projectDir, projectName, packageName)
        }

        val project = StudioProject(
            id = safeId,
            name = projectName,
            type = template.type,
            rootDir = projectDir,
            packageName = packageName,
            isCompose = template.isCompose,
            hasNativeCode = template.hasNative,
            lastModified = System.currentTimeMillis()
        )

        // Initialize Git repo for source projects
        if (template.type == ProjectType.SOURCE_PROJECT) {
            GitEngine(projectDir).initRepo()
        }

        return project
    }

    fun importApkFile(apkFile: File, newProjectName: String): StudioProject {
        val analysis = ApkEngine.analyzeApk(apkFile)
        val safeId = newProjectName.lowercase().replace(Regex("[^a-z0-9_]"), "_") + "_" + UUID.randomUUID().toString().take(6)
        val projectDir = File(projectsDir, safeId).apply { mkdirs() }

        ApkEngine.unpackApkToProject(apkFile, projectDir, analysis)

        return StudioProject(
            id = safeId,
            name = newProjectName,
            type = ProjectType.APK_PROJECT,
            rootDir = projectDir,
            packageName = analysis.packageName,
            versionName = analysis.versionName,
            versionCode = analysis.versionCode,
            minSdk = analysis.minSdk,
            targetSdk = analysis.targetSdk,
            isCompose = false,
            hasNativeCode = analysis.nativeLibs.isNotEmpty(),
            lastModified = System.currentTimeMillis()
        )
    }

    fun getProjectFileTree(rootDir: File): ProjectFile {
        return buildFileNode(rootDir, rootDir)
    }

    private fun buildFileNode(file: File, rootDir: File): ProjectFile {
        val relPath = if (file == rootDir) "" else file.relativeTo(rootDir).path.replace('\\', '/')
        val isDir = file.isDirectory
        val children = if (isDir) {
            file.listFiles()
                ?.filter { !it.name.startsWith(".git") }
                ?.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() }))
                ?.map { buildFileNode(it, rootDir) } ?: emptyList()
        } else {
            emptyList()
        }

        return ProjectFile(
            file = file,
            name = file.name,
            relativePath = relPath,
            isDirectory = isDir,
            extension = file.extension.lowercase(),
            sizeBytes = if (isDir) 0 else file.length(),
            children = children
        )
    }

    fun createBackup(project: StudioProject, backupName: String = "backup_${System.currentTimeMillis()}"): File {
        val backupFile = File(backupsDir, "${project.id}_$backupName.zip")
        ZipOutputStream(FileOutputStream(backupFile)).use { zos ->
            project.rootDir.walkTopDown().filter { it.isFile && !it.path.contains(".git") }.forEach { file ->
                val rel = file.relativeTo(project.rootDir).path.replace('\\', '/')
                zos.putNextEntry(ZipEntry(rel))
                file.inputStream().use { it.copyTo(zos) }
                zos.closeEntry()
            }
        }
        return backupFile
    }

    fun listBackups(projectId: String): List<File> {
        return backupsDir.listFiles()
            ?.filter { it.name.startsWith(projectId) && it.extension == "zip" }
            ?.sortedByDescending { it.lastModified() } ?: emptyList()
    }

    fun restoreBackup(backupZip: File, projectDir: File) {
        ZipFile(backupZip).use { zf ->
            val entries = zf.entries()
            while (entries.hasMoreElements()) {
                val entry = entries.nextElement()
                val target = File(projectDir, entry.name)
                if (entry.isDirectory) {
                    target.mkdirs()
                } else {
                    target.parentFile?.mkdirs()
                    target.outputStream().use { fos ->
                        zf.getInputStream(entry).copyTo(fos)
                    }
                }
            }
        }
    }

    data class SearchResult(
        val file: ProjectFile,
        val line: Int,
        val lineContent: String,
        val matchStartIndex: Int
    )

    fun searchInProject(rootDir: File, query: String, caseSensitive: Boolean = false): List<SearchResult> {
        if (query.trim().isEmpty()) return emptyList()
        val results = mutableListOf<SearchResult>()

        rootDir.walkTopDown().filter { it.isFile && !it.path.contains(".git") && it.length() < 1_000_000 }.forEach { file ->
            try {
                val lines = file.readLines()
                lines.forEachIndexed { idx, line ->
                    val found = if (caseSensitive) line.contains(query) else line.contains(query, ignoreCase = true)
                    if (found) {
                        val rel = file.relativeTo(rootDir).path
                        val projFile = ProjectFile(file, file.name, rel, false, file.extension)
                        val start = if (caseSensitive) line.indexOf(query) else line.indexOf(query, ignoreCase = true)
                        results.add(SearchResult(projFile, idx + 1, line.trim(), start))
                    }
                }
            } catch (e: Exception) {
                // binary file skip
            }
        }
        return results.take(100)
    }

    private fun parsePackageName(dir: File): String {
        val manifest = dir.walkTopDown().firstOrNull { it.name == "AndroidManifest.xml" }
        if (manifest != null && manifest.exists()) {
            val text = manifest.readText()
            val match = Regex("package=\"([^\"]+)\"").find(text)
            if (match != null) return match.groupValues[1]
        }
        return "com.example.app"
    }

    private fun generateComposeProject(projectDir: File, name: String, pkg: String) {
        val pkgPath = pkg.replace('.', '/')
        val javaDir = File(projectDir, "app/src/main/java/$pkgPath").apply { mkdirs() }
        val resDir = File(projectDir, "app/src/main/res").apply { mkdirs() }
        val valuesDir = File(resDir, "values").apply { mkdirs() }

        // MainActivity.kt
        File(javaDir, "MainActivity.kt").writeText(
            """
package $pkg

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppScreen()
                }
            }
        }
    }
}

@Composable
fun AppScreen() {
    var count by remember { mutableStateOf(0) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Welcome to $name",
            fontSize = 22.sp,
            style = MaterialTheme.typography.headlineMedium
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Counter: ${'$'}count",
            fontSize = 18.sp
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = { count++ },
            shape = MaterialTheme.shapes.medium
        ) {
            Text("Increment")
        }
    }
}
            """.trimIndent()
        )

        // AndroidManifest.xml
        File(projectDir, "app/src/main/AndroidManifest.xml").writeText(
            """
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    package="$pkg">

    <uses-permission android:name="android.permission.INTERNET" />

    <application
        android:allowBackup="true"
        android:icon="@mipmap/ic_launcher"
        android:label="$name"
        android:theme="@android:style/Theme.Material.Light.NoActionBar">
        <activity
            android:name=".MainActivity"
            android:exported="true">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
    </application>
</manifest>
            """.trimIndent()
        )

        // build.gradle.kts
        File(projectDir, "app/build.gradle.kts").writeText(
            """
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "$pkg"
    compileSdk = 34

    defaultConfig {
        applicationId = "$pkg"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"
    }

    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.8"
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.activity:activity-compose:1.8.2")
    implementation(platform("androidx.compose:compose-bom:2024.02.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
}
            """.trimIndent()
        )

        // settings.gradle.kts
        File(projectDir, "settings.gradle.kts").writeText(
            """
rootProject.name = "$name"
include(":app")
            """.trimIndent()
        )

        // strings.xml & colors.xml
        File(valuesDir, "strings.xml").writeText(
            """
<resources>
    <string name="app_name">$name</string>
</resources>
            """.trimIndent()
        )
    }

    private fun generateClassicProject(projectDir: File, name: String, pkg: String) {
        val pkgPath = pkg.replace('.', '/')
        val javaDir = File(projectDir, "app/src/main/java/$pkgPath").apply { mkdirs() }
        val layoutDir = File(projectDir, "app/src/main/res/layout").apply { mkdirs() }
        val valuesDir = File(projectDir, "app/src/main/res/values").apply { mkdirs() }

        File(javaDir, "MainActivity.java").writeText(
            """
package $pkg;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {
    private int counter = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        TextView textStatus = findViewById(R.id.textStatus);
        Button buttonAction = findViewById(R.id.buttonAction);

        buttonAction.setOnClickListener(v -> {
            counter++;
            textStatus.setText("Clicks: " + counter);
        });
    }
}
            """.trimIndent()
        )

        File(layoutDir, "activity_main.xml").writeText(
            """
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:orientation="vertical"
    android:gravity="center"
    android:padding="24dp">

    <TextView
        android:id="@+id/textStatus"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:text="Welcome to $name"
        android:textSize="20sp" />

    <Button
        android:id="@+id/buttonAction"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:layout_marginTop="16dp"
        android:text="Click Me" />
</LinearLayout>
            """.trimIndent()
        )

        File(valuesDir, "strings.xml").writeText("<resources><string name=\"app_name\">$name</string></resources>")
        File(projectDir, "app/src/main/AndroidManifest.xml").writeText(
            """
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android" package="$pkg">
    <application android:label="$name" android:theme="@android:style/Theme.DeviceDefault.Light">
        <activity android:name=".MainActivity" android:exported="true">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
    </application>
</manifest>
            """.trimIndent()
        )
        File(projectDir, "app/build.gradle.kts").writeText(
            """
plugins { id("com.android.application") }
android { namespace = "$pkg"; compileSdk = 34 }
            """.trimIndent()
        )
    }

    private fun generateNativeProject(projectDir: File, name: String, pkg: String) {
        generateComposeProject(projectDir, name, pkg)
        val cppDir = File(projectDir, "app/src/main/cpp").apply { mkdirs() }

        File(cppDir, "native-lib.cpp").writeText(
            """
#include <jni.h>
#include <string>

extern "C" JNIEXPORT jstring JNICALL
Java_${pkg.replace('.', '_')}_MainActivity_stringFromJNI(
        JNIEnv* env,
        jobject /* this */) {
    std::string hello = "Hello from Native C++ NDK Engine!";
    return env->NewStringUTF(hello.c_str());
}
            """.trimIndent()
        )

        File(cppDir, "CMakeLists.txt").writeText(
            """
cmake_minimum_required(VERSION 3.22.1)
project("$name")

add_library(
        native-lib
        SHARED
        native-lib.cpp)

find_library(
        log-lib
        log)

target_link_libraries(
        native-lib
        ${'$'}{log-lib})
            """.trimIndent()
        )
    }

    private fun generateSampleApkProject(projectDir: File, name: String, pkg: String) {
        val analysis = ApkAnalysis(
            packageName = pkg,
            versionName = "1.0.0",
            versionCode = 1,
            minSdk = 24,
            targetSdk = 34,
            permissions = listOf(
                "android.permission.INTERNET",
                "android.permission.ACCESS_NETWORK_STATE",
                "android.permission.VIBRATE"
            ),
            activities = listOf(".MainActivity", ".SettingsActivity"),
            services = listOf(".NotificationService"),
            dexFiles = listOf(DexParser.generateFallbackDexInfo("classes.dex")),
            totalResourcesCount = 38,
            totalAssetsCount = 4
        )

        val tempApk = File(context.cacheDir, "sample_$pkg.apk")
        ApkEngine.buildApkFromProject(projectDir, tempApk)
        ApkEngine.unpackApkToProject(tempApk, projectDir, analysis)
        tempApk.delete()
    }
}
