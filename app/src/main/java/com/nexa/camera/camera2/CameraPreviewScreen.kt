package com.nexa.camera.camera2

import android.hardware.camera2.CaptureRequest
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
    var proMode by remember { mutableStateOf(false) }
    var iso by remember { mutableStateOf(100) }
    var shutterIndex by remember { mutableStateOf(1) }
    var ev by remember { mutableStateOf(0) }
    var wbIndex by remember { mutableStateOf(0) }
    var manualFocus by remember { mutableStateOf(false) }
    var focusDistance by remember { mutableStateOf(0f) }
    var aeLock by remember { mutableStateOf(false) }
    var awbLock by remember { mutableStateOf(false) }
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
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                        FilterChip(selected = proMode, onClick = { proMode = !proMode; controller?.setManualMode(proMode) }, label = { Text(if (proMode) "PRO" else "AUTO") })
                        if (proMode) {
                            TextButton(onClick = { iso = (iso / 2).coerceAtLeast(50); controller?.setIso(iso) }) { Text("ISO− $iso") }
                            TextButton(onClick = { iso = (iso * 2).coerceAtMost(204800); controller?.setIso(iso) }) { Text("ISO+") }
                            val shutters = listOf(250_000_000L, 33_333_333L, 8_000_000L, 2_000_000L)
                            TextButton(onClick = { shutterIndex = (shutterIndex + 1) % shutters.size; controller?.setExposureNs(shutters[shutterIndex]) }) { Text("Shutter") }
                        }
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                        TextButton(onClick = { ev = (ev + 1).coerceAtMost(3); controller?.setExposureCompensation(ev) }) { Text("EV +$ev") }
                        TextButton(onClick = { ev = if (ev <= -3) 3 else ev - 1; controller?.setExposureCompensation(ev) }) { Text("EV−") }
                        val wbModes = listOf(
                            CaptureRequest.CONTROL_AWB_MODE_AUTO to "WB Auto",
                            CaptureRequest.CONTROL_AWB_MODE_INCANDESCENT to "WB Tung",
                            CaptureRequest.CONTROL_AWB_MODE_FLUORESCENT to "WB Fluor",
                            CaptureRequest.CONTROL_AWB_MODE_DAYLIGHT to "WB Day",
                            CaptureRequest.CONTROL_AWB_MODE_CLOUDY_DAYLIGHT to "WB Cloud"
                        )
                        TextButton(onClick = { wbIndex = (wbIndex + 1) % wbModes.size; controller?.setWhiteBalance(wbModes[wbIndex].first) }) { Text(wbModes[wbIndex].second) }
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                        FilterChip(selected = manualFocus, onClick = { manualFocus = !manualFocus; controller?.setManualFocus(manualFocus) }, label = { Text(if (manualFocus) "Focus Manual" else "Focus Auto") })
                        if (manualFocus) {
                            TextButton(onClick = { focusDistance = (focusDistance - 0.5f).coerceAtLeast(0f); controller?.setFocusDistance(focusDistance) }) { Text("Focus −") }
                            Text("${"%.1f".format(focusDistance)}D", color = Color.White)
                            TextButton(onClick = { focusDistance = (focusDistance + 0.5f).coerceAtMost(20f); controller?.setFocusDistance(focusDistance) }) { Text("Focus +") }
                        }
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        FilterChip(selected = aeLock, onClick = { aeLock = !aeLock; controller?.setAeLock(aeLock) }, label = { Text(if (aeLock) "AE Locked" else "Lock AE") })
                        FilterChip(selected = awbLock, onClick = { awbLock = !awbLock; controller?.setAwbLock(awbLock) }, label = { Text(if (awbLock) "AWB Locked" else "Lock WB") })
                    }
                    Spacer(Modifier.height(6.dp))
                    OutlinedButton(onClick = onBack) { Text("Close preview") }
                }
            }
        }
    }
}
