package com.tangoplus.tangoq.service

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.Image
import android.media.ImageReader
import android.media.MediaRecorder
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Binder
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.MediaStore
import android.util.DisplayMetrics
import android.util.Log
import android.view.SurfaceView
import android.view.WindowManager
import android.widget.Toast
import androidx.camera.view.PreviewView
import androidx.core.app.NotificationCompat
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.ReturnCode
import com.tangoplus.tangoq.MainActivity
import com.tangoplus.tangoq.R
import com.tangoplus.tangoq.callback.CaptureCallback
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.OutputStream

class MediaProjectionService : Service() {
    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    // 화면 캡쳐
    private lateinit var imageReader: ImageReader
    private var isCapturing = false
    // 화면 녹화
    private var mediaRecorder : MediaRecorder? = null
    private var videoFilePath: String = ""

    private var callback: CaptureCallback? = null
    fun setCallback(callback: CaptureCallback) {
        this.callback = callback
    }

    // ------! 서비스 intent 호출에 대한 응답 시작 !------
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForegroundService()
        val projectionData = intent?.getParcelableExtra<Intent>(EXTRA_PROJECTION_DATA)
        val resultCode = intent?.getIntExtra(EXTRA_RESULT_CODE, Activity.RESULT_CANCELED)

        if (projectionData != null && resultCode == Activity.RESULT_OK) {
            mediaProjection = (getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager)
                .getMediaProjection(resultCode, projectionData)

            Log.v("onStartCommand", "$mediaProjection")

        }

