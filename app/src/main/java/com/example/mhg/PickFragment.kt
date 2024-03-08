package com.example.mhg

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.mhg.Adapter.PickRecyclerViewAdapter
import com.example.mhg.VO.RoutingVO
import com.example.mhg.databinding.FragmentPickBinding


class PickFragment : Fragment() {
    lateinit var binding : FragmentPickBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentPickBinding.inflate(inflater)

        val pickList = mutableListOf(
            RoutingVO("기본 운동 루틴1", ""),
            RoutingVO("몸풀기 루틴", ""),
            RoutingVO("운동 마무리", ""),
            RoutingVO("인터벌", "")
        )
        val PickRecyclerViewAdapter = PickRecyclerViewAdapter(pickList)
        binding.rvPick.adapter = PickRecyclerViewAdapter
        val linearLayoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
        binding.rvPick.layoutManager = linearLayoutManager




        return binding.root
    }

}