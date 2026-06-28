package com.example.settings

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.view.inputmethod.InputMethodManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.ime.CompBoardInputMethodService
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.launch

class SettingsActivity : ComponentActivity() {
    private val requestNotificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { _ ->
            // Re-check happens in onResume
        }

    fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                var isEnabled by remember { mutableStateOf(false) }
                var isDefault by remember { mutableStateOf(false) }
                var hasOverlay by remember { mutableStateOf(false) }
                var hasNotifications by remember { mutableStateOf(false) }
                var isBatteryOptimized by remember { mutableStateOf(false) }
                var hasAccessibility by remember { mutableStateOf(false) }

                val context = LocalContext.current
                val lifecycleOwner = LocalLifecycleOwner.current

                DisposableEffect(lifecycleOwner) {
                    val observer = LifecycleEventObserver { _, event ->
                        if (event == Lifecycle.Event.ON_RESUME) {
                            isEnabled = checkIsEnabled(context)
                            isDefault = checkIsDefault(context)
                            hasOverlay = Settings.canDrawOverlays(context)
                            hasAccessibility = checkIsAccessibilityEnabled(context)
                            hasNotifications = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
                            } else {
                                true
                            }
                            val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
                            isBatteryOptimized = pm.isIgnoringBatteryOptimizations(context.packageName)
                        }
                    }
                    lifecycleOwner.lifecycle.addObserver(observer)
                    onDispose {
                        lifecycleOwner.lifecycle.removeObserver(observer)
                    }
                }

                val isSetupComplete = isEnabled && isDefault && hasOverlay && hasNotifications && isBatteryOptimized && hasAccessibility

                var showTestPage by remember { mutableStateOf(false) }

                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = { Text(if (showTestPage) "Test Keyboard" else if (isSetupComplete) "⚙ Settings" else "🚀 Setup CompBoard") },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                titleContentColor = MaterialTheme.colorScheme.primary,
                            ),
                            navigationIcon = {
                                if (showTestPage) {
                                    IconButton(onClick = { showTestPage = false }) {
                                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                                    }
                                }
                            },
                            actions = {
                                if (isSetupComplete && !showTestPage) {
                                    IconButton(onClick = { showTestPage = true }) {
                                        Icon(Icons.Default.Keyboard, contentDescription = "Test Keyboard")
                                    }
                                }
                            }
                        )
                    }
                ) { innerPadding ->
                    if (showTestPage) {
                        TestKeyboardScreen(innerPadding)
                    } else if (isSetupComplete) {
                        SettingsScreen(innerPadding)
                    } else {
                        SetupScreen(
                            innerPadding = innerPadding,
                            isEnabled = isEnabled,
                            isDefault = isDefault,
                            hasOverlay = hasOverlay,
                            hasAccessibility = hasAccessibility,
                            hasNotifications = hasNotifications,
                            isBatteryOptimized = isBatteryOptimized,
                            onRequestNotification = { requestNotificationPermission() }
                        )
                    }
                }
            }
        }
    }

    private fun checkIsAccessibilityEnabled(context: Context): Boolean {
        var accessibilityEnabled = 0
        try {
            accessibilityEnabled = Settings.Secure.getInt(
                context.contentResolver,
                Settings.Secure.ACCESSIBILITY_ENABLED
            )
        } catch (e: Settings.SettingNotFoundException) {
            e.printStackTrace()
        }
        val accessibilityFound = android.text.TextUtils.SimpleStringSplitter(':')
        if (accessibilityEnabled == 1) {
            val settingValue = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            )
            if (settingValue != null) {
                accessibilityFound.setString(settingValue)
                while (accessibilityFound.hasNext()) {
                    val accessibilityService = accessibilityFound.next()
                    if (accessibilityService.equals(
                            ComponentName(context, "com.example.accessibility.CompBoardAccessibilityService").flattenToString(),
                            ignoreCase = true
                        )
                    ) {
                        return true
                    }
                }
            }
        }
        return false
    }

    private fun checkIsEnabled(context: Context): Boolean {
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        val myImeId = ComponentName(context, CompBoardInputMethodService::class.java).flattenToShortString()
        return imm.enabledInputMethodList.any { it.id == myImeId }
    }

    private fun checkIsDefault(context: Context): Boolean {
        val myImeId = ComponentName(context, CompBoardInputMethodService::class.java).flattenToShortString()
        val defaultImeId = Settings.Secure.getString(context.contentResolver, Settings.Secure.DEFAULT_INPUT_METHOD)
        return myImeId == defaultImeId
    }
}

