package com.tangoplus.tangoq.adapter

import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.tangoplus.tangoq.R
import com.tangoplus.tangoq.databinding.RvPartExtendedItemBinding
import com.tangoplus.tangoq.listener.OnCategoryScrollListener

class ExtendedRVAdapter(private val fragment: Fragment, private val parts: MutableList<String?>, private val onCategoryScrollListener: OnCategoryScrollListener) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    inner class ExtendedViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvPEI : TextView = view.findViewById(R.id.tvPEI)
        val clPEI : ConstraintLayout = view.findViewById(R.id.clPEI)
        val rvPEI: RecyclerView = view.findViewById(R.id.rvPEI)
        val ivPEIArrow : ImageView = view.findViewById(R.id.ivPEIArrow)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = RvPartExtendedItemBinding.inflate(inflater, parent, false)
        return ExtendedViewHolder(binding.root)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is ExtendedViewHolder) {
            val currentItem = parts[position]

            holder.tvPEI.text=  currentItem

//            val adapter = DataStaticRVAdapter(fragment.requireContext(), , true)



            holder.clPEI.setOnClickListener {
                if (holder.rvPEI.visibility == View.GONE) {
                    holder.ivPEIArrow.setImageDrawable(ContextCompat.getDrawable(fragment.requireContext(), R.drawable.icon_arrow_up))
                    holder.rvPEI.visibility = View.VISIBLE
                    holder.rvPEI.alpha = 0f
                    holder.rvPEI.animate().apply {
                        duration = 100
                        alpha(1f)
                    }
                    Handler(Looper.getMainLooper()).postDelayed({
                        onCategoryScrollListener.categoryScroll(holder.clPEI)
                    }, 150)

                } else {
                    holder.ivPEIArrow.setImageDrawable(ContextCompat.getDrawable(fragment.requireContext(), R.drawable.icon_arrow_down))
                    holder.rvPEI.animate().apply {
                        duration = 100
                        alpha(0f)
                        withEndAction {
                            holder.rvPEI.visibility = View.GONE
                        }
                    }
                }
            }

        }
    }

    override fun getItemCount(): Int {
        return parts.size
    }
}