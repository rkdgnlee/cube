package com.tangoplus.tangoq.adapter

import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.widget.AppCompatButton
import androidx.cardview.widget.CardView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.widget.NestedScrollView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearSmoothScroller
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.google.android.material.card.MaterialCardView
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayout.Tab
import com.skydoves.balloon.ArrowPositionRules
import com.skydoves.balloon.Balloon
import com.skydoves.balloon.BalloonAnimation
import com.skydoves.balloon.BalloonSizeSpec
import com.skydoves.balloon.showAlignEnd
import com.skydoves.balloon.showAlignTop
import com.tangoplus.tangoq.R
import com.tangoplus.tangoq.databinding.RvRecommendItemBinding
import com.tangoplus.tangoq.databinding.RvReportExpandedItemBinding
import com.tangoplus.tangoq.dialog.LoginDialogFragment
import com.tangoplus.tangoq.dialog.PoseViewDialogFragment
import com.tangoplus.tangoq.listener.OnReportClickListener
import org.json.JSONObject


class ReportRVAdapter(val parts : MutableList<Triple<String,  String, JSONObject>>, private val fragment: Fragment, private val listener: OnReportClickListener, private val nsv : NestedScrollView) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    // TODO REPORT 쏴줄 때의 VO 필요. 8개 TAB LAYOUT에 들어가는 각각의 수치들.
    private lateinit var tl: TabLayout
    inner class partViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvREName = view.findViewById<TextView>(R.id.tvREName)
        val clRE = view.findViewById<ConstraintLayout>(R.id.clRE)
        val cvRELine = view.findViewById<CardView>(R.id.cvRELine)
        val ivRE = view.findViewById<ImageView>(R.id.ivRE)
        val ivReArrow = view.findViewById<ImageView>(R.id.ivReArrow)
        val tlRE = view.findViewById<TabLayout>(R.id.tlRE)
        val mcvExpand = view.findViewById<MaterialCardView>(R.id.mcvExpand)
        val btnREShowDetail = view.findViewById<AppCompatButton>(R.id.btnREShowDetail)
        val ibtnREInfo = view.findViewById<ImageButton>(R.id.ibtnREInfo)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = RvReportExpandedItemBinding.inflate(inflater, parent, false)
        return partViewHolder(binding.root)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val currentItem = parts[position]
        if (holder is partViewHolder) {
            holder.mcvExpand.visibility = View.GONE
            holder.tvREName.text = currentItem.first
            holder.clRE.setOnClickListener {
                if (holder.mcvExpand.visibility == View.GONE) {
                    holder.mcvExpand.visibility = View.VISIBLE
                    holder.mcvExpand.animate().apply {
                        duration = 150
                        rotation(0f)
                    }
                    holder.ivReArrow.setImageResource(R.drawable.icon_arrow_up)
                    Handler(Looper.getMainLooper()).postDelayed({
                        listener.onReportScroll(holder.clRE)
                    }, 200)

                } else {
                    holder.mcvExpand.visibility = View.GONE
                    holder.mcvExpand.animate().apply {
                        duration = 150
                        rotation(0f)
                    }
                    holder.ivReArrow.setImageResource(R.drawable.icon_arrow_down)
                }
            }
            tl = holder.tlRE
            when (currentItem.first) {
                "정면 자세" -> {
                    val tabs = listOf("ear", "shoulder", "elbow", "wrist", "hip", "knee", "ankle")
                    addTab("목", holder.tlRE)
                    addTab("어깨", holder.tlRE)
                    addTab("팔꿉", holder.tlRE)
                    addTab("손목", holder.tlRE)
                    addTab("엉덩", holder.tlRE)
                    addTab("무릎", holder.tlRE)
                    addTab("발목", holder.tlRE)
                    setTabListener(holder, currentItem, tabs, "result_static_front_horizontal_angle_")
                }
                "팔꿉 측정 자세" -> {
                    val tabs = listOf("shoulder_elbow", "elbow_wrist", "hip_knee", "knee_ankle")
                    addTab("상완", holder.tlRE)
                    addTab("하완", holder.tlRE)
                    addTab("허벅지", holder.tlRE)
                    addTab("종아리", holder.tlRE)
                    setTabListener(holder, currentItem, tabs, "result_static_front_vertical_angle_", "_left")
                }
                "오버헤드 스쿼트" -> {
                    val tabs = listOf("wrist", "hip", "knee")
                    addTab("손", holder.tlRE)
                    addTab("엉덩", holder.tlRE)
                    addTab("무릎", holder.tlRE)
                    setTabListener(holder, currentItem, tabs, "res_ov_hd_sq_fnt_horiz_ang_")
                }
                "왼쪽 측면 자세" -> { // 왼쪽
                    holder.cvRELine.rotation = 90f
                    val tabs = listOf("ear_shoulder", "shoulder_elbow", "elbow_wrist", "hip_knee")
                    addTab("목", holder.tlRE)
                    addTab("상완", holder.tlRE)
                    addTab("하완", holder.tlRE)
                    addTab("허벅지", holder.tlRE)
                    setTabListener(holder, currentItem, tabs, "result_static_side_left_vertical_angle_")
                }
                "오른쪽 측면 자세" -> { // 오른쪽
                    holder.cvRELine.rotation = 90f
                    val tabs = listOf("ear_shoulder", "shoulder_elbow", "elbow_wrist", "hip_knee")
                    addTab("목", holder.tlRE)
                    addTab("상완", holder.tlRE)
                    addTab("하완", holder.tlRE)
                    addTab("허벅지", holder.tlRE)
                    setTabListener(holder, currentItem, tabs, "result_static_side_right_vertical_angle_", "left")
                }
                "후면 자세" -> { // 오른쪽
                    val tabs = listOf("ear", "shoulder", "elbow", "hip", "ankle")
                    addTab("목", holder.tlRE )
                    addTab("어깨", holder.tlRE)
                    addTab("팔꿉", holder.tlRE)
                    addTab("엉덩", holder.tlRE)
                    addTab("발목", holder.tlRE)
                    setTabListener(holder, currentItem, tabs, "result_static_back_horizontal_angle_")
                }
                "의자 후면" -> { // 오른쪽
                    val tabs = listOf("ear", "shoulder", "hip")
                    addTab("목", holder.tlRE)
                    addTab("어깨", holder.tlRE)
                    addTab("엉덩", holder.tlRE)
                    setTabListener(holder, currentItem, tabs, "result_static_back_sit_horizontal_angle_")
                }
            }


            val balloon = Balloon.Builder(fragment.requireContext())
                .setWidthRatio(0.6f)
                .setHeight(BalloonSizeSpec.WRAP)
                .setText("수평·수직에 가까울 수록\n바른 자세입니다.")
                .setTextColorResource(R.color.black)
                .setTextSize(15f)
                .setArrowPositionRules(ArrowPositionRules.ALIGN_ANCHOR)
                .setArrowSize(0)
                .setMargin(6)
                .setPadding(12)
                .setCornerRadius(8f)
                .setBackgroundColorResource(R.color.subColor100)
                .setBalloonAnimation(BalloonAnimation.ELASTIC)
                .setLifecycleOwner(fragment.viewLifecycleOwner)
                .build()
            holder.ibtnREInfo.setOnClickListener {
                holder.ibtnREInfo.showAlignEnd(balloon)
                balloon.dismissWithDelay(2500L)
            }

            holder.btnREShowDetail.setOnClickListener {
//                fragment.requireActivity().supportFinishAfterTransition()
                val dialog = PoseViewDialogFragment.newInstance(currentItem.second)
                dialog.show(fragment.requireActivity().supportFragmentManager, "PoseViewDialogFragment")

            }
        }
    }

    override fun getItemCount(): Int {
        return parts.size
    }

    @SuppressLint("Recycle")
    private fun setBalanceLine(cv: CardView, prevScore : Float, afterScore: Float) {
        val animator = ObjectAnimator.ofFloat(cv, "rotation", prevScore, afterScore)
        animator.duration = 300
        animator.start()
    }

    private fun addTab(tabName: String, tl: TabLayout) {
        val tab = tl.newTab().apply {
            text = tabName
        }
        tl.addTab(tab)
    }

    private fun setTabListener(holder: partViewHolder, currentItem: Triple<String, String, JSONObject>, tabs: List<String>, anglePrefix: String, angleSuffix: String = "") {
        holder.tlRE.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: Tab?) {

                if (tab != null && currentItem.third != null && currentItem.third.keys().hasNext()) {
                    val angleKey = anglePrefix + tabs[tab.position] + angleSuffix
                    val angleValue = currentItem.third.optDouble(angleKey)
                    setBalanceLine(holder.cvRELine, 0f, Math.toDegrees(angleValue).toFloat())
                    Log.v("angle", "${ Math.toDegrees(angleValue).toFloat()}")
                } else {
                    if (currentItem.first == "왼쪽 측면 자세" || currentItem.first == "오른쪽 측면 자세"  ) {
                        setBalanceLine(holder.cvRELine, 90f, 90f)
                    } else {
                        setBalanceLine(holder.cvRELine, 0f, 0f)
                    }

                }
            }
            override fun onTabUnselected(tab: Tab?) {}
            override fun onTabReselected(tab: Tab?) {}
        })
    }
}