@Composable
fun SetupScreen(
    innerPadding: PaddingValues,
    isEnabled: Boolean,
    isDefault: Boolean,
    hasOverlay: Boolean,
    hasAccessibility: Boolean,
    hasNotifications: Boolean,
    isBatteryOptimized: Boolean,
    onRequestNotification: () -> Unit
) {
    val context = LocalContext.current
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(24.dp)
    ) {
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(120.dp)
                            .background(
                                androidx.compose.ui.graphics.Brush.linearGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                        Color(0xFF7B5CF6).copy(alpha = 0.05f)
                                    )
                                ),
                                CircleShape
                            )
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    androidx.compose.ui.graphics.Brush.linearGradient(
                                        colors = listOf(
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                                            Color.Transparent
                                        )
                                    ),
                                    CircleShape
                                )
                                .padding(12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Keyboard,
                                contentDescription = "Setup",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(64.dp)
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Text(
                        text = "Unleash Your Setup",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Black,
                            letterSpacing = (-0.5).sp
                        ),
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    Text(
                        text = "Complete these final steps to configure your premium keyboard experience.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 32.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }

            val steps = listOf(
                Triple(isEnabled, "1", "Enable Keyboard" to "Turn on CompBoard in your system settings."),
                Triple(isDefault, "2", "Select Keyboard" to "Set CompBoard as your default input method."),
                Triple(hasOverlay, "3", "Overlay Permission" to "Required to show the clipboard history panel over other apps."),
                Triple(hasAccessibility, "4", "Accessibility" to "Global shortcuts require accessibility permissions."),
                Triple(hasNotifications, "5", "Notifications" to "Keep the background service alive for clipboard listening."),
                Triple(isBatteryOptimized, "6", "Battery Optimization" to "Prevent the system from killing the background service.")
            )

            val currentStepIndex = steps.indexOfFirst { !it.first }.takeIf { it != -1 } ?: steps.size

            SetupStepCard(
                stepNumber = "1",
                title = "Enable Keyboard",
                description = "Turn on CompBoard in your system settings.",
                isComplete = isEnabled,
                isActive = currentStepIndex == 0,
                onClick = {
                    val intent = Intent(Settings.ACTION_INPUT_METHOD_SETTINGS)
                    context.startActivity(intent)
                }
            )

            SetupStepCard(
                stepNumber = "2",
                title = "Select Keyboard",
                description = "Set CompBoard as your default input method.",
                isComplete = isDefault,
                isActive = currentStepIndex == 1,
                enabled = isEnabled,
                onClick = {
                    val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                    imm.showInputMethodPicker()
                }
            )

            SetupStepCard(
                stepNumber = "3",
                title = "Overlay Permission",
                description = "Required to show the clipboard history panel over other apps.",
                isComplete = hasOverlay,
                isActive = currentStepIndex == 2,
                onClick = {
                    val intent = Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:${context.packageName}")
                    )
                    context.startActivity(intent)
                }
            )

            SetupStepCard(
                stepNumber = "4",
                title = "Accessibility Permission",
                description = "Required to intercept hardware hotkeys outside the app.",
                isComplete = hasAccessibility,
                isActive = currentStepIndex == 3,
                onClick = {
                    val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                    context.startActivity(intent)
                }
            )

            SetupStepCard(
                stepNumber = "5",
                title = "Notifications Permission",
                description = "Required to keep the background service alive for clipboard listening.",
                isComplete = hasNotifications,
                isActive = currentStepIndex == 4,
                onClick = {
                    onRequestNotification()
                }
            )

            SetupStepCard(
                stepNumber = "6",
                title = "Battery Optimization",
                description = "Prevent the system from killing the background service.",
                isComplete = isBatteryOptimized,
                isActive = currentStepIndex == 5,
                onClick = {
                    val intent = Intent(
                        Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                        Uri.parse("package:${context.packageName}")
                    )
                    context.startActivity(intent)
                }
            )
        }
    }
}

@Composable
fun SetupStepCard(
    stepNumber: String,
    title: String,
    description: String,
    isComplete: Boolean,
    isActive: Boolean,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    val containerColor = when {
        isComplete -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        isActive -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
        else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
    }
    
    val borderColor = when {
        isActive -> MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
        else -> Color.Transparent
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable(enabled = enabled && !isComplete, onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = androidx.compose.foundation.BorderStroke(1.dp, borderColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        when {
                            isComplete -> Color(0xFF30D158).copy(alpha = 0.2f)
                            isActive -> MaterialTheme.colorScheme.primary
                            else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                        },
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isComplete) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Completed",
                        tint = Color(0xFF30D158),
                        modifier = Modifier.size(24.dp)
                    )
                } else {
                    Text(
                        text = stepNumber,
                        color = if (isActive) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isActive || isComplete) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = if (isActive || isComplete) 1f else 0.6f)
                )
            }
        }
    }
}

