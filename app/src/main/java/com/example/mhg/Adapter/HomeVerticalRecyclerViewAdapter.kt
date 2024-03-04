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
import com.example.mhg.databinding.WarmupListBinding


class HomeVerticalRecyclerViewAdapter(var warmupList : List<HomeRVBeginnerDataClass>): RecyclerView.Adapter<HomeVerticalRecyclerViewAdapter.MyViewHolder> () {

    inner class MyViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivhomevertical = view.findViewById<ImageView>(R.id.ivHomeVerticalImage)
        val tvhomeverticalname = view.findViewById<TextView>(R.id.tvHomeVerticalName)
        val tvhomeverticalDuration = view.findViewById<TextView>(R.id.tvHomeVerticalDuration)
        val btnPlayVertical = view.findViewById<Button>(R.id.btnHomeVerticalPlay)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = WarmupListBinding.inflate(inflater, parent, false)
        return MyViewHolder(binding.root)
    }

    override fun getItemCount(): Int {
        return warmupList.size
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val currentItem = warmupList[position]
        holder.tvhomeverticalDuration.text = currentItem.duration.toString()
        holder.tvhomeverticalname.text = currentItem.name
//        Glide.with(holder.itemView.context)
//            .load(currentItem.imgUrl)
//            .diskCacheStrategy(DiskCacheStrategy.ALL)
//            .into(holder.ivhomevertical)

        holder.btnPlayVertical.setOnClickListener {
            showBottomSheetDialog(holder.itemView.context as FragmentActivity, currentItem)
        }
    }
    private fun showBottomSheetDialog(context: FragmentActivity, warmup: HomeRVBeginnerDataClass) {
        val bottomsheetfragment = PlayBottomSheetDialogFragment()
        val fragmentManager = context.supportFragmentManager

        val bundle = Bundle()
        bundle.putParcelable("warmupList", warmup)
        bottomsheetfragment.arguments = bundle

        bottomsheetfragment.show(fragmentManager,bottomsheetfragment.tag)


    }
}