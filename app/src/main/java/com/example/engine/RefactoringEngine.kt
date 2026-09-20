package com.example.engine

import com.example.model.StudioProject
import java.io.File

object RefactoringEngine {

    data class RefactorUsage(
        val file: File,
        val line: Int,
        val lineText: String
    )

    fun findUsages(project: StudioProject, symbol: String): List<RefactorUsage> {
        if (symbol.isBlank()) return emptyList()
        val usages = mutableListOf<RefactorUsage>()

        project.rootDir.walkTopDown().filter {
            it.isFile && it.extension in listOf("kt", "java", "xml", "gradle", "kts", "smali")
        }.forEach { file ->
            try {
                file.readLines().forEachIndexed { idx, line ->
                    if (line.contains(symbol)) {
                        usages.add(RefactorUsage(file, idx + 1, line.trim()))
                    }
                }
            } catch (e: Exception) {
                // skip
            }
        }
        return usages
    }

    fun renameSymbol(project: StudioProject, oldSymbol: String, newSymbol: String): Int {
        if (oldSymbol.isBlank() || newSymbol.isBlank() || oldSymbol == newSymbol) return 0
        var replacedCount = 0

        project.rootDir.walkTopDown().filter {
            it.isFile && it.extension in listOf("kt", "java", "xml", "gradle", "kts", "smali")
        }.forEach { file ->
            try {
                val content = file.readText()
                if (content.contains(oldSymbol)) {
                    val count = Regex.fromLiteral(oldSymbol).findAll(content).count()
                    val newContent = content.replace(oldSymbol, newSymbol)
                    file.writeText(newContent)
                    replacedCount += count
                }
            } catch (e: Exception) {
                // skip
            }
        }
        return replacedCount
    }

    fun organizeImports(file: File): String {
        if (!file.exists()) return ""
        val lines = file.readLines()
        val packageLines = mutableListOf<String>()
        val importLines = mutableListOf<String>()
        val otherLines = mutableListOf<String>()

        var inImportBlock = false
        lines.forEach { line ->
            val trimmed = line.trim()
            when {
                trimmed.startsWith("package ") -> packageLines.add(line)
                trimmed.startsWith("import ") -> {
                    inImportBlock = true
                    if (!importLines.contains(line)) {
                        importLines.add(line)
                    }
                }
                inImportBlock && trimmed.isEmpty() -> {
                    // skip empty lines inside import block
                }
                else -> {
                    inImportBlock = false
                    otherLines.add(line)
                }
            }
        }

        importLines.sort()

        val result = buildString {
            packageLines.forEach { appendLine(it) }
            if (packageLines.isNotEmpty()) appendLine()
            importLines.forEach { appendLine(it) }
            if (importLines.isNotEmpty()) appendLine()
            otherLines.forEach { appendLine(it) }
        }

        file.writeText(result)
        return result
    }

    fun extractVariable(code: String, selectedExpression: String, variableName: String = "extractedValue"): String {
        if (selectedExpression.isBlank()) return code
        val declaration = "val $variableName = $selectedExpression\n"
        return declaration + code.replace(selectedExpression, variableName)
    }

    fun extractFunction(code: String, selectedBlock: String, functionName: String = "extractedFunction"): String {
        if (selectedBlock.isBlank()) return code
        val call = "$functionName()\n"
        val funcDef = "\n\nprivate fun $functionName() {\n    $selectedBlock\n}\n"
        return code.replace(selectedBlock, call) + funcDef
    }
}
