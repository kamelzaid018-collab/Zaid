package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine.TerminalEngine
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import java.io.File

data class TerminalLine(val prompt: String, val output: String)

@Composable
fun TerminalView(
    projectRoot: File,
    modifier: Modifier = Modifier
) {
    val terminal = remember(projectRoot) { TerminalEngine(projectRoot) }
    val history = remember {
        mutableStateListOf(
            TerminalLine(
                "",
                "Android App Studio Embedded Sandbox Terminal [v1.0.0]\nType 'help' to view available sandboxed developer commands."
            )
        )
    }

    var inputCommand by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    fun runCmd(cmd: String) {
        val trimmed = cmd.trim()
        if (trimmed.isEmpty()) return

        val promptStr = "dev@studio:${terminal.currentDir.name}$ $trimmed"
        val out = terminal.execute(trimmed)

        if (out == "\u000C") {
            history.clear()
        } else {
            history.add(TerminalLine(promptStr, out))
        }
        inputCommand = ""

        coroutineScope.launch {
            if (history.isNotEmpty()) {
                listState.animateScrollToItem(history.size - 1)
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF070A0F))
    ) {
        // Terminal Title Bar
        Surface(
            color = StudioDarkSurface,
            tonalElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Terminal, contentDescription = null, tint = StudioAccentGreen, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Sandboxed Bash Terminal",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = StudioTextPrimary
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = terminal.currentDir.name,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    color = StudioAccentBlue
                )
            }
        }

        // Quick Command Bar for mobile ergonomics
        Surface(color = StudioDarkCard, modifier = Modifier.fillMaxWidth()) {
            val quickCommands = listOf("ls -l", "pwd", "git status", "git log", "dexdump classes.dex", "help", "clear")
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(quickCommands) { cmd ->
                    SuggestionChip(
                        onClick = { runCmd(cmd) },
                        label = { Text(cmd, fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = StudioAccentBlue) },
                        modifier = Modifier.height(26.dp)
                    )
                }
            }
        }

        // Terminal Output Console
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            items(history) { line ->
                Column(modifier = Modifier.padding(vertical = 2.dp)) {
                    if (line.prompt.isNotEmpty()) {
                        Text(
                            text = line.prompt,
                            color = StudioAccentGreen,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    if (line.output.isNotEmpty()) {
                        Text(
                            text = line.output,
                            color = StudioTextPrimary,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }

        // Command Input Bar
        Surface(
            color = StudioDarkSurface,
            tonalElevation = 4.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "$ ",
                    color = StudioAccentGreen,
                    fontSize = 14.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )

                OutlinedTextField(
                    value = inputCommand,
                    onValueChange = { inputCommand = it },
                    singleLine = true,
                    textStyle = TextStyle(
                        color = StudioTextPrimary,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp
                    ),
                    placeholder = { Text("enter command...", fontSize = 12.sp, color = StudioTextMuted) },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = { runCmd(inputCommand) }),
                    modifier = Modifier.weight(1f).height(50.dp)
                )

                Spacer(modifier = Modifier.width(6.dp))

                IconButton(
                    onClick = { runCmd(inputCommand) },
                    colors = IconButtonDefaults.iconButtonColors(contentColor = StudioAccentGreen),
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = "Run")
                }
            }
        }
    }
}
