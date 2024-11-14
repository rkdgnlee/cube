package com.tangoplus.tangoq.fragment

import android.icu.util.Calendar
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import com.tangoplus.tangoq.viewmodel.UserViewModel
import com.tangoplus.tangoq.databinding.FragmentSetup1Binding

class Setup1Fragment : Fragment() {
    lateinit var binding : FragmentSetup1Binding
    val viewModel: UserViewModel by activityViewModels()
    private var current = ""
    private val yyyymmdd = "YYYYMMDD"
    private val cal = Calendar.getInstance()


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentSetup1Binding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        arguments?.getString("FRAGMENT_TAG")?.let { tag ->
            fragmentManager?.beginTransaction()?.addToBackStack(tag)?.commit()
        }

        viewModel.step1.observe(viewLifecycleOwner) {
            when (it) {
                true -> {
                    binding.rbtnSquareMale.isChecked = true
                    binding.rbtnMale.isChecked = true
                }
                false -> {
                    binding.rbtnSquareFemale.isChecked = true
                    binding.rbtnFemale.isChecked = true
                }
                null -> {
                    binding.rbtnSquareMale.isChecked = false
                    binding.rbtnMale.isChecked = false
                    binding.rbtnSquareFemale.isChecked = false
                    binding.rbtnFemale.isChecked = false
                }
            }
            Log.v("VM>Gender" ,"${viewModel.step1.value}")
        }

        binding.rbtnSquareMale.setOnClickListener { viewModel.step1.value = true }
        binding.rbtnMale.setOnClickListener{ viewModel.step1.value = true }
        binding.rbtnSquareFemale.setOnClickListener { viewModel.step1.value = false }
        binding.rbtnFemale.setOnClickListener { viewModel.step1.value = false }
        // ------! 라디오 버튼 연동 끝 !------
    }

    override fun onPause() {
        super.onPause()
        if (binding.rbtnMale.isChecked) {
            viewModel.User.value?.put("user_gender", "남자")
        } else {
            viewModel.User.value?.put("user_gender", "여자")
        }

    }
}