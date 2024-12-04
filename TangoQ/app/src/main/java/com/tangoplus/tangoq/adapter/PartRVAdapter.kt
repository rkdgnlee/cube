package com.tangoplus.tangoq.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.tangoplus.tangoq.R
import com.tangoplus.tangoq.databinding.RvPartItemBinding
import com.tangoplus.tangoq.dialog.MainPartDialogFragment

class PartRVAdapter(private val fragment: Fragment, private val dangerParts:  MutableList<Pair<String, Float>>? ) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    inner class PartViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvPI : TextView = view.findViewById(R.id.tvPI)
        val cvPI : CardView = view.findViewById(R.id.cvPI)
        val ivPI : ImageView = view.findViewById(R.id.ivPI)
        val clPI : ConstraintLayout = view.findViewById(R.id.clPI)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = RvPartItemBinding.inflate(inflater, parent, false)
        return PartViewHolder(binding.root)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is PartViewHolder) {
            val currentItem = dangerParts?.get(position)
            holder.tvPI.text = currentItem?.first
            val state = when (currentItem?.second) {
                1f -> true
                else -> false
            }
            setPartItem(state, holder.clPI, holder.cvPI, holder.tvPI)

            when (currentItem?.first) {
                "목" -> holder.ivPI.setImageResource(R.drawable.icon_part1)
                "우측 어깨", "좌측 어깨" -> holder.ivPI.setImageResource(R.drawable.icon_part2)
                "우측 팔꿉", "좌측 팔꿉" -> holder.ivPI.setImageResource(R.drawable.icon_part3)
                "우측 손목", "좌측 손목" -> holder.ivPI.setImageResource(R.drawable.icon_part4)
                "우측 골반", "좌측 골반" -> holder.ivPI.setImageResource(R.drawable.icon_part5)
                "우측 무릎", "좌측 무릎" -> holder.ivPI.setImageResource(R.drawable.icon_part6)
                "우측 발목", "좌측 발목" -> holder.ivPI.setImageResource(R.drawable.icon_part7)
            }
            holder.clPI.setOnClickListener {
                val dialog = currentItem?.first?.let { it1 -> MainPartDialogFragment.newInstance(it1) }
                dialog?.show(fragment.requireActivity().supportFragmentManager, "MainPartDialogFragment")
            }
        }
    }

    override fun getItemCount(): Int {
        return dangerParts?.size ?: 0
    }

    private fun setPartItem(isWarning: Boolean, cl: ConstraintLayout, cv: CardView, tv: TextView){
        if (isWarning) {
            cl.backgroundTintList = ContextCompat.getColorStateList(fragment.requireContext(), R.color.cautionContainerColor)
            cv.setCardBackgroundColor(fragment.resources.getColor(R.color.cautionColor, null))
            tv.setTextColor(ContextCompat.getColor(fragment.requireContext(), R.color.cautionColor))
        } else {
            cl.backgroundTintList = ContextCompat.getColorStateList(fragment.requireContext(), R.color.deleteContainerColor)
            cv.setCardBackgroundColor(fragment.resources.getColor(R.color.deleteColor, null))
            tv.setTextColor(ContextCompat.getColor(fragment.requireContext(), R.color.deleteTextColor))
        }
    }
}