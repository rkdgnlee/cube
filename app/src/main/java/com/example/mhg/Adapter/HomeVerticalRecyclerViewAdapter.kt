package com.example.mhg.Adapter

import android.content.Intent
import android.os.Bundle
import android.util.SparseBooleanArray
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.MultiTransformation
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestOptions
import com.example.mhg.Dialog.PlayBottomSheetDialogFragment
import com.example.mhg.PlayActivity
import com.example.mhg.R
import com.example.mhg.VO.HomeRVBeginnerDataClass
import com.example.mhg.databinding.AddListBinding
import com.example.mhg.databinding.HomeListBinding
import com.example.mhg.databinding.TypeListBinding
import java.lang.IllegalArgumentException


class HomeVerticalRecyclerViewAdapter(var verticalList : List<HomeRVBeginnerDataClass>, var xmlname: String): RecyclerView.Adapter<RecyclerView.ViewHolder> () {

    inner class homeViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivhomevertical = view.findViewById<ImageView>(R.id.ivHomeVerticalImage)
        val tvhomeverticalname = view.findViewById<TextView>(R.id.tvHomeVerticalName)
        val tvhomeverticalDuration = view.findViewById<TextView>(R.id.tvHomeVerticalDuration)
        val btnPlayVertical = view.findViewById<Button>(R.id.btnHomeVerticalPlay)
    }
    inner class typeViewHolder(view: View): RecyclerView.ViewHolder(view) {
        val ivExerciseDetail = view.findViewById<ImageView>(R.id.ivExerciseDetail)
        val tvExerciseDetailName = view.findViewById<TextView>(R.id.tvExerciseDetailName)
        val tvExerciseDetailDscript = view.findViewById<TextView>(R.id.tvExerciseDetailDscript)
        val ibtnExerciseDetailPick = view.findViewById<ImageButton>(R.id.ibtnExerciseDetailPick)
    }

    inner class addViewHolder(view: View): RecyclerView.ViewHolder(view) {
        val ivPickAdd = view.findViewById<ImageView>(R.id.ivPickAdd)
        val tvPickAddName = view.findViewById<TextView>(R.id.tvPickAddName)
        val tvPickAddDscript = view.findViewById<TextView>(R.id.tvPickAddDscript)
        val cbPickAdd = view.findViewById<CheckBox>(R.id.cbPickAdd)
    }

    override fun getItemViewType(position: Int): Int {
        return  when (xmlname) {
            "home" -> 0
            "type" -> 1
            "add" -> 2
            else -> throw IllegalArgumentException("invalid view Type")
        }
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            0 -> {
                val binding = HomeListBinding.inflate(inflater, parent, false)
                homeViewHolder(binding.root)
            }
            1 -> {
                val binding = TypeListBinding.inflate(inflater, parent, false)
                typeViewHolder(binding.root)
            }
            2 -> {
                val binding = AddListBinding.inflate(inflater, parent, false)
                addViewHolder(binding.root)
            }
            else -> throw IllegalArgumentException("Invalid view type")
        }
    }



    override fun getItemCount(): Int {
        return verticalList.size
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int)  {
        val currentItem = verticalList[position]
        // -----! homeBeginner의 수직 RV 시작 !-----
        if (holder is homeViewHolder) {
            // -----! recyclerview에서 운동군 보여주기 !------

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

        } else if (holder is typeViewHolder) {
            // -----! RoutineDetail의 수직 RV 시작 !-----

            holder.tvExerciseDetailName.text = currentItem.exerciseName
            holder.tvExerciseDetailDscript.text = currentItem.exerciseDescription
            holder.ivExerciseDetail.setOnClickListener {
                val intent = Intent(it.context, PlayActivity::class.java)
                intent.putExtra("exercise", currentItem)
                it.context.startActivity(intent)
            }
            holder.tvExerciseDetailDscript.setOnClickListener {
                val intent = Intent(it.context, PlayActivity::class.java)
                intent.putExtra("exercise", currentItem)
                it.context.startActivity(intent)
            }
            // -----! 즐겨찾기 추가 기능 !-----
            var favorite = true
            holder.ibtnExerciseDetailPick.setOnClickListener {
                val imageResource = if (favorite) R.drawable.favorite_true else R.drawable.favorite_false
                holder.ibtnExerciseDetailPick.setImageResource(imageResource)
                favorite = !favorite
            }
            Glide.with(holder.itemView.context)
                .load(R.drawable.home_warmup)
                .apply(RequestOptions.bitmapTransform(MultiTransformation(CenterCrop(), RoundedCorners(16))))
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .into(holder.ivExerciseDetail)

            // -----! RoutineDetail의 수직 RV 끝 !-----

        } else if (holder is addViewHolder){

            // -----! PiclAdd 수직 RV 시작 !-----
            holder.tvPickAddName.text = currentItem.exerciseName
            holder.tvPickAddDscript.text = currentItem.exerciseDescription

//            val checkedItems = SparseBooleanArray()
//            holder.cbPickAdd.setOnCheckedChangeListener{ _, isChecked ->
//                checkedItems.put(position, isChecked)
//            }
//
//            fun getCheckedItems(): List<HomeRVBeginnerDataClass> {
//                val checkedData = mutableListOf<HomeRVBeginnerDataClass>()
//                for (i in 0 until checkedItems.size()) {
//                    if (checkedItems.valueAt(i)) {
//                        val checkedItem = originalData[checkedItems.keyAt(i)]
//                        checkedData.add(checkedItem)
//                    }
//                }
//                return checkedData
//            }
            // -----! PiclAdd 수직 RV 끝 !-----
        }

    }

//    private fun showBottomSheetDialog(context: FragmentActivity, warmup: HomeRVBeginnerDataClass) {
//        val bottomsheetfragment = PlayBottomSheetDialogFragment()
//        val fragmentManager = context.supportFragmentManager
//
//        val bundle = Bundle()
//        bundle.putParcelable("warmupList", warmup)
//        bottomsheetfragment.arguments = bundle
//
//        bottomsheetfragment.show(fragmentManager,bottomsheetfragment.tag)
//
//
//    }
}