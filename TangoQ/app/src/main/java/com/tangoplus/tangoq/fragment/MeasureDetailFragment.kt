package com.tangoplus.tangoq.fragment

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import com.github.mikephil.charting.data.RadarData
import com.github.mikephil.charting.data.RadarDataSet
import com.github.mikephil.charting.data.RadarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.tangoplus.tangoq.MeasureSkeletonActivity
import com.tangoplus.tangoq.R
import com.tangoplus.tangoq.adapter.MeasureDetailRVAdapter
import com.tangoplus.tangoq.databinding.FragmentMeasureDetailBinding
import com.tangoplus.tangoq.dialog.AlarmDialogFragment
import com.tangoplus.tangoq.listener.OnReportClickListener
import org.json.JSONObject
import kotlin.random.Random


class MeasureDetailFragment : Fragment(), OnReportClickListener {
    lateinit var binding : FragmentMeasureDetailBinding
    lateinit var bodyParts : List<String>
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentMeasureDetailBinding.inflate(inflater)
        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.ibtnMDBack.setOnClickListener {
            requireActivity().supportFragmentManager.beginTransaction().apply {
                replace(R.id.flMain, MeasureHistoryFragment())
                addToBackStack(null)
                commit()
            }
        }
        binding.ibtnMDAlarm.setOnClickListener {
            val dialog = AlarmDialogFragment()
            dialog.show(requireActivity().supportFragmentManager, "AlarmDialogFragment")
        }


        // ------! 6각형 레이더 차트 시작 !------
        bodyParts = listOf("목", "우측어깨", "우측팔꿉", "우측골반", "우측무릎", "발목", "좌측무릎", "좌측골반", "좌측팔꿉", "좌측어깨")

        // 데이터 세트 생성
        val entries = generateRandomData()
        val dataSet = RadarDataSet(entries, "신체 부위").apply {
            color = resources.getColor(R.color.mainColor, null)
            fillColor = resources.getColor(R.color.mainColor, null)
            setDrawFilled(true)
            fillAlpha = 150  // 투명도 조절 (0-255)
            lineWidth = 2f
            setDrawValues(false)  // 값 표시 제거
        }
        val radarData = RadarData(dataSet)

        binding.rcMD.apply {
            data = radarData
            description.isEnabled = false
            legend.isEnabled = false
            webLineWidth = 0f
            webColor = resources.getColor(R.color.white, null)
            webAlpha = 255

            webLineWidthInner = 0f
            webColorInner = resources.getColor(R.color.subColor200, null)

            xAxis.apply {
                valueFormatter = IndexAxisValueFormatter(bodyParts)
                textColor = resources.getColor(R.color.subColor800, null)
                textSize = 12f  // 텍스트 크기 증가
                yOffset = 40f  // 텍스트를 차트에서 조금 더 멀리 배치
            }
            yAxis.setDrawLabels(false)
            yAxis.setDrawTopYLabelEntry(false)
            setTouchEnabled(false)
            data.setDrawValues(false)
            setDrawWeb(true)
            invalidate() // 차트 갱신
        }

        // ------# adapter 연결 #------
        val parts = mutableListOf<String>()
        parts.add("목")
        parts.add("어깨")
        parts.add("팔꿉")
        parts.add("골반(척추)")
        parts.add("무릎")
        setAdapter(JSONObject(), parts)



        binding.fabtnMD.setOnClickListener {
            val intent = Intent(requireContext(), MeasureSkeletonActivity::class.java)
            startActivity(intent)
        }
    }
    private fun generateRandomData(): List<RadarEntry> {
        return bodyParts.map { RadarEntry(Random.nextInt(0, 100).toFloat()) }
    }


    private fun setAdapter(jsonObj: JSONObject, parts: MutableList<String>) {
        val layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
        binding.rvMD.layoutManager = layoutManager
        val adapter = MeasureDetailRVAdapter(this@MeasureDetailFragment, jsonObj, parts, this@MeasureDetailFragment)
        binding.rvMD.adapter = adapter
    }

    override fun onReportScroll(view: View) {
        scrollToView(view)
    }

    private fun scrollToView(view: View) {
        val location = IntArray(2)
        view.getLocationInWindow(location)
        val viewTop = location[1]

        val scrollViewLocation = IntArray(2)
        binding.nsvMD.getLocationInWindow(scrollViewLocation)
        val scrollViewTop = scrollViewLocation[1]

        val scrollY = binding.nsvMD.scrollY
        val scrollTo = scrollY + viewTop - scrollViewTop

        binding.nsvMD.smoothScrollTo(0, scrollTo)
    }
}