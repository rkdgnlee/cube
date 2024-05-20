package com.tangoplus.tangoq.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.health.connect.client.HealthConnectClient
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
import com.tangoplus.tangoq.fragment.SettingsFragment
import com.tangoplus.tangoq.IntroActivity
import com.tangoplus.tangoq.listener.BooleanClickListener
import com.tangoplus.tangoq.R
import com.tangoplus.tangoq.databinding.RvProfileItemBinding
import com.tangoplus.tangoq.databinding.RvProfileSpecialItemBinding
import java.lang.IllegalArgumentException

class ProfileRVAdapter(private val fragment: Fragment, private val booleanClickListener: BooleanClickListener, val first: Boolean) : RecyclerView.Adapter<RecyclerView.ViewHolder> ()  {
    var profilemenulist = mutableListOf<String>()
    private val VIEW_TYPE_NORMAL = 0
    private val VIEW_TYPE_SPECIAL_ITEM = 1
    inner class MyViewHolder(view : View) : RecyclerView.ViewHolder(view) {
        val btnPfName = view.findViewById<TextView>(R.id.tvPfSettingsName)
        val ivPf = view.findViewById<ImageView>(R.id.ivPf)
        val cltvPfSettings = view.findViewById<ConstraintLayout>(R.id.cltvPfSettings)
    }
    inner class SpecialItemViewHolder(view : View) : RecyclerView.ViewHolder(view) {
        @SuppressLint("UseSwitchCompatOrMaterialCode")
        val schPfS = view.findViewById<MaterialSwitch>(R.id.schPfS)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_NORMAL -> {
                val inflater = LayoutInflater.from(parent.context)
                val binding = RvProfileItemBinding.inflate(inflater, parent, false)
                MyViewHolder(binding.root)
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
                val myViewHolder = holder as MyViewHolder
                when (currentItem) {
                    "내정보" -> holder.ivPf.setImageResource(R.drawable.icon_profile)
                    "연동 관리" -> holder.ivPf.setImageResource(R.drawable.icon_multi_device)
                    "푸쉬 알림 설정" -> holder.ivPf.setImageResource(R.drawable.icon_alarm)
                    "로그아웃" -> holder.ivPf.setImageResource(R.drawable.icon_logout)
                    "회원탈퇴" -> holder.ivPf.setImageResource(R.drawable.icon_logout)
                }

                myViewHolder.btnPfName.text = currentItem
                myViewHolder.cltvPfSettings.setOnClickListener {
                    when (currentItem) {
                        "내정보" -> {
                            holder.ivPf.setImageResource(R.drawable.icon_profile)
                            val dialogFragment = ProfileEditDialogFragment()
                            dialogFragment.show(fragment.requireActivity().supportFragmentManager, "PlayThumbnailDialogFragment")
                        }
                        "연동 관리" -> {
                            val settingsIntent = Intent()
                            settingsIntent.action = HealthConnectClient.ACTION_HEALTH_CONNECT_SETTINGS
                            fragment.startActivity(settingsIntent)
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
                        "앱 버전" -> {

                        }
                        "개인정보 처리방침" -> {
                            val dialog = AgreementDetailDialogFragment.newInstance("agreement1")
                            dialog.show(fragment.requireActivity().supportFragmentManager, "agreement_dialog")
                        }
                        "서비스 이용약관" -> {
                            val dialog = AgreementDetailDialogFragment.newInstance("agreement2")
                            dialog.show(fragment.requireActivity().supportFragmentManager, "agreement_dialog")
                        }
                        "로그아웃" -> {
                            holder.ivPf.setImageResource(R.drawable.icon_logout)

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
                            }
                            val intent = Intent(holder.itemView.context, IntroActivity::class.java)
                            holder.itemView.context.startActivity(intent)
                            fragment.requireActivity().finishAffinity()
                        }
                    }
                }
            } // -----! 다크모드 시작 !-----
            VIEW_TYPE_SPECIAL_ITEM -> {
                val myViewHolder = holder as SpecialItemViewHolder
                when (currentItem) {
                    "다크모드" -> {
                        val sharedPref = fragment.requireActivity().getSharedPreferences("deviceSettings", Context.MODE_PRIVATE)
                        val darkMode = sharedPref?.getBoolean("darkMode", false)
                        if (darkMode != null) {
                            if (darkMode == true ) {
                                myViewHolder.schPfS.isChecked = true
                            } else {
                                myViewHolder.schPfS.isChecked = false
                            }
                        }
                        myViewHolder.schPfS.setOnCheckedChangeListener{CompoundButton, isChecked ->
                            booleanClickListener.onSwitchChanged(isChecked)

                        }
                    }
                }

            } // -----! 다크모드 끝 !-----
        }
    }

    override fun getItemViewType(position: Int): Int {
        return if (position == 1 && first == true) {
            VIEW_TYPE_SPECIAL_ITEM
        } else {
            VIEW_TYPE_NORMAL
        }
    }
    override fun getItemCount(): Int {
        return profilemenulist.size

    }

    private fun showSettingsFragment() {
        val DeviceSettingsFragment = SettingsFragment()
        fragment.requireActivity().supportFragmentManager.beginTransaction().apply {
            setCustomAnimations(R.anim.slide_in_left, R.anim.slide_in_right)
            replace(R.id.flMain, DeviceSettingsFragment)
            addToBackStack(null)
            commit()
        }
    }



}