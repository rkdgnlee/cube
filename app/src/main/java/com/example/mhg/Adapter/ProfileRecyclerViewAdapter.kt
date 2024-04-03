package com.example.mhg.Adapter

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.example.mhg.DeviceSettingsFragment
import com.example.mhg.HomeRoutineDetailFragment
import com.example.mhg.IntroActivity
import com.example.mhg.MainActivity
import com.example.mhg.R
import com.example.mhg.VO.RoutingVO
import com.example.mhg.databinding.ProfileMenuLastItemBinding
import com.example.mhg.databinding.ProfileMenuListBinding
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.kakao.sdk.auth.AuthApiClient
import com.kakao.sdk.user.UserApiClient
import com.navercorp.nid.NaverIdLoginSDK
import com.navercorp.nid.oauth.NidOAuthLoginState
import java.lang.IllegalArgumentException



class ProfileRecyclerViewAdapter(private val fragment: Fragment) : RecyclerView.Adapter<RecyclerView.ViewHolder> () {
    var profilemenulist = mutableListOf<RoutingVO>()
    private val VIEW_TYPE_NORMAL = 0
    private val VIEW_TYPE_LAST_ITEM = 1

    inner class MyViewHolder(view : View) : RecyclerView.ViewHolder(view) {
        val btnprofiletitle = view.findViewById<Button>(R.id.btnProfileTitle)

    }
    inner class LastItemViewHolder(view : View) : RecyclerView.ViewHolder(view) {
        val btnsignout = view.findViewById<Button>(R.id.btnSignOut)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_NORMAL -> {
                val inflater = LayoutInflater.from(parent.context)
                val binding = ProfileMenuListBinding.inflate(inflater, parent, false)
                MyViewHolder(binding.root)
            }
            VIEW_TYPE_LAST_ITEM -> {
                val inflater = LayoutInflater.from(parent.context)
                val binding = ProfileMenuLastItemBinding.inflate(inflater, parent, false)
                LastItemViewHolder(binding.root)
            }
            else -> throw IllegalArgumentException("Invaild view Type")
        }

    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {

        when (holder.itemViewType) {
            VIEW_TYPE_NORMAL -> {
                val myViewHolder = holder as MyViewHolder
                val currentItem = profilemenulist[position]
                myViewHolder.btnprofiletitle.text = currentItem.title

                myViewHolder.btnprofiletitle.setOnClickListener {
                    if (currentItem.title == "모드설정") {
                        showDeviceSettingsFragment()
                    }
                }

            }
            VIEW_TYPE_LAST_ITEM -> {
                val lastItemViewHolder = holder as LastItemViewHolder
                lastItemViewHolder.btnsignout.setOnClickListener {
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
        }
    }


    override fun getItemViewType(position: Int): Int {
        return if (position == profilemenulist.size) {
            VIEW_TYPE_LAST_ITEM
        } else {
            VIEW_TYPE_NORMAL
        }
    }
    override fun getItemCount(): Int {
        return profilemenulist.size + 1

    }

    private fun showDeviceSettingsFragment() {
        val DeviceSettingsFragment = DeviceSettingsFragment()
        fragment.requireActivity().supportFragmentManager.beginTransaction().apply {
            setCustomAnimations(R.anim.slide_in_left, R.anim.slide_in_right)
            replace(R.id.flProfile, DeviceSettingsFragment)
            addToBackStack(null)
            commit()
        }
    }



}