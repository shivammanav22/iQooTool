package com.perfmode.iqoo.ui.theme // Or com.perfmode.iqoo.ui.settings

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.perfmode.iqoo.model.Feature
import com.perfmode.iqoo.util.FeatureRepository
import kotlinx.coroutines.launch
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Title
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Loop
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Image // NEW: Import Image icon for placeholder
import androidx.compose.ui.graphics.vector.ImageVector // NEW: Import ImageVector
import androidx.compose.material.icons.filled.Extension // NEW: Import Extension for default icon


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdvancedSettingsScreen(
    context: Context,
    allFeatures: List<Feature>, // All features, including custom ones
    onBackClicked: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val localContext = LocalContext.current

    // State for new feature input fields
    var newFeatureTitle by remember { mutableStateOf("") }
    var newFeatureCommand by remember { mutableStateOf("") }
    var newFeatureResetCommand by remember { mutableStateOf("") }
    var newFeatureDescription by remember { mutableStateOf("") }
    var newFeatureCategory by remember { mutableStateOf("Custom") }
    var newFeatureIcon by remember { mutableStateOf(Icons.Default.Extension) } // FIX: State for ImageVector, default to Extension
    var newFeatureRequiresLoop by remember { mutableStateOf(false) }
    var newFeatureCanLoopAsSpecialCase by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Advanced Settings", color = MaterialTheme.colorScheme.onPrimary) },
                navigationIcon = {
                    IconButton(onClick = onBackClicked) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primary)
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    text = "Add Custom Feature",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                OutlinedTextField(
                    value = newFeatureTitle,
                    onValueChange = { newFeatureTitle = it },
                    label = { Text("Feature Title") },
                    leadingIcon = { Icon(Icons.Default.Title, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                )
                OutlinedTextField(
                    value = newFeatureCommand,
                    onValueChange = { newFeatureCommand = it },
                    label = { Text("Shell Command (e.g., 'echo 1 > /path')") },
                    leadingIcon = { Icon(Icons.Default.Code, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii)
                )
                OutlinedTextField(
                    value = newFeatureResetCommand,
                    onValueChange = { newFeatureResetCommand = it },
                    label = { Text("Reset Command (Optional)") },
                    leadingIcon = { Icon(Icons.Default.RestartAlt, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii)
                )
                OutlinedTextField(
                    value = newFeatureDescription,
                    onValueChange = { newFeatureDescription = it },
                    label = { Text("Description") },
                    leadingIcon = { Icon(Icons.Default.Description, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                )
                OutlinedTextField(
                    value = newFeatureCategory,
                    onValueChange = { newFeatureCategory = it },
                    label = { Text("Category (e.g., 'Custom', 'CPU')") },
                    leadingIcon = { Icon(Icons.Default.Category, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                )
                // FIX: Icon selection is now using ImageVector directly
                // For simplicity, we'll use a dropdown later or a more robust icon selection UI.
                // For now, it will always use Icons.Default.Extension.
                // If you want user to pick, you'll need a way to map strings to ImageVectors
                // or provide a selection mechanism.
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(imageVector = newFeatureIcon, contentDescription = null, modifier = Modifier.size(24.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Selected Icon: ${newFeatureIcon.name}") // Display the name of the selected icon
                    // If you want to let user pick, you'd add a clickable element here
                }


                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = newFeatureRequiresLoop,
                            onCheckedChange = { newFeatureRequiresLoop = it }
                        )
                        Text("Requires Loop", fontSize = 14.sp)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = newFeatureCanLoopAsSpecialCase,
                            onCheckedChange = { newFeatureCanLoopAsSpecialCase = it }
                        )
                        Text("Can Loop (Special Case)", fontSize = 14.sp)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        if (newFeatureTitle.isNotBlank() && newFeatureCommand.isNotBlank()) {
                            val newFeature = Feature(
                                title = newFeatureTitle.trim(),
                                command = newFeatureCommand.trim(),
                                category = newFeatureCategory.trim().ifBlank { "Custom" },
                                icon = newFeatureIcon, // FIX: Pass ImageVector directly
                                requiresLoop = newFeatureRequiresLoop,
                                canLoopAsSpecialCase = newFeatureCanLoopAsSpecialCase,
                                resetCommand = newFeatureResetCommand.trim().ifBlank { null },
                                description = newFeatureDescription.trim().ifBlank { "User-added custom feature." },
                                isCustom = true
                            )
                            scope.launch {
                                FeatureRepository.addCustomFeature(context, newFeature)
                                // Clear input fields after adding
                                newFeatureTitle = ""
                                newFeatureCommand = ""
                                newFeatureResetCommand = ""
                                newFeatureDescription = ""
                                newFeatureCategory = "Custom"
                                newFeatureIcon = Icons.Default.Extension // Reset icon to default
                                newFeatureRequiresLoop = false
                                newFeatureCanLoopAsSpecialCase = false
                                Toast.makeText(localContext, "Feature '${newFeature.title}' added!", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            Toast.makeText(localContext, "Title and Command cannot be empty!", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Feature")
                    Spacer(Modifier.width(8.dp))
                    Text("Add New Feature")
                }

                Divider(modifier = Modifier.padding(vertical = 24.dp), thickness = 1.dp, color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))

                Text(
                    text = "Manage Custom Features",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }

            // List custom features for management
            items(allFeatures.filter { it.isCustom }) { customFeature ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = customFeature.title,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = {
                                scope.launch {
                                    FeatureRepository.deleteCustomFeature(context, customFeature.id)
                                    Toast.makeText(localContext, "Feature '${customFeature.title}' deleted!", Toast.LENGTH_SHORT).show()
                                }
                            }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                        Spacer(Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = customFeature.icon, // Display the ImageVector directly
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "Icon: ${customFeature.icon.name}", // Display the name of the ImageVector
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                            )
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "Command: ${customFeature.command}",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                        )
                        customFeature.resetCommand?.let {
                            Text(
                                text = "Reset: $it",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                            )
                        }
                        Text(
                            text = "Description: ${customFeature.description}",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                        )
                        if (customFeature.requiresLoop || customFeature.canLoopAsSpecialCase) {
                            Text(
                                text = "Loopable: Yes",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            }
            item {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}