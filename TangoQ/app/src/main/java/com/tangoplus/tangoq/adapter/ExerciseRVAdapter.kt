package com.tangoplus.tangoq.adapter

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.animation.LinearInterpolator
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.PopupWindow
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.tangoplus.tangoq.PlayFullScreenActivity
import com.tangoplus.tangoq.callback.ItemTouchCallback
import com.tangoplus.tangoq.dialog.PlayThumbnailDialogFragment
import com.tangoplus.tangoq.R
import com.tangoplus.tangoq.data.ExerciseVO
import com.tangoplus.tangoq.data.HistoryVO
import com.tangoplus.tangoq.data.ProgramVO
import com.tangoplus.tangoq.databinding.RvBasketItemBinding
import com.tangoplus.tangoq.databinding.RvEditItemBinding
import com.tangoplus.tangoq.databinding.RvExerciseItemBinding
import com.tangoplus.tangoq.databinding.RvRecommendPTnItemBinding
import com.tangoplus.tangoq.databinding.VpExerciseItemBinding
import com.tangoplus.tangoq.dialog.ExerciseBSDialogFragment
import com.tangoplus.tangoq.dialog.FavoriteBSDialogFragment
import com.tangoplus.tangoq.dialog.ProgramAddFavoriteDialogFragment
import com.tangoplus.tangoq.listener.OnExerciseAddClickListener
import com.tomlecollegue.progressbars.HorizontalProgressView
import java.lang.IllegalArgumentException
import java.util.Collections


