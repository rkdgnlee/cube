package com.tangoplus.tangoq.adapter

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.PopupWindow
import android.widget.TextView
import androidx.activity.result.ActivityResultLauncher
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.tangoplus.tangoq.PlayFullScreenActivity
import com.tangoplus.tangoq.R
import com.tangoplus.tangoq.data.ExerciseVO
import com.tangoplus.tangoq.data.ProgramVO
import com.tangoplus.tangoq.databinding.RvProgramItemBinding
import com.tangoplus.tangoq.listener.OnRVClickListener

class ProgramRVAdapter(var programs: MutableList<ProgramVO>, private val onRVClickListener: OnRVClickListener, val fragment : Fragment, val xmlname: String, private val startForResult: ActivityResultLauncher<Intent>)
    : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    var popupWindow : PopupWindow?= null

    inner class rankViewHolder(view: View): RecyclerView.ViewHolder(view) {
        val cvPIThumbnail: CardView = view.findViewById(R.id.cvPIThumbnail)
        val ivPIThumbnail1: ImageView = view.findViewById(R.id.ivPIThumbnail1)
//        val llRThumbnail = view.findViewById<ImageView>(R.id.llPIThumbnail)
        val ivPIThumbnail2: ImageView = view.findViewById(R.id.ivPIThumbnail2)
        val ivPIThumbnail3: ImageView = view.findViewById(R.id.ivPIThumbnail3)
        val tvPIRanking: TextView = view.findViewById(R.id.tvPIRanking)
        val tvPIName: TextView = view.findViewById(R.id.tvPIName)
        val ibtnPIMore: ImageButton = view.findViewById(R.id.ibtnPIMore)
        val tvPICount: TextView = view.findViewById(R.id.tvPICount)
        val tvPITime: TextView = view.findViewById(R.id.tvPITime)
        val vPI: View = view.findViewById(R.id.vPI)
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
        val binding = RvProgramItemBinding.inflate(inflater, parent, false)
        return rankViewHolder(binding.root)

    }
    override fun getItemCount(): Int {
        return programs.size
    }
    @SuppressLint("ClickableViewAccessibility")
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val currentItem = programs[position]

        when (holder) {
            is rankViewHolder -> {
                holder.tvPIName.text = currentItem.programName
                holder.tvPIRanking.text = "${position + 1}"
                holder.tvPITime.text = (if (currentItem.programTime <= 60) {
                    "${currentItem.programTime}초"
                } else {
                    "${currentItem.programTime / 60}분 ${currentItem.programTime % 60}초"
                }).toString()
                holder.tvPICount.text = "${currentItem.programCount} 개"

                // ------! more 버튼 시작 !------
                holder.ibtnPIMore.setOnClickListener { view ->
                    if (popupWindow?.isShowing == true) {
                        popupWindow?.dismiss()
                        popupWindow =  null
                    } else {
                        val inflater = LayoutInflater.from(view?.context)
//                        val popupView = inflater.inflate(R.layout.pw_main_item, null)
//                        val width = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 186f, view?.context?.resources?.displayMetrics).toInt()
//                        val height = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 162f, view?.context?.resources?.displayMetrics).toInt()
//
//                        popupWindow = PopupWindow(popupView, width, height)
//                        popupWindow!!.showAsDropDown(view)
//
//                         //------! 팝업 1 재생 시작 !------
//                        popupView.findViewById<TextView>(R.id.tvPMPlay).setOnClickListener {
//                            val urls = storePickUrl(currentItem.exercises!!)
//                            val intent = Intent(fragment.requireContext(), PlayFullScreenActivity::class.java)
//                            intent.putStringArrayListExtra("urls", ArrayList(urls))
//                            intent.putExtra("total_time", currentItem.programTime)
//                            fragment.requireContext().startActivity(intent)
//                            startForResult.launch(intent)
//                            popupWindow!!.dismiss()
//                        } // ------! 팝업 2 자세히 보기 시작 !------
//                        popupView.findViewById<TextView>(R.id.tvPMGoThumbnail).setOnClickListener {
////                            val dialogFragment = PlayProgramThumbnailDialogFragment().apply {
////                                arguments = Bundle().apply {
////                                    putParcelable("Program", currentItem)
////                                }
////                            }
////                            dialogFragment.show(fragment.requireActivity().supportFragmentManager, "PlayProgramThumbnailDialogFragment")
////                            popupWindow!!.dismiss()
//                        } // -------! 팝업 즐겨찾기 추가 !------
//                        popupView.findViewById<TextView>(R.id.tvPMAddFavorite).setOnClickListener {
//
//                        }
//                        popupView.findViewById<ImageButton>(R.id.ibtnPMExit).setOnClickListener {
//                            popupWindow!!.dismiss()
//                        }
//                        popupWindow!!.isOutsideTouchable = true
//                        popupWindow!!.isFocusable = true
                    }


                } // ------! more 버튼 끝 !------

                // -------! 이미지 썸네일 시작 !------
                Glide.with(holder.itemView.context)
                    .load(currentItem.imgThumbnails!![2])
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .override(500)
                    .into(holder.ivPIThumbnail1)
//                Glide.with(holder.itemView.context)
//                    .load(currentItem.imgThumbnails!![1])
//                    .diskCacheStrategy(DiskCacheStrategy.ALL)
//                    .into(holder.ivPIThumbnail2)
//                Glide.with(holder.itemView.context)
//                    .load(currentItem.imgThumbnails!![0])
//                    .diskCacheStrategy(DiskCacheStrategy.ALL)
//                    .into(holder.ivPIThumbnail3)

                // -------! 이미지 썸네일 끝 !------

                // -------! 클릭 재생 시작 !-------
                holder.vPI.setOnClickListener {
//                    val urls = storePickUrl(currentItem.exercises!!)
//                    val intent = Intent(fragment.requireContext(), PlayFullScreenActivity::class.java)
//                    intent.putStringArrayListExtra("urls", ArrayList(urls))
////                    fragment.requireContext().startActivity(intent)
//                    startForResult.launch(intent)
//                    val dialogFragment = PlayProgramThumbnailDialogFragment().apply {
//                        arguments = Bundle().apply {
//                            putParcelable("Program", currentItem)
//                        }
//                    }
//                    dialogFragment.show(fragment.requireActivity().supportFragmentManager, "PlayProgramThumbnailDialogFragment")

                }

//                val trackSelector = DefaultTrackSelector(fragment.requireContext())
//                val parametersBuilder = trackSelector.buildUponParameters()
//                parametersBuilder.setMaxVideoSize(640, 360) // 해상도 제한 설정
//                trackSelector.parameters = parametersBuilder.build()
//
//                player = ExoPlayer.Builder(fragment.requireContext())
//                    .setTrackSelector(trackSelector)
//                    .build()
//                holder.pvR.player = player
//                val mediaItem = MediaItem.fromUri(currentItem.programVideoUrl.toString())
//                player.setMediaItem(mediaItem)
//                player.prepare()
//                player.seekTo(20000)
//                player.play()
//                player.pause()
//
//                holder.pvR.setOnTouchListener { v, event ->
//                    when (event.action) {
//                        MotionEvent.ACTION_DOWN -> {
//                            player.play()
//                        }
//                        MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
//                            player.stop()
//                        }
//                    }
//                    true
//                }
            }
        }
    }

    private fun storePickUrl(currentItem : MutableList<ExerciseVO>) : MutableList<String> {
        val urls = mutableListOf<String>()
        for (i in currentItem.indices) {
            val exercise = currentItem[i]
            urls.add(exercise.videoFilepath.toString())
        }
        Log.v("urls", "${urls}")
        return urls
    }
    override fun onViewRecycled(holder: RecyclerView.ViewHolder) {
        super.onViewRecycled(holder)
//        player.release()
    }

    fun dismissPopupWindow() {
        popupWindow?.dismiss()
        popupWindow = null
    }
}