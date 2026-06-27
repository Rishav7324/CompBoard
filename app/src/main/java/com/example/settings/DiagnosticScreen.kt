package com.example.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ime.ModifierState
import kotlin.math.max

@Composable
fun DiagnosticScreen(innerPadding: PaddingValues, onBack: () -> Unit) {
    val frequencies = ModifierState.keyPressFrequencies.toMap()
    val maxFreq = max(1, frequencies.values.maxOrNull() ?: 1)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .background(MaterialTheme.colorScheme.background)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onBackground)
            }
            Text("Diagnostic Dashboard", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
        }

        if (frequencies.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No key press data available.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            val totalKeystrokes = frequencies.values.sum()
            val timeElapsedSec = max(1L, (System.currentTimeMillis() - ModifierState.firstKeyPressTime) / 1000L)
            val kps = String.format(java.util.Locale.US, "%.2f", totalKeystrokes.toFloat() / timeElapsedSec.toFloat())
            
            val modifierKeys = setOf("Ctrl", "Shift", "Alt", "Meta", "Caps")
            val mostUsedModifier = frequencies.filterKeys { it in modifierKeys }
                .maxByOrNull { it.value }?.key ?: "None"
                
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Summary", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Total Keystrokes: $totalKeystrokes", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("Avg KPS: $kps", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Most Used Modifier: $mostUsedModifier", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val sortedEntries = frequencies.entries.sortedByDescending { it.value }
                items(sortedEntries) { entry ->
                    val intensity = entry.value.toFloat() / maxFreq.toFloat()
                    val heatColor = Color(
                        red = 1f,
                        green = 1f - intensity * 0.8f,
                        blue = 1f - intensity * 0.8f
                    )
                    
                    Card(
                        modifier = Modifier.fillMaxWidth().aspectRatio(1f),
                        colors = CardDefaults.cardColors(containerColor = heatColor),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize().padding(8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = entry.key,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black,
                                maxLines = 1,
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${entry.value}",
                                color = Color.DarkGray,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
        }
    }
}
