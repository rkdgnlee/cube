package com.example.mhg

import android.app.Activity
import android.app.Activity.RESULT_OK
import android.app.AlertDialog
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.mhg.Adapter.ProfileRecyclerViewAdapter
import com.example.mhg.VO.RoutingVO
import com.example.mhg.databinding.FragmentProfileBinding
import com.example.mhg.`object`.Singleton_t_user
import com.google.android.gms.dynamic.SupportFragmentWrapper
import java.io.IOException
import java.time.LocalDate
import java.util.Calendar
import java.util.Date


class ProfileFragment : Fragment() {
    lateinit var binding: FragmentProfileBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        binding = FragmentProfileBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // -----! profile의 나이, 몸무게, 키  설정 코드 시작 !-----
        val t_userdata = Singleton_t_user.getInstance(requireContext())
        val user_name = t_userdata.jsonObject?.optString("user_name")
        binding.tvName.text = user_name
        // TODO 실제 값들이 다 받아와질 때 나이 계산
//        val user_birthyear = t_userdata.jsonObject?.optString("user_birthday")?.substring(0, 3)?.toInt()
//        if (user_birthyear != null) {
//            val currentYear = Calendar.getInstance()[Calendar.YEAR]
//            val age = currentYear.minus(user_birthyear)
//            binding.tvAge.text = age.toString()
//        }
        // -----! profile의 나이, 몸무게, 키  설정 코드 끝!-----


        // ----- 이미지 로드 시작 -----
        val sharedPreferences = requireActivity().getSharedPreferences("MySharedPref", Context.MODE_PRIVATE)
        val imageUri = sharedPreferences.getString("imageUri", null)
        if (imageUri != null) {
            binding.ivProfile.setImageURI(Uri.parse(imageUri))
        }
        // ----- 이미지 로드 끝 -----

        val profilemenulist = mutableListOf<RoutingVO>(
            RoutingVO("개인정보", "1"),
            RoutingVO("설정", "2"),
            RoutingVO("모드설정", "3"),
            RoutingVO("기기관리", "4"),
            RoutingVO("1:1문의", "5"),
            RoutingVO("작성글 보기", "6"),
            RoutingVO("개인정보처리방침", "7"),
        )
        val adapter = ProfileRecyclerViewAdapter()
        adapter.profilemenulist = profilemenulist
        binding.rvProfile.adapter = adapter
        binding.rvProfile.layoutManager =
            LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)

        binding.btnImageEdit.setOnClickListener {
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
        binding.btnProfileEdit.setOnClickListener {
            requireActivity().supportFragmentManager.beginTransaction().apply {
                replace(R.id.flMain, ProfileEditFragment())
                commit()
            }
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
                    binding.ivProfile.setImageURI(selectedImageUri)
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
}