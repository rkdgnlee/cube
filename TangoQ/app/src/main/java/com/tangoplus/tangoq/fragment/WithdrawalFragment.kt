package com.tangoplus.tangoq.fragment

import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Typeface
import android.os.Bundle
import android.text.Editable
import android.text.Html.FROM_HTML_MODE_LEGACY
import android.text.Spannable
import android.text.SpannableString
import android.text.SpannedString
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
import androidx.core.text.HtmlCompat
import androidx.core.widget.addTextChangedListener
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.tangoplus.tangoq.IntroActivity
import com.tangoplus.tangoq.MainActivity
import com.tangoplus.tangoq.R
import com.tangoplus.tangoq.databinding.FragmentWithdrawalBinding
import com.tangoplus.tangoq.`object`.NetworkUserService.fetchUserDeleteJson
import com.tangoplus.tangoq.`object`.Singleton_t_measure
import com.tangoplus.tangoq.`object`.Singleton_t_user


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


        // ------! 화면 확장 및 세팅 & 싱글턴 초기화 시작 !------
        (activity as MainActivity).setFullLayout(requireActivity().findViewById(R.id.flMain), requireActivity().findViewById(R.id.clMain))
        binding.btnWd.isEnabled = false
        singletonUserInstance = Singleton_t_user.getInstance(requireContext())
        singletonMeasureInstance = Singleton_t_measure.getInstance(requireContext())
//            .jsonObject?.optJSONObject("data")
        // ------! 화면 확장 및 세팅 & 싱글턴 초기화 끝 !------



        // ------! 뒤로가기 !------
        binding.ibtnWd.setOnClickListener {
            requireActivity().supportFragmentManager.beginTransaction().apply {
                setCustomAnimations(R.anim.slide_in_left, R.anim.slide_in_right)
                replace(R.id.flMain, ProfileFragment())
                addToBackStack(null)
                commit()
            }
        }


        // ------! spinner !------
        val withDrawalList = listOf("선택해주세요" ,"앱을 너무 많이 사용해요", "원하는 서비스가 없어요", "서비스가 어려워요","개인 정보를 너무 많이 사용하는 것 같아요","단순 변심이에요", "기타(개인 사정)")
        val adapter = ArrayAdapter(requireActivity(), android.R.layout.simple_dropdown_item_1line, withDrawalList)
        binding.actvWd.setAdapter(adapter)
        binding.actvWd.setText(withDrawalList.firstOrNull(), false)
        binding.actvWd.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                Log.v("edit", "$s")
            }
        })


        binding.cbWd.setOnCheckedChangeListener{compoundButton, isChecked ->
            setBtnUI(isChecked)
        }

        // ------! 회원 탈퇴 시작 !------
        val message = SpannableString("회원 탈퇴 시 철회할 수 없습니다.\n확인을 누르면 회원 계정이 삭제됩니다.\n삭제하시겠습니까?")
        val startIndex = message.indexOf("회원 계정이 삭제")
        val endIndex = message.indexOf("회원 계정이 삭제")
        message.setSpan(ForegroundColorSpan(ContextCompat.getColor(requireContext(), R.color.deleteColor)), startIndex, endIndex, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        message.setSpan(ForegroundColorSpan(ContextCompat.getColor(requireContext(), R.color.deleteColor)), 47, 49, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        message.setSpan(StyleSpan(Typeface.BOLD), 0, 3, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        binding.btnWd.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext() , R.style.ThemeOverlay_App_MaterialAlertDialog).apply {
                setTitle("경고⚠️")
                setMessage(message)
                setPositiveButton("예") { dialog, _ ->
                    deleteAccount()
                }
                setNegativeButton("아니오") { dialog, _ ->
                }
                create()
            }.show()
        }
    }
    private fun deleteAccount() {
        fetchUserDeleteJson(getString(R.string.IP_ADDRESS_t_user),
            singletonUserInstance.jsonObject?.optJSONObject("data")?.optString("user_mobile").toString()
        ) {
            MaterialAlertDialogBuilder(requireContext() , R.style.ThemeOverlay_App_MaterialAlertDialog).apply {
                setMessage("탈퇴가 완료됐습니다.")
                setPositiveButton("예") { dialog, _ ->
                    val intent = Intent(requireContext(), IntroActivity::class.java)
                    singletonUserInstance.jsonObject = null
                    singletonMeasureInstance.jsonObject = null
                    startActivity(intent)
                    requireActivity().finishAffinity()
                }
                create()
            }.show()
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
    override fun onDestroy() {
        super.onDestroy()
        (activity as? MainActivity)?.setOptiLayout(requireActivity().findViewById(R.id.flMain), requireActivity().findViewById(R.id.clMain), requireActivity().findViewById(R.id.cvCl))
    }

    override fun onPause() {
        super.onPause()
        (activity as? MainActivity)?.setOptiLayout(requireActivity().findViewById(R.id.flMain), requireActivity().findViewById(R.id.clMain), requireActivity().findViewById(R.id.cvCl))
    }
}