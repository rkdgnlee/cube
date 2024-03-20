package com.example.mhg.Adapter

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.example.mhg.Dialog.PlayBottomSheetDialogFragment
import com.example.mhg.R
import com.example.mhg.VO.HomeRVBeginnerDataClass
import com.example.mhg.databinding.TypeListBinding
import org.w3c.dom.Text

class HomeRoutineRecyclerViewAdapter(var typeList: List<HomeRVBeginnerDataClass>): RecyclerView.Adapter<HomeRoutineRecyclerViewAdapter.MyViewHolder> () {
    inner class MyViewHolder(view: View): RecyclerView.ViewHolder(view) {
        val ivExerciseDetail = view.findViewById<ImageView>(R.id.ivExerciseDetail)
        val tvExerciseDetailName = view.findViewById<TextView>(R.id.tvExerciseDetailName)
        val tvExerciseDetailDscript = view.findViewById<TextView>(R.id.tvExerciseDetailDscript)
        val ibtnExerciseDetailPick = view.findViewById<ImageButton>(R.id.ibtnExerciseDetailPick)
        val clTypeList = view.findViewById<ConstraintLayout>(R.id.clTypeList)
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): HomeRoutineRecyclerViewAdapter.MyViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = TypeListBinding.inflate(inflater, parent, false)
        return MyViewHolder(binding.root)
    }
    override fun getItemCount(): Int {
        return typeList.size
    }

    override fun onBindViewHolder(
        holder: HomeRoutineRecyclerViewAdapter.MyViewHolder,
        position: Int
    ) {
        val currentItem = typeList[position]
        holder.tvExerciseDetailName.text = currentItem.exerciseName
        holder.tvExerciseDetailDscript.text = currentItem.exerciseDescription
        holder.clTypeList.setOnClickListener {
            showBottomSheetDialog(holder.itemView.context as FragmentActivity, currentItem)
        }
//        Glide.with(holder.itemView.context)
//            .load(currentItem.imgUrl)
//            .diskCacheStrategy(DiskCacheStrategy.ALL)
//            .into(holder.ivExerciseDetail)


    }

    private fun showBottomSheetDialog(context: FragmentActivity, type: HomeRVBeginnerDataClass) {
        val bottomsheetfragment = PlayBottomSheetDialogFragment()
        val fragmentManager = context.supportFragmentManager

        val bundle = Bundle()
        bundle.putParcelable("typeList", type)
        bottomsheetfragment.arguments = bundle

        bottomsheetfragment.show(fragmentManager,bottomsheetfragment.tag)
    }
}