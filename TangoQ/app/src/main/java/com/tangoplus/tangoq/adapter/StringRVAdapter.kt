package com.tangoplus.tangoq.adapter

import android.annotation.SuppressLint
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.recyclerview.widget.RecyclerView
import com.tangoplus.tangoq.R
import com.tangoplus.tangoq.viewmodel.MeasureViewModel
import com.tangoplus.tangoq.viewmodel.ProgressViewModel
import com.tangoplus.tangoq.databinding.RvMuscleItemBinding
import com.tangoplus.tangoq.databinding.RvWeeklyItemBinding
import com.tangoplus.tangoq.listener.OnDisconnectListener

class StringRVAdapter(private val fragment: Fragment,
                      private val stringList: MutableList<String>?,
                      private val xmlName: String,
                      private val vm: ViewModel) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    var onDisconnectListener: OnDisconnectListener? = null
    inner class MuscleViewHolder(view : View) : RecyclerView.ViewHolder(view) {
        val ivMI : ImageView = view.findViewById(R.id.ivMI)
        val tvMIName : TextView = view.findViewById(R.id.tvMIName)
    }

    inner class CbViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val cbWI : CheckBox = view.findViewById(R.id.cbWI)
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

            else -> throw IllegalArgumentException("Invaild View Type")
        }
    }

    override fun getItemViewType(position: Int): Int {
        return when (xmlName) {
            "muscle" -> 0

            "measure" -> 1
            "week" -> 1
//            "connect" -> 3
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
                        listOf("대원", "큰원", "극하") to "dae_won",

                        listOf("작은가슴", "소흉",  "작은원", "큰가슴", "소원") to "dae_hyung",
                        listOf("앞어깨세모", "삼각 전면") to "sam_gak",
                        listOf("견갑거", "어깨올림", "등세모", "위등세모", "머리긴", "머리곧은", "긴머리", "두장", "경장") to "seung_mo_1",
                        listOf("아래등세모", "마름", "능형") to "seung_mo_2",
                        listOf("승모") to "seung_mo_3",

                        listOf("이두", "두 갈래 포함하는 위팔 육군", "윗팔 두갈래", "팔꿈치 폄") to "i_du",
                        listOf("삼두", "위팔 세갈래") to "sam_du",
                        listOf("회내", "옆청", "긴엄지굽힘", "요측수근굴", "척측수근굴") to "hoe_nae",
                        listOf("전완", "회외", "손뒤침", "굽힘", "노쪽손목굽힘", "자쪽손목굽힘", "상지 폄 근육", "엎침", "짧은노쪽손목폄", "긴노쪽손목폄", "손가락근육", "긴손바닥") to "jeon_wan",
                        listOf("수근 신근") to "su_geon_sin",
                        listOf("대흉" ) to "dae_hyung",
                        listOf("전거") to "jeon_geo",
                        listOf("광배", "넓은등") to "gwang_bae",
                        listOf("가시", "가시위", "가시아래") to "sa_gak",
                        listOf("복근","복부", "복직", "배곧은") to "bok",
                        listOf("하복부", "궁둥구멍", "이상") to "ha_bok",
                        listOf("앞톱니") to "jeon_geo",
                        listOf( "척추기립", "척추세움", "가장긴", "허리네모", "허리 네모", "척주세움", "엉덩갈비") to "cheock_chu_gi_rip",
                        listOf("요방형") to "yo_bang_hyeong",
                        listOf("뭇갈래", "가장긴 등의 척추 근육", "다열") to "mut_gal_le",
                        listOf("외복사", "배바깥빗") to "oe_bok_sa",
                        listOf("장요") to "jang_yo",

                        listOf("두덩정강") to "du_dung_jung_gang",
                        listOf("둔", "중간볼기", "엉덩관절 가쪽 돌림", "큰볼기", "엉덩허리", "허리 굽힘") to "dun",
                        listOf("대퇴사두", "하지육", "허벅지", "내전") to "dae_toe_sa_du",
                        listOf("대퇴이두", "햄스트링", "뒤넙다리") to "dae_toe_i_du",
                        listOf("외측광") to "oe_cheug_gwang",
                        listOf("내측광") to "nae_cheug_gwang",

                        listOf("앞정강", "앞정강(전경골)", "긴발가락굽힘", "장지굴") to "jung_gang_i",
                        listOf( "넙다리네갈래", "안쪽넓은", "가쪽넓은") to "nae_cheug_gwang",
                        listOf("넙다리 빗", "넙다리빗" ) to "nub_da_ri_bit",
                        listOf( "가자미", "넙치") to "ga_ja_mi",
                        listOf("뒤정강", "후경골", "뒤정강(후경골)", "종아리", "비골", "긴종아리", "긴 종아리",  "장딴지", "가쪽장딴지(비복)", "가쪽장딴지") to "jong_a_ri",
                        listOf("발목 굽힘", "긴발가락굽힘(장지굴)", "긴엄지굽힘(장무지굴)") to "bal_ga_rak_gup_him"
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
                    holder.cbWI.text = currentItem
                    val isInitiallyChecked = position == (vm as MeasureViewModel).currentMeasureDate
                    holder.cbWI.isChecked = isInitiallyChecked
                    updateCheckboxTextColor(holder.cbWI, isInitiallyChecked)

                    vm.selectMeasureDate.observe(fragment.viewLifecycleOwner) { selectMeasureDate ->
                        val isChecked = currentItem == selectMeasureDate
                        holder.cbWI.isChecked = isChecked
                        updateCheckboxTextColor(holder.cbWI, isChecked)
                    }

                    holder.cbWI.setOnClickListener {
                        vm.selectMeasureDate.value = currentItem
                        Log.v("selectedDate", "selectMeasureDate: ${vm.selectMeasureDate.value}, currentItem: ${vm.selectedMeasureDate.value}")
                    }
                } else {
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
                    holder.cbWI.setOnClickListener {
                        vm.selectWeek.value = position
                        Log.v("selectDate", "selectWeek: ${vm.selectWeek.value}")
                    }
                }
            }
//            is ConnectViewHolder -> {
//                holder.tvCIDate.text = "등록일자: ${(vm as UserViewModel).connectedCenters[position].second}"
//                holder.tvCIName.text = currentItem
//                holder.btnCI.setOnClickListener {
//                    MaterialAlertDialogBuilder(fragment.requireContext(), R.style.ThemeOverlay_App_MaterialAlertDialog).apply {
//                        setTitle("연결 해제")
//                        setMessage("${currentItem}와(과) 연결을 해제하시겠습니까?")
//                        setPositiveButton("예") { _, _ ->
//                            if (currentItem != null) {
//                                onDisconnectListener?.onDisconnect(currentItem)
//                            }
//                        }
//                        setNegativeButton("아니오") { dialog, _ ->
//                            dialog.dismiss()
//                        }
//                    }.show()
//                }
//            }
        }
    }

    override fun getItemCount(): Int {
        return stringList?.size ?: 0
    }

    private fun updateCheckboxTextColor(checkbox: CheckBox, isChecked: Boolean = checkbox.isChecked) {
        val colorResId = if (isChecked) R.color.mainColor else R.color.subColor800
        checkbox.setTextColor(ContextCompat.getColor(fragment.requireContext(), colorResId))
    }

    @SuppressLint("DiscouragedApi")
    private fun setIV(name: String, imageView: ImageView) {
        imageView.setImageResource(fragment.resources.getIdentifier("drawable_muscle_${name}", "drawable", fragment.requireActivity().packageName))
    }
}