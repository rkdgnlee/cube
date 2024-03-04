package com.example.mhg

import android.content.pm.PackageManager
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.mhg.Adapter.ProfileRecyclerViewAdapter
import com.example.mhg.VO.RoutingVO
import com.example.mhg.databinding.FragmentProfileBinding


class ProfileFragment : Fragment() {
    lateinit var binding : FragmentProfileBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentProfileBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        val profilemenulist=  mutableListOf<RoutingVO> (
            RoutingVO("개인정보", "1"),
            RoutingVO("설정", "2"),
            RoutingVO("모드설정", "3"),
            RoutingVO("기기관리", "4"),
            RoutingVO("1:1문의", "5"),
            RoutingVO("작성글 보기", "6"),
            RoutingVO("개인정보처리방침", "7"),
        )
        val adapter = ProfileRecyclerViewAdapter()
        adapter.profilemenulist = profilemenulist
        binding.rvProfile.adapter = adapter
        binding.rvProfile.layoutManager = LinearLayoutManager(context , LinearLayoutManager.VERTICAL, false)



        binding.btnImageEdit.setOnClickListener {
            when { ContextCompat.checkSelfPermission(requireContext(), android.Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED -> {

                }
            }
        }


    }
}