package com.tangoplus.tangoq.mediapipe

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.os.Build
import android.os.SystemClock
import android.util.Log
import androidx.annotation.VisibleForTesting
import androidx.camera.core.ImageProxy
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.framework.image.MPImage
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.core.Delegate
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarker
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult
import com.tangoplus.tangoq.api.DeviceService.isEmulator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class PoseLandmarkerHelper(
    var minPoseDetectionConfidence: Float = DEFAULT_POSE_DETECTION_CONFIDENCE,
    var minPoseTrackingConfidence: Float = DEFAULT_POSE_TRACKING_CONFIDENCE,
    var minPosePresenceConfidence: Float = DEFAULT_POSE_PRESENCE_CONFIDENCE,
    private var currentModel: Int = MODEL_POSE_LANDMARKER_FULL,
    var currentDelegate: Int = DELEGATE_GPU,
    var runningMode: RunningMode = RunningMode.IMAGE,
    val context: Context,
    // 이 리스너는 RunningMode.LIVE_STREAM에서 실행될 때만 사용됩니다.
    val poseLandmarkerHelperListener: LandmarkerListener? = null
) {
    // 이 예에서는 변경 시 재설정될 수 있도록 var여야 합니다.
    // 포즈 랜드마크가 변경되지 않으면 lazy 값이 더 좋습니다.
    private var poseLandmarker: PoseLandmarker? = null

    init {
        setupPoseLandmarker()
    }
    fun clearPoseLandmarker() {
        poseLandmarker?.close()
        poseLandmarker = null
    }

    // PoseLandmarkerHelper의 실행 상태를 반환합니다.
    fun isClose(): Boolean {
        return poseLandmarker == null
    }

    fun setupPoseLandmarker() {
        // Set general pose landmarker options
        val baseOptionBuilder = BaseOptions.builder()

        // Use the specified hardware for running the model. Default to CPU
        when (currentDelegate) {
            DELEGATE_CPU -> {
                baseOptionBuilder.setDelegate(Delegate.CPU)
            }
            DELEGATE_GPU -> {
                baseOptionBuilder.setDelegate(Delegate.GPU)
            }
        }

        //-----------------------------------! ASSET MODEL 선택 !-----------------------------------
        // 이 모델은 Activity의 MODEL_POSE
        val modelName =
            when (currentModel) {
                MODEL_POSE_LANDMARKER_FULL -> "pose_landmarker_full.task"
                else -> "pose_landmarker_full.task"
            }

        baseOptionBuilder.setModelAssetPath(modelName)

        // runningMode가 poseLandmarkerHelperListener와 일치하는지 확인합니다.
        when (runningMode) {
            RunningMode.LIVE_STREAM -> {
                if (poseLandmarkerHelperListener == null) {
                    throw IllegalStateException(
                        "poseLandmarkerHelperListener must be set when runningMode is LIVE_STREAM."
                    )
                }
            }
            else -> { } // no-op
        }
        try {
            if (!isEmulator()) {
                val baseOptions = baseOptionBuilder.build()

                // 기본 옵션과 특정 옵션이 포함된 옵션 빌더를 생성합니다.
                // 옵션은 Pose Landmarker에만 사용됩니다.
                val optionsBuilder =
                    PoseLandmarker.PoseLandmarkerOptions.builder()
                        .setBaseOptions(baseOptions)
                        .setMinPoseDetectionConfidence(minPoseDetectionConfidence)
                        .setMinTrackingConfidence(minPoseTrackingConfidence)
                        .setMinPosePresenceConfidence(minPosePresenceConfidence)
                        .setRunningMode(runningMode)

                // ResultListener 및 ErrorListener는 LIVE_STREAM 모드에만 사용됩니다.
                if (runningMode == RunningMode.LIVE_STREAM) {
                    optionsBuilder
                        .setResultListener(this::returnLivestreamResult)
                        .setErrorListener(this::returnLivestreamError)
                }

                CoroutineScope(Dispatchers.Main).launch {
                    try {
                        val options = optionsBuilder.build()
                        Log.d("PoseLandmarkerDebug", "Build.PRODUCT: ${Build.PRODUCT}")
                        Log.d("PoseLandmarkerDebug", "Build.HARDWARE: ${Build.HARDWARE}")
                        Log.d("PoseLandmarkerDebug", "Build.MODEL: ${Build.MODEL}")
                        Log.d("PoseLandmarkerDebug", "Build.MANUFACTURER: ${Build.MANUFACTURER}")
                        Log.d("PoseLandmarkerDebug", "Build.BRAND: ${Build.BRAND}")
                        Log.d("PoseLandmarkerDebug", "Build.DEVICE: ${Build.DEVICE}")
                        Log.d("PoseLandmarkerDebug", "Build.FINGERPRINT: ${Build.FINGERPRINT}")

                        // 옵션 생성 전 옵션 객체 세부 정보 로깅
                        Log.d("PoseLandmarkerDebug", "Options: $options")


                        poseLandmarker = PoseLandmarker.createFromOptions(context, options)
                    } catch (e: UnsatisfiedLinkError) {
                        poseLandmarker = null
                        Log.e(TAG, "${e.message}")
                    } catch (e: IllegalStateException) {
                        Log.e(TAG, "${e.message}")
                    } catch (e: NullPointerException) {
                        Log.e(TAG, "${e.message}")
                    } catch (e: IllegalArgumentException) {
                        Log.e(TAG, "${e.message}")
                    }
                }
            } else {
                poseLandmarker = null
            }
        } catch (e: IllegalStateException) {
            poseLandmarkerHelperListener?.onError(
                "Pose Landmarker failed to initialize. See error logs for " +
                        "details"
            )
            Log.e(
                TAG, "MediaPipe failed to load the task with error: " + e
                    .message
            )
        } catch (e: RuntimeException) {
            // This occurs if the model being used does not support GPU
            poseLandmarkerHelperListener?.onError(
                "Pose Landmarker failed to initialize. See error logs for " +
                        "details", GPU_ERROR
            )
            Log.e(
                TAG,
                "Image classifier failed to load model with error: " + e.message
            )
        }
    }
    // ImageProxy를 MP 이미지로 변환하고 PoselandmakerHelper에 공급합니다.
    fun detectLiveStream(
        imageProxy: ImageProxy,
        isFrontCamera: Boolean
    ) {
        if (runningMode != RunningMode.LIVE_STREAM) {
            throw IllegalArgumentException(
                "Attempting to call detectLiveStream" +
                        " while not using RunningMode.LIVE_STREAM"
            )
        }
        val frameTime = SystemClock.uptimeMillis()

        // Copy out RGB bits from the frame to a bitmap buffer
        val bitmapBuffer =
            Bitmap.createBitmap(
                imageProxy.width,
                imageProxy.height,
                Bitmap.Config.ARGB_8888
            )

        imageProxy.use { bitmapBuffer.copyPixelsFromBuffer(imageProxy.planes[0].buffer) }
        imageProxy.close()

        val matrix = Matrix().apply {
            // 카메라에서 수신된 프레임을 표시될 방향과 같은 방향으로 회전합니다.
            postRotate(imageProxy.imageInfo.rotationDegrees.toFloat())

            // flip image if user use front camera
            if (isFrontCamera) {
                postScale(
                    -1f,
                    1f,
                    imageProxy.width.toFloat(),
                    imageProxy.height.toFloat()
                )
            }
        }
        val rotatedBitmap = Bitmap.createBitmap(
            bitmapBuffer, 0, 0, bitmapBuffer.width, bitmapBuffer.height,
            matrix, true
        )

        // 추론을 실행하기 위해 입력 Bitmap 객체를 MPImage 객체로 변환합니다.
        val mpImage = BitmapImageBuilder(rotatedBitmap).build()

        detectAsync(mpImage, frameTime)
    }

    // MediaPipe Pose Landmarker API를 사용하여 포즈 랜드마크를 실행합니다.
    @VisibleForTesting
    fun detectAsync(mpImage: MPImage, frameTime: Long) {
        poseLandmarker?.detectAsync(mpImage, frameTime)
        // LIVE_STREAM 실행 모드를 사용하므로 랜드마크 결과는
        // returnLivestreamResult 함수에서 반환됩니다.
    }

    // poseLandmarker?.Detect()가 null을 반환하는 경우 이는 오류일 가능성이 높습니다. null 반환
    // 이를 나타냅니다.
    private fun returnLivestreamResult(
        result: PoseLandmarkerResult,
        input: MPImage
    ) {
        val finishTimeMs = SystemClock.uptimeMillis()
        val inferenceTime = finishTimeMs - result.timestampMs()

        poseLandmarkerHelperListener?.onResults(
            ResultBundle(
                listOf(result),
                inferenceTime,
                input.height,
                input.width
            )
        )

    }

    // 감지 중에 발생한 오류를 이 PoseLandmarkerHelper에 반환합니다.
    // caller
    private fun returnLivestreamError(error: RuntimeException) {
        poseLandmarkerHelperListener?.onError(
            error.message ?: "An unknown error has occurred"
        )
    }
    companion object {
        const val TAG = "PoseLandmarkerHelper"

        const val DELEGATE_CPU = 0
        const val DELEGATE_GPU = 1
        const val DEFAULT_POSE_DETECTION_CONFIDENCE = 0.6F
        const val DEFAULT_POSE_TRACKING_CONFIDENCE = 0.6F
        const val DEFAULT_POSE_PRESENCE_CONFIDENCE = 0.6F
        const val OTHER_ERROR = 0
        const val GPU_ERROR = 1
        const val MODEL_POSE_LANDMARKER_FULL = 0
    }
    data class ResultBundle(
        var results: List<PoseLandmarkerResult>,
        val inferenceTime: Long,
        val inputImageHeight: Int,
        val inputImageWidth: Int,
    )
    interface LandmarkerListener {
        fun onError(error: String, errorCode: Int = OTHER_ERROR)
        fun onResults(resultBundle: ResultBundle)
    }
}