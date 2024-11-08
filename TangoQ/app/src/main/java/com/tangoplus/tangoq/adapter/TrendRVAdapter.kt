package com.tangoplus.tangoq.adapter

import android.graphics.Bitmap
import android.graphics.Matrix
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool
import com.bumptech.glide.load.resource.bitmap.BitmapTransformation
import com.skydoves.progressview.ProgressView
import com.tangoplus.tangoq.R
import com.tangoplus.tangoq.data.AnalysisVO
import com.tangoplus.tangoq.databinding.RvMeasureTrendItemBinding
import com.tangoplus.tangoq.mediapipe.CalculateUtil.calculateBoundedScore
import java.security.MessageDigest

class TrendRVAdapter(private val fragment: Fragment, private val analyzes1: MutableList<MutableList<AnalysisVO>>?, private val analyzes2: MutableList<MutableList<AnalysisVO>>?) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    inner class TrendViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvMTIPart : TextView = view.findViewById(R.id.tvMTIPart)
        val ivMTI : ImageView = view.findViewById(R.id.ivMTI)
        val tvMTI1 : TextView = view.findViewById(R.id.tvMTI1)
        val tvMTI2 : TextView = view.findViewById(R.id.tvMTI2)
        val tvMTI3 : TextView = view.findViewById(R.id.tvMTI3)
        val tvMTISeq1 : TextView = view.findViewById(R.id.tvMTISeq1)
        val tvMTISeq2 : TextView = view.findViewById(R.id.tvMTISeq2)
        val tvMTISeq3 : TextView = view.findViewById(R.id.tvMTISeq3)
        val pvMTI1Left : ProgressView = view.findViewById(R.id.pvMTI1Left)
        val pvMTI1Right : ProgressView = view.findViewById(R.id.pvMTI1Right)
        val pvMTI2Left : ProgressView = view.findViewById(R.id.pvMTI2Left)
        val pvMTI2Right : ProgressView = view.findViewById(R.id.pvMTI2Right)
        val pvMTI3Left : ProgressView = view.findViewById(R.id.pvMTI3Left)
        val pvMTI3Right : ProgressView = view.findViewById(R.id.pvMTI3Right)

        val tvSeqs = listOf(tvMTISeq1, tvMTISeq2, tvMTISeq3)
        val pvs = listOf(listOf(pvMTI1Left, pvMTI1Right), listOf(pvMTI2Left, pvMTI2Right), listOf(pvMTI3Left, pvMTI3Right))
        val tvPoses = listOf(tvMTI1, tvMTI2, tvMTI3)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = RvMeasureTrendItemBinding.inflate(inflater, parent, false)
        return TrendViewHolder(binding.root)
    }

    override fun getItemCount(): Int {
        return analyzes2?.size ?: 0
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is TrendViewHolder) {
            if (analyzes2 != null) {
                val currentItem = analyzes2.get(position) // analysisVO가 들어가있는 부위 별임.
                holder.tvMTIPart.text = matchedParts[position]
                /* 1. analysisVO를 통해 관절별 -> seq를 기준으로 끊을 수 있음 그 안에 어차피 AnalysisUnitVO가 있어서 낱개로 접근가능함.
                *  2. 안에 있는 analysisUnitVO를 통해서 그래프 값을 넣을 수 있음.
                */

                val selectAnalysisIndex = matchedIndexs[position]
                selectAnalysisIndex.forEachIndexed { index, indexTriple -> // 3, 5, 5 //  0, 0, 1
                    holder.tvSeqs[index].text = setSeqString(indexTriple.first)

                    val unit = currentItem.get(indexTriple.second).labels.get(indexTriple.third)
                    Log.v("각 점수들", "측정명: ${unit.rawDataName}, 값: ${unit.rawData}, 범위: ${unit.rawDataBound}")
                    holder.pvs[index][1].progress = calculateBoundedScore(unit.rawData, unit.rawDataBound)
                    holder.tvPoses[index].text = unit.rawDataName
                }
            }
        }
    }

    val matchedParts = listOf(
        "목관절" , "좌측 어깨", "우측 어깨", "좌측 팔꿉", "우측 팔꿉", "좌측 손목" , "우측 손목" , "좌측 골반", "우측 골반" , "좌측 무릎" , "우측 무릎" , "좌측 발목", "우측 발목")

    val matchedIndexs = listOf(
        listOf(Triple(3,1,0), Triple(6, 4, 0), Triple(6, 4, 1)),
        // 어깨
        listOf(Triple(0,0,0), Triple(3, 1, 0), Triple(6, 3, 0)),
        listOf(Triple(0,0,0), Triple(4, 1, 0), Triple(6, 3, 0)),
        // 팔꿉
        listOf(Triple(2,1,0), Triple(2, 1, 1), Triple(3, 2, 2)),
        listOf(Triple(2,1,0), Triple(2, 1, 1), Triple(4, 2, 2)),
        // 손목
        listOf(Triple(0,0,1), Triple(2, 2, 0), Triple(3, 1, 0)),
        listOf(Triple(0,0,1), Triple(2, 2, 0), Triple(4, 1, 0)),
        // 골반
        listOf(Triple(3,1,2), Triple(5, 2, 0), Triple(6, 3, 0)),
        listOf(Triple(4,1,2), Triple(5, 2, 0), Triple(6, 3, 0)),
        // 무릎
        listOf(Triple(0,0,0), Triple(0, 0, 1), Triple(5, 1, 1)),
        listOf(Triple(0,0,0), Triple(0, 0, 1), Triple(5, 1, 1)),

        listOf(Triple(0,0,2), Triple(5, 1, 0), Triple(5, 1, 1)),
        listOf(Triple(0,0,2), Triple(5, 1, 0), Triple(5, 1, 1)),
    )

    private fun setSeqString(seq: Int?) :String {
        return when (seq) {
            0 -> "정면 분석"
            1 -> "동적 분석"
            2 -> "팔꿉 분석"
            3 -> "왼쪽 측면"
            4 -> "오른쪽 측면"
            5 -> "후면 분석"
            6 -> "앉아 후면"
            else -> ""
        }
    }
}