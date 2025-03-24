package com.tangoplus.tangoq.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.compose.runtime.Composable
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.tangoplus.tangoq.R
import com.tangoplus.tangoq.vo.RecommendationVO
import com.tangoplus.tangoq.databinding.RvRecommendationItemBinding
import com.tangoplus.tangoq.dialog.ProgramCustomDialogFragment
import com.tangoplus.tangoq.fragment.ExtendedFunctions.setOnSingleClickListener

class RecommendationRVAdapter(private val fragment: Fragment,
                              private val data: List<RecommendationVO>,
                              private val exerciseTypeIds: List<Int?>,
//                              private val isSelectionModeEnabled: Boolean = false,
//                              private val onItemSelected: (List<RecommendationVO>) -> Unit
)
    : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    inner class RecommendationViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvPI : TextView = view.findViewById(R.id.tvPI)
        val clPI : ConstraintLayout = view.findViewById(R.id.clPI)
        val ivPIThumbnail : ImageView = view.findViewById(R.id.ivPIThumbnail)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = RvRecommendationItemBinding.inflate(inflater, parent, false)
        return RecommendationViewHolder(binding.root)
    }

    override fun getItemCount(): Int {
        return data.size
    }

    @SuppressLint("DiscouragedApi")
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val currentItem = data[position]
        val typeItem = exerciseTypeIds[position]
        if (holder is RecommendationViewHolder) {
            holder.tvPI.text = currentItem.title
            holder.clPI.setOnSingleClickListener {
                val dialog = ProgramCustomDialogFragment.newInstance(currentItem.programSn, currentItem.recommendationSn)
                dialog.show(fragment.requireActivity().supportFragmentManager, "ProgramCustomDialogFragment")
            }

            // ------# 복부, 등,척추, 엉덩 골반으로 통합 (임시) #------
            val partId = if (typeItem in listOf(5, 6, 7, 8)) 8 else typeItem
//            Log.v("partId", "${currentItem.title}: $partId")
            val drawableResId = fragment.resources.getIdentifier("drawable_joint_$partId", "drawable", fragment.requireContext().packageName)
            holder.ivPIThumbnail.setImageResource(drawableResId)

        }
    }
}