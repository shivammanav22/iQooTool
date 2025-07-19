package com.perfmode.iqoo.ui.theme

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.perfmode.iqoo.model.Feature
import com.perfmode.iqoo.util.DataStoreManager
import com.perfmode.iqoo.util.FeatureRepository
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import android.util.Log
import androidx.compose.foundation.clickable


/**
 * A full-page screen for users to select which features appear in the floating overlay.
 *
 * @param features The list of all available Feature objects.
 * @param onBackClicked Callback to be invoked when the back button is clicked (e.g., from TopAppBar).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OverlaySelectionScreen(
    modifier: Modifier = Modifier,
    features: List<Feature>, // All features to choose from
    onBackClicked: () -> Unit // For TopAppBar back navigation
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()

    val selectedOverlayFeatures by DataStoreManager.getSelectedOverlayFeaturesFlow(context).collectAsState(initial = emptySet())

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Overlay Features", color = MaterialTheme.colorScheme.onPrimary) },
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
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Icon(
                    imageVector = Icons.Default.Checklist,
                    contentDescription = "Overlay Selection Icon",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(64.dp)
                )
                Text(
                    text = "Customize Overlay Toggles",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 8.dp, bottom = 16.dp)
                )
                Text(
                    text = "Choose which features you want to see as quick toggles in the floating overlay. " +
                            "Only selected features will appear.",
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 4.dp,)
                )
                Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), thickness = 1.dp)
                Spacer(modifier = Modifier.height(16.dp))
            }

            items(features) { feature ->
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    tonalElevation = 2.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                scope.launch {
                                    val newSet = selectedOverlayFeatures.toMutableSet()
                                    if (selectedOverlayFeatures.contains(feature.title)) {
                                        newSet.remove(feature.title)
                                    } else {
                                        newSet.add(feature.title)
                                    }
                                    DataStoreManager.saveSelectedOverlayFeatures(context, newSet)
                                    Log.d("OverlaySelectionScreen", "Checkbox changed for ${feature.title}. New selections: $newSet")
                                }
                            }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = feature.title,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f)
                        )
                        Checkbox(
                            checked = selectedOverlayFeatures.contains(feature.title),
                            onCheckedChange = { isChecked ->
                                scope.launch {
                                    val newSet = selectedOverlayFeatures.toMutableSet()
                                    if (isChecked) {
                                        newSet.add(feature.title)
                                    } else {
                                        newSet.remove(feature.title)
                                    }
                                    DataStoreManager.saveSelectedOverlayFeatures(context, newSet)
                                    Log.d("OverlaySelectionScreen", "Checkbox changed for ${feature.title}. New selections: $newSet")
                                }
                            },
                            colors = CheckboxDefaults.colors(
                                checkedColor = MaterialTheme.colorScheme.primary,
                                uncheckedColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        )
                    }
                }
            }
            item {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}