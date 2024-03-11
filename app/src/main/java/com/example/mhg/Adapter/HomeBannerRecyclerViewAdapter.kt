package com.example.mhg.Adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.mhg.VO.HomeBannerItem
import com.example.mhg.R

class HomeBannerRecyclerViewAdapter(private val imageList: ArrayList<HomeBannerItem>, private val mContext: Context) :
RecyclerView.Adapter<HomeBannerRecyclerViewAdapter.MyViewHolder>() {
    inner class MyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView){
        val img: ImageView = itemView.findViewById(R.id.ivHomeBanner)
    }
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): HomeBannerRecyclerViewAdapter.MyViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.banner_item, parent, false)
        return MyViewHolder(view).apply {
            itemView.setOnClickListener {
                val currentPosition = bindingAdapterPosition
                Toast.makeText(mContext, "${currentPosition%5}번째 배너입니다.", Toast.LENGTH_SHORT).show()





            }
        }
    }

    override fun onBindViewHolder(
        holder: HomeBannerRecyclerViewAdapter.MyViewHolder,
        position: Int
    ) {
        Glide.with(mContext).load(imageList[position%5].img).into(holder.img) // 어떤 수가 나와도 5로 나눈 "나머지 값" 순서의 데이터로 5단위 반복되도록 함.
    }

    override fun getItemCount(): Int {
        return Int.MAX_VALUE // 무한처럼 보이게 가장 큰 숫자를 넣기
    }
}