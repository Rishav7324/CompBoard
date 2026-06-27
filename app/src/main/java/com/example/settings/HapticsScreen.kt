package com.example.settings

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HapticsScreen(innerPadding: PaddingValues, onBack: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("haptics_prefs", Context.MODE_PRIVATE) }
    
    var hapticsEnabled by remember { mutableStateOf(prefs.getBoolean("haptics_enabled", true)) }
    var intensity by remember { mutableFloatStateOf(prefs.getFloat("haptics_intensity", 0.5f)) }
    var duration by remember { mutableFloatStateOf(prefs.getFloat("haptics_duration", 20f)) }

    LaunchedEffect(hapticsEnabled) { prefs.edit().putBoolean("haptics_enabled", hapticsEnabled).apply() }
    LaunchedEffect(intensity) { prefs.edit().putFloat("haptics_intensity", intensity).apply() }
    LaunchedEffect(duration) { prefs.edit().putFloat("haptics_duration", duration).apply() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .background(MaterialTheme.colorScheme.background)
    ) {
        TopAppBar(
            title = { Text("Haptic Feedback") },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                titleContentColor = MaterialTheme.colorScheme.primary
            )
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Enable Haptic Feedback", fontWeight = FontWeight.Bold)
                            Text("Vibrate on keypress", style = MaterialTheme.typography.bodySmall)
                        }
                        Switch(checked = hapticsEnabled, onCheckedChange = { hapticsEnabled = it })
                    }
                }
            }
            
            if (hapticsEnabled) {
                item {
                    Text(
                        text = "Vibration Profile",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                item {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Intensity", fontWeight = FontWeight.Bold)
                                Text("${(intensity * 100).toInt()}%")
                            }
                            Slider(
                                value = intensity,
                                onValueChange = { intensity = it },
                                valueRange = 0f..1f,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                    }
                }

                item {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Duration (ms)", fontWeight = FontWeight.Bold)
                                Text("${duration.toInt()} ms")
                            }
                            Slider(
                                value = duration,
                                onValueChange = { duration = it },
                                valueRange = 5f..100f,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                    }
                }
                
                item {
                    Button(
                        onClick = {
                            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as android.os.Vibrator
                            if (vibrator.hasVibrator()) {
                                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                                    val amplitude = (intensity * 255).toInt().coerceIn(1, 255)
                                    vibrator.vibrate(android.os.VibrationEffect.createOneShot(duration.toLong(), amplitude))
                                } else {
                                    @Suppress("DEPRECATION")
                                    vibrator.vibrate(duration.toLong())
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                    ) {
                        Text("Test Feedback")
                    }
                }
            }
        }
    }
}
