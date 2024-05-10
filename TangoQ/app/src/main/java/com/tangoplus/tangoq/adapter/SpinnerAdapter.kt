package com.tangoplus.tangoq.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import com.tangoplus.tangoq.databinding.ItemSpinnerBinding


@Suppress("UNREACHABLE_CODE")
class SpinnerAdapter(context:Context, private val resId: Int, private val list: List<String>) : ArrayAdapter<String>(context, resId, list) {

    @SuppressLint("ViewHolder")
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val binding = ItemSpinnerBinding.inflate(LayoutInflater.from(parent.context), parent, false)

        binding.root.setPadding(0, binding.root.paddingTop, 0, binding.root.paddingBottom)

        binding.tvSpinner.text = list[position]
        return binding.root
    }

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
        val binding = ItemSpinnerBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        binding.tvSpinner.text = list[position]
        return binding.root
    }

    override fun getCount(): Int {
        return super.getCount()
        return list.size
    }
}