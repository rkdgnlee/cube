package com.tangoplus.tangoq.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.util.DisplayMetrics
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.ArrayAdapter
import androidx.core.content.ContextCompat
import com.tangoplus.tangoq.R
import com.tangoplus.tangoq.databinding.ItemSpinnerBinding


@Suppress("UNREACHABLE_CODE")
class SpinnerAdapter(context:Context, resId: Int, private val list: List<String>) : ArrayAdapter<String>(context, resId, list) {

    @SuppressLint("ViewHolder")
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val binding = ItemSpinnerBinding.inflate(LayoutInflater.from(parent.context), parent, false)

        binding.root.setPadding(0, 2, 2, 0)
        binding.root.setBackgroundColor(ContextCompat.getColor(context, R.color.secondPrimaryColor))
        binding.tvSpinner.setBackgroundColor(ContextCompat.getColor(context, R.color.secondPrimaryColor))
        binding.tvSpinner.setTextColor(ContextCompat.getColor(context, R.color.secondaryColor))

        binding.tvSpinner.text = list[position]
        binding.tvSpinner.textSize = if (isTablet(context)) 20f else 16f

        return binding.root
    }

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
        val binding = ItemSpinnerBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        binding.tvSpinner.text = list[position]
        binding.tvSpinner.setTextColor(ContextCompat.getColor(context, R.color.secondaryColor))
        binding.tvSpinner.textSize = if (isTablet(context)) 20f else 16f
        return binding.root
    }

    override fun getCount(): Int {
        return super.getCount()
        return list.size
    }

    private fun isTablet(context: Context): Boolean {
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val metrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(metrics)

        val widthDp = metrics.widthPixels / metrics.density
        return widthDp >= 600
    }
}