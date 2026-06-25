package com.example.activitytron

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.activitytron.data.local.ActivityDatabase
import com.example.activitytron.data.repository.ActivityRepository
import com.example.activitytron.ui.screens.ActivityListScreen
import com.example.activitytron.ui.theme.ActivitytronTheme
import com.example.activitytron.ui.viewmodel.ActivityViewModel
import com.example.activitytron.ui.viewmodel.ActivityViewModelFactory

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.isSystemInDarkTheme

class MainActivity : ComponentActivity() {

    private val database by lazy { ActivityDatabase.getDatabase(this) }
    private val repository by lazy { ActivityRepository(database.activityDao()) }
    private val viewModel: ActivityViewModel by viewModels {
        ActivityViewModelFactory(repository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val systemDark = isSystemInDarkTheme()
            var isDarkTheme by remember { mutableStateOf(systemDark) }

            ActivitytronTheme(darkTheme = isDarkTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    ActivityListScreen(
                        viewModel = viewModel,
                        isDarkTheme = isDarkTheme,
                        onThemeToggle = { isDarkTheme = !isDarkTheme }
                    )
                }
            }
        }
    }
}
