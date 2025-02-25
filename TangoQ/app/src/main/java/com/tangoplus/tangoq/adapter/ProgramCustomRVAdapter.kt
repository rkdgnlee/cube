package com.tangoplus.tangoq.adapter

import android.text.Spannable
import android.text.SpannableString
import android.text.style.RelativeSizeSpan
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.skydoves.progressview.ProgressView
import com.tangoplus.tangoq.R
import com.tangoplus.tangoq.databinding.RvProgramSeqItemBinding
import com.tangoplus.tangoq.listener.OnCustomCategoryClickListener
import com.tangoplus.tangoq.mediapipe.MathHelpers.isTablet

class ProgramCustomRVAdapter(private val fragment: Fragment,
                             private val seq: Triple<Int, Int, Int>,
                             private val week: Pair<Int, Int>,
                             private val hpvProgresses: List<Float>?,
                             private val onCustomCategoryClickListener: OnCustomCategoryClickListener) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    // frequency 는 총 들어가는 회차, hpvProgresses는 같이 들어가는 시청 기록, sequencdState는 현재 회차와 선택한회차
    inner class CustomViewHolder(view: View) : RecyclerView.ViewHolder(view){
        val tvPSIName : TextView = view.findViewById(R.id.tvPSIName)
        val hpvPSI : ProgressView = view.findViewById(R.id.hpvPSI)
        val ivPSICheck : ImageView = view.findViewById(R.id.ivPSICheck)
        val cvPSI: CardView = view.findViewById(R.id.cvPSI)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = RvProgramSeqItemBinding.inflate(inflater, parent, false)
        return CustomViewHolder(binding.root)
    }

    override fun getItemCount(): Int {
        return seq.first
    }
    /* seq.first == 총 회차
    *  seq.second == 현재 회차
    *  seq.third == 선택한 회차
    *  week.first == 현재 주차
    *  week.second == 선택된 주차
    * */
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {

        // episode는 첫 번째가 선택된 회차, 두 번째가 진행되는 회차임.
        if (holder is CustomViewHolder) {
            val spannableDays = SpannableString.valueOf("Day ${position+1}")
//            spannableDays.setSpan(RelativeSizeSpan(if (isTablet(fragment.requireContext())) 1.6f else 1.4f), 0, spannableDays.indexOf("\n"), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            holder.tvPSIName.text = spannableDays

            // seqState.first == 현재 회차 seqState.second == 선택한 회차
            holder.ivPSICheck.visibility = View.INVISIBLE
            holder.cvPSI.visibility = View.INVISIBLE
            holder.hpvPSI.autoAnimate = false
            if (position > seq.second) {
                holder.tvPSIName.isEnabled = false
            }

            if (!hpvProgresses.isNullOrEmpty()) {
                val currentHpvProgresses = hpvProgresses[position]
                Log.v("progress", "$hpvProgresses, $position, $currentHpvProgresses")
                if (week.second <= week.first) {
                    // 선택한 회차 + 현재 회차
                    if (position == seq.third && seq.second == seq.third) {
                        setHpv(holder, true, currentHpvProgresses)
                        setTextView(holder.tvPSIName, R.color.secondaryColor, R.color.whiteText)
                        if (holder.hpvPSI.progress > 90f) {
                            setCompleted(holder, true)
                        }
                        // 선택한 회차 + 이전 회차
                    } else if (position == seq.third && position < seq.second) {
                        setHpv(holder, true, currentHpvProgresses)
                        setTextView(holder.tvPSIName, R.color.secondaryColor, R.color.whiteText)

                        if (holder.hpvPSI.progress > 90f) {
                            setCompleted(holder, true)
                        }
                        // 선택하지 않은 회차 + 이전 회차
                    } else if (position < seq.second) {
                        setHpv(holder, false, currentHpvProgresses)
                        setTextView(holder.tvPSIName, R.color.subColor400, R.color.whiteText)

                        // 다 완료했을 경우
                        if (holder.hpvPSI.progress > 90f) {
                            setCompleted(holder, false)
                        }
                        // 선택하지 않았을 때 현재 회차
                    } else if (position == seq.second) {
                        setHpv(holder, false, currentHpvProgresses)
                        setTextView(holder.tvPSIName, R.color.secondContainerColor, R.color.thirdColor)
                    }  else {
                        setHpv(holder, false, 0f)
                        holder.ivPSICheck.visibility = View.INVISIBLE
                        setTextView(holder.tvPSIName, R.color.subColor100, R.color.subColor400)
                    }
                } else {
                    setTextView(holder.tvPSIName, R.color.subColor100, R.color.subColor400)
                    setHpv(holder, false, 0f)
                    holder.ivPSICheck.visibility = View.INVISIBLE
                }
            } else {
                setHpv(holder, false, 0f)
                setTextView(holder.tvPSIName, R.color.subColor100, R.color.subColor400)
                holder.ivPSICheck.visibility = View.INVISIBLE
            }


             // 회차별 상태 업데이트
            holder.tvPSIName.setOnClickListener {
                onCustomCategoryClickListener.customCategoryClick(position)
            }
        }
    }
    private fun setCheckBadge(holder: CustomViewHolder, isSelect: Boolean) {
        when (isSelect) {
            true -> holder.ivPSICheck.setImageDrawable(ContextCompat.getDrawable(fragment.requireContext(), R.drawable.icon_check_bckgnd_white_secondary))
            false -> holder.ivPSICheck.setImageDrawable(ContextCompat.getDrawable(fragment.requireContext(), R.drawable.icon_check_bckgnd_white))
        }
    }

    private fun setHpv(holder: CustomViewHolder, isSelect : Boolean, progress : Float) {
        if (isSelect) {
            holder.hpvPSI.colorBackground = fragment.resources.getColor(R.color.secondContainerColor, null)
            holder.hpvPSI.progress = progress.toFloat()
        } else {
            holder.hpvPSI.colorBackground = fragment.resources.getColor(R.color.subColor100, null)

            holder.hpvPSI.progress = progress.toFloat()
        }
    }

    private fun setCompleted(holder: CustomViewHolder, isSelect: Boolean) {
        holder.cvPSI.visibility = View.VISIBLE
        holder.tvPSIName.text = "Complete"
        holder.tvPSIName.textSize = 16f
        if (isSelect) {
            setCheckBadge(holder, true)
            holder.ivPSICheck.visibility = View.VISIBLE
        } else {
            setCheckBadge(holder, false)
            holder.ivPSICheck.visibility = View.VISIBLE
            holder.cvPSI.visibility = View.VISIBLE
        }
    }
    private fun setTextView(tv: TextView, color1: Int, color2: Int) {
        tv.background = ContextCompat.getDrawable(fragment.requireContext(), R.drawable.effect_ibtn_20dp)
        tv.backgroundTintList = ContextCompat.getColorStateList(fragment.requireContext(), color1)
        tv.setTextColor(ContextCompat.getColor(fragment.requireContext(), color2))
    }
}