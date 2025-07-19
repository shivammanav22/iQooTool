package com.perfmode.iqoo.model

import android.os.Parcelable
import androidx.compose.ui.graphics.vector.ImageVector
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.RawValue // Keep RawValue for Parcelize
import java.util.UUID
// Removed Gson annotations imports (@Expose, @SerializedName, @Transient)

@Parcelize
data class Feature(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val command: String,
    val category: String,
    // FIX: Reverted 'icon' back to ImageVector, but TypeAdapter will handle Gson
    // @RawValue is for Parcelize, not Gson
    val icon: @RawValue ImageVector,
    val requiresLoop: Boolean = false,
    val canLoopAsSpecialCase: Boolean = false,
    val resetCommand: String? = null,
    val description: String,
    val isCustom: Boolean = false
) : Parcelable {
    // Removed the 'by lazy' icon property, as the icon is now directly in the constructor.
    // The TypeAdapter will handle its serialization/deserialization.
}