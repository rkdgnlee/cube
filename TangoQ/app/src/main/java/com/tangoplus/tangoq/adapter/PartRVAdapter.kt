package com.tangoplus.tangoq.adapter

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool
import com.bumptech.glide.load.resource.bitmap.BitmapTransformation
import com.tangoplus.tangoq.R
import com.tangoplus.tangoq.databinding.RvPartItemBinding
import com.tangoplus.tangoq.fragment.MeasureAnalysisFragment
import java.security.MessageDigest

class PartRVAdapter(private val fragment: Fragment, private val dangerParts:  MutableList<Pair<String, Float>>? ) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    inner class PartViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvPI : TextView = view.findViewById(R.id.tvPI)
        val cvPI : CardView = view.findViewById(R.id.cvPI)
        val ivPI : ImageView = view.findViewById(R.id.ivPI)
        val clPI : ConstraintLayout = view.findViewById(R.id.clPI)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = RvPartItemBinding.inflate(inflater, parent, false)
        return PartViewHolder(binding.root)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is PartViewHolder) {
            val currentItem = dangerParts?.get(position)
            holder.tvPI.text = currentItem?.first
            val state = when (currentItem?.second) {
                1f -> true
                else -> false
            }
            setPartItem(state, holder.clPI, holder.cvPI, holder.tvPI)

            val drawableId = when (currentItem?.first) {
                "목관절" -> R.drawable.icon_part1
                "우측 어깨", "좌측 어깨" -> R.drawable.icon_part2
                "우측 팔꿉", "좌측 팔꿉" -> R.drawable.icon_part3
                "우측 손목", "좌측 손목" -> R.drawable.icon_part4
                "우측 골반", "좌측 골반" -> R.drawable.icon_part5
                "우측 무릎", "좌측 무릎" -> R.drawable.icon_part6
                "우측 발목", "좌측 발목" -> R.drawable.icon_part7
                else -> -1
            }
            val isRight = if (currentItem?.first?.contains("우측") == true) true else false
            if (drawableId != -1) {
                loadFlippedImage(holder, drawableId, isRight)
            }

            holder.clPI.setOnClickListener {
                fragment.requireActivity().supportFragmentManager.beginTransaction().apply {
                    replace(R.id.flMain, MeasureAnalysisFragment.newInstance(currentItem?.first ?: ""))
                    addToBackStack(null)
                    commit()
                }
            }
        }
    }

    override fun getItemCount(): Int {
        return dangerParts?.size ?: 0
    }

    private fun setPartItem(isWarning: Boolean, cl: ConstraintLayout, cv: CardView, tv: TextView){
        if (isWarning) {
            cl.backgroundTintList = ContextCompat.getColorStateList(fragment.requireContext(), R.color.cautionContainerColor)
            cv.setCardBackgroundColor(fragment.resources.getColor(R.color.cautionColor, null))
        } else {
            cl.backgroundTintList = ContextCompat.getColorStateList(fragment.requireContext(), R.color.deleteContainerColor)
            cv.setCardBackgroundColor(fragment.resources.getColor(R.color.deleteColor, null))
        }
    }
    private fun loadFlippedImage(holder: PartViewHolder, resourceId: Int, isRight: Boolean) {
        if (isRight) {
            Glide.with(fragment.requireContext())
                .load(resourceId)
                .transform(object : BitmapTransformation() {
                    override fun transform(pool: BitmapPool, toTransform: Bitmap, outWidth: Int, outHeight: Int): Bitmap {
                        val matrix = Matrix().apply {
                            postScale(-1f, 1f, toTransform.width / 2f, toTransform.height / 2f)
                        }

                        val result = pool.get(toTransform.width, toTransform.height, toTransform.config)
                        val canvas = Canvas(result)
                        canvas.drawBitmap(toTransform, matrix, Paint(Paint.FILTER_BITMAP_FLAG))

                        return result
                    }

                    override fun updateDiskCacheKey(messageDigest: MessageDigest) {
                        messageDigest.update("flip_transformation".toByteArray())
                    }
                })
                .into(holder.ivPI)
        } else {
            holder.ivPI.setImageResource(resourceId)
        }
    }
}