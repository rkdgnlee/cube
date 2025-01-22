package com.tangoplus.tangoq.adapter

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.skydoves.progressview.ProgressView
import com.tangoplus.tangoq.PlayFullScreenActivity
import com.tangoplus.tangoq.dialog.PlayThumbnailDialogFragment
import com.tangoplus.tangoq.R
import com.tangoplus.tangoq.vo.ExerciseVO
import com.tangoplus.tangoq.vo.ProgressUnitVO
import com.tangoplus.tangoq.databinding.RvExerciseHistoryItemBinding
import com.tangoplus.tangoq.databinding.RvExerciseItemBinding
import com.tangoplus.tangoq.databinding.RvRecommendPTnItemBinding
import com.tangoplus.tangoq.function.PreferencesManager
import com.tangoplus.tangoq.listener.OnDialogClosedListener
import com.tangoplus.tangoq.listener.OnSingleClickListener
import com.tangoplus.tangoq.vo.ExerciseHistoryVO
import java.lang.IllegalArgumentException


class ExerciseRVAdapter (
    private val fragment: Fragment,
    var exerciseList: MutableList<ExerciseVO>?,
    private val progresses : MutableList<ProgressUnitVO>?,
    private val historys: MutableList<ExerciseHistoryVO>?,
    private val sequence : Pair<Int, Int>?,
    private val MDHistory : MutableList<String>?,
    private var xmlName: String,
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    var dialogClosedListener: OnDialogClosedListener? = null
    val prefs =  PreferencesManager(fragment.requireContext())
    private val startIndex = progresses?.let { findCurrentIndex(it) }
    inner class MainViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivEIThumbnail: ImageView = view.findViewById(R.id.ivEIThumbnail)
        val tvEIName : TextView = view.findViewById(R.id.tvEIName)
        val tvEISymptom : TextView= view.findViewById(R.id.tvEISymptom)
        val tvEITime : TextView= view.findViewById(R.id.tvEITime)
        val ivEIStage : ImageView = view.findViewById(R.id.ivEIStage)
        val tvEIStage : TextView = view.findViewById(R.id.tvEIStage)
//        val ibtnEILike : ImageButton= view.findViewById(R.id.ibtnEILike)
        val vEI : View = view.findViewById(R.id.vEI)
        val hpvEI : ProgressView = view.findViewById(R.id.hpvEI)
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
        val clEHI : ConstraintLayout = view.findViewById(R.id.clEHI)
        val tvEHIName : TextView = view.findViewById(R.id.tvEHIName)
        val ivEHIThumbnail : ImageView = view.findViewById(R.id.ivEHIThumbnail)
        val tvEHISeq : TextView = view.findViewById(R.id.tvEHISeq)
        val tvEHITime : TextView = view.findViewById(R.id.tvEHITime)
        val tvEHIStage : TextView = view.findViewById(R.id.tvEHIStage)
        val ivEHIStage : ImageView = view.findViewById(R.id.ivEHIStage)
        val hpvEHI : ProgressView  = view.findViewById(R.id.hpvEHI)
    }

    override fun getItemViewType(position: Int): Int {
        return when (xmlName) {
            "E", "ED", "PCD" -> 0
            "PTD" -> 1
            "M" -> 2
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
        return exerciseList?.size ?: 0
    }

    @SuppressLint("ClickableViewAccessibility", "MissingInflatedId", "InflateParams", "SetTextI18n")
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val currentExerciseItem = exerciseList?.get(position)

        val second = "${currentExerciseItem?.duration?.toInt()?.div(60)}분 ${currentExerciseItem?.duration?.toInt()?.rem(60)}초"

        when (holder) {
            is MainViewHolder -> {
                if (xmlName in listOf("E", "ED", "PCD")) {
                    // -----! recyclerview에서 운동군 보여주기 !------
                    holder.tvEIFinish.visibility = View.INVISIBLE
                    holder.tvEISymptom.text = currentExerciseItem?.relatedSymptom.toString()
                    holder.tvEIName.text = currentExerciseItem?.exerciseName
                    holder.tvEITime.text = second
                    when (currentExerciseItem?.exerciseStage) {
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
                        .load("${currentExerciseItem?.imageFilePath}")
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .override(180)
                        .into(holder.ivEIThumbnail)

                    // ExerciseDetail탭 시청기록 (유튜브같은)
                    if (xmlName == "ED" && !historys.isNullOrEmpty()) {
                        holder.tvEIFinish.visibility = View.INVISIBLE
                        val historyUnit = historys.find { it.exerciseId == currentExerciseItem?.exerciseId?.toInt() }
                        historyUnit?.let {
                            holder.hpvEI.visibility = View.VISIBLE
                            Log.v("historyUnit", "$historyUnit")
                            holder.hpvEI.progress = (it.progress * 100 / it.duration).toFloat()
                        }
                    }

                    // ------# 시청 기록 #------\
//                if (currentExerciseItem.exerciseId == currentHistoryItem.exerciseId) {
//                    holder.hpvEIHistory.progresses = (currentHistoryItem.timestamp?.div(currentExerciseItem.videoDuration?.toInt())) * 100
//                }

                    // ------# 하트 버튼 #------
//                updateLikeButtonState(currentExerciseItem?.exerciseId.toString(), holder.ibtnEILike)
//                holder.ibtnEILike.setOnClickListener {
//                    val currentLikeState = prefs.existLike(currentExerciseItem?.exerciseId.toString())
//                    if (currentLikeState) {
//                        prefs.deleteLike(currentExerciseItem?.exerciseId.toString())
//                    } else {
//                        prefs.storeLike(currentExerciseItem?.exerciseId.toString())
//                    }
//                    // 상태 변경 후 즉시 UI 업데이트
//                    updateLikeButtonState(currentExerciseItem?.exerciseId.toString(), holder.ibtnEILike)
//                }

                    if (!progresses.isNullOrEmpty() && sequence != null ) {

                        /* Pair(pvm.currentSequence, pvm.selectedSequence.value))
                        * 회차를 기준으로 나눠진거임. progresses[position] == 16개의 운동이 담긴 1회차 선택한 회차를 가져옴.
                        * selectSeq.first == currentSeq
                        * selectSeq.second == selectedSeq
                        *
                        * 현재 progresses와 exercise가 크기가 같음.
                        * repeatcount가 1 이상이고 timestamp -1 이면 한 번 본적있는 거.
                        * */

                        // ------# 시청 기록 및 완료 버튼 #------
                        val currentItem = progresses[position] // 프로그램 갯수만큼의 progresses의 1개에 접근
                        // currentItem의 currentWeek와 currentSequence로 현재 운동의 회차를 계산

                        val currentSeq = sequence.first // 안변함
                        val selectedSeq = sequence.second // 선택된 회차이기 때문에 변함.
                        val currentUnitsSeq = currentItem.currentSequence // 0,0 으로 나왔다고 쳤을 떄,

                        val condition = when {
                            selectedSeq == currentSeq -> {
                                if (currentItem.lastProgress > 0) {
                                    1
                                } else if (currentUnitsSeq > currentSeq) {
                                    0
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
                        // Program 의 UI
                        holder.hpvEI.visibility = View.VISIBLE
                        when (condition) {
                            0 -> { // 재생 및 완료
                                holder.tvEIFinish.visibility = View.VISIBLE
//                            holder.ibtnEILike.visibility = View.INVISIBLE
//                            holder.ibtnEILike.isEnabled = false
                                holder.vEI.visibility = View.VISIBLE
                                holder.vEI.backgroundTintList = ContextCompat.getColorStateList(fragment.requireContext(), R.color.secondContainerColor)
                                holder.hpvEI.progress = 100f
                                holder.hpvEI.autoAnimate = false
                            }
                            1 -> { // 재생 시간 중간
                                holder.tvEIFinish.visibility = View.GONE
                                val duration  = currentExerciseItem?.duration
                                if (duration != null) {
                                    holder.hpvEI.progress = (currentItem.lastProgress * 100 ) / duration.toFloat()
                                }
                                Log.v("hpvprogresses", "${holder.hpvEI.progress}")
                                holder.hpvEI.autoAnimate = true
                            }
                            else -> { // 재생기록 없는 item
                                holder.tvEIFinish.visibility = View.GONE
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
                    if (MDHistory != null) {
                        // ------# 완료된 항목만 들어오니, 테이블에 contentId, user식별자, 만 있고, 그렇게 해도 될 것 같은데?
//                    val currentHistory = history[position]
                        holder.tvEIFinish.visibility = View.VISIBLE
//                    holder.ibtnEILike.visibility = View.INVISIBLE
//                    holder.ibtnEILike.isEnabled = false
                        holder.vEI.visibility = View.VISIBLE
                        holder.vEI.backgroundTintList = ContextCompat.getColorStateList(fragment.requireContext(), R.color.secondContainerColor)
                        holder.hpvEI.visibility = View.GONE
                        holder.vEI.isEnabled = false
                    }
                }
            }
            // ------! play thumbnail 추천 운동 시작 !------
            is RecommendViewHolder -> {
                holder.tvRcPName.text = currentExerciseItem?.exerciseName
                holder.tvRcPTime.text = second
                holder.tvRcPStage.text = currentExerciseItem?.exerciseStage

                Glide.with(holder.itemView.context)
                    .load("${currentExerciseItem?.imageFilePath}?width=200&height=200")
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
                holder.tvEHIName.text = currentExerciseItem?.exerciseName
                holder.tvEHITime.text = second
                when (currentExerciseItem?.exerciseStage) {
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
                // 가장 최근 완료한 운동의 index 가져오기
                Glide.with(fragment.requireContext())
                    .load("${currentExerciseItem?.imageFilePath}")
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .override(180)
                    .into(holder.ivEHIThumbnail)
                val duration = currentExerciseItem?.duration
//                Log.v("startIndex", "position: $position, startIndex: $startIndex")


                if (currentItem != null && duration != null && startIndex != null) {
                    // 재생 중간
                    if (position < startIndex) {
                        holder.hpvEHI.progress = 100f
                    } else {
                        holder.hpvEHI.progress = (currentItem.lastProgress * 100 ) / duration.toFloat()
                    }
                }

                holder.tvEHISeq.text = "${position+1}/${exerciseList?.size}"

                // Main History 버튼 클릭 > 바로 재생
                holder.clEHI.setOnSingleClickListener{
                    val videoUrls = mutableListOf<String>()
                    val exerciseIds = mutableListOf<String>()
                    val uvpIds = mutableSetOf<String>() // 중복 제거를 위해 Set 사용

                    if (progresses != null && startIndex != null) {
                        for (i in startIndex until progresses.size) {
                            val progress = progresses[i]
                            exerciseIds.add(progress.exerciseId.toString())
                            uvpIds.add(progress.uvpSn.toString())
                            videoUrls.add(exerciseList?.get(i)?.videoFilepath.toString())
                        }
                        val intent = Intent(fragment.requireContext(), PlayFullScreenActivity::class.java)
                        intent.putStringArrayListExtra("video_urls", ArrayList(videoUrls))
                        intent.putStringArrayListExtra("exercise_ids", ArrayList(exerciseIds))
                        intent.putStringArrayListExtra("uvp_sns", ArrayList(uvpIds))
                        intent.putExtra("current_position",progresses[startIndex].lastProgress.toLong())
                        fragment.requireContext().startActivity(intent)

                    }
                }
            }
        }
    }
    private fun View.setOnSingleClickListener(action: (v: View) -> Unit) {
        val listener = View.OnClickListener { action(it) }
        setOnClickListener(OnSingleClickListener(listener))
    }

    // 해당 회차에서 가장 최근 운동의 index찾아오기 ( 한 seq의 묶음만 들어옴 )
    private fun findCurrentIndex(progresses: MutableList<ProgressUnitVO>) : Int {
        // Case 1: 시청 중간 기록이 있을 경우
        val progressIndex1 = progresses.indexOfLast { it.lastProgress in 1 until it.videoDuration }
        if (progressIndex1 != -1) {
            return progressIndex1
        }

        // Case 2: sequence 기반으로 다음 볼 차례 찾기
        val maxSequence = progresses.maxOf { it.currentSequence }
        if (maxSequence > 0) {
            // 가장 큰 sequence보다 1 작은 sequence의 첫 인덱스를 찾음
            val nextIndex = progresses.indexOfFirst { it.currentSequence == maxSequence - 1 }
            return if (nextIndex != -1) nextIndex else 0
        }

        // Case 3: 초기상태
        return if (progresses.all { it.lastProgress == 0 }) {
            0
        } else {
            -1
        }
    }

//    private fun updateLikeButtonState(exerciseId: String, ibtn : ImageButton) {
//        val isLike = prefs.existLike(exerciseId)
//        ibtn.setImageDrawable(
//            ContextCompat.getDrawable(
//                fragment.requireContext(),
//                if (isLike) R.drawable.icon_like_enabled else R.drawable.icon_like_disabled
//            )
//        )
//    }
}