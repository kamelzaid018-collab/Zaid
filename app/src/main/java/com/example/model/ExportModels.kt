package com.example.model

import java.io.File

enum class ExportStepStatus {
    PENDING,
    RUNNING,
    SUCCESS,
    WARNING,
    FAILED
}

data class ExportStep(
    val id: String,
    val stepNumber: Int,
    val title: String,
    val description: String,
    var status: ExportStepStatus = ExportStepStatus.PENDING,
    var message: String = ""
)

data class UnconvertedFile(
    val relativePath: String,
    val reason: String,
    val category: String,
    val sizeBytes: Long = 0L,
    val suggestedAction: String = ""
)

data class ExportReport(
    val projectName: String,
    val isReconstructedFromApk: Boolean,
    val sourceFilesCount: Int,
    val resourceFilesCount: Int,
    val dependenciesCount: Int,
    val warningsCount: Int,
    val errorsCount: Int,
    val warningsList: List<String> = emptyList(),
    val unconvertedFiles: List<UnconvertedFile> = emptyList(),
    val zipFile: File,
    val targetDirectory: File,
    val isReady: Boolean = true,
    val durationMs: Long = 0L,
    val zipSizeBytes: Long = 0L,
    val detectedArchitecture: String = "Jetpack Compose + Gradle Kotlin DSL"
)
