package com.example.model

enum class StepStatus {
    PENDING, RUNNING, SUCCESS, FAILED
}

data class BuildStep(
    val id: String,
    val title: String,
    val status: StepStatus = StepStatus.PENDING,
    val logs: List<String> = emptyList(),
    val durationMs: Long = 0
)

data class BuildIssue(
    val filePath: String,
    val fileName: String,
    val line: Int,
    val column: Int = 1,
    val severity: IssueSeverity = IssueSeverity.ERROR,
    val message: String,
    val possibleFix: String = ""
)

enum class IssueSeverity {
    INFO, WARNING, ERROR
}

data class BuildResult(
    val isSuccess: Boolean,
    val apkFile: java.io.File? = null,
    val totalTimeMs: Long = 0,
    val steps: List<BuildStep> = emptyList(),
    val issues: List<BuildIssue> = emptyList()
)
