package com.tangoplus.tangoq.fragment

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.core.net.toFile
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.MultiTransformation
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestOptions
import com.tangoplus.tangoq.R
import com.tangoplus.tangoq.adapter.ProfileRVAdapter
import com.tangoplus.tangoq.data.SignInViewModel
import com.tangoplus.tangoq.listener.BooleanClickListener
import com.tangoplus.tangoq.`object`.Singleton_t_user
import com.tangoplus.tangoq.databinding.FragmentProfileBinding
import com.tangoplus.tangoq.dialog.AlarmDialogFragment
import com.tangoplus.tangoq.listener.ProfileUpdateListener
import com.tangoplus.tangoq.`object`.NetworkUser.sendProfileImage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.util.Calendar
import java.util.TimeZone



class ProfileFragment : Fragment(), BooleanClickListener, ProfileUpdateListener {
    lateinit var binding : FragmentProfileBinding
    val viewModel : SignInViewModel by activityViewModels()
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentProfileBinding.inflate(inflater)
        return binding.root
    }

    @SuppressLint("SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // ------! profile의 나이, 몸무게, 키  설정 코드 시작 !------
        val userJson = Singleton_t_user.getInstance(requireContext()).jsonObject

        Log.v("Singleton>Profile", "${userJson}")
        updateUserData()

        binding.ibtnPAlarm.setOnClickListener {
            val dialog = AlarmDialogFragment()
            dialog.show(requireActivity().supportFragmentManager, "AlarmDialogFragment")
        }

        // ------! 프로필 사진 관찰 시작 !------
        viewModel.ivProfile.observe(viewLifecycleOwner) {
            if (it != null) {
                Glide.with(this)
                    .load(it)
                    .apply(RequestOptions.bitmapTransform(MultiTransformation(CenterCrop(), RoundedCorners(16))))
                    .into(binding.civP)
            }
        }
        binding.civP.setOnClickListener {
            when {
                ContextCompat.checkSelfPermission(requireContext(), android.Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED -> {
                    navigateGallery()
                }
                else -> requestPermissions(
                    arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE),
                    1000
                )
            }
        }
        // ------! 프로필 사진 관찰 끝 !------

        // ------! 정보 목록 recyclerView 연결 시작 !------
        val profilemenulist = mutableListOf(
            "내정보",
            "다크 모드",
            "QR코드 핀번호 로그인",
            "푸쉬 알림 설정",
            "자주 묻는 질문",
            "문의하기",
            "공지사항",
            "앱 버전",
            "개인정보 처리방침",
            "서비스 이용약관",
            "로그아웃",
        )
        setAdapter(profilemenulist.subList(0,4), binding.rvPNormal,0)
        setAdapter(profilemenulist.subList(4,6), binding.rvPHelp, 1)
        setAdapter(profilemenulist.subList(6, profilemenulist.size), binding.rvPDetail, 2)
        // ------! 정보 목록 recyclerView 연결 끝 !------

//        binding.ibtnPfEdit.setOnClickListener {
//            when {
//                ContextCompat.checkSelfPermission(requireContext(), android.Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED -> {
//                    navigateGallery()
//                }
//                else -> requestPermissions(
//                    arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE),
//                    1000
//                )
//            }
//        }
        // ------! 회원탈퇴 시작 !------
        binding.tvWithDrawal.setOnClickListener {
            requireActivity().supportFragmentManager.beginTransaction().apply {
                setCustomAnimations(R.anim.slide_in_left, R.anim.slide_in_right)
                replace(R.id.flMain, WithdrawalFragment())
                addToBackStack(null)
                commit()
            }
        }

        // ------! 회원탈퇴 끝 !------
    }

    private fun setAdapter(list: MutableList<String>, rv: RecyclerView, index: Int) {
        if (index != 0 ) {
            val adapter = ProfileRVAdapter(this@ProfileFragment, this@ProfileFragment, false, "profile", requireActivity().supportFragmentManager)
            adapter.profilemenulist = list
            rv.adapter = adapter
            rv.layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
        } else {
            val adapter = ProfileRVAdapter(this@ProfileFragment, this@ProfileFragment, true, "profile", requireActivity().supportFragmentManager)
            adapter.profilemenulist = list
            rv.adapter = adapter
            rv.layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            1000 -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                    navigateGallery()
                else
                    Toast.makeText(requireContext(), "권한 설정을 허용해 주십시오", Toast.LENGTH_SHORT).show()
            }
        }
    }
    private fun navigateGallery() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        startActivityForResult(intent, 2000)

    }
    @SuppressLint("CheckResult")
    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode != Activity.RESULT_OK)
            return
        when (requestCode) {
            2000 -> {
                val selectedImageUri : Uri? = data?.data
                if (selectedImageUri != null) {

                    // ------# 멀티파트로 담아 프로필 사진 저장하기 #------
                    val imageFile = selectedImageUri.toFile()
                    val requestBodyBuilder = MultipartBody.Builder()
                    requestBodyBuilder.addFormDataPart(
                        "profile_image",
                        imageFile.name,
                        imageFile.asRequestBody("image/jpeg".toMediaTypeOrNull())
                    )
                    val requestBody = requestBodyBuilder.build()
                    CoroutineScope(Dispatchers.IO).launch {
                        sendProfileImage(requireContext(), getString(R.string.API_user), requestBody)
                    }


                    val sharedPreferences = requireActivity().getSharedPreferences("MySharedPref", Context.MODE_PRIVATE)
                    val editor = sharedPreferences.edit()
                    editor.putString("imageUri", selectedImageUri.toString())
                    editor.apply()
                    Glide.with(this)
                        .load(selectedImageUri)
                        .apply(RequestOptions.bitmapTransform(MultiTransformation(CenterCrop(), RoundedCorners(16))))
                        .into(binding.civP)
                } else {
                    Toast.makeText(requireContext(), "사진을 가져오지 못했습니다.", Toast.LENGTH_SHORT).show()
                }
            }
            else -> {
                Toast.makeText(requireContext(), "사진을 가져오지 못했습니다.", Toast.LENGTH_SHORT).show()
            }
        }
    }
    private fun updateUserData() {
        // ------! profile의 나이, 몸무게, 키  설정 코드 시작 !------
        val userJson = Singleton_t_user.getInstance(requireContext()).jsonObject
        Log.v("Singleton>updateUserData", "$userJson")
        requireActivity().runOnUiThread {
            if (userJson != null) {
                binding.tvPfName.text = userJson.optString("user_name")

                val height = when (userJson.optDouble("user_height")) {
                    in 0.0 .. 250.0 -> { userJson.optDouble("user_height").toInt().toString() + "cm" }
                    else -> { "미설정" }
                }
                binding.tvPHeight.text = height

                val weight = when(userJson.optDouble("user_weight")) {
                    in 0.0 .. 150.0 -> { userJson.optDouble("user_weight").toInt().toString() + "kg" }
                    else -> { "미설정" }
                }
                binding.tvPWeight.text = weight

                val c = Calendar.getInstance(TimeZone.getTimeZone("Asia/Seoul"))
                binding.tvPAge.text = try {
                    (c.get(Calendar.YEAR) - userJson.optString("user_birthday").substring(0, 4).toInt()).toString() + "세"
                } catch (e: Exception) {
                    "미설정"
                }


                // ----- 이미지 로드 시작 -----
                val sharedPreferences = requireActivity().getSharedPreferences("MySharedPref", Context.MODE_PRIVATE)
                val imageUri = sharedPreferences.getString("imageUri", null)
                if (imageUri != null) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        Glide.with(this)
                            .load(imageUri)
                            .apply(RequestOptions.bitmapTransform(MultiTransformation(CenterCrop(), RoundedCorners(16))))
                            .into(binding.civP)
                    }
                }
            }
        }
    }
    override fun onSwitchChanged(isChecked: Boolean) {
        val sharedPref = requireActivity().getSharedPreferences("deviceSettings", Context.MODE_PRIVATE)
        val modeEditor = sharedPref?.edit()
        if (isChecked) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            modeEditor?.putBoolean("darkMode", true) ?: true
            modeEditor?.apply()
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            modeEditor?.putBoolean("darkMode", false) ?: false
            modeEditor?.apply()
        }
    }

    override fun onProfileUpdated() {
        updateUserData()
    }

}