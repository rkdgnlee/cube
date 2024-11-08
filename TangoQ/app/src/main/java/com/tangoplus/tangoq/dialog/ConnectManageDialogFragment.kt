package com.tangoplus.tangoq.dialog

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.tangoplus.tangoq.adapter.StringRVAdapter
import com.tangoplus.tangoq.data.UserViewModel
import com.tangoplus.tangoq.databinding.FragmentConnectManageDialogBinding
import com.tangoplus.tangoq.listener.OnDisconnectListener
import com.tangoplus.tangoq.`object`.Singleton_t_user
import org.json.JSONObject


class ConnectManageDialogFragment : DialogFragment(), OnDisconnectListener {
    lateinit var binding : FragmentConnectManageDialogBinding
    private var userJson : JSONObject? = null
    private val uvm : UserViewModel by activityViewModels()
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentConnectManageDialogBinding.inflate(inflater)
        return  binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        userJson = Singleton_t_user.getInstance(requireContext()).jsonObject

        // ------# 연동된 기기 rv 연결 #------
        if (uvm.connectedCenters.isEmpty()) {
            uvm.connectedCenters.add(Pair("동천 탱고플러스 실버 하나로 센터 키오스크 B", "2024-11-06"))
            uvm.connectedCenters.add(Pair("탱고플러스 실버 하나로 센터 키오스크 A", "2024-11-07"))
            uvm.connectedCenters.add(Pair("광주 고령화 친화 사업장 탱고플러스 센터", "2023-12-13"))
        }
        setAdapter()
    }

    override fun onResume() {
        super.onResume()

        dialog?.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog?.window?.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
    }

    private fun setAdapter() {
        val layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
        val adapter = StringRVAdapter(this@ConnectManageDialogFragment, uvm.connectedCenters.map { it.first }.toMutableList(), "connect", uvm)
        adapter.onDisconnectListener = this@ConnectManageDialogFragment
        binding.rvCMD.layoutManager = layoutManager
        binding.rvCMD.adapter =  adapter
    }

    // TODO 연결해제에 필요한 API 필요
    override fun onDisconnect(title: String) {
        uvm.connectedCenters.remove(uvm.connectedCenters.find { it.first == title })
        setAdapter()
    }
}