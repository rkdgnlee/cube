package com.tangoplus.tangoq.fragment

import android.annotation.SuppressLint
import android.content.res.ColorStateList
import android.graphics.BitmapFactory
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Gravity
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import androidx.core.content.ContextCompat
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.tangoplus.tangoq.R
import com.tangoplus.tangoq.adapter.MeasureDetailRVAdapter
import com.tangoplus.tangoq.vo.MeasureVO
import com.tangoplus.tangoq.viewmodel.MeasureViewModel
import com.tangoplus.tangoq.databinding.FragmentMeasureDetailBinding
import com.tangoplus.tangoq.db.Singleton_t_measure
import com.tangoplus.tangoq.dialog.AlarmDialogFragment
import com.tangoplus.tangoq.fragment.ExtendedFunctions.createGuide
import com.tangoplus.tangoq.fragment.ExtendedFunctions.scrollToView
import com.tangoplus.tangoq.fragment.ExtendedFunctions.setOnSingleClickListener
import com.tangoplus.tangoq.function.MeasurementManager.createMeasureComment
import com.tangoplus.tangoq.function.MeasurementManager.getAnalysisUnits
import com.tangoplus.tangoq.function.MeasurementManager.matchedIndexs
import com.tangoplus.tangoq.function.MeasurementManager.matchedUris
import com.tangoplus.tangoq.function.MeasurementManager.partIndexes
import com.tangoplus.tangoq.view.BadgeButton
import com.tangoplus.tangoq.viewmodel.AnalysisViewModel
import com.tangoplus.tangoq.vision.ImageProcessingUtil.combineImageAndOverlay
import com.tangoplus.tangoq.vision.ImageProcessingUtil.partXBias
import com.tangoplus.tangoq.vision.ImageProcessingUtil.partYBias
import com.tangoplus.tangoq.vo.AnalysisUnitVO
import com.tangoplus.tangoq.vo.AnalysisVO
import org.json.JSONArray
import kotlin.math.hypot


