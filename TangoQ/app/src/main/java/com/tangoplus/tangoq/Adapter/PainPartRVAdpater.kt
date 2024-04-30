package com.tangoplus.tangoq.Adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.Recycler
import com.tangoplus.tangoq.Listener.OnPartCheckListener
import com.tangoplus.tangoq.R
import com.tangoplus.tangoq.databinding.RvPainPartItemBinding

class PainPartRVAdpater(var partList: MutableList<Pair<String, String>>, private val onPartCheckListener: OnPartCheckListener) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    inner class ppViewHolder(view: View) :RecyclerView.ViewHolder(view) {
        val tvPainPart = view.findViewById<TextView>(R.id.tvPainPart)
        val ivPainPart = view.findViewById<ImageView>(R.id.ivPainPart)
        val cbPainPart = view.findViewById<CheckBox>(R.id.cbPainPart)
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = RvPainPartItemBinding.inflate(inflater, parent, false)
        return ppViewHolder(binding.root)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val currentItem = partList[position]

        if (holder is ppViewHolder) {
            holder.tvPainPart.text = currentItem.second
            val resourceId = holder.itemView.context.resources.getIdentifier(
                currentItem.first, "drawable", holder.itemView.context.packageName
            )
            holder.ivPainPart.setImageResource(resourceId)


            holder.cbPainPart.setOnCheckedChangeListener { buttonView, isChecked ->
                when (isChecked) {
                    true -> {
                        onPartCheckListener.onPartCheck(currentItem.second, isChecked)
                    }
                    false -> {
                        onPartCheckListener.onPartCheck(currentItem.second, isChecked)
                    }
                }

            }
        }



    }

    override fun getItemCount(): Int {
        return partList.size
    }
}