package com.example.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ime.ModifierState
import kotlin.math.max

@Composable
fun DiagnosticScreen(innerPadding: PaddingValues, onBack: () -> Unit) {
    val context = LocalContext.current
    val frequencies = ModifierState.keyPressFrequencies.toMap()
    val maxFreq = max(1, frequencies.values.maxOrNull() ?: 1)
    
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabTitles = listOf("Statistics", "Live Debug Logs")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Toolbar Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack, 
                    contentDescription = "Back", 
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Diagnostic Dashboard", 
                style = MaterialTheme.typography.titleLarge, 
                fontWeight = FontWeight.Bold, 
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        // Segmented Tabs
        TabRow(
            selectedTabIndex = selectedTabIndex,
            containerColor = MaterialTheme.colorScheme.background,
            contentColor = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 16.dp)
        ) {
            tabTitles.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTabIndex == index,
                    onClick = { selectedTabIndex = index },
                    text = { 
                        Text(
                            text = title, 
                            fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Normal
                        ) 
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        when (selectedTabIndex) {
            0 -> {
                // Statistics / Heatmap Tab
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
            1 -> {
                // Live Debug Logs Tab
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp)
                ) {
                    // Control Row for Logs
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Log History (${ModifierState.debugLogs.size}/50)",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            // Download Logs Button
                            IconButton(
                                onClick = {
                                    if (ModifierState.debugLogs.isEmpty()) {
                                        android.widget.Toast.makeText(context, "No logs to download", android.widget.Toast.LENGTH_SHORT).show()
                                        return@IconButton
                                    }
                                    val textContent = ModifierState.debugLogs.joinToString("\n")
                                    try {
                                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                                            val resolver = context.contentResolver
                                            val contentValues = android.content.ContentValues().apply {
                                                put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, "keyboard_debug_log_${System.currentTimeMillis()}.txt")
                                                put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "text/plain")
                                                put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, android.os.Environment.DIRECTORY_DOWNLOADS)
                                            }
                                            val uri = resolver.insert(android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                                            if (uri != null) {
                                                resolver.openOutputStream(uri)?.use { outputStream ->
                                                    outputStream.write(textContent.toByteArray())
                                                }
                                                android.widget.Toast.makeText(context, "Log saved to Downloads", android.widget.Toast.LENGTH_SHORT).show()
                                            }
                                        } else {
                                            val file = java.io.File(context.getExternalFilesDir(android.os.Environment.DIRECTORY_DOWNLOADS), "keyboard_debug_log_${System.currentTimeMillis()}.txt")
                                            file.writeText(textContent)
                                            android.widget.Toast.makeText(context, "Log saved to App Downloads", android.widget.Toast.LENGTH_SHORT).show()
                                        }
                                    } catch (e: Exception) {
                                        android.widget.Toast.makeText(context, "Error saving log: ${e.localizedMessage}", android.widget.Toast.LENGTH_LONG).show()
                                    }
                                }
                            ) {
                                Icon(Icons.Default.Share, contentDescription = "Export Logs", tint = MaterialTheme.colorScheme.primary)
                            }

                            // Clear Logs Button
                            IconButton(
                                onClick = {
                                    ModifierState.debugLogs.clear()
                                    android.widget.Toast.makeText(context, "Logs cleared", android.widget.Toast.LENGTH_SHORT).show()
                                }
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Clear Logs", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }

                    if (ModifierState.debugLogs.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .weight(1f)
                                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No log messages received yet.\nStart typing on the CompBoard keyboard to see logs here.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    } else {
                        // Display Logs List
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .background(Color(0xFF0F1117), RoundedCornerShape(12.dp))
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            // Reverse the logs so latest comes at the top
                            items(ModifierState.debugLogs.reversed()) { logMsg ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Text(
                                        text = "> ",
                                        color = Color(0xFF4F8CFF),
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = logMsg,
                                        color = Color.White,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}
