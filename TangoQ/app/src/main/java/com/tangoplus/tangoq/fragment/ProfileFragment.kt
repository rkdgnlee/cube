package com.tangoplus.tangoq.fragment

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.OpenableColumns
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.MultiTransformation
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestOptions
import com.tangoplus.tangoq.MainActivity
import com.tangoplus.tangoq.R
import com.tangoplus.tangoq.adapter.ProfileRVAdapter
import com.tangoplus.tangoq.viewmodel.SignInViewModel
import com.tangoplus.tangoq.listener.BooleanClickListener
import com.tangoplus.tangoq.db.Singleton_t_user
import com.tangoplus.tangoq.databinding.FragmentProfileBinding
import com.tangoplus.tangoq.dialog.AlarmDialogFragment
import com.tangoplus.tangoq.listener.ProfileUpdateListener
import com.tangoplus.tangoq.api.NetworkUser.sendProfileImage
import com.tangoplus.tangoq.dialog.ProfileEditChangeDialogFragment
import com.tangoplus.tangoq.listener.OnSingleClickListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import org.json.JSONObject
import java.io.File
import java.util.Calendar
import java.util.TimeZone



class ProfileFragment : Fragment(), BooleanClickListener, ProfileUpdateListener {
    lateinit var binding : FragmentProfileBinding
    val svm : SignInViewModel by activityViewModels()
    private var userJson : JSONObject? = null
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
        userJson = Singleton_t_user.getInstance(requireContext()).jsonObject
        // profile쪽 VM 값 세팅
        svm.snsCount = 0
        // ------! 싱글턴에서 가져오기 !------
        svm.User.value = userJson
        svm.setHeight.value = svm.User.value?.optInt("height")
        svm.setWeight.value = svm.User.value?.optInt("weight")
        svm.setEmail.value = svm.User.value?.optString("email")
        svm.setBirthday.value = svm.User.value?.optInt("birthday").toString()
        svm.setMobile.value = svm.User.value?.optString("mobile").toString()
        svm.setGender.value = svm.User.value?.optInt("gender")

//        Log.v("Singleton>Profile", "$userJson")
        updateUserData()

        binding.ibtnPAlarm.setOnSingleClickListener {
            val dialog = AlarmDialogFragment()
            dialog.show(requireActivity().supportFragmentManager, "AlarmDialogFragment")
        }

        // ------! 프로필 사진 관찰 시작 !------
        svm.ivProfile.observe(viewLifecycleOwner) {
            if (it != null) {
                Glide.with(this)
                    .load(it)
                    .apply(RequestOptions.bitmapTransform(MultiTransformation(CenterCrop(), RoundedCorners(16))))
                    .into(binding.civP)
            }
        }
        binding.civP.setOnSingleClickListener { navigateImage() }
        binding.ivPfEdit.setOnSingleClickListener { navigateImage() }

        // ------! 정보 목록 recyclerView 연결 시작 !------
        val profilemenulist = mutableListOf(
            "내정보",
            "다크 모드",
            "QR코드 핀번호 로그인",

//            "연동 관리",
            "푸쉬 알림 설정",
            "자주 묻는 질문",
            "문의하기",

            "공지사항",
            "앱 버전",
            "개인정보 처리방침",
            "서비스 이용약관",
            "로그아웃",
            "회원탈퇴"
        )
        setAdapter(profilemenulist.subList(0,3), binding.rvPNormal,0)
        setAdapter(profilemenulist.subList(3,6), binding.rvPHelp, 1)
        setAdapter(profilemenulist.subList(6, profilemenulist.size), binding.rvPDetail, 2)
        // ------! 정보 목록 recyclerView 연결 끝 !------


