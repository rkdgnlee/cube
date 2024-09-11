package com.tangoplus.tangoq.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.tangoplus.tangoq.R
import com.tangoplus.tangoq.databinding.RvDataDynamicItemBinding
import com.tangoplus.tangoq.view.TrendCurveView

// data는 부위는 총 6개 -> 한 부위의 219프레임 동안의 x, y값인 data임
class DataDynamicRVAdapter(private val data: List<List<Pair<Float, Float>>>, private val titles: List<String>) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    inner class viewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvDDI : TextView = view.findViewById(R.id.tvDDI)

        val cvDDI : TrendCurveView = view.findViewById(R.id.cvDDI)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = RvDataDynamicItemBinding.inflate(inflater, parent, false)
        return viewHolder(binding.root)
    }

    override fun getItemCount(): Int {
        return data.size
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val currentItem = data[position]
        if (holder is viewHolder) {
            holder.tvDDI.text = titles[position]

            holder.cvDDI.setPoints(currentItem)



        }
    }


}