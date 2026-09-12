package com.nexa.camera

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.nexa.camera.lab.CameraLabViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { NexaCameraApp() }
    }
}

@Composable
private fun NexaCameraApp() {
    val context = androidx.compose.ui.platform.LocalContext.current
    var granted by remember { mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) }
    val request = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted = it }
    MaterialTheme(colorScheme = darkColorScheme()) {
        if (!granted) {
            Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center) {
                Text("NEXA CAMERA LAB", style = MaterialTheme.typography.headlineMedium)
                Spacer(Modifier.height(12.dp)); Text("Camera access is required to inspect the physical device's Camera2 capabilities.")
                Spacer(Modifier.height(20.dp)); Button(onClick = { request.launch(Manifest.permission.CAMERA) }) { Text("Grant camera access") }
            }
        } else CameraLabScreen()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CameraLabScreen() {
    val vm: CameraLabViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
    val state by vm.state.collectAsState()
    Scaffold(topBar = { TopAppBar(title = { Text("NEXA CAMERA LAB") }) }) { pad ->
        LazyColumn(Modifier.fillMaxSize().padding(pad).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item { Text("HONOR 90 diagnostic subsystem", style = MaterialTheme.typography.titleMedium); Text(state.deviceSummary, style = MaterialTheme.typography.bodySmall) }
            item { Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { Button(onClick = vm::scan) { Text(if (state.scanning) "Scanning…" else "Rescan") }; OutlinedButton(onClick = vm::exportJson, enabled = state.cameras.isNotEmpty()) { Text("Export JSON") } } }
            item { if (state.message.isNotBlank()) Text(state.message, color = MaterialTheme.colorScheme.primary) }
            items(state.cameras) { camera -> CameraCard(camera) }
            item { Text("Native engine", style = MaterialTheme.typography.titleSmall); Text(state.nativeInfo, style = MaterialTheme.typography.bodySmall) }
        }
    }
}

@Composable
private fun CameraCard(camera: com.nexa.camera.lab.CameraReport) {
    Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(14.dp)) {
        Text("Camera ${camera.id} · ${camera.classification}", style = MaterialTheme.typography.titleMedium)
        Text("Facing: ${camera.lensFacing}  |  Orientation: ${camera.sensorOrientation}°")
        Text("Sensor: ${camera.pixelArray}  |  Active array: ${camera.activeArray}")
        Text("Focal lengths: ${camera.focalLengths}  |  Apertures: ${camera.apertures}")
        Text("Focus: ${camera.focusDistance}  |  OIS: ${camera.opticalStabilization}")
        Spacer(Modifier.height(6.dp)); Text("JPEG max: ${camera.largestJpeg}  |  YUV max: ${camera.largestYuv}  |  RAW max: ${camera.largestRaw}")
        Text("RAW: ${camera.rawSupported}  |  Flash: ${camera.flashAvailable}  |  Torch: ${camera.torchAvailable}")
        Text("ISO: ${camera.isoRange}  |  Exposure: ${camera.exposureRange}  |  EV: ${camera.evRange} (step ${camera.evStep})")
        Text("AF: ${camera.afModes}  |  AE: ${camera.aeModes}  |  AWB: ${camera.awbModes}")
        Text("FPS: ${camera.fpsRanges}  |  Video: ${camera.videoConfigurations}")
        Text("Dynamic range: ${camera.dynamicRangeProfiles}  |  Color spaces: ${camera.colorSpaces}")
        Text("JPEG sizes: ${camera.jpegSizes.size}, YUV sizes: ${camera.yuvSizes.size}, RAW sizes: ${camera.rawSizes.size}", style = MaterialTheme.typography.bodySmall)
        Text("High-resolution JPEG: ${camera.highResolutionJpegSizes}  |  High-resolution YUV: ${camera.highResolutionYuvSizes}", style = MaterialTheme.typography.bodySmall)
        Text("Capabilities: ${camera.availableCapabilities}  |  Hardware level: ${camera.hardwareLevel}", style = MaterialTheme.typography.bodySmall)
        Text("Camera2 keys inspected: ${camera.allCharacteristics.count { it == '=' }}", style = MaterialTheme.typography.bodySmall)
        if (camera.physicalCameraIds.isNotEmpty()) Text("Physical IDs: ${camera.physicalCameraIds}")
    } }
}
