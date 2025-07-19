package com.perfmode.iqoo // Corrected package declaration

import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.* // Import all runtime components
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.perfmode.iqoo.FeatureControlService
import com.perfmode.iqoo.util.DataStoreManager
import com.perfmode.iqoo.util.FeatureRepository
import com.perfmode.iqoo.util.ShellUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import com.perfmode.iqoo.ui.theme.FeatureTabScreen // Keep this for "Feature Library"
import com.perfmode.iqoo.ui.theme.AboutDialog
import com.perfmode.iqoo.ui.theme.InfoScreen // This import will be removed as InfoScreen is no longer a separate screen
import com.perfmode.iqoo.ui.theme.IqooTweaksTheme
import com.perfmode.iqoo.ui.DashboardScreen
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.Alignment
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.font.FontWeight
import com.perfmode.iqoo.ui.theme.SettingsScreen // Ensure this is imported
import androidx.activity.compose.BackHandler
import com.perfmode.iqoo.OverlayControlService // NEW: Import OverlayControlService
import com.perfmode.iqoo.ui.theme.OverlaySelectionScreen // NEW: Import OverlaySelectionScreen
import androidx.compose.material.icons.automirrored.filled.ArrowBack // NEW: Import ArrowBack for consistency
import androidx.compose.ui.res.painterResource // Import painterResource for custom icon
import androidx.compose.ui.text.style.TextAlign // For placeholder text
import com.perfmode.iqoo.ui.theme.AdvancedSettingsScreen // NEW: Import AdvancedSettingsScreen


// Define screens for Navigation Drawer
sealed class DrawerScreen(val route: String, val title: String, val icon: ImageVector) {
    object Home : DrawerScreen("home", "Home", Icons.Default.Home) // Changed title to "Home" for dashboard
    object FeatureLibrary : DrawerScreen("feature_library", "Feature Library", Icons.Default.Apps)
    object Settings : DrawerScreen("settings", "Settings", Icons.Default.Settings) // Settings now includes info
    object OverlaySelection : DrawerScreen("overlay_selection", "Overlay Selection", Icons.Default.Checklist) // NEW: Overlay Selection screen
    object About : DrawerScreen("about", "About", Icons.Default.Info) // About as a screen
    // NEW: Advanced Settings Section
    object AdvancedSettings : DrawerScreen("advanced_settings", "Advanced Settings", Icons.Default.Tune) // Using Icons.Default.Tune as an example
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppContent( // RENAMED from MainScreen to AppContent
    context: Context,
    onStopClicked: () -> Unit,
    onAboutClicked: () -> Unit, // This will now launch AboutDialog
    onInfoClicked: () -> Unit // This will now launch InfoScreen (which is now SettingsScreen)
) {
    val scope = rememberCoroutineScope()
    val localContext = LocalContext.current
    val enabledToggles by DataStoreManager.getFeatureTogglesFlow(context).collectAsState(initial = emptyMap())

    val allFeatures by FeatureRepository.getAllFeaturesFlow(context).collectAsState(initial = emptyList())

    val activeFeatures = allFeatures.filter { enabledToggles[it.title] == true } // This line defines activeFeatures

    val (runOnceFeatures, loopFeatures) = remember(allFeatures, enabledToggles) { // Depend on allFeatures
        val activeFeatures = allFeatures.filter { enabledToggles[it.title] == true }
        FeatureRepository.filterFeaturesForExecution(activeFeatures)
    }

    // FIXED: Collect selected overlay features from DataStore (re-added for explicit selection)
    val selectedOverlayFeatures by DataStoreManager.getSelectedOverlayFeaturesFlow(context).collectAsState(initial = emptySet())


    LaunchedEffect(loopFeatures) {
        if (loopFeatures.isNotEmpty()) {
            val commandsToLoop = ArrayList(loopFeatures.map { it.command })
            val serviceIntent = Intent(context, FeatureControlService::class.java).apply {
                action = FeatureControlService.ACTION_START_LOOP
                putStringArrayListExtra(FeatureControlService.EXTRA_COMMANDS, commandsToLoop)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent)
            } else {
                context.startService(serviceIntent)
            }
            Toast.makeText(localContext, "Special features started in background.", Toast.LENGTH_SHORT).show()
        } else {
            val serviceIntent = Intent(context, FeatureControlService::class.java).apply {
                action = FeatureControlService.ACTION_STOP_LOOP
            }
            context.stopService(serviceIntent)
            Toast.makeText(localContext, "Special features stopped.", Toast.LENGTH_SHORT).show()
        }
    }

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    var featureLibraryTabIndex by remember { mutableIntStateOf(0) }
    val selectedItem = remember { mutableStateOf<DrawerScreen>(DrawerScreen.Home) }

