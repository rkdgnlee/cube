package com.tangoplus.tangoq.Adapter

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.tangoplus.tangoq.Callback.ItemTouchCallback
import com.tangoplus.tangoq.Dialog.FavoriteBSDialogFragment
import com.tangoplus.tangoq.Listener.BasketItemTouchListener
import com.tangoplus.tangoq.R
import com.tangoplus.tangoq.ViewModel.ExerciseVO
import com.tangoplus.tangoq.databinding.RvBasketItemBinding
import com.tangoplus.tangoq.databinding.RvEditItemBinding
import com.tangoplus.tangoq.databinding.RvMainItemBinding
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
    lateinit var addListener: OnStartDragListener
    interface OnStartDragListener {
        fun onStartDrag(viewHolder: RecyclerView.ViewHolder)
    }
    fun startDrag(listener: OnStartDragListener) {
        this.addListener = listener
    }
    // -----! main !-----
    inner class mainViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivHThumbnail = view.findViewById<ImageView>(R.id.ivMThumbnail)
        val tvMName = view.findViewById<TextView>(R.id.tvMName)
        val tvMSymptom = view.findViewById<TextView>(R.id.tvMSymptom)
        val tvMTime = view.findViewById<TextView>(R.id.tvMTime)
        val tvMIntensity = view.findViewById<TextView>(R.id.tvMIntensity)
        val tvMCount = view.findViewById<TextView>(R.id.tvMCount)
        val ibtnMMore = view.findViewById<ImageButton>(R.id.ibtnMMore)

        val clMItem = view.findViewById<ConstraintLayout>(R.id.clMItem)
    }

    // -----! favorite edit !-----
    inner class editViewHolder(view: View): RecyclerView.ViewHolder(view) {
        val tvEditName = view.findViewById<TextView>(R.id.tvEditName)
        val tvEditSymptom = view.findViewById<TextView>(R.id.tvEditSymptom)
        val tvEditTime = view.findViewById<TextView>(R.id.tvEditTime)
        val tvEditIntensity = view.findViewById<TextView>(R.id.tvEditIntensity)
        val tvEditCount = view.findViewById<TextView>(R.id.tvEditCount)
        val ivEditDrag = view.findViewById<ImageView>(R.id.ivEditDrag)
    }
    // -----! favorite basket !-----
    inner class basketViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvBkName = view.findViewById<TextView>(R.id.tvBkName)
        val tvBkSymptom = view.findViewById<TextView>(R.id.tvBkSymptom)
        val tvBkTime = view.findViewById<TextView>(R.id.tvBkTime)
        val tvBkIntensity = view.findViewById<TextView>(R.id.tvBkTime)
        val ibtnBkPlus = view.findViewById<ImageButton>(R.id.ibtnBkPlus)
        val ibtnBkMinus = view.findViewById<ImageButton>(R.id.ibtnBkMinus)
        val tvBkCount = view.findViewById<TextView>(R.id.tvBkCount)
    }

    override fun getItemViewType(position: Int): Int {
        return when (xmlname) {
            "main" -> 0
            "edit" -> 1
            "basket" -> 2
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
            else -> throw IllegalArgumentException("invalid view type binding")
        }
    }

    override fun getItemCount(): Int {
        return exerciseList.size
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val currentItem = exerciseList[position]
        val second = currentItem.videoTime?.toInt()?.div(60)
        when (holder) {
            is mainViewHolder -> {
                // -----! recyclerview에서 운동군 보여주기 !------
                holder.tvMSymptom.text = (if (currentItem.relatedSymptom.toString().length >= 25) {
                    currentItem.relatedSymptom.toString().substring(0, 22) + "..."
                } else { currentItem.relatedSymptom}).toString()
                holder.tvMName.text = currentItem.exerciseName
                holder.tvMTime.text = second.toString()
//                holder..text = currentItem.relatedJoint.toString() TODO JOINT 말고 관계된 걸 넣기
                //        Glide.with(holder.itemView.context)
                //            .load(currentItem.imgUrl)
                //            .diskCacheStrategy(DiskCacheStrategy.ALL)
                //            .into(holder.ivhomevertical)
                val params = holder.ivHThumbnail.layoutParams as ConstraintLayout.LayoutParams
                params.horizontalBias = 0f // 0f는 왼쪽, 0.5f는 가운데, 1f는 오른쪽
                params.verticalBias = 0.5f // 0f는 위쪽, 0.5f는 가운데, 1f는 아래쪽
                holder.ivHThumbnail.layoutParams = params

                val params2 = holder.tvMTime.layoutParams as ConstraintLayout.LayoutParams
                params2.verticalBias = 0.5f
                holder.tvMTime.layoutParams = params2

//                holder.clMItem.setOnClickListener {
//                    val dialog = PlayThumbnailDialogFragment()
//                    val activity = holder.itemView.context as MainActivity
//                    dialog.show(activity.supportFragmentManager, "LoginDialogFragment")
//                }
                holder.ibtnMMore.setOnClickListener {
                    val bsFragment = FavoriteBSDialogFragment()
                    val bundle = Bundle()
                    bundle.putParcelable("ExerciseUnit", currentItem)
                    bsFragment.arguments = bundle
                    val fragmentManager = fragment.requireActivity().supportFragmentManager
                    bsFragment.show(fragmentManager, bsFragment.tag)
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
                holder.tvBkTime.text = currentItem.videoTime

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