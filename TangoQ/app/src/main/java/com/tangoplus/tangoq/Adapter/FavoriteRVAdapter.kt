package com.tangoplus.tangoq.Adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.RecyclerView
import com.tangoplus.tangoq.Listener.onFavoriteDetailClickListener
import com.tangoplus.tangoq.R
import com.tangoplus.tangoq.databinding.RvFvItemBinding

class FavoriteRVAdapter(var fvList: MutableList<String>, val listener: onFavoriteDetailClickListener, private val activity: FragmentActivity) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    inner class fvViewHolder(view: View): RecyclerView.ViewHolder(view) {
        val tvPickName = view.findViewById<TextView>(R.id.tvFvName)
        val tvFvTime = view.findViewById<TextView>(R.id.tvFvTime)
        val tvFvCount = view.findViewById<TextView>(R.id.tvFvCount)

    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = RvFvItemBinding.inflate(inflater, parent, false)
        return fvViewHolder(binding.root)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val currentItem = fvList[position]


        if (holder is fvViewHolder) {
            holder.tvPickName.text = currentItem
            holder.tvPickName.setOnClickListener {
                listener.onFavoriteClick(currentItem)
            }
        }
    }

    override fun getItemCount(): Int {
        return fvList.size
    }
}