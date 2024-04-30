package com.tangoplus.tangoq.Adapter

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnClickListener
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.MutableLiveData
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.tangoplus.tangoq.Dialog.FavoriteBSDialogFragment
import com.tangoplus.tangoq.Listener.OnRVClickListener
import com.tangoplus.tangoq.R
import com.tangoplus.tangoq.ViewModel.ExerciseVO
import com.tangoplus.tangoq.ViewModel.ProgramVO
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
        holder.tvRcmExerciseStep.text = currentItem.programStep
        holder.tvRcmExerciseTime.text = currentItem.programTime
        holder.tvRcmExerciseKcal.text = currentItem.programCount
        holder.ibtnRcmMore.setOnClickListener {
            val bsFragment = FavoriteBSDialogFragment()
            val bundle = Bundle()
            bundle.putParcelable("ExerciseUnit", currentItem) // programVO를 따로 다뤄서 BOTTOMSHEET에 넣는 부분도 필요함. ㅗ
            bsFragment.arguments = bundle
            val fragmentManager = fragment.requireActivity().supportFragmentManager
            bsFragment.show(fragmentManager, bsFragment.tag)
        }
        holder.vRcmPlay.setOnClickListener {

        }
    }
}