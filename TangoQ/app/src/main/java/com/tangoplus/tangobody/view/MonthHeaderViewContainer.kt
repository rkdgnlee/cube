package com.tangoplus.tangobody.view

import android.view.View
import android.widget.TextView
import com.kizitonwose.calendar.view.ViewContainer
import com.tangoplus.tangobody.R

class MonthHeaderViewContainer(view: View) : ViewContainer(view) {
    val tvSUN : TextView = view.findViewById(R.id.tvSUN)
    val tvMON : TextView = view.findViewById(R.id.tvMON)
    val tvTUE : TextView = view.findViewById(R.id.tvTUE)
    val tvWEN : TextView = view.findViewById(R.id.tvWEN)
    val tvTUR : TextView = view.findViewById(R.id.tvTUR)
    val tvFRI : TextView = view.findViewById(R.id.tvFRI)
    val tvSAT : TextView = view.findViewById(R.id.tvSAT)
}