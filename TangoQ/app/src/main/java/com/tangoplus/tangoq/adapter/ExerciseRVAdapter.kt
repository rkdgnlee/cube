package com.tangoplus.tangoq.adapter

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.PopupWindow
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.tangoplus.tangoq.callback.ItemTouchCallback
import com.tangoplus.tangoq.dialog.ExerciseBSDialogFragment
import com.tangoplus.tangoq.dialog.PlayThumbnailDialogFragment
import com.tangoplus.tangoq.listener.BasketItemTouchListener
import com.tangoplus.tangoq.R
import com.tangoplus.tangoq.data.ExerciseVO
import com.tangoplus.tangoq.databinding.RvBasketItemBinding
import com.tangoplus.tangoq.databinding.RvEditItemBinding
import com.tangoplus.tangoq.databinding.RvMainItemBinding
import com.tangoplus.tangoq.databinding.RvRecommendPTnItemBinding
import com.tangoplus.tangoq.listener.OnMoreClickListener
import java.lang.IllegalArgumentException
import java.util.Collections


@Suppress("UNREACHABLE_CODE")
class ExerciseRVAdapter (private val fragment: Fragment,
    var exerciseList: MutableList<ExerciseVO>,
    var xmlname: String
) : RecyclerView.Adapter<RecyclerView.ViewHolder>(),
        ItemTouchCallback.AddItemTouchListener
{
    var basketListener: BasketItemTouchListener? = null
    val onMoreClickListener : OnMoreClickListener? = null
    lateinit var addListener: OnStartDragListener
    interface OnStartDragListener {
        fun onStartDrag(viewHolder: RecyclerView.ViewHolder)
    }
    fun startDrag(listener: OnStartDragListener) {
        this.addListener = listener
    }
    var popupWindow : PopupWindow?= null

    // -----! main !-----
    inner class mainViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivHThumbnail = view.findViewById<ImageView>(R.id.ivMThumbnail)
        val tvMName = view.findViewById<TextView>(R.id.tvMName)
        val tvMSymptom = view.findViewById<TextView>(R.id.tvMSymptom)
        val tvMTime = view.findViewById<TextView>(R.id.tvMTime)
        val tvMStage = view.findViewById<TextView>(R.id.tvMStage)
        val tvMKcal = view.findViewById<TextView>(R.id.tvMKcal)
        val ibtnMMore = view.findViewById<ImageButton>(R.id.ibtnMMore)
        val vM = view.findViewById<View>(R.id.vM)

    }

    // -----! favorite edit !-----
    inner class editViewHolder(view: View): RecyclerView.ViewHolder(view) {
        val tvEditName = view.findViewById<TextView>(R.id.tvEditName)
        val tvEditSymptom = view.findViewById<TextView>(R.id.tvEditSymptom)
        val tvEditTime = view.findViewById<TextView>(R.id.tvEditTime)
        val tvEditIntensity = view.findViewById<TextView>(R.id.tvEditIntensity)
//        val tvEditCount = view.findViewById<TextView>(R.id.tvEditCount)
        val ivEditDrag = view.findViewById<ImageView>(R.id.ivEditDrag)
    }
    // -----! favorite basket !-----
    inner class basketViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvBkName = view.findViewById<TextView>(R.id.tvBkName)
        val tvBkSymptom = view.findViewById<TextView>(R.id.tvBkSymptom)
