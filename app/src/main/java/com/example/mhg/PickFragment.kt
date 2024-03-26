package com.example.mhg

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.mhg.Adapter.PickRecyclerViewAdapter
import com.example.mhg.VO.ExerciseViewModel
import com.example.mhg.databinding.FragmentPickBinding
import com.example.mhg.`object`.Singleton_t_user


class PickFragment : Fragment(), onPickDetailClickListener {
    lateinit var binding : FragmentPickBinding
    val viewModel : ExerciseViewModel by activityViewModels()
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentPickBinding.inflate(inflater)

        // -----! viewmodel의 list관리 시작 !-----
        val pickList = mutableListOf<String>()
        viewModel.pickList.observe(viewLifecycleOwner) { jsonArray ->
            pickList.clear()
            for (i in 0 until jsonArray.length()) {
                val pickObject = jsonArray.getJSONObject(i)
                pickList.add(pickObject.getString("pickName"))
            }
        }
        // -----! viewmodel의 list관리 끝 !-----




        val PickRecyclerViewAdapter = PickRecyclerViewAdapter(pickList, this, requireActivity())
        binding.rvPick.adapter = PickRecyclerViewAdapter
        val linearLayoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
        binding.rvPick.layoutManager = linearLayoutManager


        binding.btnPickAdd.setOnClickListener {
            viewModel.exerciseUnits.value?.clear()
            requireActivity().supportFragmentManager.beginTransaction().apply {
                setCustomAnimations(R.anim.slide_in_left, R.anim.slide_in_right)
                replace(R.id.flPick, PickAddFragment())
                addToBackStack(null)
                commit()
            }
        }

        return binding.root
    }
    override fun onPickClick(title: String) {
        requireActivity().supportFragmentManager.beginTransaction().apply {
            replace(R.id.flMain, PickDetailFragment.newInstance(title))
            addToBackStack(null)
            commit()

        }
    }

}