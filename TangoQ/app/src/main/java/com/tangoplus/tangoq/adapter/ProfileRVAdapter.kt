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
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.ViewModel
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.kakao.sdk.auth.AuthApiClient
import com.kakao.sdk.user.UserApiClient
import com.navercorp.nid.NaverIdLoginSDK
import com.navercorp.nid.oauth.NidOAuthLoginState
import com.tangoplus.tangoq.dialog.AgreementDetailDialogFragment
import com.tangoplus.tangoq.dialog.ProfileEditDialogFragment
import com.tangoplus.tangoq.IntroActivity
import com.tangoplus.tangoq.listener.BooleanClickListener
import com.tangoplus.tangoq.R
import com.tangoplus.tangoq.data.SignInViewModel
import com.tangoplus.tangoq.databinding.RvProfileItemBinding
import com.tangoplus.tangoq.databinding.RvProfileSpecialItemBinding
import com.tangoplus.tangoq.db.SecurePreferencesManager.getEncryptedJwtToken
import com.tangoplus.tangoq.db.SecurePreferencesManager.saveEncryptedJwtToken
import com.tangoplus.tangoq.dialog.QRCodeDialogFragment
import com.tangoplus.tangoq.dialog.ProfileEditBSDialogFragment
import com.tangoplus.tangoq.fragment.ProfileFragment
import com.tangoplus.tangoq.`object`.Singleton_t_measure
import com.tangoplus.tangoq.`object`.Singleton_t_progress
import com.tangoplus.tangoq.`object`.Singleton_t_user
import org.json.JSONObject

import java.lang.IllegalArgumentException

