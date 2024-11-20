package com.tangoplus.tangoq.adapter.etc

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.airbnb.lottie.LottieAnimationView
import com.tangoplus.tangoq.R

class CautionVPAdapter(private val context: Context, private val layouts: List<Int>, private val isPose: Boolean, private val seq: Int) : RecyclerView.Adapter<CautionVPAdapter.CautionViewHolder>() {

    inner class CautionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvMSC1Num: TextView? = itemView.findViewById(R.id.tvMSC1Num)
        val tvMSC1Title: TextView? = itemView.findViewById(R.id.tvMSC1Title)
        val tvMSC1Explain: TextView? = itemView.findViewById(R.id.tvMSC1Explain)
        val cvMSC10: CardView? = itemView.findViewById(R.id.cvMSC10)
        val cvMSC11: CardView? = itemView.findViewById(R.id.cvMSC11)
        val cvMSC12: CardView? = itemView.findViewById(R.id.cvMSC12)
        val ivMSC1Frame : ImageView? = itemView.findViewById(R.id.ivSC1Frame)

        fun bind(animationName: String, lottieViewId: Int, isPose: Boolean) {
            val lottieView: LottieAnimationView = itemView.findViewById(lottieViewId)
            if (isPose) {
                setGuide(this)
                lottieView.visibility = View.GONE
                ivMSC1Frame?.visibility = View.VISIBLE
            } else {
                tvMSC1Num?.text = "1."
                tvMSC1Title?.text = "측정 환경 준비"
                tvMSC1Explain?.text = "기기와 간격을 벌려주세요\n화면의 실루엣에 몸 전체를 맞추는게 좋습니다 !"

                lottieView.setAnimation(animationName)
                lottieView.playAnimation()
                lottieView.visibility = View.VISIBLE
                ivMSC1Frame?.visibility = View.GONE
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CautionViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val view = inflater.inflate(viewType, parent, false)
        return CautionViewHolder(view)
    }

    private val lottieViewIds = listOf(
        R.id.lavSC1,
        R.id.lavSC2,
        R.id.lavSC3
    )
    private val lottieSeqAnimations = listOf<String>(
//        "measure_seq_anime_1.json","measure_seq_anime_2.json","measure_seq_anime_3.json","measure_seq_anime_4.json","measure_seq_anime_5.json","measure_seq_anime_6.json","measure_seq_anime_7.json"
        "measure_guide_anime_1.json", "measure_guide_anime_2.json","measure_guide_anime_3.json", "measure_guide_anime_1.json", "measure_guide_anime_2.json","measure_guide_anime_3.json", "measure_guide_anime_1.json"

    )

    private val lottieAnimations = listOf(
        "measure_guide_anime_1.json",
        "measure_guide_anime_2.json",
        "measure_guide_anime_3.json"
    )
    override fun onBindViewHolder(holder: CautionViewHolder, position: Int) {
        if (isPose) {
            holder.bind(lottieSeqAnimations[seq], lottieViewIds[position], true)
        } else {
            holder.bind(lottieAnimations[position], lottieViewIds[position], false)
        }
    }

    override fun getItemViewType(position: Int): Int {
        return layouts[position]
    }
    override fun getItemCount(): Int {
        return layouts.size
    }

    private fun setGuide(holder: CautionViewHolder?) {
        holder?.tvMSC1Num?.text = "${seq +1}"
        holder?.tvMSC1Title?.text = seqNames[seq]
        holder?.tvMSC1Explain?.text = seqComments[seq]
        holder?.cvMSC10?.visibility = View.GONE
        holder?.cvMSC11?.visibility = View.GONE
        holder?.cvMSC12?.visibility = View.GONE
        val imageResource = context.resources.getIdentifier("drawable_measure_$seq", "drawable", context.packageName)
        holder?.ivMSC1Frame?.setImageResource(imageResource)

    }

    private val seqNames = mapOf(
        0 to "서서 정면",
        1 to "스쿼트 자세",
        2 to "팔꿈치 정렬",
        3 to "서서 좌측",
        4 to "서서 우측",
        5 to "서서 후면",
        6 to "앉아 후면"
    )

    private val seqComments = mapOf(
        0 to "서서 정면자세를 취해주세요.\n화면크기에 맞춰 서시면 됩니다.",
        1 to "발을 양쪽으로 조금 더 벌려 서고\n손을 번쩍 들어 만세 자세로 준비해주세요\n측정이 시작되면 5초동안 스쿼트를 1회 시작하면 됩니다. 5초가 지나면 녹화가 종료됩니다.",
        2 to "주먹쥐고 양팔의 상완과 하완을 최대한 맡붙여주세요\n주먹또한 몸으로 최대한 붙이면 정확한 측정이 가능합니다.",
        3 to "편한 자세로 좌측을 보고 서주세요.",
        4 to "편한 자세로 우측을 보고 서주세요.",
        5 to "편한 자세로 화면 반대 방향을 보고 서주세요.",
        6 to "등받이가 없는 의자에 앉아 화면 반대 방향을 보고 서주세요."
    )
}
