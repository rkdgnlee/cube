package com.tangoplus.tangoq.dialog

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.tangoplus.tangoq.adapter.ExtendedRVAdapter
import com.tangoplus.tangoq.adapter.ImageRVAdapter
import com.tangoplus.tangoq.data.MeasureViewModel
import com.tangoplus.tangoq.data.UserViewModel
import com.tangoplus.tangoq.databinding.FragmentMainPartDialogBinding
import com.tangoplus.tangoq.listener.OnCategoryScrollListener


class MainPartDialogFragment : DialogFragment(), OnCategoryScrollListener {
    lateinit var binding : FragmentMainPartDialogBinding
    val uvm : UserViewModel by activityViewModels()
    val mvm : MeasureViewModel by activityViewModels()
    private lateinit var part: String

    companion object {
        private const val ARG_PART = "arg_part"
        fun newInstance(part: String): MainPartDialogFragment {
            val fragment = MainPartDialogFragment()
            val args = Bundle()
            args.putString(ARG_PART, part)
            fragment.arguments = args
            return fragment
        }
    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentMainPartDialogBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        part = arguments?.getString(ARG_PART) ?: ""
        Log.v("파트", "part: $part")
        binding.tvMPD.text = "위험 부위 분석 - $part"

        val relatedUris = mutableListOf<Pair<String, Int>>()
        for (i in matchedUris.get(part)!!) {
            relatedUris.add(Pair(mvm.selectedMeasure?.fileUris!![i], i))
        }
        val layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        val adapter = ImageRVAdapter(this@MainPartDialogFragment, relatedUris)
        binding.rvMPDHorizontal.layoutManager = layoutManager
        binding.rvMPDHorizontal.adapter = adapter

        val sequences = relatedUris.map { it.second }.map { matchedSeq[it] }.toMutableList()
        val layoutManager2 = LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
        val adapter2 = ExtendedRVAdapter(this@MainPartDialogFragment, sequences, this@MainPartDialogFragment)
        binding.rvMPD.layoutManager = layoutManager2
        binding.rvMPD.adapter = adapter2
    }

    override fun onResume() {
        super.onResume()
        // full Screen code
        dialog?.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog?.window?.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
    }

    private val matchedUris = mapOf(
        "목관절" to listOf(0, 3, 4, 5, 6),
        "우측 어깨" to listOf(0, 1, 3, 4, 5, 6),
        "좌측 어깨" to listOf(0, 1, 3, 4, 5, 6),
        "우측 팔꿉" to listOf(0, 2, 3, 4),
        "좌측 팔꿉" to listOf(0, 2, 3, 4),
        "우측 손목" to listOf(0, 2, 3, 4),
        "좌측 손목" to listOf(0, 2, 3, 4),
        "우측 골반" to listOf(0, 3, 4, 5, 6),
        "좌측 골반" to listOf(0, 3, 4, 5, 6),
        "우측 무릎" to listOf(0, 1, 5),
        "좌측 무릎" to listOf(0, 1, 5),
        "우측 발목" to listOf(0, 1, 3, 4, 5),
        "좌측 발목" to listOf(0, 1, 3, 4, 5)
    )

    private val matchedSeq = mapOf(
        0 to "정면 측정",
        1 to "동적 측정",
        2 to "팔꿉 측정",
        3 to "좌측 측정",
        4 to "우측 측정",
        5 to "후면 측정",
        6 to "앉아 후면"
    )

    override fun categoryScroll(cl: ConstraintLayout) {
        scrollToView(cl)
    }
    private fun scrollToView(view: View) {
        // 1 뷰의 위치를 저장할 배열 생성
        val location = IntArray(2)
        // 2 뷰의 위치를 'window' 기준으로 계산 후 배열 저장
        view.getLocationInWindow(location)
        val viewTop = location[1]
        // 3 스크롤 뷰의 위치를 저장할 배열 생성
        val scrollViewLocation = IntArray(2)

        // 4 스크롤 뷰의 위치를 'window' 기준으로 계산 후 배열 저장
        binding.nsvMPD.getLocationInWindow(scrollViewLocation)
        val scrollViewTop = scrollViewLocation[1]
        // 5 현재 스크롤 뷰의 스크롤된 y 위치 가져오기
        val scrollY = binding.nsvMPD.scrollY
        // 6 스크롤할 위치 계산
        //    현재 스크롤 위치 + 뷰의 상대 위치 = 스크롤 위치 계산
        val scrollTo = scrollY + viewTop - scrollViewTop
        // 7 스크롤 뷰 해당 위치로 스크롤
        binding.nsvMPD.smoothScrollTo(0, scrollTo)
    }
}