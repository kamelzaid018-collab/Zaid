package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
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
import com.example.engine.AiFeatureEngine
import com.example.model.StudioProject
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun AiFeatureEngineView(
    project: StudioProject,
    onFeatureApplied: () -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    var userPrompt by remember { mutableStateOf("") }
    var aiState by remember { mutableStateOf<AiFeatureEngine.AiState>(AiFeatureEngine.AiState.Idle) }
    var selectedDiffIndex by remember { mutableIntStateOf(0) }

    val presetPrompts = listOf(
        "Add Dark Theme & Night Mode Toggle",
        "Add Camera & Media Runtime Permissions",
        "Add Local Offline Cache Storage",
        "Add Share Intent & File Export Provider",
        "Add Custom Floating Action Button & Toast"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(StudioDarkBg)
    ) {
        // AI Feature Engine Top Header
        Surface(
            color = StudioDarkSurface,
            tonalElevation = 3.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = StudioAccentPurple.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = StudioAccentPurple)
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column {
                    Text(
                        text = "AI Feature Addition Engine",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = StudioTextPrimary
                    )
                    Text(
                        text = "Describe desired feature -> AI analyzes architecture -> Review Diff -> Apply",
                        fontSize = 11.sp,
                        color = StudioTextSecondary
                    )
                }
            }
        }

        // Main Content Area
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(12.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Prompt Card
            Card(
                colors = CardDefaults.cardColors(containerColor = StudioDarkSurface),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "DESCRIBE FEATURE TO ADD",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = StudioAccentBlue,
                        letterSpacing = 1.sp
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = userPrompt,
                        onValueChange = { userPrompt = it },
                        placeholder = {
                            Text("e.g. Add dark theme toggle with dynamic background colors, or add offline persistence...")
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 90.dp),
                        shape = RoundedCornerShape(8.dp)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Preset Quick Suggestions
                    Text("Preset Ideas:", fontSize = 11.sp, color = StudioTextSecondary)
                    Spacer(modifier = Modifier.height(4.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(presetPrompts) { preset ->
                            SuggestionChip(
                                onClick = { userPrompt = preset },
                                label = { Text(preset, fontSize = 10.sp) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            if (userPrompt.isNotBlank()) {
                                aiState = AiFeatureEngine.AiState.Analyzing("Scanning project source code & manifest...")
                                coroutineScope.launch {
                                    try {
                                        val plan = AiFeatureEngine.analyzeAndPlanFeature(project, userPrompt)
                                        aiState = AiFeatureEngine.AiState.PlanReady(plan)
                                        selectedDiffIndex = 0
                                    } catch (e: Exception) {
                                        aiState = AiFeatureEngine.AiState.Error("Analysis failed: ${e.message}")
                                    }
                                }
                            }
                        },
                        enabled = userPrompt.isNotBlank() && aiState !is AiFeatureEngine.AiState.Analyzing && aiState !is AiFeatureEngine.AiState.Applying,
                        colors = ButtonDefaults.buttonColors(containerColor = StudioAccentPurple),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Icon(Icons.Default.Psychology, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Analyze & Draft Modification Plan", fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Status: Analyzing or Applying
            when (val state = aiState) {
                is AiFeatureEngine.AiState.Analyzing -> {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = StudioDarkCard),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(color = StudioAccentPurple, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(state.message, color = StudioTextPrimary, fontSize = 13.sp)
                        }
                    }
                }

                is AiFeatureEngine.AiState.Applying -> {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = StudioDarkCard),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(color = StudioAccentGreen, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(state.progress, color = StudioTextPrimary, fontSize = 13.sp)
                        }
                    }
                }

                is AiFeatureEngine.AiState.Success -> {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = StudioDarkCard),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = StudioAccentGreen)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Changes Applied to Project", fontWeight = FontWeight.Bold, color = StudioAccentGreen)
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(state.message, fontSize = 12.sp, color = StudioTextSecondary)
                        }
                    }
                }

                is AiFeatureEngine.AiState.Error -> {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = StudioDarkCard),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Error, contentDescription = null, tint = StudioAccentRed)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(state.error, color = StudioAccentRed, fontSize = 12.sp)
                        }
                    }
                }

                is AiFeatureEngine.AiState.PlanReady -> {
                    val plan = state.plan

                    // Plan Overview Card
                    Card(
                        colors = CardDefaults.cardColors(containerColor = StudioDarkSurface),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Lightbulb, contentDescription = null, tint = StudioAccentYellow)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(plan.featureTitle, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = StudioTextPrimary)
                            }

                            Spacer(modifier = Modifier.height(6.dp))
                            Text(plan.summary, fontSize = 12.sp, color = StudioTextSecondary)

                            Spacer(modifier = Modifier.height(10.dp))

                            // Security & Permissions Badge
                            Surface(
                                color = StudioDarkCard,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Shield, contentDescription = null, tint = StudioAccentBlue, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text("Security & Sandboxing Assessment", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = StudioAccentBlue)
                                        Text(plan.securityAssessment, fontSize = 11.sp, color = StudioTextSecondary)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Text("Implementation Steps:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = StudioTextPrimary)
                            plan.estimatedSteps.forEachIndexed { i, step ->
                                Text("  ${i + 1}. $step", fontSize = 11.sp, color = StudioTextSecondary)
                            }
                        }
                    }

                    // Diff Inspection Card
                    Card(
                        colors = CardDefaults.cardColors(containerColor = StudioDarkSurface),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = "FILE MODIFICATION DIFFS (${plan.diffPlans.size})",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = StudioAccentGreen,
                                letterSpacing = 1.sp
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            // File selection tabs
                            ScrollableTabRow(
                                selectedTabIndex = selectedDiffIndex,
                                containerColor = StudioDarkCard,
                                edgePadding = 4.dp
                            ) {
                                plan.diffPlans.forEachIndexed { index, diff ->
                                    Tab(
                                        selected = selectedDiffIndex == index,
                                        onClick = { selectedDiffIndex = index },
                                        text = {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Badge(
                                                    containerColor = when (diff.action) {
                                                        "CREATE" -> StudioAccentGreen
                                                        "DELETE" -> StudioAccentRed
                                                        else -> StudioAccentBlue
                                                    }
                                                ) {
                                                    Text(diff.action.take(3), fontSize = 9.sp)
                                                }
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(diff.relativeFilePath.substringAfterLast('/'), fontSize = 11.sp)
                                            }
                                        }
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            val currentDiff = plan.diffPlans.getOrNull(selectedDiffIndex)
                            if (currentDiff != null) {
                                Text(
                                    text = "Path: ${currentDiff.relativeFilePath}",
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = StudioAccentBlue
                                )
                                Text(
                                    text = "Description: ${currentDiff.description}",
                                    fontSize = 11.sp,
                                    color = StudioTextSecondary
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                // Code snippet preview
                                Surface(
                                    color = StudioDarkCard,
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(1.dp, StudioDarkBorder, RoundedCornerShape(8.dp))
                                        .padding(8.dp)
                                ) {
                                    Column {
                                        Text(
                                            text = "+ Code Added / Injected:",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = StudioAccentGreen
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Box(modifier = Modifier.horizontalScroll(rememberScrollState())) {
                                            Text(
                                                text = currentDiff.newSnippet,
                                                fontFamily = FontFamily.Monospace,
                                                fontSize = 11.sp,
                                                color = StudioTextPrimary
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Apply Changes Button
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                OutlinedButton(
                                    onClick = { aiState = AiFeatureEngine.AiState.Idle },
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("Discard Plan")
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                Button(
                                    onClick = {
                                        aiState = AiFeatureEngine.AiState.Applying("Applying modifications to project files...")
                                        coroutineScope.launch {
                                            val success = AiFeatureEngine.applyPlanToProject(project, plan) { progress ->
                                                aiState = AiFeatureEngine.AiState.Applying(progress)
                                            }
                                            if (success) {
                                                aiState = AiFeatureEngine.AiState.Success("Applied ${plan.diffPlans.size} file changes to project successfully.")
                                                onFeatureApplied()
                                            } else {
                                                aiState = AiFeatureEngine.AiState.Error("Failed to apply some file modifications.")
                                            }
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = StudioAccentGreen),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Apply Changes to Project", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                AiFeatureEngine.AiState.Idle -> {
                    // Ready state
                }
            }
        }
    }
}
