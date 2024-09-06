package com.tangoplus.tangoq.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.divider.MaterialDivider
import com.tangoplus.tangoq.R
import com.tangoplus.tangoq.databinding.RvHistoryItemBinding
import com.tangoplus.tangoq.db.PreferencesManager
import com.tangoplus.tangoq.listener.OnHistoryClickListener
import com.tangoplus.tangoq.listener.OnHistoryDeleteListener

class ExerciseHistoryRVAdapter(private val historys: MutableList<Pair<Int, String>>, private val historyDeleteListener : OnHistoryDeleteListener, private val historyClickListener: OnHistoryClickListener) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    inner class viewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvHI : TextView = view.findViewById(R.id.tvHI)
        val ibtnHI: ImageButton = view.findViewById(R.id.ibtnHI)
        val dHI : MaterialDivider = view.findViewById(R.id.dHI)
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = RvHistoryItemBinding.inflate(inflater, parent, false)
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
            holder.tvHI.text = currentItem.second

            holder.ibtnHI.setOnClickListener{
                historyDeleteListener.onHistoryDelete(currentItem)
            }
            holder.tvHI.setOnClickListener {
                historyClickListener.onHistoryClick(currentItem.second)
            }

        }
    }
}