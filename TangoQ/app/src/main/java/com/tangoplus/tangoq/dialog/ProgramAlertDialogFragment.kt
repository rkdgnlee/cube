package com.tangoplus.tangoq.dialog

import android.annotation.SuppressLint
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.tangoplus.tangoq.R
import com.tangoplus.tangoq.databinding.FragmentProgramAlertDialogBinding
import com.tangoplus.tangoq.fragment.ExtendedFunctions.dialogFragmentResize
import com.tangoplus.tangoq.mediapipe.MathHelpers.isTablet


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

        binding.tvPAD1.textSize = if (isTablet(requireContext())) 22f else 16f
        binding.tvPAD2.textSize = if (isTablet(requireContext())) 22f else 16f
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    @Deprecated("Deprecated in Java")
    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        dialog?.window?.setDimAmount(0.7f)
        dialog?.window?.setBackgroundDrawable(resources.getDrawable(R.drawable.bckgnd_rectangle_20, null))
        dialog?.setCancelable(false)
        if (isTablet(requireContext())) {
            dialogFragmentResize(requireContext(), this@ProgramAlertDialogFragment, width =  0.6f ,height = 0.4f)
        } else {
            dialogFragmentResize(requireContext(), this@ProgramAlertDialogFragment, height = 0.475f)
        }
    }
}