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
import com.tangoplus.tangoq.data.ExerciseVO
import com.tangoplus.tangoq.data.ProgressUnitVO
import com.tangoplus.tangoq.databinding.RvExerciseHistoryItemBinding
import com.tangoplus.tangoq.databinding.RvExerciseItemBinding
import com.tangoplus.tangoq.databinding.RvRecommendPTnItemBinding
import com.tangoplus.tangoq.db.PreferencesManager
import com.tangoplus.tangoq.listener.OnDialogClosedListener
import com.tomlecollegue.progressbars.HorizontalProgressView
import kotlinx.coroutines.selects.select
import java.lang.IllegalArgumentException


class ExerciseRVAdapter (
    private val fragment: Fragment,
    var exerciseList: MutableList<ExerciseVO>,
    private val progresses : MutableList<ProgressUnitVO>?,
    private val sequence : Pair<Int, Int>?,
    private val history : MutableList<String>?,
    var xmlname: String,
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    var dialogClosedListener: OnDialogClosedListener? = null
    val prefs =  PreferencesManager(fragment.requireContext())

    inner class MainViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivEIThumbnail: ImageView = view.findViewById(R.id.ivEIThumbnail)
        val tvEIName : TextView = view.findViewById(R.id.tvEIName)
        val tvEISymptom : TextView= view.findViewById(R.id.tvEISymptom)
        val tvEITime : TextView= view.findViewById(R.id.tvEITime)
        val ivEIStage : ImageView = view.findViewById(R.id.ivEIStage)
        val tvEIStage : TextView = view.findViewById(R.id.tvEIStage)
        val ibtnEILike : ImageButton= view.findViewById(R.id.ibtnEILike)
        val vEI : View = view.findViewById(R.id.vEI)
        val hpvEI : HorizontalProgressView = view.findViewById(R.id.hpvEI)
        val tvEIFinish : TextView = view.findViewById(R.id.tvEIFinish)
    }

    inner class RecommendViewHolder(view:View) : RecyclerView.ViewHolder(view) {
        val tvRcPName : TextView = view.findViewById(R.id.tvRcPName)
        val tvRcPTime : TextView= view.findViewById(R.id.tvRcPTime)
        val tvRcPStage : TextView = view.findViewById(R.id.tvRcPStage)
        val ivRcPThumbnail : ImageView = view.findViewById(R.id.ivRcPThumbnail)
        val vRPTN : View = view.findViewById(R.id.vRPTN)
    }

    inner class HistoryViewHolder(view:View): RecyclerView.ViewHolder(view) {
        val tvEHIName : TextView = view.findViewById(R.id.tvEHIName)
        val ivEHIThumbnail : ImageView = view.findViewById(R.id.ivEHIThumbnail)
        val tvEHISeq : TextView = view.findViewById(R.id.tvEHISeq)
        val tvEHITime : TextView = view.findViewById(R.id.tvEHITime)
        val tvEHIStage : TextView = view.findViewById(R.id.tvEHIStage)
        val ivEHIStage : ImageView = view.findViewById(R.id.ivEHIStage)
        val hpvEHI : HorizontalProgressView  = view.findViewById(R.id.hpvEHI)
    }

    override fun getItemViewType(position: Int): Int {
        return when (xmlname) {
            "main" -> 0
            "recommend" -> 1
            "history" -> 2
            else -> throw IllegalArgumentException("invalied view type")
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            0 -> {
                val binding = RvExerciseItemBinding.inflate(inflater, parent, false)
                MainViewHolder(binding.root)
            }
            1 -> {
                val binding = RvRecommendPTnItemBinding.inflate(inflater, parent, false)
                RecommendViewHolder(binding.root)
            }
            2 -> {
                val binding = RvExerciseHistoryItemBinding.inflate(inflater, parent, false)
                HistoryViewHolder(binding.root)
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

        val second = "${currentExerciseItem.videoDuration?.toInt()?.div(60)}분 ${currentExerciseItem.videoDuration?.toInt()?.rem(60)}초"

        when (holder) {
            is MainViewHolder -> {
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




                // ------# 시청 기록 #------\
//                if (currentExerciseItem.exerciseId == currentHistoryItem.exerciseId) {
//                    holder.hpvEIHistory.progresses = (currentHistoryItem.timestamp?.div(currentExerciseItem.videoDuration?.toInt()!!))!! * 100
//                }

                // ------# 하트 버튼 #------
                updateLikeButtonState(currentExerciseItem.exerciseId.toString(), holder.ibtnEILike)
                holder.ibtnEILike.setOnClickListener {
                    val currentLikeState = prefs.existLike(currentExerciseItem.exerciseId.toString())
                    if (currentLikeState) {
                        prefs.deleteLike(currentExerciseItem.exerciseId.toString())
                    } else {
                        prefs.storeLike(currentExerciseItem.exerciseId.toString())
                    }
                    // 상태 변경 후 즉시 UI 업데이트
                    updateLikeButtonState(currentExerciseItem.exerciseId.toString(), holder.ibtnEILike)
                }

                // TODO MD2에 맞게 수정해야함
                if (!progresses.isNullOrEmpty() && sequence != null ) {

                    /* Pair(pvm.currentSequence, pvm.selectedSequence.value!!))
                    * 회차를 기준으로 나눠진거임. progresses[position] == 16개의 운동이 담긴 1회차 선택한 회차를 가져옴.
                    * selectSeq.first == currentSeq
                    * selectSeq.second == selectedSeq
                    *
                    * 현재 progresses와 exercise가 크기가 같음.
                    * repeatcount가 1 이상이고 timestamp -1 이면 한 번 본적있는 거.
                    *
                    * */

                    // ------# 시청 기록 및 완료 버튼 #------
                    Log.v("progresses[position]", "progresses[position]: ${progresses[position]}")

                    val currentItem = progresses[position] // 프로그램 갯수만큼의 progresses의 1개에 접근
                    // currentItem의 currentWeek와 currentSequence로 현재 운동의 회차를 계산

                    val currentSeq = sequence.first // 안변함
                    val selectedSeq = sequence.second // 선택된 회차이기 때문에 변함.
//                    val currentUnitsSeq = currentItem.currentSequence // 0,0 으로 나왔다고 쳤을 떄,
                    Log.v("어댑터내Seq", "currentSeq: $currentSeq, selectedSeq: $selectedSeq")

                    val condition = when {
                        selectedSeq == currentSeq -> {
                            if (currentItem.lastProgress > 0) {
                                1
                            } else {
                                2
                            }
                        }
                        selectedSeq < currentSeq -> 0
                        selectedSeq > currentSeq -> 2
                        else -> {
                            2
                        }

                    }
                    when (condition) {
                        0 -> { // 재생 및 완료
                            holder.tvEIFinish.visibility = View.VISIBLE
                            holder.ibtnEILike.visibility = View.INVISIBLE
                            holder.ibtnEILike.isEnabled = false
                            holder.vEI.visibility = View.VISIBLE
                            holder.vEI.backgroundTintList = ContextCompat.getColorStateList(fragment.requireContext(), R.color.secondContainerColor)
                            holder.hpvEI.visibility = View.GONE
                        }
                        1 -> { // 재생 시간 중간
                            holder.tvEIFinish.visibility = View.GONE
                            holder.hpvEI.visibility = View.VISIBLE
                            holder.hpvEI.progress = (currentItem.lastProgress * 100 ) / currentExerciseItem.videoDuration?.toInt()!!
                            Log.v("hpvprogresses", "${holder.hpvEI.progress}")
                        }
                        else -> { // 재생기록 없는 item
                            holder.tvEIFinish.visibility = View.GONE
                            holder.hpvEI.visibility = View.GONE
                        }
                    }
                } else {
                    // ------ # PlayThumbnail #------
                    holder.vEI.setOnClickListener {
                        val dialogFragment = PlayThumbnailDialogFragment().apply {
                            arguments = Bundle().apply {
                                putParcelable("ExerciseUnit", currentExerciseItem)
                            }
                            setDialogCloseListener(object : PlayThumbnailDialogFragment.DialogCloseListener {
                                override fun onDialogClose() {
                                    dialogClosedListener?.onDialogClosed()
                                }
                            })
                        }
                        dialogFragment.show(fragment.requireActivity().supportFragmentManager, "PlayThumbnailDialogFragment")
                    }
                }

                // ------# MD2 #------
                if (history != null) {
                    // ------# 완료된 항목만 들어오니, 테이블에 contentId, user식별자, 만 있고, 그렇게 해도 될 것 같은데?
//                    val currentHistory = history[position]
                    holder.tvEIFinish.visibility = View.VISIBLE
                    holder.ibtnEILike.visibility = View.INVISIBLE
                    holder.ibtnEILike.isEnabled = false
                    holder.vEI.visibility = View.VISIBLE
                    holder.vEI.backgroundTintList = ContextCompat.getColorStateList(fragment.requireContext(), R.color.secondContainerColor)
                    holder.hpvEI.visibility = View.GONE
                    holder.vEI.isEnabled = false
                }
            }
            // ------! play thumbnail 추천 운동 시작 !------
            is RecommendViewHolder -> {
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

            is HistoryViewHolder -> {
                val currentItem = progresses?.get(position)
                holder.tvEHIName.text = currentExerciseItem.exerciseName
                holder.tvEHITime.text = second
                when (currentExerciseItem.exerciseStage) {
                    "초급" -> {
                        holder.ivEHIStage.setImageDrawable(ContextCompat.getDrawable(fragment.requireContext(), R.drawable.icon_stage_1))
                        holder.tvEHIStage.text = "초급자"
                    }
                    "중급" -> {
                        holder.ivEHIStage.setImageDrawable(ContextCompat.getDrawable(fragment.requireContext(), R.drawable.icon_stage_2))
                        holder.tvEHIStage.text = "중급자"
                    }
                    "고급" -> {
                        holder.ivEHIStage.setImageDrawable(ContextCompat.getDrawable(fragment.requireContext(), R.drawable.icon_stage_3))
                        holder.tvEHIStage.text = "상급자"
                    }
                }
                Glide.with(fragment.requireContext())
                    .load("${currentExerciseItem.imageFilePathReal}")
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .override(180)
                    .into(holder.ivEHIThumbnail)
                if (currentItem != null) {
                    holder.hpvEHI.progress = (currentItem.lastProgress * 100 ) / currentExerciseItem.videoDuration?.toInt()!!
                }
                holder.tvEHISeq.text = "${position+1}/${exerciseList.size}"
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

    fun List<ProgressUnitVO>.getExerciseStatuses(currentWeek: Int, currentSeq: Int): List<Int> {
        return this.map { exercise ->
            when {
                exercise.currentWeek < currentWeek ||
                        (exercise.currentWeek == currentWeek && exercise.currentSequence < currentSeq) -> {
                    if (exercise.lastProgress >= exercise.videoDuration) 0
                    else 1
                }
                exercise.currentWeek == currentWeek && exercise.currentSequence == currentSeq -> {
                    if (exercise.lastProgress > 0) 1
                    else 2
                }
                else -> 2
            }
        }
    }

    private fun updateLikeButtonState(exerciseId: String, ibtn : ImageButton) {
        val isLike = prefs.existLike(exerciseId)
        ibtn.setImageDrawable(
            ContextCompat.getDrawable(
                fragment.requireContext(),
                if (isLike) R.drawable.icon_like_enabled else R.drawable.icon_like_disabled
            )
        )
    }
    fun setOnDialogClosedListener(listener: OnDialogClosedListener) {
        dialogClosedListener = listener
    }
}