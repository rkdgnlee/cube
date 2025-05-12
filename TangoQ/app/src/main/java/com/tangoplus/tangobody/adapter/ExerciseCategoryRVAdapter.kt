package com.tangoplus.tangobody.adapter

import android.graphics.Color
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
import com.tangoplus.tangobody.R
import com.tangoplus.tangobody.databinding.RvExerciseMainCateogoryItemBinding
import com.tangoplus.tangobody.databinding.RvExerciseSubCategoryItemBinding
import com.tangoplus.tangobody.fragment.ExerciseDetailFragment
import com.tangoplus.tangobody.fragment.ExtendedFunctions.setOnSingleClickListener
import com.tangoplus.tangobody.listener.OnCategoryClickListener
import com.tangoplus.tangobody.viewmodel.ExerciseViewModel
import com.tangoplus.tangobody.viewmodel.FragmentViewModel

class ExerciseCategoryRVAdapter(private val mainCategorys: List<ArrayList<Int>>,
                                private val subCategorys: List<Pair<String, Int?>>, // subCategory는 Pair<관절이름, 운동 갯수>
                                private val fragment: Fragment,
                                private val sn : Int?,
                                private val evm : ExerciseViewModel,
                                private val fvm: FragmentViewModel,
                                private var xmlname: String
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    var onCategoryClickListener: OnCategoryClickListener? = null
    private var selectedPosition = 0

    inner class MainCategoryViewHolder(view:View): RecyclerView.ViewHolder(view) {
        val ivMCThumbnail : ImageView = view.findViewById(R.id.ivMCThumbnail)
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
                    .diskCacheStrategy(DiskCacheStrategy.NONE)
                    .skipMemoryCache(true)
                    .into(holder.ivMCThumbnail)
                holder.ivMCThumbnail.setOnSingleClickListener{
                    goExerciseDetail(currentItemMain)
                }
            }
            is SubCategoryViewHolder -> {
                val currentItem = subCategorys[position]
                val categoryName=  currentItem.first + " (${currentItem.second})"
                holder.tvSCName.text = categoryName
                val adapterPosition = holder.bindingAdapterPosition
                holder.tvSCName.setBackgroundResource(R.drawable.bckgnd_rectangle_20)
                if (adapterPosition == selectedPosition) {
                    holder.tvSCName.backgroundTintList = ContextCompat.getColorStateList(fragment.requireContext(), R.color.secondaryColor)
                    holder.tvSCName.setTextColor(Color.WHITE)
                } else {
                    holder.tvSCName.setTextColor(ContextCompat.getColor(fragment.requireContext(), R.color.subColor400))
                    holder.tvSCName.backgroundTintList = ContextCompat.getColorStateList(fragment.requireContext(), R.color.subColor100)
                }

                holder.tvSCName.setOnSingleClickListener {
                    onCategoryClickListener?.onCategoryClick(currentItem.first)
                    val previousPosition = selectedPosition
                    selectedPosition = adapterPosition
                    notifyItemChanged(previousPosition) // 이전 선택된 아이템 갱신
                    notifyItemChanged(selectedPosition) // 새로 선택된 아이템 갱신
                }
            }
        }
    }

    private fun goExerciseDetail(category : ArrayList<Int>) {
//        Log.v("ClickIndex", "category: $category")
//        Log.v("EDsn", "$sn")
        evm.categoryId = category
        evm.sn = sn
        fvm.setCurrentFragment(FragmentViewModel.FragmentType.EXERCISE_DETAIL_FRAGMENT)
        fragment.requireActivity().supportFragmentManager.beginTransaction().apply {
            replace(R.id.flMain, ExerciseDetailFragment())
            commit()
        }
    }
}