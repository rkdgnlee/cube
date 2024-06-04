package com.tangoplus.tangoq.adapter

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.tangoplus.tangoq.dialog.FavoriteBSDialogFragment
import com.tangoplus.tangoq.listener.OnFavoriteDetailClickListener
import com.tangoplus.tangoq.R
import com.tangoplus.tangoq.data.FavoriteVO
import com.tangoplus.tangoq.databinding.RvFavoriteAddItemBinding

import com.tangoplus.tangoq.databinding.RvFvItemBinding
import com.tangoplus.tangoq.listener.OnFavoriteSelectedClickListener
import java.io.ByteArrayOutputStream
import java.lang.IllegalArgumentException

class FavoriteRVAdapter(
    var fvList: MutableList<FavoriteVO>,
    val listener: OnFavoriteDetailClickListener,
    private val selectedClickListener: OnFavoriteSelectedClickListener,
    private val fragment: Fragment,
    var xmlName: String,

) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    var selectPosition : Int = -1
    inner class fvViewHolder(view: View): RecyclerView.ViewHolder(view) {
        val cvFv = view.findViewById<CardView>(R.id.cvFv)
        val vFvTab = view.findViewById<View>(R.id.vFvTab)
        val tvPickName = view.findViewById<TextView>(R.id.tvFvName)
        val tvFvTime = view.findViewById<TextView>(R.id.tvFvTime)
        val tvFvCount = view.findViewById<TextView>(R.id.tvFvCount)
        val tvFvRegDate = view.findViewById<TextView>(R.id.tvFvRegDate)
        val ivFvThumbnail1 = view.findViewById<ImageView>(R.id.ivFvThumbnail1)
        val ivFvThumbnail2 = view.findViewById<ImageView>(R.id.ivFvThumbnail2)
        val ivFvThumbnail3 = view.findViewById<ImageView>(R.id.ivFvThumbnail3)
        val ivFvThumbnail4 = view.findViewById<ImageView>(R.id.ivFvThumbnail4)
        val llFvThumbnailBottom = view.findViewById<LinearLayout>(R.id.llFvThumbnailBottom)
        val ivFvThumbnailNull = view.findViewById<ImageView>(R.id.ivFvThumbnailNull)
        val clFvThumbnail4 = view.findViewById<ConstraintLayout>(R.id.clFvThumbnail4)
        val tvFvThumbnailMore = view.findViewById<TextView>(R.id.tvFvThumbnailMore)
        val ibtnFvMore = view.findViewById<ImageButton>(R.id.ibtnFvMore)
        val vFv = view.findViewById<View>(R.id.vFv)
        fun setThumbnailVisible(one: Boolean, two: Boolean, three: Boolean, four : Boolean) {
            ivFvThumbnail1.visibility = if (one) View.VISIBLE else View.GONE
            ivFvThumbnail2.visibility = if (two) View.VISIBLE else View.GONE
            ivFvThumbnail3.visibility = if (three) View.VISIBLE else View.GONE
            ivFvThumbnail4.visibility = if (four) View.VISIBLE else View.GONE
        }
    }

    inner class addViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val cbFvA = view.findViewById<CheckBox>(R.id.cbFvA)
        val tvFvAName = view.findViewById<TextView>(R.id.tvFvAName)
        val tvFvATime = view.findViewById<TextView>(R.id.tvFvATime)
        val tvFvACount = view.findViewById<TextView>(R.id.tvFvACount)
        val tvFvARegDate = view.findViewById<TextView>(R.id.tvFvARegDate)
        val ivFvAThumbnail1 = view.findViewById<ImageView>(R.id.ivFvAThumbnail1)
        val ivFvAThumbnail2 = view.findViewById<ImageView>(R.id.ivFvAThumbnail2)
        val ivFvAThumbnail3 = view.findViewById<ImageView>(R.id.ivFvAThumbnail3)
        val ivFvAThumbnail4 = view.findViewById<ImageView>(R.id.ivFvAThumbnail4)
        val llFvAThumbnailBottom = view.findViewById<LinearLayout>(R.id.llFvAThumbnailBottom)
        val ivFvAThumbnailNull = view.findViewById<ImageView>(R.id.ivFvAThumbnailNull)
        val clFvAThumbnail4 = view.findViewById<ConstraintLayout>(R.id.clFvAThumbnail4)
        val tvFvAThumbnailMore = view.findViewById<TextView>(R.id.tvFvAThumbnailMore)
        val ivFvACheck = view.findViewById<ImageView>(R.id.ivFvACheck)
        val vFvA = view.findViewById<View>(R.id.vFvA)

        fun setThumbnailVisible(one: Boolean, two: Boolean, three: Boolean, four : Boolean) {
            ivFvAThumbnail1.visibility = if (one) View.VISIBLE else View.GONE
            ivFvAThumbnail2.visibility = if (two) View.VISIBLE else View.GONE
            ivFvAThumbnail3.visibility = if (three) View.VISIBLE else View.GONE
            ivFvAThumbnail4.visibility = if (four) View.VISIBLE else View.GONE
        }
    }
    override fun getItemViewType(position: Int): Int {
        return when (xmlName) {
            "main" -> 0
            "add" -> 1
            else -> throw IllegalArgumentException("invalied view type")
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            0 -> {
                val binding = RvFvItemBinding.inflate(inflater, parent, false)
                fvViewHolder(binding.root)
            }
            1 -> {
                val binding = RvFavoriteAddItemBinding.inflate(inflater, parent, false)
                addViewHolder(binding.root)
            }
            else -> throw IllegalArgumentException("invalid view type binding")
        }

    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val currentItem = fvList[position]


        when (holder) {
            is fvViewHolder -> {
                holder.ivFvThumbnailNull.visibility = View.GONE
                holder.tvPickName.text = currentItem.favoriteName
                holder.tvFvCount.text = currentItem.favoriteTotalCount
                holder.tvFvTime.text = currentItem.favoriteTotalTime
                holder.tvFvRegDate.text = currentItem.favoriteRegDate?.substring(0, 11)
                holder.vFvTab.setOnClickListener {
                    listener.onFavoriteClick(currentItem.favoriteName.toString())
                }
                // ------! 바텀시트 썸네일 처리 시작 !------




                holder.ibtnFvMore.setOnClickListener {
                    val bsFragment = FavoriteBSDialogFragment()
                    val bundle = Bundle()


                    val capturedBitmap: Bitmap = captureLayout(holder.cvFv)
                    val byteArray = bitmapToByteArray(capturedBitmap)
                    bundle.putParcelable("Favorite", currentItem)
                    bundle.putByteArray("img", byteArray)
                    Log.v("byteArray", "${byteArray}")


                    bsFragment.arguments = bundle
                    val fragmentManager = fragment.requireActivity().supportFragmentManager
                    bsFragment.show(fragmentManager, bsFragment.tag)
                }
                // ------! 썸네일 처리 시작 !------
                val itemCount = currentItem.favoriteTotalCount?.toInt()
                holder.vFv.visibility = View.INVISIBLE
                Log.v("썸네일리스트갯수", "개수:  ${itemCount}, 각 리스트:${currentItem.imgThumbnails}")
                when (itemCount) {
                    0 -> {
                        holder.ivFvThumbnailNull.visibility = View.VISIBLE
                        holder.setThumbnailVisible(false, false, false, false)
                        holder.tvFvThumbnailMore.visibility = View.INVISIBLE
                        holder.tvFvTime.text = "0"
                    }
                    1 -> {
                        holder.ivFvThumbnailNull.visibility = View.GONE
                        holder.ivFvThumbnail2.visibility = View.GONE
                        holder.llFvThumbnailBottom.visibility = View.GONE
                        Glide.with(holder.itemView.context)
                            .load(currentItem.imgThumbnails!![0])
                            .diskCacheStrategy(DiskCacheStrategy.ALL)
                            .into(holder.ivFvThumbnail1)
                        holder.setThumbnailVisible(true, false, false, false)
                    }
                    2 -> {
                        holder.ivFvThumbnailNull.visibility = View.GONE
                        holder.llFvThumbnailBottom.visibility = View.GONE
                        holder.setThumbnailVisible(true, true, false, false)
                        Glide.with(holder.itemView.context)
                            .load(currentItem.imgThumbnails!![0])
                            .diskCacheStrategy(DiskCacheStrategy.ALL)
                            .into(holder.ivFvThumbnail1)
                        Glide.with(holder.itemView.context)
                            .load(currentItem.imgThumbnails!![1])
                            .diskCacheStrategy(DiskCacheStrategy.ALL)
                            .into(holder.ivFvThumbnail2)
                    }
                    3 -> {
                        holder.ivFvThumbnailNull.visibility = View.GONE
                        holder.clFvThumbnail4.visibility = View.GONE
                        holder.setThumbnailVisible(true, true, true, false)
                        Glide.with(holder.itemView.context)
                            .load(currentItem.imgThumbnails!![0])
                            .diskCacheStrategy(DiskCacheStrategy.ALL)
                            .into(holder.ivFvThumbnail1)
                        Glide.with(holder.itemView.context)
                            .load(currentItem.imgThumbnails!![1])
                            .diskCacheStrategy(DiskCacheStrategy.ALL)
                            .into(holder.ivFvThumbnail2)
                        Glide.with(holder.itemView.context)
                            .load(currentItem.imgThumbnails!![2])
                            .diskCacheStrategy(DiskCacheStrategy.ALL)
                            .into(holder.ivFvThumbnail3)
                    }
                    4 -> {
                        holder.ivFvThumbnailNull.visibility = View.GONE
                        holder.setThumbnailVisible(true, true, true, true)
                        Glide.with(holder.itemView.context)
                            .load(currentItem.imgThumbnails!![0])
                            .diskCacheStrategy(DiskCacheStrategy.ALL)
                            .into(holder.ivFvThumbnail1)
                        Glide.with(holder.itemView.context)
                            .load(currentItem.imgThumbnails!![1])
                            .diskCacheStrategy(DiskCacheStrategy.ALL)
                            .into(holder.ivFvThumbnail2)
                        Glide.with(holder.itemView.context)
                            .load(currentItem.imgThumbnails!![2])
                            .diskCacheStrategy(DiskCacheStrategy.ALL)
                            .into(holder.ivFvThumbnail3)
                        Glide.with(holder.itemView.context)
                            .load(currentItem.imgThumbnails!![3])
                            .diskCacheStrategy(DiskCacheStrategy.ALL)
                            .into(holder.ivFvThumbnail4)
                    }
                    else -> { // ------! 5개 이상일 때 !------
                        holder.ivFvThumbnailNull.visibility = View.GONE
                        holder.setThumbnailVisible(true, true, true, true)
                        Glide.with(holder.itemView.context)
                            .load(currentItem.imgThumbnails!![0])
                            .diskCacheStrategy(DiskCacheStrategy.ALL)
                            .into(holder.ivFvThumbnail1)
                        Glide.with(holder.itemView.context)
                            .load(currentItem.imgThumbnails!![1])
                            .diskCacheStrategy(DiskCacheStrategy.ALL)
                            .into(holder.ivFvThumbnail2)
                        Glide.with(holder.itemView.context)
                            .load(currentItem.imgThumbnails!![2])
                            .diskCacheStrategy(DiskCacheStrategy.ALL)
                            .into(holder.ivFvThumbnail3)
                        Glide.with(holder.itemView.context)
                            .load(currentItem.imgThumbnails!![3])
                            .diskCacheStrategy(DiskCacheStrategy.ALL)
                            .into(holder.ivFvThumbnail4)
                        if (itemCount != null) {
                            holder.vFv.visibility = View.VISIBLE
                            holder.tvFvThumbnailMore.visibility =  View.VISIBLE
                            holder.tvFvThumbnailMore.text = "+ ${itemCount - 4}"
                        }

                    }
                } // ------! 썸네일 처리 끝 !------
            }
            is addViewHolder -> {
                holder.ivFvAThumbnailNull.visibility = View.GONE
                holder.tvFvAName.text = currentItem.favoriteName
                holder.tvFvACount.text = currentItem.favoriteTotalCount
                holder.tvFvATime.text = currentItem.favoriteTotalTime
                holder.tvFvARegDate.text = currentItem.favoriteRegDate?.substring(0, 11)

                // ------! 체크박스 처리 시작 !------
                holder.cbFvA.setOnCheckedChangeListener(null)
                holder.cbFvA.isChecked = position == selectPosition

                if (holder.cbFvA.isChecked) {
                    holder.ivFvACheck.setImageResource(R.drawable.icon_checkbox_enabled)
                } else {
                    holder.ivFvACheck.setImageResource(R.drawable.icon_checkbox_disabled)
                }

                holder.cbFvA.setOnCheckedChangeListener{ _ , isChecked ->
                    if (isChecked) {
                        val previousPosition = selectPosition
                        selectPosition = holder.adapterPosition

                        // 이전 선택 항목의 체크 상태 해제
                        notifyItemChanged(previousPosition)
                        // 현재 선택 항목의 체크 상태 반영
                        notifyItemChanged(selectPosition)
                        selectedClickListener.onFavoriteSelected(currentItem)
                    } else {
                        holder.ivFvACheck.setImageResource(R.drawable.icon_checkbox_disabled)
                    }
                } // ------! 체크박스 처리 시작 !------


                // ------! 썸네일 처리 시작 !------
                val itemCount = currentItem.favoriteTotalCount?.toInt()
//                holder.v.visibility = View.INVISIBLE
                Log.v("썸네일리스트갯수", "개수:  $itemCount")
                when (itemCount) {
                    0 -> {
                        holder.ivFvAThumbnailNull.visibility = View.VISIBLE
                        holder.setThumbnailVisible(false, false, false, false)
                        holder.tvFvAThumbnailMore.visibility = View.INVISIBLE
                        holder.tvFvATime.text = "0"
                    }
                    1 -> {
                        holder.ivFvAThumbnailNull.visibility = View.GONE
                        holder.ivFvAThumbnail2.visibility = View.GONE
                        holder.llFvAThumbnailBottom.visibility = View.GONE
                        Glide.with(holder.itemView.context)
                            .load(currentItem.imgThumbnails!![0])
                            .diskCacheStrategy(DiskCacheStrategy.ALL)
                            .into(holder.ivFvAThumbnail1)
                        holder.setThumbnailVisible(true, false, false, false)
                    }
                    2 -> {
                        holder.ivFvAThumbnailNull.visibility = View.GONE
                        holder.llFvAThumbnailBottom.visibility = View.GONE
                        holder.setThumbnailVisible(true, true, false, false)
                        Glide.with(holder.itemView.context)
                            .load(currentItem.imgThumbnails!![0])
                            .diskCacheStrategy(DiskCacheStrategy.ALL)
                            .into(holder.ivFvAThumbnail1)
                        Glide.with(holder.itemView.context)
                            .load(currentItem.imgThumbnails!![1])
                            .diskCacheStrategy(DiskCacheStrategy.ALL)
                            .into(holder.ivFvAThumbnail2)
                    }
                    3 -> {
                        holder.ivFvAThumbnailNull.visibility = View.GONE
                        holder.clFvAThumbnail4.visibility = View.GONE
                        holder.setThumbnailVisible(true, true, true, false)
                        Glide.with(holder.itemView.context)
                            .load(currentItem.imgThumbnails!![0])
                            .diskCacheStrategy(DiskCacheStrategy.ALL)
                            .into(holder.ivFvAThumbnail1)
                        Glide.with(holder.itemView.context)
                            .load(currentItem.imgThumbnails!![1])
                            .diskCacheStrategy(DiskCacheStrategy.ALL)
                            .into(holder.ivFvAThumbnail2)
                        Glide.with(holder.itemView.context)
                            .load(currentItem.imgThumbnails!![2])
                            .diskCacheStrategy(DiskCacheStrategy.ALL)
                            .into(holder.ivFvAThumbnail3)
                    }
                    4 -> {
                        holder.ivFvAThumbnailNull.visibility = View.GONE
                        holder.setThumbnailVisible(true, true, true, true)
                        Glide.with(holder.itemView.context)
                            .load(currentItem.imgThumbnails!![0])
                            .diskCacheStrategy(DiskCacheStrategy.ALL)
                            .into(holder.ivFvAThumbnail1)
                        Glide.with(holder.itemView.context)
                            .load(currentItem.imgThumbnails!![1])
                            .diskCacheStrategy(DiskCacheStrategy.ALL)
                            .into(holder.ivFvAThumbnail2)
                        Glide.with(holder.itemView.context)
                            .load(currentItem.imgThumbnails!![2])
                            .diskCacheStrategy(DiskCacheStrategy.ALL)
                            .into(holder.ivFvAThumbnail3)
                        Glide.with(holder.itemView.context)
                            .load(currentItem.imgThumbnails!![3])
                            .diskCacheStrategy(DiskCacheStrategy.ALL)
                            .into(holder.ivFvAThumbnail4)
                    }
                    else -> { // ------! 5개 이상일 때 !------
                        holder.ivFvAThumbnailNull.visibility = View.GONE
                        holder.setThumbnailVisible(true, true, true, true)
                        Glide.with(holder.itemView.context)
                            .load(currentItem.imgThumbnails!![0])
                            .diskCacheStrategy(DiskCacheStrategy.ALL)
                            .into(holder.ivFvAThumbnail1)
                        Glide.with(holder.itemView.context)
                            .load(currentItem.imgThumbnails!![1])
                            .diskCacheStrategy(DiskCacheStrategy.ALL)
                            .into(holder.ivFvAThumbnail2)
                        Glide.with(holder.itemView.context)
                            .load(currentItem.imgThumbnails!![2])
                            .diskCacheStrategy(DiskCacheStrategy.ALL)
                            .into(holder.ivFvAThumbnail3)
                        Glide.with(holder.itemView.context)
                            .load(currentItem.imgThumbnails!![3])
                            .diskCacheStrategy(DiskCacheStrategy.ALL)
                            .into(holder.ivFvAThumbnail4)
                        if (itemCount != null) {
                            holder.vFvA.visibility = View.VISIBLE
                            holder.tvFvAThumbnailMore.visibility =  View.VISIBLE
                            holder.tvFvAThumbnailMore.text = "+ ${itemCount - 4}"
                        }

                    }
                } // ------! 썸네일 처리 끝 !------
            }
        }

    }

    override fun getItemCount(): Int {
        return fvList.size
    }

    private fun captureLayout(view: View): Bitmap {
        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        view.draw(canvas)
        return bitmap
    }

    private fun bitmapToByteArray(bitmap: Bitmap): ByteArray {
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        return stream.toByteArray()
    }
}