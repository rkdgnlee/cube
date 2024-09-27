package com.tangoplus.tangoq.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.tangoplus.tangoq.R
import com.tangoplus.tangoq.databinding.RvProgramItemBinding
import com.tangoplus.tangoq.dialog.ProgramCustomDialogFragment

class StringIntIntRVAdapter(private val fragment: Fragment, private val data : List<Pair<String, Int>>) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    inner class programViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvPI : TextView = view.findViewById(R.id.tvPI)
        val clPI : ConstraintLayout = view.findViewById(R.id.clPI)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = RvProgramItemBinding.inflate(inflater, parent, false)
        return programViewHolder(binding.root)
    }

    override fun getItemCount(): Int {
        return data.size
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val currentItem = data[position]
        if (holder is programViewHolder) {
            holder.tvPI.text = currentItem.first
            holder.clPI.setOnClickListener {
                val dialog = ProgramCustomDialogFragment.newInstance(currentItem.second)
                dialog.show(fragment.requireActivity().supportFragmentManager, "ProgramCustomDialogFragment")
            }

        }
    }
}