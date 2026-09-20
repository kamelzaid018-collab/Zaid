package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine.AiFeatureEngine
import com.example.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

object AiStudioConfig {
    private const val PREF_NAME = "apk_feature_studio_ai_prefs"
    private const val KEY_API_KEY = "google_ai_studio_api_key"
    private var appContext: Context? = null

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    fun getApiKey(context: Context? = appContext): String {
        val ctx = context ?: appContext
        if (ctx != null) {
            val prefs = ctx.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            val saved = prefs.getString(KEY_API_KEY, "") ?: ""
            if (saved.isNotBlank() && saved != "MY_GEMINI_API_KEY") return saved
        }
        return try {
            com.example.BuildConfig.GEMINI_API_KEY
        } catch (e: Throwable) {
            ""
        }
    }

    fun setApiKey(context: Context?, key: String) {
        val ctx = context ?: appContext
        ctx?.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            ?.edit()?.putString(KEY_API_KEY, key.trim())?.apply()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoogleAiStudioSettingsDialog(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var apiKeyInput by remember { mutableStateOf(AiStudioConfig.getApiKey(context)) }
    var passwordVisible by remember { mutableStateOf(false) }
    var testStatus by remember { mutableStateOf<String?>(null) }
    var isTesting by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = StudioAccentPurple)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Google AI Studio Account Connection", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = StudioTextPrimary)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "ربط التطبيق بحسابك على Google AI Studio. أدخل مفتاح API الخاص بك من حسابك على Google AI Studio (aistudio.google.com) لربط التطبيق مباشرة بنماذجك وحسابك لتوليد وتحليل وتعديل التطبيقات بالذكاء الاصطناعي.",
                    fontSize = 12.sp,
                    color = StudioTextSecondary
                )

                OutlinedTextField(
                    value = apiKeyInput,
                    onValueChange = { apiKeyInput = it },
                    label = { Text("Google AI Studio API Key (AIza...)") },
                    singleLine = true,
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = null
                            )
                        }
                    },
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = StudioDarkSurface,
                        unfocusedContainerColor = StudioDarkSurface
                    )
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://aistudio.google.com/"))
                            try {
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    ) {
                        Icon(Icons.Default.OpenInBrowser, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Get API Key from AI Studio", fontSize = 11.sp)
                    }

                    Button(
                        onClick = {
                            AiStudioConfig.setApiKey(context, apiKeyInput)
                            testStatus = "Saved successfully!"
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = StudioAccentGreen),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Save Key", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                if (testStatus != null) {
                    Surface(
                        color = StudioDarkSurface,
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = testStatus!!,
                            fontSize = 12.sp,
                            color = StudioAccentGreen,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }

                Divider(color = StudioDarkBorder)

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Benefits of Google AI Studio Connection:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = StudioTextPrimary)
                    Text("• Deep AST & Smali semantic understanding", fontSize = 11.sp, color = StudioTextSecondary)
                    Text("• Direct code diff modifications & syntax checks", fontSize = 11.sp, color = StudioTextSecondary)
                    Text("• Instant AI Error Fixer for compilation failures", fontSize = 11.sp, color = StudioTextSecondary)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    AiStudioConfig.setApiKey(context, apiKeyInput)
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = StudioAccentBlue)
            ) {
                Text("Done")
            }
        },
        containerColor = StudioDarkBg
    )
}
