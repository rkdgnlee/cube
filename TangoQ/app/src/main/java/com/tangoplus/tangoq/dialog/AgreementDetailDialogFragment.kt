package com.tangoplus.tangoq.dialog

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.text.util.Linkify
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import com.tangoplus.tangoq.R
import com.tangoplus.tangoq.databinding.FragmentAgreementDetailDialogBinding
import com.tangoplus.tangoq.db.Singleton_t_user
import com.tangoplus.tangoq.fragment.ExtendedFunctions.setOnSingleClickListener
import com.tangoplus.tangoq.vision.MathHelpers.isTablet
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.lang.ClassCastException
import java.time.LocalDate


class AgreementDetailDialogFragment : DialogFragment() {
    lateinit var binding: FragmentAgreementDetailDialogBinding
    private val titles = listOf("서비스 이용 약관", "개인정보 처리 방침", "마케팅 정보 수신 동의 약관", "개인정보 수집 활용 동의서", "개인정보 제3자 제공 동의서")

    companion object {
        const val ARG_AGREEMENT_TYPE = "agreement_type"

        fun newInstance(agreementType: String): AgreementDetailDialogFragment {
            val args = Bundle()
            args.putString(ARG_AGREEMENT_TYPE, agreementType)
            val fragment = AgreementDetailDialogFragment()
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentAgreementDetailDialogBinding.inflate(inflater)
        return binding.root
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
            Log.e("agreementDetail", "IO: ${e.message}")
            return ""
        } catch (e: ClassCastException) {
            Log.e("agreementDetail", "ClassCast: ${e.message}")
            return ""
        } catch (e: ClassNotFoundException) {
            Log.e("agreementDetail", "ClassNotFound: ${e.message}")
            return ""
        } catch (e: IllegalArgumentException) {
            Log.e("agreementDetail", "IllegalArgument: ${e.message}")
            return ""
        } catch (e: Exception) {
            Log.e("agreementDetail", "Exception: ${e.message}")
            return ""
        }
    }
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = AlertDialog.Builder(requireContext())
        binding = FragmentAgreementDetailDialogBinding.inflate(layoutInflater)
        builder.setView(binding.root)

        val agreementType = arguments?.getString(ARG_AGREEMENT_TYPE)
        var agreementText = when (agreementType) {
            "agreement1" -> readAgreementFromFile(R.raw.agreement1)
            "agreement2" -> readAgreementFromFile(R.raw.agreement2)
            "agreement3" -> readAgreementFromFile(R.raw.agreement3)
            "agreement4" -> readAgreementFromFile(R.raw.agreement4)
            "agreement5" -> readAgreementFromFile(R.raw.agreement5)
            "license" -> readAgreementFromFile(R.raw.license_report)
            else -> ""
        }

        if (agreementType in listOf("agreement1", "agreement2", "agreement3")) {
            binding.btnAgreement.text = "확인"
        }
        if (agreementType in listOf("agreement4", "agreement5")) {
            val userName = Singleton_t_user.getInstance(requireContext()).jsonObject?.optString("user_name") ?: ""
            val currentDate = "${LocalDate.now()}".replace("-", " .")

            agreementText += "\n${currentDate}\n이용자 성명 $userName"
        }
        if (agreementType == "license") {
            binding.btnAgreement.visibility = View.INVISIBLE
            binding.tvAgreement.autoLinkMask = Linkify.WEB_URLS
            binding.tvAgreement.movementMethod = LinkMovementMethod.getInstance()
            binding.tvAgreement.setTextIsSelectable(true)

        }
        binding.tvAgreementTitle.text = when (agreementType) {
            "agreement1" -> titles[0]
            "agreement2" -> titles[1]
            "agreement3" -> titles[2]
            "agreement4" -> titles[3]
            "agreement5" -> titles[4]
            "license" -> "Open Source License"
            else -> ""
        }

        val titleIndex = agreementText.indexOf("\n")
        agreementText = agreementText.substring(titleIndex + 1, agreementText.length)
        binding.tvAgreementTitle.textSize = if (isTablet(requireContext()) ) 24f else 20f

        binding.tvAgreement.text = agreementText
        binding.tvAgreement.textSize = if (isTablet(requireContext())) 19f else 16f

        binding.ibtnAgreement.setOnSingleClickListener { dismiss() }
        binding.btnAgreement.setOnSingleClickListener { dismiss() }
        return builder.create()
    }

    override fun onResume() {
        super.onResume()
        dialog?.window?.setBackgroundDrawable(resources.getDrawable(R.drawable.bckgnd_rectangle_20, null))
        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

}