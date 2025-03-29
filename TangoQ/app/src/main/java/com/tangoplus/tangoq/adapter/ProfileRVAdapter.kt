package com.tangoplus.tangoq.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.materialswitch.MaterialSwitch
import com.tangoplus.tangoq.dialog.AgreementDetailDialogFragment
import com.tangoplus.tangoq.dialog.ProfileEditDialogFragment
import com.tangoplus.tangoq.listener.BooleanClickListener
import com.tangoplus.tangoq.R
import com.tangoplus.tangoq.viewmodel.SignInViewModel
import com.tangoplus.tangoq.databinding.RvProfileItemBinding
import com.tangoplus.tangoq.databinding.RvProfileSpecialItemBinding
import com.tangoplus.tangoq.db.Singleton_t_user
import com.tangoplus.tangoq.dialog.InputDialogFragment
import com.tangoplus.tangoq.dialog.PinChangeDialogFragment
import com.tangoplus.tangoq.dialog.QRCodeDialogFragment
import com.tangoplus.tangoq.dialog.ProfileEditChangeDialogFragment
import com.tangoplus.tangoq.fragment.ExtendedFunctions.isKorean
import com.tangoplus.tangoq.fragment.ExtendedFunctions.setOnSingleClickListener
import com.tangoplus.tangoq.fragment.ProfileFragment
import com.tangoplus.tangoq.fragment.WithdrawalFragment
import com.tangoplus.tangoq.function.SecurePreferencesManager.logout
import org.json.JSONObject
import java.lang.IllegalArgumentException

