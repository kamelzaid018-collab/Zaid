package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.example.engine.ProjectManager
import com.example.model.StudioProject
import com.example.ui.screens.ProjectDashboardScreen
import com.example.ui.screens.StudioWorkspaceScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.StudioDarkBg

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    val projectManager = ProjectManager(applicationContext)

    setContent {
      MyApplicationTheme {
        var currentProject by remember { mutableStateOf<StudioProject?>(null) }

        Surface(
          modifier = Modifier.fillMaxSize(),
          color = StudioDarkBg
        ) {
          if (currentProject == null) {
            ProjectDashboardScreen(
              projectManager = projectManager,
              onOpenProject = { currentProject = it }
            )
          } else {
            StudioWorkspaceScreen(
              project = currentProject!!,
              projectManager = projectManager,
              onBackToDashboard = { currentProject = null }
            )
          }
        }
      }
    }
  }
}

