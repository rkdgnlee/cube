package com.tangoplus.tangoq.dialog

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.constraintlayout.widget.ConstraintSet
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
import com.tangoplus.tangoq.vo.AnalysisVO
import com.tangoplus.tangoq.viewmodel.AnalysisViewModel
import com.tangoplus.tangoq.vo.MeasureVO
import com.tangoplus.tangoq.viewmodel.MeasureViewModel
import com.tangoplus.tangoq.databinding.FragmentMeasureTrendDialogBinding
import com.tangoplus.tangoq.fragment.ExtendedFunctions.isFirstRun
import com.tangoplus.tangoq.function.MeasurementManager.getAnalysisUnits
import com.tangoplus.tangoq.function.MeasurementManager.matchedUris
import com.tangoplus.tangoq.function.MeasurementManager.setImage
import com.tangoplus.tangoq.db.Singleton_t_measure
import com.tangoplus.tangoq.function.MeasurementManager.matchedTripleIndexes
import com.tangoplus.tangoq.function.SaveSingletonManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray

class MeasureTrendDialogFragment : DialogFragment() {
    lateinit var binding : FragmentMeasureTrendDialogBinding

    private val avm : AnalysisViewModel by activityViewModels()
    private val mvm : MeasureViewModel by activityViewModels()
    private lateinit var measures : MutableList<MeasureVO>
    private lateinit var singletonMeasure : Singleton_t_measure
    private lateinit var ssm : SaveSingletonManager
    private lateinit var measureResult : JSONArray

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentMeasureTrendDialogBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        measures = Singleton_t_measure.getInstance(requireContext()).measures ?: mutableListOf()
        ssm = SaveSingletonManager(requireContext(), requireActivity())
        singletonMeasure = Singleton_t_measure.getInstance(requireContext())
        try {
            if (measures.isNotEmpty()) {
                showBalloon()
                avm.leftMeasurement.value = null
                avm.leftAnalyzes = null
                avm.rightMeasurement.value = measures[0]
                measureResult = mvm.selectedMeasure?.measureResult ?: JSONArray()
                // ------# 비교할 모든 analysisVO 넣기 #------
                avm.rightAnalyzes = transformAnalysis(avm.rightMeasurement.value?.measureResult ?: JSONArray())

                // ------# seq left spnr #------
                val seqs = listOf(
                    "정면 측정", "팔꿉 측정", "좌측 측정", "우측 측정", "후면 측정", "앉아 후면"
                )

                // ------# 측정 내 seq 고르는 spinner left #------
                binding.spnrMTDSeqLeft.setPopupBackgroundDrawable(ContextCompat.getDrawable(requireContext(), R.color.white))
                binding.spnrMTDSeqLeft.adapter = SpinnerAdapter(requireContext(), R.layout.item_spinner, seqs, 2)
                binding.spnrMTDSeqLeft.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                        CoroutineScope(Dispatchers.IO).launch {
                            if (avm.leftMeasurement.value != null) {
                                if (position == 0) {
                                    setImage(this@MeasureTrendDialogFragment, avm.leftMeasurement.value, 0 , binding.ssivMTDLeft, "trend")
                                } else {
                                    setImage(this@MeasureTrendDialogFragment, avm.leftMeasurement.value, position + 1 , binding.ssivMTDLeft, "trend")
                                }
                            }
                        }
                    }
                    override fun onNothingSelected(parent: AdapterView<*>?) {}
                }
                // ------# 측정 내 seq 고르는 spinner right #------
                binding.spnrMTDSeqRight.setPopupBackgroundDrawable(ContextCompat.getDrawable(requireContext(), R.color.white))
                binding.spnrMTDSeqRight.adapter = SpinnerAdapter(requireContext(), R.layout.item_spinner, seqs, 2)
                binding.spnrMTDSeqRight.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                        CoroutineScope(Dispatchers.IO).launch {
                            Log.v("시퀀스들", "position: ${position}, rightMeasure: ${avm.rightMeasurement.value}")
                            if (position == 0) {
                                Log.v("애널라이즈", "${avm.rightAnalyzes}")
                                setImage(this@MeasureTrendDialogFragment, avm.rightMeasurement.value, 0 , binding.ssivMTDRight, "")
                            } else {
                                setImage(this@MeasureTrendDialogFragment, avm.rightMeasurement.value, position + 1 , binding.ssivMTDRight, "trend")
                            }

                            // spinner 클릭시 adapter 갱신
                            withContext(Dispatchers.Main) {
                                val parentIndexes = matchedTripleIndexes.mapIndexedNotNull { index, triples ->
                                    Log.v("포지션인덱스", "index, position : $index, $position")
                                    when {
                                        position == 0 -> if (triples.any { triple -> triple.first == 0 }) index else null
                                        else -> if (triples.any { triple -> triple.first == position + 1 }) index else null
                                    }
                                }
                                Log.v("parentIndexes", "$parentIndexes")
//                                val filteredAnalyzes = avm.leftAnalyzes?.filterIndexed { index, _ ->
//                                    index in parentIndexes
//                                }?.toMutableList()
                                setAdapter(avm.leftAnalyzes, avm.rightAnalyzes, parentIndexes)
                            }
                        }
                    }
                    override fun onNothingSelected(parent: AdapterView<*>?) {}
                }


                // -------# 측정 고르는 spinnner left #-------
                val measureDatesLeft = measures.map { "${it.regDate.substring(0, 11)}\n${it.userName}" }.toMutableList()
                val leftAdapter = ArrayAdapter(requireContext(), R.layout.dropdown_item, measureDatesLeft)
                binding.actvMTDLeft.setAdapter(leftAdapter)
                binding.actvMTDLeft.setOnItemClickListener { _, _, position, _ ->
                    Log.v("leftPosition", "$position")
                    CoroutineScope(Dispatchers.IO).launch {
                        // 측정파일 없으면 다운로드
                        setMeasureFiles(measures[position].regDate)
                        withContext(Dispatchers.Main) {
                            avm.leftMeasurement.value = Singleton_t_measure.getInstance(requireContext()).measures?.get(position)
                            avm.leftAnalyzes = transformAnalysis(avm.leftMeasurement.value?.measureResult ?: JSONArray())
                            // 다운로드한 jpg 파일 drawing
                            setImage(this@MeasureTrendDialogFragment, avm.leftMeasurement.value,0, binding.ssivMTDLeft, "trend")

                            setAdapter(avm.leftAnalyzes, avm.rightAnalyzes)
                            binding.spnrMTDSeqLeft.setSelection(0)
                        }
                    }
                }

                // ------# 측정 고르는 spinner right #------
                // 초기 오른쪽 세팅
                val measureDatesRight = measures.map { "${it.regDate.substring(0, 11)}\n${it.userName}" }.toMutableList()
                binding.actvMTDRight.setText(measureDatesRight[0])
                avm.rightMeasurement.value = measures[0]
                avm.rightAnalyzes = transformAnalysis(avm.rightMeasurement.value?.measureResult ?: JSONArray())
                setAdapter(avm.leftAnalyzes, avm.rightAnalyzes, listOf())
                CoroutineScope(Dispatchers.IO).launch {
                    setImage(this@MeasureTrendDialogFragment, avm.rightMeasurement.value,0, binding.ssivMTDRight, "trend")
                    withContext(Dispatchers.Main) {
                        binding.spnrMTDSeqRight.setSelection(0)
                    }
                }
                // 오른쪽 actv adapter 연결
                val rightAdapter = ArrayAdapter(requireContext(), R.layout.dropdown_item, measureDatesRight)
                binding.actvMTDRight.setAdapter(rightAdapter)
                binding.actvMTDRight.setOnItemClickListener { _, _, position, _ ->
                    binding.actvMTDRight.setText(measureDatesRight[position])
                    CoroutineScope(Dispatchers.IO).launch {
                        // 측정파일 없으면 다운로드
                        setMeasureFiles(measures[position].regDate)
                        withContext(Dispatchers.Main) {
                            avm.rightMeasurement.value = Singleton_t_measure.getInstance(requireContext()).measures?.get(position)
                            avm.rightAnalyzes = transformAnalysis(avm.rightMeasurement.value?.measureResult ?: JSONArray())
                            // 다운로드한 jpg 파일 drawing
                            setImage(this@MeasureTrendDialogFragment, avm.rightMeasurement.value,0, binding.ssivMTDRight, "trend")

                            setAdapter(avm.leftAnalyzes, avm.rightAnalyzes)
                            binding.spnrMTDSeqRight.setSelection(0)
                        }
                    }
                }
            }
        } catch (e: NullPointerException) {
            Log.e("TrendError", "NullPointer: ${e.message}")
        } catch (e: IllegalStateException) {
            Log.e("TrendError", "IllegalState: ${e.message}")
        } catch (e: ClassNotFoundException) {
            Log.e("TrendError", "ClassNotFound: ${e.message}")
        }  catch (e: IllegalArgumentException) {
            Log.e("TrendError", "IllegalArgumentException: ${e.message}")
        } catch (e: Exception) {
            Log.e("TrendError", "Exception: ${e.message}")
        }

    }

    private fun setAdapter(analyzesLeft: MutableList<MutableList<AnalysisVO>>?,
                           analyzesRight : MutableList<MutableList<AnalysisVO>>?,
                           filteredIndexes: List<Int> = listOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12)) { // 부위별 > 3개 > 2개의 float
        // ------# rv #------

        val layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
        val adapter = TrendRVAdapter(this@MeasureTrendDialogFragment, analyzesLeft, analyzesRight, filteredIndexes)
        binding.rvMTD.layoutManager = layoutManager
        binding.rvMTD.adapter = adapter
        adapter.notifyDataSetChanged()

        val rightHeight = binding.ssivMTDRight.height
        val layoutParams = binding.ssivMTDLeft.layoutParams
        layoutParams.height = rightHeight
        binding.ssivMTDLeft.layoutParams = layoutParams
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
                binding.actvMTDLeft.showAlignEnd(balloon2)
                balloon2.dismissWithDelay(1800L)
            }, 700)
        }
        binding.textView30.setOnClickListener { it.showAlignBottom(balloon2) }
    }

    // 13 > 3개의 AnalysisVO임. == 관절 1개의 3개가 들어간 raw 하나를 말하는 것.
    private fun transformAnalysis(ja : JSONArray) : MutableList<MutableList<AnalysisVO>> {
        val analyzes = mutableListOf<MutableList<AnalysisVO>>()
        matchedUris.forEach { (part, seqList) ->
            val relatedAnalyzes = mutableListOf<AnalysisVO>()

            seqList.forEachIndexed { index, i ->
                val analysisUnits = getAnalysisUnits(requireContext(), part, i, ja)
                val normalUnits = analysisUnits.filter { it.state == 1 }.size
                val warningUnits = analysisUnits.filter { it.state == 2 }.size
                val dangerUnits = analysisUnits.filter { it.state == 3 }.size
                val state = if (normalUnits >= warningUnits && normalUnits >= dangerUnits) 1
                else if (warningUnits >= normalUnits && warningUnits >= dangerUnits) 2
                else if (dangerUnits >= normalUnits && dangerUnits >= warningUnits) 3
                else 0
                val analysisVO = AnalysisVO(
                    i,
                    "",
                    state,
                    analysisUnits,
                    mvm.selectedMeasure?.fileUris?.get(i) ?: ""
                )
                relatedAnalyzes.add(analysisVO)
            }
            analyzes.add(relatedAnalyzes)
        }
        return analyzes
    }

    private suspend fun setMeasureFiles(inputRegDate: String?) {
        val dialog = LoadingDialogFragment.newInstance("측정파일")
        dialog.show(requireActivity().supportFragmentManager, "LoadingDialogFragment")

        // ------# 로딩을 통해 파일 가져오기 #------
        withContext(Dispatchers.IO) {
            try {
                val currentMeasure = singletonMeasure.measures?.find { it.regDate == inputRegDate }
                val uriTuples = currentMeasure?.sn?.let { it1 -> ssm.get1MeasureUrls(it1) }
                if (uriTuples != null) {
                    Log.v("리스너1", "$uriTuples")
                    ssm.downloadFiles(uriTuples)
                    val editedMeasure = ssm.insertUrlToMeasureVO(uriTuples, currentMeasure)
                    Log.v("리스너2", "$editedMeasure")

                    // singleton의 인덱스 찾아서 ja와 값 넣기
                    val singletonIndex = singletonMeasure.measures?.indexOfLast { it.regDate == inputRegDate }
                    if (singletonIndex != null && singletonIndex >= 0) {
                        singletonMeasure.measures?.set(singletonIndex, editedMeasure)
                        avm.leftMeasurement.value = editedMeasure
                        Log.v("수정완료", "index: $singletonIndex, VO: $editedMeasure")
                        withContext(Dispatchers.Main) {
                            dialog.dismiss()
                        }
                    } else {

                    }
                } else {

                }
            } catch (e: IllegalStateException) {
                Log.e("trendError", "MeasureBSIllegalState: ${e.message}")
            } catch (e: IllegalArgumentException) {
                Log.e("trendError", "MeasureBSIllegalArgument: ${e.message}")
            } catch (e: NullPointerException) {
                Log.e("trendError", "MeasureBSNullPointer: ${e.message}")
            } catch (e: InterruptedException) {
                Log.e("trendError", "MeasureBSInterrupted: ${e.message}")
            } catch (e: IndexOutOfBoundsException) {
                Log.e("trendError", "MeasureBSIndexOutOfBounds: ${e.message}")
            } catch (e: Exception) {
                Log.e("trendError", "MeasureBS: ${e.message}")
            }
//            finally {
//                withContext(Dispatchers.Main) {
//                    if (dialog.isAdded && dialog.isVisible) {
//                        dialog.dismiss()
//                    }
//                }
//            }

        }
        Log.w("selectedMeasureDate", "selectedMeasure: ${mvm.selectedMeasureDate.value}, selectMeasure: ${mvm.selectMeasureDate.value}")
        Log.w("selectedMeasure", "${mvm.selectedMeasure}")
        dialog.dismiss()
    }

    override fun onResume() {
        super.onResume()
        dialog?.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog?.window?.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
    }
}