package com.tangoplus.tangoq.fragment

import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.github.mikephil.charting.data.RadarData
import com.github.mikephil.charting.data.RadarDataSet
import com.github.mikephil.charting.data.RadarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.tangoplus.tangoq.MeasureSkeletonActivity
import com.tangoplus.tangoq.R
import com.tangoplus.tangoq.adapter.BalanceRVAdapter
import com.tangoplus.tangoq.data.MeasureVO
import com.tangoplus.tangoq.data.MeasureViewModel
import com.tangoplus.tangoq.databinding.FragmentMeasureDetailBinding
import com.tangoplus.tangoq.dialog.AlarmDialogFragment
import com.tangoplus.tangoq.listener.OnReportClickListener
import com.tangoplus.tangoq.`object`.Singleton_t_measure
import org.json.JSONObject
import kotlin.random.Random


class MeasureDetailFragment : Fragment() {
    lateinit var binding : FragmentMeasureDetailBinding
    lateinit var bodyParts : List<String>
    private lateinit var measure : MeasureVO
    private val viewModel : MeasureViewModel by activityViewModels()
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

        // ------# measure 에 맞게 UI 수정 #------
        measure = viewModel.selectedMeasure!!
        updateUI()

        // ------# 10각형 레이더 차트 #------
        val raderScores = mutableListOf<Float>()
        val scores = mutableListOf(2.245f, 3.41f, 2.54f, 3.97f, 2.34f, 5.25f, 4.12f, 3.84f, 3.12f, 4.97f)
        bodyParts = listOf("목", "우측어깨", "우측팔꿉", "우측골반", "우측무릎", "발목", "좌측무릎", "좌측골반", "좌측팔꿉", "좌측어깨")

        // ------# 순서대로 나오는 데이터 리스트에서 raderChart에 맞게 순서 변경하기 #------
        val indices = listOf(0, 1, 3, 5, 7, 9, 8, 6, 4, 2)
        for (i in indices) {
            val convertScores = calculateShoulderBalanceScore(scores[i], bodyParts[i])
            raderScores.add(convertScores.toFloat())
        }

        Log.v("raderScores", "$raderScores")

        val entries = mutableListOf<RadarEntry>()
        for (i in 0 until bodyParts.size) {
            entries.add(RadarEntry(raderScores[i]))
        }
        val dataSet = RadarDataSet(entries, "신체 부위").apply {
            color = resources.getColor(R.color.mainColor, null)
            fillColor = resources.getColor(R.color.mainColor, null)
            setDrawFilled(true)
//            fillAlpha = 80  // 투명도 조절 (0-255)
            lineWidth = 2f
            setDrawValues(false)  // 값 표시 제거
        }
        val radarData = RadarData(dataSet)

        binding.rcMD.apply {

            data = radarData
            description.isEnabled = false
            legend.isEnabled = false
            webLineWidth = 6f
            webColor = resources.getColor(R.color.white, null)
            webAlpha = 100
            webLineWidthInner = 1f
//            webColorInner = resources.getColor(R.color.subColor400, null)


            xAxis.apply {
                setDrawGridLines(true)
                valueFormatter = IndexAxisValueFormatter(bodyParts)
                textColor = resources.getColor(R.color.subColor400, null)
                textSize = 13f  // 텍스트 크기 증가
                yOffset = 0f  // 텍스트를 차트에서 조금 더 멀리 배치


            }
            yAxis.apply {
                setLabelCount(4, true)
                setDrawGridLines(true)
                setDrawLabels(false)
                setDrawTopYLabelEntry(false)
                setTouchEnabled(false)
                data.setDrawValues(false)
                setDrawWeb(true)
                axisMaximum = 100f
                axisMinimum = 0f

            }
            legend.apply {
                xOffset = 40f
                yOffset = 40f
            }
            invalidate() // 차트 갱신
        }

