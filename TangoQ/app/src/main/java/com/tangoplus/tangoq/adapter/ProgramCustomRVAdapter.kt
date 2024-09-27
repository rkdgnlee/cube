package com.tangoplus.tangoq.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.tangoplus.tangoq.R
import com.tangoplus.tangoq.data.HistoryUnitVO
import com.tangoplus.tangoq.data.HistoryVO
import com.tangoplus.tangoq.databinding.RvExerciseSubCategoryItemBinding
import com.tangoplus.tangoq.listener.OnCustomCategoryClickListener
import org.apache.commons.math3.stat.Frequency

class ProgramCustomRVAdapter(private val fragment: Fragment, private val frequency: Int,private val episodes: MutableList<HistoryUnitVO>, private val episodeState : Pair<Int, Int>, private val onCustomCategoryClickListener: OnCustomCategoryClickListener) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    inner class viewHolder(view: View) : RecyclerView.ViewHolder(view){
        val tvSCName : TextView = view.findViewById(R.id.tvSCName)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = RvExerciseSubCategoryItemBinding.inflate(inflater, parent, false)
        return viewHolder(binding.root)
    }

    override fun getItemCount(): Int {
        return frequency
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {

        // episode는 첫 번째가 선택된 회차, 두 번째가 진행되는 회차임.
        if (holder is viewHolder) {
            holder.tvSCName.text = "$position 회차"


            val currentItem = if (position < episodes.size) {
                episodes[position]
            } else {
                // 빈 HistoryUnitVO 객체 생성
                HistoryUnitVO(null, null, null) // 필요한 기본값으로 초기화
            }
            if (episodeState.second < episodeState.first) {

                // ------# 조건 처리 #------
                when (currentItem.lastPosition == 0 && currentItem.regDate != null) {
                    true -> {
                        setTextView(holder.tvSCName, R.color.secondContainerColor, R.color.thirdColor)
                    }
                    false -> {
                        setTextView(holder.tvSCName, R.color.deleteContainerColor, R.color.deleteColor)
                    }
                }
            } else if (episodeState.second == episodeState.first) {
                when (currentItem.lastPosition == 0 && currentItem.regDate != null) {
                    true -> {
                        setTextView(holder.tvSCName, R.color.secondContainerColor, R.color.thirdColor)
                    }
                    false -> {
                        if (position < episodeState.first) {
                            setTextView(holder.tvSCName, R.color.deleteContainerColor, R.color.deleteColor)
                        } else if (position == episodeState.first) {
                            setTextView(holder.tvSCName, R.color.thirdColor, R.color.white)

                        }
                    }
                }
            }  else {
                setTextView(holder.tvSCName, R.color.subColor100, R.color.subColor400)
            }

             // 회차별 상태 업데이트
            holder.tvSCName.setOnClickListener {
                onCustomCategoryClickListener.customCategoryClick(position)
            }
        }
    }

    private fun setTextView(tv: TextView, color1: Int, color2: Int) {
        tv.background = ContextCompat.getDrawable(fragment.requireContext(), R.drawable.effect_ibtn_20dp)
        tv.backgroundTintList = ContextCompat.getColorStateList(fragment.requireContext(), color1)
        tv.setTextColor(ContextCompat.getColor(fragment.requireContext(), color2))
    }

}