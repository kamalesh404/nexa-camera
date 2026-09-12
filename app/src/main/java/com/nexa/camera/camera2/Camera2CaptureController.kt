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
import android.provider.MediaStore
import android.view.Surface
import android.view.TextureView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class Camera2CaptureController(private val context: Context, private val textureView: TextureView, private val cameraId: String, private val onStatus: (String) -> Unit) {
    private val manager = context.getSystemService(CameraManager::class.java)
    private val thread = HandlerThread("NexaCamera2").apply { start() }
    private val handler = Handler(thread.looper)
    private var camera: CameraDevice? = null
    private var session: CameraCaptureSession? = null
    private var reader: ImageReader? = null
    private var previewRequest: CaptureRequest? = null

    fun open() {
        if (!textureView.isAvailable) { textureView.surfaceTextureListener = listener; return }
        handler.post {
            try {
                if (context.checkSelfPermission(android.Manifest.permission.CAMERA) != android.content.pm.PackageManager.PERMISSION_GRANTED) { onStatus("Camera permission is required"); return@post }
                manager.openCamera(cameraId, callback, handler)
            } catch (e: Exception) { onStatus("Camera open failed: ${e.message}") }
        }
    }

    fun capture() { handler.post { try { val c = camera ?: return@post; val s = session ?: return@post; val request = c.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE).apply { addTarget(reader!!.surface); set(CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_MODE_AUTO); set(CaptureRequest.JPEG_ORIENTATION, jpegOrientation()) }.build(); s.capture(request, null, handler) } catch (e: Exception) { onStatus("Capture failed: ${e.message}") } } }

    fun close() { handler.post { session?.close(); camera?.close(); reader?.close(); session = null; camera = null; reader = null }; thread.quitSafely() }

    private val listener = object : TextureView.SurfaceTextureListener {
        override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) { open() }
        override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) = Unit
        override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean = true
        override fun onSurfaceTextureUpdated(surface: SurfaceTexture) = Unit
    }

    private val callback = object : CameraDevice.StateCallback() {
        override fun onOpened(device: CameraDevice) { camera = device; createSession(device) }
        override fun onDisconnected(device: CameraDevice) { device.close(); onStatus("Camera disconnected") }
        override fun onError(device: CameraDevice, error: Int) { device.close(); onStatus("Camera error $error") }
    }

    private fun createSession(device: CameraDevice) {
        val texture = textureView.surfaceTexture ?: return
        texture.setDefaultBufferSize(textureView.width.coerceAtLeast(640), textureView.height.coerceAtLeast(480))
        val preview = Surface(texture)
        reader = ImageReader.newInstance(4064, 3048, android.graphics.ImageFormat.JPEG, 2).also { imageReader ->
            imageReader.setOnImageAvailableListener({ source -> source.acquireNextImage()?.use { image -> saveJpeg(image.planes[0].buffer) } }, handler)
        }
        device.createCaptureSession(listOf(preview, reader!!.surface), object : CameraCaptureSession.StateCallback() {
            override fun onConfigured(value: CameraCaptureSession) { session = value; previewRequest = device.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW).apply { addTarget(preview); set(CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_MODE_AUTO); set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE) }.build(); value.setRepeatingRequest(previewRequest!!, null, handler); onStatus("Camera $cameraId preview ready") }
            override fun onConfigureFailed(value: CameraCaptureSession) { onStatus("Camera session configuration failed") }
        }, handler)
    }

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