class ProfileRVAdapter(private val fragment: Fragment,
                       private val booleanClickListener: BooleanClickListener,
                       val first: Boolean,
                       val case: String,
                       val vm: ViewModel
) : RecyclerView.Adapter<RecyclerView.ViewHolder> ()  {
    var profilemenulist = mutableListOf<String>()

    private val VIEW_TYPE_NORMAL = 0
    private val VIEW_TYPE_SPECIAL_ITEM = 1
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
            VIEW_TYPE_NORMAL -> {
                val inflater = LayoutInflater.from(parent.context)
                val binding = RvProfileItemBinding.inflate(inflater, parent, false)
                ViewHolder(binding.root)
            }
            VIEW_TYPE_SPECIAL_ITEM -> {
                val inflater = LayoutInflater.from(parent.context)
                val binding = RvProfileSpecialItemBinding.inflate(inflater, parent, false)
                SpecialItemViewHolder(binding.root)
            }
            else -> throw IllegalArgumentException("Invaild view Type")
        }

    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val currentItem = profilemenulist[position]
        when (holder.itemViewType) {
            VIEW_TYPE_NORMAL -> {
                val ViewHolder = holder as ViewHolder
                when (case) {
                    "profile" -> {
                        when (currentItem) {
                            "내정보" -> holder.ivPf.setImageResource(R.drawable.icon_profile)
                            "연동 관리" -> holder.ivPf.setImageResource(R.drawable.icon_multi_device)
                            "푸쉬 알림 설정" -> holder.ivPf.setImageResource(R.drawable.icon_alarm_small)
                            "문의하기" -> holder.ivPf.setImageResource(R.drawable.icon_inquire)
                            "공지사항" -> holder.ivPf.setImageResource(R.drawable.icon_announcement)
                            "앱 버전" -> holder.ivPf.setImageResource(R.drawable.icon_copy)
                            "개인정보 처리방침" -> holder.ivPf.setImageResource(R.drawable.icon_paper)
                            "서비스 이용약관" -> holder.ivPf.setImageResource(R.drawable.icon_paper)
                            "로그아웃" -> holder.ivPf.setImageResource(R.drawable.icon_logout)
                            "회원탈퇴" -> holder.ivPf.setImageResource(R.drawable.icon_logout)
                        }
                        // ------! 앱 버전 text 설정 시작 !------
                        ViewHolder.tvPfSettingsName.text = currentItem
                        if (ViewHolder.tvPfSettingsName.text == "앱 버전") {
                            val packageInfo = fragment.requireContext().packageManager.getPackageInfo(fragment.requireContext().packageName, 0)
                            val versionName = packageInfo.versionName
                            ViewHolder.tvPfInfo.text = "v$versionName"
                        } else
                            ViewHolder.tvPfInfo.text = ""
                        // ------! 앱 버전 text 설정 끝 !------

                        // ------! 각 item 클릭 동작 시작 !------
                        ViewHolder.cltvPfSettings.setOnClickListener {
                            when (currentItem) {
                                "내정보" -> {
                                    val dialogFragment = ProfileEditDialogFragment()
                                    dialogFragment.setProfileUpdateListener(fragment as ProfileFragment)
                                    dialogFragment.show(fragment.requireActivity().supportFragmentManager, "ProfileEditDialogFragment")
                                }
                                "QR코드 핀번호 로그인" -> {
                                    val dialog = QRCodeDialogFragment()
                                    dialog.show(fragment.requireActivity().supportFragmentManager, "LoginScanDialogFragment")
                                }
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
                                    if (Firebase.auth.currentUser != null) {
                                        Firebase.auth.signOut()
                                        Log.d("로그아웃", "Firebase sign out successful")
                                    } else if (NaverIdLoginSDK.getState() == NidOAuthLoginState.OK) {
                                        NaverIdLoginSDK.logout()
                                        Log.d("로그아웃", "Naver sign out successful")
                                    } else if (AuthApiClient.instance.hasToken()) {
                                        UserApiClient.instance.logout { error->
                                            if (error != null) {
                                                Log.e("로그아웃", "KAKAO Sign out failed", error)
                                            } else {
                                                Log.e("로그아웃", "KAKAO Sign out successful")
                                            }
                                        }
                                    } else if (getEncryptedJwtToken(fragment.requireContext()) != null) {
                                        saveEncryptedJwtToken(fragment.requireContext(), null)
                                    }
                                    val intent = Intent(holder.itemView.context, IntroActivity::class.java)
                                    holder.itemView.context.startActivity(intent)
                                    fragment.requireActivity().finishAffinity()
                                    // 싱글턴에 들어갔던거 전부 비우기
                                    Singleton_t_user.getInstance(fragment.requireContext()).jsonObject = null
                                    Singleton_t_measure.getInstance(fragment.requireContext()).measures = null
                                    Singleton_t_progress.getInstance(fragment.requireContext()).programProgresses = mutableListOf()
                                    Singleton_t_progress.getInstance(fragment.requireContext()).graphProgresses = mutableListOf()
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
                                holder.tvPfInfo.text = userJson.optString("user_name")
                                holder.ivPf.setImageResource(R.drawable.icon_profile)
                            }
                            "성별" -> {
                                holder.tvPfInfo.text = if (userJson.optString("gender")== "0") "여자" else "남자"
                                holder.ivPf.setImageResource(R.drawable.icon_gender)
                            }
                            "몸무게" -> {
                                (vm as SignInViewModel).setWeight.observe(fragment.viewLifecycleOwner) { weight ->
                                    holder.tvPfInfo.text = "$weight kg"
                                }
                                holder.ivPf.setImageResource(R.drawable.icon_weight)
                            }
                            "신장" -> {
                                (vm as SignInViewModel).setHeight.observe(fragment.viewLifecycleOwner) { height ->
                                    holder.tvPfInfo.text = "$height cm"
                                }
                                holder.ivPf.setImageResource(R.drawable.icon_height)
                            }
                            "이메일" -> {
                                (vm as SignInViewModel).setEmail.observe(fragment.viewLifecycleOwner) { email ->
                                    holder.tvPfInfo.text = "$email"
                                }
                                holder.ivPf.setImageResource(R.drawable.icon_email)
                            }
                        }


                        holder.cltvPfSettings.setOnClickListener {
                            when (holder.tvPfSettingsName.text) {
                                "몸무게" -> {
                                    val dialog = ProfileEditBSDialogFragment.newInstance("몸무게", (vm as SignInViewModel).setWeight.value.toString())
                                    dialog.show(fragment.requireActivity().supportFragmentManager, "ProfileEditBSDialogFragment")
                                }
                                "신장" -> {
                                    val dialog = ProfileEditBSDialogFragment.newInstance("신장", (vm as SignInViewModel).setHeight.value.toString())
                                    dialog.show(fragment.requireActivity().supportFragmentManager, "ProfileEditBSDialogFragment")
                                }
                                "이메일" -> {
                                    val dialog = ProfileEditBSDialogFragment.newInstance("이메일", (vm as SignInViewModel).setEmail.value.toString())
                                    dialog.show(fragment.requireActivity().supportFragmentManager, "ProfileEditBSDialogFragment")
                                }

                            }
                        }
                    }
                }

            } // -----! 다크모드 시작 !-----
            VIEW_TYPE_SPECIAL_ITEM -> {
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
            VIEW_TYPE_SPECIAL_ITEM
        } else {
            VIEW_TYPE_NORMAL
        }
    }
    override fun getItemCount(): Int {
        return profilemenulist.size

    }
}