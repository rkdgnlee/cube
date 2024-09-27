package com.tangoplus.tangoq.dialog

import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.skydoves.balloon.ArrowPositionRules
import com.skydoves.balloon.Balloon
import com.skydoves.balloon.BalloonAnimation
import com.skydoves.balloon.BalloonSizeSpec
import com.skydoves.balloon.showAlignBottom
import com.tangoplus.tangoq.PlayFullScreenActivity
import com.tangoplus.tangoq.R
import com.tangoplus.tangoq.adapter.ProgramCustomRVAdapter
import com.tangoplus.tangoq.adapter.ExerciseRVAdapter
import com.tangoplus.tangoq.data.HistoryVO
import com.tangoplus.tangoq.data.ExerciseVO
import com.tangoplus.tangoq.data.ExerciseViewModel
import com.tangoplus.tangoq.data.HistoryUnitVO
import com.tangoplus.tangoq.data.HistoryViewModel
import com.tangoplus.tangoq.data.ProgramVO
import com.tangoplus.tangoq.databinding.FragmentProgramCustomDialogBinding
import com.tangoplus.tangoq.db.PreferencesManager
import com.tangoplus.tangoq.fragment.isFirstRun
import com.tangoplus.tangoq.listener.OnCustomCategoryClickListener
import com.tangoplus.tangoq.`object`.NetworkProgram.fetchProgramVOBySn
import com.tangoplus.tangoq.`object`.Singleton_t_history
import com.tangoplus.tangoq.`object`.Singleton_t_user
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import kotlin.random.Random

class ProgramCustomDialogFragment : DialogFragment(), OnCustomCategoryClickListener {
    lateinit var binding : FragmentProgramCustomDialogBinding
    val viewModel : ExerciseViewModel by activityViewModels()
    val hViewModel : HistoryViewModel by activityViewModels()
    private var historys =  HistoryVO("-1", false, mutableListOf())
    private lateinit var  singletonHistory : Singleton_t_history

