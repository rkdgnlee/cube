package com.example.mhg

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.mhg.Adapter.PickRecyclerViewAdapter
import com.example.mhg.VO.RoutingVO
import com.example.mhg.databinding.FragmentPickBinding
import com.google.android.gms.dynamic.SupportFragmentWrapper


class PickFragment : Fragment(), onPickDetailClickListener {
    lateinit var binding : FragmentPickBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentPickBinding.inflate(inflater)

        val pickList = mutableListOf(
            RoutingVO("기본 운동 루틴1", "pick_detail"),
            RoutingVO("몸풀기 루틴", "pick_detail"),
            RoutingVO("운동 마무리", "pick_detail"),
            RoutingVO("인터벌", "pick_detail"),

        )
        val PickRecyclerViewAdapter = PickRecyclerViewAdapter(pickList, this)
        binding.rvPick.adapter = PickRecyclerViewAdapter
        val linearLayoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
        binding.rvPick.layoutManager = linearLayoutManager


        binding.btnPickAdd.setOnClickListener {
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