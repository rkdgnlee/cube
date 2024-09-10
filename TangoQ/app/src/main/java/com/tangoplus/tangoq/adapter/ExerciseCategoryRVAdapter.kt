package com.tangoplus.tangoq.adapter

import android.content.res.ColorStateList
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.tangoplus.tangoq.R
import com.tangoplus.tangoq.data.ExerciseVO
import com.tangoplus.tangoq.databinding.RvExerciseMainCateogoryItemBinding
import com.tangoplus.tangoq.databinding.RvExerciseSubCategoryItemBinding
import com.tangoplus.tangoq.fragment.ExerciseDetailFragment
import com.tangoplus.tangoq.listener.OnCategoryClickListener

class ExerciseCategoryRVAdapter(private val mainCategorys: MutableList<Pair<Int, String>>,
                                private val subCategorys: List<String>,
                                private val fragment: Fragment,
                                private val onCategoryClickListener: OnCategoryClickListener,
                                private val sn : Int,
                                private var xmlname: String
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var selectedPosition = 0
    inner class MainCategoryViewHolder(view:View): RecyclerView.ViewHolder(view) {
//        val tvMCName : TextView = view.findViewById(R.id.tvMCName)
        val ivMCThumbnail : ImageView = view.findViewById(R.id.ivMCThumbnail)
//        val rvMC : RecyclerView = view.findViewById(R.id.rvMC)
//        val mcvMC : MaterialCardView = view.findViewById(R.id.mcvMC)
    }
    inner class SubCategoryViewHolder(view:View): RecyclerView.ViewHolder(view) {
        val tvSCName : TextView = view.findViewById(R.id.tvSCName)

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
                MainCategoryViewHolder(binding.root)
            }
            1 -> {
                val binding = RvExerciseSubCategoryItemBinding.inflate(inflater, parent, false)
                SubCategoryViewHolder(binding.root)
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
            is MainCategoryViewHolder -> {
                val currentItemMain = mainCategorys[position]

                Glide.with(fragment)
                    .load(fragment.resources.getIdentifier("drawable_main_category_${position}", "drawable", fragment.requireActivity().packageName))
                    .diskCacheStrategy(DiskCacheStrategy.NONE) // 캐싱 무시
                    .skipMemoryCache(true) // 메모리 캐시 무시

                    .into(holder.ivMCThumbnail)

                holder.ivMCThumbnail.setOnClickListener{
                    goExerciseDetail(currentItemMain)

                }
            }
            is SubCategoryViewHolder -> {
                val currentItem = subCategorys[position]
                holder.tvSCName.text = currentItem
                val adapterPosition = holder.adapterPosition
                holder.tvSCName.setBackgroundResource(R.drawable.background_rectangle_20dp)
                if (adapterPosition == selectedPosition) {
                    holder.tvSCName.backgroundTintList = ContextCompat.getColorStateList(fragment.requireContext(), R.color.mainColor)
                    holder.tvSCName.setTextColor(ContextCompat.getColor(fragment.requireContext(), R.color.white))
                } else {
                    holder.tvSCName.setTextColor(ContextCompat.getColor(fragment.requireContext(), R.color.subColor400))
                    holder.tvSCName.backgroundTintList = ContextCompat.getColorStateList(fragment.requireContext(), R.color.subColor100)

                }
                holder.tvSCName.setOnClickListener {
                    onCategoryClickListener.onCategoryClick(sn, currentItem)
                    val previousPosition = selectedPosition
                    selectedPosition = adapterPosition
                    notifyItemChanged(previousPosition) // 이전 선택된 아이템 갱신
                    notifyItemChanged(selectedPosition) // 새로 선택된 아이템 갱신
                    Log.v("subCategoryIndex", "$selectedPosition")
                }
            }
        }
    }

    private fun goExerciseDetail(category : Pair<Int, String>) {
        Log.v("ClickIndex", "category: $category")
        Log.v("EDsn", "$sn")
        fragment.requireActivity().supportFragmentManager.beginTransaction().apply {
//            setCustomAnimations(R.anim.slide_in_left, R.anim.slide_in_right)
            replace(R.id.flMain, ExerciseDetailFragment.newInstance(category, sn))
            addToBackStack(null)
            commit()
        }
    }
}