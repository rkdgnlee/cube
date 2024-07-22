package com.tangoplus.tangoq.dialog

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.CheckBox
import android.widget.ImageView
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import com.tangoplus.tangoq.R
import com.tangoplus.tangoq.data.MeasureVO
import com.tangoplus.tangoq.listener.OnPartCheckListener
import com.tangoplus.tangoq.data.MeasureViewModel
import com.tangoplus.tangoq.databinding.FragmentFeedbackPartDialogBinding
import com.tangoplus.tangoq.`object`.Singleton_t_user
import org.json.JSONObject


class FeedbackPartDialogFragment : DialogFragment(), OnPartCheckListener {
    lateinit var binding : FragmentFeedbackPartDialogBinding
    val viewModel : MeasureViewModel by activityViewModels()
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentFeedbackPartDialogBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val userJson = Singleton_t_user.getInstance(requireContext()).jsonObject

        // ------! RV checkbox 취합 시작 !------
        binding.btnFPDFinish.setOnClickListener {
//            viewModel.parts.value //
            dismiss()
            Log.v("VM>part", "${viewModel.feedbackParts.value}")

            // ------! db 전송 시작 !------
            val partsList = mutableListOf<String>()
            for (i in 0 until viewModel.parts.value?.size!!) {
                partsList.add(viewModel.feedbackParts.value!![i].partName)
            }
            userJson?.optString("user_mobile")
//            insertMeasurePartsByJson(getString(R.string.IP_ADDRESS_t_favorite),)




            // ------! db 전송 끝 !------

        } // ------! RV checkbox 취합 끝 !------

        binding.ibtnFPDBack.setOnClickListener { dismiss() }


        // ------! 부위 빨갛게 시작 !------
        setPartCheck(binding.cbFPDNeck, binding.ivFPDNeck)
        setPartCheck(binding.cbFPDShoulder, binding.ivFPDShoulder)
        setPartCheck(binding.cbFPDWrist, binding.ivFPDWrist)
        setPartCheck(binding.cbFPDStomach, binding.ivFPDStomach)
        setPartCheck(binding.cbFPDHipJoint, binding.ivFPDHipJoint)
        setPartCheck(binding.cbFPDKnee, binding.ivFPDKnee)
        setPartCheck(binding.cbFPDAnkle, binding.ivFPDAnkle)

    }
    // 체크 연동
    fun setPartCheck(cb:CheckBox, iv: ImageView) {
        // ------! 기존 데이터 받아서 쓰기 !------
//        val part = Triple(cb.text.toString(), cb.text.toString(), cb.isChecked)
        val part = MeasureVO(
            partName = cb.text.toString(),
            select = cb.isChecked,
            drawableName = "",
            anglesNDistances = JSONObject()
        )
        cb.isChecked = viewModel.feedbackParts.value?.contains(part) == true

        cb.setOnCheckedChangeListener { buttonView, isChecked ->
            when (isChecked) {
                true -> {
                    iv.setImageResource(R.drawable.drawable_select_part_enabled)
                    viewModel.addFeedbackPart(part)
                }
                else -> {
                    iv.setImageResource(R.drawable.drawable_select_part_disabled)
                    viewModel.deleteFeedbackPart(part)
                }
            }
        }
    }

//    fun setvmPart(cb: CheckBox, iv: ImageView) {
//        val enabledPart = viewModel.feedbackParts.value?.find { it.partName == cb.text }
//        if (enabledPart != null) {
//            cb.isEnabled = true
//            setPartCheck(cb, iv)
//        } else {
//            cb.isEnabled = false
//            setPartCheck(cb, iv)
//        }
//    }

    override fun onResume() {
        super.onResume()
        // full Screen code
        dialog?.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog?.window?.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
    }

    override fun onPartCheck(part: MeasureVO) {
        if (part.select) {
            viewModel.addFeedbackPart(part)
            Log.v("viewModel.part", "${viewModel.parts.value}")
        } else {
            viewModel.deleteFeedbackPart(part)
        }
    }
//    private fun setAnimation(view: View, onOff: Boolean) {
//        if (onOff) view.visibility = View.VISIBLE
//        val animator = ObjectAnimator.ofFloat(view, "alpha", if (onOff) 0f else 1f, if (onOff) 1f else 0f)
//        animator.duration = 300
//        animator.addListener(object: AnimatorListenerAdapter() {
//            override fun onAnimationEnd(animation: Animator) {
//                super.onAnimationEnd(animation)
//                view.visibility = if (onOff) View.VISIBLE else View.INVISIBLE
//            }
//
//            override fun onAnimationStart(animation: Animator) {
//                super.onAnimationStart(animation)
//
//            }
//        })
//        animator.start()
//    }

}