package com.example.mhg.Adapter

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.RecyclerView
import com.example.mhg.Dialog.PlayBottomSheetDialogFragment
import com.example.mhg.R
import com.example.mhg.VO.HomeRVBeginnerDataClass
import com.example.mhg.databinding.RoutineListBinding

class HomeHorizontalRecyclerViewAdapter(var routineList : List<HomeRVBeginnerDataClass>): RecyclerView.Adapter<HomeHorizontalRecyclerViewAdapter.MyViewHolder>() {

    inner class MyViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivhomehorizonal = view.findViewById<ImageView>(R.id.ivHomeHorizontal)
        val tvhomehorizontal = view.findViewById<TextView>(R.id.tvHomeHorizontalName)
        val btnhomeHorzontal = view.findViewById<Button>(R.id.btnHomeHorizonalPlay)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = RoutineListBinding.inflate(inflater, parent, false)
        return MyViewHolder(binding.root)
    }

    override fun getItemCount(): Int {
        return routineList.size
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val currentItem = routineList[position]
//        Glide.with(holder.itemView.context)
//            .load(currentItem.imgUrl)
//            .diskCacheStrategy(DiskCacheStrategy.ALL)
//            .into(holder.ivhomehorizonal)
        holder.tvhomehorizontal.text = currentItem.name

        holder.btnhomeHorzontal.setOnClickListener {
            showBottomSheetDialog(holder.itemView.context as FragmentActivity, currentItem)


        }
    }
    private fun showBottomSheetDialog(context: FragmentActivity, routine: HomeRVBeginnerDataClass) {
        val bottomsheetfragment = PlayBottomSheetDialogFragment()
        val fragmentManager = context.supportFragmentManager

        val bundle = Bundle()
        bundle.putParcelable("routineList", routine)
        bottomsheetfragment.arguments = bundle

        bottomsheetfragment.show(fragmentManager,bottomsheetfragment.tag)


    }
}