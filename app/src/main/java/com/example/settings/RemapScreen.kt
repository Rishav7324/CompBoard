package com.example.settings

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun RemapScreen(innerPadding: PaddingValues, onBack: () -> Unit) {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("remaps_prefs", Context.MODE_PRIVATE)
    
    var remaps by remember { 
        mutableStateOf(prefs.all.map { it.key to it.value.toString() }.toMap()) 
    }
    var showAddDialog by remember { mutableStateOf(false) }

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
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onBackground)
            }
            Text("Key Remapping", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
        }
        
        if (remaps.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                Text("No remaps configured", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f),
                contentPadding = PaddingValues(16.dp)
            ) {
                items(remaps.size) { index ->
                    val entry = remaps.entries.elementAt(index)
                    val originalKey = entry.key
                    val targetAction = entry.value
                    
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Key Code: $originalKey", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                Text("Mapped to: $targetAction", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            IconButton(
                                onClick = {
                                    prefs.edit().remove(originalKey).apply()
                                    remaps = prefs.all.map { it.key to it.value.toString() }.toMap()
                                }
                            ) {
                                Icon(Icons.Default.Warning, contentDescription = "Remove", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }
        
        Button(
            onClick = { showAddDialog = true },
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            Text("Add Remap")
        }
    }
    
    if (showAddDialog) {
        var originalKeyCode by remember { mutableStateOf("") }
        var targetAction by remember { mutableStateOf("") }
        
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("New Remap") },
            text = {
                Column {
                    OutlinedTextField(
                        value = originalKeyCode,
                        onValueChange = { originalKeyCode = it },
                        label = { Text("Original KeyCode (e.g. 29 for A)") },
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = targetAction,
                        onValueChange = { targetAction = it },
                        label = { Text("Target Action (e.g. CHAR:B or SHORTCUT:COPY)") },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (originalKeyCode.isNotEmpty() && targetAction.isNotEmpty()) {
                            prefs.edit().putString(originalKeyCode, targetAction).apply()
                            remaps = prefs.all.map { it.key to it.value.toString() }.toMap()
                            showAddDialog = false
                        }
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
