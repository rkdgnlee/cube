package com.tangoplus.tangoq.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.tangoplus.tangoq.R
import com.tangoplus.tangoq.databinding.RvMuscleItemBinding
import com.tangoplus.tangoq.databinding.RvPartItemBinding

class StringRVAdapter(private val fragment: Fragment, private val stringList: MutableList<Pair<String, Int>>?, private val xmlName: String) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    inner class muscleViewHolder(view : View) : RecyclerView.ViewHolder(view) {
        val ivMI : ImageView = view.findViewById(R.id.ivMI)
        val tvMIName : TextView = view.findViewById(R.id.tvMIName)
    }

    inner class partViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvPI : TextView = view.findViewById(R.id.tvPI)
        val cvPI : CardView = view.findViewById(R.id.cvPI)
        val ivPI : ImageView = view.findViewById(R.id.ivPI)
        val clPI : ConstraintLayout = view.findViewById(R.id.clPI)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            0 -> {
                val binding = RvMuscleItemBinding.inflate(inflater, parent, false)
                muscleViewHolder(binding.root)
            }
            1 -> {
                val binding = RvPartItemBinding.inflate(inflater, parent, false)
                partViewHolder(binding.root)
            }
            else -> throw IllegalArgumentException("Invaild View Type")
        }
    }

    override fun getItemViewType(position: Int): Int {
        return when (xmlName) {
            "muscle" -> 0
            "part" -> 1
            else -> throw IllegalArgumentException("Invalid View Type")
        }
    }
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val currentItem = stringList?.get(position)
        when (holder) {
            is muscleViewHolder -> {
                if (currentItem != null) {
                    holder.tvMIName.text = currentItem.first
                    when (currentItem.first) {
                        // 목
                        "목빗근", "흉쇄유돌근" -> setIV("mok_bit", holder.ivMI)
                        "사각근", "목갈비근" -> setIV("sa_gak", holder.ivMI)
                        "머리긴근", "머리곧은근" -> setIV("seung_mo_1", holder.ivMI)
                        "목긴근" -> setIV("mok_bit", holder.ivMI)
                        "씹기근" -> setIV("mok_bit", holder.ivMI)
                        "두힘살근" -> setIV("mok_bit", holder.ivMI)
                        "긴목근" -> setIV("mok_bit", holder.ivMI)

                        // 어깨
                        "삼각근" -> setIV("sam_gak", holder.ivMI)
                        "후면 삼각근" -> setIV("hu_myeon_sam_gak", holder.ivMI)
                        "뒤어깨세모근"-> setIV("hu_myeon_sam_gak", holder.ivMI)
                        "승모근", "승모"-> setIV("seung_mo_3", holder.ivMI)
                        "대원근", "큰원근" -> setIV("dae_won", holder.ivMI)
                        "견갑거근", "어깨올림근", "등세모근" -> setIV("seung_mo_1", holder.ivMI)
                        "작은가슴근", "소흉근" -> setIV("dae_hyung", holder.ivMI)
                        "앞어깨세모근", "삼각근 전면" -> setIV("sam_gak", holder.ivMI)
                        "어깨밑근" -> setIV("dae_hyung", holder.ivMI)
                        "위등세모근" -> setIV("seung_mo_1", holder.ivMI)
                        "아래등세모근" -> setIV("seung_mo_2", holder.ivMI)

                        "회전근개" -> setIV("sam_gak", holder.ivMI)
                        "빗장" -> setIV("sa_gak", holder.ivMI)
                        "작은원근" -> setIV("dae_hyung", holder.ivMI)

                        // 상완
                        "이두근", "이두" -> setIV("i_du", holder.ivMI)
                        "삼두근", "삼두" -> setIV("sam_du", holder.ivMI)
                        "위팔 세갈래근" -> setIV("sam_du", holder.ivMI)
                        "두 갈래근 포함하는 위팔 근육군", "윗팔 두갈래근", "팔꿈치 폄근" -> setIV("i_du", holder.ivMI)

                        // 하완
                        "회내근", "옆청근" -> setIV("hoe_nae", holder.ivMI)
                        "전완근", "전완" -> setIV("jeon_wan", holder.ivMI)
                        "회외근", "손뒤침근" -> setIV("jeon_wan", holder.ivMI)
                        "긴엄지굽힘근" -> setIV("hoe_nae", holder.ivMI)
                        "굽힘근" -> setIV("jeon_wan", holder.ivMI)
                        "노쪽손목굽힘근", "자쪽손목굽힘근", "상지 폄 근육" -> setIV("jeon_wan", holder.ivMI)
                        "엎침근" -> setIV("jeon_wan", holder.ivMI)
                        "짧은노쪽손목폄근" -> setIV("jeon_wan", holder.ivMI)
                        "긴노쪽손목폄근" -> setIV("jeon_wan", holder.ivMI)
                        "손가락근육" -> setIV("jeon_wan", holder.ivMI)
                        "긴손바닥근" -> setIV("jeon_wan", holder.ivMI)

                        // 가슴
                        "대흉근" -> setIV("dae_hyung", holder.ivMI)
                        "전거근" -> setIV("jeon_geo", holder.ivMI)

                        // 등
                        "광배근", "넓은등근" -> setIV("gwang_bae", holder.ivMI)
                        "가시근" -> setIV("sa_gak", holder.ivMI)
                        "가시위근" -> setIV("sa_gak", holder.ivMI)
                        "가시아래근" -> setIV("sa_gak", holder.ivMI)
                        "마름근" -> setIV("seung_mo_2", holder.ivMI)

                        // 복부
                        "복근" -> setIV("bok", holder.ivMI)
                        "하복부" -> setIV("ha_bok", holder.ivMI)
                        "복직근" -> setIV("bok", holder.ivMI)
                        "앞톱니근" -> setIV("jeon_geo", holder.ivMI)
                        "배곧은근" -> setIV("bok", holder.ivMI)
                        // 허리
                        "척추기립근", "척추세움근", "가장긴근" -> setIV("cheock_chu_gi_rip", holder.ivMI)
                        "요방형근", "허리 네모근" -> setIV("yo_bang_hyeong", holder.ivMI)

                        // 고관절 & 엉덩이
                        "둔근" -> setIV("dun", holder.ivMI)
                        "봉공근", "넙다리빗근" -> setIV("dae_toe_sa_du", holder.ivMI)
                        "엉덩갈비근" -> setIV("cheock_chu_gi_rip", holder.ivMI)
                        "큰볼기근" -> setIV("dun", holder.ivMI)
                        "엉덩정강근막띠", "장경인대" -> setIV("dae_toe_sa_du", holder.ivMI)
                        "궁등구멍근", "이상근" -> setIV("ha_bok", holder.ivMI)
                        "엉덩허리근"-> setIV("dun", holder.ivMI)

                        // 대퇴
                        "대퇴사두근" -> setIV("dae_toe_sa_du", holder.ivMI)
                        "대퇴이두", "햄스트링" -> setIV("dae_toe_i_du", holder.ivMI)
                        "내전근" -> setIV("dae_toe_sa_du", holder.ivMI)
                        "넙다리네갈래근" -> setIV("dae_toe_sa_du", holder.ivMI)
                        "뒤넙다리근" -> setIV("dae_toe_i_du", holder.ivMI)

                        // 하퇴
                        "장딴지근" -> setIV("dae_toe_i_du", holder.ivMI)
                        "가자미근", "넙치근" -> setIV("dae_toe_i_du", holder.ivMI)
                        "종아리근", "비골근" -> setIV("dae_toe_i_du", holder.ivMI)
                        "뒤정강근", "후경골근" -> setIV("dae_toe_i_du", holder.ivMI)
                        "장지굴근" -> setIV("dae_toe_i_du", holder.ivMI)
                        "앞정강근" -> setIV("dae_toe_i_du", holder.ivMI)

                        //
                        "발목 굽힘근" -> setIV("dae_toe_i_du", holder.ivMI)
                        "긴발가락굽힘근(장지굴근)" -> setIV("dae_toe_i_du", holder.ivMI)
                        "긴엄지굽힘근(장무지굴근)"-> setIV("dae_toe_i_du", holder.ivMI)

                        else -> {

                        }
                    }
                }

            }
            is partViewHolder -> {
                if (currentItem != null) {
                    holder.tvPI.text = currentItem.first
                    setPartItem(currentItem.second, holder.clPI, holder.cvPI, holder.tvPI)
                    when (currentItem.first) {
                        "목" -> holder.ivPI.setImageResource(R.drawable.icon_part1)
                        "어깨" -> holder.ivPI.setImageResource(R.drawable.icon_part2)
                        "팔꿉" -> holder.ivPI.setImageResource(R.drawable.icon_part3)
                        "손목" -> holder.ivPI.setImageResource(R.drawable.icon_part4)
                        "골반" -> holder.ivPI.setImageResource(R.drawable.icon_part5)
                        "무릎" -> holder.ivPI.setImageResource(R.drawable.icon_part6)
                        "발목" -> holder.ivPI.setImageResource(R.drawable.icon_part7)
                    }
                }

            }
        }


    }

    override fun getItemCount(): Int {
        return stringList!!.size
    }

    private fun setIV(name: String, imageView: ImageView) {
//        Glide.with(fragment)
//            .load(fragment.resources.getIdentifier("drawable_muscle_${name}", "drawable", fragment.requireActivity().packageName))
//            .into(imageView)
        imageView.setImageResource(fragment.resources.getIdentifier("drawable_muscle_${name}", "drawable", fragment.requireActivity().packageName))
    }

    private fun setPartItem(score: Int, cl: ConstraintLayout, cv: CardView, tv: TextView){

        when (score) {
            2 -> {
                cl.backgroundTintList = ContextCompat.getColorStateList(fragment.requireContext(), R.color.deleteContainerColor)
                cv.setCardBackgroundColor(fragment.resources.getColor(R.color.deleteColor, null))
                tv.setTextColor(ContextCompat.getColor(fragment.requireContext(), R.color.deleteColor))
            }
            1 -> {
                cl.backgroundTintList = ContextCompat.getColorStateList(fragment.requireContext(), R.color.cautionContainerColor)
                cv.setCardBackgroundColor(fragment.resources.getColor(R.color.cautionColor, null))
                tv.setTextColor(ContextCompat.getColor(fragment.requireContext(), R.color.cautionColor))

            }
            0 -> {
                cl.backgroundTintList = ContextCompat.getColorStateList(fragment.requireContext(), R.color.subColor100)
                cv.setCardBackgroundColor(fragment.resources.getColor(R.color.subColor700, null))
                tv.setTextColor(ContextCompat.getColor(fragment.requireContext(), R.color.subColor700))
            }
            else -> {}
        }
    }
}