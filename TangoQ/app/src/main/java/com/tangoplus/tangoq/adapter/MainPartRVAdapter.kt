package com.tangoplus.tangoq.adapter

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
import com.tangoplus.tangoq.data.AnalysisVO
import com.tangoplus.tangoq.data.AnalysisViewModel
import com.tangoplus.tangoq.databinding.RvMainPartItemBinding
import com.tangoplus.tangoq.dialog.MainPartAnalysisDialogFragment
import com.tangoplus.tangoq.dialog.MainPartPoseDialogFragment
import java.security.MessageDigest

class MainPartRVAdapter(private val fragment: Fragment, private val analysizes : MutableList<AnalysisVO>) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    inner class MPViewHolder(view:View) : RecyclerView.ViewHolder(view) {
        val clMPI : ConstraintLayout = view.findViewById(R.id.clMPI)
        val ivMPI: ImageView = view.findViewById(R.id.ivMPI)
        val tvMPITitle: TextView = view.findViewById(R.id.tvMPITitle)
        val tvMPISummary: TextView = view.findViewById(R.id.tvMPISummary)
        val vMPILeft : View = view.findViewById(R.id.vMPILeft)
        val vMPIRight : View = view.findViewById(R.id.vMPIRight)
        val tvMPILeft : TextView = view.findViewById(R.id.tvMPILeft)
        val tvMPIRight : TextView = view.findViewById(R.id.tvMPIRight)
        val ivMPIArrow : ImageView = view.findViewById(R.id.ivMPIArrow)
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

            holder.tvMPITitle.text = matchedSeq.get(currentItem.seq)
            setState(holder, currentItem.isNormal)
            Glide.with(fragment)
                .load(currentItem.url)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .apply(RequestOptions.bitmapTransform(MultiTransformation(
                    CenterCrop(),
                    RoundedCorners(20),
                    FlipHorizontalTransformation()
                )))
                .into(holder.ivMPI)


            holder.ivMPI.setOnClickListener{
                val dialog = MainPartPoseDialogFragment.newInstance(currentItem.seq)
                dialog.show(fragment.requireActivity().supportFragmentManager, "MainPartPoseDialogFragment")
            }

            holder.clMPI.setOnClickListener {
                (avm as AnalysisViewModel).selectedSeq = currentItem.seq
                val dialog = MainPartAnalysisDialogFragment()
                dialog.show(fragment.requireActivity().supportFragmentManager, "MainPartAnalysisDialogFragment")
            }

            holder.tvMPISummary.text = currentItem.summary

//            val balloon2 = Balloon.Builder(fragment.requireContext())
//                .setWidthRatio(0.5f)
//                .setHeight(BalloonSizeSpec.WRAP)
//                .setText("±2° 이내를 정상범위로 취급합니다.")
//                .setTextColorResource(R.color.white)
//                .setTextSize(15f)
//                .setArrowPositionRules(ArrowPositionRules.ALIGN_ANCHOR)
//                .setArrowSize(0)
//                .setMargin(10)
//                .setPadding(12)
//                .setCornerRadius(8f)
//                .setBackgroundColorResource(R.color.mainColor)
//                .setBalloonAnimation(BalloonAnimation.OVERSHOOT)
//                .setLifecycleOwner(fragment.viewLifecycleOwner)
//                .build()
//            holder.ivMPIAlert.setOnClickListener{
//                it.showAlignBottom(balloon2)
//            }

//            // ------# 결과값 #------
//            if (ja != null) {
//                // seq를 통해 jsonObject값 가져오기
//                Log.v("columns", "${columns.map { it.key }}")
//                var resultText = ""
//                columns.get(currentItem.second)?.forEach{ (key, data) ->
//                    val raw = ja.getJSONObject(currentItem.second).optDouble(key)
//                    if (data.contains("기울기")) {
//                        resultText += "$data: ${String.format("%.2f", raw)}°\n"
//                    } else {
//                        resultText += "$data: ${String.format("%.2f", raw)}cm\n"
//                    }
//                }
//                holder.tvMPIResult.text = resultText.removeSuffix("\n")
//            }
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

    private fun setState(holder: MPViewHolder, isNormal: Boolean) {
        val params = holder.ivMPIArrow.layoutParams as ConstraintLayout.LayoutParams
        when (isNormal) {
            false -> {

                holder.vMPILeft.visibility = View.VISIBLE
                holder.vMPIRight.visibility = View.GONE
                holder.tvMPILeft.setTextColor(ContextCompat.getColorStateList(fragment.requireContext(), R.color.deleteColor))
                holder.tvMPIRight.setTextColor(ContextCompat.getColorStateList(fragment.requireContext(), R.color.subColor400))
                params.horizontalBias = 0.125f
            }
            true -> {
                holder.vMPILeft.visibility = View.GONE
                holder.vMPIRight.visibility = View.VISIBLE
                holder.tvMPILeft.setTextColor(ContextCompat.getColorStateList(fragment.requireContext(), R.color.subColor400))
                holder.tvMPIRight.setTextColor(ContextCompat.getColorStateList(fragment.requireContext(), R.color.thirdColor))
                params.horizontalBias = 0.65f
            }
        }
    }

}