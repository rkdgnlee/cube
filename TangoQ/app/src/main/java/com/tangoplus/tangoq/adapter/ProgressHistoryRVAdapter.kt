package com.tangoplus.tangoq.adapter

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.GlideException
import com.tangoplus.tangoq.R
import com.tangoplus.tangoq.api.NetworkExercise.fetchExerciseById
import com.tangoplus.tangoq.vo.ProgressHistoryVO
import com.tangoplus.tangoq.databinding.RvProgressHistoryItemBinding
import com.tangoplus.tangoq.dialog.PlayThumbnailDialogFragment
import com.tangoplus.tangoq.fragment.ExtendedFunctions.setOnSingleClickListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import okio.FileNotFoundException

class ProgressHistoryRVAdapter(private val fragment: Fragment, val data: List<ProgressHistoryVO>) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    inner class PHViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvPHITitle : TextView = view.findViewById(R.id.tvPHITitle)
        val tvPHIDuration : TextView = view.findViewById(R.id.tvPHIDuration)
        val ivPHIThumbnail : ImageView = view.findViewById(R.id.ivPHIThumbnail)
        val clPHI: ConstraintLayout = view.findViewById(R.id.clPHI)
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

            holder.clPHI.setOnSingleClickListener{
                CoroutineScope(Dispatchers.IO).launch {
                    // 운동 1개값 가져오기

                    val currentExerciseItem = async { fetchExerciseById(fragment.getString(R.string.API_exercise), currentItem.contentSn.toString()) }
                    val dialogFragment = PlayThumbnailDialogFragment().apply {
                        arguments = Bundle().apply {
                            putParcelable("ExerciseUnit", currentExerciseItem.await())
                        }
                    }
                    dialogFragment.show(fragment.requireActivity().supportFragmentManager, "PlayThumbnailDialogFragment")
                }
            }
        }
    }

    override fun getItemCount(): Int {
        return data.size
    }
}