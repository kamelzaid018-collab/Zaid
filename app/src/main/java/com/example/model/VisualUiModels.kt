package com.example.model

enum class WidgetType(val displayName: String, val category: String) {
    TEXT("Text / TextView", "Typography"),
    BUTTON("Button / MaterialButton", "Actions"),
    IMAGE("Image / ImageView", "Media"),
    CARD("Card / CardView", "Containers"),
    COLUMN("Column / LinearLayout (Vertical)", "Layouts"),
    ROW("Row / LinearLayout (Horizontal)", "Layouts"),
    BOX("Box / FrameLayout", "Containers"),
    LAZY_COLUMN("LazyColumn / RecyclerView", "Lists"),
    TEXT_FIELD("TextField / EditText", "Inputs"),
    SWITCH("Switch / SwitchCompat", "Toggles"),
    CHECKBOX("Checkbox / CheckBox", "Toggles")
}

data class VisualWidget(
    val id: String,
    val type: WidgetType,
    var text: String = "",
    var width: String = "fillMaxWidth()", // wrap_content, match_parent, or dp
    var height: String = "wrapContentSize()",
    var paddingDp: Int = 12,
    var marginDp: Int = 8,
    var fontSizeSp: Int = 16,
    var textColorHex: String = "#E2E8F0",
    var backgroundColorHex: String = "#1E293B",
    var cornerRadiusDp: Int = 8,
    var alignment: String = "CenterStart",
    var isChecked: Boolean = false,
    var isVisible: Boolean = true,
    var children: MutableList<VisualWidget> = mutableListOf()
) {
    fun toComposeCode(indent: String = ""): String {
        val nextIndent = "$indent    "
        return when (type) {
            WidgetType.TEXT -> {
                "$indent Text(\n$nextIndent text = \"$text\",\n$nextIndent fontSize = ${fontSizeSp}.sp,\n$nextIndent color = Color(android.graphics.Color.parseColor(\"$textColorHex\")),\n$nextIndent modifier = Modifier.padding(${paddingDp}.dp)\n$indent)"
            }
            WidgetType.BUTTON -> {
                "$indent Button(\n$nextIndent onClick = { /* TODO */ },\n$nextIndent shape = RoundedCornerShape(${cornerRadiusDp}.dp),\n$nextIndent modifier = Modifier.padding(${marginDp}.dp)\n$indent) {\n$nextIndent Text(\"$text\")\n$indent}"
            }
            WidgetType.IMAGE -> {
                "$indent Image(\n$nextIndent painter = painterResource(R.drawable.ic_launcher_foreground),\n$nextIndent contentDescription = \"$text\",\n$nextIndent modifier = Modifier.size(64.dp).padding(${paddingDp}.dp)\n$indent)"
            }
            WidgetType.CARD -> {
                val childCode = children.joinToString("\n") { it.toComposeCode(nextIndent) }
                "$indent Card(\n$nextIndent shape = RoundedCornerShape(${cornerRadiusDp}.dp),\n$nextIndent modifier = Modifier.fillMaxWidth().padding(${marginDp}.dp)\n$indent) {\n$nextIndent Column(modifier = Modifier.padding(${paddingDp}.dp)) {\n$childCode\n$nextIndent}\n$indent}"
            }
            WidgetType.COLUMN -> {
                val childCode = children.joinToString("\n") { it.toComposeCode(nextIndent) }
                "$indent Column(\n$nextIndent modifier = Modifier.fillMaxWidth().padding(${paddingDp}.dp)\n$indent) {\n$childCode\n$indent}"
            }
            WidgetType.ROW -> {
                val childCode = children.joinToString("\n") { it.toComposeCode(nextIndent) }
                "$indent Row(\n$nextIndent modifier = Modifier.fillMaxWidth().padding(${paddingDp}.dp),\n$nextIndent verticalAlignment = Alignment.CenterVertically\n$indent) {\n$childCode\n$indent}"
            }
            WidgetType.BOX -> {
                val childCode = children.joinToString("\n") { it.toComposeCode(nextIndent) }
                "$indent Box(\n$nextIndent modifier = Modifier.fillMaxWidth().padding(${paddingDp}.dp)\n$indent) {\n$childCode\n$indent}"
            }
            WidgetType.LAZY_COLUMN -> {
                "$indent LazyColumn(modifier = Modifier.fillMaxSize()) {\n$nextIndent items(5) { index ->\n$nextIndent     Text(\"Item #\$index\", modifier = Modifier.padding(${paddingDp}.dp))\n$nextIndent }\n$indent}"
            }
            WidgetType.TEXT_FIELD -> {
                "$indent OutlinedTextField(\n$nextIndent value = \"$text\",\n$nextIndent onValueChange = { /* onChange */ },\n$nextIndent label = { Text(\"Enter input\") },\n$nextIndent modifier = Modifier.fillMaxWidth().padding(${marginDp}.dp)\n$indent)"
            }
            WidgetType.SWITCH -> {
                "$indent Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(${paddingDp}.dp)) {\n$nextIndent Text(\"$text\", modifier = Modifier.weight(1f))\n$nextIndent Switch(checked = $isChecked, onCheckedChange = { /* onToggle */ })\n$indent}"
            }
            WidgetType.CHECKBOX -> {
                "$indent Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(${paddingDp}.dp)) {\n$nextIndent Checkbox(checked = $isChecked, onCheckedChange = { /* onCheck */ })\n$nextIndent Text(\"$text\")\n$indent}"
            }
        }
    }

    fun toXmlCode(indent: String = ""): String {
        val nextIndent = "$indent    "
        return when (type) {
            WidgetType.TEXT -> {
                "$indent<TextView\n$nextIndent android:layout_width=\"wrap_content\"\n$nextIndent android:layout_height=\"wrap_content\"\n$nextIndent android:text=\"$text\"\n$nextIndent android:textSize=\"${fontSizeSp}sp\"\n$nextIndent android:textColor=\"$textColorHex\"\n$nextIndent android:padding=\"${paddingDp}dp\" />"
            }
            WidgetType.BUTTON -> {
                "$indent<Button\n$nextIndent android:layout_width=\"wrap_content\"\n$nextIndent android:layout_height=\"wrap_content\"\n$nextIndent android:text=\"$text\"\n$nextIndent android:layout_margin=\"${marginDp}dp\" />"
            }
            WidgetType.IMAGE -> {
                "$indent<ImageView\n$nextIndent android:layout_width=\"64dp\"\n$nextIndent android:layout_height=\"64dp\"\n$nextIndent android:src=\"@drawable/ic_launcher_foreground\"\n$nextIndent android:contentDescription=\"$text\" />"
            }
            WidgetType.COLUMN -> {
                val childXml = children.joinToString("\n") { it.toXmlCode(nextIndent) }
                "$indent<LinearLayout\n$nextIndent xmlns:android=\"http://schemas.android.com/apk/res/android\"\n$nextIndent android:layout_width=\"match_parent\"\n$nextIndent android:layout_height=\"wrap_content\"\n$nextIndent android:orientation=\"vertical\"\n$nextIndent android:padding=\"${paddingDp}dp\">\n$childXml\n$indent</LinearLayout>"
            }
            WidgetType.ROW -> {
                val childXml = children.joinToString("\n") { it.toXmlCode(nextIndent) }
                "$indent<LinearLayout\n$nextIndent xmlns:android=\"http://schemas.android.com/apk/res/android\"\n$nextIndent android:layout_width=\"match_parent\"\n$nextIndent android:layout_height=\"wrap_content\"\n$nextIndent android:orientation=\"horizontal\"\n$nextIndent android:padding=\"${paddingDp}dp\">\n$childXml\n$indent</LinearLayout>"
            }
            WidgetType.CARD -> {
                val childXml = children.joinToString("\n") { it.toXmlCode(nextIndent) }
                "$indent<androidx.cardview.widget.CardView\n$nextIndent android:layout_width=\"match_parent\"\n$nextIndent android:layout_height=\"wrap_content\"\n$nextIndent app:cardCornerRadius=\"${cornerRadiusDp}dp\"\n$nextIndent android:layout_margin=\"${marginDp}dp\">\n$childXml\n$indent</androidx.cardview.widget.CardView>"
            }
            WidgetType.BOX -> {
                val childXml = children.joinToString("\n") { it.toXmlCode(nextIndent) }
                "$indent<FrameLayout\n$nextIndent android:layout_width=\"match_parent\"\n$nextIndent android:layout_height=\"wrap_content\">\n$childXml\n$indent</FrameLayout>"
            }
            WidgetType.TEXT_FIELD -> {
                "$indent<EditText\n$nextIndent android:layout_width=\"match_parent\"\n$nextIndent android:layout_height=\"wrap_content\"\n$nextIndent android:hint=\"$text\"\n$nextIndent android:layout_margin=\"${marginDp}dp\" />"
            }
            WidgetType.SWITCH -> {
                "$indent<androidx.appcompat.widget.SwitchCompat\n$nextIndent android:layout_width=\"wrap_content\"\n$nextIndent android:layout_height=\"wrap_content\"\n$nextIndent android:text=\"$text\"\n$nextIndent android:checked=\"$isChecked\" />"
            }
            WidgetType.CHECKBOX -> {
                "$indent<CheckBox\n$nextIndent android:layout_width=\"wrap_content\"\n$nextIndent android:layout_height=\"wrap_content\"\n$nextIndent android:text=\"$text\"\n$nextIndent android:checked=\"$isChecked\" />"
            }
            WidgetType.LAZY_COLUMN -> {
                "$indent<androidx.recyclerview.widget.RecyclerView\n$nextIndent android:layout_width=\"match_parent\"\n$nextIndent android:layout_height=\"match_parent\" />"
            }
        }
    }
}
