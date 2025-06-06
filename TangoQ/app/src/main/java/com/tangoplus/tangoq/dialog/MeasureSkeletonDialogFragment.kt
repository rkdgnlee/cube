package com.tangoplus.tangoq.dialog

import android.app.Dialog
import android.content.Context
import android.graphics.Point
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.DialogFragment
import com.tangoplus.tangoq.R
import com.tangoplus.tangoq.adapter.etc.CautionVPAdapter
import com.tangoplus.tangoq.databinding.FragmentMeasureSkeletonDialogBinding
import com.tangoplus.tangoq.fragment.ExtendedFunctions.setOnSingleClickListener

class MeasureSkeletonDialogFragment : DialogFragment() {
    lateinit var binding : FragmentMeasureSkeletonDialogBinding
    private var isPose = false
    private var seq = 0
    private var isFront = 0
    companion object {
        const val ARG_MS_TYPE = "ms_alert"
        const val ARG_MS_SEQ = "ms_alert_seq"
        const val ARG_MS_ISFRONT = "ms_alert_isfront"
        fun newInstance(isPose: Boolean, seq: Int = 0, isFront: Int = 0): MeasureSkeletonDialogFragment {
            val fragment = MeasureSkeletonDialogFragment()
            val args = Bundle()
            args.putBoolean(ARG_MS_TYPE, isPose)
            args.putInt(ARG_MS_SEQ, seq)
            args.putInt(ARG_MS_ISFRONT, isFront)
            fragment.arguments = args
            return fragment
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.AppTheme_FlexableDialogFragment)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        return dialog
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
        // api35이상 화면 크기 조절
        ViewCompat.setOnApplyWindowInsetsListener(view) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            // 상태 표시줄 높이만큼 상단 패딩 적용
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        isPose = arguments?.getBoolean(ARG_MS_TYPE) ?: false
        seq = arguments?.getInt(ARG_MS_SEQ) ?: 0
        isFront = arguments?.getInt(ARG_MS_ISFRONT) ?: 0
        val layouts = when (isPose) {
            true -> listOf(R.layout.measure_skeleton_caution1)
            false -> listOf(
                R.layout.measure_skeleton_caution1,
                R.layout.measure_skeleton_caution2,
                R.layout.measure_skeleton_caution3,
                R.layout.measure_skeleton_caution4,
                R.layout.measure_skeleton_caution5,)
        }
        binding.vpMSD.adapter = CautionVPAdapter(requireContext(), layouts, isPose, seq, isFront)
        binding.vpMSD.currentItem = 0
        binding.btnMSDConfirm.setOnSingleClickListener {
//            Log.v("resumePoseLandmarker", "btnMSDConfirm")
            dismiss()
        }
        binding.ibtnMSDExit.setOnSingleClickListener {
//            Log.v("resumePoseLandmarker", "ibtnMSDExit")
            dismiss()
        }
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

    override fun onResume() {
        super.onResume()
        dialog?.window?.setDimAmount(0.9f)
        dialog?.window?.setBackgroundDrawable(ContextCompat.getDrawable(requireContext(), R.drawable.bckgnd_rectangle_20))
        dialogFragmentResize()
    }
    private fun dialogFragmentResize() {
        val windowManager = context?.getSystemService(Context.WINDOW_SERVICE) as WindowManager

        if (Build.VERSION.SDK_INT < 30) {
            val display = windowManager.defaultDisplay
            val size = Point()

            display.getSize(size)

            val window = dialog?.window

            val x = (size.x * 0.9f).toInt()
            val y = (size.y * 0.85f).toInt()
            window?.setLayout(x, y)
        } else {
            val rect = windowManager.currentWindowMetrics.bounds

            val window = dialog?.window

            val x = (rect.width()* 0.9f).toInt()
            val y = (rect.height() * 0.85f).toInt()

            window?.setLayout(x, y)
        }
    }
}