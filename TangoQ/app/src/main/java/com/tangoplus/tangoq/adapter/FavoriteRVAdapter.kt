package com.tangoplus.tangoq.adapter

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.tangoplus.tangoq.dialog.FavoriteBSDialogFragment
import com.tangoplus.tangoq.listener.OnFavoriteDetailClickListener
import com.tangoplus.tangoq.R
import com.tangoplus.tangoq.data.FavoriteItemVO

import com.tangoplus.tangoq.databinding.RvFvItemBinding

class FavoriteRVAdapter(
    var fvList: MutableList<FavoriteItemVO>,
    val listener: OnFavoriteDetailClickListener,
    private val fragment: Fragment
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    inner class fvViewHolder(view: View): RecyclerView.ViewHolder(view) {
        val vFvTab = view.findViewById<View>(R.id.vFvTab)
        val tvPickName = view.findViewById<TextView>(R.id.tvFvName)
        val tvFvTime = view.findViewById<TextView>(R.id.tvFvTime)
        val tvFvCount = view.findViewById<TextView>(R.id.tvFvCount)
        val tvFvRegDate = view.findViewById<TextView>(R.id.tvFvRegDate)
        val ivFvThumbnail1 = view.findViewById<ImageView>(R.id.ivFvThumbnail1)
        val ivFvThumbnail2 = view.findViewById<ImageView>(R.id.ivFvThumbnail2)
        val ivFvThumbnail3 = view.findViewById<ImageView>(R.id.ivFvThumbnail3)
        val ivFvThumbnail4 = view.findViewById<ImageView>(R.id.ivFvThumbnail4)
        val ivFvThumbnailNull = view.findViewById<ImageView>(R.id.ivFvThumbnailNull)
        val clFvThumbnail4 = view.findViewById<ConstraintLayout>(R.id.clFvThumbnail4)
        val tvFvThumbnailMore = view.findViewById<TextView>(R.id.tvFvThumbnailMore)
        val ibtnFvMore = view.findViewById<ImageButton>(R.id.ibtnFvMore)
        fun setThumbnailVisible(one: Boolean, two: Boolean, three: Boolean, four : Boolean) {
            ivFvThumbnail1.visibility = if (one) View.VISIBLE else View.GONE
            ivFvThumbnail2.visibility = if (two) View.VISIBLE else View.GONE
            ivFvThumbnail3.visibility = if (three) View.VISIBLE else View.GONE
            ivFvThumbnail4.visibility = if (four) View.VISIBLE else View.GONE

        }
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = RvFvItemBinding.inflate(inflater, parent, false)
        return fvViewHolder(binding.root)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val currentItem = fvList[position]


        if (holder is fvViewHolder) {
            holder.ivFvThumbnailNull.visibility = View.GONE
            holder.tvPickName.text = currentItem.favoriteName
            holder.tvFvCount.text = currentItem.favoriteTotalCount
            holder.tvFvTime.text = currentItem.favoriteTotalTime
            holder.tvFvRegDate.text = currentItem.favoriteRegDate?.substring(0, 11)
            holder.vFvTab.setOnClickListener {
                listener.onFavoriteClick(currentItem.favoriteName.toString())
            }
            holder.ibtnFvMore.setOnClickListener {
                val bsFragment = FavoriteBSDialogFragment()
                val bundle = Bundle()
                bundle.putParcelable("Favorite", currentItem)
                bsFragment.arguments = bundle
                val fragmentManager = fragment.requireActivity().supportFragmentManager
                bsFragment.show(fragmentManager, bsFragment.tag)
            }
            val thumbnailCount = currentItem.imgThumbnailList?.size
            Log.v("썸네일리스트갯수", "각 리스트: ${currentItem.imgThumbnailList}, 개수: ${thumbnailCount}")
            when (thumbnailCount) {
                0 -> {
                    holder.ivFvThumbnailNull.visibility = View.VISIBLE
                    holder.setThumbnailVisible(false, false, false, false)
                }
                1 -> {
                    holder.ivFvThumbnailNull.visibility = View.GONE
//                    Glide.with(holder.itemView.context)
//                        .load(currentItem.imgThumbnailList[0])
//                        .diskCacheStrategy(DiskCacheStrategy.ALL)
//                        .into(holder.ivFvThumbnail1)
                    holder.setThumbnailVisible(true, false, false, false)
                }
                2 -> {
                    holder.ivFvThumbnailNull.visibility = View.GONE
                    holder.setThumbnailVisible(true, true, false, false)
//                    Glide.with(holder.itemView.context)
//                        .load(currentItem.imgThumbnailList[0])
//                        .diskCacheStrategy(DiskCacheStrategy.ALL)
//                        .into(holder.ivFvThumbnail2)
//                    Glide.with(holder.itemView.context)
//                        .load(currentItem.imgThumbnailList[1])
//                        .diskCacheStrategy(DiskCacheStrategy.ALL)
//                        .into(holder.ivFvThumbnail2)
                }
                3 -> {
                    holder.ivFvThumbnailNull.visibility = View.GONE
                    holder.setThumbnailVisible(true, true, true, false)
//                    Glide.with(holder.itemView.context)
//                        .load(currentItem.imgThumbnailList[0])
//                        .diskCacheStrategy(DiskCacheStrategy.ALL)
//                        .into(holder.ivFvThumbnail1)
//                    Glide.with(holder.itemView.context)
//                        .load(currentItem.imgThumbnailList[1])
//                        .diskCacheStrategy(DiskCacheStrategy.ALL)
//                        .into(holder.ivFvThumbnail2)
//                    Glide.with(holder.itemView.context)
//                        .load(currentItem.imgThumbnailList[2])
//                        .diskCacheStrategy(DiskCacheStrategy.ALL)
//                        .into(holder.ivFvThumbnail3)
                }
                4 -> {
                    holder.ivFvThumbnailNull.visibility = View.GONE
                    holder.setThumbnailVisible(true, true, true, true)
//                    Glide.with(holder.itemView.context)
//                        .load(currentItem.imgThumbnailList[0])
//                        .diskCacheStrategy(DiskCacheStrategy.ALL)
//                        .into(holder.ivFvThumbnail1)
//                    Glide.with(holder.itemView.context)
//                        .load(currentItem.imgThumbnailList[1])
//                        .diskCacheStrategy(DiskCacheStrategy.ALL)
//                        .into(holder.ivFvThumbnail2)
//                    Glide.with(holder.itemView.context)
//                        .load(currentItem.imgThumbnailList[2])
//                        .diskCacheStrategy(DiskCacheStrategy.ALL)
//                        .into(holder.ivFvThumbnail3)
//                    Glide.with(holder.itemView.context)
//                        .load(currentItem.imgThumbnailList[3])
//                        .diskCacheStrategy(DiskCacheStrategy.ALL)
//                        .into(holder.ivFvThumbnail4)
                }
                else -> { // ------! 5개 이상일 때 !------
                    holder.ivFvThumbnailNull.visibility = View.GONE
                    holder.setThumbnailVisible(true, true, true, true)
                    holder.clFvThumbnail4.visibility = View.VISIBLE
                    holder.tvFvThumbnailMore.visibility = View.VISIBLE
                    if (thumbnailCount != null) {
                        holder.tvFvThumbnailMore.text = "+ ${thumbnailCount - 4}"
                    }
                }
            }

        }
    }

    override fun getItemCount(): Int {
        return fvList.size
    }
}