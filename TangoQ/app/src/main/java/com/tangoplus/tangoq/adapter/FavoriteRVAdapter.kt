package com.tangoplus.tangoq.adapter

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.tangoplus.tangoq.dialog.FavoriteBSDialogFragment
import com.tangoplus.tangoq.listener.OnFavoriteDetailClickListener
import com.tangoplus.tangoq.R
import com.tangoplus.tangoq.data.FavoriteItemVO

import com.tangoplus.tangoq.databinding.RvFvItemBinding

class FavoriteRVAdapter(
    var fvList: MutableList<FavoriteItemVO>,
    val listener: OnFavoriteDetailClickListener,
    private val fragment: Fragment
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    inner class fvViewHolder(view: View): RecyclerView.ViewHolder(view) {
        val vFvTab = view.findViewById<View>(R.id.vFvTab)
        val tvPickName = view.findViewById<TextView>(R.id.tvFvName)
        val tvFvTime = view.findViewById<TextView>(R.id.tvFvTime)
        val tvFvCount = view.findViewById<TextView>(R.id.tvFvCount)
        val tvFvRegDate = view.findViewById<TextView>(R.id.tvFvRegDate)
        val ivFvThumbnail1 = view.findViewById<ImageView>(R.id.ivFvThumbnail1)
        val ivFvThumbnail2 = view.findViewById<ImageView>(R.id.ivFvThumbnail2)
        val ivFvThumbnail3 = view.findViewById<ImageView>(R.id.ivFvThumbnail3)
        val ivFvThumbnail4 = view.findViewById<ImageView>(R.id.ivFvThumbnail4)
        val ivFvThumbnailNull = view.findViewById<ImageView>(R.id.ivFvThumbnailNull)
        val tvFvInfo = view.findViewById<TextView>(R.id.tvFvRegDate)
        val ibtnFvMore = view.findViewById<ImageButton>(R.id.ibtnFvMore)

    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = RvFvItemBinding.inflate(inflater, parent, false)
        return fvViewHolder(binding.root)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val currentItem = fvList[position]


        if (holder is fvViewHolder) {
            holder.ivFvThumbnailNull.visibility = View.GONE
            holder.tvPickName.text = currentItem.favoriteName
            holder.tvFvCount.text = currentItem.favoriteTotalCount
            holder.tvFvTime.text = currentItem.favoriteTotalTime
            holder.tvFvRegDate.text = currentItem.favoriteRegDate?.substring(0, 11)
            holder.vFvTab.setOnClickListener {
                listener.onFavoriteClick(currentItem.favoriteName.toString())
            }
            holder.ibtnFvMore.setOnClickListener {
                val bsFragment = FavoriteBSDialogFragment()
                val bundle = Bundle()
                bundle.putParcelable("Favorite", currentItem)
                bsFragment.arguments = bundle
                val fragmentManager = fragment.requireActivity().supportFragmentManager
                bsFragment.show(fragmentManager, bsFragment.tag)
            }


//            holder.ivFvThumbnail1.visibility = View.VISIBLE
//            holder.ivFvThumbnail2.visibility = View.VISIBLE
//            holder.ivFvThumbnail3.visibility = View.VISIBLE
//            holder.ivFvThumbnail4.visibility = View.VISIBLE
            if (currentItem.imgThumbnailList?.size!! >= 4) {
//                holder.ivFvThumbnailNull.visibility = View.GONE
//                holder.ivFvThumbnail1.setImageResource()
            } else {
//                holder.ivFvThumbnailNull.visibility = View.VISIBLE

            }

        }
    }

    override fun getItemCount(): Int {
        return fvList.size
    }
}