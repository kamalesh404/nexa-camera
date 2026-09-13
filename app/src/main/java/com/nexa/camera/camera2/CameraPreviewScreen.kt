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
    var selectedCamera by remember { mutableStateOf("0") }
    var controller by remember { mutableStateOf<Camera2CaptureController?>(null) }
    var status by remember { mutableStateOf("Opening Camera2 preview…") }
    DisposableEffect(selectedCamera) { onDispose { controller?.close(); controller = null } }
    Box(Modifier.fillMaxSize().background(Color.Black)) {
        key(selectedCamera) { AndroidView(factory = { context -> TextureView(context).also { view -> controller = Camera2CaptureController(context, view, selectedCamera) { message -> status = message }; controller?.open() } }, modifier = Modifier.fillMaxSize()) }
        Column(Modifier.fillMaxWidth().align(Alignment.BottomCenter).padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(status, color = Color.White, style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                OutlinedButton(onClick = { selectedCamera = "0" }) { Text("Rear") }
                OutlinedButton(onClick = { selectedCamera = "1" }) { Text("Front") }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                OutlinedButton(onClick = onBack) { Text("Back") }
                Button(onClick = { controller?.capture() }) { Text("Capture JPEG") }
            }
        }
    }
}
