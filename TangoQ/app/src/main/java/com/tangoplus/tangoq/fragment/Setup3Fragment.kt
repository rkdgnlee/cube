package com.tangoplus.tangoq.fragment

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import androidx.fragment.app.activityViewModels
import com.tangoplus.tangoq.data.UserViewModel
import com.tangoplus.tangoq.databinding.FragmentSetup3Binding


class Setup3Fragment : Fragment(){
    lateinit var binding : FragmentSetup3Binding
    val viewModel: UserViewModel by activityViewModels()

    val goals = mutableListOf<Int>()
    val parts = mutableListOf<Int>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentSetup3Binding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        arguments?.getString("FRAGMENT_TAG")?.let { tag ->
            fragmentManager?.beginTransaction()?.addToBackStack(tag)?.commit()
        }

        viewModel.step3.observe(viewLifecycleOwner) {
            if (it == true) {
                viewModel.User.value?.put("user_goal", "$goals")
                viewModel.User.value?.put("user_parts", "$parts")
            } else {
                viewModel.User.value?.put("user_goal", "")
                viewModel.User.value?.put("user_parts", "")
            }
        }
    }

    private fun transIdByCheckbox(checkBox: CheckBox) : Int {
        return when (checkBox.text) {
            "체력증가" -> 0
            "기초 운동 능력 증가" -> 1
            "자세 교정" -> 2
            "근재활운동 (통증 관리)" -> 3
            "일상생활 개선" -> 4

            "목관절" -> 0
            "어깨" -> 1
            "팔꿉" -> 2
            "손목" -> 3
            "하복부" -> 4
            "등" -> 5
            "무릎" -> 6
            "발목" -> 7

            else -> -1
        }
    }

}