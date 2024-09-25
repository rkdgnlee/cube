package com.tangoplus.tangoq.adapter

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.res.ColorStateList
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.LinearInterpolator
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.tangoplus.tangoq.dialog.PlayThumbnailDialogFragment
import com.tangoplus.tangoq.R
import com.tangoplus.tangoq.data.EpisodeVO
import com.tangoplus.tangoq.data.ExerciseVO
import com.tangoplus.tangoq.databinding.RvExerciseItemBinding
import com.tangoplus.tangoq.databinding.RvRecommendPTnItemBinding
import com.tangoplus.tangoq.dialog.ExerciseBSDialogFragment
import com.tomlecollegue.progressbars.HorizontalProgressView
import java.lang.IllegalArgumentException


class ExerciseRVAdapter (
    private val fragment: Fragment,
    var exerciseList: MutableList<ExerciseVO>,
    private val episode : EpisodeVO?,
    var xmlname: String
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {


    // -----! main !-----
    inner class mainViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivEIThumbnail: ImageView = view.findViewById(R.id.ivEIThumbnail)
        val tvEIName : TextView = view.findViewById(R.id.tvEIName)
        val tvEISymptom : TextView= view.findViewById(R.id.tvEISymptom)
        val tvEITime : TextView= view.findViewById(R.id.tvEITime)
        val ivEIStage : ImageView = view.findViewById(R.id.ivEIStage)
        val tvEIStage : TextView = view.findViewById(R.id.tvEIStage)
        val tvEIRepeat : TextView = view.findViewById(R.id.tvEIRepeat)
        val ibtnEIMore : ImageButton= view.findViewById(R.id.ibtnEIMore)
        val vEI : View = view.findViewById(R.id.vEI)
        val hpvEI : HorizontalProgressView = view.findViewById(R.id.hpvEI)
        val tvEIFinish : TextView = view.findViewById(R.id.tvEIFinish)
    }

    inner class recommendViewHolder(view:View) : RecyclerView.ViewHolder(view) {
        val tvRcPName : TextView = view.findViewById(R.id.tvRcPName)
        val tvRcPTime : TextView= view.findViewById(R.id.tvRcPTime)
        val tvRcPStage : TextView = view.findViewById(R.id.tvRcPStage)
        val ivRcPThumbnail : ImageView = view.findViewById(R.id.ivRcPThumbnail)
        val vRPTN : View = view.findViewById(R.id.vRPTN)
    }

    override fun getItemViewType(position: Int): Int {
        return when (xmlname) {
            "main" -> 0
            "recommend" -> 1
            else -> throw IllegalArgumentException("invalied view type")
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            0 -> {
                val binding = RvExerciseItemBinding.inflate(inflater, parent, false)
                mainViewHolder(binding.root)
            }
            1 -> {
                val binding = RvRecommendPTnItemBinding.inflate(inflater, parent, false)
                recommendViewHolder(binding.root)
            }
            else -> throw IllegalArgumentException("invalid view type binding")
        }
    }

    override fun getItemCount(): Int {
        return exerciseList.size
    }

    @SuppressLint("ClickableViewAccessibility", "MissingInflatedId", "InflateParams", "SetTextI18n")
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val currentExerciseItem = exerciseList[position]
//        val currentHistoryItem = viewingHistory[position]

        val second = "${currentExerciseItem.videoDuration?.toInt()?.div(60)}분 ${currentExerciseItem.videoDuration?.toInt()?.rem(60)}초"

        when (holder) {
            is mainViewHolder -> {
                // -----! recyclerview에서 운동군 보여주기 !------
                holder.tvEISymptom.text = currentExerciseItem.relatedSymptom.toString()
                holder.tvEIName.text = currentExerciseItem.exerciseName
                holder.tvEITime.text = second
                when (currentExerciseItem.exerciseStage) {
                    "초급" -> {
                        holder.ivEIStage.setImageDrawable(ContextCompat.getDrawable(fragment.requireContext(), R.drawable.icon_stage_1))
                        holder.tvEIStage.text = "초급자"
                    }
                    "중급" -> {
                        holder.ivEIStage.setImageDrawable(ContextCompat.getDrawable(fragment.requireContext(), R.drawable.icon_stage_2))
                        holder.tvEIStage.text = "중급자"
                    }
                    "고급" -> {
                        holder.ivEIStage.setImageDrawable(ContextCompat.getDrawable(fragment.requireContext(), R.drawable.icon_stage_3))
                        holder.tvEIStage.text = "상급자"
                    }
                }


                Glide.with(fragment.requireContext())
                    .load("${currentExerciseItem.imageFilePathReal}")
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .override(180)
                    .into(holder.ivEIThumbnail)

                // ------! 시청 기록 시작 !------\
//                if (currentExerciseItem.exerciseId == currentHistoryItem.exerciseId) {
//                    holder.hpvEIHistory.progress = (currentHistoryItem.timestamp?.div(currentExerciseItem.videoDuration?.toInt()!!))!! * 100
//                }

                // ------! 점점점 버튼 시작 !------
                holder.ibtnEIMore.setOnClickListener {
                    val bsFragment = ExerciseBSDialogFragment()
                    val bundle = Bundle()
                    bundle.putParcelable("exerciseUnit", currentExerciseItem)
                    bsFragment.arguments = bundle
                    val fragmentManager = fragment.requireActivity().supportFragmentManager
                    bsFragment.show(fragmentManager, bsFragment.tag)
                }
                // ------ ! thumbnail 시작 !------
                holder.vEI.setOnClickListener {
                    val dialogFragment = PlayThumbnailDialogFragment().apply {
                        arguments = Bundle().apply {
                            putParcelable("ExerciseUnit", currentExerciseItem)
                        }
                    }
                    dialogFragment.show(fragment.requireActivity().supportFragmentManager, "PlayThumbnailDialogFragment")
                }

                // ------# 시청 기록 및 완료 버튼 #------

                if (episode?.doingExercises != null) {
                    val currentEpisodeItem = episode.doingExercises[position]

                    val condition = if (currentEpisodeItem.regDate != null && currentEpisodeItem.lastPosition!! == 0) {
                        0
                    } else if (currentEpisodeItem.regDate != null && currentEpisodeItem.lastPosition!! > 0) {
                        1
                    } else {
                        2
                    }
                    when (condition) {
                        0 -> { // 재생 및 완료
                            holder.tvEIFinish.visibility = View.VISIBLE
                            holder.ibtnEIMore.visibility = View.INVISIBLE
                            holder.ibtnEIMore.isEnabled = false
                            holder.vEI.visibility = View.VISIBLE
                            holder.vEI.backgroundTintList = ContextCompat.getColorStateList(fragment.requireContext(), R.color.secondPrimaryColor)
                            holder.hpvEI.visibility = View.GONE
                        }
                        1 -> { // 재생 시간 중간
                            holder.tvEIFinish.visibility = View.GONE
                            holder.hpvEI.visibility = View.VISIBLE
                            holder.hpvEI.progress = (currentEpisodeItem.lastPosition!! * 100) / currentExerciseItem.videoDuration?.toInt()!!
                            Log.v("hpvProgress", "${holder.hpvEI.progress}")
                        }
                        else -> { // 재생기록 없는 item
                            holder.tvEIFinish.visibility = View.GONE
                            holder.hpvEI.visibility = View.GONE
                        }
                    }

                }

                // repeatcount가 1 이상이고 timestamp -1 이면 한 번 본적있는 거.




            }
            // ------! play thumbnail 추천 운동 시작 !------
            is recommendViewHolder -> {
                holder.tvRcPName.text = currentExerciseItem.exerciseName
                holder.tvRcPTime.text = second
                holder.tvRcPStage.text = currentExerciseItem.exerciseStage

                Glide.with(holder.itemView.context)
                    .load("${currentExerciseItem.imageFilePathReal}?width=200&height=200")
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .override(200)
                    .into(holder.ivRcPThumbnail)
                holder.vRPTN.setOnClickListener {
                    val dialogFragment = PlayThumbnailDialogFragment().apply {
                        arguments = Bundle().apply {
                            putParcelable("ExerciseUnit", currentExerciseItem)
                        }
                    }
                    dialogFragment.show(fragment.requireActivity().supportFragmentManager, "PlayThumbnailDialogFragment")
                }
            }
        }
    }

    private fun changeBackgroundColor(view:View, startColor: Int, endColor: Int, duration: Long = 350) {
        val animator = ValueAnimator.ofFloat(0f, 1f)
        animator.duration = duration

        animator.addUpdateListener { animation ->
            val fraction = animation.animatedValue as Float
            val newColor = ColorUtils.blendARGB(startColor, endColor, fraction)
            view.backgroundTintList = ColorStateList.valueOf(newColor)
        }
        animator.interpolator = LinearInterpolator()
        animator.start()
    }

}