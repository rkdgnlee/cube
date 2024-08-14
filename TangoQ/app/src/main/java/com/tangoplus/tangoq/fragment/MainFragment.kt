package com.tangoplus.tangoq.fragment

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.RenderEffect
import android.graphics.Shader
import android.graphics.drawable.BitmapDrawable
import android.icu.util.Measure
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.github.mikephil.charting.animation.ChartAnimator
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.interfaces.datasets.IBarDataSet
import com.github.mikephil.charting.listener.OnChartValueSelectedListener
import com.github.mikephil.charting.renderer.BarChartRenderer
import com.github.mikephil.charting.utils.ViewPortHandler
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.skydoves.balloon.ArrowPositionRules
import com.skydoves.balloon.Balloon
import com.skydoves.balloon.BalloonAnimation
import com.skydoves.balloon.BalloonSizeSpec
import com.tangoplus.tangoq.MainActivity
import com.tangoplus.tangoq.listener.OnRVClickListener
import com.tangoplus.tangoq.`object`.Singleton_t_user
import com.tangoplus.tangoq.R
import com.tangoplus.tangoq.db.PreferencesManager
import com.tangoplus.tangoq.data.ExerciseVO
import com.tangoplus.tangoq.data.GraphVO
import com.tangoplus.tangoq.data.ProgramVO
import com.tangoplus.tangoq.data.UserViewModel
import com.tangoplus.tangoq.databinding.FragmentMainBinding
import com.tangoplus.tangoq.dialog.AlarmDialogFragment
import com.tangoplus.tangoq.dialog.CustomExerciseDialogFragment
import com.tangoplus.tangoq.dialog.MainFilterDialogFragment
import com.tangoplus.tangoq.dialog.SetupDialogFragment
import com.tangoplus.tangoq.`object`.DeviceService.isNetworkAvailable
import com.tangoplus.tangoq.`object`.NetworkExercise.fetchCategoryAndSearch
import com.tangoplus.tangoq.`object`.NetworkExercise.fetchExerciseById
import com.tangoplus.tangoq.`object`.NetworkProgram.fetchProgramVOBySn
import com.tangoplus.tangoq.view.BarChartRender
import jp.wasabeef.blurry.Blurry
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.TextStyle
import java.util.Date
import java.util.Locale
import kotlin.random.Random


class MainFragment : Fragment(), OnRVClickListener {
    lateinit var binding: FragmentMainBinding

    val viewModel : UserViewModel by activityViewModels()
//    val mViewModel : MeasureViewModel by activityViewModels()
    private var bannerPosition = Int.MAX_VALUE/2
    private val intervalTime = 2400.toLong()
    private lateinit var currentExerciseItem : ExerciseVO
    private lateinit var startForResult: ActivityResultLauncher<Intent>
    lateinit var prefsManager : PreferencesManager
    val emptyHistory = false



    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentMainBinding.inflate(inflater)
//        binding.sflM.startShimmer()

        // ActivityResultLauncher 초기화
        startForResult = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                // 결과 처리
            }
        }
        return binding.root
    }

    @SuppressLint("SetTextI18n", "NotifyDataSetChanged")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // ------# 스크롤 관리 #------
        binding.nsvM.isNestedScrollingEnabled = false
        prefsManager = PreferencesManager(requireContext())

        // ------# 알람 intent #------
        binding.ibtnMAlarm.setOnClickListener {
            val dialog = AlarmDialogFragment()
            dialog.show(requireActivity().supportFragmentManager, "AlarmDialogFragment")
        }

