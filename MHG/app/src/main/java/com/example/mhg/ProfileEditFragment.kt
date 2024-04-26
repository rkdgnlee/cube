package com.example.mhg

import android.app.AlertDialog
import android.content.ContentValues.TAG
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.mhg.databinding.FragmentProfileEditBinding
import com.example.mhg.`object`.NetworkUserService.fetchUserDeleteJson
import com.example.mhg.`object`.NetworkUserService.fetchUserUPDATEJson
import com.example.mhg.`object`.Singleton_t_user
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.kakao.sdk.auth.AuthApiClient
import com.kakao.sdk.user.UserApiClient
import com.navercorp.nid.NaverIdLoginSDK
import com.navercorp.nid.oauth.NidOAuthLoginState
import org.json.JSONObject


class ProfileEditFragment : Fragment() {
    lateinit var binding : FragmentProfileEditBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentProfileEditBinding.inflate(inflater)

        val t_userData = Singleton_t_user.getInstance(requireContext())

//        val user_password = t_userData.jsonObject?.getString("user_password")
        val user_email = t_userData.jsonObject?.getString("user_email")
        val user_mobile = t_userData.jsonObject?.getString("user_mobile")
        Log.w("$TAG, update", "user_email: $user_email")

//        binding.etPWProfileEdit.setText(user_password ?: "")

        if (user_email != null) {
            binding.etEmailProfileEdit.setText(user_email ?: "")
        }
        if (user_mobile != null) {
            binding.etPhoneProfileEdit.setText(user_mobile ?: "")
        }
        binding.btnProfileUpdate.setOnClickListener {
            val etUser_password = binding.etPWProfileEdit.text.toString()
            val etUser_email =binding.etEmailProfileEdit.text.toString()
            val etUser_mobile = binding.etPhoneProfileEdit.text.toString()

            val JsonObj = JSONObject()
            JsonObj.put("user_password",etUser_password )
            JsonObj.put("user_email", etUser_email )
            JsonObj.put("user_mobile", etUser_mobile )
            if (user_mobile != null) {
                fetchUserUPDATEJson(getString(R.string.IP_ADDRESS_t_user), JsonObj.toString(), t_userData.jsonObject!!.optString("user_mobile")) {
                    requireActivity().supportFragmentManager.beginTransaction().apply {
                        replace(R.id.flMain, ProfileFragment())
                        commit()
                    }
                }
            }
        }
        binding.btnProfileDelete.setOnClickListener {
            edit(user_mobile.toString())
        }


        return binding.root
    }
    fun edit(user_mobile : String) {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("계정 삭제")
            .setMessage("계정을 삭제하면 복구할 수 없습니다.\n그래도 진행하시겠습니까?")
            .setPositiveButton("확인",
                DialogInterface.OnClickListener { dialog, id ->
                    fetchUserDeleteJson(getString(R.string.IP_ADDRESS_t_user), user_mobile = user_mobile) {
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
                        val intent = Intent(requireContext(), IntroActivity::class.java)
                        startActivity(intent)
                        dialog.dismiss()
                    }
                })
            .setNegativeButton("취소",
                DialogInterface.OnClickListener { dialog, id ->
                    dialog.dismiss()
                })
        builder.show()

    }

}