        // ------# adapter 연결 #------
//        // TODO 다 가져와서, 부위를 넣어야함-->
//        put("front_vertical_angle_shoulder_elbow_left")
//        put("front_vertical_angle_shoulder_elbow_right")
//        put("front_vertical_angle_elbow_wrist_left")
//        put("front_vertical_angle_elbow_wrist_right")
//        put("front_vertical_angle_hip_knee_left")
//        put("front_vertical_angle_hip_knee_right")
//        put("front_vertical_angle_knee_ankle_left")
//        put("front_vertical_angle_knee_ankle_right")
//        put("front_vertical_angle_shoulder_elbow_wrist_left")
//        put("front_vertical_angle_shoulder_elbow_wrist_right")
//        put("front_vertical_angle_hip_knee_ankle_left")
//        put("front_vertical_angle_hip_knee_ankle_right")
//
//
//        put("front_elbow_align_distance_left_wrist_shoulder")
//        put("front_elbow_align_distance_right_wrist_shoulder")
//        put("front_elbow_align_distance_wrist_height")
//        put("front_elbow_align_angle_left_shoulder_elbow_wrist")
//        put("front_elbow_align_angle_right_shoulder_elbow_wrist")
//        put("front_elbow_align_distance_center_wrist_left")
//        put("front_elbow_align_distance_center_wrist_right")
//
//        // 2. 측면
//        put("side_left_vertical_angle_shoulder_elbow")
//        put("side_left_vertical_angle_elbow_wrist")
//        put("side_left_vertical_angle_hip_knee")
//        put("side_left_vertical_angle_ear_shoulder")
//        put("side_left_vertical_angle_nose_shoulder")
//        put("side_left_vertical_angle_shoulder_elbow_wrist")
//        put("side_left_vertical_angle_hip_knee_ankle")
//
//        put("side_right_vertical_angle_shoulder_elbow")
//        put("side_right_vertical_angle_elbow_wrist")
//        put("side_right_vertical_angle_hip_knee")
//        put("side_right_vertical_angle_ear_shoulder")
//        put("side_right_vertical_angle_nose_shoulder")
//        put("side_right_vertical_angle_shoulder_elbow_wrist")
//        put("side_right_vertical_angle_hip_knee_ankle")
//
//        // 3. 후면
//        put("back_vertical_angle_nose_center_shoulder")
//        put("back_vertical_angle_shoudler_center_hip")
//        put("back_vertical_angle_nose_center_hip")
//        put("back_vertical_angle_knee_heel_left")
//        put("back_vertical_angle_knee_heel_right")
//        put("back_horizontal_distance_wrist_left")
//        put("back_horizontal_distance_wrist_right")
//
//        // 4. 스쿼트
//        put("ohs_front_horizontal_angle_mid_finger_tip")
//        put("ohs_front_horizontal_angle_hip")
//        put("ohs_front_horizontal_angle_knee")


        val stages = mutableListOf<MutableList<String>>()
        val balanceParts1 = mutableListOf("어깨", "골반")
        stages.add(balanceParts1)
        val balanceParts2 = mutableListOf("어깨", "팔꿉", "좌측 전완")
        stages.add(balanceParts2)
        val balanceParts3 = mutableListOf("골반", "좌측 어깨", "목")
        stages.add(balanceParts3)
        val balanceParts4 = mutableListOf("좌측 허벅지", "좌측 골반", "좌측 어깨")
        stages.add(balanceParts4)

        val degrees =  mutableListOf(Pair(1, 3), Pair(1,0), Pair(1, 2), Pair(0 , -4))
        setAdapter(stages, degrees)



        binding.fabtnMD.setOnClickListener {
            val intent = Intent(requireContext(), MeasureSkeletonActivity::class.java)
            startActivity(intent)
        }
    }

    private fun setAdapter(stages: MutableList<MutableList<String>>, degrees: MutableList<Pair<Int,Int>>) {
        val layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
        binding.rvMD.layoutManager = layoutManager
        val balanceAdapter = BalanceRVAdapter(this@MeasureDetailFragment, stages, degrees)
        binding.rvMD.adapter = balanceAdapter
    }

    private fun calculateShoulderBalanceScore(angle: Float, case: String): Int {
        val normalRange = when (case) {
            "목" -> 0.5
            "우측어깨", "좌측어깨","우측무릎", "좌측무릎" -> 1.0
            else -> 3.0
        }
        val deviationFromNormal = Math.abs(angle) - normalRange

        return when {
            deviationFromNormal <= 0 -> 100
            else -> (100 - (deviationFromNormal * 10)).toInt().coerceAtLeast(0)
        }
    }

    private fun updateUI() {
        binding.tvMDScore.text = measure.overall.toString()
        binding.tvMDDate.text = measure.regDate
        binding.tvMDParts.text = "우려부위: ${measure.dangerParts.map { it.first }.joinToString(", ")}"
    }
}