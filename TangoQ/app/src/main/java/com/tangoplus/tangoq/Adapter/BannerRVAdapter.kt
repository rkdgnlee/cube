package com.tangoplus.tangoq.Adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.tangoplus.tangoq.R
import com.tangoplus.tangoq.ViewModel.BannerViewModel

class BannerRVAdapter(private val imageList: ArrayList<String>, private val mContext: Context) :
    RecyclerView.Adapter<BannerRVAdapter.MyViewHolder>() {
    inner class MyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView){
        val img: ImageView = itemView.findViewById(R.id.ivHomeBanner)
    }
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): BannerRVAdapter.MyViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_banner, parent, false)
        return MyViewHolder(view).apply {
            itemView.setOnClickListener {
//                val currentPosition = bindingAdapterPosition
//                Toast.makeText(mContext, "${currentPosition%5}번째 배너입니다.", Toast.LENGTH_SHORT).show()





            }
        }
    }

    override fun onBindViewHolder(
        holder: BannerRVAdapter.MyViewHolder,
        position: Int
    ) {
        Glide.with(mContext).load(imageList[position%5]).into(holder.img) // 어떤 수가 나와도 5로 나눈 "나머지 값" 순서의 데이터로 5단위 반복되도록 함.
    }

    override fun getItemCount(): Int {
        return Int.MAX_VALUE // 무한처럼 보이게 가장 큰 숫자를 넣기
    }
}