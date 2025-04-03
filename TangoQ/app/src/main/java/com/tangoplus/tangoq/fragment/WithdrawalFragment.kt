package com.tangoplus.tangoq.fragment

import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Typeface
import android.os.Bundle
import android.text.Editable
import android.text.Spannable
import android.text.SpannableString
import android.text.TextWatcher
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.core.content.ContextCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.tangoplus.tangoq.IntroActivity
import com.tangoplus.tangoq.MainActivity
import com.tangoplus.tangoq.R
import com.tangoplus.tangoq.databinding.FragmentWithdrawalBinding
import com.tangoplus.tangoq.api.NetworkUser.fetchUserDeleteJson
import com.tangoplus.tangoq.db.Singleton_t_measure
import com.tangoplus.tangoq.db.Singleton_t_user
import com.tangoplus.tangoq.fragment.ExtendedFunctions.setOnSingleClickListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


class WithdrawalFragment : Fragment() {
    lateinit var binding : FragmentWithdrawalBinding
    private lateinit var singletonUserInstance: Singleton_t_user
    private lateinit var singletonMeasureInstance: Singleton_t_measure

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentWithdrawalBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        // ------# 화면 확장 및 세팅 & 싱글턴 초기화 #------
        binding.btnWd.isEnabled = false
        singletonUserInstance = Singleton_t_user.getInstance(requireContext())
        singletonMeasureInstance = Singleton_t_measure.getInstance(requireContext())

        // ------! spinner !------
        val withdrawalList = listOf("선택해주세요" ,"앱을 너무 많이 사용해요", "원하는 서비스가 없어요", "서비스가 어려워요","개인 정보를 너무 많이 사용하는 것 같아요","단순 변심이에요", "기타(개인 사정)")
        val adapter = ArrayAdapter(requireActivity(), android.R.layout.simple_dropdown_item_1line, withdrawalList)
        binding.actvW.setAdapter(adapter)
        binding.actvW.setText(withdrawalList.firstOrNull(), false)
        binding.actvW.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                Log.v("edit", "$s")
            }
        })

        binding.cbW.setOnCheckedChangeListener{ _, isChecked ->
            setBtnUI(isChecked)
        }

        // ------! 회원 탈퇴 시작 !------
        val message = SpannableString("회원 탈퇴 시 철회할 수 없습니다.\n확인을 누르면 회원 계정이 삭제됩니다.\n삭제하시겠습니까?")
        val startIndex = message.indexOf("회원 계정")
        val endIndex = message.indexOf("회원 계정")
        message.setSpan(ForegroundColorSpan(ContextCompat.getColor(requireContext(), R.color.deleteColor)), startIndex, endIndex, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        message.setSpan(ForegroundColorSpan(ContextCompat.getColor(requireContext(), R.color.deleteColor)), 42, 44, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        message.setSpan(StyleSpan(Typeface.BOLD), 0, 5, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        binding.btnWd.setOnSingleClickListener {
            MaterialAlertDialogBuilder(requireContext() , R.style.ThemeOverlay_App_MaterialAlertDialog).apply {
                setTitle("경고⚠️")
                setMessage(message)
                setPositiveButton("예") { dialog, _ ->
                    dialog.dismiss()
                    deleteAccount()
                }
                setNegativeButton("아니오") { _, _ ->
                }
                create()
            }.show()
        }
    }
    private fun deleteAccount() {
        CoroutineScope(Dispatchers.IO).launch {
            val responseCode = fetchUserDeleteJson(requireContext(), getString(R.string.API_user),
                singletonUserInstance.jsonObject?.optInt("sn").toString()
            )
            if (responseCode == 200) {
                requireActivity().runOnUiThread {
                    MaterialAlertDialogBuilder(requireContext() , R.style.ThemeOverlay_App_MaterialAlertDialog).apply {
                        setMessage("탈퇴가 완료됐습니다. 이용해 주셔서 감사합니다.")
                        setPositiveButton("예") { _, _ ->
                            val intent = Intent(requireContext(), IntroActivity::class.java)
                            singletonUserInstance.jsonObject = null
                            singletonMeasureInstance.measures = null
                            startActivity(intent)
                            requireActivity().finishAffinity()
                        }
                        create()
                    }.show()
                }
            }
        }
    }
    private fun setBtnUI(isChecked: Boolean) {
        val states = arrayOf(
            intArrayOf(android.R.attr.state_enabled),  // enabled
            intArrayOf(-android.R.attr.state_enabled)  // disabled
        )

        val colors = if (isChecked) {
            intArrayOf(
                ContextCompat.getColor(requireContext(), R.color.subColor700),  // enabled color
                ContextCompat.getColor(requireContext(), R.color.subColor150)   // disabled color
            )
        } else {
            intArrayOf(
                ContextCompat.getColor(requireContext(), R.color.subColor150),  // enabled color
                ContextCompat.getColor(requireContext(), R.color.subColor150)   // disabled color
            )
        }

        val colorStateList = ColorStateList(states, colors)
        binding.btnWd.backgroundTintList = colorStateList
        binding.btnWd.isEnabled = isChecked
    }
}