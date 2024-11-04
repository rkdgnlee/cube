package com.tangoplus.tangoq.dialog

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Point
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.view.WindowManager
import androidx.core.net.toUri
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.davemorrissey.labs.subscaleview.ImageSource
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView
import com.tangoplus.tangoq.R
import com.tangoplus.tangoq.data.MeasureViewModel
import com.tangoplus.tangoq.databinding.FragmentMainPartPoseDialogBinding
import com.tangoplus.tangoq.mediapipe.ImageProcessingUtil
import com.tangoplus.tangoq.mediapipe.ImageProcessingUtil.cropToPortraitRatio
import com.tangoplus.tangoq.mediapipe.PoseLandmarkResult.Companion.fromCoordinates
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import org.json.JSONObject
import java.io.File
import kotlin.coroutines.resume

class MainPartPoseDialogFragment : DialogFragment() {
    private lateinit var binding : FragmentMainPartPoseDialogBinding
    private val mvm : MeasureViewModel by activityViewModels()
    private var count = false

    companion object {
        private const val ARG_SEQ = "arg_seq"
        fun newInstance(seq: Int): MainPartPoseDialogFragment {
            val fragment = MainPartPoseDialogFragment()
            val args = Bundle()
            args.putInt(ARG_SEQ, seq)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentMainPartPoseDialogBinding.inflate(inflater)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setStyle(STYLE_NO_TITLE, R.style.DialogTheme)
        val seq = arguments?.getInt(ARG_SEQ)
        if (seq != null) {
            lifecycleScope.launch {
                setImage(seq, binding.ssivMPPD)
                binding.ssivMPPD.viewTreeObserver.addOnGlobalLayoutListener (object : ViewTreeObserver.OnGlobalLayoutListener{
                    override fun onGlobalLayout() {
                        binding.ssivMPPD.viewTreeObserver.removeOnGlobalLayoutListener(this)
                    }
                })
            }
        }
    }

    override fun onResume() {
        super.onResume()
//        isCancelable = false
        dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog?.window?.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
        dialog?.window?.setDimAmount(0.6f) // 원하는 만큼의 어둠 설정
    }


    private suspend fun setImage(seq: Int, ssiv: SubsamplingScaleImageView): Boolean = suspendCancellableCoroutine { continuation ->
        try {
            val jsonData = mvm.selectedMeasure?.measureResult?.optJSONObject(seq)
            val coordinates = extractImageCoordinates(jsonData!!)
            val imageUrls = mvm.selectedMeasure?.fileUris?.get(seq)
            if (imageUrls != null) {
                val imageFile = imageUrls.let { File(it) }
                val bitmap = BitmapFactory.decodeFile(imageUrls)
                lifecycleScope.launch(Dispatchers.Main) {

                    ssiv.setImage(ImageSource.uri(imageFile.toUri().toString()))

                    ssiv.setOnImageEventListener(object : SubsamplingScaleImageView.OnImageEventListener {
                        override fun onReady() {
                            if (!count) {
                                val imageViewWidth = ssiv.width
                                val imageViewHeight = ssiv.height
                                // iv에 들어간 image의 크기 같음 screenWidth
                                val sWidth = ssiv.sWidth
                                val sHeight = ssiv.sHeight
                                // 스케일 비율 계산
                                val scaleFactorX = imageViewHeight / sHeight.toFloat()
                                val scaleFactorY =  imageViewHeight / sHeight.toFloat()
                                // 오프셋 계산 (뷰 크기 대비 이미지 크기의 여백)
                                val offsetX = (imageViewWidth - sWidth * scaleFactorX) / 2f
                                val offsetY = (imageViewHeight - sHeight * scaleFactorY) / 2f
                                val poseLandmarkResult = fromCoordinates(coordinates!!)
                                val combinedBitmap = ImageProcessingUtil.combineImageAndOverlay(
                                    bitmap,
                                    poseLandmarkResult,
                                    scaleFactorX,
                                    scaleFactorY,
                                    offsetX,
                                    offsetY,

                                    seq
                                )
                                count = true
                                // 실패
                                when (seq) {
                                    0, 2, 3, 4 -> ssiv.setImage(
                                        ImageSource.bitmap(
                                            cropToPortraitRatio(combinedBitmap)
                                        ))
                                    else -> ssiv.setImage(ImageSource.bitmap(combinedBitmap))
                                }
                                continuation.resume(true)

                            }
                        }
                        override fun onImageLoaded() { count = false }

                        override fun onPreviewLoadError(e: Exception?) { continuation.resume(false) }
                        override fun onImageLoadError(e: Exception?) { continuation.resume(false) }
                        override fun onTileLoadError(e: Exception?) { continuation.resume(false) }
                        override fun onPreviewReleased() { continuation.resume(false) }
                    })
                }
            } else { continuation.resume(false) }
        } catch (e: IndexOutOfBoundsException) {
            Log.e("Error", "${e}")
        }
    }

    private fun extractImageCoordinates(jsonData: JSONObject): List<Pair<Float, Float>>? {
        val poseData = jsonData.optJSONArray("pose_landmark")
        return if (poseData != null) {
            List(poseData.length()) { i ->
                val landmark = poseData.getJSONObject(i)
                Pair(
                    landmark.getDouble("sx").toFloat(),
                    landmark.getDouble("sy").toFloat()
                )
            }
        } else null
    }
}