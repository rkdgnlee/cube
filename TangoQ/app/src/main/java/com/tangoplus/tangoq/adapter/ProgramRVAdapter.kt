package com.tangoplus.tangoq.adapter

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.MutableLiveData
import androidx.recyclerview.widget.RecyclerView
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.ui.PlayerView
import com.tangoplus.tangoq.dialog.RecommendBSDialogFragment
import com.tangoplus.tangoq.listener.OnRVClickListener
import com.tangoplus.tangoq.R
import com.tangoplus.tangoq.data.ProgramVO
import com.tangoplus.tangoq.databinding.RvProgramItemBinding
import com.tangoplus.tangoq.databinding.RvRankItemBinding
import java.lang.IllegalArgumentException

class ProgramRVAdapter(var programs: MutableList<ProgramVO>, private val onRVClickListener: OnRVClickListener, val fragment : Fragment, val xmlname: String)
    : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private lateinit var player: ExoPlayer
        inner class horizonViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val ivRcmThumbnail = view.findViewById<ImageView>(R.id.ivRcmThumbnail)
            val tvRcmExerciseName = view.findViewById<TextView>(R.id.tvRcmExerciseName)
            val tvRcmExerciseTime = view.findViewById<TextView>(R.id.tvRcmExerciseTime)
            val tvRcmExerciseStep = view.findViewById<TextView>(R.id.tvRcmExerciseStep)
            val tvRcmExerciseKcal = view.findViewById<TextView>(R.id.tvRcmExerciseKcal)
            val ibtnRcmMore = view.findViewById<ImageButton>(R.id.ibtnRcmMore)
            val vRcmPlay = view.findViewById<View>(R.id.vRcmPlay)
        }
    inner class rankViewHolder(view: View): RecyclerView.ViewHolder(view) {
        val tvRanking = view.findViewById<TextView>(R.id.tvRanking)
        val tvRName = view.findViewById<TextView>(R.id.tvRName)
        val tvRExplain = view.findViewById<TextView>(R.id.tvRExplain)
        val ibtnRMore = view.findViewById<ImageButton>(R.id.ibtnRMore)
        val tvRCount = view.findViewById<TextView>(R.id.tvRCount)
        val tvRTime = view.findViewById<TextView>(R.id.tvRTime)
        val pvR = view.findViewById<PlayerView>(R.id.pvR)
    }

    override fun getItemViewType(position: Int): Int {
        return when (xmlname) {
            "horizon" -> 0
            "rank" -> 1
            else -> throw IllegalArgumentException("invalied view type")
        }
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            0 -> {
                val binding = RvProgramItemBinding.inflate(inflater, parent, false)
                return horizonViewHolder(binding.root)
            }
            1 -> {
                val binding = RvRankItemBinding.inflate(inflater, parent, false)
                return rankViewHolder(binding.root)
            }
            else -> throw IllegalArgumentException("invalid view type binding")
        }

    }
    override fun getItemCount(): Int {
        return programs.size
    }
    @SuppressLint("ClickableViewAccessibility")
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val currentItem = programs[position]
        when (holder) {
            is horizonViewHolder -> {
                holder.tvRcmExerciseName.text = currentItem.programName
                holder.tvRcmExerciseStep.text = when (currentItem.programStage) {
                    "유지" -> "상급자"
                    "향상" -> "중급자"
                    else -> "초급자"
                }
                holder.tvRcmExerciseTime.text = currentItem.programTime
                holder.tvRcmExerciseKcal.text = currentItem.programCount
                holder.ibtnRcmMore.setOnClickListener {
                    val bsFragment = RecommendBSDialogFragment()
                    val bundle = Bundle()
//            val FavoriteVOfromProgramm = FavoriteItemVO(
//                favoriteSn = 102,
//                favoriteName = currentItem.programName.toString(),
//                favoriteExplain = currentItem.programCount.toString() + "개 ," + currentItem.programTime.toString() + "분 ",
//                favoriteDisclosure = "",
//                exercises = currentItem.exercises
//            )
                    bundle.putParcelable("Program", currentItem)

                    bsFragment.arguments = bundle
                    val fragmentManager = fragment.requireActivity().supportFragmentManager
                    bsFragment.show(fragmentManager, bsFragment.tag)
                }
                holder.vRcmPlay.setOnClickListener {
                    onRVClickListener.onRVClick(currentItem)
                }
            }
            is rankViewHolder -> {
                holder.tvRName.text = currentItem.programName
                holder.tvRanking.text = position.toString()
                holder.tvRTime.text = currentItem.programTime
                holder.tvRCount.text = currentItem.programCount
                holder.tvRExplain.text = currentItem.programDescription
                // ------! more 버튼 시작 !------
                holder.ibtnRMore.setOnClickListener {

                } // ------! more 버튼 끝 !------

                player = ExoPlayer.Builder(fragment.requireContext()).build()
                holder.pvR.player = player
                val mediaItem = MediaItem.fromUri(currentItem.programVideoUrl.toString())
                player.setMediaItem(mediaItem)
                player.prepare()
                player.seekTo(40000)
                player.play()
                player.pause()

                holder.pvR.setOnTouchListener { v, event ->
                    when (event.action) {
                        MotionEvent.ACTION_DOWN -> {
                            player.play()
                        }
                        MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                            player.stop()
                        }
                    }
                    true
                }
            }
        }
    }

    override fun onViewRecycled(holder: RecyclerView.ViewHolder) {
        super.onViewRecycled(holder)
        player.release()
    }
}