package com.tangoplus.tangoq.Adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.tangoplus.tangoq.Listener.OnPartCheckListener
import com.tangoplus.tangoq.R
import com.tangoplus.tangoq.databinding.RvPainPartItemBinding
import com.tangoplus.tangoq.databinding.RvSelectPainPartItemBinding
import java.lang.IllegalArgumentException

class PainPartRVAdpater(var partList: MutableList<Pair<String, String>>, var xmlname: String ,private val onPartCheckListener: OnPartCheckListener) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    inner class selectPpViewHolder(view: View) :RecyclerView.ViewHolder(view) {
        val tvScPp = view.findViewById<TextView>(R.id.tvScPp)
        val ivScPp = view.findViewById<ImageView>(R.id.ivScPp)
        val cbScPp = view.findViewById<CheckBox>(R.id.cbScPp)
    }

    inner class ppViewHolder(view: View): RecyclerView.ViewHolder(view) {
        val tvPpName = view.findViewById<TextView>(R.id.tvPpName)
        val ivPp = view.findViewById<ImageView>(R.id.ivPp)
        val ibtnPpMore = view.findViewById<ImageButton>(R.id.ibtnPpMore)
    }

    override fun getItemViewType(position: Int): Int {
        return when (xmlname) {
            "Pp" -> 0
            "selectPp" -> 1
            else -> throw IllegalArgumentException("invalid view type")
        }
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            0 -> {
                val binding = RvPainPartItemBinding.inflate(inflater, parent, false)
                ppViewHolder(binding.root)
            }
            1 -> {
                val binding = RvSelectPainPartItemBinding.inflate(inflater, parent, false)
                selectPpViewHolder(binding.root)
            }
            else -> throw IllegalArgumentException("invalid view type binding")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val currentItem = partList[position]

        if (holder is ppViewHolder) {
            holder.tvPpName.text = currentItem.second
            holder.ibtnPpMore.setOnClickListener {
                onPartCheckListener.onPartCheck(Pair(currentItem.first, currentItem.second), false)
            }
//            val resourceId = holder.itemView.context.resources.getIdentifier(
//                currentItem.first, "drawable", holder.itemView.context.packageName
//            )
//            holder.ivPp.setImageResource(resourceId)

        } else if (holder is selectPpViewHolder) {
            holder.tvScPp.text = currentItem.second
            val resourceId = holder.itemView.context.resources.getIdentifier(
                currentItem.first, "drawable", holder.itemView.context.packageName
            )
            holder.ivScPp.setImageResource(resourceId)


            holder.cbScPp.setOnCheckedChangeListener { buttonView, isChecked ->
                when (isChecked) {
                    true -> {
                        onPartCheckListener.onPartCheck(Pair(currentItem.first,currentItem.second), isChecked)
                    }
                    false -> {
                        onPartCheckListener.onPartCheck(Pair(currentItem.first,currentItem.second), isChecked)
                    }
                }

            }
        }
    }

    override fun getItemCount(): Int {
        return partList.size
    }
}