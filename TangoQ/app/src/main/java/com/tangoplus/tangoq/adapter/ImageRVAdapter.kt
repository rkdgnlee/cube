package com.tangoplus.tangoq.adapter

import android.graphics.Bitmap
import android.graphics.Matrix
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
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
import com.tangoplus.tangoq.databinding.RvMainPartItemBinding
import com.tangoplus.tangoq.dialog.MainPartPoseDialogFragment
import java.security.MessageDigest

class ImageRVAdapter(private val fragment: Fragment, private val urls: MutableList<Pair<String,Int>>) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    inner class imageViewHolder(view:View) : RecyclerView.ViewHolder(view) {
        val ivMPI: ImageView = view.findViewById(R.id.ivMPI)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = RvMainPartItemBinding.inflate(inflater, parent, false)
        return imageViewHolder(binding.root)
    }

    override fun getItemCount(): Int {
        return urls.size
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val currentItem = urls[position]

        if (holder is imageViewHolder) {
            Glide.with(fragment)
                .load(currentItem.first)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .apply(RequestOptions.bitmapTransform(MultiTransformation(
                    CenterCrop(),
                    RoundedCorners(20),
                    FlipHorizontalTransformation()
                )))
                .into(holder.ivMPI)


            holder.ivMPI.setOnClickListener{
                val dialog = MainPartPoseDialogFragment.newInstance(currentItem.second)
                dialog.show(fragment.requireActivity().supportFragmentManager, "MainPartPoseDialogFragment")
            }
        }


    }

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
}