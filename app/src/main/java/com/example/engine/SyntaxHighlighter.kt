package com.example.engine

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle

object SyntaxHighlighter {

    private val KEYWORD_COLOR = Color(0xFFCC7832) // JetBrains orange-brown
    private val TYPE_COLOR = Color(0xFF4EC9B0)    // VSCode / Studio teal
    private val ANNOTATION_COLOR = Color(0xFFBBB529) // Warm gold
    private val STRING_COLOR = Color(0xFF6A8759)  // Studio green
    private val NUMBER_COLOR = Color(0xFF6897BB)  // Studio soft blue
    private val COMMENT_COLOR = Color(0xFF808080) // Gray
    private val TAG_COLOR = Color(0xFFE8BF6A)     // XML yellow
    private val ATTR_COLOR = Color(0xFFBABABA)    // XML attr
    private val SMALI_DIRECTIVE = Color(0xFFCF85D6) // Pinkish purple

    private val KOTLIN_KEYWORDS = setOf(
        "package", "import", "class", "object", "interface", "fun", "val", "var",
        "private", "public", "protected", "internal", "override", "open", "abstract",
        "sealed", "data", "enum", "companion", "constructor", "init", "this", "super",
        "return", "if", "else", "when", "for", "while", "do", "break", "continue",
        "throw", "try", "catch", "finally", "is", "as", "in", "by", "suspend", "inline",
        "tailrec", "operator", "infix", "typealias", "true", "false", "null"
    )

    private val JAVA_KEYWORDS = setOf(
        "abstract", "assert", "boolean", "break", "byte", "case", "catch", "char", "class",
        "const", "continue", "default", "do", "double", "else", "enum", "extends", "final",
        "finally", "float", "for", "goto", "if", "implements", "import", "instanceof", "int",
        "interface", "long", "native", "new", "package", "private", "protected", "public",
        "return", "short", "static", "strictfp", "super", "switch", "synchronized", "this",
        "throw", "throws", "transient", "try", "void", "volatile", "while", "true", "false", "null"
    )

    private val SMALI_KEYWORDS = setOf(
        ".class", ".super", ".source", ".implements", ".field", ".method", ".end",
        ".registers", ".locals", ".parameter", ".prologue", ".line",
        "return-void", "return", "return-object", "const-string", "const/4", "const",
        "invoke-virtual", "invoke-direct", "invoke-static", "invoke-super", "invoke-interface",
        "move-result", "move-result-object", "if-eq", "if-ne", "if-eqz", "if-nez", "goto",
        "new-instance", "check-cast", "instance-of", "sget-object", "sput-object", "iget-object", "iput-object"
    )

    private val COMMON_TYPES = setOf(
        "String", "Int", "Boolean", "Float", "Double", "Long", "Short", "Byte", "Char", "Unit",
        "Any", "List", "Map", "Set", "ArrayList", "HashMap", "Array", "Modifier", "Composable",
        "Context", "Activity", "ComponentActivity", "View", "TextView", "Button", "Bundle",
        "File", "CoroutineScope", "StateFlow", "Flow", "ViewModel", "Scaffold", "Column", "Row", "Box"
    )

    fun highlightCode(code: String, extension: String): AnnotatedString {
        val builder = AnnotatedString.Builder()
        val lines = code.lines()

        lines.forEachIndexed { index, line ->
            highlightLine(builder, line, extension)
            if (index < lines.size - 1) {
                builder.append("\n")
            }
        }
        return builder.toAnnotatedString()
    }

