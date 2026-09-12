package com.nexa.camera.lab

import android.graphics.ImageFormat
import android.hardware.camera2.*
import android.hardware.camera2.params.StreamConfigurationMap
import android.util.Range
import android.util.Size
import java.util.Locale

object CameraReportReader {
    fun read(manager: CameraManager, id: String): CameraReport {
        val c = manager.getCameraCharacteristics(id); val map = c.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
        fun sizes(format: Int) = map?.getOutputSizes(format)?.map { "${it.width}x${it.height}" } ?: emptyList()
        fun value(key: CameraCharacteristics.Key<*>): String {
            val v = c.get(key) ?: return "not exposed"
            return when (v) {
                is IntArray -> v.joinToString()
                is FloatArray -> v.joinToString()
                is LongArray -> v.joinToString()
                is Array<*> -> v.joinToString()
                else -> v.toString()
            }
        }
        val jpeg = sizes(ImageFormat.JPEG); val yuv = sizes(ImageFormat.YUV_420_888); val raw = sizes(ImageFormat.RAW_SENSOR)
        val highJpeg = map?.getHighResolutionOutputSizes(ImageFormat.JPEG)?.map { "${it.width}x${it.height}" } ?: emptyList()
        val highYuv = map?.getHighResolutionOutputSizes(ImageFormat.YUV_420_888)?.map { "${it.width}x${it.height}" } ?: emptyList()
        val maximumMap = if (android.os.Build.VERSION.SDK_INT >= 31) c.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP_MAXIMUM_RESOLUTION) else null
        fun maximumSizes(format: Int) = maximumMap?.getOutputSizes(format)?.map { "${it.width}x${it.height}" } ?: emptyList()
        val facing = when (c.get(CameraCharacteristics.LENS_FACING)) { CameraCharacteristics.LENS_FACING_BACK -> "BACK"; CameraCharacteristics.LENS_FACING_FRONT -> "FRONT"; CameraCharacteristics.LENS_FACING_EXTERNAL -> "EXTERNAL"; else -> "UNKNOWN" }
        val focal = c.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS)?.joinToString { "${"%.2f".format(Locale.US, it)}mm" }.orEmpty()
        val minimumFocal = c.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS)?.minOrNull()
        val cls = when {
            facing == "FRONT" -> "FRONT CAMERA"
            minimumFocal != null && minimumFocal <= 2.0f -> "ULTRAWIDE / WIDE (verify physically)"
            else -> "MAIN / REAR CAMERA (verify physically)"
        }
        val ev = c.get(CameraCharacteristics.CONTROL_AE_COMPENSATION_RANGE); val step = c.get(CameraCharacteristics.CONTROL_AE_COMPENSATION_STEP)
        val profiles = if (android.os.Build.VERSION.SDK_INT >= 33) runCatching { c.get(CameraCharacteristics.REQUEST_AVAILABLE_DYNAMIC_RANGE_PROFILES)?.supportedProfiles?.joinToString() }.getOrNull() ?: "not exposed" else "API 33+ only"
        val allCharacteristics = c.keys.joinToString("\n") { key -> "${key.name}=${runCatching { c.get(key)?.toString() ?: "null" }.getOrDefault("unavailable")}" }
        val minDurations = durations(map, ImageFormat.JPEG, jpeg.firstOrNull())
        val stallDurations = stallDurations(map, ImageFormat.JPEG, jpeg.firstOrNull())
        return CameraReport(id, cls, facing, c.get(CameraCharacteristics.SENSOR_ORIENTATION) ?: -1, c.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE)?.toString() ?: "not exposed", c.get(CameraCharacteristics.SENSOR_INFO_PIXEL_ARRAY_SIZE)?.toString() ?: "not exposed", c.physicalCameraIds, focal, value(CameraCharacteristics.LENS_INFO_AVAILABLE_APERTURES), c.get(CameraCharacteristics.LENS_INFO_MINIMUM_FOCUS_DISTANCE)?.toString() ?: "not exposed", value(CameraCharacteristics.LENS_INFO_AVAILABLE_OPTICAL_STABILIZATION), largest(jpeg), largest(yuv), largest(raw), raw.isNotEmpty(), c.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true, c.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true, c.get(CameraCharacteristics.SENSOR_INFO_SENSITIVITY_RANGE)?.toString() ?: "not exposed", c.get(CameraCharacteristics.SENSOR_INFO_EXPOSURE_TIME_RANGE)?.toString() ?: "not exposed", ev?.toString() ?: "not exposed", step?.toString() ?: "not exposed", value(CameraCharacteristics.CONTROL_AF_AVAILABLE_MODES), value(CameraCharacteristics.CONTROL_AE_AVAILABLE_MODES), value(CameraCharacteristics.CONTROL_AWB_AVAILABLE_MODES), c.get(CameraCharacteristics.CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES)?.joinToString() ?: "not exposed", videoConfigs(map), profiles, "API-dependent / not directly exposed", jpeg, yuv, raw, highJpeg, highYuv, maximumSizes(ImageFormat.JPEG), maximumSizes(ImageFormat.YUV_420_888), maximumSizes(ImageFormat.RAW_SENSOR), value(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES), value(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL), minDurations, stallDurations, allCharacteristics)
    }
    private fun largest(xs: List<String>) = xs.maxByOrNull { it.substringBefore('x').toLongOrNull()?.times(it.substringAfter('x').toLongOrNull() ?: 0) ?: 0 } ?: "not exposed"
    private fun videoConfigs(map: StreamConfigurationMap?) = map?.getOutputSizes(android.media.MediaRecorder::class.java)?.joinToString() ?: "not exposed"
    private fun durations(map: StreamConfigurationMap?, format: Int, first: String?) = first?.split("x")?.mapNotNull { it.toLongOrNull() }?.let { if (it.size == 2) map?.getOutputMinFrameDuration(format, Size(it[0].toInt(), it[1].toInt())).toString() else "not exposed" } ?: "not exposed"
    private fun stallDurations(map: StreamConfigurationMap?, format: Int, first: String?) = first?.split("x")?.mapNotNull { it.toLongOrNull() }?.let { if (it.size == 2) map?.getOutputStallDuration(format, Size(it[0].toInt(), it[1].toInt())).toString() else "not exposed" } ?: "not exposed"
}
