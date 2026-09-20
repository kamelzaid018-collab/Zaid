package com.example.engine

import java.io.File

class TerminalEngine(private val projectRoot: File) {

    var currentDir: File = projectRoot
        private set

    fun execute(commandLine: String): String {
        val trimmed = commandLine.trim()
        if (trimmed.isEmpty()) return ""

        val tokens = splitCommandLine(trimmed)
        if (tokens.isEmpty()) return ""

        val cmd = tokens[0]
        val args = tokens.drop(1)

        return try {
            when (cmd.lowercase()) {
                "pwd" -> getSanitizedPath(currentDir)
                "ls" -> handleLs(args)
                "cd" -> handleCd(args)
                "cat" -> handleCat(args)
                "mkdir" -> handleMkdir(args)
                "touch" -> handleTouch(args)
                "cp" -> handleCp(args)
                "mv" -> handleMv(args)
                "rm" -> handleRm(args)
                "grep" -> handleGrep(args)
                "find" -> handleFind(args)
                "echo" -> handleEcho(args, trimmed)
                "diff" -> handleDiff(args)
                "dexdump" -> handleDexDump(args)
                "signapk" -> handleSignApk(args)
                "git" -> handleGit(args)
                "help" -> handleHelp()
                "clear" -> "\u000C"
                else -> "bash: $cmd: command not found. Type 'help' to see available tools."
            }
        } catch (e: Exception) {
            "Error: ${e.localizedMessage}"
        }
    }

    private fun handleLs(args: List<String>): String {
        val showLong = args.contains("-l")
        val targetPath = args.firstOrNull { !it.startsWith("-") }
        val target = if (targetPath != null) resolvePath(targetPath) else currentDir

        if (!target.exists()) return "ls: cannot access '$targetPath': No such file or directory"
        if (target.isFile) return if (showLong) formatFileInfo(target) else target.name

        val files = target.listFiles()?.sortedWith(compareBy({ !it.isDirectory }, { it.name })) ?: emptyList()
        if (files.isEmpty()) return "(empty directory)"

        return if (showLong) {
            files.joinToString("\n") { formatFileInfo(it) }
        } else {
            files.joinToString("  ") { file ->
                if (file.isDirectory) "${file.name}/" else file.name
            }
        }
    }

    private fun formatFileInfo(file: File): String {
        val type = if (file.isDirectory) "d" else "-"
        val perm = if (file.canWrite()) "rw-r--r--" else "r--r--r--"
        val size = file.length().toString().padStart(8)
        val name = if (file.isDirectory) "${file.name}/" else file.name
        return "$type$perm 1 developer $size $name"
    }

    private fun handleCd(args: List<String>): String {
        if (args.isEmpty()) {
            currentDir = projectRoot
            return ""
        }
        val target = resolvePath(args[0])
        if (!target.exists()) return "cd: ${args[0]}: No such file or directory"
        if (!target.isDirectory) return "cd: ${args[0]}: Not a directory"

        // Sandbox check: ensure target is within projectRoot
        if (!target.canonicalPath.startsWith(projectRoot.canonicalPath)) {
            currentDir = projectRoot
            return "cd: restricted by sandbox to project root"
        }
        currentDir = target
        return ""
    }

    private fun handleCat(args: List<String>): String {
        if (args.isEmpty()) return "cat: missing file operand"
        val file = resolvePath(args[0])
        if (!file.exists()) return "cat: ${args[0]}: No such file or directory"
        if (file.isDirectory) return "cat: ${args[0]}: Is a directory"
        if (file.length() > 500_000) return "cat: file too large (>500KB)"
        return file.readText()
    }

    private fun handleMkdir(args: List<String>): String {
        val dirName = args.firstOrNull { !it.startsWith("-") } ?: return "mkdir: missing operand"
        val target = resolvePath(dirName)
        if (!isInsideSandbox(target)) return "mkdir: permission denied by sandbox"
        if (target.mkdirs() || target.exists()) {
            return "created directory '${target.name}'"
        }
        return "mkdir: cannot create directory '$dirName'"
    }

    private fun handleTouch(args: List<String>): String {
        if (args.isEmpty()) return "touch: missing file operand"
        val target = resolvePath(args[0])
        if (!isInsideSandbox(target)) return "touch: permission denied by sandbox"
        target.parentFile?.mkdirs()
        target.createNewFile()
        return ""
    }

