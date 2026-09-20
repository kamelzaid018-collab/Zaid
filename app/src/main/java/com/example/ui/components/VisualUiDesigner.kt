package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.VisualWidget
import com.example.model.WidgetType
import com.example.ui.theme.*

@Composable
fun VisualUiDesigner(
    onExportCode: (String, Boolean) -> Unit, // code, isCompose
    isComposeProject: Boolean = true,
    modifier: Modifier = Modifier
) {
    // Canvas widget hierarchy
    val widgets = remember {
        mutableStateListOf(
            VisualWidget("w_col", WidgetType.COLUMN, text = "Main Column", paddingDp = 16).apply {
                children.add(VisualWidget("w_title", WidgetType.TEXT, text = "Android App Studio Mobile", fontSizeSp = 22, textColorHex = "#58A6FF"))
                children.add(VisualWidget("w_desc", WidgetType.TEXT, text = "Visual WYSIWYG Layout Designer", fontSizeSp = 14, textColorHex = "#8B949E", paddingDp = 4))
                children.add(VisualWidget("w_btn", WidgetType.BUTTON, text = "Interactive Action Button", cornerRadiusDp = 12, marginDp = 8))
                children.add(VisualWidget("w_input", WidgetType.TEXT_FIELD, text = "Sample user input field", marginDp = 8))
                children.add(VisualWidget("w_sw", WidgetType.SWITCH, text = "Enable Dynamic Theming", isChecked = true, paddingDp = 8))
            }
        )
    }

    var selectedWidgetId by remember { mutableStateOf("w_title") }
    var showCodeDialog by remember { mutableStateOf(false) }
    var generatedCodeOutput by remember { mutableStateOf("") }
    var codeDialogIsCompose by remember { mutableStateOf(true) }

    fun findWidget(id: String, list: List<VisualWidget>): VisualWidget? {
        for (w in list) {
            if (w.id == id) return w
            val child = findWidget(id, w.children)
            if (child != null) return child
        }
        return null
    }

    val selectedWidget = findWidget(selectedWidgetId, widgets) ?: widgets.firstOrNull()

    Column(modifier = modifier.fillMaxSize().background(StudioDarkBg)) {
        // Component Palette Bar
        Surface(color = StudioDarkSurface, tonalElevation = 2.dp, modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(8.dp)) {
                Text(
                    text = "COMPONENT PALETTE",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = StudioAccentBlue,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(WidgetType.values()) { type ->
                        FilterChip(
                            selected = false,
                            onClick = {
                                val newWidget = VisualWidget(
                                    id = "w_${System.currentTimeMillis() % 10000}",
                                    type = type,
                                    text = "New ${type.name.lowercase().capitalize()}"
                                )
                                // Add to selected container or root
                                if (selectedWidget?.type in listOf(WidgetType.COLUMN, WidgetType.ROW, WidgetType.BOX, WidgetType.CARD)) {
                                    selectedWidget?.children?.add(newWidget)
                                } else {
                                    widgets.add(newWidget)
                                }
                                selectedWidgetId = newWidget.id
                            },
                            label = { Text("+ ${type.name.replace('_', ' ')}", fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = StudioDarkCard,
                                labelColor = StudioTextPrimary
                            )
                        )
                    }
                }
            }
        }

        // Action controls (Sync to Code / Clear)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(StudioDarkSurface)
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Preview Canvas (Interactive)",
                fontSize = 12.sp,
                color = StudioTextSecondary,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = {
                    codeDialogIsCompose = true
                    generatedCodeOutput = widgets.joinToString("\n\n") { it.toComposeCode() }
                    showCodeDialog = true
                },
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                modifier = Modifier.height(30.dp)
            ) {
                Text("Export Compose", fontSize = 11.sp)
            }

            Spacer(modifier = Modifier.width(6.dp))

            OutlinedButton(
                onClick = {
                    codeDialogIsCompose = false
                    generatedCodeOutput = widgets.joinToString("\n\n") { it.toXmlCode() }
                    showCodeDialog = true
                },
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                modifier = Modifier.height(30.dp)
            ) {
                Text("Export XML", fontSize = 11.sp)
            }
        }

        // Center Area: Visual Preview Canvas (Scrollable Phone Mockup)
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(12.dp)
                .background(Color(0xFF000000), RoundedCornerShape(16.dp))
                .border(1.dp, StudioDarkBorder, RoundedCornerShape(16.dp))
                .padding(8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                widgets.forEach { rootWidget ->
                    RenderInteractiveWidget(
                        widget = rootWidget,
                        isSelected = rootWidget.id == selectedWidgetId,
                        onSelect = { selectedWidgetId = it }
                    )
                }
            }
        }

        // Bottom: Property Inspector for selected element
        if (selectedWidget != null) {
            Surface(
                color = StudioDarkSurface,
                tonalElevation = 4.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(210.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(10.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "PROPERTIES: ${selectedWidget.type.displayName}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = StudioAccentGreen
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        IconButton(
                            onClick = {
                                // Remove widget
                                removeWidgetRecursive(selectedWidget.id, widgets)
                                selectedWidgetId = widgets.firstOrNull()?.id ?: ""
                            },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = StudioAccentRed)
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Text & Label
                    OutlinedTextField(
                        value = selectedWidget.text,
                        onValueChange = { selectedWidget.text = it },
                        label = { Text("Text / Label", fontSize = 11.sp) },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        textStyle = LocalTextStyle.current.copy(fontSize = 12.sp)
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // Sliders Row: Font Size & Padding & Corner Radius
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Font: ${selectedWidget.fontSizeSp}sp", fontSize = 11.sp, color = StudioTextSecondary)
                            Slider(
                                value = selectedWidget.fontSizeSp.toFloat(),
                                onValueChange = { selectedWidget.fontSizeSp = it.toInt() },
                                valueRange = 10f..36f
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text("Padding: ${selectedWidget.paddingDp}dp", fontSize = 11.sp, color = StudioTextSecondary)
                            Slider(
                                value = selectedWidget.paddingDp.toFloat(),
                                onValueChange = { selectedWidget.paddingDp = it.toInt() },
                                valueRange = 0f..40f
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text("Radius: ${selectedWidget.cornerRadiusDp}dp", fontSize = 11.sp, color = StudioTextSecondary)
                            Slider(
                                value = selectedWidget.cornerRadiusDp.toFloat(),
                                onValueChange = { selectedWidget.cornerRadiusDp = it.toInt() },
                                valueRange = 0f..32f
                            )
                        }
                    }

                    // Colors & Toggles
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = selectedWidget.textColorHex,
                            onValueChange = { selectedWidget.textColorHex = it },
                            label = { Text("Text Color Hex", fontSize = 10.sp) },
                            modifier = Modifier.weight(1f).height(48.dp),
                            textStyle = LocalTextStyle.current.copy(fontSize = 11.sp)
                        )
                        OutlinedTextField(
                            value = selectedWidget.backgroundColorHex,
                            onValueChange = { selectedWidget.backgroundColorHex = it },
                            label = { Text("Bg Color Hex", fontSize = 10.sp) },
                            modifier = Modifier.weight(1f).height(48.dp),
                            textStyle = LocalTextStyle.current.copy(fontSize = 11.sp)
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Active", fontSize = 11.sp, color = StudioTextSecondary)
                            Switch(
                                checked = selectedWidget.isChecked,
                                onCheckedChange = { selectedWidget.isChecked = it }
                            )
                        }
                    }
                }
            }
        }
    }

    // Exported Code Dialog
    if (showCodeDialog) {
        AlertDialog(
            onDismissRequest = { showCodeDialog = false },
            title = { Text(if (codeDialogIsCompose) "Generated Jetpack Compose" else "Generated XML Layout") },
            text = {
                Column(modifier = Modifier.fillMaxWidth().heightIn(max = 350.dp).verticalScroll(rememberScrollState())) {
                    Text(
                        text = generatedCodeOutput,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = StudioTextPrimary
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onExportCode(generatedCodeOutput, codeDialogIsCompose)
                        showCodeDialog = false
                    }
                ) {
                    Text("Insert into Project File")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCodeDialog = false }) { Text("Close") }
            }
        )
    }
}

