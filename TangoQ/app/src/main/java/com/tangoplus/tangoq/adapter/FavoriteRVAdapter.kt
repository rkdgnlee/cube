package com.tangoplus.tangoq.adapter

import android.annotation.SuppressLint
import android.content.Context
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
    private var fvList: MutableList<FavoriteVO>,
    val listener: OnFavoriteDetailClickListener,
    private val selectedClickListener: OnFavoriteSelectedClickListener,
    private val fragment: Fragment,
    private var xmlName: String,

    ) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private var selectPosition : Int = -1
    inner class FvViewHolder(view: View): RecyclerView.ViewHolder(view) {
        val cvFv : CardView= view.findViewById(R.id.cvFv)
        val vFvTab : View = view.findViewById(R.id.vFvTab)
        val tvPickName : TextView = view.findViewById(R.id.tvFvName)
        val tvFvTime : TextView = view.findViewById(R.id.tvFvTime)
        val tvFvCount : TextView = view.findViewById(R.id.tvFvCount)
        val tvFvRegDate : TextView = view.findViewById(R.id.tvFvRegDate)
        val ivFvThumbnail1 : ImageView = view.findViewById(R.id.ivFvThumbnail1)
        val ivFvThumbnail2 : ImageView = view.findViewById(R.id.ivFvThumbnail2)
        val ivFvThumbnail3 : ImageView = view.findViewById(R.id.ivFvThumbnail3)
        val ivFvThumbnail4 : ImageView = view.findViewById(R.id.ivFvThumbnail4)
        val llFvThumbnailBottom : LinearLayout = view.findViewById(R.id.llFvThumbnailBottom)
        val ivFvThumbnailNull : ImageView = view.findViewById(R.id.ivFvThumbnailNull)
        val clFvThumbnail4 : ConstraintLayout = view.findViewById(R.id.clFvThumbnail4)
        val tvFvThumbnailMore : TextView = view.findViewById(R.id.tvFvThumbnailMore)
        val ibtnFvMore : ImageButton = view.findViewById(R.id.ibtnFvMore)
        val vFv : View = view.findViewById(R.id.vFv)
        fun setThumbnailGone() {
            ivFvThumbnail1.visibility = View.GONE
            ivFvThumbnail2.visibility = View.GONE
            ivFvThumbnail3.visibility = View.GONE
            ivFvThumbnail4.visibility = View.GONE
        }
    }

    inner class AddViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val cbFvA : CheckBox = view.findViewById(R.id.cbFvA)
        val tvFvAName : TextView = view.findViewById(R.id.tvFvAName)
        val tvFvATime : TextView = view.findViewById(R.id.tvFvATime)
        val tvFvACount : TextView = view.findViewById(R.id.tvFvACount)
        val tvFvARegDate : TextView = view.findViewById(R.id.tvFvARegDate)
        val ivFvAThumbnail1 : ImageView = view.findViewById(R.id.ivFvAThumbnail1)
        val ivFvAThumbnail2 : ImageView = view.findViewById(R.id.ivFvAThumbnail2)
        val ivFvAThumbnail3 : ImageView = view.findViewById(R.id.ivFvAThumbnail3)
        val ivFvAThumbnail4 : ImageView = view.findViewById(R.id.ivFvAThumbnail4)
        val llFvAThumbnailBottom : LinearLayout = view.findViewById(R.id.llFvAThumbnailBottom)
        val ivFvAThumbnailNull : ImageView = view.findViewById(R.id.ivFvAThumbnailNull)
        val clFvAThumbnail4 : ConstraintLayout = view.findViewById(R.id.clFvAThumbnail4)
        val tvFvAThumbnailMore : TextView = view.findViewById(R.id.tvFvAThumbnailMore)
        val ivFvACheck : ImageView = view.findViewById(R.id.ivFvACheck)
        val vFvA : View = view.findViewById(R.id.vFvA)

        fun setThumbnailGone() {
            ivFvAThumbnail1.visibility = View.GONE
            ivFvAThumbnail2.visibility = View.GONE
            ivFvAThumbnail3.visibility = View.GONE
            ivFvAThumbnail4.visibility = View.GONE
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
                FvViewHolder(binding.root)
            }
            1 -> {
                val binding = RvFavoriteAddItemBinding.inflate(inflater, parent, false)
                AddViewHolder(binding.root)
            }
            else -> throw IllegalArgumentException("invalid view type binding")
        }

    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val currentItem = fvList[position]


        when (holder) {
            is FvViewHolder -> {
                holder.ivFvThumbnailNull.visibility = View.GONE
                holder.tvPickName.text = currentItem.favoriteName
                holder.tvFvCount.text = "${currentItem.favoriteTotalCount} 개"
                holder.tvFvTime.text = "${currentItem.favoriteTotalTime?.toInt()?.div(60)}분 ${currentItem.favoriteTotalTime?.toInt()?.rem(60)}초"
                holder.tvFvRegDate.text = currentItem.favoriteRegDate?.substring(0, 11)
                holder.vFvTab.setOnClickListener {
                    listener.onFavoriteClick(currentItem.favoriteSn)
                }
                // ------! 바텀시트 썸네일 처리 시작 !------
                holder.ibtnFvMore.setOnClickListener {
                    val bsFragment = FavoriteBSDialogFragment()
                    val bundle = Bundle()


                    val capturedBitmap: Bitmap = captureLayout(holder.cvFv)
                    val byteArray = bitmapToByteArray(capturedBitmap)
                    bundle.putParcelable("Favorite", currentItem)
                    bundle.putByteArray("img", byteArray)
                    Log.v("byteArray", "$byteArray")


                    bsFragment.arguments = bundle
                    val fragmentManager = fragment.requireActivity().supportFragmentManager
                    bsFragment.show(fragmentManager, bsFragment.tag)
                }

                // ------! 썸네일 처리 시작 !------
                val itemCount = currentItem.favoriteTotalCount?.toInt()
                holder.vFv.visibility = View.INVISIBLE
//                Log.v("썸네일리스트갯수", "개수:  ${itemCount}, 각 리스트:${currentItem.imgThumbnails}")
                when (itemCount) {
                    0 -> {
                        holder.ivFvThumbnailNull.visibility = View.VISIBLE
                        holder.tvFvThumbnailMore.visibility = View.INVISIBLE
                        holder.setThumbnailGone()
                        holder.tvFvTime.text = "0"
                    }
                    1 -> {
                        val list = listOf(holder.ivFvThumbnail1)
                        holder.ivFvThumbnailNull.visibility = View.GONE
                        holder.ivFvThumbnail2.visibility = View.GONE
                        holder.llFvThumbnailBottom.visibility = View.GONE
                        setThumbnails(holder, currentItem.imgThumbnails!!.take(1), list , listOf(true, false, false, false))
                    }
                    2 -> {
                        val list = listOf(holder.ivFvThumbnail1, holder.ivFvThumbnail2)
                        holder.ivFvThumbnailNull.visibility = View.GONE
                        holder.llFvThumbnailBottom.visibility = View.GONE
                        setThumbnails(holder, currentItem.imgThumbnails!!.take(2), list , listOf(true, true, false, false))
                    }
                    3 -> {
                        val list = listOf(holder.ivFvThumbnail1, holder.ivFvThumbnail2, holder.ivFvThumbnail3)
                        holder.ivFvThumbnailNull.visibility = View.GONE
                        holder.clFvThumbnail4.visibility = View.GONE
                        setThumbnails(holder, currentItem.imgThumbnails!!.take(3), list , listOf(true, true, true, false))

                    }
                    4 -> {
                        val list = listOf(holder.ivFvThumbnail1, holder.ivFvThumbnail2, holder.ivFvThumbnail3, holder.ivFvThumbnail4)
                        holder.ivFvThumbnailNull.visibility = View.GONE
                        setThumbnails(holder, currentItem.imgThumbnails!!.take(4), list , listOf(true, true, true, true))
                    }
                    else -> { // ------! 5개 이상일 때 !------
                        val list = listOf(holder.ivFvThumbnail1, holder.ivFvThumbnail2, holder.ivFvThumbnail3, holder.ivFvThumbnail4)
                        holder.ivFvThumbnailNull.visibility = View.GONE
                        holder.vFv.visibility = View.VISIBLE
                        holder.tvFvThumbnailMore.visibility = View.VISIBLE
                        setThumbnails(holder, currentItem.imgThumbnails!!.take(4), list , listOf(true, true, true, true))
                        if (itemCount != null) {
                            holder.tvFvThumbnailMore.text = "+ ${itemCount - 4}"
                        }

                    }
                } // ------! 썸네일 처리 끝 !------
            }
            is AddViewHolder -> {
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
                        holder.tvFvAThumbnailMore.visibility = View.INVISIBLE
                        holder.setThumbnailGone()
                        holder.tvFvATime.text = "0"
                    }
                    1 -> {
                        val list = listOf(holder.ivFvAThumbnail1)
                        holder.ivFvAThumbnailNull.visibility = View.GONE
                        holder.ivFvAThumbnail2.visibility = View.GONE
                        holder.llFvAThumbnailBottom.visibility = View.GONE
                        setThumbnails(holder, currentItem.imgThumbnails!!.take(1), list , listOf(true, false, false, false))
                    }
                    2 -> {
                        val list = listOf(holder.ivFvAThumbnail1, holder.ivFvAThumbnail2)
                        holder.ivFvAThumbnailNull.visibility = View.GONE
                        holder.llFvAThumbnailBottom.visibility = View.GONE
                        setThumbnails(holder, currentItem.imgThumbnails!!.take(2), list , listOf(true, true, false, false))
                    }
                    3 -> {
                        val list = listOf(holder.ivFvAThumbnail1, holder.ivFvAThumbnail2, holder.ivFvAThumbnail3)
                        holder.ivFvAThumbnailNull.visibility = View.GONE
                        holder.clFvAThumbnail4.visibility = View.GONE
                        setThumbnails(holder, currentItem.imgThumbnails!!.take(3), list , listOf(true, true, true, false))

                    }
                    4 -> {
                        val list = listOf(holder.ivFvAThumbnail1, holder.ivFvAThumbnail2, holder.ivFvAThumbnail3, holder.ivFvAThumbnail4)
                        holder.ivFvAThumbnailNull.visibility = View.GONE
                        setThumbnails(holder, currentItem.imgThumbnails!!.take(4), list , listOf(true, true, true, true))
                    }
                    else -> { // ------! 5개 이상일 때 !------
                        val list = listOf(holder.ivFvAThumbnail1, holder.ivFvAThumbnail2, holder.ivFvAThumbnail3, holder.ivFvAThumbnail4)
                        holder.ivFvAThumbnailNull.visibility = View.GONE
                        holder.vFvA.visibility = View.VISIBLE
                        holder.tvFvAThumbnailMore.visibility = View.VISIBLE
                        setThumbnails(holder, currentItem.imgThumbnails!!.take(4), list , listOf(true, true, true, true))
                        if (itemCount != null) {
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
    private fun loadImage(context: Context, url: String, imageView: ImageView) {
        Glide.with(context)
            .load(url)
            .override(100)
            .into(imageView)
    }

    private fun setThumbnails(holder: RecyclerView.ViewHolder, urls: List<String>, imageViews: List<ImageView>, visibilityFlags: List<Boolean>) {
        imageViews.forEachIndexed{ index, imageView ->
            if (index < urls.size) {
                loadImage(holder.itemView.context, urls[index], imageView)
                imageView.visibility = if (visibilityFlags[index]) View.VISIBLE else View.GONE
            } else {
                imageView.visibility = View.GONE
            }
        }
    }
}