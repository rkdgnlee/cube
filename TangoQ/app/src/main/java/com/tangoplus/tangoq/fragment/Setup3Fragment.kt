package com.tangoplus.tangoq.fragment

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.widget.CheckBox
import androidx.fragment.app.activityViewModels
import com.tangoplus.tangoq.R
import com.tangoplus.tangoq.data.UserViewModel
import com.tangoplus.tangoq.databinding.FragmentSetup3Binding
import com.tangoplus.tangoq.listener.SetupDialogListener
import com.tangoplus.tangoq.listener.WeightVisibilityListener


class Setup3Fragment : Fragment(), WeightVisibilityListener {
    lateinit var binding : FragmentSetup3Binding
    val viewModel: UserViewModel by activityViewModels()
    private var setupDialogListener: SetupDialogListener? = null
    val goals = mutableListOf<Int>()
    val parts = mutableListOf<Int>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentSetup3Binding.inflate(inflater)
        return binding.root
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (parentFragment is SetupDialogListener) {
            setupDialogListener = parentFragment as SetupDialogListener
        }
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        arguments?.getString("FRAGMENT_TAG")?.let { tag ->
            fragmentManager?.beginTransaction()?.addToBackStack(tag)?.commit()
        }

        binding.clSetupPart.visibility = View.GONE

        setCbTextColor(binding.cbSetupGoal1, 0)
        setCbTextColor(binding.cbSetupGoal2, 0)
        setCbTextColor(binding.cbSetupGoal3, 0)
        setCbTextColor(binding.cbSetupGoal4, 0)
        setCbTextColor(binding.cbSetupGoal5, 0)
        setCbTextColor(binding.cbSetupPart1, 1)
        setCbTextColor(binding.cbSetupPart2, 1)
        setCbTextColor(binding.cbSetupPart3, 1)
        setCbTextColor(binding.cbSetupPart4, 1)
        setCbTextColor(binding.cbSetupPart5, 1)
        setCbTextColor(binding.cbSetupPart6, 1)
        setCbTextColor(binding.cbSetupPart7, 1)
        setCbTextColor(binding.cbSetupPart8, 1)

//        updateStep31State()
//        updateStep32State()
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

    override fun visibleWeight() {

        binding.tvSetupGoalGuide.text = "집중 부위를 설정해주세요\n(집중 선택 가능)"
        val moveUpHeight = ObjectAnimator.ofFloat(binding.clSetupGoal, "translationY", 0f, -150f)
        moveUpHeight.duration = 700 // clSetupWeight와 동일한 시간으로 설정
        moveUpHeight.interpolator = DecelerateInterpolator() // 부드러운 감속 효과

        // clSetupWeight 애니메이션
        binding.clSetupPart.visibility = View.VISIBLE
        val fadeIn = ObjectAnimator.ofFloat(binding.clSetupPart, "alpha", 0f, 1f)
        val moveUpWeight = ObjectAnimator.ofFloat(binding.clSetupPart, "translationY", 150f, 0f)

        // 모든 애니메이션을 함께 실행
        val animatorSet = AnimatorSet()
        animatorSet.playTogether(moveUpHeight, fadeIn, moveUpWeight)
        animatorSet.duration = 700
        animatorSet.start()

    }

    private fun updateStep31State() {
        val isAnyChecked = binding.cbSetupGoal1.isChecked ||
            binding.cbSetupGoal2.isChecked ||
            binding.cbSetupGoal3.isChecked ||
            binding.cbSetupGoal4.isChecked ||
            binding.cbSetupGoal5.isChecked
        viewModel.step31.value = isAnyChecked
        Log.v("setup>Goal", "step31: ${viewModel.step31.value}, step32: ${viewModel.step32.value}")
    }

    private fun updateStep32State()  {
        val isAnyChecked = binding.cbSetupPart1.isChecked ||
            binding.cbSetupPart2.isChecked ||
            binding.cbSetupPart3.isChecked ||
            binding.cbSetupPart4.isChecked ||
            binding.cbSetupPart5.isChecked ||
            binding.cbSetupPart6.isChecked ||
            binding.cbSetupPart7.isChecked ||
            binding.cbSetupPart8.isChecked
        viewModel.step32.value = isAnyChecked
        Log.v("setup>Goal", "step32: ${viewModel.step32.value}, step3: ${viewModel.step3.value}")
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

    private fun setCbTextColor(checkBox: CheckBox, type: Int) {
        checkBox.setOnCheckedChangeListener{buttonView, isChecked ->
            when (isChecked) {
                true -> {
                    checkBox.setTextColor(checkBox.resources.getColor(R.color.mainColor, null))
                    when (type) {
                        0 -> { goals.add(transIdByCheckbox(checkBox)) }
                        1 -> { parts.add(transIdByCheckbox(checkBox)) }
                    }
                }
                false -> {
                    checkBox.setTextColor(checkBox.resources.getColor(R.color.subColor700, null))
                    when (type) {
                        0 -> { goals.remove(transIdByCheckbox(checkBox)) }
                        1 -> { parts.remove(transIdByCheckbox(checkBox)) }
                    }
                }
            }
            updateStep31State()
            updateStep32State()
        }
    }
}