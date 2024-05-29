package com.tangoplus.tangoq.listener

import android.view.View
import androidx.core.widget.NestedScrollView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.google.android.material.card.MaterialCardView
import com.google.android.material.tabs.TabLayout

interface OnReportClickListener {
    fun onReportScroll(view: View)
}