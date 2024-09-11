package com.tangoplus.tangoq.dialog

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Point
import android.os.Build
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.fragment.app.DialogFragment
import com.tangoplus.tangoq.R
import com.tangoplus.tangoq.databinding.FragmentProgramAlertDialogBinding
import com.tangoplus.tangoq.dialog.AgreementDetailDialogFragment.Companion.ARG_AGREEMENT_TYPE


class ProgramAlertDialogFragment : DialogFragment() {
    lateinit var binding : FragmentProgramAlertDialogBinding
    private lateinit var parentDialog: ProgramCustomDialogFragment
    companion object {
        fun newInstance (parentDialog : ProgramCustomDialogFragment) : ProgramAlertDialogFragment {
            val fragment = ProgramAlertDialogFragment()
            fragment.parentDialog = parentDialog
            return fragment
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentProgramAlertDialogBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        binding.btnPAD.setOnClickListener {
            dismiss()
            parentDialog.dismissThisFragment()
        }
    }


    @SuppressLint("UseCompatLoadingForDrawables")
    @Deprecated("Deprecated in Java")
    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        dialog?.window?.setDimAmount(0.6f)
        dialog?.window?.setBackgroundDrawable(resources.getDrawable(R.drawable.background_rectangle_20dp, null))
        dialog?.setCancelable(false)
        val agreementType = arguments?.getString(ARG_AGREEMENT_TYPE)
        if (agreementType == "agreement1") {
            dialogFragmentResize(0.8f, 0.7f)
        } else {
            dialogFragmentResize(0.8f, 0.7f)
        }
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