class MeasureDetailFragment : Fragment() {
    private lateinit var binding : FragmentMeasureDetailBinding
    private var singletonMeasure : MutableList<MeasureVO>? = null
    private var measure : MeasureVO? = null
    private val mvm : MeasureViewModel by activityViewModels()
    private val avm : AnalysisViewModel by activityViewModels()
    private lateinit var adapterAnalysises : List<AnalysisVO>
    private lateinit var partStates : MutableList<Pair<String, Float>>
    private var btns : List<BadgeButton>? = null
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentMeasureDetailBinding.inflate(inflater)
        return binding.root
    }


    @SuppressLint("ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        singletonMeasure = Singleton_t_measure.getInstance(requireContext()).measures
        val showMeasure = arguments?.getBoolean("showMeasure", false) ?: false
        if (showMeasure) {
            showGuideAfterFinishMeasure()
        }
        avm.analysisType = 1
        btns = listOf(binding.btnMD0, binding.btnMD1, binding.btnMD2, binding.btnMD3,
            binding.btnMD4, binding.btnMD5, binding.btnMD6, binding.btnMD7,
            binding.btnMD8, binding.btnMD9, binding.btnMD10, binding.btnMD11, binding.btnMD12 )

        // skeleton의 관절을 누르면 터치 리스너로 버튼 누른 것처럼 동작하기
        binding.ivMDSkeleton.viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                binding.ivMDSkeleton.viewTreeObserver.removeOnGlobalLayoutListener(this)
                binding.ivMDSkeleton.setOnTouchListener { _, event ->
                    if (event.action == MotionEvent.ACTION_DOWN) {
                        val touchX = event.x
                        val touchY = event.y
                        val viewWidth = binding.ivMDSkeleton.width.toFloat()
                        val viewHeight = binding.ivMDSkeleton.height.toFloat()
                        val touchRadiusPx = 36 * resources.displayMetrics.density
                        for ((part, xBias) in partXBias) {

                            // danger, warning 마크가 있는 곳만 버튼 클릭이 되게끔 하기

                            if (measure?.dangerParts?.map { it.first }?.contains(part) == true) {
                                val yBias = partYBias[part] ?: continue
                                val jointX = xBias * viewWidth
                                val jointY = yBias * viewHeight
                                val distance = hypot(touchX - jointX, touchY - jointY)
                                if (distance <= touchRadiusPx) {
                                    avm.currentPart.value = part
                                    Handler(Looper.getMainLooper()).postDelayed({
                                        scrollToView(binding.ivMDSkeleton, binding.nsvMD)
                                    }, 400)
                                    break
                                }
                            }
                        }
                    }
                    true
                }
            }
        })

        binding.ibtnMDAlarm.setOnSingleClickListener {
            val dialog = AlarmDialogFragment()
            dialog.show(requireActivity().supportFragmentManager, "AlarmDialogFragment")
        }
        binding.ibtnMDBack.setOnSingleClickListener {
            requireActivity().supportFragmentManager.beginTransaction().apply {
                replace(R.id.flMain, MeasureHistoryFragment())
                commit()
            }
        }
        // ------# measure 에 맞게 UI 수정 #------
        measure = mvm.selectedMeasure
        avm.mdMeasureResult = measure?.measureResult?.optJSONArray(1) ?: JSONArray()
        updateUI()
        setParts()        // 스켈레톤 drawable 넣기
        setButtonState()

        // summary 넣기
        val summaryComments  = createMeasureComment(measure?.dangerParts)
        Log.v("써머리들어간 후", "$summaryComments")
        if (summaryComments.size > 1) {
            binding.tvMDResult1.text = summaryComments[0]
            binding.tvMDResult2.text = summaryComments[1]
        } else if (summaryComments.size == 1) {
            binding.tvMDResult1.text = summaryComments[0]
            binding.tvMDResult2.visibility = View.INVISIBLE
            binding.cvMDResult2.visibility = View.INVISIBLE
        } else {
            binding.tvMDResult1.text = ""
            binding.cvMDResult1.visibility = View.INVISIBLE
            binding.cvMDResult2.visibility = View.INVISIBLE
        }

        val originalBitmap = BitmapFactory.decodeResource(requireContext().resources, R.drawable.drawable_skeleton)
        val resultBitmap = combineImageAndOverlay(requireContext(), originalBitmap, measure?.dangerParts)
        binding.ivMDSkeleton.setImageBitmap(resultBitmap)

        avm.currentPart.observe(viewLifecycleOwner) { part ->
            if (!part.isNullOrEmpty() && measure?.measureResult != JSONArray() ) { //
                setAdapter(part)
                // 초기 상태
                avm.currentIndex = matchedIndexs.indexOf(part)
                updateButtonState()
            }
        }
    }

    private fun setButtonState() {
        // Pair -> index ( 몇번 관절인지 ) , 1.0f
        val dangerIndexMap: Map<Int, Float> = measure?.dangerParts?.mapNotNull { pair ->
            val index = partIndexes.entries.find { it.value == pair.first }?.key
            if (index != null) index to pair.second else null
        }?.toMap() ?: emptyMap()

        btns?.forEachIndexed { index, btn ->
            val state = dangerIndexMap[index]
            if (state != null) {
                btn.showBadge = true
                when (state) {
                    1.0f -> btn.setWarningState()
                    2.0f -> btn.setDangerState()
                }
            }
        }
    }

    private fun updateButtonState() {
        btns?.forEachIndexed { index, btn ->
            val isSelected = avm.currentPart.value == btn.text.toString()
            btn.backgroundTintList = when {
                avm.currentPart.value == btn.text.toString() -> ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.mainColor))
                else -> ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.subColor200))
            }
            btn.isClickable = !isSelected
            btn.isEnabled = !isSelected

            btn.setOnSingleClickListener {
                avm.currentPart.value = btn.text.toString()
                updateButtonState()  // 버튼 상태를 다시 업데이트

                Handler(Looper.getMainLooper()).postDelayed({
                    scrollToView(binding.ivMDSkeleton, binding.nsvMD)
                }, 400)
            }
        }
    }

    private fun setAdapter(part: String) {
//        val painPart = avm.currentParts?.find { it == part }
        val seqs = matchedUris[part]
        val groupedAnalyses = mutableMapOf<Int, MutableList<MutableList<AnalysisUnitVO>>>()

        seqs?.forEach { seqIndex ->
            val analyses = getAnalysisUnits(requireContext(), part, seqIndex, measure?.measureResult ?: JSONArray())
            if (!groupedAnalyses.containsKey(seqIndex)) {
                groupedAnalyses[seqIndex] = mutableListOf()
            }
            groupedAnalyses[seqIndex]?.add(analyses)
        }

        adapterAnalysises = groupedAnalyses.map { (indexx, analysesList) ->
            AnalysisVO(
                indexx = indexx,
                labels = analysesList.flatten().toMutableList(),
                url = measure?.fileUris?.get(indexx) ?: ""
            )
        }.sortedBy { it.indexx }

        val layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
        val adapter = MeasureDetailRVAdapter(this@MeasureDetailFragment, adapterAnalysises, avm) // avm.currentIndex가 2인데 adapterAnalysises에는 0, 5밖에없어서 indexOutOfBoundException이 나옴.
        binding.rvMD.layoutManager = layoutManager
        binding.rvMD.adapter = adapter
    }

    private fun setParts() {
        partStates = matchedIndexs.map { part ->
            measure?.dangerParts?.find { it.first == part }  ?: (part to 0f)
        }. toMutableList().apply {
            val zeroItem = filter { it.second == 0f }
            removeAll(zeroItem)
            addAll(zeroItem)
        }
        if (avm.mdMeasureResult != JSONArray()) {
            avm.currentPart.value = partStates.get(0).first
        }
    }

    private fun updateUI() {
        binding.tvMDScore.text = measure?.overall.toString()
        binding.tvMDDate.text = "${measure?.regDate?.substring(0, 16)}" // ${measure?.userName}
    }

    private fun showGuideAfterFinishMeasure() {
        context.let { safeContext ->
            if (safeContext != null) {
                createGuide(
                    context = requireContext(),
                    text = "측정 결과에 대한 요약 부분입니다",
                    anchor = binding.view37,
                    gravity = Gravity.BOTTOM,
                    dismiss = {
                        if (safeContext != null) {
                            createGuide(
                                context = requireContext(),
                                text = "버튼을 눌러 각 관절별 세부 결과를 확인하세요",
                                anchor = binding.view8,
                                gravity = Gravity.BOTTOM,
                                dismiss = {
                                }
                            )
                        }
                    }
                )
            }
        }
    }

}