package com.perfmode.iqoo.ui.theme

import androidx.compose.foundation.layout.*
import androidx.compose.material3.* // Use Material3 imports
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog

@Composable
fun AboutDialog(
    onDismissRequest: () -> Unit
) {
    Dialog(onDismissRequest = onDismissRequest) {
        Surface(
            shape = MaterialTheme.shapes.medium,
            tonalElevation = 6.dp,
            color = MaterialTheme.colorScheme.surface // Use Material3 color
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .widthIn(min = 280.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "About",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface // Use Material3 color
                )

                Spacer(modifier = Modifier.height(16.dp))

                val annotatedText = buildAnnotatedString {

                    pushStyle(SpanStyle(fontWeight = FontWeight.Bold, fontSize = 18.sp, color = MaterialTheme.colorScheme.primary)) // Use primary for bold text
                    append("iQOO Hidden Features Unlocker\n")
                    pop()


                    pushStyle(SpanStyle(color = MaterialTheme.colorScheme.tertiary, fontWeight = FontWeight.Medium)) // Use tertiary for distinct accent
                    append("Developed by Shivam aka Agent47\n")
                    pop()

                    pushStyle(SpanStyle(color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)) // Use primary for questions/headers
                    append("How does this app work?\n")
                    pop()

                    append("Uses Shizuku to hook into system and enable features restricted to users outside system apps.\n")

                    pushStyle(SpanStyle(color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.SemiBold)) // Use error for warnings
                    append("Note: ")
                    pop()
                    append("Use Game mode and TSR together if it's not working.\n")

                    pushStyle(SpanStyle(color = MaterialTheme.colorScheme.tertiary, fontWeight = FontWeight.Bold)) // Use tertiary for emphasis
                    append("This app is meant to unlock full potential of your device")
                    pop()
                }

                Text(
                    text = annotatedText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant // Use Material3 color for body text
                )

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = onDismissRequest,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary) // Use Material3 button colors
                ) {
                    Text("OK", color = MaterialTheme.colorScheme.onPrimary) // Use Material3 color
                }
            }
        }
    }
}