    private fun handleCp(args: List<String>): String {
        if (args.size < 2) return "cp: missing destination file operand after '${args.firstOrNull() ?: ""}'"
        val src = resolvePath(args[0])
        val dest = resolvePath(args[1])
        if (!src.exists()) return "cp: cannot stat '${args[0]}': No such file or directory"
        if (!isInsideSandbox(dest)) return "cp: destination outside sandbox"

        return try {
            if (src.isDirectory) {
                src.copyRecursively(dest, overwrite = true)
            } else {
                src.copyTo(dest, overwrite = true)
            }
            "copied '${src.name}' -> '${dest.name}'"
        } catch (e: Exception) {
            "cp: ${e.message}"
        }
    }

    private fun handleMv(args: List<String>): String {
        if (args.size < 2) return "mv: missing file operands"
        val src = resolvePath(args[0])
        val dest = resolvePath(args[1])
        if (!src.exists()) return "mv: cannot stat '${args[0]}': No such file or directory"
        if (!isInsideSandbox(dest)) return "mv: destination outside sandbox"

        return if (src.renameTo(dest)) {
            "renamed '${src.name}' -> '${dest.name}'"
        } else {
            "mv: cannot move '${src.name}'"
        }
    }

    private fun handleRm(args: List<String>): String {
        val targetPath = args.firstOrNull { !it.startsWith("-") } ?: return "rm: missing operand"
        val target = resolvePath(targetPath)
        if (target == projectRoot) return "rm: cannot delete project root"
        if (!isInsideSandbox(target)) return "rm: permission denied"
        if (!target.exists()) return "rm: cannot remove '$targetPath': No such file or directory"

        val deleted = if (target.isDirectory) target.deleteRecursively() else target.delete()
        return if (deleted) "removed '$targetPath'" else "rm: failed to delete '$targetPath'"
    }

    private fun handleGrep(args: List<String>): String {
        val ignoreCase = args.contains("-i")
        val cleanArgs = args.filter { !it.startsWith("-") }
        if (cleanArgs.isEmpty()) return "grep: missing pattern"
        val pattern = cleanArgs[0]
        val targetFile = if (cleanArgs.size > 1) resolvePath(cleanArgs[1]) else currentDir

        val matches = mutableListOf<String>()
        val filesToSearch = if (targetFile.isDirectory) {
            targetFile.walkTopDown().filter { it.isFile && it.length() < 1_000_000 }.toList()
        } else {
            listOf(targetFile)
        }

        for (file in filesToSearch) {
            val lines = try { file.readLines() } catch (e: Exception) { emptyList() }
            lines.forEachIndexed { idx, line ->
                val matched = if (ignoreCase) line.contains(pattern, ignoreCase = true) else line.contains(pattern)
                if (matched) {
                    val rel = file.relativeTo(projectRoot).path
                    matches.add("$rel:${idx + 1}: $line")
                }
            }
        }

        return if (matches.isEmpty()) "Pattern not found." else matches.take(50).joinToString("\n")
    }

    private fun handleFind(args: List<String>): String {
        val query = args.firstOrNull { !it.startsWith("-") } ?: ""
        val results = projectRoot.walkTopDown()
            .filter { query.isEmpty() || it.name.contains(query, ignoreCase = true) }
            .map { it.relativeTo(projectRoot).path }
            .take(50)
            .toList()

        return if (results.isEmpty()) "No matching files." else results.joinToString("\n")
    }

    private fun handleEcho(args: List<String>, fullLine: String): String {
        if (fullLine.contains(">")) {
            val parts = fullLine.split(">", limit = 2)
            val textToEcho = parts[0].removePrefix("echo").trim().removeSurrounding("\"").removeSurrounding("'")
            val targetFileName = parts[1].trim()
            val target = resolvePath(targetFileName)
            if (!isInsideSandbox(target)) return "echo: permission denied"
            target.parentFile?.mkdirs()
            target.writeText(textToEcho + "\n")
            return ""
        }
        return args.joinToString(" ").removeSurrounding("\"").removeSurrounding("'")
    }

    private fun handleDiff(args: List<String>): String {
        if (args.size < 2) return "diff: usage: diff <file1> <file2>"
        val f1 = resolvePath(args[0])
        val f2 = resolvePath(args[1])
        if (!f1.exists()) return "diff: ${args[0]}: No such file"
        if (!f2.exists()) return "diff: ${args[1]}: No such file"

        val lines1 = f1.readLines()
        val lines2 = f2.readLines()
        val out = StringBuilder()
        out.appendLine("--- ${f1.name}")
        out.appendLine("+++ ${f2.name}")
        val max = maxOf(lines1.size, lines2.size)
        for (i in 0 until max) {
            val l1 = lines1.getOrNull(i)
            val l2 = lines2.getOrNull(i)
            if (l1 != l2) {
                if (l1 != null) out.appendLine("- $l1")
                if (l2 != null) out.appendLine("+ $l2")
            }
        }
        return if (out.lines().size <= 3) "Files are identical." else out.toString()
    }