//        val dialog = SetupDialogFragment.newInstance("startSetup")
//        dialog.show(requireActivity().supportFragmentManager, "SetupDialogFragment")


        when (isNetworkAvailable(requireContext())) {
            true -> {
                val userJson = Singleton_t_user.getInstance(requireContext()).jsonObject

                binding.tvMName.text = userJson?.optString("user_name") + " 님"

                // ------! 일주일 간 운동 기록 들어올 곳 시작 !------
                val weeklySets = listOf(-1f, 38f, 60f, 12f, -1f, 0f, 0f)
                var finishSets = 0
                for (indices in weeklySets) {
                    if (indices > 0f) finishSets += 1
                }

                // ------# 일주일 정해진 set 가져오기 #------
                val goalSets = 7

                binding.tvMWeekly.text = "주 ${finishSets} / ${goalSets} 회 "
                // ------! 일주일 간 운동 기록 들어올 곳 끝 !------

                // ------! bar chart 시작 !------
                val barChart: BarChart = binding.bcMWeekly
                barChart.renderer = BarChartRender(barChart, barChart.animator, barChart.viewPortHandler)
                val entries = ArrayList<BarEntry>()

                for (i in weeklySets.indices) {
                    val entry = BarEntry(i.toFloat(), weeklySets[i])
                    entries.add(entry)
                }
                val dataSet = BarDataSet(entries, "")
                dataSet.apply {
                    color =  resources.getColor(R.color.mainColor, null)
                    setDrawValues(false)
                }
                // BarData 생성 및 차트에 설정
                val bcdata = BarData(dataSet)
                bcdata.apply {
                    barWidth = 0.5f
                }
                barChart.data = bcdata
                // X축 설정
                barChart.xAxis.apply {
                    position = XAxis.XAxisPosition.BOTTOM
                    setDrawGridLines(false)
                    setDrawAxisLine(false)
                    labelRotationAngle = 2f
                    setDrawLabels(false)
                }
                barChart.legend.apply {
                    formSize = 0f
                }
                // 왼쪽 Y축 설정
                barChart.axisLeft.apply {
                    axisMinimum = -1f // Y축 최소값
                    setDrawAxisLine(false)
                    setDrawGridLines(false)
                    setLabelCount(0, false)
                    setDrawLabels(false)
                }
                // 차트 스타일링 및 설정
                barChart.apply {
                    axisRight.isEnabled = false
                    description.isEnabled = false
                    legend.isEnabled = false
                    setDrawValueAboveBar(false)
                    setDrawGridBackground(false)
                    setFitBars(false)
                    animateY(500)
                    setScaleEnabled(false)
                    setTouchEnabled(false)
                    invalidate()
                }

                // ------! 월화수목금토일 데이터 존재할 시 변경할 구간 시작 !------
                for (indices in weeklySets) {
                    if (indices > 0) {
                        setWeeklyDrawable("ivM${weeklySets.indexOf(indices) + 1}", "icon_week_${weeklySets.indexOf(indices)+1}_enabled")
                    } else {
                        setWeeklyDrawable("ivM${weeklySets.indexOf(indices) + 1}", "icon_week_${weeklySets.indexOf(indices)+1}_disabled")
                    }
                }

                binding.vMProfile.setOnClickListener{
                    activity?.let { activity ->
                        val bnb: BottomNavigationView = activity.findViewById(R.id.bnbMain)
                        bnb.selectedItemId = R.id.measure

                        activity.supportFragmentManager.executePendingTransactions()

                        val measureFragment = activity.supportFragmentManager.fragments
                            .find { it is MeasureFragment } as? MeasureFragment

                        measureFragment?.selectDashBoard2()
                    } ?: run {
                        Log.e("MainFragment", "Activity is not attached or null")
                    }
                }
                // ------! 월화수목금토일 데이터 존재할 시 변경할 구간 끝 !------

                // ------! 측정 기간 시작 !------
                //TODO 측정 요약 데이터 가져와야 함.
                binding.tvMMeasureDuration.text = "기간 06.29 ~ ${SimpleDateFormat("MM.dd").format(Date().time)}"

                // ------! 측정 기간 끝 !------

                // ------! 꺾은선 그래프 시작 !------
                val lineChart = binding.lcMMeasure
                val lcXAxis = lineChart.xAxis
                val lcYAxisLeft = lineChart.axisLeft
                val lcYAxisRight = lineChart.axisRight
                val lcLegend = lineChart.legend

                val lcDataList : MutableList<GraphVO> = mutableListOf()

                val weekList = listOf("", "", "", "", "", "", "")
                for (i in weekList) {
                    val y = Random.nextInt(70, 99)
//            lcDataList.add(GraphVO(i, i.length.p))
                    lcDataList.add(GraphVO(i, y))
//            y += 1
                }

                val lcEntries : MutableList<Entry> = mutableListOf()
                for (i in lcDataList.indices) {
                    // entry는 y축에 넣는 데이터 형식을 말함. Entry의 1번째 인자는 x축의 데이터의 순서, 두 번째 인자는 y값
                    lcEntries.add(Entry(i.toFloat(), lcDataList[i].yAxis.toFloat()))
                }
                val lcLineDataSet = LineDataSet(lcEntries, "")
                lcLineDataSet.apply {

                    color = resources.getColor(R.color.mainColor, null)
                    lineWidth = 3F
                    valueTextSize = 0F
                    circleRadius = 6f
                    circleHoleRadius = 3f
                    setCircleColors(resources.getColor(R.color.mainColor))
                    setDrawCircleHole(true)
                    setDrawFilled(true)
//                    fillDrawable = ContextCompat.getDrawable(requireContext(), R.drawable.color_gradient_sub_color_300)
                    mode = LineDataSet.Mode.CUBIC_BEZIER
                    setDrawFilled(false)
                }

                lcXAxis.apply {

                    labelRotationAngle = 2F
                    setDrawAxisLine(false)
                    setDrawGridLines(false)
                    lcXAxis.valueFormatter = (IndexAxisValueFormatter(lcDataList.map { it.xAxis }))
                    setLabelCount(lcDataList.size, false)
                    lcXAxis.position = XAxis.XAxisPosition.BOTTOM
                }
                lcYAxisLeft.apply {
                    setDrawGridLines(false)
                    setDrawAxisLine(false)
                    setLabelCount(3, false)
                    setDrawLabels(false)
                    axisMinimum = 60f
                }
                lcYAxisRight.apply {
                    setDrawGridLines(false)
                    setDrawAxisLine(false)
                    setLabelCount(0, false)
                    setDrawLabels(false)
                }
                lcLegend.apply {
                    lcLegend.formSize = 0f
                }
                lineChart.apply {
                    data = LineData(lcLineDataSet)
                    notifyDataSetChanged()
                    description.text = ""
                    setScaleEnabled(false)
                    setTouchEnabled(true)
                    isDragEnabled = true
                    invalidate()
                    data.setDrawValues(false)
                    for (set in data.dataSets) {
                        if (set is LineDataSet) {
                            set.setDrawHighlightIndicators(false)
                        }
                    }
                }
                // ---- 꺾은선 그래프 코드 끝 ----



                // ------! sharedPrefs에서 오늘 운동한 횟수 가져오기 시작 !------
                val userSn = userJson?.optString("user_sn").toString()
//                val userSn = "70"
                // ------! sharedPrefs에서 오늘 운동한 횟수 가져오기 끝 !------

                // ------! db에서 받아서 뿌려주기 시작 !------
                lifecycleScope.launch {

                    // ------! 맞춤 운동 item 시작 !------
                    // TODO 프로그램을 불러오거나 전체에서 필터링해서 만들어야 함

                    when (emptyHistory) {
                        true -> setViewVisibility(true) // 측정기록이 없을 때
                        else -> {
                            // 측정 기록이 있을 때
                            setViewVisibility(false)

                            currentExerciseItem = fetchExerciseById(getString(R.string.IP_ADDRESS_t_exercise_description), "136")
                            Glide.with(requireContext())
                                .load("${currentExerciseItem.imageFilePathReal}")
                                .diskCacheStrategy(DiskCacheStrategy.ALL)
                                .into(binding.ivMCustomThumbnail)
                            binding.tvMCustomName.text = currentExerciseItem.exerciseName
                            binding.tvMCustomSymptom.text = currentExerciseItem.relatedSymptom
                            val second = "${currentExerciseItem.videoDuration?.toInt()?.div(60)}분 ${currentExerciseItem.videoDuration?.toInt()?.rem(60)}초"
                            binding.tvMCustomTime.text = second
                            when (currentExerciseItem.exerciseStage) {
                                "초급" -> {
                                    binding.ivMCustomStage.setImageDrawable(ContextCompat.getDrawable(requireContext(), R.drawable.icon_stage_1))
                                    binding.tvMCustomStage.text = "초급자"
                                }
                                "중급" -> {
                                    binding.ivMCustomStage.setImageDrawable(ContextCompat.getDrawable(requireContext(), R.drawable.icon_stage_2))
                                    binding.tvMCustomStage.text = "중급자"
                                }
                                "고급" -> {
                                    binding.ivMCustomStage.setImageDrawable(ContextCompat.getDrawable(requireContext(), R.drawable.icon_stage_3))
                                    binding.tvMCustomStage.text = "상급자"
                                }
                            }

                            // ------! 운동 카운트 및 버튼 경로 변경 !------
                            val prefsManager = PreferencesManager(requireContext())
                            val currentValue = prefsManager.getStoredInt(userSn)
                            Log.v("prefs>CurrentValue", "user_sn: ${userSn}, currentValue: ${currentValue}")
                            binding.tvMCustomProgress.text = "완료 $currentValue /5 개"
                            binding.hpvMCustomProgress.progress = (currentValue  * 100 ) / 5
                            viewModel.dailyProgress.observe(viewLifecycleOwner) {
                                binding.tvMCustomProgress.text = "완료 $it /5 개"
                                binding.hpvMCustomProgress.progress = (it  * 100 ) / 5
                            }
                            binding.btnMCustom.text = "운동 시작하기"
                        }
                    }
                    // ------! 맞춤 운동 item 끝 !------

                    val programVOList = mutableListOf<ProgramVO>()
                    for (i in 10 downTo 8) {
                        programVOList.add(fetchProgramVOBySn(getString(R.string.IP_ADDRESS_t_exercise_programs), i.toString()))
                    }
//                    binding.sflM.stopShimmer()
//                    binding.sflM.visibility = View.GONE
                    setRVAdapter(programVOList)

                    binding.btnMCustom.setOnClickListener{
                        if (binding.btnMCustom.text == "측정하기") {
                            requireActivity().supportFragmentManager.beginTransaction().apply {
                                replace(R.id.flMain, MeasureDashBoard1Fragment())
                                commit()
                            }
                        } else {
                            val dialog = CustomExerciseDialogFragment()
                            val bundle = Bundle()
                            bundle.putParcelable("Program", programVOList[0])
                            dialog.arguments = bundle
                            Log.v("program", "${programVOList[0]}")
                            dialog.show(requireActivity().supportFragmentManager, "CustomExerciseDialogFragment")
                        }
                    }
                }



                // ------! 블러 처리 유무 및 기능 유무 시작 !------
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (emptyHistory) {


                    } else {
                        // ------! rvM보여주기 !------
                        lifecycleScope.launch {
                            val programVOList = mutableListOf<ProgramVO>()

                            for (i in 10 downTo 8) {
                                programVOList.add(
                                    fetchProgramVOBySn(
                                        getString(R.string.IP_ADDRESS_t_exercise_programs),
                                        i.toString()
                                    )
                                )
                            }
                            setRVAdapter(programVOList)
                        }

                        // ------# 그래프 클릭 시 balloon #------

                        lineChart.setOnChartValueSelectedListener(object: OnChartValueSelectedListener {
                            override fun onValueSelected(e: Entry?, h: Highlight?) {
                                e?.let { entry ->
                                    val balloonMlc = Balloon.Builder(requireContext())
                                        .setWidthRatio(0.5f)
                                        .setHeight(BalloonSizeSpec.WRAP)
                                        .setText("${entry.y.toInt()}점\n${weekList[entry.x.toInt()]}요일")  // 선택된 데이터 포인트의 y값을 텍스트로 설정
                                        .setTextColorResource(R.color.subColor800)
                                        .setTextSize(15f)
                                        .setArrowPositionRules(ArrowPositionRules.ALIGN_ANCHOR)
                                        .setArrowSize(0)
                                        .setMargin(10)
                                        .setPadding(12)
                                        .setCornerRadius(8f)
                                        .setBackgroundColorResource(R.color.white)
                                        .setBalloonAnimation(BalloonAnimation.OVERSHOOT)
                                        .setLifecycleOwner(viewLifecycleOwner)
                                        .build()


                                    val pts = FloatArray(2)
                                    pts[0] = e.x
                                    pts[1] = e.y
                                    lineChart.getTransformer(YAxis.AxisDependency.LEFT).pointValuesToPixel(pts)
                                    balloonMlc.showAtCenter(lineChart, pts[0].toInt(), pts[1].toInt())

                                    Log.v("eeee", "e.x: ${e.x}, e.y: ${e.y}")
                                }
                                Log.v("eeee", "e.x: ${e?.x}, e.y: ${e?.y}")
                            }
                            override fun onNothingSelected() {}
                        })
                    }
                }
                // ------! 블러 처리 유무 및 기능 유무 끝 !------

            }
            false -> {
                // ------# 인터넷 연결이 없을 때 #------
            }
        }




        binding.tvMBlur.setOnClickListener {
            val bnb : BottomNavigationView = requireActivity().findViewById(R.id.bnbMain)
            bnb.selectedItemId = R.id.measure
        }

        binding.tvMFilter.setOnClickListener {
            val dialog = MainFilterDialogFragment()
            dialog.show(requireActivity().supportFragmentManager, "MainFilterDialogFragment")
        }

    }

    fun setViewVisibility(emptyHistory : Boolean) {
        if (emptyHistory) {
//            binding.tvMCustomEmpty.visibility = View.VISIBLE
            binding.clMCustom.visibility = View.GONE
            binding.tvMCustom.visibility = View.GONE
            binding.tvMFilter.visibility = View.GONE
            binding.clMExerciseHistory.visibility = View.GONE

            // ------# 측정 component 크기 조절 #------
            binding.tvMMeasureTitle.visibility = View.GONE
            applyBlurToConstraintLayout(binding.clMMeasure, binding.ivMBlur)

            val lp = binding.clMMeasure.layoutParams as ConstraintLayout.LayoutParams
            lp.apply {
                height = (150 * (context?.resources?.displayMetrics?.density!!)).toInt()
            }
            binding.clMMeasure.layoutParams = lp

        } else {
            binding.clMCustom.visibility = View.VISIBLE
            binding.tvMCustom.visibility = View.VISIBLE
            binding.tvMFilter.visibility = View.VISIBLE
            binding.clMExerciseHistory.visibility = View.VISIBLE
            binding.tvMMeasureTitle.visibility = View.VISIBLE
            binding.tvMBlur.visibility = View.GONE
            val lp = binding.clMMeasure.layoutParams as ConstraintLayout.LayoutParams
            lp.apply {
                height = (200 * (context?.resources?.displayMetrics?.density!!)).toInt()
            }

        }
    }

    fun setWeeklyDrawable(ivId: String, drawableName: String) {
        val resId = resources.getIdentifier(ivId, "id", requireContext().packageName)
        val imageView = requireActivity().findViewById<ImageView>(resId)
        val drawableResId = resources.getIdentifier(drawableName, "drawable", requireContext().packageName)

        imageView.setImageResource(drawableResId)
    }

    override fun onRVClick(program: ProgramVO) {
//        val intent = Intent(requireContext(), PlayFullScreenActivity::class.java)
//        val url = storeUrl(program)
//        intent.putStringArrayListExtra("resourceList", ArrayList(url))
//        startActivityForResult(intent, 8080)

    }
