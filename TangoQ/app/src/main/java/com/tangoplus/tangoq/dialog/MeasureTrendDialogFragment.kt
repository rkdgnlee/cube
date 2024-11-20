package com.tangoplus.tangoq.dialog

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.skydoves.balloon.ArrowPositionRules
import com.skydoves.balloon.Balloon
import com.skydoves.balloon.BalloonAnimation
import com.skydoves.balloon.BalloonSizeSpec
import com.skydoves.balloon.showAlignBottom
import com.skydoves.balloon.showAlignEnd
import com.tangoplus.tangoq.R
import com.tangoplus.tangoq.adapter.etc.SpinnerAdapter
import com.tangoplus.tangoq.adapter.TrendRVAdapter
import com.tangoplus.tangoq.data.AnalysisVO
import com.tangoplus.tangoq.viewmodel.AnalysisViewModel
import com.tangoplus.tangoq.data.MeasureVO
import com.tangoplus.tangoq.viewmodel.MeasureViewModel
import com.tangoplus.tangoq.databinding.FragmentMeasureTrendDialogBinding
import com.tangoplus.tangoq.fragment.ExtendedFunctions.isFirstRun
import com.tangoplus.tangoq.function.MeasurementManager.getAnalysisUnits
import com.tangoplus.tangoq.function.MeasurementManager.setImage
import com.tangoplus.tangoq.`object`.Singleton_t_measure
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray

class MeasureTrendDialogFragment : DialogFragment() {
    lateinit var binding : FragmentMeasureTrendDialogBinding
    private lateinit var trend1 : String
    private lateinit var trend2 : String
    private val avm : AnalysisViewModel by activityViewModels()
    private val mvm : MeasureViewModel by activityViewModels()
    private lateinit var measures : MutableList<MeasureVO>


    private lateinit var measureResult : JSONArray
    companion object{
        private const val ARG_COMPARE1 = "arg_compare1"
        private const val ARG_COMPARE2 = "arg_compare2"
        fun newInstance(trend1: String, trend2: String) : MeasureTrendDialogFragment {
            val fragment = MeasureTrendDialogFragment()
            val args = Bundle()
            args.putString(ARG_COMPARE1, trend1)
            args.putString(ARG_COMPARE2, trend2)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentMeasureTrendDialogBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        trend1 = arguments?.getString(ARG_COMPARE1) ?: ""
        trend2 = arguments?. getString(ARG_COMPARE2) ?: ""
        measures = Singleton_t_measure.getInstance(requireContext()).measures ?: mutableListOf()

        try {
            if (measures.isNotEmpty()) {
                showBalloon()
                avm.leftMeasurement.value = null
                avm.leftAnalyzes = null
                avm.rightMeasurement.value = measures[0]
                measureResult = mvm.selectedMeasure?.measureResult ?: JSONArray()
                // ------# 비교할 모든 analysisVO 넣기 #------
                avm.rightAnalyzes = transformAnalysis(avm.rightMeasurement.value?.measureResult!!)

                // ------# seq left spnr #------
                val seqs = listOf(
                    "정면 측정", "팔꿉 측정", "좌측 측정", "우측 측정", "후면 측정", "앉아 후면"
                )
                binding.spnrMTDSeqLeft.setPopupBackgroundDrawable(ContextCompat.getDrawable(requireContext(), R.color.white))
                binding.spnrMTDSeqLeft.adapter = SpinnerAdapter(requireContext(), R.layout.item_spinner, seqs, 2)
                binding.spnrMTDSeqLeft.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                        CoroutineScope(Dispatchers.IO).launch {
                            if (avm.leftMeasurement.value != null) {
                                if (position == 0) {
                                    setImage(this@MeasureTrendDialogFragment, avm.leftMeasurement.value, 0 , binding.ssivMTDLeft)
                                } else {
                                    setImage(this@MeasureTrendDialogFragment, avm.leftMeasurement.value, position + 1 , binding.ssivMTDLeft)
                                }
                            }
                        }
                    }
                    override fun onNothingSelected(parent: AdapterView<*>?) {}
                }

                binding.spnrMTDSeqRight.setPopupBackgroundDrawable(ContextCompat.getDrawable(requireContext(), R.color.white))
                binding.spnrMTDSeqRight.adapter = SpinnerAdapter(requireContext(), R.layout.item_spinner, seqs, 2)
                binding.spnrMTDSeqRight.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                        CoroutineScope(Dispatchers.IO).launch {
                            Log.v("시퀀스들", "position: ${position}, rightMeasure: ${avm.rightMeasurement.value}")
                            if (position == 0) {
                                setImage(this@MeasureTrendDialogFragment, avm.rightMeasurement.value, 0 , binding.ssivMTDRight)
                            } else {
                                setImage(this@MeasureTrendDialogFragment, avm.rightMeasurement.value, position + 1 , binding.ssivMTDRight)
                            }
                        }
                    }
                    override fun onNothingSelected(parent: AdapterView<*>?) {}
                }


                // ------# left spinner 연결 #------
                val measureDates = measures.map { it.regDate.substring(0, 11) }.toMutableList()
                measureDates.add(0, "선택")

