package com.example.mhg.Adapter

import android.annotation.SuppressLint
import android.content.Intent
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import com.example.mhg.BasketItemTouchListener
import com.example.mhg.ItemTouchCallback
import com.example.mhg.PlayActivity
import com.example.mhg.R
import com.example.mhg.VO.ExerciseVO
import com.example.mhg.databinding.RvAddListBinding
import com.example.mhg.databinding.RvBasketListBinding
import com.example.mhg.databinding.RvHomeListBinding
import com.example.mhg.databinding.RvTypeListBinding
import org.w3c.dom.Text
import java.lang.IllegalArgumentException
import java.util.Collections


class HomeVerticalRecyclerViewAdapter(
    var verticalList : MutableList<ExerciseVO>,
    var xmlname: String
) : RecyclerView.Adapter<RecyclerView.ViewHolder>(),
    ItemTouchCallback.AddItemTouchListener
{
    var basketListener: BasketItemTouchListener? = null
    lateinit var addListener: OnStartDragListener

    inner class homeViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivhomevertical = view.findViewById<ImageView>(R.id.ivHomeVerticalImage)
        val tvhomeverticalname = view.findViewById<TextView>(R.id.tvHomeVerticalName)
        val tvhomeverticalDuration = view.findViewById<TextView>(R.id.tvHomeVerticalDuration)
        val btnPlayVertical = view.findViewById<Button>(R.id.btnHomeVerticalPlay)
        val tvHomeVerticalSymptom = view.findViewById<TextView>(R.id.tvHomeVerticalSymptom)
        val tvHomeVerticalJoint = view.findViewById<TextView>(R.id.tvHomeVerticalJoint)
        val ivHomeVerticalWarmup = view.findViewById<ImageView>(R.id.ivHomeVerticalWarmup)
        val ivHomeVerticalStrength = view.findViewById<ImageView>(R.id.ivHomeVerticalStrength)
        val ivHomeVerticalMaintain = view.findViewById<ImageView>(R.id.ivHomeVerticalMaintain)
    }

    inner class addViewHolder(view: View): RecyclerView.ViewHolder(view) {
        val ivPickAdd = view.findViewById<ImageView>(R.id.ivPickAdd)
        val tvPickAddName = view.findViewById<TextView>(R.id.tvPickAddName)
        val ivPickAddDrag = view.findViewById<ImageView>(R.id.ivPickAddDrag)
        val tvPickAddDuration = view.findViewById<TextView>(R.id.tvPickAddDuration)
        val btnPickAddDelete = view.findViewById<Button>(R.id.btnPickAddDelete)
        val tvPickAddSymptom = view.findViewById<TextView>(R.id.tvPickAddSymptom)
        val tvPickAddJoint = view.findViewById<TextView>(R.id.tvPickAddJoint)
        val ivPickAddWarmup = view.findViewById<ImageView>(R.id.ivPickAddWarmup)
        val ivPickAddStrength = view.findViewById<ImageView>(R.id.ivPickAddStrength)
        val ivPickAddMaintain = view.findViewById<ImageView>(R.id.ivPickAddMaintain)

    }

    inner class basketViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivPickBasket = view.findViewById<ImageView>(R.id.ivPickBasket)
        val tvPickBasketName = view.findViewById<TextView>(R.id.tvPickBasketName)
        val tvBasketCount = view.findViewById<TextView>(R.id.tvBasketCount)
        val tvPickBasketDuration = view.findViewById<TextView>(R.id.tvPickBasketDuration)
        val ibtnBasketPlus = view.findViewById<ImageButton>(R.id.ibtnBasketPlus)
        val ibtnBasketMinus = view.findViewById<ImageButton>(R.id.ibtnBasketMinus)
        val tvPickBasketSymptom = view.findViewById<TextView>(R.id.tvPickBasketSymptom)
        val tvPickBasketJoint = view.findViewById<TextView>(R.id.tvPickBasketJoint)
        val ivPickBasketWarmup = view.findViewById<ImageView>(R.id.ivPickBasketWarmup)
        val ivPickBasketStrength = view.findViewById<ImageView>(R.id.ivPickBasketStrength)
        val ivPickBasketMaintain = view.findViewById<ImageView>(R.id.ivPickBasketMaintain)
    }

    override fun getItemViewType(position: Int): Int {
        return  when (xmlname) {
            "home" -> 0
            "add" -> 1
            "basket" -> 2
            else -> throw IllegalArgumentException("invalid view Type")
        }
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            0 -> {
                val binding = RvHomeListBinding.inflate(inflater, parent, false)
                homeViewHolder(binding.root)
            }
            1 -> {
                val binding = RvAddListBinding.inflate(inflater, parent, false)
                addViewHolder(binding.root)
            }
            2 -> {
                val binding = RvBasketListBinding.inflate(inflater, parent, false)
                basketViewHolder(binding.root)
            }
            else -> throw IllegalArgumentException("Invalid view type")
        }
    }

    override fun getItemCount(): Int {
        return verticalList.size
    }

    @SuppressLint("ClickableViewAccessibility", "SetTextI18n")
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int)  {
        val currentItem = verticalList[position]
        // -----! homeBeginner의 수직 RV 시작 !-----
        when (holder) {
            is homeViewHolder -> {
                // -----! recyclerview에서 운동군 보여주기 !------
                holder.tvHomeVerticalSymptom.text = (if (currentItem.relatedSymptom.toString().length >= 25) {
                    currentItem.relatedSymptom.toString().substring(0, 22) + "..."
                } else { currentItem.relatedSymptom}).toString()
                holder.tvHomeVerticalJoint.text = currentItem.relatedJoint.toString()
                when (currentItem.exerciseStage) {
                    "초기" -> {
                        holder.ivHomeVerticalStrength.visibility = View.GONE
                        holder.ivHomeVerticalMaintain.visibility = View.GONE
                    }
                    "향상" -> {
                        holder.ivHomeVerticalWarmup.visibility = View.GONE
                        holder.ivHomeVerticalMaintain.visibility = View.GONE
                    }
                    "유지" -> {
                        holder.ivHomeVerticalWarmup.visibility = View.GONE
                        holder.ivHomeVerticalStrength.visibility = View.GONE
                    }
                }
                holder.tvhomeverticalDuration.text = currentItem.videoTime
                holder.tvhomeverticalname.text = currentItem.exerciseName
    //        Glide.with(holder.itemView.context)
    //            .load(currentItem.imgUrl)
    //            .diskCacheStrategy(DiskCacheStrategy.ALL)
    //            .into(holder.ivhomevertical)

                holder.ivhomevertical.setOnClickListener {
                    val intent = Intent(it.context, PlayActivity::class.java)
                    intent.putExtra("exercise", currentItem)
                    it.context.startActivity(intent)
                }
                holder.btnPlayVertical.setOnClickListener {
                    val intent = Intent(it.context, PlayActivity::class.java)
                    intent.putExtra("exercise", currentItem)
                    it.context.startActivity(intent)
                }

                val params = holder.ivhomevertical.layoutParams as ConstraintLayout.LayoutParams

                params.horizontalBias = 0f // 0f는 왼쪽, 0.5f는 가운데, 1f는 오른쪽
                params.verticalBias = 0.5f // 0f는 위쪽, 0.5f는 가운데, 1f는 아래쪽
                holder.ivhomevertical.layoutParams = params

                val params2 = holder.tvhomeverticalDuration.layoutParams as ConstraintLayout.LayoutParams
                params2.verticalBias = 0.5f
                holder.tvhomeverticalDuration.layoutParams = params2
                // -----! homeBeginner의 수직 RV 끝 !-----
            }

            is addViewHolder -> {
                holder.tvPickAddSymptom.text = (if (currentItem.relatedSymptom.toString().length >= 25) {
                    currentItem.relatedSymptom.toString().substring(0, 25) + "..."
                } else { currentItem.relatedSymptom}).toString()
                holder.tvPickAddJoint.text = currentItem.relatedJoint.toString()
                when (currentItem.exerciseStage) {
                    "초기" -> {
                        holder.ivPickAddStrength.visibility = View.GONE
                        holder.ivPickAddMaintain.visibility = View.GONE
                    }
                    "향상" -> {
                        holder.ivPickAddWarmup.visibility = View.GONE
                        holder.ivPickAddMaintain.visibility = View.GONE
                    }
                    "유지" -> {
                        holder.ivPickAddWarmup.visibility = View.GONE
                        holder.ivPickAddStrength.visibility = View.GONE
                    }
                }
                holder.tvPickAddName.text = currentItem.exerciseName
                holder.tvPickAddDuration.text = currentItem.videoTime
                holder.ivPickAddDrag.setOnTouchListener { view, event ->
                    if (event.action == MotionEvent.ACTION_DOWN) {
                        addListener.onStartDrag(holder)
                    }
                    return@setOnTouchListener false

                }

                holder.btnPickAddDelete.setOnClickListener {
                    verticalList.removeAt(holder.position)
                    notifyItemRemoved(holder.position)
                }


                // -----! PiclAdd 수직 RV 끝 !-----

                // -----! pickbasket 수직 rv 시작 !-----
            }

            is basketViewHolder -> {
                holder.tvPickBasketSymptom.text = (if (currentItem.relatedSymptom.toString().length >= 25) {
                    currentItem.relatedSymptom.toString().substring(0, 25) + "..."
                } else { currentItem.relatedSymptom}).toString()
                holder.tvPickBasketJoint.text = currentItem.relatedJoint.toString()
                when (currentItem.exerciseStage) {
                    "초기" -> {
                        holder.ivPickBasketStrength.visibility = View.GONE
                        holder.ivPickBasketMaintain.visibility = View.GONE
                    }
                    "향상" -> {
                        holder.ivPickBasketWarmup.visibility = View.GONE
                        holder.ivPickBasketMaintain.visibility = View.GONE
                    }
                    "유지" -> {
                        holder.ivPickBasketWarmup.visibility = View.GONE
                        holder.ivPickBasketStrength.visibility = View.GONE
                    }
                }
                holder.tvPickBasketName.text = currentItem.exerciseName
                holder.tvPickBasketDuration.text = currentItem.videoTime

                holder.ibtnBasketPlus.setOnClickListener {
                    currentItem.quantity += 1
                    basketListener?.onBasketItemQuantityChanged(currentItem.exerciseDescriptionId.toString(), currentItem.quantity)
                    Log.w("basketTouch", "${basketListener?.onBasketItemQuantityChanged(currentItem.exerciseDescriptionId.toString(), currentItem.quantity)}")
                    holder.tvBasketCount.text = ( holder.tvBasketCount.text.toString().toInt() + 1 ). toString()

                }
                holder.ibtnBasketMinus.setOnClickListener {
                    if (currentItem.quantity > 0) {
                        currentItem.quantity -= 1
                        basketListener?.onBasketItemQuantityChanged(currentItem.exerciseDescriptionId.toString(), currentItem.quantity)
                        Log.w("basketTouch", "${basketListener?.onBasketItemQuantityChanged(currentItem.exerciseDescriptionId.toString(), currentItem.quantity)}")
                        holder.tvBasketCount.text = currentItem.quantity.toString()
                    }
                }
                holder.tvBasketCount.text = currentItem.quantity.toString()



            }
        } // -----! pickbasket 수직 rv 끝 !-----

    }
