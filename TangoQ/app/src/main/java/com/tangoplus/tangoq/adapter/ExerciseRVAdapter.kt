package com.tangoplus.tangoq.adapter

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.PopupWindow
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.tangoplus.tangoq.PlayFullScreenActivity
import com.tangoplus.tangoq.callback.ItemTouchCallback
import com.tangoplus.tangoq.dialog.PlayThumbnailDialogFragment
import com.tangoplus.tangoq.listener.BasketItemTouchListener
import com.tangoplus.tangoq.R
import com.tangoplus.tangoq.data.ExerciseVO
import com.tangoplus.tangoq.data.HistoryVO
import com.tangoplus.tangoq.data.ProgramVO
import com.tangoplus.tangoq.databinding.RvBasketItemBinding
import com.tangoplus.tangoq.databinding.RvEditItemBinding
import com.tangoplus.tangoq.databinding.RvExerciseItemBinding
import com.tangoplus.tangoq.databinding.RvRecommendPTnItemBinding
import com.tangoplus.tangoq.dialog.ProgramAddFavoriteDialogFragment
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
    var basketListener: BasketItemTouchListener? = null
    lateinit var addListener: OnStartDragListener
    interface OnStartDragListener {
        fun onStartDrag(viewHolder: RecyclerView.ViewHolder)
    }
    fun startDrag(listener: OnStartDragListener) {
        this.addListener = listener
    }
    private var popupWindow : PopupWindow?= null

    // -----! main !-----
    inner class mainViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivEIThumbnail: ImageView = view.findViewById(R.id.ivEIThumbnail)
        val tvEIName : TextView = view.findViewById(R.id.tvEIName)
        val tvEISymptom : TextView= view.findViewById(R.id.tvEISymptom)
        val tvEITime : TextView= view.findViewById(R.id.tvEITime)
        val tvEIStage : TextView = view.findViewById(R.id.tvEIStage)
