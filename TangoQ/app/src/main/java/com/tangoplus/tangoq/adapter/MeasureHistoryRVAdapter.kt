package com.tangoplus.tangoq.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.tangoplus.tangoq.R
import com.tangoplus.tangoq.vo.MeasureVO
import com.tangoplus.tangoq.viewmodel.MeasureViewModel
import com.tangoplus.tangoq.databinding.RvMeasureItemBinding
import com.tangoplus.tangoq.fragment.ExtendedFunctions.hideBadgeOnClick
import com.tangoplus.tangoq.fragment.MeasureDetailFragment

class MeasureHistoryRVAdapter(val fragment: Fragment, val measures: MutableList<MeasureVO>, private val viewModel : MeasureViewModel): RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    inner class viewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvMIName: TextView = view.findViewById(R.id.tvMIName)
        val tvMIScore : TextView = view.findViewById(R.id.tvMIScore)
        val clMI : ConstraintLayout = view.findViewById(R.id.clMI)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val binding = RvMeasureItemBinding.inflate(layoutInflater, parent, false)
        return viewHolder(binding.root)
    }

    override fun getItemCount(): Int {
        return measures.size
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val currentItem = measures[position]
        if (holder is viewHolder) {
            holder.tvMIName.text = "${currentItem.regDate.substring(0, 10)} 측정 기록"
            holder.tvMIScore.text = currentItem.overall.toString()
            val hideBadgeFunction = fragment.hideBadgeOnClick(holder.tvMIName, holder.clMI, "${holder.tvMIName.text}", ContextCompat.getColor(fragment.requireContext(), R.color.thirdColor))
            holder.clMI.setOnClickListener {
                fragment.requireActivity().supportFragmentManager.beginTransaction().apply {
                    setCustomAnimations(R.anim.slide_in_left, R.anim.slide_in_right)
                    replace(R.id.flMain, MeasureDetailFragment())
                    addToBackStack(null)
                    commit()
                }
                hideBadgeFunction?.invoke()
                viewModel.selectedMeasure = currentItem
            }
        }
    }
}