//    fun getSelectedItems(): MutableList<ExerciseVO> {
//        return verticalList.filter {
//            it.quantity >= 1
//        }.toMutableList()
//    }
////    fun getCheckedItems(): JSONObject {
//        val checkedData = JSONObject()
//        for (i in 0 until checkedItems.size()) {
//            if (checkedItems.valueAt(i)) {
//                val checkedItem = verticalList[i]
//                val itemData = JSONObject()
//                itemData.put("exercise_name","${checkedItem.exerciseName}")
//                itemData.put("exercise_description","${checkedItem.exerciseDescription}")
//                checkedData.put(i.toString(), itemData)
//            }
//        }
//        return checkedData
//    }

    // -----! 탭 감지 !-----
    interface OnStartDragListener {
        fun onStartDrag(viewHolder: RecyclerView.ViewHolder)
    }
    // -----! 순서 변경 시작 !-----
    fun startDrag(listener: OnStartDragListener) {
        this.addListener = listener
    }
    override fun onItemMoved(from: Int, to: Int) {
        Collections.swap(verticalList, from, to)
        notifyItemMoved(from, to)
        Log.w("순서 변경", "리스트목록: $verticalList")
    }
    // -----! 순서 끝 !-----
    override fun onItemSwiped(position: Int) {
        verticalList.removeAt(position)
        notifyItemRemoved(position)
    }



//    override fun onItemMove(from: Int, to: Int) {
//        val item: HomeRVBeginnerDataClass = verticalList[from]
//        verticalList.removeAt(from)
//        verticalList.add(to, item)
//        notifyItemMoved(from, to)
//    }
//    private fun showBottomSheetDialog(context: FragmentActivity, warmup: HomeRVBeginnerDataClass) {
//        val bottomsheetfragment = PlayBottomSheetDialogFragment()
//        val fragmentManager = context.supportFragmentManager
//
//        val bundle = Bundle()
//        bundle.putParcelable("warmupList", warmup)
//        bottomsheetfragment.arguments = bundle
//
//        bottomsheetfragment.show(fragmentManager,bottomsheetfragment.tag)
//    }
}