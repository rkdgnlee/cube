package com.tangoplus.tangoq.dialog

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Point
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.fragment.app.DialogFragment
import com.tangoplus.tangoq.R
import com.tangoplus.tangoq.adapter.CautionVPAdapter
import com.tangoplus.tangoq.databinding.FragmentMeasureSkeletonDialogBinding

class MeasureSkeletonDialogFragment : DialogFragment() {
   lateinit var binding : FragmentMeasureSkeletonDialogBinding
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentMeasureSkeletonDialogBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val layouts = listOf(
            R.layout.measure_skeleton_caution1,
            R.layout.measure_skeleton_caution2,
            R.layout.measure_skeleton_caution3,
        )
        binding.vpMSD.adapter = CautionVPAdapter(layouts)

        binding.btnMSDConfirm.setOnClickListener { dismiss() }
        binding.ibtnMSDExit.setOnClickListener { dismiss() }


    }

    @Deprecated("Deprecated in Java")
    @SuppressLint("UseCompatLoadingForDrawables")
    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        dialog?.window?.setDimAmount(0.9f)
        dialog?.window?.setBackgroundDrawable(resources.getDrawable(R.drawable.background_rectangle_20dp))
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