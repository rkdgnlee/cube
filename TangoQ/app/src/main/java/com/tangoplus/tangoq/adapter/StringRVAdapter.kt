package com.tangoplus.tangoq.adapter

import android.annotation.SuppressLint
import android.graphics.Color
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.collection.intListOf
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet.Constraint
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.recyclerview.widget.RecyclerView
import com.tangoplus.tangoq.R
import com.tangoplus.tangoq.viewmodel.MeasureViewModel
import com.tangoplus.tangoq.viewmodel.ProgressViewModel
import com.tangoplus.tangoq.databinding.RvMuscleItemBinding
import com.tangoplus.tangoq.databinding.RvPartBSItemBinding
import com.tangoplus.tangoq.databinding.RvPartItemBinding
import com.tangoplus.tangoq.databinding.RvWeeklyItemBinding
import com.tangoplus.tangoq.fragment.ExtendedFunctions.setOnSingleClickListener
import com.tangoplus.tangoq.listener.OnCategoryClickListener
import com.tangoplus.tangoq.listener.OnDisconnectListener
import com.tangoplus.tangoq.viewmodel.AnalysisViewModel
import com.tangoplus.tangoq.vo.DateDisplay

class StringRVAdapter(private val fragment: Fragment,
                      private val stringList: MutableList<String>?,
                      private val nameList : MutableList<String>?,
                      private val xmlName: String,
                      private val vm: ViewModel) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {



    inner class MuscleViewHolder(view : View) : RecyclerView.ViewHolder(view) {
        val ivMI : ImageView = view.findViewById(R.id.ivMI)
        val tvMIName : TextView = view.findViewById(R.id.tvMIName)
    }

    inner class CbViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val cbWI : CheckBox = view.findViewById(R.id.cbWI)
    }
    inner class MainPartBSViewHolder(view:View) : RecyclerView.ViewHolder(view) {
        val cbPBSI : CheckBox = view.findViewById(R.id.cbPBSI)
        val cvPBSI : CardView = view.findViewById(R.id.cvPBSI)
        val ivPBSI : ImageView = view.findViewById(R.id.ivPBSI)

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            0 -> {
                val binding = RvMuscleItemBinding.inflate(inflater, parent, false)
                MuscleViewHolder(binding.root)
            }

            1 -> {
                val binding = RvWeeklyItemBinding.inflate(inflater, parent, false)
                CbViewHolder(binding.root)
            }
            2 -> {
                val binding = RvPartBSItemBinding.inflate(inflater, parent, false)
                MainPartBSViewHolder(binding.root)
            }
            else -> throw IllegalArgumentException("Invaild View Type")
        }
    }

    override fun getItemViewType(position: Int): Int {
        return when (xmlName) {
            "muscle" -> 0
            "measure", "week" -> 1
            "seq" -> 2
            else -> throw IllegalArgumentException("Invalid View Type")
        }
    }
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val currentItem = stringList?.get(position)
        when (holder) {
            is MuscleViewHolder -> {
                if (currentItem != null) {
                    holder.tvMIName.text = currentItem
                    val keywordToImageMap = mapOf(
                        listOf("사각", "목갈비", "빗장") to "sa_gak",
                        listOf("목빗", "흉쇄유돌", "목긴", "씹기", "두힘살", "긴목", "이복") to "mok_bit",
                        listOf("삼각", "회전근개") to "sam_gak",
                        listOf("후면 삼각", "뒤어깨세모", "어깨밑") to "hu_myeon_sam_gak",
                        listOf("대원", "큰원", "극하", "작은원", "소원") to "dae_won",

                        listOf("작은가슴", "소흉", "큰가슴") to "dae_hyung",
                        listOf("앞어깨세모", "삼각 전면") to "sam_gak",
                        listOf("견갑거", "어깨올림", "등세모", "위등세모", "머리긴", "머리곧은", "긴머리", "두장", "경장") to "seung_mo_1",
                        listOf("아래등세모", "마름", "능형") to "seung_mo_2",
                        listOf("승모") to "seung_mo_3",

                        listOf("이두", "두 갈래 포함하는 위팔 육군", "윗팔 두갈래", "팔꿈치 폄") to "i_du",
                        listOf("삼두", "위팔 세갈래") to "sam_du",
                        listOf("회내", "옆청", "긴엄지굽힘", "요측수근굴", "척측수근굴", "손가락폄근", "굽힘근") to "hoe_nae",
                        listOf("전완", "회외", "손뒤침", "노쪽손목굽힘", "자쪽손목굽힘", "상지 폄 근육", "엎침", "짧은노쪽손목폄", "긴노쪽손목폄", "손가락근육", "긴손바닥", "손목근육") to "jeon_wan",
                        listOf("수근 신근") to "su_geon_sin",
                        listOf("대흉" ) to "dae_hyung",
                        listOf("전거") to "jeon_geo",
                        listOf("광배", "넓은등") to "gwang_bae",
                        listOf("가시", "가시위", "가시아래") to "sa_gak",
                        listOf("복근","복부", "복직", "배곧은") to "bok",
                        listOf("하복부", ) to "ha_bok",
                        listOf("앞톱니") to "jeon_geo",
                        listOf( "척추기립", "척추세움", "가장긴", "허리네모", "허리 네모", "척주세움", "엉덩갈비") to "cheock_chu_gi_rip",
                        listOf("요방형") to "yo_bang_hyeong",
                        listOf("뭇갈래", "가장긴 등의 척추 근육", "다열") to "mut_gal_le",
                        listOf("외복사", "배바깥빗") to "oe_bok_sa",
                        listOf("장요", "대퇴근막장근", "작은볼기근") to "jang_yo",

                        listOf("두덩정강", "박근") to "du_dung_jung_gang",
                        listOf("둔", "중간볼기", "엉덩관절 가쪽 돌림", "큰볼기", "엉덩허리", "허리 굽힘", "궁둥구멍", "이상") to "dun",
                        listOf("대퇴사두", "하지육", "허벅지", "내전", "짧은 엉덩관절 모음근") to "dae_toe_sa_du",
                        listOf("대퇴이두", "햄스트링", "뒤넙다리") to "dae_toe_i_du",
                        listOf("외측광", "엉덩정강근막띠", "장경인대: IT band", "넙다리근막긴장근", "대퇴막장근", "가쪽넓은") to "oe_cheug_gwang",
                        listOf("내측광", "짧은엉덩관절") to "nae_cheug_gwang",

                        listOf("앞정강", "앞정강(전경골)", "정강", "긴발가락굽힘", "장지굴") to "jung_gang_i",
                        listOf( "넙다리네갈래", "안쪽넓은") to "nae_cheug_gwang",
                        listOf("넙다리 빗", "넙다리빗" ) to "nub_da_ri_bit",
                        listOf( "가자미", "넙치") to "ga_ja_mi",
                        listOf("뒤정강", "후경골", "뒤정강(후경골)", "종아리", "비골", "긴종아리", "긴 종아리",  "장딴지", "가쪽장딴지(비복)", "가쪽장딴지", "비복근", "하지근육") to "jong_a_ri",
                        listOf("발목 굽힘", "긴발가락굽힘(장지굴)", "긴엄지굽힘(장무지굴)", "장무지굴근", "발목 굽힘근", "발목 폄근") to "bal_ga_rak_gup_him"
                    )
                    val matchedImageKey = keywordToImageMap.entries.find { entry ->
                        entry.key.any { keyword -> currentItem.contains(keyword) }
                    }?.value
                    matchedImageKey?.let { imageKey ->
                        setIV(imageKey, holder.ivMI)
                    }
                }
            }

            is CbViewHolder -> {
                if (xmlName == "measure") {
//                    val currentItem2 = nameList?.get(position)
                    holder.cbWI.text = "${currentItem?.substring(0, 10)}" //${currentItem2}
                    val isInitiallyChecked = position == (vm as MeasureViewModel).currentMeasureDate
                    holder.cbWI.isChecked = isInitiallyChecked
                    updateCheckboxTextColor(holder.cbWI, isInitiallyChecked)
                    val currentDate =
                        currentItem?.let { DateDisplay(it, currentItem.substring(0, 11)) }
                    vm.selectMeasureDate.observe(fragment.viewLifecycleOwner) { selectMeasureDate ->
                        val isChecked = currentDate == selectMeasureDate
                        holder.cbWI.isChecked = isChecked
                        updateCheckboxTextColor(holder.cbWI, isChecked)
                    }

                    holder.cbWI.setOnSingleClickListener {
                        vm.selectMeasureDate.value = currentDate
//                        Log.v("selectedDate", "selectMeasureDate: ${vm.selectMeasureDate.value}, currentItem: ${vm.selectedMeasureDate.value}")
                    }


                }  else {
                    val isInitiallyChecked = position == (vm as ProgressViewModel).selectedWeek.value
                    holder.cbWI.apply {
                        isChecked = isInitiallyChecked
                        text = currentItem
                        updateCheckboxTextColor(this, isInitiallyChecked)
                        setOnCheckedChangeListener { _, isChecked ->
                            updateCheckboxTextColor(this, isChecked)
                        }
                    }
                    vm.selectWeek.observe(fragment.viewLifecycleOwner) { selectWeek ->

                        val isChecked = position  == selectWeek
                        holder.cbWI.isChecked = isChecked
                        updateCheckboxTextColor(holder.cbWI, isChecked)
                    }
                    holder.cbWI.setOnSingleClickListener {
                        vm.selectWeek.value = position
//                        Log.v("selectDate", "selectWeek: ${vm.selectWeek.value}")
                    }
                }
            }
            is MainPartBSViewHolder -> {

                holder.cbPBSI.text = if (currentItem != "목관절") "$currentItem 관절" else currentItem
                holder.ivPBSI.setImageDrawable(ContextCompat.getDrawable(fragment.requireContext(), getImageResourceId(currentItem.toString())))

                val isInitiallyChecked = position == (vm as AnalysisViewModel).currentParts?.indexOf(vm.currentPart.value)
                holder.cbPBSI.isChecked = isInitiallyChecked
                updateCheckbox(holder, holder.cbPBSI, isInitiallyChecked)

                vm.selectPart.observe(fragment.viewLifecycleOwner) { part ->
                    val isChecked = currentItem == part
                    holder.cbPBSI.isChecked = isChecked
                    updateCheckbox(holder ,holder.cbPBSI, isChecked)
                }

                holder.cbPBSI.setOnSingleClickListener {
                    if (currentItem != null) {
                        vm.selectPart.value = currentItem
                        Log.v("selectedDate", "현재: ${vm.currentPart.value}, 선택값: ${vm.selectPart}")
                    }
                }
            }


        }
    }

    override fun getItemCount(): Int {
        return stringList?.size ?: 0
    }

    private fun updateCheckboxTextColor(checkbox: CheckBox, isChecked: Boolean = checkbox.isChecked) {
        val colorResId = if (isChecked) R.color.mainColor else R.color.subColor800
        checkbox.setTextColor(ContextCompat.getColor(fragment.requireContext(), colorResId))
    }

    private fun updateCheckbox(holder: MainPartBSViewHolder, checkbox: CheckBox, isChecked: Boolean = checkbox.isChecked) {
        val cvBackgroundColor = if (isChecked) R.color.secondaryColor else R.color.subColor400
        val cbBackgroundClor = if (isChecked) R.color.secondContainerColor else R.color.white
        checkbox.setTextColor(ContextCompat.getColor(fragment.requireContext(), cvBackgroundColor))
        checkbox.setBackgroundColor(ContextCompat.getColor(fragment.requireContext(), cbBackgroundClor))
        holder.cvPBSI.setCardBackgroundColor(ContextCompat.getColor(fragment.requireContext(), cvBackgroundColor))
    }


    private fun getImageResourceId(currentItem: String): Int {
        return when (currentItem) {
            "목관절" -> R.drawable.icon_part1
            "우측 어깨", "좌측 어깨" -> R.drawable.icon_part2
            "우측 팔꿉", "좌측 팔꿉" -> R.drawable.icon_part3
            "우측 손목", "좌측 손목" -> R.drawable.icon_part4
            "우측 골반", "좌측 골반" -> R.drawable.icon_part5
            "우측 무릎", "좌측 무릎" -> R.drawable.icon_part6
            "우측 발목", "좌측 발목" -> R.drawable.icon_part7
            else -> R.drawable.icon_part1
        }
    }
    @SuppressLint("DiscouragedApi")
    private fun setIV(name: String, imageView: ImageView) {
        imageView.setImageResource(fragment.resources.getIdentifier("drawable_muscle_${name}", "drawable", fragment.requireActivity().packageName))
    }
}