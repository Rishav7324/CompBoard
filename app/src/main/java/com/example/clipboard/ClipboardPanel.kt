package com.example.clipboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ClipboardEntry
import com.example.ui.theme.*

@Composable
fun ClipboardPanel(
    entries: List<ClipboardEntry>,
    onItemClick: (ClipboardEntry) -> Unit,
    onPinClick: (ClipboardEntry) -> Unit,
    onDeleteClick: (ClipboardEntry) -> Unit,
    onClearAll: () -> Unit,
    onClose: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.6f)
            .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
            .background(Background)
    ) {
        // Handle
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp, bottom = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .width(48.dp)
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(Color(0xFF38383A))
            )
        }
        
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Clipboard", color = TextPrimary, fontWeight = FontWeight.ExtraBold, fontSize = 22.sp)
            Text(
                "Clear All", 
                color = KeyDanger, 
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                modifier = Modifier.clickable { onClearAll() }
            )
        }
        
        // Search
        OutlinedTextField(
            value = "",
            onValueChange = {},
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .height(52.dp),
            placeholder = { Text("Search clipboard...", color = TextSecondary) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextSecondary) },
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedBorderColor = Color.Transparent,
                focusedBorderColor = Primary,
                unfocusedTextColor = TextPrimary,
                focusedTextColor = TextPrimary,
                unfocusedContainerColor = SurfaceVariant,
                focusedContainerColor = SurfaceVariant
            ),
            shape = RoundedCornerShape(14.dp)
        )
        
        if (entries.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("📋", fontSize = 56.sp)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("No clipboard history yet", color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("Copied items will appear here", color = TextSecondary, fontSize = 14.sp)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(entries) { entry ->
                    ClipboardItemCard(
                        entry = entry,
                        onClick = { onItemClick(entry) },
                        onPin = { onPinClick(entry) },
                        onDelete = { onDeleteClick(entry) }
                    )
                }
            }
        }
    }
}

@Composable
fun ClipboardItemCard(
    entry: ClipboardEntry,
    onClick: () -> Unit,
    onPin: () -> Unit,
    onDelete: () -> Unit
) {
    val bgColor = if (entry.isPinned) KeyModifier else Surface
    val borderColor = if (entry.isPinned) Primary.copy(alpha = 0.5f) else Color.Transparent
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        border = androidx.compose.foundation.BorderStroke(1.dp, borderColor)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    text = entry.preview,
                    color = TextPrimary,
                    fontSize = 15.sp,
                    maxLines = 2,
                    lineHeight = 22.sp,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                if (entry.isPinned) {
                    Text("📌", modifier = Modifier.padding(start = 12.dp), fontSize = 16.sp)
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(), 
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (entry.contentType == "secure") "Secure Content" else "Text",
                    color = if (entry.contentType == "secure") Success else TextSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(
                        if (entry.isPinned) "Unpin" else "Pin", 
                        color = Primary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable { onPin() }
                    )
                    Text(
                        "Delete", 
                        color = Error,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable { onDelete() }
                    )
                }
            }
        }
    }
}
