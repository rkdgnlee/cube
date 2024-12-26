package com.tangoplus.tangoq.adapter

import android.annotation.SuppressLint
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.Matrix
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.MultiTransformation
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool
import com.bumptech.glide.load.resource.bitmap.BitmapTransformation
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestOptions
import com.tangoplus.tangoq.R
import com.tangoplus.tangoq.vo.AnalysisVO
import com.tangoplus.tangoq.viewmodel.AnalysisViewModel
import com.tangoplus.tangoq.databinding.RvMainPartItemBinding
import com.tangoplus.tangoq.dialog.MainPartPoseDialogFragment
import java.security.MessageDigest

class MainPartRVAdapter(private val fragment: Fragment, private val analysizes : MutableList<AnalysisVO>) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    inner class MPViewHolder(view:View) : RecyclerView.ViewHolder(view) {
        val clMPI : ConstraintLayout = view.findViewById(R.id.clMPI)
        val ivMPI: ImageView = view.findViewById(R.id.ivMPI)
        val tvMPITitle: TextView = view.findViewById(R.id.tvMPITitle)
        val tvMPISummary: TextView = view.findViewById(R.id.tvMPISummary)
        val tvMPIState : TextView = view.findViewById(R.id.tvMPIState)
    }

    var avm : ViewModel? = null
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = RvMainPartItemBinding.inflate(inflater, parent, false)
        return MPViewHolder(binding.root)
    }

    override fun getItemCount(): Int {
        return analysizes.size
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val currentItem = analysizes[position]

        if (holder is MPViewHolder) {

            holder.tvMPITitle.text = matchedSeq[currentItem.seq]
            setState(holder, currentItem.isNormal)
            Glide.with(fragment.requireContext())
                .load(currentItem.url)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .apply(RequestOptions.bitmapTransform(MultiTransformation(
                    CenterCrop(),
                    RoundedCorners(20),
                    FlipHorizontalTransformation()
                )))
                .into(holder.ivMPI)


            holder.clMPI.setOnClickListener{
                (avm as AnalysisViewModel).selectedSeq = currentItem.seq
                val dialog = MainPartPoseDialogFragment.newInstance(currentItem.seq)
                dialog.show(fragment.requireActivity().supportFragmentManager, "MainPartPoseDialogFragment")
            }

            holder.tvMPISummary.text = currentItem.summary
        }
    }

    // ------# 좌우 반전 #------
    inner class FlipHorizontalTransformation : BitmapTransformation() {
        override fun updateDiskCacheKey(messageDigest: MessageDigest) {
            messageDigest.update("flip_horizontal".toByteArray())
        }
        override fun transform(
            pool: BitmapPool,
            toTransform: Bitmap,
            outWidth: Int,
            outHeight: Int
        ): Bitmap {
            val matrix = Matrix().apply { postScale(-1f, 1f, toTransform.width / 2f, toTransform.height / 2f) }
            return Bitmap.createBitmap(toTransform, 0, 0, toTransform.width, toTransform.height, matrix, true)
        }
    }

    private val matchedSeq = mapOf(
        0 to "정면 측정",
        1 to "동적 측정",
        2 to "팔꿉 측정",
        3 to "좌측 측정",
        4 to "우측 측정",
        5 to "후면 측정",
        6 to "앉아 후면"
    )

    @SuppressLint("UseCompatTextViewDrawableApis")
    private fun setState(holder: MPViewHolder, isNormal: Int) {
        when (isNormal) {
            2, 3 -> {
                holder.tvMPIState.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(fragment.requireContext(), R.color.deleteColor))
                holder.tvMPIState.setTextColor(ContextCompat.getColorStateList(fragment.requireContext(), R.color.white))
                holder.tvMPIState.compoundDrawableTintList = ColorStateList.valueOf(ContextCompat.getColor(fragment.requireContext(), R.color.white))
                holder.tvMPIState.text = "확인 필요"
            }
            else -> {
                holder.tvMPIState.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(fragment.requireContext(), R.color.subColor100))
                holder.tvMPIState.setTextColor(ContextCompat.getColorStateList(fragment.requireContext(), R.color.subColor700))
                holder.tvMPIState.compoundDrawableTintList = ColorStateList.valueOf(ContextCompat.getColor(fragment.requireContext(), R.color.subColor700))
                holder.tvMPIState.text = "평균 수치"
            }
        }
    }

}