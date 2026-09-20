package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine.SyntaxHighlighter
import com.example.model.EditorTab
import com.example.ui.theme.*

@Composable
fun CodeEditorView(
    tab: EditorTab,
    onContentChange: (String) -> Unit,
    onOrganizeImports: () -> Unit = {},
    onRenameSymbol: (String, String) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    var textFieldValue by remember(tab.projectFile.relativePath) {
        mutableStateOf(TextFieldValue(tab.content))
    }

    // Search and Replace states
    var showSearchBar by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var replaceQuery by remember { mutableStateOf("") }
    var showGoToLineDialog by remember { mutableStateOf(false) }
    var goToLineText by remember { mutableStateOf("") }
    var showRenameDialog by remember { mutableStateOf(false) }
    var oldSymbolText by remember { mutableStateOf("") }
    var newSymbolText by remember { mutableStateOf("") }

    // Undo / Redo history stack
    val history = remember { mutableStateListOf(tab.content) }
    var historyIndex by remember { mutableIntStateOf(0) }

    fun applyTextChange(newText: String) {
        textFieldValue = textFieldValue.copy(text = newText)
        onContentChange(newText)
        if (historyIndex < history.size - 1) {
            while (history.size > historyIndex + 1) {
                history.removeAt(history.size - 1)
            }
        }
        history.add(newText)
        historyIndex = history.size - 1
    }

    val lines = remember(textFieldValue.text) {
        textFieldValue.text.lines()
    }

    val highlightedText = remember(textFieldValue.text, tab.projectFile.extension) {
        SyntaxHighlighter.highlightCode(textFieldValue.text, tab.projectFile.extension)
    }

    val completions = remember(tab.projectFile.extension) {
        SyntaxHighlighter.getCodeCompletions("", tab.projectFile.extension)
    }

    val verticalScroll = rememberScrollState()
    val horizontalScroll = rememberScrollState()

    Column(modifier = modifier.fillMaxSize().background(StudioDarkBg)) {
        // Top Toolbar inside editor
        Surface(
            color = StudioDarkSurface,
            tonalElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // File info chip
                Text(
                    text = tab.projectFile.name,
                    color = StudioTextPrimary,
                    fontSize = 13.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(end = 8.dp)
                )

                if (tab.isDirty) {
                    Badge(
                        containerColor = StudioAccentOrange,
                        modifier = Modifier.size(6.dp)
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                // Action buttons
                IconButton(
                    onClick = {
                        if (historyIndex > 0) {
                            historyIndex--
                            val prev = history[historyIndex]
                            textFieldValue = textFieldValue.copy(text = prev)
                            onContentChange(prev)
                        }
                    },
                    enabled = historyIndex > 0,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(Icons.Default.Undo, contentDescription = "Undo", tint = if (historyIndex > 0) StudioAccentBlue else StudioTextMuted)
                }

                IconButton(
                    onClick = {
                        if (historyIndex < history.size - 1) {
                            historyIndex++
                            val next = history[historyIndex]
                            textFieldValue = textFieldValue.copy(text = next)
                            onContentChange(next)
                        }
                    },
                    enabled = historyIndex < history.size - 1,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(Icons.Default.Redo, contentDescription = "Redo", tint = if (historyIndex < history.size - 1) StudioAccentBlue else StudioTextMuted)
                }

                IconButton(
                    onClick = { showSearchBar = !showSearchBar },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(Icons.Default.Search, contentDescription = "Find/Replace", tint = if (showSearchBar) StudioAccentGreen else StudioTextSecondary)
                }

                IconButton(
                    onClick = { showGoToLineDialog = true },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(Icons.Default.FormatListNumbered, contentDescription = "Go To Line", tint = StudioTextSecondary)
                }

                IconButton(
                    onClick = { showRenameDialog = true },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(Icons.Default.DriveFileRenameOutline, contentDescription = "Rename Symbol", tint = StudioAccentPurple)
                }

                if (tab.projectFile.isKotlin || tab.projectFile.isJava) {
                    IconButton(
                        onClick = onOrganizeImports,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.AutoFixHigh, contentDescription = "Organize Imports", tint = StudioAccentGreen)
                    }
                }
            }
        }

        // Search & Replace Bar
        if (showSearchBar) {
            Surface(
                color = StudioDarkCard,
                modifier = Modifier.fillMaxWidth().padding(4.dp),
                shape = RoundedCornerShape(6.dp)
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Find...", fontSize = 12.sp) },
                            singleLine = true,
                            textStyle = TextStyle(color = StudioTextPrimary, fontSize = 12.sp),
                            modifier = Modifier.weight(1f).height(48.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        OutlinedTextField(
                            value = replaceQuery,
                            onValueChange = { replaceQuery = it },
                            placeholder = { Text("Replace...", fontSize = 12.sp) },
                            singleLine = true,
                            textStyle = TextStyle(color = StudioTextPrimary, fontSize = 12.sp),
                            modifier = Modifier.weight(1f).height(48.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Button(
                            onClick = {
                                if (searchQuery.isNotEmpty()) {
                                    val newText = textFieldValue.text.replace(searchQuery, replaceQuery)
                                    applyTextChange(newText)
                                }
                            },
                            contentPadding = PaddingValues(horizontal = 8.dp),
                            modifier = Modifier.height(36.dp)
                        ) {
                            Text("Replace All", fontSize = 11.sp)
                        }
                    }
                }
            }
        }

        // Quick Code Completion Chips & Syntax Shortcuts
        Surface(
            color = StudioDarkSurface,
            modifier = Modifier.fillMaxWidth().height(36.dp)
        ) {
            LazyRow(
                modifier = Modifier.fillMaxSize().padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Syntax punctuation shortcuts
                val punctuation = listOf("{ }", "( )", "[ ]", "\"", "->", "::", ".", "=", ";", "< >")
                items(punctuation) { p ->
                    AssistChip(
                        onClick = {
                            val cursor = textFieldValue.selection.start
                            val insert = when (p) {
                                "{ }" -> "{\n    \n}"
                                "( )" -> "()"
                                "[ ]" -> "[]"
                                "< >" -> "<>"
                                else -> p
                            }
                            val newText = textFieldValue.text.substring(0, cursor) + insert + textFieldValue.text.substring(cursor)
                            applyTextChange(newText)
                        },
                        label = { Text(p, fontSize = 11.sp, fontFamily = FontFamily.Monospace) },
                        modifier = Modifier.height(28.dp)
                    )
                }

                // Keyword/Component completions
                items(completions) { comp ->
                    SuggestionChip(
                        onClick = {
                            val cursor = textFieldValue.selection.start
                            val newText = textFieldValue.text.substring(0, cursor) + comp + " " + textFieldValue.text.substring(cursor)
                            applyTextChange(newText)
                        },
                        label = { Text(comp, fontSize = 11.sp, color = StudioAccentBlue) },
                        modifier = Modifier.height(28.dp)
                    )
                }
            }
        }

        // Editor Area: Line Numbers + Code Canvas
        Row(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(verticalScroll)
        ) {
            // Line numbers column
            Column(
                modifier = Modifier
                    .background(StudioDarkSurface)
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.End
            ) {
                lines.indices.forEach { index ->
                    Text(
                        text = "${index + 1}",
                        color = StudioLineNumber,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        lineHeight = 18.sp
                    )
                }
            }

            // Code text field with syntax highlighting
            Box(
                modifier = Modifier
                    .weight(1f)
                    .horizontalScroll(horizontalScroll)
                    .padding(horizontal = 8.dp, vertical = 8.dp)
            ) {
                BasicTextField(
                    value = textFieldValue,
                    onValueChange = {
                        textFieldValue = it
                        onContentChange(it.text)
                    },
                    textStyle = TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        color = StudioTextPrimary
                    ),
                    cursorBrush = SolidColor(StudioEditorCursor),
                    visualTransformation = {
                        androidx.compose.ui.text.input.TransformedText(
                            highlightedText,
                            androidx.compose.ui.text.input.OffsetMapping.Identity
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }

    // Go To Line Dialog
    if (showGoToLineDialog) {
        AlertDialog(
            onDismissRequest = { showGoToLineDialog = false },
            title = { Text("Go To Line") },
            text = {
                OutlinedTextField(
                    value = goToLineText,
                    onValueChange = { goToLineText = it.filter { ch -> ch.isDigit() } },
                    label = { Text("Line number (1 - ${lines.size})") },
                    singleLine = true
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showGoToLineDialog = false
                    }
                ) {
                    Text("Go")
                }
            },
            dismissButton = {
                TextButton(onClick = { showGoToLineDialog = false }) { Text("Cancel") }
            }
        )
    }

    // Rename Symbol Dialog
    if (showRenameDialog) {
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = { Text("Rename Symbol across Project") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = oldSymbolText,
                        onValueChange = { oldSymbolText = it },
                        label = { Text("Current Symbol") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = newSymbolText,
                        onValueChange = { newSymbolText = it },
                        label = { Text("New Symbol") },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (oldSymbolText.isNotBlank() && newSymbolText.isNotBlank()) {
                            onRenameSymbol(oldSymbolText.trim(), newSymbolText.trim())
                            showRenameDialog = false
                        }
                    }
                ) {
                    Text("Refactor")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = false }) { Text("Cancel") }
            }
        )
    }
}
