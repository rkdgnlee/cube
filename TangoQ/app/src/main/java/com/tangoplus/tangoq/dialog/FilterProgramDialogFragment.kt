package com.tangoplus.tangoq.dialog

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import com.skydoves.balloon.ArrowPositionRules
import com.skydoves.balloon.Balloon
import com.skydoves.balloon.BalloonAnimation
import com.skydoves.balloon.BalloonSizeSpec
import com.skydoves.balloon.showAlignBottom
import com.tangoplus.tangoq.R
import com.tangoplus.tangoq.data.ProgramViewModel
import com.tangoplus.tangoq.databinding.FragmentFilterProgramDialogBinding
import com.tangoplus.tangoq.db.PreferencesManager
import com.tangoplus.tangoq.fragment.isFirstRun
import org.json.JSONArray
import org.json.JSONObject


class FilterProgramDialogFragment : DialogFragment() {
    lateinit var binding : FragmentFilterProgramDialogBinding
    private val viewModel : ProgramViewModel by activityViewModels()
    lateinit var prefsManager : PreferencesManager
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentFilterProgramDialogBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.ibtnMFDBack.setOnClickListener { dismiss() }
        prefsManager = PreferencesManager(requireContext())

        // ------! balloon 시작 !------
        val balloon = Balloon.Builder(requireContext())
            .setWidthRatio(0.5f)
            .setHeight(BalloonSizeSpec.WRAP)
            .setText("내가 원하는 옵션의 운동을 추천 받을 수 있습니다.")
            .setTextColorResource(R.color.white)
            .setTextSize(15f)
            .setArrowPositionRules(ArrowPositionRules.ALIGN_ANCHOR)
            .setArrowSize(0)
            .setMargin(10)
            .setPadding(12)
            .setCornerRadius(8f)
            .setBackgroundColorResource(R.color.mainColor)
            .setBalloonAnimation(BalloonAnimation.OVERSHOOT)
            .setLifecycleOwner(viewLifecycleOwner)
            .build()

        if (isFirstRun("CustomExerciseDialogFragment_isFirstRun")) {
            balloon.dismissWithDelay(1800L)
        }
        binding.ibtnMFDInfo.showAlignBottom(balloon)

        binding.ibtnMFDInfo.setOnClickListener { it.showAlignBottom(balloon) }


        // ------! 기존 필터에서 가져오기 시작 !------
        val filterKeyMap = mapOf(
            viewModel.filter1 to "재활 부위 선택",
            viewModel.filter2 to "제외 운동",
            viewModel.filter3 to "운동 난이도 설정",
            viewModel.filter4 to "보유 기구 설정",
            viewModel.filter5 to "운동 장소(가능 자세)"
        )

        val filters = listOf(
            viewModel.filter1,
            viewModel.filter2,
            viewModel.filter3,
            viewModel.filter4,
            viewModel.filter5
        )
        for (filter in filters) {
            val key = filterKeyMap[filter] ?: continue
            when (filter) {
                viewModel.filter1 -> {
                    val value = prefsManager.getFilter(key)
                    val jsonArray = JSONArray()
                    if (value.isNotEmpty()) {
                        // 쉼표로 구분된 문자열로 가정하고 분리
                        value.split(",").forEach { jsonArray.put(it.trim()) }
                    }
                    filter.value = jsonArray
                }
                else -> {
                    val updatedJson = filter.value as JSONObject
                    updatedJson.put(key, prefsManager.getFilter(key))
                }
            }
        }
        // ------! 기존 필터에서 가져오기 끝 !------

        // ------! 각 명칭마다 bottomsheet 나오게 하기 !------
        binding.clMFDSelectPart.setOnClickListener { showBSDialog(binding.tvMFDSelectPart) }
        binding.clMFDStage.setOnClickListener { showBSDialog(binding.tvMFDStage) }
        binding.clMFDExcept.setOnClickListener { showBSDialog(binding.tvMFDExcept) }
        binding.clMFDTool.setOnClickListener { showBSDialog(binding.tvMFDTool) }
        binding.clMFDLocation.setOnClickListener { showBSDialog(binding.tvMFDLocation) }

        viewModel.filter1.observe(viewLifecycleOwner) { jsonArray ->
            val values = mutableListOf<String>()
            for (i in 0 until jsonArray.length()) {
                values.add(jsonArray.optString(i))
            }
            binding.tvMFDSelect1.text = values.joinToString(", ")

        }

        viewModel.filter2.observe(viewLifecycleOwner) {
            binding.tvMFDSelect2.text = it.optString("${binding.tvMFDExcept.text}")
        }
        viewModel.filter3.observe(viewLifecycleOwner) {
            binding.tvMFDSelect3.text = it.optString("${binding.tvMFDStage.text}")
        }
        viewModel.filter4.observe(viewLifecycleOwner) {
            binding.tvMFDSelect4.text = it.optString("${binding.tvMFDTool.text}")
        }
        viewModel.filter5.observe(viewLifecycleOwner) {
            binding.tvMFDSelect5.text = it.optString("${binding.tvMFDLocation.text}")
        }

        binding.btnMFDFilter.setOnClickListener{

            // ------! filter pref에 넣기 시작 !------
            prefsManager.storeFilter("${binding.tvMFDSelectPart.text}", "${binding.tvMFDSelect1.text}")
            prefsManager.storeFilter("${binding.tvMFDExcept.text}", "${binding.tvMFDSelect2.text}")
            prefsManager.storeFilter("${binding.tvMFDStage.text}", "${binding.tvMFDSelect3.text}")
            prefsManager.storeFilter("${binding.tvMFDTool.text}", "${binding.tvMFDSelect4.text}")
            prefsManager.storeFilter("${binding.tvMFDLocation.text}", "${binding.tvMFDSelect5.text}")
            // ------! filter pref에 넣기 끝 !------
            dismiss()
            val dialog = ProgramCustomDialogFragment()
            dialog.show(requireActivity().supportFragmentManager, "CustomExerciseDialogFragment")
        }
    }


    override fun onResume() {
        super.onResume()
        // full Screen code
        dialog?.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog?.window?.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
    }

    private fun showBSDialog(tv: TextView) {
        val dialog = FilterBSDialogFragment.newInstance(tv.text.toString())
        dialog.show(requireActivity().supportFragmentManager, "FilterBSDialogFragment")
    }


}