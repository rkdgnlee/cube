package com.example.mhg

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.mhg.Adapter.HomeVerticalRecyclerViewAdapter
import com.example.mhg.VO.ExerciseViewModel
import com.example.mhg.databinding.FragmentPickDetailBinding
import com.example.mhg.`object`.NetworkService.fetchPickItemJsonById
import com.example.mhg.`object`.Singleton_t_user
import kotlinx.coroutines.launch


class PickDetailFragment : Fragment() {
    lateinit var binding : FragmentPickDetailBinding
    val viewModel : ExerciseViewModel by activityViewModels()
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

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentPickDetailBinding.inflate(inflater)
        return binding.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.nsvPickDetail.isNestedScrollingEnabled = true
        binding.rvPickDetail.isNestedScrollingEnabled = false
        binding.rvPickDetail.overScrollMode = View.OVER_SCROLL_NEVER

        val title = requireArguments().getString(ARG_TITLE).toString()
        binding.actPickDetail.setText(title)
        val t_userData = Singleton_t_user.getInstance(requireContext())

//        // ----- 운동 picklist 가져오기 시작 -----

        val pickList = mutableListOf<String>()
        viewModel.pickList.observe(viewLifecycleOwner) { jsonArray ->
            pickList.clear()
            for (i in 0 until jsonArray.length()) {
                val pickObject = jsonArray.getJSONObject(i)
                pickList.add(pickObject.getString("pickName"))
                Log.w("PickDetail>pickList", "${pickList}")
            }

            val currentItem = viewModel.pickItems.value?.find { it.pickName == title }
            binding.tvPickDetailExplainTitle.text = currentItem?.pickExplainTitle.toString()
            binding.tvPickDetailExplain.text = currentItem?.pickExplain.toString()
            val RvAdapter = HomeVerticalRecyclerViewAdapter(currentItem?.exercises!!, "add")
            RvAdapter.verticalList = currentItem.exercises
            val linearLayoutManager2 =
                LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
            binding.rvPickDetail.layoutManager = linearLayoutManager2
            binding.rvPickDetail.adapter = RvAdapter
        }
        val adapter = ArrayAdapter(requireContext(), androidx.appcompat.R.layout.support_simple_spinner_dropdown_item, pickList)
        binding.actPickDetail.setAdapter(adapter)
        binding.actPickDetail.addTextChangedListener(object: TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {  }
            override fun afterTextChanged(s: Editable?) {
                val currentItem = viewModel.pickItems.value?.get(0)
                binding.tvPickDetailExplainTitle.text = currentItem?.pickExplainTitle.toString()
                binding.tvPickDetailExplain.text = currentItem?.pickExplain.toString()
                val RvAdapter = HomeVerticalRecyclerViewAdapter(currentItem?.exercises!!, "add")
                RvAdapter.verticalList = currentItem.exercises
                val linearLayoutManager2 =
                    LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
                binding.rvPickDetail.layoutManager = linearLayoutManager2
                binding.rvPickDetail.adapter = RvAdapter
            }
        })
        // ----- 운동 즐겨찾기 리스트 가져오기 끝 -----


        // -----! 즐겨찾기로 운동 시작 !-----
        binding.btnPickStart.setOnClickListener {

            val resourceList : List<String>

            val intent = Intent(requireContext(), PlayFullScreenActivity::class.java)
            intent.putExtra("pick_list", "운동 리스트 넣어야 할 자리 (url) 로")
//            startActivity(intent)
        }

        // -----! 즐겨찾기로 운동 끝 !-----

        binding.btnPickDetailBack.setOnClickListener {
            if (!it.isClickable) { return@setOnClickListener }
            it.isClickable = false
            requireActivity().supportFragmentManager.beginTransaction().apply {
                replace(R.id.flMain, PickFragment())
                commit()
            }
            it.isClickable = true
        }
    }

}
