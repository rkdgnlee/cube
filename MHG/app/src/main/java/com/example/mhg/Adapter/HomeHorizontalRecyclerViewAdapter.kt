package com.example.mhg.Adapter

import android.content.ContentValues.TAG
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.example.mhg.HomeRoutineDetailFragment
import com.example.mhg.R
import com.example.mhg.databinding.RvHorizontalListBinding

class HomeHorizontalRecyclerViewAdapter(private val fragment: Fragment, var routineList : List<String>): RecyclerView.Adapter<HomeHorizontalRecyclerViewAdapter.MyViewHolder>() {

    inner class MyViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivhomehorizonal = view.findViewById<ImageView>(R.id.ivHomeHorizontal)
        val tvhomehorizontal = view.findViewById<TextView>(R.id.tvHomeHorizontalName)
        val btnhomeHorzontal = view.findViewById<Button>(R.id.btnHomeHorizonalPlay)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = RvHorizontalListBinding.inflate(inflater, parent, false)
        return MyViewHolder(binding.root)
    }

    override fun getItemCount(): Int {
        return routineList.size
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val currentItem = routineList[position]

        // -----! item하나의 기능 구현 시작 !-----
        holder.tvhomehorizontal.text = currentItem
//        Glide.with(holder.itemView.context)
//            .load(currentItem.imgUrl)
//            .diskCacheStrategy(DiskCacheStrategy.ALL)
//            .into(holder.ivhomehorizonal)


        holder.btnhomeHorzontal.setOnClickListener {
            showDetailFragment(currentItem)
            Log.w(TAG+"타입", currentItem)
        }
    }
//    private fun showBottomSheetDialog(context: FragmentActivity, type: String) {
//        val bottomsheetfragment = PlayBottomSheetDialogFragment()
//        val fragmentManager = context.supportFragmentManager
//
//        val bundle = Bundle()
//        bundle.putString("routineList", type)
//        bottomsheetfragment.arguments = bundle
//
//        bottomsheetfragment.show(fragmentManager,bottomsheetfragment.tag)
//    }

    private fun showDetailFragment(type: String) {
        val HomeRoutineDetailFragment = HomeRoutineDetailFragment()

        val bundle = Bundle()
        bundle.putString("type", type)
        HomeRoutineDetailFragment.arguments = bundle
        fragment.requireActivity().supportFragmentManager.beginTransaction().apply {
            setCustomAnimations(R.anim.slide_in_left, R.anim.slide_in_right)
            replace(R.id.flHome, HomeRoutineDetailFragment)
            addToBackStack(null)
            commit()
        }
    }
    // -----! item하나의 기능 구현 끝 !-----
}