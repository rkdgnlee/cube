package com.tangoplus.tangoq.Dialog

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.CheckBox
import android.widget.ImageView
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.tangoplus.tangoq.Listener.OnPartCheckListener
import com.tangoplus.tangoq.R
import com.tangoplus.tangoq.ViewModel.MeasureViewModel
import com.tangoplus.tangoq.databinding.FragmentMeasurePainPartDialogBinding


class MeasurePainPartDialogFragment : DialogFragment(), OnPartCheckListener {
    lateinit var binding : FragmentMeasurePainPartDialogBinding
    val viewModel : MeasureViewModel by activityViewModels()
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentMeasurePainPartDialogBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

//        val ppList = mutableListOf<Pair<String, String>>()
//        ppList.add(Pair("drawable_pain1", "손목"))
//        ppList.add(Pair("drawable_pain2", "척추"))
//        ppList.add(Pair("drawable_pain3", "팔꿉"))
//        ppList.add(Pair("drawable_pain4", "목"))
//        ppList.add(Pair("drawable_pain5", "발목"))
//        ppList.add(Pair("drawable_pain6", "어깨"))
//        ppList.add(Pair("drawable_pain7", "무릎"))
//        ppList.add(Pair("drawable_pain8", "복부"))
//
//        val adapter = PainPartRVAdpater(ppList, "selectPp",this@MeasurePainPartDialogFragment)
//        binding.rvPp.adapter = adapter
//        val linearLayoutManager =
//            LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
//        binding.rvPp.layoutManager = linearLayoutManager
//
        // ------! RV checkbox 취합 시작 !------
        binding.btnPpSet.setOnClickListener {
//            viewModel.parts.value // TODO 통증 부위 선택 후 번갈아가면서 넣기vh
            dismiss()
            Log.v("VM>part", "${viewModel.parts.value}")
        } // ------! RV checkbox 취합 끝 !------




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
        val part = Pair(cb.text.toString(), cb.text.toString())
        cb.isChecked = viewModel.parts.value?.contains(part) == true

        cb.setOnCheckedChangeListener { buttonView, isChecked ->
            when (isChecked) {
                true -> {
                    iv.visibility = View.VISIBLE
                    viewModel.addPart(Pair(cb.text.toString(), cb.text.toString()))
                }
                else -> {
                    iv.visibility = View.GONE
                    viewModel.deletePart(Pair(cb.text.toString(), cb.text.toString()))
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

    override fun onPartCheck(part: Pair<String,String>, checked: Boolean) {
        if (checked) {
            viewModel.addPart(part)
            Log.v("viewModel.part", "${viewModel.parts.value}")
        } else {
            viewModel.deletePart(part)
        }
    }
}