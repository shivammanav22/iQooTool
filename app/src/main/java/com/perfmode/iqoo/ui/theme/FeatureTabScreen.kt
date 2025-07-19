package com.perfmode.iqoo.ui.theme

import android.content.Context // Import Context
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import com.perfmode.iqoo.model.Feature

@Composable
fun FeatureTabScreen(
    context: Context, // Add context parameter
    features: List<Feature>,
    enabledToggles: Map<String, Boolean>,
    onToggleChanged: (Feature, Boolean) -> Unit
) {
    LazyColumn {
        items(features) { feature ->
            FeatureCard(
                context = context, // Pass context here
                feature = feature,
                isEnabled = enabledToggles[feature.title] == true,
                onToggleChanged = { onToggleChanged(feature, it) }
            )
        }
    }
}
