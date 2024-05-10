package com.tangoplus.tangoq.adapter

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.MutableLiveData
import androidx.recyclerview.widget.RecyclerView
import com.tangoplus.tangoq.dialog.RecommendBSDialogFragment
import com.tangoplus.tangoq.listener.OnRVClickListener
import com.tangoplus.tangoq.R
import com.tangoplus.tangoq.data.ProgramVO
import com.tangoplus.tangoq.databinding.RvRecommendItemBinding

class RecommendRVAdapter(var programs: MutableLiveData<MutableList<ProgramVO>>, private val onRVClickListener: OnRVClickListener, val fragment : Fragment)
    : RecyclerView.Adapter<RecommendRVAdapter.MyViewHolder>() {
        inner class MyViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val ivRcmThumbnail = view.findViewById<ImageView>(R.id.ivRcmThumbnail)
            val tvRcmExerciseName = view.findViewById<TextView>(R.id.tvRcmExerciseName)
            val tvRcmExerciseTime = view.findViewById<TextView>(R.id.tvRcmExerciseTime)
            val tvRcmExerciseStep = view.findViewById<TextView>(R.id.tvRcmExerciseStep)
            val tvRcmExerciseKcal = view.findViewById<TextView>(R.id.tvRcmExerciseKcal)
            val ibtnRcmMore = view.findViewById<ImageButton>(R.id.ibtnRcmMore)
            val vRcmPlay = view.findViewById<View>(R.id.vRcmPlay)
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = RvRecommendItemBinding.inflate(inflater, parent, false)
        return MyViewHolder(binding.root)
    }
    override fun getItemCount(): Int {
        return programs.value!!.size
    }
    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val currentItem = programs.value!![position]

//        Glide.with(holder.itemView.context)
//            .load(currentItem.programImageUrl)
//            .diskCacheStrategy(DiskCacheStrategy.ALL)
//            .into(holder.ivRcmThumbnail)
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
}