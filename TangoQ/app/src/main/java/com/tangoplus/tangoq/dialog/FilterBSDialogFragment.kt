package com.tangoplus.tangoq.dialog

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.tangoplus.tangoq.R
import com.tangoplus.tangoq.adapter.FilterRVAdapter
import com.tangoplus.tangoq.adapter.ProgramRVAdapter
import com.tangoplus.tangoq.data.ProgramViewModel
import com.tangoplus.tangoq.databinding.FragmentFilterBSDialogBinding
import com.tangoplus.tangoq.listener.OnFilterSelectedListener
import org.json.JSONArray
import org.json.JSONObject


class FilterBSDialogFragment : BottomSheetDialogFragment(), OnFilterSelectedListener {
    lateinit var binding : FragmentFilterBSDialogBinding
    lateinit var filterBig: String
    val viewModel : ProgramViewModel by activityViewModels()
    var choices = mutableListOf<String>()
    private lateinit var adapter: FilterRVAdapter

    companion object {
        private const val ARG_FILTER = "filterBig"
        fun newInstance(filterBig: String) : FilterBSDialogFragment {
            val fragment = FilterBSDialogFragment()
            val args = Bundle()
            args.putString(ARG_FILTER, filterBig)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentFilterBSDialogBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        filterBig = requireArguments().getString(ARG_FILTER).toString()
        Log.v("filter", filterBig)

        binding.tvPBSName.text = filterBig
        binding.ibtnPBSExit.setOnClickListener { dismiss() }

        when (filterBig) {
            "재활 부위 선택" -> {
                choices.add("목")
                choices.add("어깨")
                choices.add("팔뚝")
                choices.add("골반(척추)")
                choices.add("무릎")
                setAdapter(choices, 1)
                Log.v("choices", "${choices}")
                binding.ivFBSDLogo.setImageDrawable(ContextCompat.getDrawable(requireContext(), R.drawable.icon_stage_2))
            }
            "제외 운동" -> {
                choices.add("고강도 체력 소모 프로그램")
                choices.add("고강도 근력 운동 프로그램")
                choices.add("장시간 운동 프로그램")
                choices.add("기구가 필요한 운동")
                setAdapter(choices, 2)
                binding.ivFBSDLogo.setImageDrawable(ContextCompat.getDrawable(requireContext(), R.drawable.icon_stage_1))
                Log.v("choices", "${choices}")
            }
            "운동 난이도 설정" -> {
                choices.add("초급자")
                choices.add("중급자")
                choices.add("상급자")
                setAdapter(choices, 3)
                binding.ivFBSDLogo.setImageDrawable(ContextCompat.getDrawable(requireContext(), R.drawable.icon_stage_3))
                Log.v("choices", "${choices}")
            }
            "보유 기구 설정" -> {
                choices.add("라텍스 밴드")
                choices.add("아령(덤벨)")
                choices.add("역기(바벨)")
                setAdapter(choices, 4)
                binding.ivFBSDLogo.setImageDrawable(ContextCompat.getDrawable(requireContext(), R.drawable.icon_5_forward))
                Log.v("choices", "${choices}")
            }
            "운동 장소(가능 자세)" -> {
                choices.add("가정(누워서도 가능)")
                choices.add("야외(서서만 가능)")
                choices.add("회사(앉아서만 가능)")
                binding.ivFBSDLogo.setImageDrawable(ContextCompat.getDrawable(requireContext(), R.drawable.icon_paper))
                setAdapter(choices, 5)
                Log.v("choices", "${choices}")
            }
        }
        binding.btnFBSDOff.setOnClickListener {
             viewModel.allFalseInFilter(filterBig)
            uncheckedAllFilters()
        }
        binding.btnFBSDFinish.setOnClickListener { dismiss() }

    }

    override fun onFilterSelected(filterMiddle: String) {
        when (filterBig) {
            "재활 부위 선택" -> {}
            "제외 운동" -> {
                val updatedJson = viewModel.filter2.value ?: JSONObject()
                updatedJson.put(filterBig, filterMiddle)
                viewModel.filter2.value = updatedJson
                Log.v("choices", "viewModel.filterMiddle.value: ${viewModel.filter2.value}")
            }
            "운동 난이도 설정" -> {
                val updatedJson = viewModel.filter3.value ?: JSONObject()
                updatedJson.put(filterBig, filterMiddle)
                viewModel.filter3.value = updatedJson
                Log.v("choices", "viewModel.filter3.value: ${viewModel.filter3.value}")
            }
            "보유 기구 설정" -> {
                val updatedJson = viewModel.filter4.value ?: JSONObject()
                updatedJson.put(filterBig, filterMiddle)
                viewModel.filter4.value = updatedJson
                Log.v("choices", "viewModel.filter4.value: ${viewModel.filter4.value}")
            }
            "운동 장소(가능 자세)" -> {
                val updatedJson = viewModel.filter5.value ?: JSONObject()
                updatedJson.put(filterBig, filterMiddle)
                viewModel.filter5.value = updatedJson
                Log.v("choices", "viewModel.filter5.value: ${viewModel.filter5.value}")
            }
        }


    }

    private fun setAdapter(choices: MutableList<String>, filterNumber: Int) {
        val linearLayoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
        binding.rvPBS.layoutManager = linearLayoutManager
        adapter = FilterRVAdapter(this@FilterBSDialogFragment, choices, this@FilterBSDialogFragment, filterNumber, viewModel)
        binding.rvPBS.adapter = adapter
    }

    fun uncheckedAllFilters() {
        adapter.uncheckAllItems()
    }


}