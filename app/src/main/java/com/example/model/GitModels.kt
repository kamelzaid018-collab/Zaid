package com.example.model

data class GitCommitItem(
    val hash: String,
    val shortHash: String = hash.take(7),
    val message: String,
    val author: String,
    val timestamp: Long,
    val changedFilesCount: Int
)

data class GitStatusItem(
    val path: String,
    val status: GitFileStatus // MODIFIED, ADDED, DELETED, UNTRACKED
)

enum class GitFileStatus {
    MODIFIED, ADDED, DELETED, UNTRACKED
}

data class DiffLine(
    val lineNumberOriginal: Int?,
    val lineNumberModified: Int?,
    val type: DiffType, // ADDED, REMOVED, UNCHANGED
    val text: String
)

enum class DiffType {
    ADDED, REMOVED, UNCHANGED
}
