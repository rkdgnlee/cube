package com.tangoplus.tangoq.adapter

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.skydoves.progressview.ProgressView
import com.tangoplus.tangoq.R
import com.tangoplus.tangoq.databinding.RvMainProgressItemBinding
import com.tangoplus.tangoq.dialog.ProgramCustomDialogFragment
import com.tangoplus.tangoq.fragment.ExtendedFunctions.setOnSingleClickListener
import com.tangoplus.tangoq.listener.OnSingleClickListener
import com.tangoplus.tangoq.viewmodel.ProgressViewModel
import com.tangoplus.tangoq.vo.RecommendationVO

class MainProgressRVAdapter(private val fragment: Fragment, private val recommends : List<RecommendationVO>, private val pvm : ProgressViewModel) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private val MAX_COLLAPSED_ITEMS = 3


    inner class MPViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvMPIName: TextView = view.findViewById(R.id.tvMPIName)
        val ivMPIThumbnail: ImageView = view.findViewById(R.id.ivMPIThumbnail)
        val clMPI : ConstraintLayout = view.findViewById(R.id.clMPI)
        val tvMPIPercent : TextView = view.findViewById(R.id.tvMPIPercent)
        val pvMPI: ProgressView = view.findViewById(R.id.pvMPI)
        val tvMPITime : TextView = view.findViewById(R.id.tvMPITime)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = RvMainProgressItemBinding.inflate(inflater, parent, false)
        return MPViewHolder(binding.root)
    }

    override fun getItemCount(): Int {
        return if (pvm.isExpanded || recommends.size <= MAX_COLLAPSED_ITEMS) {
            recommends.size
        } else {
            MAX_COLLAPSED_ITEMS
        }
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val currentItem = recommends[position]
        if (holder is MPViewHolder) {
            val title = replaceJointProgram(currentItem.title, keywords)
            holder.tvMPIName.text = title

            val partId = when {
                currentItem.title.contains("목관절") ->  1
                currentItem.title.contains("어깨") -> 2
                currentItem.title.contains("팔꿉") -> 3
                currentItem.title.contains("손목") -> 4
                currentItem.title.contains("고관절") -> 8
                currentItem.title.contains("무릎") ->  9
                currentItem.title.contains("발목") -> 10
                else -> -1
            }
            if (partId != -1) {
                val drawableResId = fragment.resources.getIdentifier("drawable_joint_$partId", "drawable", fragment.requireContext().packageName)
                holder.ivMPIThumbnail.setImageResource(drawableResId)
            }

            if (currentItem.totalProgress > 0) {
                val durationSet = (currentItem.totalProgress * 100).toFloat() / (currentItem.totalDuration * 12)
                holder.tvMPIPercent.text = "${durationSet.toInt()} %"
                holder.pvMPI.progress = durationSet
            } else {
                holder.tvMPIPercent.text = "0%"
                holder.pvMPI.progress = 0f
            }
            val second = "${currentItem.totalDuration.div(60)}분 ${currentItem.totalDuration.rem(60)}초"
            holder.tvMPITime.text = second

            // 선택
            holder.clMPI.setOnSingleClickListener {
                pvm.selectedRecProgress = (currentItem.totalProgress * 100).toFloat() / (currentItem.totalDuration * 12)
                Log.w("프로그램", "${currentItem.programSn}, ${currentItem.recommendationSn}")
                val dialog = ProgramCustomDialogFragment.newInstance(currentItem.programSn, currentItem.recommendationSn)
                dialog.show(fragment.requireActivity().supportFragmentManager, "ProgramCustomDialogFragment")
            }
        }
    }

    private val keywords = listOf("목관절", "어깨", "팔꿉", "손목", "고관절", "무릎", "발목")
    private fun replaceJointProgram(input: String, jointParts: List<String>): String {
        var result = input
        jointParts.forEach { part ->
            val modifyPart = if (part == "목관절") "목" else part
            result = result
                .replace("$modifyPart 부위 운동", "운동")
                .replace("증상을 위한", "")
                .replace("질환", "")
                .replace("운동프로그램", "운동 프로그램")
        }
        return result
    }
    fun toggleExpand(recyclerView: RecyclerView) {
        // 아이템 개수가 3개 이하면 펼치기 기능 비활성화
        if (recommends.size <= MAX_COLLAPSED_ITEMS) return

        // 현재 상태 토글
        pvm.isExpanded = !pvm.isExpanded

        // 높이 애니메이션 계산
        val startHeight = recyclerView.height
        val itemHeight = recyclerView.getChildAt(0).height
        val marginPixel = dpToPixels(20, fragment.requireContext())
        val endHeight = if (pvm.isExpanded) {
            ( itemHeight + marginPixel ) * recommends.size
        } else {
            ( itemHeight + marginPixel ) * MAX_COLLAPSED_ITEMS
        }

        if (pvm.isExpanded) {
            // 펼칠 때: 새로운 아이템들만 추가됨
            notifyItemRangeInserted(MAX_COLLAPSED_ITEMS, recommends.size - MAX_COLLAPSED_ITEMS)
        } else {
            // 접을 때: 추가된 아이템들만 제거됨
            notifyItemRangeRemoved(recommends.size, MAX_COLLAPSED_ITEMS - recommends.size)
        }
        // 애니메이션 설정
        val animator = ValueAnimator.ofInt(startHeight, endHeight)
        animator.addUpdateListener { valueAnimator ->
            recyclerView.layoutParams.height = valueAnimator.animatedValue as Int
            recyclerView.requestLayout()
        }
        animator.duration = 300
        animator.start()
    }

    fun dpToPixels(dp: Int, context: Context): Int {
        val displayMetrics = context.resources.displayMetrics
        return (dp * displayMetrics.density).toInt()
    }
}