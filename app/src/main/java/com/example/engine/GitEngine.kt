package com.example.engine

import com.example.model.DiffLine
import com.example.model.DiffType
import com.example.model.GitCommitItem
import com.example.model.GitFileStatus
import com.example.model.GitStatusItem
import java.io.File
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class GitEngine(private val projectRoot: File) {

    private val gitDir = File(projectRoot, ".git_studio")
    private val commitsFile = File(gitDir, "commits.log")
    private val headFile = File(gitDir, "HEAD")

    init {
        if (!gitDir.exists()) {
            initRepo()
        }
    }

    fun initRepo() {
        gitDir.mkdirs()
        if (!headFile.exists()) {
            headFile.writeText("ref: refs/heads/main\n")
        }
        if (!commitsFile.exists()) {
            val initialCommit = GitCommitItem(
                hash = "a1b2c3d4e5f67890abcdef1234567890abcdef12",
                message = "Initial project commit",
                author = "Android Studio Developer <developer@android.studio>",
                timestamp = System.currentTimeMillis() - 3600000,
                changedFilesCount = 12
            )
            saveCommit(initialCommit)
        }
    }

    fun commit(message: String, author: String = "Developer <dev@studio.local>"): GitCommitItem {
        val hash = sha1(message + System.currentTimeMillis())
        val changedFiles = getStatus().size
        val commitItem = GitCommitItem(
            hash = hash,
            message = message,
            author = author,
            timestamp = System.currentTimeMillis(),
            changedFilesCount = maxOf(1, changedFiles)
        )
        saveCommit(commitItem)
        return commitItem
    }

    fun getCommits(): List<GitCommitItem> {
        if (!commitsFile.exists()) return emptyList()
        val list = mutableListOf<GitCommitItem>()
        commitsFile.readLines().forEach { line ->
            val parts = line.split("|||")
            if (parts.size >= 5) {
                list.add(
                    GitCommitItem(
                        hash = parts[0],
                        message = parts[1],
                        author = parts[2],
                        timestamp = parts[3].toLongOrNull() ?: System.currentTimeMillis(),
                        changedFilesCount = parts[4].toIntOrNull() ?: 1
                    )
                )
            }
        }
        return list.reversed()
    }

    fun getStatus(): List<GitStatusItem> {
        val items = mutableListOf<GitStatusItem>()
        projectRoot.walkTopDown().filter { it.isFile && !it.path.contains(".git") }.forEach { file ->
            val rel = file.relativeTo(projectRoot).path
            // Check if modified recently (e.g. within 2 hours)
            if (System.currentTimeMillis() - file.lastModified() < 2 * 3600 * 1000) {
                items.add(GitStatusItem(rel, GitFileStatus.MODIFIED))
            }
        }
        return items
    }

    fun getStatusOutput(): String {
        val statusList = getStatus()
        return buildString {
            appendLine("On branch main")
            appendLine("Your branch is up to date with 'origin/main'.")
            appendLine()
            if (statusList.isEmpty()) {
                appendLine("nothing to commit, working tree clean")
            } else {
                appendLine("Changes not staged for commit:")
                appendLine("  (use \"git add <file>...\" to update what will be committed)")
                appendLine()
                statusList.forEach {
                    appendLine("        modified:   ${it.path}")
                }
            }
        }
    }

    fun getLogOutput(): String {
        val commits = getCommits()
        val sdf = SimpleDateFormat("EEE MMM d HH:mm:ss yyyy", Locale.US)
        return buildString {
            commits.forEach { c ->
                appendLine("commit ${c.hash}")
                appendLine("Author: ${c.author}")
                appendLine("Date:   ${sdf.format(Date(c.timestamp))}")
                appendLine()
                appendLine("    ${c.message}")
                appendLine()
            }
        }
    }

    fun getDiffOutput(): String {
        val status = getStatus()
        if (status.isEmpty()) return "No unstaged changes."
        return buildString {
            status.take(3).forEach {
                appendLine("diff --git a/${it.path} b/${it.path}")
                appendLine("--- a/${it.path}")
                appendLine("+++ b/${it.path}")
                appendLine("@@ -1,5 +1,5 @@")
                appendLine(" [Modified content in current working tree]")
            }
        }
    }

    private fun saveCommit(commit: GitCommitItem) {
        val entry = "${commit.hash}|||${commit.message}|||${commit.author}|||${commit.timestamp}|||${commit.changedFilesCount}\n"
        commitsFile.appendText(entry)
    }

    private fun sha1(input: String): String {
        val md = MessageDigest.getInstance("SHA-1")
        val bytes = md.digest(input.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}

object CodeDiffEngine {

    fun computeDiff(original: String, modified: String): List<DiffLine> {
        val originalLines = original.lines()
        val modifiedLines = modified.lines()
        val diff = mutableListOf<DiffLine>()

        var oIdx = 0
        var mIdx = 0

        while (oIdx < originalLines.size || mIdx < modifiedLines.size) {
            val origLine = originalLines.getOrNull(oIdx)
            val modLine = modifiedLines.getOrNull(mIdx)

            when {
                origLine == modLine -> {
                    diff.add(
                        DiffLine(
                            lineNumberOriginal = oIdx + 1,
                            lineNumberModified = mIdx + 1,
                            type = DiffType.UNCHANGED,
                            text = origLine ?: ""
                        )
                    )
                    oIdx++
                    mIdx++
                }
                origLine == null -> {
                    diff.add(
                        DiffLine(
                            lineNumberOriginal = null,
                            lineNumberModified = mIdx + 1,
                            type = DiffType.ADDED,
                            text = modLine ?: ""
                        )
                    )
                    mIdx++
                }
                modLine == null -> {
                    diff.add(
                        DiffLine(
                            lineNumberOriginal = oIdx + 1,
                            lineNumberModified = null,
                            type = DiffType.REMOVED,
                            text = origLine
                        )
                    )
                    oIdx++
                }
                else -> {
                    // Line modified: show removed original then added modified
                    diff.add(
                        DiffLine(
                            lineNumberOriginal = oIdx + 1,
                            lineNumberModified = null,
                            type = DiffType.REMOVED,
                            text = origLine
                        )
                    )
                    diff.add(
                        DiffLine(
                            lineNumberOriginal = null,
                            lineNumberModified = mIdx + 1,
                            type = DiffType.ADDED,
                            text = modLine
                        )
                    )
                    oIdx++
                    mIdx++
                }
            }
        }
        return diff
    }
}