    // NEW: State to control overlay visibility (Moved to higher scope)
    val isOverlayActive = remember { mutableStateOf(false) }

    BackHandler(enabled = selectedItem.value != DrawerScreen.Home) {
        selectedItem.value = DrawerScreen.Home
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier.width(280.dp),
                drawerContainerColor = MaterialTheme.colorScheme.surfaceVariant
            ) {
                DrawerHeader()
                Spacer(Modifier.height(16.dp))
                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Home, contentDescription = null) },
                    label = { Text(DrawerScreen.Home.title) },
                    selected = selectedItem.value == DrawerScreen.Home,
                    onClick = {
                        scope.launch { drawerState.close() }
                        selectedItem.value = DrawerScreen.Home
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Apps, contentDescription = null) },
                    label = { Text(DrawerScreen.FeatureLibrary.title) },
                    selected = selectedItem.value == DrawerScreen.FeatureLibrary,
                    onClick = {
                        scope.launch { drawerState.close() }
                        selectedItem.value = DrawerScreen.FeatureLibrary
                        featureLibraryTabIndex = 0
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                    label = { Text(DrawerScreen.Settings.title) },
                    selected = selectedItem.value == DrawerScreen.Settings,
                    onClick = {
                        scope.launch { drawerState.close() }
                        selectedItem.value = DrawerScreen.Settings
                        onInfoClicked()
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Checklist, contentDescription = null) },
                    label = { Text(DrawerScreen.OverlaySelection.title) },
                    selected = selectedItem.value == DrawerScreen.OverlaySelection,
                    onClick = {
                        scope.launch { drawerState.close() }
                        selectedItem.value = DrawerScreen.OverlaySelection
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
                // NEW: Navigation Drawer Item for Advanced Settings
                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Tune, contentDescription = null) },
                    label = { Text(DrawerScreen.AdvancedSettings.title) },
                    selected = selectedItem.value == DrawerScreen.AdvancedSettings,
                    onClick = {
                        scope.launch { drawerState.close() }
                        selectedItem.value = DrawerScreen.AdvancedSettings
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
                Divider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Info, contentDescription = null) },
                    label = { Text(DrawerScreen.About.title) },
                    selected = selectedItem.value == DrawerScreen.About,
                    onClick = {
                        scope.launch { drawerState.close() }
                        onAboutClicked()
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(selectedItem.value.title) },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "Menu")
                        }
                    },
                    actions = { }
                )
            },
            bottomBar = {
                BottomAppBar(
                    modifier = Modifier.fillMaxWidth(),
                    containerColor = MaterialTheme.colorScheme.surface
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = {
                                scope.launch(Dispatchers.IO) {
                                    val resetCommands = allFeatures
                                        .filter { enabledToggles[it.title] == true }
                                        .mapNotNull { it.resetCommand }
                                        .distinct()

                                    withContext(Dispatchers.Main) {
                                        if (resetCommands.isNotEmpty()) {
                                            Toast.makeText(localContext, "🔄 Resetting active features...", Toast.LENGTH_SHORT).show()
                                            resetCommands.forEach { command ->
                                                ShellUtils.runShellCommand(command)
                                            }
                                        }
                                        val serviceIntent = Intent(context, FeatureControlService::class.java).apply {
                                            action = FeatureControlService.ACTION_STOP_LOOP
                                        }
                                        context.stopService(serviceIntent)

                                        enabledToggles.keys.forEach { featureTitle ->
                                            if (enabledToggles[featureTitle] == true) {
                                                DataStoreManager.setFeatureEnabled(context, featureTitle, false)
                                            }
                                        }

                                        if (isOverlayActive.value) {
                                            val overlayIntent = Intent(context, OverlayControlService::class.java).apply {
                                                action = OverlayControlService.ACTION_HIDE_OVERLAY
                                            }
                                            context.stopService(overlayIntent)
                                            isOverlayActive.value = false
                                        }

                                        Toast.makeText(localContext, "🛑 All active features disabled.", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                        ) { Text("STOP") }

                        Spacer(modifier = Modifier.weight(1f))

                        Button(
                            onClick = {
                                val newState = !isOverlayActive.value
                                isOverlayActive.value = newState

                                val serviceIntent = Intent(context, OverlayControlService::class.java).apply {
                                    if (newState) {
                                        action = OverlayControlService.ACTION_SHOW_OVERLAY
                                        putExtra(OverlayControlService.EXTRA_TOGGLE_STATE_MAP, HashMap(enabledToggles))
                                        putStringArrayListExtra(OverlayControlService.EXTRA_OVERLAY_FEATURE_TITLES, ArrayList(selectedOverlayFeatures.toList()))
                                    } else {
                                        action = OverlayControlService.ACTION_HIDE_OVERLAY
                                    }
                                }
                                context.startService(serviceIntent)
                                Toast.makeText(localContext, if (newState) "Overlay shown" else "Overlay hidden", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            )
                        ) {
                            // Revert to standard Material Icons Visibility icons
                            Icon(
                                imageVector = if (isOverlayActive.value) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = if (isOverlayActive.value) "Hide Overlay" else "Show Overlay",
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }
        ) { padding ->
            // Content based on selected drawer item
            Column(modifier = Modifier.padding(padding)) {
                when (selectedItem.value) {
                    DrawerScreen.Home -> DashboardScreen(
                        context = context,
                        enabledToggles = enabledToggles,
                        onToggleChanged = { feature, isEnabled ->
                            scope.launch {
                                DataStoreManager.setFeatureEnabled(context, feature.title, isEnabled)
                            }
                        },
                        // FIX: Pass allFeatures to DashboardScreen
                        allFeatures = allFeatures,
                        activeFeatures = activeFeatures,
                        onNavigateToFeatureCategory = { category ->
                            scope.launch { drawerState.close() }
                            selectedItem.value = DrawerScreen.FeatureLibrary
                            val categories = FeatureRepository.getCategorizedFeatures(allFeatures).keys.toList()
                            featureLibraryTabIndex = categories.indexOf(category).coerceAtLeast(0)
                        }
                    )
                    DrawerScreen.FeatureLibrary -> {
                        val categorizedFeaturesVal = remember(allFeatures) {
                            FeatureRepository.getCategorizedFeatures(allFeatures)
                        }
                        val categoriesVal = remember(categorizedFeaturesVal) { categorizedFeaturesVal.keys.toList() }
                        var selectedTabIndexVal by remember(featureLibraryTabIndex) { mutableIntStateOf(featureLibraryTabIndex) }

                        Column(modifier = Modifier.fillMaxSize()) {
                            TabRow(
                                selectedTabIndex = selectedTabIndexVal,
                                modifier = Modifier.fillMaxWidth().height(60.dp)
                            ) {
                                categoriesVal.forEachIndexed { index, category ->
                                    Tab(
                                        selected = index == selectedTabIndexVal,
                                        onClick = { selectedTabIndexVal = index },
                                        text = { Text(category, fontSize = 16.sp) }
                                    )
                                }
                            }
                            val currentSelectedCategory = categoriesVal.getOrNull(selectedTabIndexVal)
                            currentSelectedCategory?.let { category ->
                                FeatureTabScreen(
                                    context = context,
                                    features = categorizedFeaturesVal[category].orEmpty(),
                                    enabledToggles = enabledToggles,
                                    onToggleChanged = { feature, enabled ->
                                        scope.launch {
                                            DataStoreManager.setFeatureEnabled(context, feature.title, enabled)
                                        }
                                    }
                                )
                            }
                        }
                    }
                    DrawerScreen.Settings -> {
                        SettingsScreen(
                            features = allFeatures // Pass all features
                        )
                    }
                    DrawerScreen.OverlaySelection -> {
                        OverlaySelectionScreen(
                            features = allFeatures, // Pass all features
                            onBackClicked = {
                                selectedItem.value = DrawerScreen.Home
                            }
                        )
                    }
                    // Content for Advanced Settings
                    DrawerScreen.AdvancedSettings -> {
                        AdvancedSettingsScreen(
                            context = context,
                            allFeatures = allFeatures, // Pass all features for potential display/interaction
                            onBackClicked = { selectedItem.value = DrawerScreen.Home }
                        )
                    }
                    DrawerScreen.About -> { /* About content (if it becomes a screen) */ }
                }
            }
        }
    }
}

@Composable
fun DrawerHeader() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(8.dp))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = Icons.Default.PanoramaPhotosphere,
            contentDescription = "App Icon",
            tint = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.size(48.dp)
        )
        Text(
            text = "iQOO TOOLS",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
        Text(
            text = "Control Your Device",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
        )
    }
}