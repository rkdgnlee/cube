package com.tangoplus.tangobody.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.divider.MaterialDivider
import com.tangoplus.tangobody.R
import com.tangoplus.tangobody.databinding.RvExerciseSearchHistoryItemBinding
import com.tangoplus.tangobody.fragment.ExtendedFunctions.setOnSingleClickListener
import com.tangoplus.tangobody.listener.OnHistoryClickListener
import com.tangoplus.tangobody.listener.OnHistoryDeleteListener

class ExerciseSearchHistoryRVAdapter(private val historys: MutableList<Pair<Int, String>>, private val historyDeleteListener : OnHistoryDeleteListener, private val historyClickListener: OnHistoryClickListener) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    inner class viewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvHI : TextView = view.findViewById(R.id.tvHI)
        val ibtnHI: ImageButton = view.findViewById(R.id.ibtnHI)
        val dHI : MaterialDivider = view.findViewById(R.id.dHI)
        val tvHIDate : TextView = view.findViewById(R.id.tvHIDate)
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = RvExerciseSearchHistoryItemBinding.inflate(inflater, parent, false)
        return viewHolder(binding.root)
    }

    override fun getItemCount(): Int {
        return historys.size
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val currentItem = historys[position]
        if (holder is viewHolder) {
            if (position == 0) {
                holder.dHI.visibility = View.VISIBLE
            } else {
                holder.dHI.visibility = View.GONE
            }

            val fullHistory = currentItem.second
            val historyDateIndex = fullHistory.lastIndexOf("날짜")
            val historyUnit = fullHistory.substring(0, historyDateIndex)
            val historyDate = fullHistory.substring(historyDateIndex + 2, fullHistory.length)
            holder.tvHI.text = historyUnit
            holder.tvHIDate.text = "검색 날짜: $historyDate"

            holder.ibtnHI.setOnSingleClickListener{
                historyDeleteListener.onHistoryDelete(currentItem)
            }
            holder.tvHI.setOnSingleClickListener {
                historyClickListener.onHistoryClick(currentItem.second)
            }

        }
    }
}