package com.example.settings

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.ui.theme.MyApplicationTheme

class SettingsActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MyApplicationTheme {
                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = { Text("⚙ Settings") },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                titleContentColor = MaterialTheme.colorScheme.primary,
                            )
                        )
                    }
                ) { innerPadding ->
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                        contentPadding = PaddingValues(16.dp)
                    ) {
                        item {
                            SettingsSectionTitle("Appearance")
                            SettingsItem("Theme", "AMOLED Dark")
                            SettingsItem("Key Height", "Normal")
                            SettingsItem("Background Blur", "Off")
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            SettingsSectionTitle("Keys & Input")
                            SettingsItem("Key Repeat Speed", "Normal")
                            SettingsItem("Swap Ctrl/Caps", "Off")
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            SettingsSectionTitle("Gestures")
                            SettingsItem("Spacebar Swipe", "Move Cursor")
                            SettingsItem("Backspace Swipe", "Delete Word")
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            SettingsSectionTitle("Haptic & Sound")
                            SettingsItem("Key Vibration", "Light")
                            SettingsItem("Key Sounds", "None")
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            SettingsSectionTitle("Advanced")
                            SettingsItem("Key Remapping", "Configure...")
                            SettingsItem("Macros", "Manage...")
                            SettingsItem("Backup & Restore", "Export/Import...")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsSectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(vertical = 8.dp)
    )
}

@Composable
fun SettingsItem(title: String, subtitle: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
    ) {
        Text(text = title, style = MaterialTheme.typography.bodyLarge)
        Text(text = subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