    var programSn = 0
    private lateinit var program : ProgramVO
    companion object {
        private const val ARG_PROGRAM_SN = "program_sn"
        fun newInstance(programSn: Int) : ProgramCustomDialogFragment {
            val fragment = ProgramCustomDialogFragment()
            val args = Bundle()
            args.putInt(ARG_PROGRAM_SN, programSn)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentProgramCustomDialogBinding.inflate(inflater)
        return binding.root
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // -------# 기본 셋팅 #-------
        val userJson = Singleton_t_user.getInstance(requireContext()).jsonObject
        programSn = arguments?.getInt(ARG_PROGRAM_SN)!!

        CoroutineScope(Dispatchers.IO).launch {
            program = fetchProgramVOBySn(getString(R.string.IP_ADDRESS_t_exercise_programs), programSn.toString())
            hViewModel.currentProgram = program

            if (hViewModel.currentProgram?.programSn != -1) {
                binding.etPCDTitle.isEnabled = false
                binding.btnPCDRight.text = "운동 시작하기"
                binding.btnPCDLeft.visibility = View.GONE
                // ------# select program으로 필터링 했을 때 #------
            } else if (hViewModel.currentProgram?.programSn == -1) {
                binding.btnPCDRight.text = "프로그램 선택하기"
                binding.ibtnPCDTop.setImageDrawable(resources.getDrawable(R.drawable.icon_edit))
                binding.etPCDTitle.isEnabled = true
                binding.btnPCDLeft.visibility = View.VISIBLE
            }
        }

        // ------! 요약 시작 !------
        binding.etPCDTitle.setText(hViewModel.currentProgram?.programName)

        binding.tvPCDTime.text = (if (hViewModel.currentProgram?.programTime!! <= 60) {
            "${hViewModel.currentProgram?.programTime}초"
        } else {
            "${hViewModel.currentProgram?.programTime!! / 60 }분 ${hViewModel.currentProgram?.programTime!! % 60}초"
        }).toString()
        when (hViewModel.currentProgram?.programStage) {
            "초급" -> binding.tvPCDStage.text = "초급자"
            "중급" -> binding.tvPCDStage.text = "중급자"
            "고급" -> binding.tvPCDStage.text = "상급자"
        }
        binding.tvPCDCount.text = "${hViewModel.currentProgram?.programCount} 개"

        val userSn = userJson?.optString("user_sn").toString()
        val prefsManager = PreferencesManager(requireContext())
        // ------! 요약 끝 !------

        // ------# 회차 데이터 넣는 곳 #------
        singletonHistory = Singleton_t_history.getInstance(requireContext())
        historys = singletonHistory.historys?.find { it.programId == programSn.toString() }!!
        Log.v("싱글턴historys2", "${singletonHistory.historys}")

        // 하나 밖에 없음 10번이고, 그걸 가져온 상태임. 현재 10번 타고 들어왔어. 들어왔으니 program도 가져오고
        // ------! 현재 시점을 가져와야기 떄문에 날짜 계산은 필요 없음 !-------
        setAdapter(hViewModel.currentProgram!!, historys.doingExercises[hViewModel.currentEpisode], hViewModel.currentEpisode) // 마지막 episode 자리에 기록이 들어가야 함.
        hViewModel.currentEpisode = historys.doingExercises.size - 1

// ------# history 더미 데이터 #------
        // 기존 데이터 초기화
        val weekEpisodes = mutableListOf<MutableList<HistoryUnitVO>>()
        // ------# 운동 기록 #------
        for (weekIndex in 0 until (hViewModel.currentProgram?.programWeek!!)) {
            for (episodeIndex in 0 until hViewModel.currentProgram!!.programFrequency) {
                val historyUnits = mutableListOf<HistoryUnitVO>()
                when (weekIndex) { // 주차
                    0 -> {
                        when (episodeIndex) { // 회차
                            0 -> { // 회차 하나에 들어가는 ExerciseUnit
                                for (k in 0 until (hViewModel.currentProgram?.exercises?.size ?: 0)) {
                                    val historyUnit = HistoryUnitVO(
                                        hViewModel.currentProgram?.exercises?.get(k)?.exerciseId,
                                        0,
                                        "2024-08-26 16:00:24" // TODO 실제 데이터는 각각의 regDate가 다름
                                    )
                                    historyUnits.add(historyUnit)
                                }
                            }
                            1 -> {
                                for (k in 0 until (hViewModel.currentProgram?.exercises?.size ?: 0)) {
                                    val historyUnit = HistoryUnitVO(
                                        hViewModel.currentProgram?.exercises?.get(k)?.exerciseId,
                                        0,
                                        "2024-08-27 17:51:24"
                                    )
                                    historyUnits.add(historyUnit)
                                }
                            }
                            2 -> {
                                for (k in 0 until (hViewModel.currentProgram?.exercises?.size ?: 0)) {
                                    val historyUnit = HistoryUnitVO(
                                        hViewModel.currentProgram?.exercises?.get(k)?.exerciseId,
                                        0,
                                        "2024-08-29 18:26:24"
                                    )
                                    historyUnits.add(historyUnit)
                                }
                            }
                            else -> {
                                for (k in 0 until 1) {
                                    val historyUnit = HistoryUnitVO(
                                        hViewModel.currentProgram?.exercises?.get(k)?.exerciseId,
                                        0,
                                        "2024-08-31 20:11:57"
                                    )
                                    historyUnits.add(historyUnit)
                                }
                                val historyUnit = HistoryUnitVO(
                                    hViewModel.currentProgram?.exercises?.get(4)?.exerciseId,
                                    Random.nextInt(0, hViewModel.currentProgram?.exercises?.get(4)?.videoDuration!!.toInt()),
                                    "2024-08-31 20:13:57"
                                )
                                historyUnits.add(historyUnit)
                            }
                        }

                    }
//                        1 -> { // 2주차
//                            when (episodeIndex) { // 회차
//                                0 -> {
//                                    for (k in 0 until (hViewModel.currentProgram?.exercises?.size!! - 1)) {
//                                        val historyUnit = HistoryUnitVO(
//                                            hViewModel.currentProgram?.exercises?.get(k)?.exerciseId,
//                                            0,
//                                            "2024-09-02 22:54:12"
//                                        )
//                                        historyUnits.add(historyUnit)
//                                    }
//                                    val historyUnit = HistoryUnitVO(
//                                        hViewModel.currentProgram?.exercises?.get(6)?.exerciseId,
//                                        0,
//                                        null
//                                        )
//                                    historyUnits.add(historyUnit)
//
//                                }
//                                1 -> {
//                                    for (k in 0 until (hViewModel.currentProgram?.exercises?.size ?: 0)) {
//                                        val historyUnit = HistoryUnitVO(
//                                            hViewModel.currentProgram?.exercises?.get(k)?.exerciseId,
//                                            0,
//                                            "2024-09-03 20:39:44"
//                                        )
//                                        historyUnits.add(historyUnit)
//                                    }
//                                }
//                                2 -> {
//                                    for (k in 0 until 3) {
//                                        val historyUnit = HistoryUnitVO(
//                                            hViewModel.currentProgram?.exercises?.get(k)?.exerciseId,
//                                            0,
//                                            "2024-09-04 17:54:24"
//                                        )
//                                        historyUnits.add(historyUnit)
//                                    }
//                                    val historyUnit = HistoryUnitVO(
//                                        hViewModel.currentProgram?.exercises?.get(3)?.exerciseId,
//                                        Random.nextInt(0, hViewModel.currentProgram?.exercises?.get(3)?.videoDuration!!.toInt()),
//                                        "2024-09-04 19:24:11"
//                                    )
//                                    historyUnits.add(historyUnit)
//                                    for (k in 3 until 7) {
//                                        val historyUnit = HistoryUnitVO(
//                                            hViewModel.currentProgram?.exercises?.get(k)?.exerciseId,
//                                            0,
//                                            null
//                                        )
//                                        historyUnits.add(historyUnit)
//                                    }
//                                }
//                                else -> {
//                                    for (k in 0 until (hViewModel.currentProgram?.exercises?.size ?: 0)) {
//                                        val historyUnit = HistoryUnitVO(
//                                            hViewModel.currentProgram?.exercises?.get(k)?.exerciseId,
//                                            0,
//                                            null
//                                        )
//                                        historyUnits.add(historyUnit)
//                                    }
//                                }
//                            }
//                        }
//                        else -> { // 나머지 주차 들
//                            for (k in 0 until (hViewModel.currentProgram?.exercises?.size ?: 0)) {
//                                val historyUnit = HistoryUnitVO(
//                                    hViewModel.currentProgram?.exercises?.get(k)?.exerciseId,
//                                    0,
//                                    null
//                                    )
//                                historyUnits.add(historyUnit)
//                            }
//                        }
                }
                weekEpisodes.add(historyUnits)
            }
        }
        val historyVO = HistoryVO(
            "10",
            false,
            weekEpisodes
        )
        singletonHistory = Singleton_t_history.getInstance(requireContext())
        singletonHistory.historys?.add(historyVO)

        //
        for (i in 0 until historys.doingExercises.size) {
            val historyUnit = historys.doingExercises[i]
            for (j in 0 until historyUnit.size) {
                // 하나의 하루간 운동량 만들고 넣고 초기화
                var regDate = ""
                var progressTime = 0
                var finishedExercise = 0

                regDate = historyUnit[j].regDate.toString()
                if (historyUnit[j].lastPosition!! > 0) {
                    progressTime += historyUnit[j].lastPosition!!

                } else if (historyUnit[j].lastPosition == 0 && historyUnit[j].regDate != null) {
                    progressTime += getTime(historyUnit[j].exerciseId.toString())
                    finishedExercise += 1
                }

                hViewModel.classifiedByDay?.add(Triple(regDate, progressTime, finishedExercise))
            }
        }
        // 전체 반복문 빠져나옴

        // ------# 그래프에 들어갈 가장 최근 일주일간 데이터 가져오기 #------
        hViewModel.weeklyHistorys = getWeeklyExerciseHistory(hViewModel.classifiedByDay)

        // ------# dates만 넣기(달력에 들어갈 것들) #------
        val historysUntilToday = hViewModel.classifiedByDay?.filter { it.first != "null" }
        for (i in 0 until historysUntilToday?.size!!) {
            hViewModel.datesClassifiedByDay.add(stringToLocalDate(historysUntilToday[i].first))
        }



        binding.btnPCDLeft.setOnClickListener { dismiss() }
        binding.btnPCDRight.setOnClickListener {
            when (binding.btnPCDRight.text) {
                "운동 시작하기" -> {
                    val videoUrls = storeUrls(hViewModel.currentProgram?.exercises!!)
                    val exerciseIds = hViewModel.currentProgram?.exercises!!.filter { exercise ->
                        val relevantHistories = historys.doingExercises[hViewModel.selectedEpisode.value!!].filter { history ->
                            history.exerciseId == exercise.exerciseId
                        }

                        relevantHistories.any { it.regDate == null } || relevantHistories.any { it.lastPosition!! > 0 }
                    }.map { it.exerciseId }.toMutableList()
                    Log.v("안본 것들만 filter", "${exerciseIds}")

                    val intent = Intent(requireContext(), PlayFullScreenActivity::class.java)
                    intent.putStringArrayListExtra("video_urls", ArrayList(videoUrls))
                    intent.putStringArrayListExtra("exercise_ids", ArrayList(exerciseIds))
                    intent.putExtra("total_time", hViewModel.currentProgram?.programTime)

                    requireContext().startActivity(intent)
                    startActivityForResult(intent, 8080)
                }
                else -> {
                    // TODO 사용자의 선택된 프로그램 저장 후 보여주기. programVO
                }
            }
        }

        val balloon2 = Balloon.Builder(requireContext())
            .setWidthRatio(0.5f)
            .setHeight(BalloonSizeSpec.WRAP)
            .setText("주차 요일에 맞추어 운동을 진행합니다.")
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

        if (isFirstRun("CustomExerciseDialogFragment_isFirstRun")) {
            binding.ibtnPCDTop.showAlignBottom(balloon2)
            balloon2.dismissWithDelay(1800L)
        }


        binding.ibtnPCDTop.setOnClickListener { it.showAlignBottom(balloon2) }


    }


    override fun onResume() {
        super.onResume()
        // full Screen code
        dialog?.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog?.window?.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
    }

    private fun storeUrls(currentItem : MutableList<ExerciseVO>) : MutableList<String> {
        val urls = mutableListOf<String>()
        for (i in currentItem.indices) {
            val exercise = currentItem[i]
            urls.add(exercise.videoFilepath.toString())
        }
        Log.v("urls", "${urls}")
        return urls
    }

    private fun getTime(id: String) : Int {
        return hViewModel.currentProgram?.exerciseTimes?.find { it.first == id }?.second!!
    }
    private fun storeSns(currentItem: MutableList<ExerciseVO>) : MutableList<String> {
        val sns = mutableListOf<String>()
        for (i in currentItem.indices) {
            val exercises = currentItem[i]
            sns.add(exercises.exerciseId.toString())
        }
        Log.v("sns", "$sns")
        return sns
    }
    private fun getWeeklyExerciseHistory(data: MutableList<Triple<String, Int, Int>>?): MutableList<Triple<String, Int, Int>>? {
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        val currentDateTime = LocalDateTime.now(ZoneId.of("Asia/Seoul"))
        val sevenDaysAgo = currentDateTime.minusDays(6).truncatedTo(ChronoUnit.DAYS)

        val filteredData = data?.filter {
            it.first != "null" &&
                    LocalDateTime.parse(it.first, formatter).isAfter(sevenDaysAgo.minusSeconds(1)) &&
                    LocalDateTime.parse(it.first, formatter).isBefore(currentDateTime.plusDays(1).truncatedTo(
                        ChronoUnit.DAYS))
        }?.toMutableList()

        val completeData = mutableListOf<Triple<String, Int, Int>>()
        for (i in 0..6) {
            val date = sevenDaysAgo.plusDays(i.toLong())
            val nextDate = date.plusDays(1)

            val entry = filteredData?.find {
                val entryDateTime = LocalDateTime.parse(it.first, formatter)
                entryDateTime.isAfter(date.minusSeconds(1)) && entryDateTime.isBefore(nextDate)
            }
            if (entry != null) {
                completeData.add(entry)
            } else {
                // 빈 데이터의 경우 해당 날짜의 자정(00:00:00)으로 설정
                val dateString = date.format(formatter)
                completeData.add(Triple(dateString, 0, 0))
            }
        }
        Log.v("completedData", "$completeData")
        return completeData
    }

    private fun stringToLocalDate(dateTimeString: String): LocalDate {
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        val localDateTime = LocalDateTime.parse(dateTimeString, formatter)
        return localDateTime.toLocalDate()
    }

    private fun setAdapter(program: ProgramVO, historys : MutableList<HistoryUnitVO>, episode: Int) {

        /* currentEpisode 는 진행중인 주차, 진행중인 회차, 선택된 회차 이렇게 나눠짐 */
        hViewModel.selectedEpisode.value = episode
        val adapter = ProgramCustomRVAdapter(this@ProgramCustomDialogFragment, program.programWeek * program.programFrequency, historys,  Pair(hViewModel.currentEpisode, hViewModel.selectedEpisode.value!!), this@ProgramCustomDialogFragment)
        val layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        binding.rvPCDHorizontal.layoutManager = layoutManager
        binding.rvPCDHorizontal.adapter = adapter
        adapter.notifyDataSetChanged()

        val adapter2 = ExerciseRVAdapter(this@ProgramCustomDialogFragment, program.exercises!!, historys, "main")
        binding.rvPCD.adapter = adapter2
        val layoutManager2 = LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
        binding.rvPCD.layoutManager = layoutManager2

        adapter2.notifyDataSetChanged()
    }

    override fun customCategoryClick(episode: Int) {
        // TODO viewModel에 담은 program에서 필터링해서 보여주기
        hViewModel.selectedEpisode.value = episode
        Log.v("historys", "selectEpisode :${hViewModel.selectedEpisode.value}, currentEpisode: ${hViewModel.currentEpisode} position : $episode")
        setAdapter(hViewModel.currentProgram!!, historys.doingExercises[episode], hViewModel.selectedEpisode.value!!)
    }

    fun dismissThisFragment() {
        dismiss()
    }
}