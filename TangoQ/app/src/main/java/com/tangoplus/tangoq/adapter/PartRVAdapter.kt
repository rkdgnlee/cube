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
import com.tangoplus.tangoq.databinding.RvMainPartItemBinding
import com.tangoplus.tangoq.databinding.RvPartItemBinding
import com.tangoplus.tangoq.fragment.ExtendedFunctions.setOnSingleClickListener
import com.tangoplus.tangoq.fragment.MainAnalysisFragment
import com.tangoplus.tangoq.listener.OnCategoryClickListener
import com.tangoplus.tangoq.viewmodel.AnalysisViewModel
import com.tangoplus.tangoq.viewmodel.FragmentViewModel

class PartRVAdapter(private val fragment: Fragment, private val dangerParts:  MutableList<Pair<String, Float>>?, private val avm: AnalysisViewModel, private val fvm: FragmentViewModel, private val xmlName: String) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    var onCategoryClickListener: OnCategoryClickListener? = null
    private var selectedPosition = 0


    inner class MainPartViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvPI : TextView = view.findViewById(R.id.tvMPI)
        val cvPI : CardView = view.findViewById(R.id.cvMPI)
        val ivPI : ImageView = view.findViewById(R.id.ivMPI)
        val clPI : ConstraintLayout = view.findViewById(R.id.clMPI)
    }
    inner class PartViewHolder(view:View) : RecyclerView.ViewHolder(view) {
        val clPI : ConstraintLayout = view.findViewById(R.id.clPI)
        val tvPIPart : TextView = view.findViewById(R.id.tvPIPart)
        val cvPIPartCheck: CardView = view.findViewById(R.id.cvPIPartCheck)
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            0 -> {
                val binding = RvMainPartItemBinding.inflate(inflater, parent, false)
                MainPartViewHolder(binding.root)
            }

            1 -> {
                val binding = RvPartItemBinding.inflate(inflater, parent, false)
                PartViewHolder(binding.root)
            }
            else -> throw IllegalArgumentException("Invaild View Type")
        }
    }
    override fun getItemViewType(position: Int): Int {
        return when (xmlName) {
            "main" -> 0
            "measureDetail" -> 1
            else -> throw IllegalArgumentException("Invalid View Type")
        }
    }
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val currentItem = dangerParts?.get(position)
        if (holder is MainPartViewHolder) {
            holder.tvPI.text = currentItem?.first
            val state = when (currentItem?.second) {
                1f -> true
                else -> false
            }
            setPartItem(state, holder.cvPI)

            val drawableId = when (currentItem?.first) {
                "목관절" -> R.drawable.icon_part1
                "우측 어깨", "좌측 어깨" -> R.drawable.icon_part2
                "우측 팔꿉", "좌측 팔꿉" -> R.drawable.icon_part3
                "우측 손목", "좌측 손목" -> R.drawable.icon_part4
                "우측 골반", "좌측 골반" -> R.drawable.icon_part5
                "우측 무릎", "좌측 무릎" -> R.drawable.icon_part6
                "우측 발목", "좌측 발목" -> R.drawable.icon_part7
                else -> -1
            }
            val isRight = if (currentItem?.first?.contains("우측") == true) true else false
            if (drawableId != -1) {
                loadFlippedImage(holder, drawableId, isRight)
            }

            holder.clPI.setOnSingleClickListener {
                fvm.setCurrentFragment(FragmentViewModel.FragmentType.MAIN_ANALYSIS_FRAGMENT)
                avm.currentPart.value = currentItem?.first
                avm.selectPart.value = currentItem?.first
                fragment.requireActivity().supportFragmentManager.beginTransaction().apply {
                    replace(R.id.flMain, MainAnalysisFragment.newInstance(currentItem?.first ?: ""))
                    addToBackStack(null)
                    commit()
                }
            }
        } else if (holder is PartViewHolder) {
            holder.tvPIPart.text = currentItem?.first
            val adapterPosition = holder.adapterPosition

            val state = when (currentItem?.second) {
                1f -> 1
                2f -> 2
                else -> 0
            }
            updatePartBadge(holder, state)
            if (adapterPosition == selectedPosition) {
                holder.clPI.backgroundTintList = ContextCompat.getColorStateList(fragment.requireContext(), R.color.mainColor)
            } else {
                holder.clPI.backgroundTintList = ContextCompat.getColorStateList(fragment.requireContext(), R.color.subColor200)
            }
            holder.clPI.setOnSingleClickListener {
                if (currentItem != null) {
                    onCategoryClickListener?.onCategoryClick(currentItem.first.toString())
                }
                val previousPosition = selectedPosition
                selectedPosition = adapterPosition
                notifyItemChanged(previousPosition) // 이전 선택된 아이템 갱신
                notifyItemChanged(selectedPosition) // 새로 선택된 아이템 갱신
            }
        }
    }

    private fun updatePartBadge(holder: PartViewHolder, state: Int) {
        holder.cvPIPartCheck.visibility = View.VISIBLE
        when (state) {
            0 -> holder.cvPIPartCheck.visibility = View.INVISIBLE
            1 -> holder.cvPIPartCheck.setCardBackgroundColor(ContextCompat.getColor(fragment.requireContext(), R.color.cautionColor))
            2 -> holder.cvPIPartCheck.setCardBackgroundColor(ContextCompat.getColor(fragment.requireContext(), R.color.deleteColor))
        }
    }

    override fun getItemCount(): Int {
        return dangerParts?.size ?: 0
    }

    private fun setPartItem(isWarning: Boolean, cv: CardView){
        if (isWarning) {
            cv.setCardBackgroundColor(fragment.resources.getColor(R.color.cautionColor, null))
        } else {
            cv.setCardBackgroundColor(fragment.resources.getColor(R.color.deleteColor, null))
        }
    }
    private fun loadFlippedImage(holder: MainPartViewHolder, resourceId: Int, isRight: Boolean) {
        holder.ivPI.setImageResource(resourceId)
        if (!isRight) {
            holder.ivPI.scaleX = -1f
        }
    }
}