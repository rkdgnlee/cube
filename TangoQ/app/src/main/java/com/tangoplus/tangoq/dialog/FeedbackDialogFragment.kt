package com.tangoplus.tangoq.dialog

import android.annotation.SuppressLint
import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import com.mikhaellopez.circularprogressbar.CircularProgressBar
import com.tangoplus.tangoq.R
import com.tangoplus.tangoq.data.ExerciseViewModel

import com.tangoplus.tangoq.data.MeasureViewModel
import com.tangoplus.tangoq.databinding.FragmentFeedbackDialogBinding
import com.tangoplus.tangoq.`object`.Singleton_t_user
import org.json.JSONObject

class FeedbackDialogFragment : DialogFragment() {
    lateinit var binding: FragmentFeedbackDialogBinding
    private val eViewModel: ExerciseViewModel by activityViewModels()
    private val mViewModel : MeasureViewModel by activityViewModels()

    private lateinit var fatigues: TextViewGroup
    private lateinit var intensitys: TextViewGroup
    private lateinit var satisfactions: TextViewGroup
    private var selectedIndex: Int = -1

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentFeedbackDialogBinding.inflate(inflater)
        return binding.root
    }

    @SuppressLint("SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val userJson = Singleton_t_user.getInstance(requireContext()).jsonObject

        // ------# 가져온 시간 + 운동 갯수 뿌려주기 #------
        binding.tvFDTime1.text = "${eViewModel.exerciseLog.first / 60}:${eViewModel.exerciseLog.first % 60}"
        binding.tvFDTime2.text = "${eViewModel.exerciseLog.second / 60}:${eViewModel.exerciseLog.second % 60}"
        binding.tvFDCount.text = "${eViewModel.exerciseLog.third} 개"

        // ------! 각 점수표 조작 시작 !------
        fatigues = TextViewGroup(listOf(binding.tvFDFatigue1, binding.tvFDFatigue2, binding.tvFDFatigue3, binding.tvFDFatigue4, binding.tvFDFatigue5),
            ContextCompat.getColor(requireContext(), R.color.white),
            ContextCompat.getColor(requireContext(), R.color.mainColor))

        intensitys = TextViewGroup(listOf(binding.tvFDIntensity1, binding.tvFDIntensity2, binding.tvFDIntensity3, binding.tvFDIntensity4, binding.tvFDIntensity5),
            ContextCompat.getColor(requireContext(), R.color.white),
            ContextCompat.getColor(requireContext(), R.color.mainColor))

        satisfactions= TextViewGroup(listOf(binding.tvFDSatisfaction1, binding.tvFDSatisfaction2, binding.tvFDSatisfaction3, binding.tvFDSatisfaction4, binding.tvFDSatisfaction5),
            ContextCompat.getColor(requireContext(), R.color.white),
            ContextCompat.getColor(requireContext(), R.color.mainColor))
        // ------! 각 점수표 조작 끝 !------

        binding.tvFDSkip.paintFlags = Paint.UNDERLINE_TEXT_FLAG
        binding.tvFDSkip.setOnClickListener {
//            eViewModel.exerciseLog.value = null
            eViewModel.isDialogShown.value = true
            dismiss()
        }

        binding.btnFDSubmit.setOnClickListener{
            Log.v("score", "${intensitys.getIndex()}")
            Log.v("score", "${fatigues.getIndex()}")
            Log.v("score", "${satisfactions.getIndex()}")
            val jsonObj = JSONObject()
            val parts = mutableListOf<String>()
            for (i in 0 until mViewModel.feedbackParts.value?.size!!) {
//                parts.add(mViewModel.feedbackParts.value!![i].name)
            }
            jsonObj.put("user_sn", userJson?.optString("user_sn"))
            jsonObj.put("intensity_score",intensitys.getIndex())
            jsonObj.put("fatigue_score",fatigues.getIndex())
            jsonObj.put("satisfaction_score",satisfactions.getIndex())

            jsonObj.put("pain_parts", parts)

            Log.v("피드백 점수", "$jsonObj")
//            val intent = Intent(requireActivity(), MainActivity::class.java)
//            startActivity(intent)
//            requireActivity().finishAffinity()
            dismiss()
//            eViewModel.exerciseLog.value = null
            eViewModel.isDialogShown.value = true
        }
    }

    override fun onResume() {
        super.onResume()
        // full Screen code
        dialog?.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog?.window?.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)

    }
//    fun getCheckedRadioButtonIndex(radioGroup: RadioGroup): Int {
//        val checkedRadioButtonId = radioGroup.checkedRadioButtonId
//        val radioButton = radioGroup.findViewById<RadioButton>(checkedRadioButtonId)
//        return radioGroup.indexOfChild(radioButton)
//    }
    inner class TextViewGroup(
        private val textViews: List<TextView>,
        private val defaultColor: Int = Color.BLACK,
        private val mainColor: Int = Color.BLUE
    ) {
        var selectedIndex: Int = -1

        init {
            textViews.forEachIndexed { index, textView ->
                textView.setOnClickListener {
                    updateSelection(index)
                }
            }
        }

        private fun updateSelection(index: Int) {
            if (selectedIndex != -1) {
                textViews[selectedIndex].setTextColor(defaultColor)
                textViews[index].paintFlags = 0
            }
            textViews[index].setTextColor(mainColor)
            textViews[index].paintFlags = Paint.UNDERLINE_TEXT_FLAG
            selectedIndex = index
        }

        fun getIndex(): Int {
            return selectedIndex + 1
        }
    }
}
