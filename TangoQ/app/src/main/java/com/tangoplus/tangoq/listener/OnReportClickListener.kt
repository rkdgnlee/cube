package com.tangoplus.tangoq.listener

import androidx.core.widget.NestedScrollView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.google.android.material.card.MaterialCardView
import com.google.android.material.tabs.TabLayout

interface OnReportClickListener {
    fun onReportClick(currentItem: Int, nsv: NestedScrollView, mcv: MaterialCardView)
}