class ExerciseRVAdapter (
    private val fragment: Fragment,
    var exerciseList: MutableList<ExerciseVO>,
    var viewingHistory : List<HistoryVO>,
    var xmlname: String
) : RecyclerView.Adapter<RecyclerView.ViewHolder>(),
        ItemTouchCallback.AddItemTouchListener
{


    // ------! exerciseDetail 장바구니 담기 시작 !------
    private var onExerciseAddClickListener: OnExerciseAddClickListener? = null
    fun setOnExerciseAddClickListener(listener: OnExerciseAddClickListener) {
        this.onExerciseAddClickListener = listener
    }

    // ------! exerciseDetail 장바구니 담기 끝 !------

    // ------! edit drag swipe listener 시작 !------
    lateinit var addListener: OnStartDragListener
    interface OnStartDragListener {
        fun onStartDrag(viewHolder: RecyclerView.ViewHolder)
    }
    fun startDrag(listener: OnStartDragListener) {
        this.addListener = listener
    }
    // ------! edit drag swipe listener 끝 !------

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
        val hpvEIHistory : HorizontalProgressView = view.findViewById(R.id.hpvEIHistory)
    }

    // -----! favorite edit !-----
    inner class editViewHolder(view: View): RecyclerView.ViewHolder(view) {
        val ivEditThumbnail : ImageView = view.findViewById(R.id.ivEditThumbnail)
        val tvEditName : TextView = view.findViewById(R.id.tvEditName)
        val tvEditSymptom : TextView = view.findViewById(R.id.tvEditSymptom)
        val tvEditTime : TextView= view.findViewById(R.id.tvEditTime)
        val tvEditIntensity : TextView = view.findViewById(R.id.tvEditIntensity)
//        val tvEditCount = view.findViewById<TextView>(R.id.tvEditCount)
        val ivEditDrag : ImageView= view.findViewById(R.id.ivEditDrag)
    }
    // -----! favorite basket !-----
    inner class basketViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivBkThumbnail : ImageView= view.findViewById(R.id.ivBkThumbnail)
        val tvBkName : TextView = view.findViewById(R.id.tvBkName)
        val tvBkSymptom : TextView = view.findViewById(R.id.tvBkSymptom)
        val tvBkStage : TextView = view.findViewById(R.id.tvBkStage)
        val cvBkState : CardView = view.findViewById(R.id.cvBkState)
        val ivBkStage : ImageView = view.findViewById(R.id.ivBkStage)
        val tvBkRepeat : TextView = view.findViewById(R.id.tvBkRepeat)
        val ibtnBIMore : ImageButton = view.findViewById(R.id.ibtnBIMore)
        val tvBITime : TextView = view.findViewById(R.id.tvBITime)
        val vBk : View = view.findViewById(R.id.vBk)
    }

    inner class recommendViewHolder(view:View) : RecyclerView.ViewHolder(view) {
        val tvRcPName : TextView = view.findViewById(R.id.tvRcPName)
        val tvRcPTime : TextView= view.findViewById(R.id.tvRcPTime)
        val tvRcPStage : TextView = view.findViewById(R.id.tvRcPStage)
        val tvRcPKcal : TextView = view.findViewById(R.id.tvRcPKcal)
        val ivRcPThumbnail : ImageView = view.findViewById(R.id.ivRcPThumbnail)
        val vRPTN : View = view.findViewById(R.id.vRPTN)
    }

    inner class dailyViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvVEIName : TextView = view.findViewById(R.id.tvVEIName)
        val tvVEITime : TextView = view.findViewById(R.id.tvVEITime)
        val ivVEIThumbnail : ImageView = view.findViewById(R.id.ivVEIThumbnail)
        val tvVEIStage : TextView = view.findViewById(R.id.tvVEIStage)
        val vVEI : View = view.findViewById(R.id.vVEI)
    }
    override fun getItemViewType(position: Int): Int {
        return when (xmlname) {
            "main" -> 0
            "edit" -> 1
            "basket" -> 2
            "recommend" -> 3
            "daily" -> 4
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
                val binding = RvEditItemBinding.inflate(inflater, parent, false)
                editViewHolder(binding.root)
            }
            2 -> {
                val binding = RvBasketItemBinding.inflate(inflater, parent, false)
                basketViewHolder(binding.root)
            }
            3 -> {
                val binding = RvRecommendPTnItemBinding.inflate(inflater, parent, false)
                recommendViewHolder(binding.root)
            }
            4 -> {
                val binding = VpExerciseItemBinding.inflate(inflater, parent, false)
                dailyViewHolder(binding.root)
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
            }

            is editViewHolder -> {
                holder.tvEditSymptom.text = currentExerciseItem.relatedSymptom.toString()
//                holder.tvPickAddJoint.text = currentExerciseItem.relatedJoint.toString()

                // ------! 썸네일 !------
                Glide.with(fragment.requireContext())
                    .load("${currentExerciseItem.imageFilePathReal}?width=180&height=180")
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .into(holder.ivEditThumbnail)

                holder.tvEditName.text = currentExerciseItem.exerciseName
                holder.tvEditTime.text = second
                holder.ivEditDrag.setOnTouchListener { _, event ->
                    if (event.action == MotionEvent.ACTION_DOWN) {
                        addListener.onStartDrag(holder)
                    }
                    return@setOnTouchListener false
                }
            }
            is basketViewHolder -> {
                // ------!정보 설정 시작 !------
                holder.tvBkSymptom.text = currentExerciseItem.relatedSymptom.toString()
                holder.tvBkName.text = currentExerciseItem.exerciseName
                holder.tvBITime.text = (if (currentExerciseItem.videoDuration?.toInt()!! <= 60) {
                    "${currentExerciseItem.videoDuration}초"
                } else {
                    "${currentExerciseItem.videoDuration!!.toInt() / 60}분 ${currentExerciseItem.videoDuration!!.toInt() % 60}초"
                }).toString()
                when (currentExerciseItem.exerciseStage) {
                    "초급" -> {
                        holder.ivBkStage.setImageDrawable(ContextCompat.getDrawable(fragment.requireContext(), R.drawable.icon_stage_1))
                        holder.tvBkStage.text = "초급자"
                    }
                    "중급" -> {
                        holder.ivBkStage.setImageDrawable(ContextCompat.getDrawable(fragment.requireContext(), R.drawable.icon_stage_2))
                        holder.tvBkStage.text = "중급자"
                    }
                    "고급" -> {
                        holder.ivBkStage.setImageDrawable(ContextCompat.getDrawable(fragment.requireContext(), R.drawable.icon_stage_3))
                        holder.tvBkStage.text = "상급자"
                    }
                }

                // ------!정보 설정 끝 !------

                // ------! 점점점 버튼 + 썸네일 시작 !------
                holder.ibtnBIMore.setOnClickListener {
                    val bsFragment = ExerciseBSDialogFragment()
                    val bundle = Bundle()
                    bundle.putParcelable("exerciseUnit", currentExerciseItem)

                    bsFragment.arguments = bundle
                    val fragmentManager = fragment.requireActivity().supportFragmentManager
                    bsFragment.show(fragmentManager, bsFragment.tag)
                }
                Glide.with(fragment.requireContext())
                    .load("${currentExerciseItem.imageFilePathReal}?width=180&height=180")
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .override(180)
                    .into(holder.ivBkThumbnail)
                // ------! 점점점 버튼 + 썸네일 끝 !------

                var clickable = currentExerciseItem.select
                if (clickable) {
                    changeBackgroundColor(holder.vBk,
                        ContextCompat.getColor(fragment.requireContext(), R.color.white),
                        ContextCompat.getColor(fragment.requireContext(), R.color.opacityColor))
                    holder.cvBkState.visibility = View.VISIBLE
                    clickable = true
                }

                holder.vBk.setOnClickListener{
                    if (clickable) {
                        changeBackgroundColor(holder.vBk,
                            ContextCompat.getColor(fragment.requireContext(), R.color.opacityColor),
                            ContextCompat.getColor(fragment.requireContext(), R.color.white))
                        holder.cvBkState.visibility = View.INVISIBLE
                        clickable = false
                    } else {
                        changeBackgroundColor(holder.vBk,
                            ContextCompat.getColor(fragment.requireContext(), R.color.white),
                            ContextCompat.getColor(fragment.requireContext(), R.color.opacityColor))
                        holder.cvBkState.visibility = View.VISIBLE
                        clickable = true
                    }
                    onExerciseAddClickListener?.onExerciseAddClick(currentExerciseItem, !clickable)
                }

//                holder.ibtnBkPlus.setOnClickListener {
//                    currentExerciseItem.quantity += 1
//                    basketListener?.onBasketItemQuantityChanged(currentExerciseItem.exerciseId.toString(), currentExerciseItem.quantity)
//                    Log.w("basketTouch", "${basketListener?.onBasketItemQuantityChanged(currentExerciseItem.exerciseId.toString(), currentExerciseItem.quantity)}")
//                    holder.tvBkCount.text = ( holder.tvBkCount.text.toString().toInt() + 1 ). toString()
//
//                }
//                holder.ibtnBkMinus.setOnClickListener {
//                    if (currentExerciseItem.quantity > 0) {
//                        currentExerciseItem.quantity -= 1
//                        basketListener?.onBasketItemQuantityChanged(currentExerciseItem.exerciseId.toString(), currentExerciseItem.quantity)
//                        Log.w("basketTouch", "${basketListener?.onBasketItemQuantityChanged(currentExerciseItem.exerciseId.toString(), currentExerciseItem.quantity)}")
//                        holder.tvBkCount.text = currentExerciseItem.quantity.toString()
//                    }
//                }
//                holder.tvBkCount.text = currentExerciseItem.quantity.toString()
            }
            // ------! play thumbnail 추천 운동 시작 !------
            is recommendViewHolder -> {
                holder.tvRcPName.text = currentExerciseItem.exerciseName
                holder.tvRcPTime.text = second
                holder.tvRcPStage.text = currentExerciseItem.exerciseStage
                holder.tvRcPKcal.text
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
            is dailyViewHolder -> {
                holder.tvVEIName.text = currentExerciseItem.exerciseName
                Glide.with(holder.itemView.context)
                    .load("${currentExerciseItem.imageFilePathReal}?width=180&height=180")
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .override(180)
                    .into(holder.ivVEIThumbnail)
                holder.tvVEIStage.text = currentExerciseItem.exerciseStage
                holder.tvVEITime.text = (if (currentExerciseItem.videoDuration?.toInt()!! <= 60) {
                    "${currentExerciseItem.videoDuration}초"
                } else {
                    "${currentExerciseItem.videoDuration!!.toInt() / 60}분 ${currentExerciseItem.videoDuration!!.toInt() % 60}초"
                }).toString()

                holder.vVEI.setOnClickListener {
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

    override fun onItemMoved(from: Int, to: Int) {
        Collections.swap(exerciseList, from, to)
        notifyItemMoved(from, to)
        Log.w("순서 변경", "리스트목록: $exerciseList")
    }

    override fun onItemSwiped(position: Int) {
        exerciseList.removeAt(position)
        notifyItemRemoved(position)
    }
}