package com.tangoplus.tangoq.adapter

import android.annotation.SuppressLint
import android.util.Log
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.PopupWindow
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.tangoplus.tangoq.listener.OnPartCheckListener
import com.tangoplus.tangoq.R
import com.tangoplus.tangoq.data.MeasureVO
import com.tangoplus.tangoq.databinding.RvPainPartItemBinding
import com.tangoplus.tangoq.databinding.RvSelectPainPartItemBinding
import com.tangoplus.tangoq.dialog.PoseViewDialogFragment
import com.tangoplus.tangoq.fragment.ReportDiseaseFragment
import com.tangoplus.tangoq.fragment.ReportFragment
import java.lang.IllegalArgumentException

class PainPartRVAdpater(val fragment: Fragment, private var parts: MutableList<MeasureVO>, private var xmlname: String, private val onPartCheckListener: OnPartCheckListener) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private var popupWindow : PopupWindow?= null

    inner class ppViewHolder(view: View): RecyclerView.ViewHolder(view) {
        val tvPpName : TextView = view.findViewById(R.id.tvPpName)
        val ivPp : ImageView = view.findViewById(R.id.ivPp)
        val ibtnPpMore : ImageButton = view.findViewById(R.id.ibtnPpMore)
        val ivPpDone : ImageView = view.findViewById(R.id.ivPpDone)
        val ivPpUp : ImageView = view.findViewById(R.id.ivPpUp)
        val ivPpDown : ImageView = view.findViewById(R.id.ivPpDown)
        val tvPpScore : TextView = view.findViewById(R.id.tvPpScore)
    }

    inner class SelectPpViewHolder(view: View) :RecyclerView.ViewHolder(view) {
        val tvSPp : TextView = view.findViewById(R.id.tvSPp)
        val ivSPp : ImageView = view.findViewById(R.id.ivSPp)
        val cbSPp : CheckBox = view.findViewById(R.id.cbSPp)
        val ivSPpCheck : ImageView = view.findViewById(R.id.ivSPpCheck)
    }


    override fun getItemViewType(position: Int): Int {
        return when (xmlname) {
            "Pp" -> 0
            "selectPp" -> 1
            else -> throw IllegalArgumentException("invalid view type")
        }
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            0 -> {
                val binding = RvPainPartItemBinding.inflate(inflater, parent, false)
                ppViewHolder(binding.root)
            }
            1 -> {
                val binding = RvSelectPainPartItemBinding.inflate(inflater, parent, false)
                SelectPpViewHolder(binding.root)
            }
            else -> throw IllegalArgumentException("invalid view type binding")
        }
    }

    @SuppressLint("InflateParams")
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val currentItem = parts[position]

        if (holder is ppViewHolder) {
            holder.tvPpName.text = currentItem.partName
            holder.ibtnPpMore.setOnClickListener {
                onPartCheckListener.onPartCheck(currentItem)
            }
            val resourceId = holder.itemView.context.resources.getIdentifier(
                currentItem.drawableName, "drawable", holder.itemView.context.packageName
            )
            holder.ivPp.setImageResource(resourceId)

            holder.ibtnPpMore.setOnClickListener{ view ->
                if (popupWindow?.isShowing == true) {
                    popupWindow?.dismiss()
                    popupWindow =  null
                } else {
                    val inflater = LayoutInflater.from(view?.context)
                    val popupView = inflater.inflate(R.layout.pw_pain_part_item, null)
                    val width = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 186f, view.context.resources.displayMetrics).toInt()
                    val height = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,  162f, view.context.resources.displayMetrics).toInt()

                    popupWindow = PopupWindow(popupView, width, height)
                    popupWindow?.showAsDropDown(view)
                    popupView.findViewById<TextView>(R.id.tvPPP1).setOnClickListener {
                        fragment.requireActivity().supportFragmentManager.beginTransaction().apply {
                            setCustomAnimations(R.anim.slide_in_left, R.anim.slide_in_right)
                            add(R.id.flMain, ReportFragment())
                            commit()
                        }
                        popupWindow!!.dismiss()
                    }
                    popupView.findViewById<TextView>(R.id.tvPPP2).setOnClickListener {
                        val dialog = PoseViewDialogFragment.newInstance(currentItem.drawableName) // TODO drawableName이 아니라 실제 파일 String에 대한 값을 MeasureVO에 추가해야할수도
                        dialog.show(fragment.requireActivity().supportFragmentManager, "PoseViewDialogFragment")
                        popupWindow!!.dismiss()
                    }
                    popupView.findViewById<TextView>(R.id.tvPPP3).setOnClickListener {

                    }
                    popupWindow!!.isOutsideTouchable = true
                    popupWindow!!.isFocusable = true
                    popupView.findViewById<ImageButton>(R.id.ibtnPPPExit).setOnClickListener { popupWindow!!.dismiss() }
                }

            }
            // ------! 점수 상승 icon control 시작 !------
//            holder.ivPpDone.visibility = View.GONE
//            holder.ivPpDown.visibility = View.GONE

            if (currentItem.anglesNDistances == null) {
                holder.tvPpScore.text = "미설정"
                holder.ivPpUp.visibility = View.GONE
                holder.ivPpDown.visibility = View.GONE
            }
            // ------! 점수 상승 icon control 끝 !------
//            val resourceId = holder.itemView.context.resources.getIdentifier(
//                currentItem.first, "drawable", holder.itemView.context.packageName
//            )
//            holder.ivPp.setImageResource(resourceId)

        } else if (holder is SelectPpViewHolder) {

            holder.cbSPp.setOnCheckedChangeListener { _, isChecked ->
                currentItem.select = isChecked
                onPartCheckListener.onPartCheck(currentItem)
                when (isChecked) {
                    true -> holder.ivSPpCheck.setImageResource(R.drawable.icon_checkbox_enabled)
                    false -> holder.ivSPpCheck.setImageResource(R.drawable.icon_checkbox_disabled)
                }
            }
            // ------! 값 보존 !------
            when (currentItem.select) {
                true -> {
                    holder.ivSPpCheck.setImageResource(R.drawable.icon_checkbox_enabled)
                    holder.cbSPp.isChecked = true
                }
                false -> {
                    holder.ivSPpCheck.setImageResource(R.drawable.icon_checkbox_disabled)
                    holder.cbSPp.isChecked = false
                }
            }

            // ------! 부위 이름, drawable !------
            holder.tvSPp.text = currentItem.partName
            val resourceId = holder.itemView.context.resources.getIdentifier(
                currentItem.drawableName, "drawable", holder.itemView.context.packageName
            )
            holder.ivSPp.setImageResource(resourceId)
        }
    }
    override fun getItemCount(): Int {
        return parts.size
    }
}