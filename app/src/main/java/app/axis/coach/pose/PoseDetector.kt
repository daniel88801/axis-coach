package app.axis.coach.pose

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.os.SystemClock
import android.util.Log
import androidx.camera.core.ImageProxy
import app.axis.coach.domain.model.Landmark
import app.axis.coach.domain.model.PoseFrame
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.core.Delegate
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarker
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

class PoseDetector(
    context: Context,
    private val onFrame: (PoseFrame) -> Unit,
    private val onError: (String) -> Unit = {},
) {
    val executor: ExecutorService = Executors.newSingleThreadExecutor()
    private val closed = AtomicBoolean(false)
    private var landmarker: PoseLandmarker? = null

    init {
        try {
            val options = PoseLandmarker.PoseLandmarkerOptions.builder()
                .setBaseOptions(
                    BaseOptions.builder()
                        .setModelAssetPath(MODEL_ASSET)
                        .setDelegate(Delegate.CPU)
                        .build(),
                )
                .setRunningMode(RunningMode.LIVE_STREAM)
                .setNumPoses(1)
                .setMinPoseDetectionConfidence(0.5f)
                .setMinTrackingConfidence(0.5f)
                .setMinPosePresenceConfidence(0.5f)
                .setResultListener { result, input ->
                    val pose = result.toDomain(input.width, input.height)
                    if (pose != null) onFrame(pose)
                }
                .setErrorListener { error ->
                    Log.e(TAG, "Landmarker error", error)
                    onError(error.message ?: "Pose detector failed")
                }
                .build()
            landmarker = PoseLandmarker.createFromOptions(context, options)
        } catch (t: Throwable) {
            Log.e(TAG, "Failed to create PoseLandmarker", t)
            onError(t.message ?: "Could not start pose model")
        }
    }

    fun detect(imageProxy: ImageProxy) {
        val marker = landmarker
        if (marker == null || closed.get()) {
            imageProxy.close()
            return
        }
        try {
            val rotation = imageProxy.imageInfo.rotationDegrees
            val bitmap = imageProxy.toRgbaBitmap()
            val matrix = Matrix().apply {
                postRotate(rotation.toFloat())
            }
            val upright = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
            if (upright != bitmap) bitmap.recycle()
            val mpImage = BitmapImageBuilder(upright).build()
            marker.detectAsync(mpImage, SystemClock.uptimeMillis())
        } catch (t: Throwable) {
            Log.e(TAG, "detect failed", t)
        } finally {
            imageProxy.close()
        }
    }

    fun close() {
        if (closed.compareAndSet(false, true)) {
            landmarker?.close()
            landmarker = null
            executor.shutdown()
        }
    }

    companion object {
        private const val TAG = "PoseDetector"
        const val MODEL_ASSET = "pose_landmarker_lite.task"
    }
}

private fun landmarkVisibility(
    landmark: com.google.mediapipe.tasks.components.containers.NormalizedLandmark,
): Float {
    val raw = landmark.visibility()
    return when (raw) {
        is java.util.Optional<*> -> (raw.orElse(1f) as? Number)?.toFloat() ?: 1f
        is Number -> raw.toFloat()
        else -> 1f
    }
}

private fun PoseLandmarkerResult.toDomain(width: Int, height: Int): PoseFrame? {
    val pose = landmarks().firstOrNull() ?: return null
    val points = pose.map { lm ->
        Landmark(
            x = lm.x(),
            y = lm.y(),
            z = lm.z(),
            visibility = landmarkVisibility(lm),
        )
    }
    return PoseFrame(
        landmarks = points,
        imageWidth = width,
        imageHeight = height,
        timestampMs = timestampMs(),
    )
}

private fun ImageProxy.toRgbaBitmap(): Bitmap {
    val plane = planes[0]
    val buffer = plane.buffer
    buffer.rewind()
    val pixelStride = plane.pixelStride
    val rowStride = plane.rowStride
    val rowPadding = rowStride - pixelStride * width
    val paddedWidth = width + if (pixelStride == 0) 0 else rowPadding / pixelStride
    val padded = Bitmap.createBitmap(paddedWidth, height, Bitmap.Config.ARGB_8888)
    padded.copyPixelsFromBuffer(buffer)
    return if (paddedWidth == width) {
        padded
    } else {
        Bitmap.createBitmap(padded, 0, 0, width, height).also { padded.recycle() }
    }
}
