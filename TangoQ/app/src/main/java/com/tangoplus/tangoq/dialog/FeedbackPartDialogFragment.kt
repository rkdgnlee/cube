package com.tangoplus.tangoq.dialog

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
import com.tangoplus.tangoq.listener.OnPartCheckListener
import com.tangoplus.tangoq.data.MeasureViewModel
import com.tangoplus.tangoq.databinding.FragmentFeedbackPartDialogBinding
import com.tangoplus.tangoq.`object`.NetworkMeasureService.insertMeasurePartsByJson
import com.tangoplus.tangoq.`object`.Singleton_t_user


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

        val t_userdata = Singleton_t_user.getInstance(requireContext())
        val userJson= t_userdata.jsonObject?.getJSONObject("data")
        // ------! RV checkbox 취합 시작 !------
        binding.btnPpFinish.setOnClickListener {
//            viewModel.parts.value // TODO 통증 부위 선택 후 번갈아가면서 넣기vh
            dismiss()
            Log.v("VM>part", "${viewModel.parts.value}")

            // ------! db 전송 시작 !------
            val partsList = mutableListOf<String>()
            for (i in 0 until viewModel.parts.value?.size!!) {
                partsList.add(viewModel.parts.value!![i].second)
            }
            userJson?.optString("user_mobile")
//            insertMeasurePartsByJson(getString(R.string.IP_ADDRESS_t_favorite),)





            // ------! db 전송 끝 !------

        } // ------! RV checkbox 취합 끝 !------

        binding.ibtnPpBack.setOnClickListener { dismiss() }


        // ------! 부위 빨갛게 시작 !------
        binding.ivPpNeck.visibility = View.GONE
        binding.ivPpShoulder.visibility = View.GONE
        binding.ivPpWrist.visibility = View.GONE
        binding.ivPpStomach.visibility = View.GONE
        binding.ivPpHipJoint.visibility = View.GONE
        binding.ivPpKnee.visibility = View.GONE
        binding.ivPpAnkle.visibility = View.GONE

        setPartCheck(binding.cbPpNeck, binding.ivPpNeck)
        setPartCheck(binding.cbPpShoulder, binding.ivPpShoulder)
        setPartCheck(binding.cbPpWrist, binding.ivPpWrist)
        setPartCheck(binding.cbPpStomach, binding.ivPpStomach)
        setPartCheck(binding.cbPpHipJoint, binding.ivPpHipJoint)
        setPartCheck(binding.cbPpKnee, binding.ivPpKnee)
        setPartCheck(binding.cbPpAnkle, binding.ivPpAnkle)

    }
    // 체크 연동
    fun setPartCheck(cb:CheckBox, iv: ImageView) {
        // ------! 기존 데이터 받아서 쓰기 !------
        val part = Triple(cb.text.toString(), cb.text.toString(), cb.isChecked)
        cb.isChecked = viewModel.parts.value?.contains(part) == true

        cb.setOnCheckedChangeListener { buttonView, isChecked ->
            when (isChecked) {
                true -> {
                    iv.visibility = View.VISIBLE
                    viewModel.addPart(Triple(cb.text.toString(), cb.text.toString(), true))
                }
                else -> {
                    iv.visibility = View.GONE
                    viewModel.deletePart(Triple(cb.text.toString(), cb.text.toString(), false))
                }
            }
        }
    }

    fun setvmPart(cb: CheckBox, iv: ImageView) {
        val enabledPart = viewModel.parts.value?.find { it.first == cb.text }
        if (enabledPart != null) {
            cb.isEnabled = true
            setPartCheck(cb, iv)
        } else {
            cb.isEnabled = false
            setPartCheck(cb, iv)
        }
    }

    override fun onResume() {
        super.onResume()
        // full Screen code
        dialog?.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog?.window?.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
    }

    override fun onPartCheck(part: Triple<String,String, Boolean>) {
        if (part.third) {
            viewModel.addPart(part)
            Log.v("viewModel.part", "${viewModel.parts.value}")
        } else {
            viewModel.deletePart(part)
        }
    }
}