    private fun highlightLine(builder: AnnotatedString.Builder, line: String, ext: String) {
        val trimmed = line.trimStart()

        // 1. Full line comments
        if (trimmed.startsWith("//") || trimmed.startsWith("#")) {
            builder.withStyle(SpanStyle(color = COMMENT_COLOR, fontStyle = FontStyle.Italic)) {
                append(line)
            }
            return
        }

        // 2. XML / HTML line highlighting
        if (ext.equals("xml", ignoreCase = true) || ext.equals("html", ignoreCase = true)) {
            highlightXmlLine(builder, line)
            return
        }

        // 3. General Tokenizer
        var i = 0
        while (i < line.length) {
            val ch = line[i]

            // In-line comment check
            if (ch == '/' && i + 1 < line.length && line[i + 1] == '/') {
                builder.withStyle(SpanStyle(color = COMMENT_COLOR, fontStyle = FontStyle.Italic)) {
                    append(line.substring(i))
                }
                break
            }

            // String literal
            if (ch == '\"' || ch == '\'') {
                val quote = ch
                val start = i
                i++
                while (i < line.length && line[i] != quote) {
                    if (line[i] == '\\' && i + 1 < line.length) i += 2 else i++
                }
                if (i < line.length) i++ // include closing quote
                builder.withStyle(SpanStyle(color = STRING_COLOR)) {
                    append(line.substring(start, i))
                }
                continue
            }

            // Annotations
            if (ch == '@' && (ext == "kt" || ext == "java")) {
                val start = i
                i++
                while (i < line.length && (line[i].isLetterOrDigit() || line[i] == '.')) i++
                builder.withStyle(SpanStyle(color = ANNOTATION_COLOR, fontWeight = FontWeight.SemiBold)) {
                    append(line.substring(start, i))
                }
                continue
            }

            // Numbers
            if (ch.isDigit() && (i == 0 || !line[i - 1].isLetter())) {
                val start = i
                while (i < line.length && (line[i].isLetterOrDigit() || line[i] == '.' || line[i] == 'x' || line[i] == 'X')) i++
                builder.withStyle(SpanStyle(color = NUMBER_COLOR)) {
                    append(line.substring(start, i))
                }
                continue
            }

            // Words & Identifiers (or Smali directives starting with .)
            if (ch.isLetter() || ch == '_' || (ch == '.' && ext == "smali")) {
                val start = i
                while (i < line.length && (line[i].isLetterOrDigit() || line[i] == '_' || line[i] == '-' || (ch == '.' && line[i] == '.'))) i++
                val word = line.substring(start, i)

                when {
                    ext == "smali" && (word in SMALI_KEYWORDS || word.startsWith(".")) -> {
                        builder.withStyle(SpanStyle(color = SMALI_DIRECTIVE, fontWeight = FontWeight.Bold)) {
                            append(word)
                        }
                    }
                    (ext == "kt" || ext == "gradle" || ext.endsWith("kts")) && word in KOTLIN_KEYWORDS -> {
                        builder.withStyle(SpanStyle(color = KEYWORD_COLOR, fontWeight = FontWeight.Bold)) {
                            append(word)
                        }
                    }
                    ext == "java" && word in JAVA_KEYWORDS -> {
                        builder.withStyle(SpanStyle(color = KEYWORD_COLOR, fontWeight = FontWeight.Bold)) {
                            append(word)
                        }
                    }
                    word in COMMON_TYPES -> {
                        builder.withStyle(SpanStyle(color = TYPE_COLOR, fontWeight = FontWeight.SemiBold)) {
                            append(word)
                        }
                    }
                    else -> {
                        builder.append(word)
                    }
                }
                continue
            }

            // Punctuation / whitespace
            builder.append(ch)
            i++
        }
    }

