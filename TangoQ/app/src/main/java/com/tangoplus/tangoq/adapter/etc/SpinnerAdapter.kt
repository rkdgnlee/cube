package com.tangoplus.tangoq.adapter.etc

import android.annotation.SuppressLint
import android.content.Context
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.core.content.ContextCompat
import com.tangoplus.tangoq.R
import com.tangoplus.tangoq.databinding.ItemSpinnerBinding
import com.tangoplus.tangoq.mediapipe.MathHelpers.isTablet


@Suppress("UNREACHABLE_CODE")
class SpinnerAdapter(context:Context, resId: Int, private val list: List<String>, private val case: Int) : ArrayAdapter<String>(context, resId, list) {

    @SuppressLint("ViewHolder")
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val binding = ItemSpinnerBinding.inflate(LayoutInflater.from(parent.context), parent, false)

        when (case) {
            0 -> {
                binding.root.setBackgroundColor(ContextCompat.getColor(context, R.color.white))
                binding.tvSpinner.setBackgroundColor(ContextCompat.getColor(context, R.color.white))
                binding.tvSpinner.setTextColor(ContextCompat.getColor(context, R.color.subColor800))
            }
            1 -> {
                binding.root.setPadding(0, 2, 2, 0)
                binding.root.setBackgroundColor(ContextCompat.getColor(context, R.color.secondContainerColor))
                binding.tvSpinner.setBackgroundColor(ContextCompat.getColor(context, R.color.secondContainerColor))
                binding.tvSpinner.setTextColor(ContextCompat.getColor(context, R.color.secondWhiteColor))
            }
            2 -> {
                binding.root.setPadding(0, 2, 2, 0)
                binding.root.setBackgroundColor(ContextCompat.getColor(context, R.color.transparentColor))
                binding.tvSpinner.setBackgroundColor(ContextCompat.getColor(context, R.color.transparentColor))
                binding.tvSpinner.setTextColor(ContextCompat.getColor(context, R.color.white))
            }
        }
        binding.tvSpinner.text = list[position]
        binding.tvSpinner.textSize = if (isTablet(context)) 20f else 16f

        binding.tvSpinner.gravity = Gravity.CENTER
        return binding.root
    }

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
        val binding = ItemSpinnerBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        binding.tvSpinner.text = list[position]
        binding.tvSpinner.gravity = Gravity.CENTER
        when (case) {
            0 -> {
                binding.tvSpinner.setTextColor(ContextCompat.getColor(context, R.color.subColor800))
                binding.tvSpinner.setBackgroundColor(ContextCompat.getColor(context, R.color.white))
                binding.root.setBackgroundColor(ContextCompat.getColor(context, R.color.white))
            }
            1 -> {
                binding.tvSpinner.setTextColor(ContextCompat.getColor(context, R.color.secondWhiteColor))
                binding.tvSpinner.setBackgroundColor(ContextCompat.getColor(context, R.color.whitebar))
                binding.root.setBackgroundColor(ContextCompat.getColor(context, R.color.whitebar))
            }
            2 -> {

            }
        }
        binding.tvSpinner.textSize = if (isTablet(context)) 20f else 16f
        return binding.root
    }

    override fun getCount(): Int {
        return super.getCount()
        return list.size
    }

}