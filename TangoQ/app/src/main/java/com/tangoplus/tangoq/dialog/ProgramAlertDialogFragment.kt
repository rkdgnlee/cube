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
import androidx.fragment.app.activityViewModels
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.tangoplus.tangoq.R
import com.tangoplus.tangoq.databinding.FragmentProgramAlertDialogBinding
import com.tangoplus.tangoq.fragment.ExtendedFunctions.dialogFragmentResize
import com.tangoplus.tangoq.mediapipe.MathHelpers.isTablet
import com.tangoplus.tangoq.viewmodel.ProgressViewModel


class ProgramAlertDialogFragment : DialogFragment() {
    lateinit var binding : FragmentProgramAlertDialogBinding
    private lateinit var parentDialog: ProgramCustomDialogFragment
    private var case = 0
    private var alertMessage = ""
    private val pvm : ProgressViewModel by activityViewModels()
    companion object {
        const val ALERT_KEY_CASE = "a"
        fun newInstance (parentDialog : ProgramCustomDialogFragment, case: Int) : ProgramAlertDialogFragment {
            val fragment = ProgramAlertDialogFragment()
            fragment.parentDialog = parentDialog
            val args = Bundle()
            args.putInt(ALERT_KEY_CASE, case)
            fragment.arguments = args
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

        case = arguments?.getInt(ALERT_KEY_CASE) ?: 0

        when (case) {
            2 -> {
                alertMessage = "프로그램이 완료되었습니다.\n정확한 운동 추천을 위하여\n키오스크, 모바일 앱으로 측정을 진행한 후\n운동 프로그램을 다시 진행해 주시기 바랍니다."
                val spannableString = SpannableString(alertMessage)
                val accentIndex = spannableString.indexOf("키오스크, 모바일 앱")
                val colorSpan = ForegroundColorSpan(ContextCompat.getColor(requireContext(), R.color.mainColor))
                spannableString.setSpan(colorSpan, accentIndex, accentIndex + 11, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                binding.tvPAD.text = spannableString
                binding.ivPAD.setImageDrawable(ContextCompat.getDrawable(requireContext(), R.drawable.drawable_finish_program))
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
                binding.btnPAD1.visibility = View.GONE
                binding.btnPAD2.visibility = View.VISIBLE
            }
            1 -> {
                binding.btnPAD2.text = "확인"
                binding.ivPAD.setImageDrawable(ContextCompat.getDrawable(requireContext(), R.drawable.drawable_finish_sequence))
                alertMessage = "오늘 필요한 운동을 다 마쳤습니다.\n개인 운동을 진행 하거나 휴식을 취해주세요."
                binding.tvPAD.text = alertMessage
                binding.btnPAD1.visibility = View.GONE
                binding.btnPAD2.visibility = View.VISIBLE
                binding.btnPAD2.setOnClickListener {
                    dismiss()
                    parentDialog.dismissThisFragment()
                }
            }
            0 -> {

            }
        }
        binding.tvPAD.textSize = if (isTablet(requireContext())) 22f else 16f
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