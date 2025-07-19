package com.perfmode.iqoo.ui // Note: This is 'ui', not 'ui.theme'

import android.content.Context // Import Context
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.perfmode.iqoo.model.Feature
import com.perfmode.iqoo.ui.theme.FeatureTabScreen // Assuming FeatureTabScreen is used here

@Composable
fun FeatureScreen(
    context: Context, // NEW: Add context parameter here
    features: List<Feature>,
    enabledToggles: Map<String, Boolean>,
    onToggleChanged: (Feature, Boolean) -> Unit
) {
    Column(modifier = Modifier.padding(16.dp)) { // Example padding, adjust as needed
        FeatureTabScreen(
            context = context, // NEW: Pass context down to FeatureTabScreen
            features = features,
            enabledToggles = enabledToggles,
            onToggleChanged = onToggleChanged
        )
    }
}
