package com.perfmode.iqoo.ui // Corrected package declaration

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.DeveloperMode
import androidx.compose.material.icons.filled.Games // NEW: Icon for gaming features
import androidx.compose.material.icons.filled.Tune // NEW: Icon for tuning/optimization
import androidx.compose.material.icons.filled.TouchApp // NEW: Icon for touch features
import androidx.compose.material.icons.filled.DisplaySettings // NEW: Icon for display settings
import androidx.compose.material.icons.filled.CheckCircle // NEW: Icon for active features
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.perfmode.iqoo.ui.theme.IqooTweaksTheme
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import kotlinx.coroutines.delay
import kotlin.random.Random
import com.perfmode.iqoo.util.ShellUtils
import android.util.Log
import com.perfmode.iqoo.model.Feature // Import Feature model
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Size
import com.perfmode.iqoo.util.FeatureRepository // Import FeatureRepository
import android.content.Context // Import Context
import androidx.compose.material.icons.filled.Rocket
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch // Import launch for coroutine scope
import java.util.UUID // Import UUID for preview data Feature IDs


@Composable
fun DashboardScreen(
    modifier: Modifier = Modifier,
    activeFeatures: List<Feature> = emptyList(),
    onNavigateToFeatureCategory: (String) -> Unit = {},
    // Parameters for quick toggles
    context: Context,
    enabledToggles: Map<String, Boolean>,
    onToggleChanged: (Feature, Boolean) -> Unit, // Callback to update DataStore
    allFeatures: List<Feature> // FIX: allFeatures is now a required parameter
) {
    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope() // Coroutine scope for onClick actions

    // Re-initialized states for real-time CPU metrics
    var cpuFrequency by remember { mutableStateOf("N/A MHz") }
    var cpuUsage by remember { mutableStateOf("N/A %") }
    var cpuUsageFloat by remember { mutableStateOf(0f) }

    // Re-initialized states for real-time RAM metrics
    var totalRam by remember { mutableStateOf("N/A GB") }
    var usedRam by remember { mutableStateOf("N/A GB") }
    var ramPercentage by remember { mutableStateOf("N/A %") }
    var ramPercentageFloat by remember { mutableStateOf(0f) }

    // States for GPU and Fan metrics (still mock)
    var gpuFrequency by remember { mutableStateOf("N/A MHz") }
    var gpuUsage by remember { mutableStateOf("N/A %") }
    var gpuUsageFloat by remember { mutableStateOf(0f) }
    var gpuMemoryFrequency by remember { mutableStateOf("N/A MHz") }
    var gpuTemperature by remember { mutableStateOf("N/A °C") }
    var gpuVoltage by remember { mutableStateOf("N/A mV") }

    var cpuFan by remember { mutableStateOf("N/A RPM") }
    var gpuFan by remember { mutableStateOf("N/A RPM") }
    var systemFan by remember { mutableStateOf("0 RPM") }


    // Re-added: LaunchedEffect for real-time updates
    LaunchedEffect(Unit) {
        var previousCpuTotal: Long = 0
        var previousCpuIdle: Long = 0

        while (true) {
            delay(1000L) // Update every 1 second

            // --- CPU Usage (Real-time attempt) ---
            val cpuStatOutput = ShellUtils.executeCommandWithOutput("cat /proc/stat")
            if (cpuStatOutput != null) {
                val lines = cpuStatOutput.split("\n")
                val cpuLine = lines.firstOrNull { it.startsWith("cpu ") }
                if (cpuLine != null) {
                    val parts = cpuLine.split(Regex("\\s+")).filter { it.isNotEmpty() }
                    if (parts.size >= 8) { // user, nice, system, idle, iowait, irq, softirq, steal
                        try {
                            val user = parts[1].toLong()
                            val nice = parts[2].toLong()
                            val system = parts[3].toLong()
                            val idle = parts[4].toLong()
                            val iowait = parts[5].toLong()
                            val irq = parts[6].toLong()
                            val softirq = parts[7].toLong()
                            val steal = parts[8].toLong()

                            val currentCpuIdle = idle + iowait
                            val currentCpuTotal = user + nice + system + idle + iowait + irq + softirq + steal

                            if (previousCpuTotal != 0L) {
                                val deltaTotal = currentCpuTotal - previousCpuTotal
                                val deltaIdle = currentCpuIdle - previousCpuIdle

                                if (deltaTotal > 0) {
                                    val usage = ((deltaTotal - deltaIdle).toFloat() / deltaTotal) * 100
                                    cpuUsage = "${String.format("%.1f", usage)} %"
                                    cpuUsageFloat = usage / 100f
                                }
                            }
                            previousCpuTotal = currentCpuTotal
                            previousCpuIdle = currentCpuIdle

                            val cpuFreqOutput = ShellUtils.executeCommandWithOutput("cat /sys/devices/system/cpu/cpu0/cpufreq/scaling_cur_freq")
                            cpuFrequency = if (cpuFreqOutput != null) {
                                "${cpuFreqOutput.trim().toLong() / 1000} MHz"
                            } else {
                                "N/A MHz"
                            }

                        } catch (e: Exception) {
                            Log.e("DashboardScreen", "Error parsing /proc/stat CPU data: ${e.message}")
                            cpuUsage = "Error %"
                            cpuUsageFloat = 0f
                            cpuFrequency = "Error MHz"
                        }
                    }
                }
            } else {
                cpuUsage = "N/A %"
                cpuUsageFloat = 0f
                cpuFrequency = "N/A MHz"
            }

            // --- RAM Usage (Real-time attempt) ---
            val memInfoOutput = ShellUtils.executeCommandWithOutput("cat /proc/meminfo")
            if (memInfoOutput != null) {
                try {
                    val memTotalLine = memInfoOutput.split("\n").firstOrNull { it.startsWith("MemTotal:") }
                    val memAvailableLine = memInfoOutput.split("\n").firstOrNull { it.startsWith("MemAvailable:") }

                    if (memTotalLine != null && memAvailableLine != null) {
                        val totalKb = memTotalLine.split(Regex("\\s+"))[1].toLong()
                        val availableKb = memAvailableLine.split(Regex("\\s+"))[1].toLong()

                        val usedKb = totalKb - availableKb
                        val totalGb = totalKb / (1024 * 1024).toFloat()
                        val usedGbValue = usedKb / (1024 * 1024).toFloat()
                        val percentage = (usedKb.toFloat() / totalKb) * 100

                        totalRam = "${String.format("%.1f", totalGb)} GB"
                        usedRam = "${String.format("%.1f", usedGbValue)} GB"
                        ramPercentage = "${String.format("%.1f", percentage)} %"
                        ramPercentageFloat = percentage / 100f
                    }
                } catch (e: Exception) {
                    Log.e("DashboardScreen", "Error parsing /proc/meminfo RAM data: ${e.message}")
                    totalRam = "Error GB"
                    usedRam = "Error GB"
                    ramPercentage = "Error %"
                    ramPercentageFloat = 0f
                }
            } else {
                totalRam = "N/A GB"
                usedRam = "N/A GB"
                ramPercentage = "N/A %"
                ramPercentageFloat = 0f
            }


            // --- GPU, Fan, Voltage, Temperature (Still Mocked) ---
            gpuFrequency = "${Random.nextInt(100, 1000)} MHz"
            gpuUsage = "${Random.nextInt(0, 100)} %"
            gpuUsageFloat = gpuUsage.filter { it.isDigit() }.toFloatOrNull()?.div(100f) ?: 0f
            gpuMemoryFrequency = "${Random.nextInt(300, 600)} MHz"
            gpuTemperature = "${Random.nextInt(40, 70)} °C"
            gpuVoltage = "${Random.nextInt(500, 800)} mV"

            cpuFan = "${Random.nextInt(3000, 6000)} RPM"
            gpuFan = "${Random.nextInt(3000, 6000)} RPM"
            systemFan = "${Random.nextInt(0, 1000)} RPM"
        }
    }


    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // System Usage Section
        Text(
            text = "System Usage",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // CPU Card
            SystemMetricCard(
                icon = Icons.Default.DeveloperMode,
                title = "CPU",
                metrics = mapOf(
                    "Frequency" to cpuFrequency,
                    "Usage" to cpuUsage,
                ),
                usageProgress = cpuUsageFloat,
                modifier = Modifier.weight(1f)
            )

            // GPU Card
            SystemMetricCard(
                icon = Icons.Default.Speed,
                title = "GPU",
                metrics = mapOf(
                    "Frequency" to gpuFrequency,
                    "Usage" to gpuUsage,
                    "Memory Frequency" to gpuMemoryFrequency,
                    "Temperature" to gpuTemperature,
                    "Voltage" to gpuVoltage
                ),
                usageProgress = gpuUsageFloat,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Active Features & Memory Section
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ActiveFeaturesCard(
                activeFeatures = activeFeatures,
                modifier = Modifier.weight(1f)
            )

            SystemMetricCard(
                icon = Icons.Default.Memory,
                title = "Memory",
                metrics = mapOf(
                    "Total RAM" to totalRam,
                    "Used RAM" to usedRam,
                    "Usage %" to ramPercentage
                ),
                usageProgress = ramPercentageFloat,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Operating Mode Section (now with direct feature activation)
        Text(
            text = "Operating Mode",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Get specific Feature objects for the toggles using 'allFeatures' list directly
            val monsterModePlusFeature = allFeatures.firstOrNull { it.title == "☛ MONSTER MODE PLUS ☚" }
            val monsterModeFeature = allFeatures.firstOrNull { it.title == "Monster Mode" }
            val normalModeFeature = allFeatures.firstOrNull { it.title == "Normal Mode" }
            val thermalOverrideFeature = allFeatures.firstOrNull { it.title == "Thermal Override" }

            // Helper function to toggle a feature ON and others OFF
            val toggleOperatingMode = { selectedFeature: Feature ->
                scope.launch {
                    val allOperatingModes = listOfNotNull(
                        monsterModePlusFeature,
                        monsterModeFeature,
                        normalModeFeature,
                        thermalOverrideFeature
                    )
                    // Turn off all other operating modes
                    allOperatingModes.forEach { modeFeature ->
                        if (modeFeature.title != selectedFeature.title && enabledToggles[modeFeature.title] == true) {
                            onToggleChanged(modeFeature, false)
                        }
                    }
                    // Turn on the selected operating mode
                    onToggleChanged(selectedFeature, true)
                }
            }

            // Windows® -> Normal Mode
            OperatingModeButton(
                label = "CPU",
                icon = Icons.Default.Rocket,
                onClick = { normalModeFeature?.let { toggleOperatingMode(it) } }
            )
            // Silent -> (Assuming a silent mode feature, or just ensure Normal Mode is active and others are off)
            OperatingModeButton(
                label = "RAM",
                icon = Icons.Default.Memory,
                onClick = { normalModeFeature?.let { toggleOperatingMode(it) } } // For simplicity, Silent toggles Normal Mode
            )
            // Performance -> Monster Mode
            OperatingModeButton(
                label = "Performance",
                icon = Icons.Default.RocketLaunch,
                onClick = { monsterModeFeature?.let { toggleOperatingMode(it) } }
            )
            // Turbo -> Monster Mode Plus
            OperatingModeButton(
                label = "Turbo",
                icon = Icons.Default.FlashOn,
                onClick = { monsterModePlusFeature?.let { toggleOperatingMode(it) } }
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Main Features Section (original position, now with icons for navigation)
        Text(
            text = "Main Features",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Reverted to icons for navigation, not toggles
            OperatingModeButton(
                label = "Gaming Tweaks",
                icon = Icons.Default.Games,
                onClick = { onNavigateToFeatureCategory("Gaming") }
            )
            OperatingModeButton(
                label = "Display Opt.",
                icon = Icons.Default.DisplaySettings,
                onClick = { onNavigateToFeatureCategory("Display") }
            )
            OperatingModeButton(
                label = "Touch Boost",
                icon = Icons.Default.TouchApp,
                onClick = { onNavigateToFeatureCategory("Touch") }
            )
            OperatingModeButton(
                label = "System Tune",
                icon = Icons.Default.Tune,
                onClick = { onNavigateToFeatureCategory("System") }
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Fan Status Section (Moved to the end)
        Text(
            text = "Fan Status",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SystemMetricCard(
                icon = Icons.Default.Thermostat,
                title = "Fan",
                metrics = mapOf(
                    "CPU Fan" to cpuFan,
                    "GPU Fan" to gpuFan,
                    "System Fan" to systemFan
                ),
                usageProgress = null,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.weight(1f))
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun SystemMetricCard(
    icon: ImageVector,
    title: String,
    metrics: Map<String, String>,
    modifier: Modifier = Modifier,
    usageProgress: Float? = null
) {
    Card(
        modifier = modifier.height(IntrinsicSize.Max),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                metrics.forEach { (key, value) ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = key,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = value,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
            usageProgress?.let { progress ->
                Spacer(modifier = Modifier.height(8.dp))
                UsageProgressBar(progress = progress)
            }
        }
    }
}

@Composable
fun ActiveFeaturesCard(
    activeFeatures: List<Feature>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.height(IntrinsicSize.Max),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Active Features",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            if (activeFeatures.isEmpty()) {
                Text(
                    text = "No features currently active.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    activeFeatures.take(5).forEach { feature ->
                        Text(
                            text = "• ${feature.title}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    if (activeFeatures.size > 5) {
                        Text(
                            text = "...and ${activeFeatures.size - 5} more",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}


@Composable
fun UsageProgressBar(progress: Float) {
    val progressColor = MaterialTheme.colorScheme.primary
    val backgroundColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(8.dp)
            .clip(RoundedCornerShape(4.dp))
    ) {
        drawRect(
            color = backgroundColor,
            size = Size(width = size.width, height = size.height)
        )
        drawRect(
            color = progressColor,
            size = Size(width = size.width * progress.coerceIn(0f, 1f), height = size.height)
        )
    }
}

@Composable
fun OperatingModeButton(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit = {}
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.padding(8.dp)
    ) {
        IconButton(
            onClick = onClick,
            modifier = Modifier
                .size(64.dp)
                .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(12.dp))
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(36.dp)
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}

@Preview(showBackground = true)
@Composable
fun DashboardScreenPreview() {
    IqooTweaksTheme {
        DashboardScreen(
            activeFeatures = listOf(
                // FIX: Use 'icon' directly as it's ImageVector now
                Feature(id = "preview_monster", title = "Monster Mode", command = "cmd", category = "cat", icon = Icons.Default.FlashOn, requiresLoop = false, canLoopAsSpecialCase = false, resetCommand = null, description = "desc", isCustom = false),
                Feature(id = "preview_fps", title = "Target FPS 144", command = "cmd", category = "cat", icon = Icons.Default.Speed, requiresLoop = false, canLoopAsSpecialCase = false, resetCommand = null, description = "desc", isCustom = false),
                Feature(id = "preview_touch", title = "Touch Rate 333", command = "cmd", category = "cat", icon = Icons.Default.TouchApp, requiresLoop = false, canLoopAsSpecialCase = false, resetCommand = null, description = "desc", isCustom = false)
            ),
            onNavigateToFeatureCategory = { _ -> },
            context = LocalContext.current,
            enabledToggles = mapOf(
                "Monster Mode" to true,
                "Target FPS 144" to false,
                "Touch Rate 333" to true
            ),
            onToggleChanged = { _, _ -> },
            allFeatures = listOf( // Provide dummy allFeatures for preview
                Feature(id = "preview_monster_full", title = "Monster Mode", command = "cmd", category = "cat", icon = Icons.Default.FlashOn, requiresLoop = false, canLoopAsSpecialCase = false, resetCommand = null, description = "desc", isCustom = false),
                Feature(id = "preview_fps_full", title = "Target FPS 144", command = "cmd", category = "cat", icon = Icons.Default.Speed, requiresLoop = false, canLoopAsSpecialCase = false, resetCommand = null, description = "desc", isCustom = false),
                Feature(id = "preview_touch_full", title = "Touch Rate 333", command = "cmd", category = "cat", icon = Icons.Default.TouchApp, requiresLoop = false, canLoopAsSpecialCase = false, resetCommand = null, description = "desc", isCustom = false),
                Feature(id = "preview_normal_full", title = "Normal Mode", command = "cmd", category = "cat", icon = Icons.Default.Rocket, requiresLoop = false, canLoopAsSpecialCase = false, resetCommand = null, description = "desc", isCustom = false),
                Feature(id = "preview_monster_plus_full", title = "☛ MONSTER MODE PLUS ☚", command = "cmd", category = "cat", icon = Icons.Default.FlashOn, requiresLoop = false, canLoopAsSpecialCase = false, resetCommand = null, description = "desc", isCustom = false),
                Feature(id = "preview_thermal_full", title = "Thermal Override", command = "cmd", category = "cat", icon = Icons.Default.Thermostat, requiresLoop = false, canLoopAsSpecialCase = false, resetCommand = null, description = "desc", isCustom = false)
            )
        )
    }
}