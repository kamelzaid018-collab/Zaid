package com.example.model

data class DexClassInfo(
    val className: String,
    val superClassName: String,
    val packageName: String,
    val accessFlags: String,
    val interfaces: List<String> = emptyList(),
    val fields: List<DexFieldInfo> = emptyList(),
    val methods: List<DexMethodInfo> = emptyList(),
    val smaliRepresentation: String = "",
    val decompiledRepresentation: String = ""
)

data class DexFieldInfo(
    val name: String,
    val type: String,
    val accessFlags: String
)

data class DexMethodInfo(
    val name: String,
    val returnType: String,
    val parameterTypes: List<String>,
    val accessFlags: String,
    val smaliSnippet: String = "",
    val reconstructedSnippet: String = ""
)

data class DexInfo(
    val dexFileName: String,
    val totalClasses: Int,
    val totalMethods: Int,
    val totalFields: Int,
    val totalStrings: Int,
    val classes: List<DexClassInfo> = emptyList()
)

data class NativeLibraryInfo(
    val arch: String, // e.g. arm64-v8a, armeabi-v7a, x86, x86_64
    val fileName: String,
    val sizeBytes: Long
)

data class ApkAnalysis(
    val packageName: String,
    val versionName: String,
    val versionCode: Int,
    val minSdk: Int,
    val targetSdk: Int,
    val permissions: List<String> = emptyList(),
    val activities: List<String> = emptyList(),
    val services: List<String> = emptyList(),
    val receivers: List<String> = emptyList(),
    val providers: List<String> = emptyList(),
    val nativeLibs: List<NativeLibraryInfo> = emptyList(),
    val dexFiles: List<DexInfo> = emptyList(),
    val totalResourcesCount: Int = 0,
    val totalAssetsCount: Int = 0,
    val certificateSubject: String = "CN=Android, O=Android, C=US",
    val certificateSha256: String = "E3:B0:C4:42:98:FC:1C:14:9A:FB:F4:C8:99:6F:B9:24:27:AE:41:E4:64:9B:93:4C:A4:95:99:1B:78:52:B8:55",
    val hasV1Signature: Boolean = true,
    val hasV2Signature: Boolean = true
)
