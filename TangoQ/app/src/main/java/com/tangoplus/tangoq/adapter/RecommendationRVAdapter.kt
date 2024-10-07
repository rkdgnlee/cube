package com.tangoplus.tangoq.adapter

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.tangoplus.tangoq.R
import com.tangoplus.tangoq.data.RecommendationVO
import com.tangoplus.tangoq.databinding.RvRecommendationItemBinding
import com.tangoplus.tangoq.dialog.ProgramCustomDialogFragment

class RecommendationRVAdapter(private val fragment: Fragment, private val data: List<RecommendationVO>, private val exerciseTypeIds: List<Int?>) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    inner class programViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvPI : TextView = view.findViewById(R.id.tvPI)
        val clPI : ConstraintLayout = view.findViewById(R.id.clPI)
        val ivPIThumbnail : ImageView = view.findViewById(R.id.ivPIThumbnail)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = RvRecommendationItemBinding.inflate(inflater, parent, false)
        return programViewHolder(binding.root)
    }

    override fun getItemCount(): Int {
        return data.size
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        Log.v("adapter내부정보", "${data.size}, ${exerciseTypeIds.size}")
        val currentItem = data[position]
        val typeItem = exerciseTypeIds[position]
        if (holder is programViewHolder) {
            holder.tvPI.text = currentItem.title
            holder.clPI.setOnClickListener {
                val dialog = ProgramCustomDialogFragment.newInstance(currentItem.programSn, currentItem.recommendationSn)
                dialog.show(fragment.requireActivity().supportFragmentManager, "ProgramCustomDialogFragment")
            }

            // drawable 가져오기

            val drawableResId = fragment.resources.getIdentifier("drawable_joint_$typeItem", "drawable", fragment.requireContext().packageName)
            holder.ivPIThumbnail.setImageResource(drawableResId)

        }
    }
}