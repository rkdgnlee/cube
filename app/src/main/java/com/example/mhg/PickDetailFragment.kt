package com.example.mhg

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.core.widget.addTextChangedListener
import com.example.mhg.databinding.FragmentPickDetailBinding


class PickDetailFragment : Fragment() {
    lateinit var binding : FragmentPickDetailBinding
    companion object {
        private const val ARG_TITLE = "title"
        fun newInstance(title: String): PickDetailFragment {
            val fragment = PickDetailFragment()
            val args = Bundle()
            args.putString(ARG_TITLE, title)
            fragment.arguments = args
            return fragment
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentPickDetailBinding.inflate(inflater)
        return binding.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val title = requireArguments().getString(ARG_TITLE).toString()
        binding.actPickDetail.setText(title)

        // ----- 운동 즐겨찾기 리스트 가져오기 시작 -----


        // ----- 운동 즐겨찾기 리스트 가져오기 끝 -----

        val pickList = mutableListOf(
            title, "몸풀기 루틴", "운동 마무리", "인터벌"   // 즐겨찾기 추가 해야 함
        )
        val adapter = ArrayAdapter(requireContext(), androidx.appcompat.R.layout.support_simple_spinner_dropdown_item, pickList)
        binding.actPickDetail.setAdapter(adapter)
//        binding.actPickDetail.addTextChangedListener(object: TextWatcher {
//            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
//                TODO("Not yet implemented")
//            }
//
//            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
//                TODO("Not yet implemented")
//            }
//
//            override fun afterTextChanged(s: Editable?) {
//                TODO("Not yet implemented")
//            }
//        })


    }

}