@Composable
fun RenderInteractiveWidget(
    widget: VisualWidget,
    isSelected: Boolean,
    onSelect: (String) -> Unit
) {
    val selectModifier = Modifier
        .clickable { onSelect(widget.id) }
        .then(
            if (isSelected) Modifier.border(2.dp, StudioAccentBlue, RoundedCornerShape(widget.cornerRadiusDp.dp))
            else Modifier
        )

    val textColor = try {
        Color(android.graphics.Color.parseColor(widget.textColorHex))
    } catch (e: Exception) {
        Color.White
    }

    val bgColor = try {
        Color(android.graphics.Color.parseColor(widget.backgroundColorHex))
    } catch (e: Exception) {
        StudioDarkCard
    }

    when (widget.type) {
        WidgetType.TEXT -> {
            Text(
                text = widget.text,
                fontSize = widget.fontSizeSp.sp,
                color = textColor,
                modifier = selectModifier
                    .padding(widget.paddingDp.dp)
                    .padding(horizontal = widget.marginDp.dp)
            )
        }
        WidgetType.BUTTON -> {
            Button(
                onClick = { onSelect(widget.id) },
                shape = RoundedCornerShape(widget.cornerRadiusDp.dp),
                colors = ButtonDefaults.buttonColors(containerColor = StudioAccentBlue),
                modifier = selectModifier.padding(widget.marginDp.dp)
            ) {
                Text(widget.text, color = Color.White)
            }
        }
        WidgetType.IMAGE -> {
            Surface(
                color = StudioDarkCard,
                shape = RoundedCornerShape(widget.cornerRadiusDp.dp),
                modifier = selectModifier
                    .size(64.dp)
                    .padding(widget.marginDp.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Android, contentDescription = widget.text, tint = StudioAccentGreen, modifier = Modifier.size(36.dp))
                }
            }
        }
        WidgetType.CARD -> {
            Card(
                shape = RoundedCornerShape(widget.cornerRadiusDp.dp),
                colors = CardDefaults.cardColors(containerColor = bgColor),
                modifier = selectModifier
                    .fillMaxWidth()
                    .padding(widget.marginDp.dp)
            ) {
                Column(modifier = Modifier.padding(widget.paddingDp.dp)) {
                    Text(widget.text, color = StudioAccentPurple, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    widget.children.forEach { child ->
                        RenderInteractiveWidget(child, false, onSelect)
                    }
                }
            }
        }
        WidgetType.COLUMN -> {
            Column(
                modifier = selectModifier
                    .fillMaxWidth()
                    .padding(widget.paddingDp.dp)
            ) {
                widget.children.forEach { child ->
                    RenderInteractiveWidget(child, false, onSelect)
                }
            }
        }
        WidgetType.ROW -> {
            Row(
                modifier = selectModifier
                    .fillMaxWidth()
                    .padding(widget.paddingDp.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                widget.children.forEach { child ->
                    RenderInteractiveWidget(child, false, onSelect)
                }
            }
        }
        WidgetType.BOX -> {
            Box(
                modifier = selectModifier
                    .fillMaxWidth()
                    .padding(widget.paddingDp.dp)
            ) {
                widget.children.forEach { child ->
                    RenderInteractiveWidget(child, false, onSelect)
                }
            }
        }
        WidgetType.LAZY_COLUMN -> {
            Column(modifier = selectModifier.fillMaxWidth().padding(widget.paddingDp.dp)) {
                Text("LazyColumn List Container", color = StudioTextSecondary, fontSize = 11.sp)
                (1..3).forEach {
                    Surface(
                        color = StudioDarkSurface,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text("List Item #$it", modifier = Modifier.padding(10.dp), color = StudioTextPrimary)
                    }
                }
            }
        }
        WidgetType.TEXT_FIELD -> {
            OutlinedTextField(
                value = widget.text,
                onValueChange = {},
                readOnly = true,
                label = { Text("Input Field") },
                modifier = selectModifier.fillMaxWidth().padding(widget.marginDp.dp)
            )
        }
        WidgetType.SWITCH -> {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = selectModifier
                    .fillMaxWidth()
                    .padding(widget.paddingDp.dp)
            ) {
                Text(widget.text, color = textColor, modifier = Modifier.weight(1f))
                Switch(checked = widget.isChecked, onCheckedChange = {})
            }
        }
        WidgetType.CHECKBOX -> {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = selectModifier
                    .fillMaxWidth()
                    .padding(widget.paddingDp.dp)
            ) {
                Checkbox(checked = widget.isChecked, onCheckedChange = {})
                Text(widget.text, color = textColor)
            }
        }
    }
}

private fun removeWidgetRecursive(id: String, list: MutableList<VisualWidget>): Boolean {
    val iter = list.iterator()
    while (iter.hasNext()) {
        val item = iter.next()
        if (item.id == id) {
            iter.remove()
            return true
        }
        if (removeWidgetRecursive(id, item.children)) return true
    }
    return false
}