@Composable
fun SettingsScreen(innerPadding: PaddingValues) {
    val context = LocalContext.current
    val settingsManager = remember { SettingsManager(context) }
    val profileStr by settingsManager.profileFlow.collectAsState(initial = "WINDOWS")
    val keyStyleStr by settingsManager.keyStyleFlow.collectAsState(initial = "MECHANICAL")
    val scope = rememberCoroutineScope()

    var showShortcuts by remember { mutableStateOf(false) }
    var showHaptics by remember { mutableStateOf(false) }
    var showRemap by remember { mutableStateOf(false) }
    var showDiagnostics by remember { mutableStateOf(false) }

    if (showDiagnostics) {
        DiagnosticScreen(
            innerPadding = innerPadding,
            onBack = { showDiagnostics = false }
        )
        return
    }

    if (showShortcuts) {
        ShortcutsScreen(
            innerPadding = innerPadding,
            onBack = { showShortcuts = false }
        )
        return
    }

    if (showHaptics) {
        HapticsScreen(
            innerPadding = innerPadding,
            onBack = { showHaptics = false }
        )
        return
    }

    if (showRemap) {
        RemapScreen(
            innerPadding = innerPadding,
            onBack = { showRemap = false }
        )
        return
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(16.dp)
    ) {
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .background(
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("CB", style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("CompBoard", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("Version 1.0.0", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            SettingsSectionCard("Appearance") {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Theme", 
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // AMOLED Theme Card
                        Card(
                            modifier = Modifier.weight(1f).aspectRatio(4f/3f),
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primaryContainer),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Column(
                                modifier = Modifier.fillMaxSize().padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().height(24.dp).background(Color(0xFF000000), RoundedCornerShape(4.dp)).padding(2.dp),
                                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Box(modifier = Modifier.weight(1f).fillMaxHeight().background(Color(0xFF1E1E1E), RoundedCornerShape(2.dp)))
                                    Box(modifier = Modifier.weight(1f).fillMaxHeight().background(Color(0xFF1E1E1E), RoundedCornerShape(2.dp)))
                                    Box(modifier = Modifier.width(12.dp).fillMaxHeight().background(Color(0xFF7B5CF6), RoundedCornerShape(2.dp)))
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                                Text("AMOLED", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                            }
                        }
                        
                        // HACKER Theme Card
                        Card(
                            modifier = Modifier.weight(1f).aspectRatio(4f/3f),
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Column(
                                modifier = Modifier.fillMaxSize().padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().height(24.dp).background(Color(0xFF0D1117), RoundedCornerShape(4.dp)).padding(2.dp),
                                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Box(modifier = Modifier.weight(1f).fillMaxHeight().background(Color(0xFF161B22), RoundedCornerShape(2.dp)))
                                    Box(modifier = Modifier.weight(1f).fillMaxHeight().background(Color(0xFF161B22), RoundedCornerShape(2.dp)))
                                    Box(modifier = Modifier.width(12.dp).fillMaxHeight().background(Color(0xFF238636), RoundedCornerShape(2.dp)))
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                                Text("HACKER", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))
                    Text(
                        "Keyboard Key Design", 
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Classic Mechanical Card
                        val isMech = keyStyleStr == "MECHANICAL"
                        val mechBorderColor = if (isMech) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                        val mechBgColor = if (isMech) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        
                        Card(
                            modifier = Modifier.weight(1f).aspectRatio(4f/3f).clickable {
                                scope.launch {
                                    settingsManager.setKeyStyle("MECHANICAL")
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(if (isMech) 2.dp else 1.dp, mechBorderColor),
                            colors = CardDefaults.cardColors(containerColor = mechBgColor)
                        ) {
                            Column(
                                modifier = Modifier.fillMaxSize().padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                // Mini Mechanical Key simulation with 3D shadow skirt!
                                Box(
                                    modifier = Modifier
                                        .width(50.dp)
                                        .height(32.dp)
                                ) {
                                    // Base shadow
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(top = 4.dp)
                                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
                                    )
                                    // Key Cap
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .fillMaxHeight()
                                            .padding(bottom = 3.dp)
                                            .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(4.dp))
                                            .border(1.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(4.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("A", color = Color.White, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                    }
                                }
                                Spacer(modifier = Modifier.height(10.dp))
                                Text("Mechanical", color = if (isMech) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                Text("Tactile 3D cap", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall, fontSize = 8.sp)
                            }
                        }
                        
                        // Minimalist Flat Card
                        val isFlat = keyStyleStr == "FLAT"
                        val flatBorderColor = if (isFlat) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                        val flatBgColor = if (isFlat) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        
                        Card(
                            modifier = Modifier.weight(1f).aspectRatio(4f/3f).clickable {
                                scope.launch {
                                    settingsManager.setKeyStyle("FLAT")
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(if (isFlat) 2.dp else 1.dp, flatBorderColor),
                            colors = CardDefaults.cardColors(containerColor = flatBgColor)
                        ) {
                            Column(
                                modifier = Modifier.fillMaxSize().padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                // Mini Flat Key simulation
                                Box(
                                    modifier = Modifier
                                        .width(50.dp)
                                        .height(28.dp)
                                        .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                                        .border(0.5.dp, MaterialTheme.colorScheme.secondary, RoundedCornerShape(4.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("A", color = MaterialTheme.colorScheme.secondary, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                }
                                Spacer(modifier = Modifier.height(10.dp))
                                Text("Flat", color = if (isFlat) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                Text("Sleek minimal", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall, fontSize = 8.sp)
                            }
                        }
                    }
                }
                
                Divider(color = MaterialTheme.colorScheme.surfaceVariant)
                SettingsItem("Key Height", "Normal")
                Divider(color = MaterialTheme.colorScheme.surfaceVariant)
                SettingsItem("Background Blur", "Off")
            }

            Spacer(modifier = Modifier.height(16.dp))
            SettingsSectionCard("Keyboard Profile") {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Choose an optimized layout profile to transform modifier keys, visual designs, and shortcuts dynamically.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    
                    val profiles = listOf("WINDOWS", "LINUX", "MACOS", "TERMUX", "VSCODE", "GAMING")
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        profiles.chunked(3).forEach { rowProfiles ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                rowProfiles.forEach { profile ->
                                    val isSelected = profileStr == profile
                                    val cardBorderColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                                    val cardBgColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                    val textColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                    
                                    Card(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable {
                                                scope.launch {
                                                    settingsManager.setProfile(profile)
                                                }
                                            },
                                        shape = RoundedCornerShape(8.dp),
                                        border = androidx.compose.foundation.BorderStroke(if (isSelected) 2.dp else 1.dp, cardBorderColor),
                                        colors = CardDefaults.cardColors(containerColor = cardBgColor)
                                    ) {
                                        Box(
                                            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp, horizontal = 4.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = profile,
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = textColor
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            SettingsSectionCard("Keys & Input") {
                SettingsItem("Key Repeat Speed", "Normal")
                Divider(color = MaterialTheme.colorScheme.surfaceVariant)
                SettingsItem("Swap Ctrl/Caps", "Off")
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            SettingsSectionCard("Gestures") {
                SettingsItem("Spacebar Swipe", "Move Cursor")
                Divider(color = MaterialTheme.colorScheme.surfaceVariant)
                SettingsItem("Backspace Swipe", "Delete Word")
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            SettingsSectionCard("Haptic & Sound") {
                SettingsItem("Key Vibration", "Configure...", onClick = { showHaptics = true })
                Divider(color = MaterialTheme.colorScheme.surfaceVariant)
                SettingsItem("Key Sounds", "None")
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            SettingsSectionCard("Advanced") {
                SettingsItem("Diagnostic Dashboard", "View Key Press Heatmap", onClick = { showDiagnostics = true })
                Divider(color = MaterialTheme.colorScheme.surfaceVariant)
                SettingsItem("Key Remapping", "Configure...", onClick = { showRemap = true })
                Divider(color = MaterialTheme.colorScheme.surfaceVariant)
                SettingsItem("Shortcuts Engine", "Manage Profiles & Macros", onClick = { showShortcuts = true })
                Divider(color = MaterialTheme.colorScheme.surfaceVariant)
                SettingsItem("Backup & Restore", "Export/Import...")
            }
        }
    }
}

@Composable
fun SettingsSectionCard(title: String, content: @Composable () -> Unit) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp)
    )
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(vertical = 4.dp)) {
            content()
        }
    }
}

@Composable
fun SettingsItem(title: String, subtitle: String, onClick: () -> Unit = {}) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TestKeyboardScreen(innerPadding: PaddingValues) {
    var text by remember { mutableStateOf("const app = initializeApp();\n// Keyboard overlays this area.") }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF050505))
            .padding(innerPadding)
            .imePadding()
            .padding(24.dp)
    ) {
        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Code Editor View",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Black,
                    letterSpacing = (-0.5).sp,
                    fontSize = 24.sp
                ),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            BasicTextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier.fillMaxWidth().background(Color(0xFF1A1A1A), RoundedCornerShape(12.dp)).padding(16.dp),
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                    color = Color(0xFFF5F5F5)
                ),
                cursorBrush = androidx.compose.ui.graphics.SolidColor(Color(0xFF5C3DDD))
            )
        }
        
        androidx.compose.foundation.lazy.LazyRow(
            modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val keywords = listOf("function", "const", "let", "var", "if", "else", "for", "return", "class", "import")
            items(keywords.size) { index ->
                androidx.compose.material3.Surface(
                    color = Color(0xFF1E1E1E),
                    shape = RoundedCornerShape(4.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF333333))
                ) {
                    Text(
                        text = keywords[index],
                        color = Color(0xFFAAAAAA),
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
        }
    }
}

