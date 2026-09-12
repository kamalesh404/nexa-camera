package com.nexa.camera.camera2

import android.view.TextureView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView

@Composable
fun CameraPreviewScreen(onBack: () -> Unit) {
    var controller by remember { mutableStateOf<Camera2CaptureController?>(null) }
    var status by remember { mutableStateOf("Opening Camera2 preview…") }
    DisposableEffect(Unit) { onDispose { controller?.close() } }
    Box(Modifier.fillMaxSize().background(Color.Black)) {
        AndroidView(factory = { context -> TextureView(context).also { view -> controller = Camera2CaptureController(context, view) { message -> status = message }; controller?.open() } }, modifier = Modifier.fillMaxSize())
        Column(Modifier.fillMaxWidth().align(Alignment.BottomCenter).padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(status, color = Color.White, style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                OutlinedButton(onClick = onBack) { Text("Back") }
                Button(onClick = { controller?.capture() }) { Text("Capture JPEG") }
            }
        }
    }
}
