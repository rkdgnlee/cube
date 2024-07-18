package com.tangoplus.tangoq.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.tangoplus.tangoq.R
import com.tangoplus.tangoq.databinding.RvMuscleItemBinding

class MuscleRVAdpater(private val fragment: Fragment, private val muscleList: MutableList<String>?) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    inner class muscleViewHolder(view : View) : RecyclerView.ViewHolder(view) {
        val ivMI : ImageView = view.findViewById(R.id.ivMI)
        val tvMIName : TextView = view.findViewById(R.id.tvMIName)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = RvMuscleItemBinding.inflate(inflater, parent, false)
        return muscleViewHolder(binding.root)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {

        if (holder is muscleViewHolder) {
            holder.tvMIName.text = muscleList!![position]
            when (muscleList[position]) {
                "삼각근" -> setIV("sam_gak", holder.ivMI)
                "대흉근" -> setIV("dae_hyung", holder.ivMI)
                "전거근" -> setIV("jeon_geo", holder.ivMI)
                "이두근" -> setIV("i_du", holder.ivMI)
                "이두" -> setIV("i_du", holder.ivMI)
                "목빗근" -> setIV("mok_bit", holder.ivMI)
                "사각근" -> setIV("sa_gak", holder.ivMI)
                "복근" -> setIV("bok", holder.ivMI)
                "하복부" -> setIV("ha_bok", holder.ivMI)
                "회내근" -> setIV("hoe_nae", holder.ivMI)
                "전완근" -> setIV("jeon_wan", holder.ivMI)
                "대퇴사두근" -> setIV("dae_toe_sa_du", holder.ivMI)
                "후면 삼각근" -> setIV("hu_myeon_sam_gak", holder.ivMI)
                "삼두근" -> setIV("sam_du", holder.ivMI)
                "삼두" -> setIV("sam_du", holder.ivMI)
                "승모근" -> setIV("seung_mo", holder.ivMI)
                "대원근" -> setIV("dae_won", holder.ivMI)
                "광배근" -> setIV("gwang_bae", holder.ivMI)
                "척추기립근" -> setIV("cheock_chu_gi_rip", holder.ivMI)
                "요방형근" -> setIV("yo_bang_hyeong", holder.ivMI)
                "둔근" -> setIV("dun", holder.ivMI)
                "대퇴이두" -> setIV("dae_toe_i_du", holder.ivMI)
                "햄스트링" -> setIV("dae_toe_i_du", holder.ivMI)
                else -> {

                }
            }
        }
    }

    override fun getItemCount(): Int {
        return muscleList!!.size
    }

    private fun setIV(name: String, imageView: ImageView) {
//        Glide.with(fragment)
//            .load(fragment.resources.getIdentifier("drawable_muscle_${name}", "drawable", fragment.requireActivity().packageName))
//            .into(imageView)
        imageView.setImageResource(fragment.resources.getIdentifier("drawable_muscle_${name}", "drawable", fragment.requireActivity().packageName))
    }
}