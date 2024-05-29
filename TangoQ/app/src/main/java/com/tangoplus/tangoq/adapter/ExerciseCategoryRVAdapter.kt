package com.tangoplus.tangoq.adapter

import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.card.MaterialCardView
import com.tangoplus.tangoq.R
import com.tangoplus.tangoq.databinding.RvExerciseMainCateogoryItemBinding
import com.tangoplus.tangoq.databinding.RvExerciseSubCategoryItemBinding
import com.tangoplus.tangoq.listener.OnCategoryClickListener
import com.tangoplus.tangoq.listener.onCategoryScrollListener

class ExerciseCategoryRVAdapter(private val mainCategorys: MutableList<String>,
                                private val subCategorys: MutableList<String>,
                                private val fragment: Fragment,
                                private val recyclerView: RecyclerView,
                                private val onCategoryScrollListener: onCategoryScrollListener,
                                var xmlname: String
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    inner class mainCategoryViewHolder(view:View): RecyclerView.ViewHolder(view) {
        val tvMCName = view.findViewById<TextView>(R.id.tvMCName)
        val ivMCThumbnail = view.findViewById<ImageView>(R.id.ivMCThumbnail)
        val rvMC = view.findViewById<RecyclerView>(R.id.rvMC)
        val mcvMC = view.findViewById<MaterialCardView>(R.id.mcvMC)
    }
    inner class subCategoryViewHolder(view:View): RecyclerView.ViewHolder(view) {
        val tvSCName = view.findViewById<TextView>(R.id.tvSCName)

    }

    override fun getItemViewType(position: Int): Int {
        return when (xmlname) {
            "mainCategory" -> 0
            "subCategory" -> 1
            else -> throw IllegalArgumentException("invalid View Type")
        }

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            0 -> {
                val binding = RvExerciseMainCateogoryItemBinding.inflate(inflater, parent, false)
                mainCategoryViewHolder(binding.root)
            }
            1 -> {
                val binding = RvExerciseSubCategoryItemBinding.inflate(inflater, parent, false)
                subCategoryViewHolder(binding.root)
            }
            else -> throw IllegalArgumentException("invalid view type binding")
        }
    }

    override fun getItemCount(): Int {
        return when (xmlname) {
            "mainCategory" -> {
                mainCategorys.size
            }
            "subCategory" -> {
                subCategorys.size
            }
            else -> throw IllegalArgumentException("invalid Item Count")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {

        when (holder) {
            // ------! 대분류 item 시작 !------
            is mainCategoryViewHolder -> {
                val currentItem = mainCategorys[position]
                holder.mcvMC.visibility = View.GONE
                holder.tvMCName.text = currentItem
//                Glide.with(fragment.requireContext())
//                    .load()
//                    .diskCacheStrategy(DiskCacheStrategy.ALL)
//                    .into(holder.ivMCThumbnail)

                // -----! 이미지 클릭 시 서브 카테고리 시작 !------
                val adapter = ExerciseCategoryRVAdapter(mainCategorys, subCategorys, fragment, holder.rvMC, onCategoryScrollListener,"subCategory" )
                holder.rvMC.adapter = adapter
                val linearLayoutManager = LinearLayoutManager(fragment.requireContext(), LinearLayoutManager.VERTICAL, false)
                holder.rvMC.layoutManager = linearLayoutManager

                holder.ivMCThumbnail.setOnClickListener{
                    if (holder.mcvMC.visibility == View.GONE) {
                        holder.mcvMC.visibility = View.VISIBLE
                        holder.mcvMC.alpha = 0f
                        holder.mcvMC.animate().apply {
                            duration = 150
                            alpha(1f)
                        }
                        Handler(Looper.getMainLooper()).postDelayed({
                            onCategoryScrollListener.categoryScroll(holder.ivMCThumbnail)
                        }, 200)

                    } else {
                        holder.mcvMC.animate().apply {
                            duration = 150
                            alpha(0f)
                            withEndAction {
                                holder.mcvMC.visibility = View.GONE
                            }
                        }
                    }
                }
            }
            is subCategoryViewHolder -> {
                val currentItem = subCategorys[position]
                holder.tvSCName.text = currentItem
            }
        }
    }

}