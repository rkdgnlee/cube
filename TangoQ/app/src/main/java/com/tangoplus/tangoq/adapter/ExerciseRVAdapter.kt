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
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.skydoves.progressview.ProgressView
import com.tangoplus.tangoq.dialog.PlayThumbnailDialogFragment
import com.tangoplus.tangoq.R
import com.tangoplus.tangoq.vo.ExerciseVO
import com.tangoplus.tangoq.vo.ProgressUnitVO
import com.tangoplus.tangoq.databinding.RvExerciseItemBinding
import com.tangoplus.tangoq.databinding.RvRecommendPTnItemBinding
import com.tangoplus.tangoq.fragment.ExtendedFunctions.setOnSingleClickListener
import com.tangoplus.tangoq.function.PreferencesManager
import com.tangoplus.tangoq.listener.OnDialogClosedListener
import com.tangoplus.tangoq.listener.OnExerciseClickListener
import com.tangoplus.tangoq.listener.OnSingleClickListener
import com.tangoplus.tangoq.viewmodel.ProgressViewModel
import com.tangoplus.tangoq.vo.ExerciseHistoryVO
import java.lang.IllegalArgumentException


class ExerciseRVAdapter (
    private val fragment: Fragment,
    var exerciseList: MutableList<ExerciseVO>?,
    private val progresses : MutableList<ProgressUnitVO>?,
    private val historys: MutableList<ExerciseHistoryVO>?,
    private val sequence : Pair<Int, Int?>?,
    private var xmlName: String,
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    var dialogClosedListener: OnDialogClosedListener? = null
    var exerciseClickListener : OnExerciseClickListener? = null
    inner class MainViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val clEI : ConstraintLayout = view.findViewById(R.id.clEI)
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
    override fun getItemViewType(position: Int): Int {
        return when (xmlName) {
            "E", "ED", "ESD", "PCD" -> 0
            "PTD" -> 1
            else -> throw IllegalArgumentException("invalid view type")
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
                if (xmlName in listOf("E", "ESD",  "ED", "PCD")) {


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
                    if (xmlName in listOf("ED", "ESD") && !historys.isNullOrEmpty()) {
                        holder.tvEIFinish.visibility = View.INVISIBLE
                        val historyUnit = historys.find { it.exerciseId == currentExerciseItem?.exerciseId?.toInt() }
                        historyUnit?.let {
                            holder.hpvEI.visibility = View.VISIBLE
                            holder.hpvEI.progress = (it.progress * 100 / it.duration).toFloat()
                        }
                    }

                    // ------# 시청 기록 #------\
//                if (currentExerciseItem.exerciseId == currentHistoryItem.exerciseId) {
//                    holder.hpvEIHistory.progresses = (currentHistoryItem.timestamp?.div(currentExerciseItem.videoDuration?.toInt())) * 100
//                }

                    // ------# 하트 버튼 #------
//                updateLikeButtonState(currentExerciseItem?.exerciseId.toString(), holder.ibtnEILike)
//                holder.ibtnEILike.setOnSingleClickListener {
//                    val currentLikeState = prefs.existLike(currentExerciseItem?.exerciseId.toString())
//                    if (currentLikeState) {
//                        prefs.deleteLike(currentExerciseItem?.exerciseId.toString())
//                    } else {
//                        prefs.storeLike(currentExerciseItem?.exerciseId.toString())
//                    }
//                    // 상태 변경 후 즉시 UI 업데이트
//                    updateLikeButtonState(currentExerciseItem?.exerciseId.toString(), holder.ibtnEILike)
//                }

                    // progresses를 보는 곳
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
                        Log.v("currentItem", "$currentItem")
                        val condition = when (currentItem.cycleProgress * 100 / currentItem.duration) {
                            in 95 .. 1000 -> 0
                            in 1 .. 94 -> 1
                            else -> 2
                        }
//                        Log.v("condition", "${currentItem.uvpSn}, ${currentItem.exerciseId}, ${currentExerciseItem?.exerciseId} ${currentItem.cycleProgress}, ${currentItem.duration}")
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
                                    holder.hpvEI.progress = (currentItem.cycleProgress * 100 ) / duration.toFloat()
                                }
                                holder.hpvEI.autoAnimate = true
                            }
                            else -> { // 재생기록 없는 item
                                holder.tvEIFinish.visibility = View.GONE
                            }
                        }
                    }
                    itemState.observe(fragment.viewLifecycleOwner) { state ->
                        holder.vEI.setOnSingleClickListener {
                            when (state) {
                                0, 1 -> {
                                    exerciseClickListener?.exerciseClick(currentExerciseItem?.exerciseName.toString())
                                    val currentItem = progresses?.get(position)
                                    val dialogFragment = PlayThumbnailDialogFragment().apply {
                                        arguments = Bundle().apply {
                                            putParcelable("ExerciseUnit", currentExerciseItem)
                                            if (progresses != null && state == 0) {
                                                Log.v("state", "state확인: $state")
                                                // 지난 값일 경우
                                                putBoolean("isProgram", true)
                                                putInt("uvpSn", currentItem?.uvpSn ?: 0)

                                            }
                                        }
                                    }
                                    dialogFragment.show(fragment.requireActivity().supportFragmentManager, "PlayThumbnailDialogFragment")
                                }
                                2 -> {
                                    // 터치 동작 없음
                                }
                            }
                        }
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
                holder.vRPTN.setOnSingleClickListener {
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
    // itemState : 0 -> 터치 자유로움  // 1 -> 터치는 되는데 UVP는 안담김 // 2 -> 터치가 전혀 안됨
    private var itemState = MutableLiveData(0)
    fun setTouchLocked(state: Int) {
        itemState.value = state
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