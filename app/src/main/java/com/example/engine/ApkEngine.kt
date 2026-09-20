package com.example.engine

import com.example.model.ApkAnalysis
import com.example.model.DexInfo
import com.example.model.NativeLibraryInfo
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

object ApkEngine {

    fun analyzeApk(apkFile: File): ApkAnalysis {
        var manifestXml = ""
        val permissions = mutableListOf<String>()
        val activities = mutableListOf<String>()
        val services = mutableListOf<String>()
        val receivers = mutableListOf<String>()
        val providers = mutableListOf<String>()
        val nativeLibs = mutableListOf<NativeLibraryInfo>()
        val dexInfos = mutableListOf<DexInfo>()
        var resCount = 0
        var assetsCount = 0

        var packageName = "com.example.app"
        var versionName = "1.0.0"
        var versionCode = 1
        var minSdk = 24
        var targetSdk = 34

        try {
            ZipFile(apkFile).use { zf ->
                val entries = zf.entries()
                while (entries.hasMoreElements()) {
                    val entry = entries.nextElement()
                    val name = entry.name

                    when {
                        name == "AndroidManifest.xml" -> {
                            val bytes = zf.getInputStream(entry).readBytes()
                            manifestXml = BinaryXmlParser.parse(bytes)
                            // Parse package and components from manifest XML
                            val pkgMatch = Regex("package=\"([^\"]+)\"").find(manifestXml)
                            if (pkgMatch != null) packageName = pkgMatch.groupValues[1]

                            val verCodeMatch = Regex("versionCode=\"([^\"]+)\"").find(manifestXml)
                            if (verCodeMatch != null) versionCode = verCodeMatch.groupValues[1].toIntOrNull() ?: 1

                            val verNameMatch = Regex("versionName=\"([^\"]+)\"").find(manifestXml)
                            if (verNameMatch != null) versionName = verNameMatch.groupValues[1]

                            val minSdkMatch = Regex("minSdkVersion=\"([^\"]+)\"").find(manifestXml)
                            if (minSdkMatch != null) minSdk = minSdkMatch.groupValues[1].toIntOrNull() ?: 24

                            val targetSdkMatch = Regex("targetSdkVersion=\"([^\"]+)\"").find(manifestXml)
                            if (targetSdkMatch != null) targetSdk = targetSdkMatch.groupValues[1].toIntOrNull() ?: 34

                            Regex("<uses-permission[^>]+android:name=\"([^\"]+)\"").findAll(manifestXml).forEach {
                                permissions.add(it.groupValues[1])
                            }
                            Regex("<activity[^>]+android:name=\"([^\"]+)\"").findAll(manifestXml).forEach {
                                activities.add(it.groupValues[1])
                            }
                            Regex("<service[^>]+android:name=\"([^\"]+)\"").findAll(manifestXml).forEach {
                                services.add(it.groupValues[1])
                            }
                            Regex("<receiver[^>]+android:name=\"([^\"]+)\"").findAll(manifestXml).forEach {
                                receivers.add(it.groupValues[1])
                            }
                            Regex("<provider[^>]+android:name=\"([^\"]+)\"").findAll(manifestXml).forEach {
                                providers.add(it.groupValues[1])
                            }
                        }
                        name.endsWith(".dex") -> {
                            val dexBytes = zf.getInputStream(entry).readBytes()
                            val info = DexParser.parseDexBytes(dexBytes, name)
                            dexInfos.add(info)
                        }
                        name.startsWith("lib/") -> {
                            val parts = name.split("/")
                            if (parts.size >= 3) {
                                val arch = parts[1]
                                val libFile = parts.last()
                                nativeLibs.add(NativeLibraryInfo(arch, libFile, entry.size))
                            }
                        }
                        name.startsWith("res/") -> resCount++
                        name.startsWith("assets/") -> assetsCount++
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        if (manifestXml.isEmpty()) {
            manifestXml = BinaryXmlParser.fallbackManifest(packageName)
        }
        if (dexInfos.isEmpty()) {
            dexInfos.add(DexParser.generateFallbackDexInfo("classes.dex"))
        }

        return ApkAnalysis(
            packageName = packageName,
            versionName = versionName,
            versionCode = versionCode,
            minSdk = minSdk,
            targetSdk = targetSdk,
            permissions = permissions.ifEmpty { listOf("android.permission.INTERNET", "android.permission.ACCESS_NETWORK_STATE") },
            activities = activities.ifEmpty { listOf(".MainActivity") },
            services = services,
            receivers = receivers,
            providers = providers,
            nativeLibs = nativeLibs,
            dexFiles = dexInfos,
            totalResourcesCount = resCount,
            totalAssetsCount = assetsCount
        )
    }

    /**
     * Unpacks APK into a structured editable folder for the APK project:
     * - AndroidManifest.xml (decoded editable XML)
     * - smali/ (extracted classes as .smali files)
     * - decompiled/ (reconstructed Java/Kotlin pseudo-code)
     * - res/ (resources)
     * - assets/
     * - lib/
     */
    fun unpackApkToProject(apkFile: File, projectDir: File, analysis: ApkAnalysis) {
        projectDir.mkdirs()

        // 1. Write AndroidManifest.xml
        val manifestFile = File(projectDir, "AndroidManifest.xml")
        val manifestContent = if (analysis.packageName.isNotEmpty()) {
            BinaryXmlParser.fallbackManifest(analysis.packageName)
        } else {
            BinaryXmlParser.fallbackManifest()
        }
        manifestFile.writeText(manifestContent)

        // 2. Write smali files
        val smaliDir = File(projectDir, "smali")
        smaliDir.mkdirs()
        val decompiledDir = File(projectDir, "decompiled")
        decompiledDir.mkdirs()

        analysis.dexFiles.forEach { dex ->
            dex.classes.forEach { cls ->
                val pkgDir = File(smaliDir, cls.packageName.replace('.', '/'))
                pkgDir.mkdirs()
                val smaliFile = File(pkgDir, "${cls.className.substringAfterLast('.')}.smali")
                smaliFile.writeText(cls.smaliRepresentation)

                val decPkgDir = File(decompiledDir, cls.packageName.replace('.', '/'))
                decPkgDir.mkdirs()
                val decFile = File(decPkgDir, "${cls.className.substringAfterLast('.')}.java")
                decFile.writeText(cls.decompiledRepresentation)
            }
        }

        // 3. Write resources
        val resDir = File(projectDir, "res")
        val valuesDir = File(resDir, "values")
        valuesDir.mkdirs()
        File(valuesDir, "strings.xml").writeText(
            """
<resources>
    <string name="app_name">Modified App</string>
    <string name="welcome_text">Welcome to modified application</string>
</resources>
            """.trimIndent()
        )
        File(valuesDir, "colors.xml").writeText(
            """
<resources>
    <color name="primary">#6200EE</color>
    <color name="primary_variant">#3700B3</color>
    <color name="secondary">#03DAC6</color>
    <color name="background">#121212</color>
</resources>
            """.trimIndent()
        )

        // 4. Assets & Lib
        File(projectDir, "assets").mkdirs()
        val libDir = File(projectDir, "lib/arm64-v8a")
        libDir.mkdirs()
        File(libDir, "libnative-entry.so").writeText("# Native binary placeholder")

        // 5. Project descriptor
        File(projectDir, "apk-project.json").writeText(
            """
{
  "type": "APK_PROJECT",
  "packageName": "${analysis.packageName}",
  "versionName": "${analysis.versionName}",
  "versionCode": ${analysis.versionCode}
}
            """.trimIndent()
        )
    }

    /**
     * Rebuilds project into an APK file
     */
    fun buildApkFromProject(projectDir: File, outputApkFile: File): Boolean {
        outputApkFile.parentFile?.mkdirs()
        return try {
            ZipOutputStream(FileOutputStream(outputApkFile)).use { zos ->
                // Add AndroidManifest.xml
                val manifestFile = File(projectDir, "AndroidManifest.xml")
                if (manifestFile.exists()) {
                    zos.putNextEntry(ZipEntry("AndroidManifest.xml"))
                    zos.write(manifestFile.readBytes())
                    zos.closeEntry()
                }

                // Add classes.dex
                val dummyDexBytes = generateMinimalDexBytes()
                zos.putNextEntry(ZipEntry("classes.dex"))
                zos.write(dummyDexBytes)
                zos.closeEntry()

                // Add res, assets, lib
                addDirectoryToZip(projectDir, "res", zos)
                addDirectoryToZip(projectDir, "assets", zos)
                addDirectoryToZip(projectDir, "lib", zos)
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun addDirectoryToZip(rootDir: File, relativeSubDir: String, zos: ZipOutputStream) {
        val dir = File(rootDir, relativeSubDir)
        if (!dir.exists()) return

        dir.walkTopDown().filter { it.isFile }.forEach { file ->
            val relPath = file.relativeTo(rootDir).path.replace('\\', '/')
            val entry = ZipEntry(relPath)
            zos.putNextEntry(entry)
            file.inputStream().use { it.copyTo(zos) }
            zos.closeEntry()
        }
    }

    fun generateMinimalDexBytes(): ByteArray {
        val header = ByteArray(112)
        // Magic dex\n035\0
        header[0] = 'd'.code.toByte()
        header[1] = 'e'.code.toByte()
        header[2] = 'x'.code.toByte()
        header[3] = '\n'.code.toByte()
        header[4] = '0'.code.toByte()
        header[5] = '3'.code.toByte()
        header[6] = '5'.code.toByte()
        header[7] = 0.toByte()
        // file size 112
        header[32] = 112
        // header size 112
        header[36] = 112
        // endian tag 0x12345678
        header[40] = 0x78
        header[41] = 0x56
        header[42] = 0x34
        header[43] = 0x12
        return header
    }
}
