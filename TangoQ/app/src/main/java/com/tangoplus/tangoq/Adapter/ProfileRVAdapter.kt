package com.tangoplus.tangoq.Adapter

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CompoundButton
import android.widget.Switch
import android.widget.TextView
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.kakao.sdk.auth.AuthApiClient
import com.kakao.sdk.user.UserApiClient
import com.navercorp.nid.NaverIdLoginSDK
import com.navercorp.nid.oauth.NidOAuthLoginState
import com.tangoplus.tangoq.Fragment.SettingsFragment
import com.tangoplus.tangoq.IntroActivity
import com.tangoplus.tangoq.Listener.BooleanClickListener
import com.tangoplus.tangoq.MainActivity
import com.tangoplus.tangoq.R
import com.tangoplus.tangoq.ViewModel.RoutingVO
import com.tangoplus.tangoq.databinding.RvProfileItemBinding
import com.tangoplus.tangoq.databinding.RvProfileSpecialItemBinding
import java.lang.IllegalArgumentException

class ProfileRVAdapter(private val fragment: Fragment, private val booleanClickListener: BooleanClickListener) : RecyclerView.Adapter<RecyclerView.ViewHolder> ()  {
    var profilemenulist = mutableListOf<RoutingVO>()
    private val VIEW_TYPE_NORMAL = 0
    private val VIEW_TYPE_SPECIAL_ITEM = 1
    inner class MyViewHolder(view : View) : RecyclerView.ViewHolder(view) {
        val btnPfName = view.findViewById<TextView>(R.id.tvPfSettingsName)

    }
    inner class SpecialItemViewHolder(view : View) : RecyclerView.ViewHolder(view) {
        @SuppressLint("UseSwitchCompatOrMaterialCode")
        val schDSDark = view.findViewById<MaterialSwitch>(R.id.schDSDark)
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

                myViewHolder.btnPfName.text = currentItem.title
                myViewHolder.btnPfName.setOnClickListener {
                    if (currentItem.title == "환경설정") {
                        showSettingsFragment()

                    } else if (currentItem.title == "로그아웃") {
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
                        MainActivity().finish()
                    }
                }
            } // -----! 다크모드 시작 !-----
            VIEW_TYPE_SPECIAL_ITEM -> {
                val myViewHolder = holder as SpecialItemViewHolder
                val sharedPref = fragment.requireActivity().getSharedPreferences("deviceSettings", Context.MODE_PRIVATE)
                val darkMode = sharedPref?.getBoolean("darkMode", false)
                if (darkMode != null) {
                    if (darkMode == true ) {
                        myViewHolder.schDSDark.isChecked = true
                    } else {
                        myViewHolder.schDSDark.isChecked = false
                    }
                }
                myViewHolder.schDSDark.setOnCheckedChangeListener{CompoundButton, onSwitch ->
                    booleanClickListener.onSwitchChanged(onSwitch)

                }
            } // -----! 다크모드 끝 !-----
        }
    }


    override fun getItemViewType(position: Int): Int {
        return if (position == 1) {
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