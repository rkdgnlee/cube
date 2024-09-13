package com.tangoplus.tangoq.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.tangoplus.tangoq.R
import com.tangoplus.tangoq.data.EpisodeVO
import com.tangoplus.tangoq.databinding.RvExerciseSubCategoryItemBinding
import com.tangoplus.tangoq.listener.OnCustomCategoryClickListener

class CustomExerciseRVAdapter(private val fragment: Fragment, private val episodes: MutableList<EpisodeVO>, private val weekState : Pair<Int, Int>, private val episodeState : Pair<Int, Int>,   private val onCustomCategoryClickListener: OnCustomCategoryClickListener) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    inner class viewHolder(view: View) : RecyclerView.ViewHolder(view){
        val tvSCName : TextView = view.findViewById(R.id.tvSCName)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = RvExerciseSubCategoryItemBinding.inflate(inflater, parent, false)
        return viewHolder(binding.root)
    }

    override fun getItemCount(): Int {
        return episodes.size
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val currentItem = episodes[position]

        if (holder is viewHolder) {
            holder.tvSCName.text = "${position + 1}회차"

            // 기본 상태 설정
            // episode는 현재 진행중인 episode, week도 마찬가지 현재 진행중인 week ex) 화요일이다. 2주차 2회차

            /* currentEpisode 는 진행중인 주차, 진행중인 회차, 선택된 회차 이렇게 나눠짐 */
            //episode에 2가들어가있는 상태.

            if (weekState.second < weekState.first) {

                // ------# 조건 처리 #------
                when (currentItem.isFinish) {
                    true -> {
                        setTextView(holder.tvSCName, R.color.secondPrimaryColor, R.color.thirdColor)
                    }
                    false -> {
                        setTextView(holder.tvSCName, R.color.deleteContainerColor, R.color.deleteColor)
                    }
                }
            } else if (weekState.second == weekState.first) {
                when (currentItem.isFinish) {
                    true -> {
                        setTextView(holder.tvSCName, R.color.secondPrimaryColor, R.color.thirdColor)
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


//            when (currentItem.isFinish) {
//                true ->
//                false -> {
//                    if (position < currentState.second) {
//                        setTextView(holder.tvSCName, R.color.deleteContainerColor, R.color.deleteColor)
//                    } else {
//                        setTextView(holder.tvSCName, R.color.subColor100, R.color.subColor400)
//                    }
//                }
//            }
//             if (episodes.indexOf(currentItem) == currentState.second) {
//                 setTextView(holder.tvSCName, R.color.mainColor, R.color.white)
//             }

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