package com.example.mhg.Adapter

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.RecyclerView
import com.example.mhg.Dialog.PickBottomSheetDialogFragment
import com.example.mhg.Dialog.PlayBottomSheetDialogFragment
import com.example.mhg.VO.RoutingVO
import com.example.mhg.databinding.PicklistBinding
import kotlinx.coroutines.NonDisposableHandle.parent


class PickRecyclerViewAdapter(var pickList: MutableList<RoutingVO>) : RecyclerView.Adapter<PickRecyclerViewAdapter.MyViewHolder>() {
    fun showBottomSheetDialog(context: FragmentActivity, pick : RoutingVO) {
        val bottomsheetfragment = PickBottomSheetDialogFragment()
        val fragmentManager = context.supportFragmentManager
        val bundle = Bundle()
        bundle.putParcelable("picklist", pick)
        bottomsheetfragment.arguments = bundle
        bottomsheetfragment.show(fragmentManager, bottomsheetfragment.tag)
    }


    inner class MyViewHolder(private val binding: PicklistBinding, private val context: FragmentActivity) : RecyclerView.ViewHolder(binding.root) {
        fun bind(pick : RoutingVO) {
            binding.tvPickTitle.text = pick.title
            binding.ivMore.setOnClickListener {
                showBottomSheetDialog(context, pick)
            }
        }
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder = MyViewHolder(
        PicklistBinding.inflate (
            LayoutInflater.from(parent.context),
            parent,
            false
        ), parent.context as FragmentActivity
    )

    override fun onBindViewHolder(holder: PickRecyclerViewAdapter.MyViewHolder, position: Int) = holder.bind(pickList[position])

    override fun getItemCount(): Int {
        return pickList.size
    }
}