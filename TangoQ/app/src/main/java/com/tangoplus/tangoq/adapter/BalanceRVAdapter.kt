package com.tangoplus.tangoq.adapter

import android.annotation.SuppressLint
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.tangoplus.tangoq.R
import com.tangoplus.tangoq.databinding.RvBalanceItemBinding
import com.tangoplus.tangoq.fragment.MeasureAnalysisFragment

class BalanceRVAdapter(private val fragment: Fragment) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    inner class BalanceViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvBIName: TextView = view.findViewById(R.id.tvBIName)
        val ivBI: ImageView = view.findViewById(R.id.ivBI)
        val clBI : ConstraintLayout = view.findViewById(R.id.clBI)
        val tvBIExplain : TextView = view.findViewById(R.id.tvBIExplain)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = RvBalanceItemBinding.inflate(inflater, parent, false)
        return BalanceViewHolder(binding.root)
    }

    override fun getItemCount(): Int {
        return 5
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
//        val currentItem = stages[position]
        if (holder is BalanceViewHolder) {
            setItem(position, holder.ivBI, holder.tvBIName, holder.tvBIExplain)


//            var currentComment = ""
//            if (currentItem.size < 2) {
//                currentComment = "${currentItem[0]} ${transferDegree(degree[position])}"
//            } else if (degree[position].second > 3 || degree[position].second < -2) {
//                currentComment = "전체적인 균형이 잘 잡혀 있습니다."
//            } else {
//                currentComment = "${currentItem[0]}, ${currentItem[1]}${transferDegree(degree[position])}"
//            }
//            holder.tvBIPredict.text = currentComment

            holder.clBI.setOnClickListener{
                fragment.requireActivity().supportFragmentManager.beginTransaction().apply {
                    replace(R.id.flMain, MeasureAnalysisFragment.newInstance(position))
                    addToBackStack(null)
                    commit()
                }
            }
        }
    }

    private fun transferDegree(degree: Pair<Int, Int>): String {
        return when (degree.second) {
            3 ->  "는(은) 올바른 균형입니다."
            2 -> "에 미세한 불균형이 있습니다."
            1 -> "에 치우침이 있습니다."
            0 -> "에 약간의 불균형이 있습니다."
            -1 -> "에 치우침이 있습니다."
            -2 -> "이(가) 과도하게 치우쳐 있습니다."
            else -> ""
        }
    }

    private fun setColor(index: Int, tv1: TextView, iv: ImageView) {
        when (index) {
            2 -> {
                tv1.setTextColor(Color.parseColor("#FF5449"))
                iv.setImageDrawable(ContextCompat.getDrawable(fragment.requireContext(), R.drawable.icon_caution))
            }
            1 -> {
                tv1.setTextColor(Color.parseColor("#FF981D"))
                iv.setImageDrawable(ContextCompat.getDrawable(fragment.requireContext(), R.drawable.icon_warning))
            }
            0 -> {
                tv1.setTextColor(Color.parseColor("#AEAEAE"))
                iv.setImageDrawable(ContextCompat.getDrawable(fragment.requireContext(), R.drawable.icon_ball))

            }
        }
    }

    private fun setItem(index: Int, iv: ImageView, tvName: TextView, tvExplain: TextView) {
        when (index) {
            0 -> {
                iv.setImageResource(R.drawable.drawable_front)
                tvName.text = "정면 균형"
                tvExplain.text = "정면 선 자세와 팔꿈치 정렬 자세에 대해 체크할 수 있습니다. 몸의 균형과 손목과 팔꿈치의 각도를 확인하세요"
            }
            1 -> {
                iv.setImageResource(R.drawable.drawable_side)
                tvName.text = "측면 균형"
                tvExplain.text = "양 측면 자세를 비교해서 좌우 불균형을 확인할 수 있습니다. 몸의 중심을 기준으로 각 부위의 위치를 중점적으로 확인하세요"
            }
            2 -> {
                iv.setImageResource(R.drawable.drawable_back)
                tvName.text = "후면 균형"
                tvExplain.text = "후면 선 자세에 대해 체크할 수 있습니다. 정면 자세에서 알 수 없었던 다리, 발 뒷꿈치의 위치를 통해 서 있는 자세를 확인하세요"
            }
            3 -> {
                iv.setImageResource(R.drawable.drawable_sit_back)
                tvName.text = "앉은 후면"
                tvExplain.text = "의자에 앉은 자세를 체크할 수 있습니다. 후면 선 자세와 비교해서 골반 틀어짐, 어깨 쏠림 등을 체크해 보세요"
            }
            4 -> {
                iv.setImageResource(R.drawable.drawable_dynamic)
                tvName.text = "동적 균형"
                tvExplain.text = "오버헤드 스쿼트 자세를 통해 손끝, 골반, 무릎의 궤적에서 흔들림을 체크해 보세요"
            }
        }
    }
}