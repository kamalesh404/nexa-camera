package com.nexa.camera.lab

import android.app.Application
import android.hardware.camera2.CameraManager
import android.provider.Settings
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nexa.camera.NativeBridge
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class LabState(val scanning: Boolean = false, val cameras: List<CameraReport> = emptyList(), val message: String = "", val deviceSummary: String = "", val nativeInfo: String = "")
data class CameraReport(val id: String, val classification: String, val lensFacing: String, val sensorOrientation: Int, val activeArray: String, val pixelArray: String, val physicalCameraIds: Set<String>, val focalLengths: String, val apertures: String, val focusDistance: String, val opticalStabilization: String, val largestJpeg: String, val largestYuv: String, val largestRaw: String, val rawSupported: Boolean, val flashAvailable: Boolean, val torchAvailable: Boolean, val isoRange: String, val exposureRange: String, val evRange: String, val evStep: String, val afModes: String, val aeModes: String, val awbModes: String, val fpsRanges: String, val videoConfigurations: String, val dynamicRangeProfiles: String, val colorSpaces: String, val jpegSizes: List<String>, val yuvSizes: List<String>, val rawSizes: List<String>, val highResolutionJpegSizes: List<String>, val highResolutionYuvSizes: List<String>, val availableCapabilities: String, val hardwareLevel: String, val minFrameDurations: String, val stallDurations: String, val allCharacteristics: String)

class CameraLabViewModel(app: Application) : AndroidViewModel(app) {
    private val _state = MutableStateFlow(LabState())
    val state = _state.asStateFlow()
    private val manager = app.getSystemService(CameraManager::class.java)
    init { scan() }

    fun scan() = viewModelScope.launch(Dispatchers.Default) {
        _state.value = _state.value.copy(scanning = true, message = "Reading Camera2 characteristics…")
        try {
            val reports = manager.cameraIdList.mapNotNull { id -> runCatching { CameraReportReader.read(manager, id) }.getOrNull() }
            val d = getApplication<Application>(); val dm = d.resources.displayMetrics
            val memoryInfo = android.app.ActivityManager.MemoryInfo(); d.getSystemService(android.app.ActivityManager::class.java).getMemoryInfo(memoryInfo)
            _state.value = LabState(false, reports, "${reports.size} camera(s) inspected. Export the report after connecting the physical HONOR 90.", "${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL} · ${android.os.Build.DEVICE} · Android ${android.os.Build.VERSION.RELEASE} (SDK ${android.os.Build.VERSION.SDK_INT}) · ABI ${android.os.Build.SUPPORTED_ABIS.firstOrNull() ?: "unknown"} · RAM ${memoryInfo.totalMem / 1_000_000} MB · ${dm.densityDpi} dpi", runCatching { NativeBridge.nativeBuildInfo() }.getOrDefault("Native layer unavailable"))
        } catch (e: Exception) { _state.value = _state.value.copy(scanning = false, message = "Camera Lab error: ${e.message}") }
    }

    fun exportJson() = viewModelScope.launch(Dispatchers.IO) {
        try {
            val cameraJson = JSONArray(); _state.value.cameras.forEach { cameraJson.put(it.toJson()) }
            val root = JSONObject().put("schemaVersion", 1).put("generatedAt", SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.US).format(Date())).put("device", _state.value.deviceSummary).put("cameras", cameraJson)
            val resolver = getApplication<Application>().contentResolver
            val values = android.content.ContentValues().apply { put(android.provider.MediaStore.Downloads.DISPLAY_NAME, "honor90_camera_report_${System.currentTimeMillis()}.json"); put(android.provider.MediaStore.Downloads.MIME_TYPE, "application/json"); put(android.provider.MediaStore.Downloads.IS_PENDING, 1) }
            val uri = resolver.insert(android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
            check(uri != null) { "Android could not create a Downloads file" }
            resolver.openOutputStream(uri)?.use { it.write(root.toString(2).toByteArray()) } ?: error("Android could not open the report file")
            values.clear(); values.put(android.provider.MediaStore.Downloads.IS_PENDING, 0); resolver.update(uri, values, null, null)
            _state.value = _state.value.copy(message = "Capability report exported to Downloads.")
        } catch (error: Exception) {
            _state.value = _state.value.copy(message = "Export failed safely: ${error.javaClass.simpleName}: ${error.message ?: "unknown error"}")
        }
    }
}

private fun CameraReport.toJson() = JSONObject().apply { CameraReport::class.java.declaredFields.filter { !it.isSynthetic && !java.lang.reflect.Modifier.isStatic(it.modifiers) }.forEach { f -> f.isAccessible = true; put(f.name, f.get(this@toJson)?.toString() ?: JSONObject.NULL) } }
