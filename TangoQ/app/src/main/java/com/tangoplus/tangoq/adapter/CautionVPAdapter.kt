package com.tangoplus.tangoq.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.airbnb.lottie.LottieAnimationView
import com.tangoplus.tangoq.R

class CautionVPAdapter(private val layouts: List<Int>) : RecyclerView.Adapter<CautionVPAdapter.CautionViewHolder>() {

    inner class CautionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind(animationName: String, lottieViewId: Int) {
            val lottieView: LottieAnimationView = itemView.findViewById(lottieViewId)
            lottieView.setAnimation(animationName)
            lottieView.playAnimation()
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CautionViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val view = inflater.inflate(viewType, parent, false)
        return CautionViewHolder(view)
    }

    private val lottieViewIds = listOf(
        R.id.lavSC1,
        R.id.lavSC2,
        R.id.lavSC3
    )

    private val lottieAnimations = listOf(
        "measure_guide_anime_1.json",
        "measure_guide_anime_2.json",
        "measure_guide_anime_3.json"
    )
    override fun onBindViewHolder(holder: CautionViewHolder, position: Int) {
        holder.bind(lottieAnimations[position], lottieViewIds[position])
    }

    override fun getItemViewType(position: Int): Int {
        return layouts[position]
    }
    override fun getItemCount(): Int {
        return layouts.size
    }
}