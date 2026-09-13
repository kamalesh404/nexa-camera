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
    DisposableEffect(Unit) { onDispose { controller?.close(); controller = null } }
    Box(Modifier.fillMaxSize().background(Color.Black)) {
        AndroidView(factory = { context -> TextureView(context).also { view -> controller = Camera2CaptureController(context, view, selectedCamera) { message -> status = message }; controller?.open() } }, modifier = Modifier.fillMaxSize())
        Column(Modifier.fillMaxWidth().align(Alignment.TopCenter).padding(top = 28.dp, start = 16.dp, end = 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Surface(color = Color.Black.copy(alpha = 0.55f), shape = MaterialTheme.shapes.medium) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("NEXA CAMERA", color = Color.White, style = MaterialTheme.typography.titleMedium)
                    Text(if (selectedCamera == "0") "REAR · 1×" else "FRONT", color = Color.White, style = MaterialTheme.typography.labelLarge)
                }
            }
        }
        Column(Modifier.fillMaxWidth().align(Alignment.BottomCenter).padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Surface(color = Color.Black.copy(alpha = 0.65f), shape = MaterialTheme.shapes.large) {
                Column(Modifier.fillMaxWidth().padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(status, color = Color.White, style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.height(10.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        FilterChip(selected = selectedCamera == "0", onClick = { selectedCamera = "0"; controller?.switchCamera("0") }, label = { Text("Rear") })
                        Button(onClick = { controller?.capture() }, modifier = Modifier.size(width = 150.dp, height = 54.dp)) { Text("SHUTTER") }
                        FilterChip(selected = selectedCamera == "1", onClick = { selectedCamera = "1"; controller?.switchCamera("1") }, label = { Text("Front") })
                    }
                    Spacer(Modifier.height(6.dp))
                    OutlinedButton(onClick = onBack) { Text("Close preview") }
                }
            }
        }
    }
}
