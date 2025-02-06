package com.tangoplus.tangoq.adapter

import android.annotation.SuppressLint
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
import com.tangoplus.tangoq.listener.OnSingleClickListener
import com.tangoplus.tangoq.vo.RecommendationVO

class MainProgressRVAdapter(private val fragment: Fragment, private val recommends : List<RecommendationVO>) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    inner class MPViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvMPIName: TextView = view.findViewById(R.id.tvMPIName)
        val ivMPIThumbnail: ImageView = view.findViewById(R.id.ivMPIThumbnail)
        val clMPI : ConstraintLayout = view.findViewById(R.id.clMPI)
        val tvMPIPercent : TextView = view.findViewById(R.id.tvMPIPercent)
        val pvMPI: ProgressView = view.findViewById(R.id.pvMPI)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = RvMainProgressItemBinding.inflate(inflater, parent, false)
        return MPViewHolder(binding.root)
    }

    override fun getItemCount(): Int {
        return recommends.size
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

            // progress bar 넣기
            holder.tvMPIPercent.text = "0%"
            holder.pvMPI.progress = 0f

            // 선택
            holder.clMPI.setOnSingleClickListener {
                val dialog = ProgramCustomDialogFragment.newInstance(currentItem.programSn, currentItem.recommendationSn)
                dialog.show(fragment.requireActivity().supportFragmentManager, "ProgramCustomDialogFragment")
            }
        }
    }
    private fun View.setOnSingleClickListener(action: (v: View) -> Unit) {
        val listener = View.OnClickListener { action(it) }
        setOnClickListener(OnSingleClickListener(listener))
    }
    private val keywords = listOf("목관절", "어깨", "팔꿉", "손목", "고관절", "무릎", "발목")
    fun replaceJointProgram(input: String, jointParts: List<String>): String {
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
}