                binding.spnrMTDLeft.setPopupBackgroundDrawable(ContextCompat.getDrawable(requireContext(), R.color.white))
                binding.spnrMTDLeft.adapter = SpinnerAdapter(requireContext(), R.layout.item_spinner, measureDates, 0)
                binding.spnrMTDLeft.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                        override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                            if (position != 0) {
                                avm.leftMeasurement.value = measures[position - 1]
                                avm.leftAnalyzes = transformAnalysis(avm.leftMeasurement.value?.measureResult!!)
                                Log.v("왼쪽analysis", "${avm.leftMeasurement.value}")
                                setAdapter(avm.leftAnalyzes, avm.rightAnalyzes)
                                CoroutineScope(Dispatchers.IO).launch {
                                    setImage(this@MeasureTrendDialogFragment, avm.leftMeasurement.value,0, binding.ssivMTDLeft)
                                    withContext(Dispatchers.Main) {
                                        binding.spnrMTDSeqLeft.setSelection(0)
                                    }
                                }
                            }
                        }
                        override fun onNothingSelected(parent: AdapterView<*>?) {}
                    }

                // ------# right spinner 연결 #------
                val measureDatesRight = measures.map { it.regDate.substring(0, 11) }.toMutableList()
                binding.spnrMTDRight.setPopupBackgroundDrawable(ContextCompat.getDrawable(requireContext(), R.color.white))
                binding.spnrMTDRight.adapter = SpinnerAdapter(requireContext(), R.layout.item_spinner, measureDatesRight, 0)
                binding.spnrMTDRight.onItemSelectedListener =
                    object : AdapterView.OnItemSelectedListener {
                        override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                            avm.rightMeasurement.value = measures[position]
                            avm.rightAnalyzes = transformAnalysis(avm.rightMeasurement.value?.measureResult!!)

                            setAdapter(avm.leftAnalyzes, avm.rightAnalyzes)
                            CoroutineScope(Dispatchers.IO).launch {
                                setImage(this@MeasureTrendDialogFragment, avm.rightMeasurement.value,0, binding.ssivMTDRight)
                                withContext(Dispatchers.Main) {
                                    binding.spnrMTDSeqRight.setSelection(0)
                                }
                            }
                        }
                        override fun onNothingSelected(parent: AdapterView<*>?) {}
                    }
            }
            Log.v("AVM>Trend", "$avm")
        } catch (e: IllegalArgumentException) {
            Log.e("TrendError", "${e.printStackTrace()}")
        }

    }

    private fun setAdapter(analyzesLeft: MutableList<MutableList<AnalysisVO>>?, analyzesRight : MutableList<MutableList<AnalysisVO>>?) { // 부위별 > 3개 > 2개의 float
        // ------# rv #------

        val layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
        val adapter = TrendRVAdapter(this@MeasureTrendDialogFragment, analyzesLeft, analyzesRight)
        binding.rvMTD.layoutManager = layoutManager
        binding.rvMTD.adapter = adapter
        adapter.notifyDataSetChanged()
    }

    private fun showBalloon() {
        val balloon2 = Balloon.Builder(requireContext())
            .setWidthRatio(0.5f)
            .setHeight(BalloonSizeSpec.WRAP)
            .setText("비교 날짜를 선택해주세요")
            .setTextColorResource(R.color.white)
            .setTextSize(15f)
            .setArrowPositionRules(ArrowPositionRules.ALIGN_ANCHOR)
            .setArrowSize(0)
            .setMargin(10)
            .setPadding(12)
            .setCornerRadius(8f)
            .setBackgroundColorResource(R.color.mainColor)
            .setBalloonAnimation(BalloonAnimation.OVERSHOOT)
            .setLifecycleOwner(viewLifecycleOwner)
            .build()

        if (isFirstRun("MeasureTrendDialogFragment_isFirstRun")) {
            Handler(Looper.getMainLooper()).postDelayed({
                binding.spnrMTDLeft.showAlignEnd(balloon2)
                balloon2.dismissWithDelay(1800L)
            }, 700)
        }
        binding.textView30.setOnClickListener { it.showAlignBottom(balloon2) }
    }
    private val matchedUris = mapOf(
        "목관절" to listOf(0, 3, 4, 5, 6),
        "좌측 어깨" to listOf(0, 3, 5, 6),
        "우측 어깨" to listOf(0, 4, 5, 6),
        "좌측 팔꿉" to listOf(0, 2, 3),
        "우측 팔꿉" to listOf(0, 2, 4),
        "좌측 손목" to listOf(0, 2, 3),
        "우측 손목" to listOf(0, 2, 4),
        "좌측 골반" to listOf(0, 3, 5, 6),
        "우측 골반" to listOf(0, 4, 5, 6),
        "좌측 무릎" to listOf(0, 5),
        "우측 무릎" to listOf(0, 5),
        "좌측 발목" to listOf(0, 5),
        "우측 발목" to listOf(0, 5)
    )

    private fun transformAnalysis(ja : JSONArray) : MutableList<MutableList<AnalysisVO>> {
        val analyzes = mutableListOf<MutableList<AnalysisVO>>()
        matchedUris.forEach { (part, seqList) ->
            val relatedAnalyzes = mutableListOf<AnalysisVO>()

            seqList.forEachIndexed { index, i ->
                val analysisUnits = getAnalysisUnits(part, i, ja)
                val normalUnits = analysisUnits.filter { it.state }
                val isNormal = if (normalUnits.size > (analysisUnits.size - normalUnits.size )) true else false
                val analysisVO = AnalysisVO(
                    i,
                    "",
                    isNormal,
                    analysisUnits,
                    mvm.selectedMeasure?.fileUris!![i]
                )
                relatedAnalyzes.add(analysisVO)
            }
            analyzes.add(relatedAnalyzes)
        }
        return analyzes
    }
}