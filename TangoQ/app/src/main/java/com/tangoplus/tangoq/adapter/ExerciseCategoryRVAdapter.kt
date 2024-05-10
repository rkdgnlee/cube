package com.tangoplus.tangoq.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.tangoplus.tangoq.listener.OnCategoryClickListener
import com.tangoplus.tangoq.R
import com.tangoplus.tangoq.databinding.RvHorizontalItemBinding

class ExerciseCategoryRVAdapter(var category: Array<String>, private val onCategoryClickListener: OnCategoryClickListener) : RecyclerView.Adapter<ExerciseCategoryRVAdapter.MyViewHolder>()  {
    inner class MyViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvCategory = view.findViewById<TextView>(R.id.tvCategory)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = RvHorizontalItemBinding.inflate(inflater, parent, false)
        return MyViewHolder(binding.root)
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val currentItem = category[position]
        holder.tvCategory.text = currentItem
        holder.tvCategory.setOnClickListener {
            onCategoryClickListener.onCategoryClick(currentItem)
        }
    }
    override fun getItemCount(): Int {
        return category.size
    }
}