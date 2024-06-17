package com.tangoplus.tangoq.dialog

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.fragment.app.Fragment
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
            R.layout.measure_skeleton_caution3
        )
        binding.vpMSD.adapter = CautionVPAdapter(layouts)

        binding.tvMSDConfirm.setOnClickListener {
            dismiss()
        }


    }

    @SuppressLint("UseCompatLoadingForDrawables")
    @Deprecated("Deprecated in Java")
    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        dialog?.window?.setDimAmount(0.6f)
        dialog?.window?.setBackgroundDrawable(resources.getDrawable(R.drawable.background_dialog))
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val dialog = dialog
        if (dialog != null) {
            val width = (resources.displayMetrics.widthPixels * 0.6).toInt()
            val height = (resources.displayMetrics.heightPixels * 0.4).toInt()
            dialog.window?.setLayout(width, height)
            dialog.window?.addFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
        }
    }
}