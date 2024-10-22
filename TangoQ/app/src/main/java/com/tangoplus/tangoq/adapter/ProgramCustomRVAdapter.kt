package com.tangoplus.tangoq.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.tangoplus.tangoq.R
import com.tangoplus.tangoq.databinding.RvExerciseSubCategoryItemBinding
import com.tangoplus.tangoq.databinding.RvProgramSeqItemBinding
import com.tangoplus.tangoq.listener.OnCustomCategoryClickListener

class ProgramCustomRVAdapter(private val fragment: Fragment,
                             private val seq: Triple<Int, Int, Int>,
                             private val week: Pair<Int, Int>,
                             private val onCustomCategoryClickListener: OnCustomCategoryClickListener) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    // frequency 는 총 들어가는 회차, progresses는 같이 들어가는 시청 기록, sequencdState는 현재 회차와 선택한회차
    inner class viewHolder(view: View) : RecyclerView.ViewHolder(view){
        val tvPSIName : TextView = view.findViewById(R.id.tvPSIName)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = RvProgramSeqItemBinding.inflate(inflater, parent, false)
        return viewHolder(binding.root)
    }

    override fun getItemCount(): Int {
        return seq.first
    }
    /* seq.first == 총 회차
    *  seq.second == 현재 회차
    *  seq.third == 선택한 회차
    *  week.first == 현재 주차
    *  week.second == 선택된 주차
    * */
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {

        // episode는 첫 번째가 선택된 회차, 두 번째가 진행되는 회차임.
        if (holder is viewHolder) {

            holder.tvPSIName.text = "Day ${position+1}"

            // seqState.first == 현재 회차 seqState.second == 선택한 회차

            if (week.second <= week.first) {
                if (position == seq.third) {
                    setTextView(holder.tvPSIName, R.color.secondaryColor, R.color.white)
                } else if (position <= seq.second) {
                    setTextView(holder.tvPSIName, R.color.secondHalfColor, R.color.white)
                }
            } else {
                setTextView(holder.tvPSIName, R.color.subColor100, R.color.subColor400)
            }


             // 회차별 상태 업데이트
            holder.tvPSIName.setOnClickListener {
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