        return START_NOT_STICKY
    } // ------! 서비스 intent 호출에 대한 응답 끝 !------

    private fun startForegroundService() {
        val channelId = "ScreenCaptureServiceChannel"
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        Log.v("serviceinit", channelId)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Screen Capture Service",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationManager.createNotificationChannel(channel)
        }

        val notification: Notification = Notification.Builder(this, channelId)
            .setContentTitle("Screen Capture Service")
            .setContentText("Capturing the screen")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .build()
        Log.v("serviceinit", "$notification")
        startForeground(1, notification)
    }
    private val binder = LocalBinder()
    inner class LocalBinder : Binder() {
        fun getService(): MediaProjectionService = this@MediaProjectionService
    }
    override fun onBind(intent: Intent?): IBinder {
        return binder
    }

    // ------! 화면 생성 시작 !------
    private fun setupVirtualDisplay() {
        val metrics = DisplayMetrics()
        val windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        windowManager.defaultDisplay.getMetrics(metrics)

        val density = metrics.densityDpi
        val displayWidth = metrics.widthPixels
        val displayHeight = metrics.heightPixels

        imageReader = ImageReader.newInstance(displayWidth, displayHeight, PixelFormat.RGBA_8888, 2)

        virtualDisplay = mediaProjection?.createVirtualDisplay(
            "ScreenCapture",
            displayWidth,
            displayHeight,
            density,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            imageReader.surface,
            null,
            null
        )
        isCapturing = true
        imageReader.setOnImageAvailableListener({ reader ->
            if (isCapturing) {
                isCapturing = false
                val image = reader.acquireLatestImage()
                if (image != null) {
                    processImage(image)
                    image.close()
                }
            }
            imageReader.setOnImageAvailableListener(null, null)
            virtualDisplay?.release()
            virtualDisplay = null
        }, null)

    }
    // ------! 화면에 대한 캡처 시작 !------
    private fun processImage(image: Image) {
        val buffer = image.planes[0].buffer
        val pixelStride = image.planes[0].pixelStride
        val rowStride = image.planes[0].rowStride
        val rowPadding = rowStride - pixelStride * image.width

        val bitmap = Bitmap.createBitmap(
            image.width + rowPadding / pixelStride,
            image.height,
            Bitmap.Config.ARGB_8888
        )
        bitmap.copyPixelsFromBuffer(buffer)
        val croppedBitmap = cropStatusBarAndNavigationBar(bitmap)
        saveBitmapToGallery(croppedBitmap)
        imageReader.close()

    } // ------! 화면에 대한 캡처 끝 !------

    // ------! 갤러리에 저장 시작 !------
    private fun saveBitmapToGallery(bitmap: Bitmap) {

        val filename = "screenshot_${System.currentTimeMillis()}.png"
        val fos: OutputStream?

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val resolver = contentResolver
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
            }
            val imageUri: Uri? = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
            fos = imageUri?.let { resolver.openOutputStream(it) }
        } else {
            val imagesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).toString()
            val image = File(imagesDir, filename)
            fos = FileOutputStream(image)
        }

        fos?.use {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
            Toast.makeText(this, "Image saved to gallery", Toast.LENGTH_SHORT).show()
            callback?.onCaptureComplete(filename)
        }

    } // ------! 갤러리에 저장 끝 !------

    // ------! 화면 캡처 트리거 !------
    fun captureScreen(callback: () -> Unit){
        // Trigger an image capture
        Log.v("captureScreen", "Triggering screen capture")
        setupVirtualDisplay()
        callback()
    }

    // ------! 화면 생성 끝 !------
    private fun setupMediaRecorder() {
        val videoFile = File(getExternalFilesDir(null), "temp_video_${System.currentTimeMillis()}.mp4")
        videoFilePath = videoFile.absolutePath
        mediaRecorder = MediaRecorder()
        mediaRecorder?.apply {
            setVideoSource(MediaRecorder.VideoSource.SURFACE)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setVideoSize(1080, 1920) // Change according to your requirement
            setVideoEncoder(MediaRecorder.VideoEncoder.H264)
            setVideoEncodingBitRate(256 * 500)
            setVideoFrameRate(30)
            setOutputFile(videoFilePath)
            prepare()
        }
    }
    // ------! 화면 녹화 시작 !------
    private fun startRecording() {
        setupMediaRecorder()
        val metrics = DisplayMetrics()
        val windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        windowManager.defaultDisplay.getMetrics(metrics)

        val density = metrics.densityDpi
        val displayWidth = metrics.widthPixels
        val displayHeight = metrics.heightPixels

        virtualDisplay = mediaProjection?.createVirtualDisplay(
            "ScreenRecording",
            displayWidth,
            displayHeight,
            density,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            mediaRecorder?.surface,
            null,
            null
        )

        mediaRecorder?.start()
        Handler(Looper.getMainLooper()).postDelayed({
            stopRecording()
        }, 4000) // 3 seconds
    }
    private fun stopRecording() {
        mediaRecorder?.apply {
            stop()
            val outputFilePath = "${getExternalFilesDir(null)}/cropped_video_${System.currentTimeMillis()}.mp4"
            cropVideo(videoFilePath, outputFilePath, getStatusBarHeight(), getNavigationBarHeight())
            reset()
            release()
        }
        mediaRecorder = null
        virtualDisplay?.release()
        virtualDisplay = null
        imageReader.close()
    }
    // ------! 화면 녹화 끝 !------

    // ------! 비디오 저장 시작 !------
    private fun cropVideo(inputFilePath: String, outputFilePath: String, topCrop: Int, bottomCrop: Int) {
        val metrics = DisplayMetrics()
        val windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        windowManager.defaultDisplay.getMetrics(metrics)

        val videoWidth = metrics.widthPixels
        val videoHeight = metrics.heightPixels - topCrop - bottomCrop

        val command = "-i $inputFilePath -vf crop=$videoWidth:$videoHeight:0:$topCrop -c:a copy $outputFilePath"

        FFmpegKit.executeAsync(command) { session ->
            val returnCode = session.returnCode
            if (ReturnCode.isSuccess(returnCode)) {
                // Command succeeded
                saveVideoToGallery(outputFilePath)
            } else if (ReturnCode.isCancel(returnCode)) {
                // Command cancelled
                Log.e("FFmpeg", "Command cancelled")
            } else {
                // Command failed
                Log.e("FFmpeg", "Command failed with returnCode=$returnCode")
            }
        }
    }
    private fun saveVideoToGallery(videoFilePath: String) {

        val filename = "video_${System.currentTimeMillis()}.mp4"
        val fos: OutputStream?

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val resolver = contentResolver
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
            }
            val videoUri: Uri? = resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, contentValues)
            fos = videoUri?.let { resolver.openOutputStream(it) }
        } else {
            val videosDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES).toString()
            val video = File(videosDir, filename)
            fos = FileOutputStream(video)
        }
        fos?.use { outputStream ->
            FileInputStream(videoFilePath).use { inputStream ->
                inputStream.copyTo(outputStream)
            }
            Toast.makeText(this, "촬영한 비디오가 갤러리에 저장됐습니다 !", Toast.LENGTH_LONG).show()

        } ?: run {
            Toast.makeText(this, "저장에 실패했습니다.", Toast.LENGTH_SHORT).show()

        }
    }
    // ------! 비디오 저장 끝 !------

    // ------! 화면 녹화 트리거 시작 !------
    fun recordScreen(callback: () -> Unit) {
        Log.v("recordScreen", "Triggering screen record")
        startRecording()
        callback()
    }

    // ------! 화면 녹화 트리거 끝 !------
    companion object {
        const val EXTRA_PROJECTION_DATA = "EXTRA_PROJECTION_DATA"
        const val EXTRA_RESULT_CODE = "EXTRA_RESULT_CODE"
    }

    // ------! 캡처 후 상단 하단바 자르기 시작 !------
    @SuppressLint("InternalInsetResource")
    private fun getStatusBarHeight(): Int {
        var result = 0
        val resourceId = resources.getIdentifier("status_bar_height", "dimen", "android")
        if (resourceId > 0) {
            result = resources.getDimensionPixelSize(resourceId)
        }
        return result
    }

    @SuppressLint("InternalInsetResource")
    private fun getNavigationBarHeight(): Int {
        var result = 0
        val resourceId = resources.getIdentifier("navigation_bar_height", "dimen", "android")
        if (resourceId > 0) {
            result = resources.getDimensionPixelSize(resourceId)
        }
        return result
    }
    private fun cropStatusBarAndNavigationBar(bitmap: Bitmap): Bitmap {
        val statusBarHeight = getStatusBarHeight()
        val navigationBarHeight = getNavigationBarHeight()
        val width = bitmap.width
        val height = bitmap.height - statusBarHeight - navigationBarHeight

        return Bitmap.createBitmap(bitmap, 0, statusBarHeight, width, height)
    } // ------! 캡처 후 상단 하단바 자르기 끝 !------

    // ------! 액티비티 종료 시 파일 전부 삭제 !------
    private fun deleteFilesInExternalStorage() {
        Log.v("deleteData", "DeleteFilesInExternalStorage")
        val directory = getExternalFilesDir(null)
        if (directory != null && directory.isDirectory) {
            val files = directory.listFiles()
            files?.forEach { file ->
                file.delete()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::imageReader.isInitialized) {
            imageReader.close()
        }

        deleteFilesInExternalStorage()
    }
}