//        val tvMKcal = view.findViewById<TextView>(R.id.tvMKcal)
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
//        val tvBkTime = view.findViewById<TextView>(R.id.tvBkTime)
//        val tvBkIntensity = view.findViewById<TextView>(R.id.tvBkIntensity)
        val ibtnBkPlus : ImageButton = view.findViewById(R.id.ibtnBkPlus)
        val ibtnBkMinus : ImageButton = view.findViewById(R.id.ibtnBkMinus)
        val tvBkCount : TextView = view.findViewById(R.id.tvBkCount)
    }

    inner class recommendViewHolder(view:View) : RecyclerView.ViewHolder(view) {
        val tvRcPName : TextView = view.findViewById(R.id.tvRcPName)
        val tvRcPTime : TextView= view.findViewById(R.id.tvRcPTime)
        val tvRcPStage : TextView = view.findViewById(R.id.tvRcPStage)
        val tvRcPKcal : TextView = view.findViewById(R.id.tvRcPKcal)
        val ivRcPThumbnail : ImageView = view.findViewById(R.id.ivRcPThumbnail)
        val vRPTN : View = view.findViewById(R.id.vRPTN)
    }

    override fun getItemViewType(position: Int): Int {
        return when (xmlname) {
            "main" -> 0
            "edit" -> 1
            "basket" -> 2
            "recommend" -> 3
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
                holder.tvEIStage.text = currentExerciseItem.exerciseStage


                Glide.with(fragment.requireContext())
                    .load("${currentExerciseItem.imageFilePathReal}")
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .into(holder.ivEIThumbnail)

                // ------! 시청 기록 시작 !------\
//                if (currentExerciseItem.exerciseId == currentHistoryItem.exerciseId) {
//                    holder.hpvEIHistory.progress = (currentHistoryItem.timestamp?.div(currentExerciseItem.videoDuration?.toInt()!!))!! * 100
//                }

                // ------! 점점점 버튼 시작 !------
                holder.ibtnEIMore.setOnClickListener {view ->
                    if (popupWindow?.isShowing == true) {
                        popupWindow?.dismiss()
                        popupWindow =  null
                    } else {
                        val inflater = LayoutInflater.from(view?.context)
                        val popupView = inflater.inflate(R.layout.pw_main_item, null)
                        val width = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 186f, view?.context?.resources?.displayMetrics).toInt()
                        val height = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 162f, view?.context?.resources?.displayMetrics).toInt()

                        popupWindow = PopupWindow(popupView, width, height)
                        popupWindow!!.showAsDropDown(view)
                        popupView.findViewById<TextView>(R.id.tvPMPlay).setOnClickListener {
                            val intent = Intent(fragment.requireContext(), PlayFullScreenActivity::class.java)
                            intent.putExtra("exercise_id", currentExerciseItem.exerciseId)
                            intent.putExtra("video_url", currentExerciseItem.videoFilepath)
                            fragment.startActivityForResult(intent, 8080)
                        }
                        popupView.findViewById<TextView>(R.id.tvPMGoThumbnail).setOnClickListener {
                            val dialogFragment = PlayThumbnailDialogFragment().apply {
                                arguments = Bundle().apply {
                                    putParcelable("ExerciseUnit", currentExerciseItem)
                                }
                            }
                            dialogFragment.show(fragment.requireActivity().supportFragmentManager, "PlayThumbnailDialogFragment")
                        }
                        popupView.findViewById<TextView>(R.id.tvPMAddFavorite).setOnClickListener {
                            val exerciseUnit = mutableListOf<ExerciseVO>()
                            exerciseUnit.add(currentExerciseItem)
                            val program = ProgramVO(0, mutableListOf(), "", 0, "","","", exerciseUnit)
                            val bundle = Bundle().apply {
                                putParcelable("Program", program)
                            }
                            val dialog = ProgramAddFavoriteDialogFragment().apply {
                                arguments = bundle
                            }
                            dialog.show(fragment.requireActivity().supportFragmentManager, "ProgramAddFavoriteDialogFragment")
                            popupWindow!!.dismiss()
                        }
                        popupView.findViewById<ImageButton>(R.id.ibtnPMExit).setOnClickListener {
                            popupWindow!!.dismiss()
                        }
                        popupWindow!!.isOutsideTouchable = true
                        popupWindow!!.isFocusable = true
                    }
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
                    .load("${currentExerciseItem.imageFilePathReal}")
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
//            holder.btnPickAddDelete.setOnClickListener {
//                verticalList.removeAt(holder.position)
//                notifyItemRemoved(holder.position)
//            }

            }
            is basketViewHolder -> {
                holder.tvBkSymptom.text = currentExerciseItem.relatedSymptom.toString()
                holder.tvBkName.text = currentExerciseItem.exerciseName
//                holder.tvBkTime.text = currentExerciseItem.videoTime

                // ------! 썸네일 !------
                Glide.with(fragment.requireContext())
                    .load("${currentExerciseItem.imageFilePathReal}")
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .into(holder.ivBkThumbnail)
                holder.ibtnBkPlus.setOnClickListener {
                    currentExerciseItem.quantity += 1
                    basketListener?.onBasketItemQuantityChanged(currentExerciseItem.exerciseId.toString(), currentExerciseItem.quantity)
                    Log.w("basketTouch", "${basketListener?.onBasketItemQuantityChanged(currentExerciseItem.exerciseId.toString(), currentExerciseItem.quantity)}")
                    holder.tvBkCount.text = ( holder.tvBkCount.text.toString().toInt() + 1 ). toString()

                }
                holder.ibtnBkMinus.setOnClickListener {
                    if (currentExerciseItem.quantity > 0) {
                        currentExerciseItem.quantity -= 1
                        basketListener?.onBasketItemQuantityChanged(currentExerciseItem.exerciseId.toString(), currentExerciseItem.quantity)
                        Log.w("basketTouch", "${basketListener?.onBasketItemQuantityChanged(currentExerciseItem.exerciseId.toString(), currentExerciseItem.quantity)}")
                        holder.tvBkCount.text = currentExerciseItem.quantity.toString()
                    }
                }
                holder.tvBkCount.text = currentExerciseItem.quantity.toString()
            }
            // ------! play thumbnail 추천 운동 시작 !------
            is recommendViewHolder -> {
                holder.tvRcPName.text = (if (currentExerciseItem.exerciseName.toString().length >= 10) {
                    currentExerciseItem.exerciseName.toString().substring(0, 8)
                } else {
                    currentExerciseItem.exerciseName
                }).toString()
                holder.tvRcPTime.text = second
                holder.tvRcPStage.text = currentExerciseItem.exerciseStage
                holder.tvRcPKcal.text
                Glide.with(holder.itemView.context)
                    .load(currentExerciseItem.imageFilePathReal)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
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