//    private fun setBlurText(isBlur: Boolean) {
//        with(binding) {
//            binding.tvMBlur?.setLayerType(View.LAYER_TYPE_SOFTWARE, null).apply {
//                if (isBlur) binding.tvMBlur?.paint?.maskFilter =
//                    BlurMaskFilter(16f , BlurMaskFilter.Blur.NORMAL)
//                else binding.tvMBlur?.paint?.maskFilter = null
//            }
//        }
//    }

    private fun storeUrl(program: ProgramVO) : MutableList<String> {
        val exercises = program.exercises
        val resourceList = mutableListOf<String>()
        for (i in 0 until exercises!!.size) {
            resourceList.add(exercises[i].videoFilepath.toString())
        }
        return resourceList
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun setRVAdapter (programList: MutableList<ProgramVO>) {
//        val adapter = ProgramRVAdapter(programList, this@MainFragment, this@MainFragment,"rank", startForResult)
//        adapter.programs = programList
//        binding.rvM.adapter = adapter
//        val linearLayoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
//        binding.rvM.layoutManager = linearLayoutManager
//        adapter.notifyDataSetChanged()
    }

    private suspend fun fetchExercise(categoryId: Int, searchId: Int) : MutableList<ExerciseVO> {
        return fetchCategoryAndSearch(getString(R.string.IP_ADDRESS_t_exercise_description), categoryId, searchId)
    }

    fun applyBlurToConstraintLayout(constraintLayout: ConstraintLayout, imageView: ImageView) {
        constraintLayout.post {
            Blurry.with(constraintLayout.context)
                .radius(10)
                .sampling(2)
                .async()
                .capture(constraintLayout) // ConstraintLayout의 스크린샷을 캡처하여 블러 처리
                .into(imageView) // 블러 처리된 이미지를 ImageView에 설정
        }
        Log.v("BLured", "Blur is success")
    }
}