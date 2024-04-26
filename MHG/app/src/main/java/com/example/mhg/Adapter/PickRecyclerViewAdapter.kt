package com.example.mhg.Adapter

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.RecyclerView
import com.example.mhg.Dialog.PickBottomSheetDialogFragment
import com.example.mhg.R
import com.example.mhg.VO.PickItemVO
import com.example.mhg.VO.RoutingVO
import com.example.mhg.databinding.RvPickListBinding
import com.example.mhg.onPickDetailClickListener


class PickRecyclerViewAdapter(var pickList: MutableList<String>,val listener: onPickDetailClickListener, private val activity: FragmentActivity) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    inner class listViewHolder(view: View): RecyclerView.ViewHolder(view) {
        val tvPickTitle = view.findViewById<TextView>(R.id.tvPickTitle)
        val ivMore = view.findViewById<ImageView>(R.id.ivMore)
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = RvPickListBinding.inflate(inflater, parent, false)
        return listViewHolder(binding.root)
    }
    override fun getItemCount(): Int {
        return pickList.size
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val currentItem = pickList[position]

        if (holder is listViewHolder) {
            holder.tvPickTitle.text = currentItem
            holder.tvPickTitle.setOnClickListener {
                listener.onPickClick(currentItem)
            }
            holder.ivMore.setOnClickListener {
                showBottomSheetDialog(activity, pickList[position])
            }
        }
    }
    fun showBottomSheetDialog(context: FragmentActivity, pick: String) {
        val bottomsheetfragment = PickBottomSheetDialogFragment()
        val fragmentManager = context.supportFragmentManager
        val bundle = Bundle()
        bundle.putString("pickItemName", pick)
        bottomsheetfragment.arguments = bundle
        bottomsheetfragment.show(fragmentManager, bottomsheetfragment.tag)
    }
}