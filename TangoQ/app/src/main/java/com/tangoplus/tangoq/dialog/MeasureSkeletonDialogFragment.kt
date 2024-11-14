package com.tangoplus.tangoq.dialog

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.graphics.Point
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.fragment.app.DialogFragment
import com.tangoplus.tangoq.MeasureSkeletonActivity
import com.tangoplus.tangoq.R
import com.tangoplus.tangoq.adapter.CautionVPAdapter
import com.tangoplus.tangoq.databinding.FragmentMeasureSkeletonDialogBinding

class MeasureSkeletonDialogFragment : DialogFragment() {
    lateinit var binding : FragmentMeasureSkeletonDialogBinding
    private var isPose = false
    private var seq = 0
    companion object {
        const val ARG_MS_TYPE = "ms_alert"
        const val ARG_MS_SEQ = "ms_alert_seq"
        fun newInstance(isPose: Boolean, seq: Int = 0): MeasureSkeletonDialogFragment {
            val fragment = MeasureSkeletonDialogFragment()
            val args = Bundle()
            args.putBoolean(ARG_MS_TYPE, isPose)
            args.putInt(ARG_MS_SEQ, seq)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)

        // Dialog가 실제로 표시될 때 호출되는 Listener를 설정
        dialog.setOnShowListener {
            // PoseLandmarker를 일시 중지하는 코드 삽입
            (activity as? MeasureSkeletonActivity)?.pausePoseLandmarker()
        }

        return dialog
    }

    override fun onStart() {
        super.onStart()

        dialog?.setOnDismissListener {
            (activity as? MeasureSkeletonActivity)?.resumePoseLandmarker()
        }
    }
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentMeasureSkeletonDialogBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        isPose = arguments?.getBoolean(ARG_MS_TYPE) ?: false
        seq = arguments?.getInt(ARG_MS_SEQ) ?: 0
        val layouts = when (isPose) {
            true -> listOf(R.layout.measure_skeleton_caution1)
            false -> listOf(
                R.layout.measure_skeleton_caution1,
                R.layout.measure_skeleton_caution2,
                R.layout.measure_skeleton_caution3,)
        }
        binding.vpMSD.adapter = CautionVPAdapter(layouts, isPose, seq)
        binding.btnMSDConfirm.setOnClickListener { dismiss() }
        binding.ibtnMSDExit.setOnClickListener { dismiss() }
        setUI(isPose)

    }
    private fun setUI(isPose: Boolean) {
        when (isPose) {
            true -> {
                binding.btnMSDConfirm.text = "확인했습니다"
            }
            false -> {
                binding.btnMSDConfirm.text = "모두 이해했습니다"
            }
        }
    }
    @Deprecated("Deprecated in Java")
    @SuppressLint("UseCompatLoadingForDrawables")
    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        dialog?.window?.setDimAmount(0.9f)
        dialog?.window?.setBackgroundDrawable(resources.getDrawable(R.drawable.bckgnd_rectangle_20))
        dialogFragmentResize(0.9f, 0.85f)
    }
    private fun dialogFragmentResize(width: Float, height: Float) {
        val windowManager = context?.getSystemService(Context.WINDOW_SERVICE) as WindowManager

        if (Build.VERSION.SDK_INT < 30) {
            val display = windowManager.defaultDisplay
            val size = Point()

            display.getSize(size)

            val window = dialog?.window

            val x = (size.x * width).toInt()
            val y = (size.y * height).toInt()
            window?.setLayout(x, y)
        } else {
            val rect = windowManager.currentWindowMetrics.bounds

            val window = dialog?.window

            val x = (rect.width() * width).toInt()
            val y = (rect.height() * height).toInt()

            window?.setLayout(x, y)
        }
    }
}