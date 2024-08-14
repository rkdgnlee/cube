package com.tangoplus.tangoq.adapter

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.tangoplus.tangoq.R
import com.tangoplus.tangoq.data.ProgramViewModel
import com.tangoplus.tangoq.databinding.RvFilterItemBinding
import com.tangoplus.tangoq.listener.OnFilterSelectedListener


class FilterRVAdapter(
    val fragment: Fragment,
    private val filterMiddle: MutableList<String>,
    private val onFilterSelectedListener: OnFilterSelectedListener,
    private val filterNumber: Int,
    private val viewModel: ProgramViewModel
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private val checkedItems = MutableList(filterMiddle.size) { false }

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val cbPI: CheckBox = view.findViewById(R.id.cbFI)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = RvFilterItemBinding.inflate(inflater, parent, false)
        return ViewHolder(binding.root)
    }



    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val currentItem = filterMiddle[position]

        if (holder is ViewHolder) {
            holder.cbPI.apply {
                text = currentItem
//                setOnClickListener(null) // 이전 리스너 제거
//                setOnCheckedChangeListener(null) // 이전 리스너 제거
                isChecked = checkedItems[position]
                setTextColor(resources.getColor(R.color.subColor700, null))

                when (filterNumber) {
                    1 -> {
                        isChecked = isItemChecked(currentItem)
                        setTextColor(resources.getColor(
                            if (isChecked) R.color.mainColor else R.color.subColor700,
                            null
                        ))
                        setOnCheckedChangeListener { _, isChecked ->
                            checkedItems[position] = isChecked
                            updateFilter1(currentItem, isChecked)
                            setTextColor(resources.getColor(
                                if (isChecked) R.color.mainColor else R.color.subColor700,
                                null
                            ))
                            Log.v("vm>filter1-1", "value: ${viewModel.filter1.value}, checkbox: $isChecked")
                        }
                    }
                    else -> {
                        val isCurrentlySelected = viewModel.getSelectedItem(filterNumber) == currentItem
                        isChecked = isCurrentlySelected
                        setTextColor(resources.getColor(
                            if (isCurrentlySelected) R.color.mainColor else R.color.subColor700,
                            null
                        ))
                        setOnClickListener {
                            if (!isCurrentlySelected) {
                                viewModel.updateFilter(filterNumber, currentItem)
                                onFilterSelectedListener.onFilterSelected(currentItem)

                                // 현재 아이템 상태 즉시 업데이트
                                isChecked = true
                                setTextColor(resources.getColor(R.color.mainColor, null))

                                // 다른 아이템들의 상태 업데이트
                                notifyDataSetChanged()
                            }
                        }
                    }
                }
            }
        }
    }

    override fun getItemCount(): Int = filterMiddle.size

    private fun isItemChecked(item: String): Boolean {
        return viewModel.filter1.value?.toString()?.contains(item) == true
    }

    private fun updateFilter1(item: String, isChecked: Boolean) {
        if (isChecked) {
            viewModel.addToFilter1(item)
        } else {
            viewModel.removeFromFilter1(item)
        }
    }

    fun uncheckAllItems() {
        checkedItems.fill(false)
        notifyDataSetChanged()
    }
}