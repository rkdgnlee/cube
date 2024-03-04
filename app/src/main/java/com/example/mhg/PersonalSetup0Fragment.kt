package com.example.mhg

import android.app.Activity.RESULT_OK
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import com.example.mhg.VO.UserViewModel
import com.example.mhg.databinding.FragmentPersonalSetup0Binding


class PersonalSetup0Fragment : Fragment() {
    private lateinit var viewModel: UserViewModel
    lateinit var binding : FragmentPersonalSetup0Binding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentPersonalSetup0Binding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

//        binding.btnBbatonOAuth.setOnClickListener {
//            val bbatonIntent = Intent(requireContext(), BbatonSdk::class.java).apply {
//                putExtra("clientId", "JDJhJDA0JFZrQUQ0ZUN5QldqaHZyRnIzZUJtek8wNlFZaHpuZjNFYVpwWjdR")
//                putExtra("clientSecret", "YThmRlJwY3FIR29OZVV5")
//                putExtra("redirectUri", "http://www.optonics.biz/html/")
//            }
//            startActivityForResult(bbatonIntent, 1) //SDK 호출
//
//        }


    }
    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode ==1 && resultCode == RESULT_OK) {
            val adultFlag = data?.getStringExtra("adult_flag")
        }
    }
}
