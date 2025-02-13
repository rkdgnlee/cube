package com.tangoplus.tangoq.adapter

import android.annotation.SuppressLint
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.GlideException
import com.tangoplus.tangoq.R
import com.tangoplus.tangoq.vo.ProgressHistoryVO
import com.tangoplus.tangoq.databinding.RvProgressHistoryItemBinding
import okio.FileNotFoundException

class ProgressHistoryRVAdapter(private val fragment: Fragment, val data: List<ProgressHistoryVO>) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    inner class PHViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvPHITitle : TextView = view.findViewById(R.id.tvPHITitle)
        val tvPHITime : TextView = view.findViewById(R.id.tvPHITime)
        val tvPHIDuration : TextView = view.findViewById(R.id.tvPHIDuration)
        val ivPHIThumbnail : ImageView = view.findViewById(R.id.ivPHIThumbnail)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val binding = RvProgressHistoryItemBinding.inflate(layoutInflater, parent, false)
        return PHViewHolder(binding.root)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val currentItem = data[position]

        if (holder is PHViewHolder) {

            holder.tvPHITime.text = "완료한 시간 : ${currentItem.createdAt.substring(10, currentItem.createdAt.length - 3)}"
            holder.tvPHITitle.text = currentItem.exerciseName
            val second = "${currentItem.duration?.div(60)}분 ${currentItem.duration?.rem(60)}초"
            holder.tvPHIDuration.text = if (currentItem.duration == null) "0분" else second
            try {
                Glide.with(fragment.requireContext())
                    .load(currentItem.imageFilePathReal)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .override(180)
                    .into(holder.ivPHIThumbnail)
            } catch (e: GlideException) {
                Log.e("PHIGlideError", "glideException: ${e.message}")
            } catch (e: FileNotFoundException) {
                Log.e("PHIGlideError", "fileNotFoundException: ${e.message}")
            } catch (e: Exception) {
                Log.e("PHIGlideError", "Exception: ${e.message}")
            }

        }
    }

    override fun getItemCount(): Int {
        return data.size
    }

}