    private fun handleDexDump(args: List<String>): String {
        val fileName = args.firstOrNull() ?: "classes.dex"
        val file = resolvePath(fileName)
        if (!file.exists()) return "dexdump: file '$fileName' not found"
        val info = DexParser.parseDexFile(file)
        return buildString {
            appendLine("DEX File: ${info.dexFileName}")
            appendLine("Total Classes: ${info.totalClasses}")
            appendLine("Total Methods: ${info.totalMethods}")
            appendLine("Total Fields: ${info.totalFields}")
            appendLine("Total Strings: ${info.totalStrings}")
            appendLine("Classes:")
            info.classes.take(10).forEach { cls ->
                appendLine("  class ${cls.className} extends ${cls.superClassName} (${cls.methods.size} methods)")
            }
        }
    }

    private fun handleSignApk(args: List<String>): String {
        val fileName = args.firstOrNull() ?: return "signapk: missing apk file"
        val file = resolvePath(fileName)
        if (!file.exists()) return "signapk: file '$fileName' not found"
        val signedFile = File(file.parentFile, "${file.nameWithoutExtension}-signed.apk")
        val result = ApkSignerEngine.signApk(file, signedFile)
        return if (result.success) {
            "✓ Successfully signed ${file.name} -> ${signedFile.name}\nCert Fingerprint: ${result.certFingerprintSha256}"
        } else {
            "Failed to sign: ${result.logs.joinToString("\n")}"
        }
    }

    private fun handleGit(args: List<String>): String {
        val sub = args.firstOrNull() ?: return "usage: git <status|log|branch|diff>"
        val engine = GitEngine(projectRoot)
        return when (sub.lowercase()) {
            "status" -> engine.getStatusOutput()
            "log" -> engine.getLogOutput()
            "branch" -> "* main"
            "diff" -> engine.getDiffOutput()
            else -> "git: '$sub' is not a supported git command"
        }
    }

    private fun handleHelp(): String {
        return """
Android App Studio Sandboxed Terminal:
  pwd               Print working directory
  ls [-l]           List directory contents
  cd <dir>          Change directory
  cat <file>        Display file contents
  mkdir <dir>       Create directory
  touch <file>      Create empty file
  cp <src> <dst>    Copy file or directory
  mv <src> <dst>    Move or rename file
  rm [-rf] <file>   Remove file or directory
  grep [-i] <pat>   Search for pattern in files
  find [name]       Find files matching pattern
  echo <txt> [>f]   Echo or write text to file
  diff <f1> <f2>    Compare two files
  dexdump [dex]     Inspect Dalvik DEX header & classes
  signapk <apk>     Sign APK with keystore
  git <status|log>  Check Git repository status
  clear             Clear screen
        """.trimIndent()
    }

    private fun resolvePath(path: String): File {
        return if (path.startsWith("/")) {
            File(projectRoot, path.removePrefix("/"))
        } else {
            File(currentDir, path)
        }
    }

    private fun isInsideSandbox(file: File): Boolean {
        return try {
            file.canonicalPath.startsWith(projectRoot.canonicalPath)
        } catch (e: Exception) {
            false
        }
    }

    private fun getSanitizedPath(dir: File): String {
        val rel = dir.relativeTo(projectRoot).path
        return if (rel.isEmpty()) "~/" else "~/$rel"
    }

    private fun splitCommandLine(line: String): List<String> {
        val list = mutableListOf<String>()
        val current = StringBuilder()
        var inQuotes = false
        var quoteChar = ' '

        for (ch in line) {
            when {
                (ch == '\'' || ch == '\"') && !inQuotes -> {
                    inQuotes = true
                    quoteChar = ch
                }
                ch == quoteChar && inQuotes -> {
                    inQuotes = false
                }
                ch == ' ' && !inQuotes -> {
                    if (current.isNotEmpty()) {
                        list.add(current.toString())
                        current.clear()
                    }
                }
                else -> current.append(ch)
            }
        }
        if (current.isNotEmpty()) list.add(current.toString())
        return list
    }
}
