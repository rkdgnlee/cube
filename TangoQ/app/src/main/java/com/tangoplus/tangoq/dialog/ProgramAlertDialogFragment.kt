package com.tangoplus.tangoq.dialog

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Point
import android.os.Build
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.tangoplus.tangoq.R
import com.tangoplus.tangoq.databinding.FragmentProgramAlertDialogBinding


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


        binding.btnPAD1.setOnClickListener {
            dismiss()
            parentDialog.dismissThisFragment()
        }

        binding.btnPAD2.setOnClickListener {
            dismiss()
            parentDialog.dismissThisFragment()

            val bnb = requireActivity().findViewById<BottomNavigationView>(R.id.bnbMain)
            bnb.selectedItemId = R.id.measure
        }

        val spannableString = SpannableString(binding.tvPAD2.text)
        val colorSpan = ForegroundColorSpan(ContextCompat.getColor(requireContext(), R.color.mainColor))
        spannableString.setSpan(colorSpan, 0, 11, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        binding.tvPAD2.text = spannableString

    }


    @SuppressLint("UseCompatLoadingForDrawables")
    @Deprecated("Deprecated in Java")
    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        dialog?.window?.setDimAmount(0.7f)
        dialog?.window?.setBackgroundDrawable(resources.getDrawable(R.drawable.bckgnd_rectangle_20, null))
        dialog?.setCancelable(false)
        dialogFragmentResize()

    }
    private fun dialogFragmentResize() {
        val windowManager = context?.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val width = 0.8f
        val height = 0.5f
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

            val x = (rect.width() *  width).toInt()
            val y = (rect.height() * height).toInt()

            window?.setLayout(x, y)
        }
    }
}