    private fun highlightXmlLine(builder: AnnotatedString.Builder, line: String) {
        var i = 0
        while (i < line.length) {
            val ch = line[i]

            // XML Comment <!-- ... -->
            if (ch == '<' && line.startsWith("<!--", i)) {
                val end = line.indexOf("-->", i)
                val commentText = if (end != -1) line.substring(i, end + 3) else line.substring(i)
                builder.withStyle(SpanStyle(color = COMMENT_COLOR, fontStyle = FontStyle.Italic)) {
                    append(commentText)
                }
                i += commentText.length
                continue
            }

            // Tag open <tag or </tag
            if (ch == '<') {
                val start = i
                i++
                if (i < line.length && line[i] == '/') i++
                while (i < line.length && (line[i].isLetterOrDigit() || line[i] == ':' || line[i] == '_' || line[i] == '-')) i++
                builder.withStyle(SpanStyle(color = TAG_COLOR, fontWeight = FontWeight.Bold)) {
                    append(line.substring(start, i))
                }
                continue
            }

            // Tag close > or />
            if (ch == '>') {
                builder.withStyle(SpanStyle(color = TAG_COLOR, fontWeight = FontWeight.Bold)) {
                    append(">")
                }
                i++
                continue
            }
            if (ch == '/' && i + 1 < line.length && line[i + 1] == '>') {
                builder.withStyle(SpanStyle(color = TAG_COLOR, fontWeight = FontWeight.Bold)) {
                    append("/>")
                }
                i += 2
                continue
            }

            // Attributes: android:name=
            if (ch.isLetter() || ch == ':') {
                val start = i
                while (i < line.length && (line[i].isLetterOrDigit() || line[i] == ':' || line[i] == '_' || line[i] == '-')) i++
                val attr = line.substring(start, i)
                builder.withStyle(SpanStyle(color = ATTR_COLOR)) {
                    append(attr)
                }
                continue
            }

            // String attribute value "..."
            if (ch == '\"') {
                val start = i
                i++
                while (i < line.length && line[i] != '\"') i++
                if (i < line.length) i++
                builder.withStyle(SpanStyle(color = STRING_COLOR)) {
                    append(line.substring(start, i))
                }
                continue
            }

            builder.append(ch)
            i++
        }
    }

    fun getCodeCompletions(prefix: String, ext: String): List<String> {
        val clean = prefix.trim().lowercase()
        val list = when {
            ext == "kt" || ext == "gradle" || ext.endsWith("kts") -> listOf(
                "Composable", "Modifier", "fillMaxWidth()", "fillMaxSize()", "padding(16.dp)",
                "Scaffold", "Column", "Row", "Box", "LazyColumn", "Text(\"Hello\")",
                "Button(onClick = {})", "OutlinedTextField", "remember { mutableStateOf() }",
                "LaunchedEffect(Unit) {}", "viewModelScope.launch {}", "StateFlow", "MutableStateFlow",
                "val ", "var ", "fun ", "override fun ", "data class ", "interface ", "private val "
            )
            ext == "java" -> listOf(
                "public class ", "private void ", "protected void onCreate(Bundle savedInstanceState)",
                "findViewById(R.id.)", "setContentView(R.layout.)", "Toast.makeText(this, \"\", Toast.LENGTH_SHORT).show()",
                "Intent intent = new Intent(this, .class);", "Log.d(\"DEBUG\", \"\");", "new View.OnClickListener() {}"
            )
            ext == "xml" -> listOf(
                "android:layout_width=\"wrap_content\"", "android:layout_width=\"match_parent\"",
                "android:layout_height=\"wrap_content\"", "android:layout_height=\"match_parent\"",
                "android:text=\"\"", "android:textSize=\"16sp\"", "android:textColor=\"#FFFFFF\"",
                "android:padding=\"16dp\"", "android:layout_margin=\"8dp\"", "android:id=\"@+id/\"",
                "<TextView\n    android:layout_width=\"wrap_content\"\n    android:layout_height=\"wrap_content\" />",
                "<Button\n    android:layout_width=\"wrap_content\"\n    android:layout_height=\"wrap_content\" />",
                "<LinearLayout\n    android:layout_width=\"match_parent\"\n    android:layout_height=\"wrap_content\"\n    android:orientation=\"vertical\">"
            )
            ext == "smali" -> listOf(
                ".method public ", ".registers ", ".end method", "return-void", "const-string v0, \"\"",
                "invoke-virtual {p0}, ", "invoke-direct {p0}, ", "sget-object ", "new-instance v0, "
            )
            else -> listOf("public", "private", "class", "function", "return")
        }

        return if (clean.isEmpty()) list.take(8) else list.filter { it.lowercase().contains(clean) }.take(8)
    }
}
