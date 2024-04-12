package com.example.mhg.Dialog

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.Dialog
import android.content.ContentValues.TAG
import android.content.Context
import android.graphics.Point
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.fragment.app.DialogFragment
import androidx.paging.LOGGER
import com.example.mhg.R
import com.example.mhg.databinding.FragmentAgreementDialogBinding
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader

class AgreementDialogFragment : DialogFragment() {
    companion object {
        private const val ARG_AGREEMENT_TYPE = "agreement_type"

        fun newInstance(agreementType: String): AgreementDialogFragment {
            val args = Bundle()
            args.putString(ARG_AGREEMENT_TYPE, agreementType)

            val fragment = AgreementDialogFragment()
            fragment.arguments = args
            return fragment
        }
    }
    lateinit var binding: FragmentAgreementDialogBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentAgreementDialogBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.w(TAG, "onViewCreated called")
//        binding.btnAgreement.setOnClickListener {
//            Log.w("$TAG, 약관창", "Button Clicked")
//            dismiss()
//        }
    }

    private fun readAgreementFromFile(fileResId: Int): String {
        // 파일에서 약관을 읽어오는 코드
        try {
            val inputStream = resources.openRawResource(fileResId)
            val reader = BufferedReader(InputStreamReader(inputStream))
            val stringBuilder = StringBuilder()
            var line: String? = reader.readLine()
            while (line != null) {
                stringBuilder.append(line)
                stringBuilder.append('\n')
                line = reader.readLine()
            }
            reader.close()
            inputStream.close()
            val termsAndConditions = stringBuilder.toString()

            return termsAndConditions
        } catch (e: IOException) {
            return ""
        }
    }
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {

        val agreementType = arguments?.getString(ARG_AGREEMENT_TYPE)
        val agreementText = when (agreementType) {
            "agreement1" -> { readAgreementFromFile(R.raw.agreement1) }
            "agreement2" -> readAgreementFromFile(R.raw.agreement2)
            else -> ""
        }
        val builder = AlertDialog.Builder(requireContext())

        binding = FragmentAgreementDialogBinding.inflate(layoutInflater)
        builder.setView(binding.root)

        binding.tvAgreement.text = agreementText

        return builder.create()
    }


    @SuppressLint("UseCompatLoadingForDrawables")
    @Deprecated("Deprecated in Java")
    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        dialog?.window?.setDimAmount(0.6f)
        dialog?.window?.setBackgroundDrawable(resources.getDrawable(R.drawable.dialog_16))
        val agreementType = arguments?.getString(ARG_AGREEMENT_TYPE)
        if (agreementType == "agreement1") {
            requireContext().dialogFragmentResize(0.9f, 0.8f)
        }
    }
    private fun Context.dialogFragmentResize(width: Float, height: Float) {
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