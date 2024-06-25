package com.tangoplus.tangoq.adapter

import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.widget.AppCompatButton
import androidx.cardview.widget.CardView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.widget.NestedScrollView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.bumptech.glide.Glide
import com.google.android.material.card.MaterialCardView
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayout.Tab
import com.skydoves.balloon.ArrowPositionRules
import com.skydoves.balloon.Balloon
import com.skydoves.balloon.BalloonAnimation
import com.skydoves.balloon.BalloonSizeSpec
import com.skydoves.balloon.showAlignEnd
import com.tangoplus.tangoq.R
import com.tangoplus.tangoq.data.MeasureVO
import com.tangoplus.tangoq.databinding.RvReportExpandedItemBinding
import com.tangoplus.tangoq.dialog.PoseViewDialogFragment
import com.tangoplus.tangoq.listener.OnReportClickListener
import org.json.JSONObject


class ReportRVAdapter(val parts : MutableList<MeasureVO>, private val fragment: Fragment, private val listener: OnReportClickListener, private val nsv : NestedScrollView) : RecyclerView.Adapter<ViewHolder>() {

    private lateinit var tl: TabLayout
    inner class PartViewHolder(view: View) : ViewHolder(view) {
        val tvREName: TextView = view.findViewById(R.id.tvREName)
        val clRE: ConstraintLayout = view.findViewById(R.id.clRE)
        val cvRELine: CardView = view.findViewById(R.id.cvRELine)
        val ivRE: ImageView = view.findViewById(R.id.ivRE)
        val ivReArrow: ImageView = view.findViewById(R.id.ivReArrow)
        val tlRE: TabLayout = view.findViewById(R.id.tlRE)
        val mcvExpand: MaterialCardView = view.findViewById(R.id.mcvExpand)
        val btnREShowDetail: AppCompatButton = view.findViewById(R.id.btnREShowDetail)
        val ibtnREInfo: ImageButton = view.findViewById(R.id.ibtnREInfo)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = RvReportExpandedItemBinding.inflate(inflater, parent, false)
        return PartViewHolder(binding.root)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val currentItem = parts[position]
        if (holder is PartViewHolder) {
            holder.mcvExpand.visibility = View.GONE
            holder.tvREName.text = currentItem.partName
            Glide.with(fragment)
                .load(fragment.resources.getIdentifier("drawable_posture_${position + 1}_disabled", "drawable", fragment.requireActivity().packageName))
                .into(holder.ivRE)
            // -----! 펼치기 접기 시작 !------
            holder.clRE.setOnClickListener {
                if (holder.mcvExpand.visibility == View.GONE) {
                    holder.mcvExpand.visibility = View.VISIBLE
                    Glide.with(fragment)
                        .load(fragment.resources.getIdentifier("drawable_posture_${position + 1}_enabled", "drawable", fragment.requireActivity().packageName))
                        .into(holder.ivRE)


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
                    Glide.with(fragment)
                        .load(fragment.resources.getIdentifier("drawable_posture_${position + 1}_disabled", "drawable", fragment.requireActivity().packageName))
                        .into(holder.ivRE)
                    holder.mcvExpand.animate().apply {
                        duration = 150
                        rotation(0f)
                    }
                    holder.ivReArrow.setImageResource(R.drawable.icon_arrow_down)
                }
            } // -----! 펼치기 접기 끝 !------

            /** 점수 process
             *  1. 탭 레이아웃을 만들며 각각의 list를 가져옴
             *  2. 해당 anglePrefix + 순서 + 각 부위 영문명 result_static_front_horizontal_angle_shoulder_elbow_left 등
             *  3. 해당 값으로 json을 가져옴 (setTabListener)
             * */

            tl = holder.tlRE
            when (currentItem.partName) {
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
                    setTabListener(holder, currentItem, tabs, "result_static_side_right_vertical_angle_", "_right")
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
                val dialog = PoseViewDialogFragment.newInstance(currentItem.partName)
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

    private fun setTabListener(holder: PartViewHolder, currentItem: MeasureVO, tabs: List<String>, anglePrefix: String, angleSuffix: String = "") {
        holder.tlRE.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: Tab?) {

                if (tab != null && currentItem.anglesNDistances != null && currentItem.anglesNDistances.keys().hasNext()) {
                    val angleKey = anglePrefix + tabs[tab.position] + angleSuffix

                    // ------! 각도 받아와서 계산 !------

                    val angleValue = currentItem.anglesNDistances.optDouble(angleKey)
                    setBalanceLine(holder.cvRELine, 0f, Math.toDegrees(angleValue).toFloat())
                    Log.v("angle", "${ Math.toDegrees(angleValue).toFloat()}")
                } else {
                    if (currentItem.partName == "왼쪽 측면 자세" || currentItem.partName == "오른쪽 측면 자세"  ) {
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