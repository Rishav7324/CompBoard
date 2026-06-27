package com.example

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      MyApplicationTheme {
        Scaffold(
          topBar = {
            @OptIn(ExperimentalMaterial3Api::class)
            TopAppBar(
              title = { Text("⚙ Settings") },
              colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
          },
          modifier = Modifier.fillMaxSize()
        ) { innerPadding ->
          SettingsScreen(
            modifier = Modifier.padding(innerPadding),
            onOpenSystemSettings = { startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS)) },
            onOpenAccessibility = { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }
          )
        }
      }
    }
  }
}

@Composable
fun SettingsScreen(modifier: Modifier = Modifier, onOpenSystemSettings: () -> Unit, onOpenAccessibility: () -> Unit) {
  val context = LocalContext.current
  LazyColumn(
    modifier = modifier.fillMaxSize().padding(horizontal = 16.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp)
  ) {
    item {
      Spacer(Modifier.height(8.dp))
      ProfileSelector()
    }
    item {
      SettingsSection("🎨 Appearance") {
        SettingsItem("Theme", "AMOLED (Default)")
        SettingsItem("Key Height", "Normal")
        SettingsItem("Font", "Monospace")
      }
    }
    item {
      SettingsSection("⌨ Keys & Input") {
        SettingsSwitch("Swap Ctrl & Caps Lock", false)
        SettingsSwitch("Double-tap Shift for Caps", true)
        SettingsItem("Long Press Delay", "400ms")
      }
    }
    item {
      SettingsSection("👆 Gestures") {
        SettingsSwitch("Spacebar Left/Right to Move Cursor", true)
        SettingsSwitch("Spacebar Up for Clipboard", true)
        SettingsSwitch("Backspace Swipe to Delete Word", true)
      }
    }
    item {
      SettingsSection("🚀 Startup & Battery") {
        SettingsButton("Enable CompBoard", "System input settings", onOpenSystemSettings)
        SettingsButton("Disable Battery Optimization", "Keep service alive", onClick = {
          val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
            data = Uri.parse("package:${context.packageName}")
          }
          context.startActivity(intent)
        })
      }
    }
    item {
      SettingsSection("♿ Accessibility") {
        SettingsButton("Enable Accessibility Service", "Required for global shortcuts", onOpenAccessibility)
      }
    }
    item {
      SettingsSection("ℹ About") {
        Text("CompBoard v1.0.0\nPC Keyboard for Android", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
      }
    }
    item { Spacer(Modifier.height(32.dp)) }
  }
}

@Composable
fun ProfileSelector() {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(8.dp))
      .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
      .padding(12.dp)
      .clickable { },
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically
  ) {
    Text("Active Profile", color = MaterialTheme.colorScheme.onBackground)
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
      Text("🎮 Gaming", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
      Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
    }
  }
}

@Composable
fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(12.dp))
      .background(MaterialTheme.colorScheme.surface)
      .padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp)
  ) {
    Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
    content()
  }
}

@Composable
fun SettingsItem(title: String, value: String) {
  Row(modifier = Modifier.fillMaxWidth().clickable {  }, horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
    Text(title, color = MaterialTheme.colorScheme.onBackground)
    Text(value, color = MaterialTheme.colorScheme.onSurfaceVariant)
  }
}

@Composable
fun SettingsSwitch(title: String, initialValue: Boolean) {
  var checked by remember { mutableStateOf(initialValue) }
  Row(modifier = Modifier.fillMaxWidth().clickable { checked = !checked }, horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
    Text(title, color = MaterialTheme.colorScheme.onBackground)
    Switch(checked = checked, onCheckedChange = { checked = it })
  }
}

@Composable
fun SettingsButton(title: String, subtitle: String, onClick: () -> Unit) {
  Column(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 4.dp)) {
    Text(title, color = MaterialTheme.colorScheme.onBackground)
    Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
  }
}
