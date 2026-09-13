package com.nexa.camera.camera2

import android.content.ContentValues
import android.content.Context
import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CaptureRequest
import android.media.ImageReader
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.provider.MediaStore
import android.view.Surface
import android.view.TextureView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class Camera2CaptureController(private val context: Context, private val textureView: TextureView, initialCameraId: String, private val onStatus: (String) -> Unit) {
    private val manager = context.getSystemService(CameraManager::class.java)
    private val thread = HandlerThread("NexaCamera2").apply { start() }
    private val handler = Handler(thread.looper)
    private var cameraId = initialCameraId
    private var camera: CameraDevice? = null
    private var session: CameraCaptureSession? = null
    private var reader: ImageReader? = null
    private var previewRequest: CaptureRequest? = null
    private var manualMode = false
    private var manualIso = 100
    private var manualExposureNs = 33_333_333L
    private val mainHandler = Handler(Looper.getMainLooper())
    private var closed = false

    private fun status(message: String) { if (!closed) mainHandler.post { if (!closed) onStatus(message) } }

    fun open() {
        if (!textureView.isAvailable) { textureView.surfaceTextureListener = listener; return }
        handler.post {
            try {
                if (context.checkSelfPermission(android.Manifest.permission.CAMERA) != android.content.pm.PackageManager.PERMISSION_GRANTED) { status("Camera permission is required"); return@post }
                openInternal()
            } catch (e: Exception) { status("Camera open failed: ${e.message}") }
        }
    }

    fun switchCamera(newCameraId: String) {
        if (newCameraId == cameraId && camera != null) return
        handler.post {
            closeDeviceOnly()
            cameraId = newCameraId
            if (!closed) openInternal()
        }
    }

    fun setManualMode(enabled: Boolean) { handler.post { manualMode = enabled; updatePreview() } }
    fun setIso(value: Int) { handler.post { manualIso = value.coerceIn(50, 204800); updatePreview() } }
    fun setExposureNs(value: Long) { handler.post { manualExposureNs = value.coerceIn(1_000_000L, 1_000_000_000L); updatePreview() } }

    fun capture() { handler.post { try { val c = camera ?: return@post; val s = session ?: return@post; val request = c.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE).apply { addTarget(reader!!.surface); applyControls(this); set(CaptureRequest.JPEG_ORIENTATION, jpegOrientation()) }.build(); s.capture(request, null, handler) } catch (e: Exception) { status("Capture failed: ${e.message}") } } }

    fun close() { closed = true; handler.post { closeDeviceOnly() }; thread.quitSafely() }

    private fun openInternal() {
        try { manager.openCamera(cameraId, callback, handler) } catch (e: Exception) { status("Camera $cameraId open failed: ${e.message}") }
    }

    private fun closeDeviceOnly() { session?.close(); camera?.close(); reader?.close(); session = null; camera = null; reader = null; previewRequest = null }

    private val listener = object : TextureView.SurfaceTextureListener {
        override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) { open() }
        override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) = Unit
        override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean = true
        override fun onSurfaceTextureUpdated(surface: SurfaceTexture) = Unit
    }

    private val callback = object : CameraDevice.StateCallback() {
        override fun onOpened(device: CameraDevice) { camera = device; createSession(device) }
        override fun onDisconnected(device: CameraDevice) { device.close(); status("Camera disconnected") }
        override fun onError(device: CameraDevice, error: Int) { device.close(); status("Camera error $error") }
    }

    private fun createSession(device: CameraDevice) {
        val texture = textureView.surfaceTexture ?: return
        val characteristics = manager.getCameraCharacteristics(cameraId)
        val map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
        val previewSize = map?.getOutputSizes(android.graphics.SurfaceTexture::class.java)?.filter { it.width <= 1920 && it.height <= 1080 }?.maxByOrNull { it.width.toLong() * it.height } ?: android.util.Size(1280, 720)
        val jpegSize = map?.getOutputSizes(android.graphics.ImageFormat.JPEG)?.maxByOrNull { it.width.toLong() * it.height } ?: android.util.Size(640, 480)
        texture.setDefaultBufferSize(previewSize.width, previewSize.height)
        val preview = Surface(texture)
        reader = ImageReader.newInstance(jpegSize.width, jpegSize.height, android.graphics.ImageFormat.JPEG, 2).also { imageReader ->
            imageReader.setOnImageAvailableListener({ source -> source.acquireNextImage()?.use { image -> saveJpeg(image.planes[0].buffer) } }, handler)
        }
        device.createCaptureSession(listOf(preview, reader!!.surface), object : CameraCaptureSession.StateCallback() {
            override fun onConfigured(value: CameraCaptureSession) { session = value; previewRequest = device.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW).apply { addTarget(preview); val afModes = characteristics.get(CameraCharacteristics.CONTROL_AF_AVAILABLE_MODES)?.toSet().orEmpty(); if (afModes.contains(CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE)) set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE); applyControls(this) }.build(); value.setRepeatingRequest(previewRequest!!, null, handler); status("Camera $cameraId preview ready · ${jpegSize.width}×${jpegSize.height}") }
            override fun onConfigureFailed(value: CameraCaptureSession) { status("Camera $cameraId session configuration failed") }
        }, handler)
    }

    private fun applyControls(builder: CaptureRequest.Builder) {
        if (!manualMode) { builder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON); return }
        val c = manager.getCameraCharacteristics(cameraId)
        val isoRange = c.get(CameraCharacteristics.SENSOR_INFO_SENSITIVITY_RANGE)
        val exposureRange = c.get(CameraCharacteristics.SENSOR_INFO_EXPOSURE_TIME_RANGE)
        builder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_OFF)
        builder.set(CaptureRequest.SENSOR_SENSITIVITY, manualIso.coerceIn(isoRange?.lower ?: 50, isoRange?.upper ?: 204800))
        builder.set(CaptureRequest.SENSOR_EXPOSURE_TIME, manualExposureNs.coerceIn(exposureRange?.lower ?: 1_000_000L, exposureRange?.upper ?: 1_000_000_000L))
    }

    private fun updatePreview() { val current = session ?: return; val device = camera ?: return; val texture = textureView.surfaceTexture ?: return; val preview = Surface(texture); try { previewRequest = device.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW).apply { addTarget(preview); set(CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_MODE_AUTO); applyControls(this) }.build(); current.setRepeatingRequest(previewRequest!!, null, handler); status(if (manualMode) "PRO · ISO $manualIso · 1/${(1_000_000_000L / manualExposureNs).coerceAtLeast(1)}s" else "AUTO") } catch (e: Exception) { status("Manual control unavailable: ${e.message}") } }

    private fun saveJpeg(buffer: java.nio.ByteBuffer) {
        val values = ContentValues().apply { put(MediaStore.Images.Media.DISPLAY_NAME, "NEXA_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())}.jpg"); put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg"); put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/Nexa Camera"); put(MediaStore.Images.Media.IS_PENDING, 1) }
        val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: run { onStatus("Could not create image file"); return }
        context.contentResolver.openOutputStream(uri)?.use { output -> val bytes = ByteArray(buffer.remaining()); buffer.get(bytes); output.write(bytes) }
        values.clear(); values.put(MediaStore.Images.Media.IS_PENDING, 0); context.contentResolver.update(uri, values, null, null); onStatus("Photo saved to Pictures/Nexa Camera")
    }

    private fun jpegOrientation(): Int {
        val sensor = camera?.let { manager.getCameraCharacteristics(it.id).get(CameraCharacteristics.SENSOR_ORIENTATION) } ?: 90
        return sensor
    }
}