class ProfileRVAdapter(private val fragment: Fragment,
                       private val booleanClickListener: BooleanClickListener,
                       val first: Boolean,
                       val case: String,
                       private val vm: ViewModel
) : RecyclerView.Adapter<RecyclerView.ViewHolder> ()  {
    var profileMenuList = mutableListOf<String>()

    private val viewTypeNormal = 0
    private val viewTypeSpecial = 1
    var userJson = JSONObject()
    inner class ViewHolder(view : View) : RecyclerView.ViewHolder(view) {
        val tvPfSettingsName : TextView = view.findViewById(R.id.tvPfSettingsName)
        val tvPfInfo: TextView = view.findViewById(R.id.tvPfInfo)
        val ivPf : ImageView = view.findViewById(R.id.ivPf)
        val cltvPfSettings : ConstraintLayout = view.findViewById(R.id.cltvPfSettings)
    }
    inner class SpecialItemViewHolder(view : View) : RecyclerView.ViewHolder(view) {
        @SuppressLint("UseSwitchCompatOrMaterialCode")
        val schPfS: MaterialSwitch = view.findViewById(R.id.schPfS)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            viewTypeNormal -> {
                val inflater = LayoutInflater.from(parent.context)
                val binding = RvProfileItemBinding.inflate(inflater, parent, false)
                ViewHolder(binding.root)
            }
            viewTypeSpecial -> {
                val inflater = LayoutInflater.from(parent.context)
                val binding = RvProfileSpecialItemBinding.inflate(inflater, parent, false)
                SpecialItemViewHolder(binding.root)
            }
            else -> throw IllegalArgumentException("Invaild view Type")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val currentItem = profileMenuList[position]
        when (holder.itemViewType) {
            viewTypeNormal -> {
                val ViewHolder = holder as ViewHolder
                when (case) {
                    "profile" -> {
                        when (currentItem) {
                            "내정보" -> holder.ivPf.setImageResource(R.drawable.icon_profile)

                            "QR코드 핀번호 로그인" -> holder.ivPf.setImageResource(R.drawable.icon_qr_code)
                            "키오스크 핀번호 재설정" -> holder.ivPf.setImageResource(R.drawable.icon_security)
                            "푸쉬 알림 설정" -> holder.ivPf.setImageResource(R.drawable.icon_alarm_small)
                            "문의하기" -> holder.ivPf.setImageResource(R.drawable.icon_inquire)
                            "공지사항" -> holder.ivPf.setImageResource(R.drawable.icon_announcement)
                            "앱 버전" -> holder.ivPf.setImageResource(R.drawable.icon_copy)
                            "개인정보 처리방침" -> holder.ivPf.setImageResource(R.drawable.icon_paper)
                            "서비스 이용약관" -> holder.ivPf.setImageResource(R.drawable.icon_paper)
                            "로그아웃", "회원탈퇴" -> holder.ivPf.setImageResource(R.drawable.icon_logout)
                        }
                        // ------! 앱 버전 text 설정 시작 !------
                        ViewHolder.tvPfSettingsName.text = currentItem
                        if (ViewHolder.tvPfSettingsName.text == "앱 버전") {
                            val packageInfo = fragment.requireContext().packageManager.getPackageInfo(fragment.requireContext().packageName, 0)
                            val versionName =  "v" + packageInfo.versionName
                            ViewHolder.tvPfInfo.text = versionName
                        } else
                            ViewHolder.tvPfInfo.text = ""
                        // ------! 앱 버전 text 설정 끝 !------

                        // ------! 각 item 클릭 동작 시작 !------
                        ViewHolder.cltvPfSettings.setOnSingleClickListener {
                            when (currentItem) {
                                "내정보" -> {
                                    val dialogFragment = ProfileEditDialogFragment()
                                    dialogFragment.setProfileUpdateListener(fragment as ProfileFragment)
                                    dialogFragment.show(fragment.requireActivity().supportFragmentManager, "ProfileEditDialogFragment")
                                }
                                "QR코드 핀번호 로그인" -> {
                                    val dialog = QRCodeDialogFragment()
                                    dialog.show(fragment.requireActivity().supportFragmentManager, "QRCodeDialogFragment")
                                }
                                "키오스크 핀번호 재설정" -> {
                                    val dialog = PinChangeDialogFragment()
                                    dialog.show(fragment.requireActivity().supportFragmentManager, "PinChangeDialogFragment")
                                }
//                                "연동 관리" -> {
//                                    val dialog = ConnectManageDialogFragment()
//                                    dialog.show(fragment.requireActivity().supportFragmentManager, "LinkManageDialogFragment")
//                                }
                                "푸쉬 알림 설정" -> {
                                    val intent = Intent().apply {
                                        action = "android.settings.APP_NOTIFICATION_SETTINGS"
                                        putExtra("android.provider.extra.APP_PACKAGE", fragment.requireContext().packageName)
                                    }
                                    fragment.startActivity(intent)
                                }
                                "자주 묻는 질문" -> {
                                    val intent = Intent(Intent.ACTION_VIEW)
                                    val url = Uri.parse("https://tangoplus.co.kr/ko/20")
                                    intent.setData(url)
                                    fragment.startActivity(intent)
                                }
                                "문의하기" -> {
                                    val intent = Intent(Intent.ACTION_VIEW)
                                    val url = Uri.parse("https://tangoplus.co.kr/ko/21")
                                    intent.setData(url)
                                    fragment.startActivity(intent)
                                }
                                "공지사항" -> {
                                    val intent = Intent(Intent.ACTION_VIEW)
                                    val url = Uri.parse("https://tangoplus.co.kr/ko/18")
                                    intent.setData(url)
                                    fragment.startActivity(intent)
                                }
                                "개인정보 처리방침" -> {
                                    val dialog = AgreementDetailDialogFragment.newInstance("agreement2")
                                    dialog.show(fragment.requireActivity().supportFragmentManager, "agreement_dialog")
                                }
                                "서비스 이용약관" -> {
                                    val dialog = AgreementDetailDialogFragment.newInstance("agreement1")
                                    dialog.show(fragment.requireActivity().supportFragmentManager, "agreement_dialog")
                                }
                                "로그아웃" -> {
                                    MaterialAlertDialogBuilder(fragment.requireContext(), R.style.ThemeOverlay_App_MaterialAlertDialog).apply {
                                        setTitle("로그아웃")
                                        setMessage("로그아웃 하시겠습니까?")
                                        setPositiveButton("예") { _, _ ->
                                            logout(fragment.requireActivity(), 0)
                                        }
                                        setNegativeButton("아니오") { _, _ -> }
                                    }.show()
                                }
                                "회원탈퇴" -> {
                                    val provider = Singleton_t_user.getInstance(fragment.requireContext()).jsonObject?.optString("provider") ?: ""
                                    Log.v("provider", provider)
                                    if (provider == "null" || provider == "") {
                                        val dialog = InputDialogFragment.newInstance(2)
                                        dialog.show(fragment.requireActivity().supportFragmentManager, "InputDialogFragment")
                                    } else {
                                        Toast.makeText(fragment.requireContext(), "소셜로그인으로 로그인한 계정입니다.", Toast.LENGTH_SHORT).show()
                                        fragment.requireActivity().supportFragmentManager.beginTransaction().apply {
                                            setCustomAnimations(R.anim.slide_in_left, R.anim.slide_in_right)
                                            replace(R.id.flMain, WithdrawalFragment())

                                            commit()
                                        }
                                    }

                                }
                            }
                        }
                    }
                    // ------! 각 item 클릭 동작 끝 !------

                    "profileEdit" -> {

                        holder.ivPf.setImageResource(R.drawable.icon_profile)
                        holder.tvPfSettingsName.text = currentItem
                        when (holder.tvPfSettingsName.text) {
                            "이름" -> {
                                val userName = userJson.optString("user_name")
//                                if (isKorean(userName)) holder.tvPfInfo.text = userName.replaceRange(userName.length - 1, userName.length, "*")
//                                else holder.tvPfInfo.text = maskedProfileData(userName)
                                holder.tvPfInfo.text = userName
                                holder.ivPf.setImageResource(R.drawable.icon_profile)
                                holder.tvPfSettingsName.setBackgroundColor(ContextCompat.getColor(fragment.requireContext(), R.color.subColor100))
                            }
                            "이메일" -> {
                                (vm as SignInViewModel).setEmail.observe(fragment.viewLifecycleOwner) { email ->
                                    val maskingEmail = maskedProfileData(email)
                                    holder.tvPfInfo.text = maskingEmail
                                }
                                holder.ivPf.setImageResource(R.drawable.icon_email)
                                holder.tvPfSettingsName.setBackgroundColor(ContextCompat.getColor(fragment.requireContext(), R.color.subColor100))
                            }
                            "비밀번호" -> {
                                holder.tvPfInfo.text = "************"
                                holder.ivPf.setImageResource(R.drawable.icon_security)
                            }
                            "전화번호" -> {
                                (vm as SignInViewModel).setMobile.observe(fragment.viewLifecycleOwner) { mobile ->
                                    holder.tvPfInfo.text =
                                        if (mobile.length > 9) {
                                        "010${maskedProfileData(mobile.toString())}"
                                    } else "미설정"
                                }
                                holder.ivPf.setImageResource(R.drawable.icon_phone)
                            }
                            "몸무게" -> {
                                (vm as SignInViewModel).setWeight.observe(fragment.viewLifecycleOwner) { weight ->
                                    holder.tvPfInfo.text = when (weight) {
                                        0 -> "미설정"
                                        else -> "$weight kg"
                                    }
                                }
                                holder.ivPf.setImageResource(R.drawable.icon_weight)
                            }

                            "신장" -> {
                                (vm as SignInViewModel).setHeight.observe(fragment.viewLifecycleOwner) { height ->
                                    holder.tvPfInfo.text = when (height) {
                                        0 -> "미설정"
                                        else -> "$height cm"
                                    }
                                }
                                holder.ivPf.setImageResource(R.drawable.icon_height)
                            }
                            "생년월일" -> {
                                (vm as SignInViewModel).setBirthday.observe(fragment.viewLifecycleOwner) { birthday ->
                                    Log.v("생년월일", "$birthday, ${birthday.length}")
                                    holder.tvPfInfo.text = if (birthday == "0" || birthday == "" || birthday == "null") "미설정" else birthday
                                }
                                if (holder.tvPfInfo.text != "미설정") holder.tvPfSettingsName.setBackgroundColor(ContextCompat.getColor(fragment.requireContext(), R.color.subColor100))
                                holder.ivPf.setImageResource(R.drawable.icon_cake)
                            }
                            "성별" -> {
                                (vm as SignInViewModel).setGender.observe(fragment.viewLifecycleOwner) { genderString ->
                                    Log.v("성별string", "$genderString, ${genderString.length}")
                                    if (genderString == null || genderString == "" || genderString == "null") {
                                        holder.tvPfInfo.text = "미설정"
                                    } else if (genderString == "여자") {
                                        holder.tvPfInfo.text = "여자"
                                        holder.tvPfSettingsName.setBackgroundColor(ContextCompat.getColor(fragment.requireContext(), R.color.subColor100))
                                    } else if (genderString == "남자") {
                                        holder.tvPfInfo.text = "남자"
                                        holder.tvPfSettingsName.setBackgroundColor(ContextCompat.getColor(fragment.requireContext(), R.color.subColor100))
                                    }
                                }
                                holder.ivPf.setImageResource(R.drawable.icon_gender)
                            }
                        }

                        holder.cltvPfSettings.setOnSingleClickListener {
                            when (holder.tvPfSettingsName.text) {
//                                "이메일" -> {
//                                    val dialog = ProfileEditChangeDialogFragment.newInstance("이메일", (vm as SignInViewModel).setEmail.value.toString())
//                                    dialog.show(fragment.requireActivity().supportFragmentManager, "ProfileEditBSDialogFragment")
//                                }
                                "비밀번호" -> {
                                    val provider = Singleton_t_user.getInstance(fragment.requireContext()).jsonObject?.optString("provider") ?: ""
                                    Log.v("provider", provider)
                                    if (provider == "null") {
                                        val dialog = InputDialogFragment.newInstance(1)
                                        dialog.show(fragment.requireActivity().supportFragmentManager, "InputDialogFragment")
                                    } else {
                                        Toast.makeText(fragment.requireContext(), "소셜로그인으로 로그인한 계정입니다.", Toast.LENGTH_SHORT).show()
                                    }
                                }
                                "전화번호" -> {
                                    val dialog = ProfileEditChangeDialogFragment.newInstance("전화번호", "")
                                    dialog.show(fragment.requireActivity().supportFragmentManager, "ProfileEditBSDialogFragment")
                                }
                                "몸무게" -> {
                                    val dialog = ProfileEditChangeDialogFragment.newInstance("몸무게", (vm as SignInViewModel).setWeight.value.toString())
                                    dialog.show(fragment.requireActivity().supportFragmentManager, "ProfileEditBSDialogFragment")
                                }
                                "신장" -> {
                                    val dialog = ProfileEditChangeDialogFragment.newInstance("신장", (vm as SignInViewModel).setHeight.value.toString())
                                    dialog.show(fragment.requireActivity().supportFragmentManager, "ProfileEditBSDialogFragment")
                                }
                                "생년월일" -> {
                                    if (holder.tvPfInfo.text == "미설정") {
                                        val dialog = ProfileEditChangeDialogFragment.newInstance("생년월일", (vm as SignInViewModel).setBirthday.value.toString())
                                        dialog.show(fragment.requireActivity().supportFragmentManager, "ProfileEditBSDialogFragment")
                                    }
                                }
                                "성별" -> {
                                    if (holder.tvPfInfo.text == "미설정") {
                                        val dialog = ProfileEditChangeDialogFragment.newInstance("성별", (vm as SignInViewModel).setBirthday.value.toString())
                                        dialog.show(fragment.requireActivity().supportFragmentManager, "ProfileEditBSDialogFragment")
                                    }
                                }
                            }
                        }
                    }
                }
            } // -----! 다크모드 시작 !-----
            viewTypeSpecial -> {
                val myViewHolder = holder as SpecialItemViewHolder
                when (currentItem) {
                    "다크 모드" -> {
                        val sharedPref = fragment.requireActivity().getSharedPreferences("deviceSettings", Context.MODE_PRIVATE)
                        val darkMode = sharedPref?.getBoolean("darkMode", false)
                        if (darkMode != null) {
                            myViewHolder.schPfS.isChecked = darkMode == true
                        }
                        myViewHolder.schPfS.setOnCheckedChangeListener{ _, isChecked ->
                            booleanClickListener.onSwitchChanged(isChecked)
                        }
                    }
                }

            } // -----! 다크모드 끝 !-----
        }
    }

    override fun getItemViewType(position: Int): Int {
        return if (position == 1 && first) {
            viewTypeSpecial
        } else {
            viewTypeNormal
        }
    }
    override fun getItemCount(): Int {
        return profileMenuList.size
    }

    private fun maskedProfileData(resultString: String) : String {
        val atIndex = resultString.indexOf('@')
        if (atIndex == -1) {
            val except010String = resultString.substring(3, resultString.length).replace("-", "")
            val maskedString = except010String.mapIndexed{ index, char ->
                when {
                    index % 6 == 0 || index % 6 == 2 || index % 6 == 3 -> char
                    index % 6 == 1 || index % 6 == 4 || index % 6 == 5 -> '*'
                    else -> char
                }
            }.joinToString("")

            return maskedString
        }  // @ 기호가 없으면 원본 그대로 반환

        val username = resultString.substring(0, atIndex)
        val domain = resultString.substring(atIndex)
        val maskedUsername = username.mapIndexed { index, char ->

            when {
                index % 6 == 0 || index % 6 == 2 || index % 6 == 3 -> char
                index % 6 == 1 || index % 6 == 4 || index % 6 == 5 -> '*'
                else -> char
            }
        }.joinToString("")

        val maskedDomain = domain.mapIndexed { index, char ->
            when {
                index % 6 == 0 || index % 6 == 2 || index % 6 == 3 -> char
                index % 6 == 1 || index % 6 == 4 || index % 6 == 5 -> '*'
                else -> char
            }
        }.joinToString("")

        return maskedUsername + maskedDomain
    }


}