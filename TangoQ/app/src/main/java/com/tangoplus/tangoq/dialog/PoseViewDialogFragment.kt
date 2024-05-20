package com.tangoplus.tangoq.dialog

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.fragment.app.DialogFragment
import com.tangoplus.tangoq.R
import com.tangoplus.tangoq.databinding.FragmentPoseViewDialogBinding


class PoseViewDialogFragment : DialogFragment() {
    lateinit var binding : FragmentPoseViewDialogBinding
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentPoseViewDialogBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.ibtnPVExit.setOnClickListener {
            dismiss()
        }
    }

    override fun onResume() {
        super.onResume()
        // full Screen code
//        dialog?.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
//        dialog?.window?.setDimAmount(0.4f)
//        dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
//        dialog?.window?.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
        val darkTransparentBlack = Color.argb((255 * 0.6).toInt(), 0, 0, 0)
        dialog?.window?.setBackgroundDrawable(ColorDrawable(darkTransparentBlack))
        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        dialog?.window?.setDimAmount(0.4f)
    }




}