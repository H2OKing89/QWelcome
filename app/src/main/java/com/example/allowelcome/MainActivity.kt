package com.example.allowelcome

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.allowelcome.ui.CustomerIntakeScreen
import com.example.allowelcome.ui.settings.SettingsScreen
import com.example.allowelcome.ui.theme.ALLOWelcomeTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ALLOWelcomeTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    var showSettings by rememberSaveable { mutableStateOf(false) }

                    if (showSettings) {
                        SettingsScreen(onBack = { showSettings = false })
                    } else {
                        CustomerIntakeScreen(onOpenSettings = { showSettings = true })
                    }
                }
            }
        }
    }
}
