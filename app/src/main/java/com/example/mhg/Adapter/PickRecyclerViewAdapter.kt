package com.example.mhg.Adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.RecyclerView
import com.example.mhg.VO.RoutingVO
import com.example.mhg.databinding.RvPickListBinding
import com.example.mhg.onPickDetailClickListener


class PickRecyclerViewAdapter(var pickList: MutableList<String>, private val listener: onPickDetailClickListener ) : RecyclerView.Adapter<PickRecyclerViewAdapter.MyViewHolder>() {
//    fun showBottomSheetDialog(context: FragmentActivity, pick: ExerciseViewModel) {
//        val bottomsheetfragment = PickBottomSheetDialogFragment()
//        val fragmentManager = context.supportFragmentManager
//        val bundle = Bundle()
//        bundle.putParcelable("picklist", pick.exercises.value?.get(0))
//        bottomsheetfragment.arguments = bundle
//        bottomsheetfragment.show(fragmentManager, bottomsheetfragment.tag)
//    }

    inner class MyViewHolder(private val binding: RvPickListBinding, private val context: FragmentActivity) : RecyclerView.ViewHolder(binding.root) {
        fun bind(pick : RoutingVO) {
            binding.tvPickTitle.text = pick.title
            binding.ivMore.setOnClickListener {
//                showBottomSheetDialog(context, pick)
            }
            binding.tvPickTitle.setOnClickListener {
                listener.onPickClick(pick.title)
            }
        }
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder = MyViewHolder(
        RvPickListBinding.inflate (
            LayoutInflater.from(parent.context),
            parent,
            false
        ), parent.context as FragmentActivity
    )

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val currentItem = pickList.value!![position]
    }

    override fun getItemCount(): Int {
        return pickList.value!!.length()
    }
}