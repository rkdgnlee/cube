package com.tangoplus.tangoq.dialog

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.AdapterView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import com.bumptech.glide.Glide
import com.bumptech.glide.load.MultiTransformation
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestOptions
import com.tangoplus.tangoq.R
import com.tangoplus.tangoq.SetupActivity
import com.tangoplus.tangoq.adapter.SpinnerAdapter
import com.tangoplus.tangoq.data.SignInViewModel
import com.tangoplus.tangoq.`object`.Singleton_t_user
import com.tangoplus.tangoq.databinding.FragmentProfileEditDialogBinding
import com.tangoplus.tangoq.`object`.NetworkUserService.fetchUserUPDATEJson
import org.json.JSONObject
import java.net.URLEncoder


class ProfileEditDialogFragment : DialogFragment() {
    lateinit var binding : FragmentProfileEditDialogBinding
    val viewModel : SignInViewModel by activityViewModels()
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentProfileEditDialogBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.nsvPfE.isNestedScrollingEnabled = false

        // -----! 이미지 로드 시작 !-----
        val sharedPreferences = requireActivity().getSharedPreferences("MySharedPref", Context.MODE_PRIVATE)


        val imageUri = sharedPreferences.getString("imageUri", null)
        if (imageUri != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                Glide.with(this)
                    .load(imageUri)
                    .apply(RequestOptions.bitmapTransform(MultiTransformation(CenterCrop(), RoundedCorners(16))))
                    .into(binding.civPE)
            }
        } // -----! 이미지 로드 끝 !-----

        val userJson = Singleton_t_user.getInstance(requireContext()).jsonObject?.getJSONObject("data")

        val userId = userJson?.optString("user_id")
        if (userId != null) {
            binding.etPEName.setText(userId)
            binding.etPEPassword.setText("************")
            binding.etPEName.isEnabled = false
            binding.etPEPassword.isEnabled = false
        }

        binding.etPEMobile.setText(userJson?.optString("user_mobile"))

        val userEmail = userJson?.optString("user_email")
        if (userEmail != null) {
            binding.etPEEmailId.setText(userEmail.substring(0, userEmail.indexOf("@")))
        }

        binding.etPEMobile.setText(userJson?.optString("user_mobile"))

        binding.ibtnPEBack.setOnClickListener {
            dismiss()
        }
        binding.spnPE
        val domain_list = listOf("gmail.com", "naver.com", "kakao.com", "직접입력")
        binding.spnPE.adapter = SpinnerAdapter(requireContext(), R.layout.item_spinner, domain_list)
        binding.spnPE.onItemSelectedListener = object : AdapterView.OnItemSelectedListener{
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                binding.spnPE.getItemAtPosition(position).toString()
                if (position == 3) {
                    binding.etPEEmail.visibility = View.VISIBLE
                    binding.spnPE.visibility = View.GONE
                    binding.ivPESpn.setOnClickListener{
                        binding.spnPE.performClick()
                        binding.spnPE.visibility = View.VISIBLE
                    }

                } else {
                    binding.etPEEmail.visibility = View.GONE
                    binding.etPEEmail.setText("")
                    binding.spnPE.visibility = View.VISIBLE
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
        binding.civPE.setOnClickListener {
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

        binding.btnPEFinish.setOnClickListener {

            when (binding.spnPE.selectedItemPosition) {
                0, 1, 2 -> {
                    viewModel.User.value?.put("user_email", "${binding.etPEEmailId.text}@${binding.spnPE.selectedItem as String}")
                    userJson?.put("user_email", viewModel.User.value?.optString("user_email"))
                }
                else -> {
                    viewModel.User.value?.put("user_email", "${binding.etPEEmailId.text}@${binding.etPEEmail.text}")
                    userJson?.put("user_email", viewModel.User.value?.optString("user_email"))
                }
            }
            val user_mobile = userJson?.optString("user_mobile")
            val encodedUserMobile = URLEncoder.encode(user_mobile, "UTF-8")
            fetchUserUPDATEJson(getString(R.string.IP_ADDRESS_t_user), userJson.toString(), encodedUserMobile) {
                Log.w(ContentValues.TAG +" 싱글톤객체추가", userJson?.optString("user_weight").toString())
                dismiss()
            }
        }
        binding.btnPEGoSetup.setOnClickListener {
            val intent = Intent(requireContext(), SetupActivity::class.java)
            startActivity(intent)
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
                    val sharedPreferences = requireActivity().getSharedPreferences("MySharedPref", Context.MODE_PRIVATE)
                    val editor = sharedPreferences.edit()
                    editor.putString("imageUri", selectedImageUri.toString())
                    editor.apply()
                    Glide.with(this)
                        .load(selectedImageUri)
                        .apply(RequestOptions.bitmapTransform(MultiTransformation(CenterCrop(), RoundedCorners(16))))
                        .into(binding.civPE)
                } else {
                    Toast.makeText(requireContext(), "사진을 가져오지 못했습니다.", Toast.LENGTH_SHORT).show()
                }
            }
            else -> {
                Toast.makeText(requireContext(), "사진을 가져오지 못했습니다.", Toast.LENGTH_SHORT).show()
            }
        }
    }
    private fun showPermissionContextPopup() {
        AlertDialog.Builder(requireContext())
            .setTitle("권한이 필요합니다.")
            .setMessage("프로필 이미지를 바꾸기 위해서는 갤러리 접근 권한이 필요합니다.")
            .setPositiveButton("동의하기") { _, _ ->
                requestPermissions(arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE), 1000)
            }
            .setNegativeButton("취소하기") { _, _ -> }
            .create()
            .show()
    }
    private fun navigateGallery() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        startActivityForResult(intent, 2000)

    }
    override fun onResume() {
        super.onResume()
        // full Screen code
        dialog?.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog?.window?.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
    }
}