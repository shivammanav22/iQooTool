package com.perfmode.iqoo.ui.theme // Or com.perfmode.iqoo.ui.overlay

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.perfmode.iqoo.model.Feature
import com.perfmode.iqoo.OverlaySection // Import the enum
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.platform.LocalContext // NEW: Import LocalContext
import java.util.UUID // NEW: Import UUID for preview data


// Define some consistent colors for the overlay UI
val OverlayDarkBackground = Color(0xAA212121) // Semi-transparent dark grey
val OverlayPrimaryText = Color.White
val OverlaySecondaryText = Color(0xFFCCCCCC) // Light grey
val OverlayAccentColor = Color(0xFF4CAF50) // Green accent for active/success
val OverlayInputBackground = Color(0x33FFFFFF) // Light transparent white

@Composable
fun OverlayScreen(
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    currentSection: OverlaySection,
    onSectionSelected: (OverlaySection) -> Unit,
    cpuUsage: String,
    memUsage: String,
    terminalOutput: String,
    onTerminalCommand: (String) -> Unit,
    onClearTerminal: () -> Unit,
    featuresList: List<Feature>, // Features selected for overlay (passed from service)
    featureStates: Map<String, Boolean>, // Current toggle states of all features (passed from service)
    onFeatureToggle: (Feature, Boolean) -> Unit,
    onTerminalFocusChanged: (Boolean) -> Unit // Callback for terminal focus
) {
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current

    Column(
        modifier = Modifier
            .wrapContentSize()
            .clip(RoundedCornerShape(12.dp))
            .background(OverlayDarkBackground)
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Row 1: Collapse Button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onToggleExpand,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    imageVector = if (isExpanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                    contentDescription = if (isExpanded) "Collapse Overlay" else "Expand Overlay",
                    tint = OverlayPrimaryText
                )
            }
        }

        // Row 2: Navigation Buttons (Visible only when expanded)
        if (isExpanded) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OverlayNavButton(
                    icon = Icons.Default.Apps,
                    text = "Features",
                    isSelected = currentSection == OverlaySection.FEATURES,
                    onClick = { onSectionSelected(OverlaySection.FEATURES) }
                )
                OverlayNavButton(
                    icon = Icons.Default.Memory,
                    text = "Stats",
                    isSelected = currentSection == OverlaySection.STATS,
                    onClick = { onSectionSelected(OverlaySection.STATS) }
                )
                OverlayNavButton(
                    icon = Icons.Default.Terminal,
                    text = "Terminal",
                    isSelected = currentSection == OverlaySection.TERMINAL,
                    onClick = { onSectionSelected(OverlaySection.TERMINAL) }
                )
            }
        }

        // Row 3: Content Frame (Visible only when expanded)
        if (isExpanded) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 300.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0x22FFFFFF))
                    .padding(8.dp)
            ) {
                when (currentSection) {
                    OverlaySection.FEATURES -> {
                        FeaturesContent(
                            features = featuresList,
                            featureStates = featureStates,
                            onFeatureToggle = onFeatureToggle
                        )
                    }
                    OverlaySection.STATS -> {
                        StatsContent(cpuUsage = cpuUsage, memUsage = memUsage)
                    }
                    OverlaySection.TERMINAL -> {
                        TerminalContent(
                            output = terminalOutput,
                            onCommand = onTerminalCommand,
                            onClear = onClearTerminal,
                            onFocusChanged = onTerminalFocusChanged,
                            focusRequester = focusRequester
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun OverlayNavButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .background(if (isSelected) OverlayAccentColor.copy(alpha = 0.2f) else Color.Transparent)
            .padding(vertical = 4.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = text,
            tint = if (isSelected) OverlayAccentColor else OverlaySecondaryText,
            modifier = Modifier.size(24.dp)
        )
        Text(
            text = text,
            fontSize = 10.sp,
            color = if (isSelected) OverlayAccentColor else OverlaySecondaryText,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun StatsContent(cpuUsage: String, memUsage: String) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = cpuUsage,
            color = OverlayPrimaryText,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        Text(
            text = memUsage,
            color = OverlayPrimaryText,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun TerminalContent(
    output: String,
    onCommand: (String) -> Unit,
    onClear: () -> Unit,
    onFocusChanged: (Boolean) -> Unit,
    focusRequester: FocusRequester
) {
    var commandInput by remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current

    Column(modifier = Modifier.fillMaxWidth()) {
        BasicTextField(
            value = commandInput,
            onValueChange = { commandInput = it },
            textStyle = TextStyle(
                color = OverlayPrimaryText,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace
            ),
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(4.dp))
                .background(OverlayInputBackground)
                .padding(8.dp)
                .focusRequester(focusRequester)
                .onFocusChanged { focusState ->
                    onFocusChanged(focusState.isFocused)
                },
            singleLine = true,
            cursorBrush = SolidColor(OverlayAccentColor),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Ascii,
                imeAction = ImeAction.Send
            ),
            keyboardActions = KeyboardActions(
                onSend = {
                    if (commandInput.isNotBlank()) {
                        onCommand(commandInput)
                        commandInput = ""
                    }
                    focusManager.clearFocus()
                }
            ),
            decorationBox = { innerTextField ->
                if (commandInput.isEmpty()) {
                    Text("Enter command...", color = OverlaySecondaryText.copy(alpha = 0.7f), fontSize = 12.sp)
                }
                innerTextField()
            }
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 6.dp),
            horizontalArrangement = Arrangement.End
        ) {
            Button(
                onClick = {
                    if (commandInput.isNotBlank()) {
                        onCommand(commandInput)
                        commandInput = ""
                    }
                    focusManager.clearFocus()
                },
                colors = ButtonDefaults.buttonColors(containerColor = OverlayAccentColor),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                modifier = Modifier.height(32.dp)
            ) {
                Text("Execute", fontSize = 12.sp, color = Color.White)
            }
            Spacer(Modifier.width(8.dp))
            Button(
                onClick = onClear,
                colors = ButtonDefaults.buttonColors(containerColor = OverlayAccentColor),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                modifier = Modifier.height(32.dp)
            ) {
                Text("Clear", fontSize = 12.sp, color = Color.White)
            }
        }

        Spacer(Modifier.height(8.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(OverlayInputBackground)
                .verticalScroll(rememberScrollState())
                .padding(8.dp)
        ) {
            Text(
                text = output,
                color = OverlayAccentColor,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

@Composable
fun FeaturesContent(
    features: List<Feature>,
    featureStates: Map<String, Boolean>,
    onFeatureToggle: (Feature, Boolean) -> Unit
) {
    if (features.isEmpty()) {
        Text(
            text = "No features selected for overlay. Go to 'Overlay Selection' in app settings.",
            color = OverlaySecondaryText,
            fontSize = 12.sp,
            modifier = Modifier.fillMaxWidth().padding(8.dp)
        )
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(features) { feature ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color.Transparent)
                        .padding(vertical = 4.dp, horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = feature.title.replace("\n", " ", ignoreCase = true),
                        color = if (featureStates[feature.title] == true) OverlayAccentColor else OverlayPrimaryText,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.weight(1f)
                    )
                    Switch(
                        checked = featureStates[feature.title] == true,
                        onCheckedChange = { newCheckedState ->
                            onFeatureToggle(feature, newCheckedState)
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = OverlayAccentColor,
                            checkedTrackColor = OverlayAccentColor.copy(alpha = 0.5f),
                            uncheckedThumbColor = OverlaySecondaryText,
                            uncheckedTrackColor = OverlaySecondaryText.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier.size(width = 36.dp, height = 20.dp)
                    )
                }
            }
        }
    }
}


@Preview(showBackground = true, backgroundColor = 0xFF333333)
@Composable
fun OverlayScreenPreviewExpanded() {
    MaterialTheme {
        OverlayScreen(
            isExpanded = true,
            onToggleExpand = {},
            currentSection = OverlaySection.FEATURES,
            onSectionSelected = {},
            cpuUsage = "CPU: 25%",
            memUsage = "RAM: 2GB / 4GB",
            terminalOutput = "Welcome to terminal!\n$ ls -l\n-rw-r--r-- 1 user user 1024 Jan 1 2023 file.txt",
            onTerminalCommand = {},
            onClearTerminal = {},
            featuresList = listOf(
                // FIX: Pass ImageVector directly for 'icon' parameter
                Feature(id = UUID.randomUUID().toString(), title = "Feature A", command = "cmd1", category = "cat1", icon = Icons.Default.Memory, description = "Desc A", requiresLoop = false, canLoopAsSpecialCase = false, resetCommand = null, isCustom = false),
                Feature(id = UUID.randomUUID().toString(), title = "Feature B\nLong Title", command = "cmd2", category = "cat1", icon = Icons.Default.Terminal, description = "Desc B", requiresLoop = false, canLoopAsSpecialCase = true, resetCommand = "resetCmd2", isCustom = false),
                Feature(id = UUID.randomUUID().toString(), title = "Feature C", command = "cmd3", category = "cat2", icon = Icons.Default.Apps, description = "Desc C", requiresLoop = false, canLoopAsSpecialCase = false, resetCommand = null, isCustom = false)
            ),
            featureStates = mapOf("Feature A" to true, "Feature C" to false),
            onFeatureToggle = { _, _ -> },
            onTerminalFocusChanged = {}
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF333333)
@Composable
fun OverlayScreenPreviewCollapsed() {
    MaterialTheme {
        OverlayScreen(
            isExpanded = false,
            onToggleExpand = {},
            currentSection = OverlaySection.STATS, // Section doesn't matter when collapsed
            onSectionSelected = {},
            cpuUsage = "CPU: 10%",
            memUsage = "RAM: 1.5GB / 4GB",
            terminalOutput = "",
            onTerminalCommand = {},
            onClearTerminal = {},
            featuresList = emptyList(),
            featureStates = emptyMap(),
            onFeatureToggle = { _, _ -> },
            onTerminalFocusChanged = {}
        )
    }
}