//        val tvBkTime = view.findViewById<TextView>(R.id.tvBkTime)
//        val tvBkIntensity = view.findViewById<TextView>(R.id.tvBkIntensity)
        val ibtnBkPlus = view.findViewById<ImageButton>(R.id.ibtnBkPlus)
        val ibtnBkMinus = view.findViewById<ImageButton>(R.id.ibtnBkMinus)
        val tvBkCount = view.findViewById<TextView>(R.id.tvBkCount)
    }

    inner class recommendViewHolder(view:View) : RecyclerView.ViewHolder(view) {
        val tvRcPName = view.findViewById<TextView>(R.id.tvRcPName)
        val tvRcPTime = view.findViewById<TextView>(R.id.tvRcPTime)
        val tvRcPStage = view.findViewById<TextView>(R.id.tvRcPStage)
        val tvRcPKcal = view.findViewById<TextView>(R.id.tvRcPKcal)
        val ivRcPThumbnail = view.findViewById<ImageView>(R.id.ivRcPThumbnail)
        val vRPTN = view.findViewById<View>(R.id.vRPTN)
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
                val binding = RvMainItemBinding.inflate(inflater, parent, false)
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

    @SuppressLint("ClickableViewAccessibility", "MissingInflatedId", "InflateParams")
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val currentItem = exerciseList[position]
        val second = currentItem.videoTime?.toInt()?.div(60)
        when (holder) {
            is mainViewHolder -> {
                // -----! recyclerview에서 운동군 보여주기 !------
                holder.tvMSymptom.text = (if (currentItem.relatedSymptom.toString().length >= 20) {
                    currentItem.relatedSymptom.toString().substring(0, 17) + "..."
                } else { currentItem.relatedSymptom}).toString()
                holder.tvMName.text = currentItem.exerciseName
                holder.tvMTime.text = second.toString()
                holder.tvMStage.text = when (currentItem.exerciseStage) {
                    "향상" -> "상급자"
                    "유지" -> "중급자"
                    else -> "초급자"
                }
//                val params = holder.ivHThumbnail.layoutParams as ConstraintLayout.LayoutParams
//                params.horizontalBias = 0f // 0f는 왼쪽, 0.5f는 가운데, 1f는 오른쪽
//                params.verticalBias = 0.5f // 0f는 위쪽, 0.5f는 가운데, 1f는 아래쪽
//                holder.ivHThumbnail.layoutParams = params
//
//                val params2 = holder.tvMTime.layoutParams as ConstraintLayout.LayoutParams
//                params2.verticalBias = 0.5f
//                holder.tvMTime.layoutParams = params2

//                holder.clMItem.setOnClickListener {
//                    val dialog = PlayThumbnailDialogFragment()
//                    val activity = holder.itemView.context as MainActivity
//                    dialog.show(activity.supportFragmentManager, "LoginDialogFragment")
//                }
                // ------! 점점점 버튼 시작 !------

                holder.ibtnMMore.setOnClickListener {view ->
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
                        popupView.findViewById<TextView>(R.id.tvPWPlay).setOnClickListener {

                        }
                        popupView.findViewById<TextView>(R.id.tvPWGoThumbnail).setOnClickListener {
                            val dialogFragment = PlayThumbnailDialogFragment().apply {
                                arguments = Bundle().apply {
                                    putParcelable("ExerciseUnit", currentItem)
                                }
                            }
                            dialogFragment.show(fragment.requireActivity().supportFragmentManager, "PlayThumbnailDialogFragment")
                        }
                        popupView.findViewById<TextView>(R.id.tvPWAddFavorite).setOnClickListener {

                        }
                        popupView.findViewById<ImageButton>(R.id.ibtnPWExit).setOnClickListener {
                            popupWindow!!.dismiss()
                        }
                        popupWindow!!.isOutsideTouchable = true
                        popupWindow!!.isFocusable = true
                    }



                }
                // ------ ! thumbnail 시작 !------
                holder.vM.setOnClickListener {
                    val DialogFragment = PlayThumbnailDialogFragment().apply {
                        arguments = Bundle().apply {
                            putParcelable("ExerciseUnit", currentItem)
                        }
                    }
                    DialogFragment.show(fragment.requireActivity().supportFragmentManager, "PlayThumbnailDialogFragment")
                }
            }

            is editViewHolder -> {
                holder.tvEditSymptom.text = (if (currentItem.relatedSymptom.toString().length >= 25) {
                    currentItem.relatedSymptom.toString().substring(0, 25) + "..."
                } else { currentItem.relatedSymptom}).toString()
//                holder.tvPickAddJoint.text = currentItem.relatedJoint.toString()
                holder.tvEditName.text = currentItem.exerciseName
                holder.tvEditTime.text = second.toString()
                holder.ivEditDrag.setOnTouchListener { view, event ->
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
                holder.tvBkSymptom.text = (if (currentItem.relatedSymptom.toString().length >= 25) {
                    currentItem.relatedSymptom.toString().substring(0, 25) + "..."
                } else { currentItem.relatedSymptom}).toString()
                holder.tvBkName.text = currentItem.exerciseName
//                holder.tvBkTime.text = currentItem.videoTime

                holder.ibtnBkPlus.setOnClickListener {
                    currentItem.quantity += 1
                    basketListener?.onBasketItemQuantityChanged(currentItem.exerciseDescriptionId.toString(), currentItem.quantity)
                    Log.w("basketTouch", "${basketListener?.onBasketItemQuantityChanged(currentItem.exerciseDescriptionId.toString(), currentItem.quantity)}")
                    holder.tvBkCount.text = ( holder.tvBkCount.text.toString().toInt() + 1 ). toString()

                }
                holder.ibtnBkMinus.setOnClickListener {
                    if (currentItem.quantity > 0) {
                        currentItem.quantity -= 1
                        basketListener?.onBasketItemQuantityChanged(currentItem.exerciseDescriptionId.toString(), currentItem.quantity)
                        Log.w("basketTouch", "${basketListener?.onBasketItemQuantityChanged(currentItem.exerciseDescriptionId.toString(), currentItem.quantity)}")
                        holder.tvBkCount.text = currentItem.quantity.toString()
                    }
                }
                holder.tvBkCount.text = currentItem.quantity.toString()
            }
            // ------! play thumbnail 추천 운동 시작 !------
            is recommendViewHolder -> {
                holder.tvRcPName.text = (if (currentItem.exerciseName.toString().length >= 10) {
                    currentItem.exerciseName.toString().substring(0, 8)
                } else {
                    currentItem.exerciseName
                }).toString()
                holder.tvRcPTime.text = second.toString()
                holder.tvRcPStage.text = when (currentItem.exerciseStage) {
                    "향상" -> "상급자"
                    "유지" -> "중급자"
                    else -> "초급자"
                }
                holder.tvRcPKcal.text
                holder.vRPTN.setOnClickListener {
                    val DialogFragment = PlayThumbnailDialogFragment().apply {
                        arguments = Bundle().apply {
                            putParcelable("ExerciseUnit", currentItem)
                        }
                    }
                    DialogFragment.show(fragment.requireActivity().supportFragmentManager, "PlayThumbnailDialogFragment")
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