        svm.setWeight.observe(viewLifecycleOwner) { weight ->
            binding.tvPWeight.text = weight.toString() + "kg"
        }
        svm.setHeight.observe(viewLifecycleOwner) { height ->
            binding.tvPHeight.text = height.toString() + "cm"
        }
        svm.setBirthday.observe(viewLifecycleOwner) { birthday ->
            if (birthday.length >= 8) {
                Log.v("버스데이", "$birthday")
                val c = Calendar.getInstance(TimeZone.getTimeZone("Asia/Seoul"))
                binding.tvPAge.text = (c.get(Calendar.YEAR) - birthday.substring(0, 4).toInt()).toString() + "세"
            }
        }
        binding.tvPHeight.setOnSingleClickListener {
            val dialog = ProfileEditChangeDialogFragment.newInstance("신장", svm.setHeight.value.toString())
            dialog.show(requireActivity().supportFragmentManager, "ProfileEditBSDialogFragment")
        }
        binding.tvPWeight.setOnSingleClickListener {
            val dialog = ProfileEditChangeDialogFragment.newInstance("몸무게", svm.setWeight.value.toString())
            dialog.show(requireActivity().supportFragmentManager, "ProfileEditBSDialogFragment")
        }
        binding.tvPAge.setOnSingleClickListener {
            if (binding.tvPAge.text == "미설정") {
                val dialog = ProfileEditChangeDialogFragment.newInstance("생년월일", svm.setBirthday.value.toString())
                dialog.show(requireActivity().supportFragmentManager, "ProfileEditBSDialogFragment")
            }
        }
    }

    // ------! 프로필 사진 관찰 끝 !------
    private fun hasPermission(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(
            requireContext(),
            permission
        ) == PackageManager.PERMISSION_GRANTED
    }

    // 여러 권한 체크 헬퍼 함수
    private fun hasPermissions(vararg permissions: String): Boolean {
        return permissions.all { permission ->
            ContextCompat.checkSelfPermission(
                requireContext(),
                permission
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    // permission 결과 처리
    @Deprecated("Deprecated in Java")
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            1000 -> {
                if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                    navigateGallery()
                } else {
                    Toast.makeText(requireContext(), "권한 설정을 허용해 주십시오", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun setAdapter(list: MutableList<String>, rv: RecyclerView, index: Int) {
        if (index != 0 ) {
            val adapter = ProfileRVAdapter(this@ProfileFragment, this@ProfileFragment, false, "profile", svm)
            adapter.profileMenuList = list
            rv.adapter = adapter
            rv.layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
        } else {
            val adapter = ProfileRVAdapter(this@ProfileFragment, this@ProfileFragment, true, "profile", svm)
            adapter.profileMenuList = list
            rv.adapter = adapter
            rv.layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
        }
    }
    // ------# code 2000 으로 갤러리 프로필 사진 바꾸기 #------
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
                val selectedImageUri: Uri? = data?.data
                selectedImageUri?.let { uri ->
                    val contentResolver = requireContext().contentResolver
                    // 실제 파일 이름 가져오기
                    val cursor = contentResolver.query(uri, null, null, null, null)
                    val nameIndex = cursor?.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    cursor?.moveToFirst()
                    val fileName = cursor?.getString(nameIndex ?: 0) ?: "unknown_file"
                    cursor?.close()

                    // MIME 타입 가져오기
//                    val mimeType = contentResolver.getType(uri) ?: "image/jpeg"
                    val mimeType = "image/jpeg"
                    val imageFile = requireContext().uriToFile(uri, fileName)
                    imageFile?.let {
                        val requestBody = MultipartBody.Builder()
                            .setType(MultipartBody.FORM)
                            .addFormDataPart(
                                "profile_image",
                                fileName,
                                it.asRequestBody(mimeType.toMediaTypeOrNull())
                            )
                            .build()
                        CoroutineScope(Dispatchers.IO).launch {
                            sendProfileImage(requireContext(), getString(R.string.API_user),
                                userJson?.optString("sn").toString(), requestBody) { imageUrl ->
                                CoroutineScope(Dispatchers.Main).launch {
                                    Singleton_t_user.getInstance(requireContext()).jsonObject?.put("profile_file_path", imageUrl.replace("\\", ""))
                                }
                            }
                        }
                        Glide.with(this)
                            .load(selectedImageUri)
                            .diskCacheStrategy(DiskCacheStrategy.ALL)
                            .apply(RequestOptions.bitmapTransform(MultiTransformation(CenterCrop(), RoundedCorners(16))))
                            .into(binding.civP)
                    }
                }
            }
        }
    }
    private fun updateUserData() {
        // ------! profile의 나이, 몸무게, 키  설정 코드 시작 !------
        val userJson = Singleton_t_user.getInstance(requireContext()).jsonObject
        Log.v("Singleton>updateUserData", "$userJson")
        val heightJo = userJson?.optDouble("height")
        val weightJo = userJson?.optDouble("weight")
        requireActivity().runOnUiThread {
            if (userJson != null) {
                if (heightJo != null && weightJo != null) {
                    val height = when (userJson.optDouble("height")) {
                        in 80.0 .. 250.0 -> { userJson.optDouble("height").toInt().toString() + "cm" }
                        else -> { "미설정" }
                    }
                    binding.tvPHeight.text = height

                    val weight = when(userJson.optDouble("weight")) {
                        in 20.0 .. 200.0 -> { userJson.optDouble("weight").toInt().toString() + "kg" }
                        else -> { "미설정" }
                    }
                    binding.tvPWeight.text = weight
                }
                binding.tvPfName.text = userJson.optString("user_name")
                val c = Calendar.getInstance(TimeZone.getTimeZone("Asia/Seoul"))
                binding.tvPAge.text = try {
                    if (userJson.optString("birthday") != "0000-00-00") {
                        (c.get(Calendar.YEAR) - userJson.optString("birthday").substring(0, 4).toInt()).toString() + "세"
                    } else {
                        "미설정"
                    }

                } catch (e: IndexOutOfBoundsException) {
                    Log.e("ProfileError", "IndexOutOfBounds: ${e.message}")
                    "미설정"
                } catch (e: IllegalArgumentException) {
                    Log.e("ProfileError", "IllegalArgument: ${e.message}")
                    "미설정"
                } catch (e: IllegalStateException) {
                    Log.e("ProfileError", "IllegalState: ${e.message}")
                    "미설정"
                } catch (e: NullPointerException) {
                    Log.e("ProfileError", "NullPointer: ${e.message}")
                    "미설정"
                } catch (e: java.lang.Exception) {
                    Log.e("ProfileError", "Exception: ${e.message}")
                    "미설정"
                }

                // ----- 이미지 로드 시작 -----
                val imageUri = userJson.optString("profile_file_path")
                Log.v("imageUri", imageUri)
                if (imageUri != "") {
                    Glide.with(this)
                        .load(imageUri)
                        .apply(RequestOptions.bitmapTransform(MultiTransformation(CenterCrop(), RoundedCorners(16))))
                        .into(binding.civP)
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

    private fun Context.uriToFile(uri: Uri, fileName: String): File? {
        val inputStream = contentResolver.openInputStream(uri) ?: return null
        val file = File(cacheDir, fileName)
        inputStream.use { input ->
            file.outputStream().use { output ->
                input.copyTo(output)
            }
        }
        return file
    }

    private fun navigateImage() {
        when {
            // Android 14
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE -> {
                if (hasPermissions(
                        Manifest.permission.READ_MEDIA_IMAGES,
                        Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED)) {
                    navigateGallery()
                } else {
                    requestPermissions(
                        arrayOf(
                            Manifest.permission.READ_MEDIA_IMAGES,
                            Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED
                        ), 1000)
                }
            }
            // Android 13
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
                if (hasPermission(Manifest.permission.READ_MEDIA_IMAGES)) {
                    navigateGallery()
                } else {
                    requestPermissions(
                        arrayOf(Manifest.permission.READ_MEDIA_IMAGES), 1000)
                }
            }
            // Android 12 이하
            else -> {
                if (hasPermission(Manifest.permission.READ_EXTERNAL_STORAGE)) {
                    navigateGallery()
                } else {
                    requestPermissions(
                        arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                        1000
                    )
                }
            }
        }
    }

    private fun View.setOnSingleClickListener(action: (v: View) -> Unit) {
        val listener = View.OnClickListener { action(it) }
        setOnClickListener(OnSingleClickListener(listener))
    }
}