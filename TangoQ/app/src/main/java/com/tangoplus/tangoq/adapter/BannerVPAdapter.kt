package com.tangoplus.tangoq.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.tangoplus.tangoq.R

class BannerVPAdapter(private val imageList: ArrayList<String>, private val context : String ,private val mContext: Context) :
    RecyclerView.Adapter<BannerVPAdapter.MyViewHolder>() {
    inner class MyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView){
        val img: ImageView = itemView.findViewById(R.id.ivHomeBanner)
    }
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): BannerVPAdapter.MyViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_banner, parent, false)
        return MyViewHolder(view).apply {
            itemView.setOnClickListener {
//                val currentPosition = bindingAdapterPosition
//                Toast.makeText(mContext, "${currentPosition%5}번째 배너입니다.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onBindViewHolder(
        holder: BannerVPAdapter.MyViewHolder,
        position: Int
    ) {

        if (context == "intro") {
            val currentItem = imageList[position % 5]
            Glide.with(mContext).load(currentItem).into(holder.img) // 어떤 수가 나와도 5로 나눈 "나머지 값" 순서의 데이터로 5단위 반복되도록 함.
        } else if (context == "main") {
            val currentItem = imageList[position % 4]
            val resourceId = holder.itemView.context.resources.getIdentifier(
                currentItem, "drawable", holder.itemView.context.packageName
            )
            holder.img.setImageResource(resourceId)
        }

    }

    override fun getItemCount(): Int {
        return Int.MAX_VALUE // 무한처럼 보이게 가장 큰 숫자를 넣기
    }
}