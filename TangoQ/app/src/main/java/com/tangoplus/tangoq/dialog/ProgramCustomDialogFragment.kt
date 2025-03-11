package com.tangoplus.tangoq.dialog

import android.annotation.SuppressLint
import android.content.DialogInterface
import android.content.Intent
import android.content.res.ColorStateList
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
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
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
import com.tangoplus.tangoq.viewmodel.ExerciseViewModel
import com.tangoplus.tangoq.viewmodel.MeasureViewModel
import com.tangoplus.tangoq.vo.ProgressUnitVO
import com.tangoplus.tangoq.viewmodel.ProgressViewModel
import com.tangoplus.tangoq.vo.ProgramVO
import com.tangoplus.tangoq.databinding.FragmentProgramCustomDialogBinding
import com.tangoplus.tangoq.dialog.bottomsheet.ProgramWeekBSDialogFragment
import com.tangoplus.tangoq.fragment.ExtendedFunctions.isFirstRun
import com.tangoplus.tangoq.function.PreferencesManager
import com.tangoplus.tangoq.listener.OnCustomCategoryClickListener
import com.tangoplus.tangoq.api.NetworkProgram.fetchProgram
import com.tangoplus.tangoq.api.NetworkProgress.getProgress
import com.tangoplus.tangoq.function.SaveSingletonManager
import com.tangoplus.tangoq.db.Singleton_t_user
import com.tangoplus.tangoq.listener.OnSingleClickListener
import com.tangoplus.tangoq.mediapipe.MathHelpers.isTablet
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class ProgramCustomDialogFragment : DialogFragment(), OnCustomCategoryClickListener {
    lateinit var binding : FragmentProgramCustomDialogBinding
    val evm : ExerciseViewModel by activityViewModels()
    private val pvm : ProgressViewModel by activityViewModels()
    private val mvm : MeasureViewModel by activityViewModels()
    private lateinit var ssm : SaveSingletonManager
    private var programSn = 0
    private var isResume = false
    private lateinit var program : ProgramVO
    private lateinit var userJson : JSONObject

    companion object {
        private const val ARG_PROGRAM_SN = "program_sn"
        private const val ARG_RECOMMENDATION_SN = "recommendation_sn"

        fun newInstance(programSn: Int, recommendationSn: Int) : ProgramCustomDialogFragment {
            val fragment = ProgramCustomDialogFragment()
            val args = Bundle()
            args.putInt(ARG_PROGRAM_SN, programSn)
            args.putInt(ARG_RECOMMENDATION_SN, recommendationSn)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)

    }
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentProgramCustomDialogBinding.inflate(inflater)
        return binding.root
    }

    override fun onResume() {
        super.onResume()
        // full Screen code
        dialog?.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog?.window?.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
        if (isResume) {
            updateUI()
        }
    }

    @SuppressLint("SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // -------# 기본 셋팅 #-------
        initVMValue()
        binding.ibtnPCDBack.setOnClickListener { dismiss() }
        // main으로 돌아갈시 업데이트
        pvm.fromProgramCustom = true

        userJson = Singleton_t_user.getInstance(requireContext()).jsonObject ?: JSONObject()
        programSn = arguments?.getInt(ARG_PROGRAM_SN) ?: -1
        pvm.recommendationSn = arguments?.getInt(ARG_RECOMMENDATION_SN) ?: -1
        ssm = SaveSingletonManager(requireContext(), requireActivity())
        isResume = false
        initializeProgram()
        lifecycleScope.launch {
            // 프로그램에 들어가는 운동
            binding.sflPCD.startShimmer()
            binding.sflPCD.visibility = View.VISIBLE


            pvm.selectedWeek.observe(viewLifecycleOwner) { selectedWeek ->

                /* week, seq든 상관없음
                * 이제 하나하나 unit이니까
                * recommendation_sn + week 까지만 받아서 쓰면 될듯?
                * 그러면 내가 week
                * */
                lifecycleScope.launch(Dispatchers.IO) {
                    val result = async { pvm.getProgressData(requireContext()) }
                    result.await()
                    withContext(Dispatchers.Main) {
                        Log.v("옵저버동작", "selectedWeek: ${pvm.selectedWeek.value}, currentWeek: ${pvm.currentWeek}, selectedSequence: ${pvm.selectedSequence.value}")
                        if (pvm.currentProgram != null && pvm.selectedSequence.value != null) {
                            val maxSeq = pvm.currentProgram?.programFrequency

                            if (selectedWeek != null) {
                                val seqProgresses = pvm.currentProgresses
                                val minSequenceInWeek = seqProgresses.minOfOrNull { it.countSet } ?: 0

                                if (minSequenceInWeek != maxSeq) {
                                    pvm.currentSequence = minSequenceInWeek // 지금 1주찬데 2주차로넘어갔을 때 0으로 가지겠지?
                                }

                                val newSequence = pvm.calculateCurrentSeq(selectedWeek) // 다른 주차일 경우 무조건 0 임. newSequence는. 이전 완료한 주차일 경우 3
                                if (newSequence == maxSeq) {
                                    pvm.selectedSequence.value = 0
                                    binding.rvPCDHorizontal.isEnabled = false
                                } else {
                                    pvm.selectedSequence.value = newSequence
                                    binding.rvPCDHorizontal.isEnabled = true
                                }
                                pvm.currentSequence = newSequence
//                                Log.v("옵저버seq존재", "프로그레스들: ${pvm.currentProgresses.map { it.progress }}, cycle: ${pvm.currentProgresses.map { it.cycleProgress }}  selectedWeek: ${pvm.selectedWeek.value}, currentWeek: ${pvm.currentWeek}, selectedSequence: ${pvm.selectedSequence.value}, newSequence: $newSequence")
                                val selectedSeqValue = pvm.selectedSequence.value
                                setAdapter(pvm.currentProgram, pvm.currentProgresses, Pair(pvm.currentSequence, selectedSeqValue))
                                val tvTotalWeek = pvm.currentProgram?.programWeek ?: 0
                                val tvWeek = pvm.selectedWeek.value
                                binding.tvPCDWeek.text = "${tvWeek?.plus(1)}/$tvTotalWeek 주차"

                                setButtonFlavor()



                                val currentSequenceProgresses = pvm.currentProgresses
                                // 가장 최근에 시작한 운동의 인덱스 찾기
                                val startIndex = findCurrentIndex(currentSequenceProgresses)
                                Log.v("startIndex찾기", "${currentSequenceProgresses.sortedBy { it.exerciseId }.map { it.isWatched }} $startIndex")
                            }
                        }
                    }
                }
            }
        }



        binding.btnPCDRight.setOnSingleClickListener {
            when (binding.btnPCDRight.text) {
                "운동 시작하기" -> {

                    // PreferencesManager 초기화 및 추천 저장
                    val prefManager = PreferencesManager(requireContext())
                    prefManager.saveLatestRecommendation(pvm.recommendationSn)
                    Log.v("마지막시청rec", "${prefManager.getLatestRecommendation()}")
                    evm.latestProgram = pvm.currentProgram

                    val currentSequenceProgresses = pvm.currentProgresses
                    // 가장 최근에 시작한 운동의 인덱스 찾기
                    val startIndex = findCurrentIndex(currentSequenceProgresses)

                    // startIndex가 유효한지 확인
                    if (startIndex >= 0 && startIndex < currentSequenceProgresses.size) {
                        Log.v("startIndex", "${startIndex}, currentSequenceProgresses: ${currentSequenceProgresses.size}")
                        val videoUrls = mutableListOf<String>()
                        val exerciseIds = mutableListOf<String>()
                        val uvpIds = mutableSetOf<String>() // 중복 제거를 위해 Set 사용

                        for (i in startIndex until currentSequenceProgresses.size) {

                            // 재생완료인 것들은 빼버리기
                            val progress = currentSequenceProgresses[i]
                            Log.v("progress", "${progress.exerciseId}, ${progress.uvpSn}")
                            if (progress.cycleProgress <= (progress.duration * 92 ) / 100) {
                                exerciseIds.add(progress.exerciseId.toString())
                                uvpIds.add(progress.uvpSn.toString())
                                videoUrls.add(program.exercises?.find { it.exerciseId == progress.exerciseId.toString() }?.videoFilepath.toString())
                            }
                        }

                        val intent = Intent(requireContext(), PlayFullScreenActivity::class.java)
                        intent.putStringArrayListExtra("video_urls", ArrayList(videoUrls))
                        intent.putStringArrayListExtra("exercise_ids", ArrayList(exerciseIds))
                        intent.putStringArrayListExtra("uvp_sns", ArrayList(uvpIds))

                        // 현재 주차, 회차 넣기
                        intent.putExtra("currentWeek", pvm.currentWeek + 1)
                        intent.putExtra("currentSeq", pvm.currentSequence + 1)
                        intent.putExtra("current_position", currentSequenceProgresses[startIndex].progress.toLong())
                        requireContext().startActivity(intent)
                        startActivityForResult(intent, 8080)
                        Log.v("인텐트담은것들", "${videoUrls}, $exerciseIds, $uvpIds")
                    } else {
                        Log.e("ExerciseProgress", "Invalid startIndex: $startIndex")
                    }
                }
                else -> { }
            }
        }
    }
    private suspend fun fetchProgramData() {
        try {
            withContext(Dispatchers.IO) {
                program = fetchProgram(getString(R.string.API_programs), requireContext(), programSn.toString()) ?: ProgramVO()
                var times = 0
                val exerciseValue = program.exercises
                if (program != ProgramVO() && exerciseValue != null) {
                    for (i in 0 until exerciseValue.size) {
                        val exerciseTime = exerciseValue[i].duration?.toInt()
                        if (exerciseTime != null) {
                            times += exerciseTime
                        }
                    }
                    program.programTime = times
                    pvm.currentProgram = program
                }
            }
        }  catch (e: IndexOutOfBoundsException) {
            Log.e("ProgramIndex", "${e.message}")
        } catch (e: IllegalArgumentException) {
            Log.e("ProgramIllegal", "${e.message}")
        } catch (e: IllegalStateException) {
            Log.e("ProgramIllegal", "${e.message}")
        }catch (e: NullPointerException) {
            Log.e("ProgramNull", "${e.message}")
        } catch (e: java.lang.Exception) {
            Log.e("ProgramException", "${e.message}")
        }
    }



    // 핵심 함수 여기서 초기, 클릭시, 보고 왔을 때, UI들이 update됨.
    private fun updateUI() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val initSeqWeek = async { calculateInitialWeekAndSequence() }
                initSeqWeek.await()
                val result = withContext(Dispatchers.IO) {
                    // 1주에 해당하는 전체 progresses를 통해서 계산.

                    endSequence()
                    // 현재 기본값 설정
                    pvm.getProgressData(requireContext())
                    // 모든 IO 작업이 완료되면 결과를 반환
                    Triple(pvm.currentSequence, pvm.currentProgram, pvm.currentProgresses)
                }

                withContext(Dispatchers.Main) {
                    val (currentSeq, currentProgram, currentProgresses) = result
                    if (currentProgram != null) { // && currentProgresses.isNotEmpty()

                        // ------# 현재 기록으로 어댑터 연걸 #------
                        /* 어댑터에는 현재 회차를 자체적으로 가져와서 넣기.
                        * */
                        if (pvm.selectedSequence.value != null) {
                            setAdapter(currentProgram, currentProgresses, Pair(currentSeq, pvm.selectedSequence.value))
                        }

                        // ------# UI 업데이트 로직 #------
                        binding.etPCDTitle.isEnabled = false
                        binding.btnPCDRight.text = "운동 시작하기"

                        binding.etPCDTitle.setText(currentProgram.programName)
//                        Log.v("시간", "programTime: ${currentProgram.programTime}")
                        binding.tvPCDTime.text = if (currentProgram.programTime <= 60) {
                            "${currentProgram.programTime}초"
                        } else {
                            "${currentProgram.programTime / 60}분 ${currentProgram.programTime % 60}초"
                        }
                        binding.tvPCDStage.text = when (currentProgram.programStage) {
                            "1" -> "초급자"
                            "2" -> "중급자"
                            "3" -> "상급자"
                            else -> ""
                        }
                        binding.tvPCDCount.text = "총 ${currentProgram.programCount} 개"
                        CoroutineScope(Dispatchers.Main).launch {
                            setButtonFlavor()
                        }
                    } else {
                        Log.e("Error", "Some required data is null or empty, program: ${currentProgram?.exercises?.map { it.exerciseName }}, seq: $currentSeq, size: ${currentProgresses.size}")
                        // 에러 처리 로직 추가
                    }
                    binding.sflPCD.stopShimmer()
                    binding.sflPCD.visibility = View.GONE
                    showBalloon()
                    isResume = true
                    Log.v("현재IsResume", "isResume: $isResume")
                    binding.tvPCDWeek.setOnClickListener {
                        val dialog = ProgramWeekBSDialogFragment()
                        dialog.show(requireActivity().supportFragmentManager, "ProgramWeekBSDialogFragment")
                    }
                }
            } catch (e: IndexOutOfBoundsException) {
                Log.e("ProgramIndex", "${e.printStackTrace()}")
            } catch (e: IllegalArgumentException) {
                Log.e("ProgramIllegal", "${e.message}")
            } catch (e: IllegalStateException) {
                Log.e("ProgramIllegal", "${e.message}")
            } catch (e: NullPointerException) {
                Log.e("ProgramNull", "${e.message}")
            } catch (e: java.lang.Exception) {
                Log.e("ProgramException", "${e.printStackTrace()}")
            }
        }
    }

    private fun initializeProgram() {
        lifecycleScope.launch {
            fetchProgramData() // 프로그램 데이터 한 번만 가져오기
            updateUI()  // UI 업데이트는 필요할 때마다 호출
        }
    }

    private fun setAdapter(program: ProgramVO?, progresses : MutableList<ProgressUnitVO>?, sequence: Pair<Int, Int?>) {
        /* sequence = Pair(현재 회차(currentSeq), 선택한 회차 (selectedSeq))
        currentSequence 는 진행중인 주차, 진행중인 회차, 선택된 회차 이렇게 나눠짐 */
        pvm.selectedSequence.value = sequence.second
        val selectSeqValue = pvm.selectedSequence.value
        val frequency = program?.programFrequency
        val adapter : ProgramCustomRVAdapter
        val selectedWeekValue = pvm.selectedWeek.value

        if (selectSeqValue != null && frequency != null && selectedWeekValue != null && sequence.second != null) {

            adapter = ProgramCustomRVAdapter(this@ProgramCustomDialogFragment,
                Triple(frequency, pvm.currentSequence, selectSeqValue),
                Pair(pvm.currentWeek, selectedWeekValue),
                pvm.seqHpvs,
                this@ProgramCustomDialogFragment)

            val layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
            binding.rvPCDHorizontal.layoutManager = layoutManager
            binding.rvPCDHorizontal.adapter = adapter
            adapter.notifyDataSetChanged()
            val adapter2 = program.exercises?.let { ExerciseRVAdapter(this@ProgramCustomDialogFragment, it, progresses?.sortedBy { it.exerciseId }?.toMutableList(), null, sequence, "PCD") }

            binding.rvPCD.adapter = adapter2
            val layoutManager2 = LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
            binding.rvPCD.layoutManager = layoutManager2
            adapter2?.notifyDataSetChanged()
        }
    }

    override fun customCategoryClick(sequence: Int) {
        if (pvm.selectedWeek.value == pvm.currentWeek) {
            pvm.selectedSequence.value = sequence
            CoroutineScope(Dispatchers.IO).launch {
                pvm.getProgressData(requireContext())
                withContext(Dispatchers.Main) {
                    //            Log.v("historys", "selectedSequence : ${pvm.selectedSequence.value}, currentSequence: ${pvm.currentSequence}")
                    val selectedWeekValue = pvm.selectedWeek.value
                    val selectedSeqValue = pvm.selectedSequence.value
                    if (selectedWeekValue != null && selectedSeqValue != null) {
                        Log.v("회차들", "selectedWeekValue: $selectedWeekValue, selectedSeqValue: $selectedSeqValue, VM.selectedSeq: ${pvm.selectedSequence.value}, VM.currentSeq: ${pvm.currentSequence}")
                        setAdapter(pvm.currentProgram, pvm.currentProgresses, Pair(pvm.currentSequence, selectedSeqValue))
                        setButtonFlavor()
                    }
                }
            }
        }
    }
    private fun setButtonFlavor() {
        val selectedWeekValue = pvm.selectedWeek.value
        val selectedSeqValue = pvm.selectedSequence.value
        if (selectedWeekValue != null && pvm.currentProgresses.isNotEmpty()) {
            // 현재 week에서 벗어남
            if (selectedWeekValue > pvm.currentWeek) {
                (binding.rvPCD.adapter as ExerciseRVAdapter).setTouchLocked(true)
                binding.btnPCDRight.apply {
                    isEnabled = false
                    backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.subColor150))
                    text = "이번주 운동을 진행해주세요"
                }
                // 현재 주차 ok > 현재 회차 벗어남
            } else if (selectedSeqValue != null &&  selectedSeqValue < pvm.currentSequence) {
                // rv와 버튼 잠금
                (binding.rvPCD.adapter as ExerciseRVAdapter).setTouchLocked(true)
                binding.btnPCDRight.apply {
                    isEnabled = false
                    backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.subColor150))
                    text = "오늘자 운동을 진행해주세요"
                }
                // 오늘자인데.
            }  else if (selectedSeqValue == pvm.currentSequence  && pvm.dailySeqFinished) {
                binding.btnPCDRight.apply {
                    isEnabled = false
                    backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.subColor150))
                    text = "오늘 필요한 운동을 완료했습니다"
                }
            } else if (selectedSeqValue == pvm.currentSequence) {
                binding.btnPCDRight.apply {
                    isEnabled = true
                    backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.mainColor))
                    text = "운동 시작하기"
                }
            }
        }
    }
    fun dismissThisFragment() {
        dismiss()
    }

    // 해당 회차에서 가장 최근 운동의 index찾아오기 ( 한 seq의 묶음만 들어옴 )
    private fun findCurrentIndex(progresses: MutableList<ProgressUnitVO>) : Int {
        // Case 1 시청 중간 기록이 있을 경우
//        val progressIndex1 = progresses.indexOfLast { it.progress in 1 until it.duration }
//        if (progressIndex1 != -1) {
//            return progressIndex1
//        }
        val sortedProgresses = progresses.sortedBy { it.exerciseId }
        val progressItem = sortedProgresses.firstOrNull { it.isWatched == 0 } ?: -1
        val progressIndex1 = sortedProgresses.indexOf(progressItem)
        if (progressIndex1 != -1) {
            return progressIndex1
        } else if (progresses.all { it.isWatched == 0 }) {
            Log.v("가장 최근Index", "0")
            return 0
        }  else {
            return -1
        }
        // Case 2 다 보고 난 뒤 완료 처리가 된 후 (currentSequence가 1이 됐을 때) 그 다음 index 반환
//        val progressIndex = progresses.indexOfLast { it.countSet - 1 == pvm.currentSequence }
//        Log.v("프로그레스", "$progressIndex, currentSeq: ${pvm.currentSequence}")
//        if (progressIndex != progresses.size) {
//            Log.v("가장 최근Index", "${progressIndex +  1}")
//            return progressIndex + 1
//        }

        // Case 3 초기상태


    }

    private fun endSequence() {
        if (pvm.currentProgresses.isNotEmpty()) {
            val currentSeqFinished = pvm.currentProgresses.map { it.isWatched == 1 }.all { it }
//            Log.v("endSeq?", "currentWeek: ${pvm.currentWeek}, currentSeq: ${pvm.currentSequence}, ${currentSeqFinished}")
            if (pvm.currentWeek== 3 && pvm.currentSequence == 2 && currentSeqFinished) {
                val dialog = ProgramAlertDialogFragment.newInstance(this, 1)
                dialog.show(requireActivity().supportFragmentManager, "ProgramAlertDialogFragment")

                // 버튼 잠금
                setButtonFlavor()
            }
        }
    }

    private suspend fun calculateInitialWeekAndSequence() {
        withContext(Dispatchers.IO) {
            val jo = JSONObject().apply {
                put("recommendation_sn", pvm.recommendationSn)
                put("exercise_program_sn", programSn)
                put("server_sn", mvm.selectedMeasure?.sn)
            }
            Log.v("json>Progress", "$jo")

            getProgress(getString(R.string.API_progress), jo, requireContext()) { (historySn, week, seq), result ->
                CoroutineScope(Dispatchers.Main).launch {  // LiveData 업데이트는 메인 스레드에서
                    val adjustedWeek = if (week == -1) 0 else week - 1
                    val adjustedSeq = if (seq == -1) 0 else seq - 1
                    val countSets = result[adjustedWeek].map { it.countSet }
                    val isMaxSeq = countSets.map { it == 3 }.all { it }
                    val isMinSeq = countSets.min()
                    val isSeqFinish = countSets.distinct().size == 1
                    Log.v("initPostPg", "($adjustedWeek, $adjustedSeq, $isMaxSeq, $isMinSeq, $isSeqFinish) result: ${result[adjustedWeek].map { it.countSet }}")

                    // 전부 봤는지 + 오늘 날짜 인지 + 현재 seq인지 + 현재 week인지?
                    // 이 곳에서는 isWatched가 존재하지 않음. "seq"와 "count_set"이 전부 일치하는지 + 오늘 날짜 인건지? 확인해야 함

                    val isAllFinish = countSets.map { it == seq }.all { it }

                    val rightNow = LocalDate.now()
                    if (pvm.currentProgresses.isNotEmpty()) {
                        val recentUpdatedAt = pvm.currentProgresses.sortedByDescending { it.updatedAt }[0].updatedAt
                        Log.v("recentUpdateAt", "$recentUpdatedAt")
                        val recentUpdateDate = if (!recentUpdatedAt.isNullOrBlank() && recentUpdatedAt != "null") {
                            LocalDate.parse(recentUpdatedAt, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                        } else {
                            LocalDate.now()
                        }
//                        Log.v("currentValid", "${pvm.currentProgresses.map { it.isWatched }}, $rightNow $recentUpdateDate, ${rightNow == recentUpdateDate}")

                        if (isAllFinish && rightNow == recentUpdateDate) {
                            pvm.dailySeqFinished = true
                            val dialog = ProgramAlertDialogFragment.newInstance(this@ProgramCustomDialogFragment, 1)
                            dialog.show(requireActivity().supportFragmentManager, "ProgramAlertDialogFragment")
                        }
                    }

                    if (isMaxSeq && isSeqFinish) {
                        // 모든 회차가 끝남 ( 주차가 넘어가야하는 상황 )
                        pvm.currentWeek = adjustedWeek + 1
                        pvm.selectWeek.value = adjustedWeek + 1
                        pvm.selectedWeek.value = adjustedWeek + 1
                        pvm.currentSequence = 0
                        pvm.selectedSequence.value = 0
                        // 회차 진행중
                    } else if (!isMaxSeq && isSeqFinish && isMinSeq > 0) {
                        pvm.currentWeek = adjustedWeek
                        pvm.selectWeek.value = adjustedWeek
                        pvm.selectedWeek.value = adjustedWeek
                        pvm.currentSequence = adjustedSeq + 1
                        pvm.selectedSequence.value= adjustedSeq + 1
                        // 시청 중간
                    } else if (isMinSeq > 0) {
                        pvm.currentWeek = adjustedWeek
                        pvm.selectWeek.value = adjustedWeek
                        pvm.selectedWeek.value = adjustedWeek
                        pvm.currentSequence = adjustedSeq
                        pvm.selectedSequence.value= adjustedSeq
                    }else {
                        pvm.currentWeek = adjustedWeek
                        pvm.selectWeek.value = adjustedWeek
                        pvm.selectedWeek.value = adjustedWeek
                        pvm.currentSequence = adjustedSeq
                        pvm.selectedSequence.value= adjustedSeq
                    }
                    Log.v("초기WeekSeq", "selectedWeek: ${pvm.selectedWeek.value} selectWeek: ${pvm.selectWeek.value}, currentWeek: ${pvm.currentWeek}, currentSeq: ${pvm.currentSequence}, selectedSequence: ${pvm.selectedSequence.value}")
                }
            }
        }
    }

    private fun showBalloon() {
        val balloon2 = Balloon.Builder(requireContext())
            .setWidth(BalloonSizeSpec.WRAP)
            .setHeight(BalloonSizeSpec.WRAP)
            .setText("주차 요일에 맞추어 운동을 진행합니다.\n프로그램을 한 주동안 원하는 날짜에 진행해보세요")
            .setTextColorResource(R.color.white)
            .setTextSize(if (isTablet(requireContext())) 20f else 18f)
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
            Handler(Looper.getMainLooper()).postDelayed({
                binding.ibtnPCDTop.showAlignBottom(balloon2)
                balloon2.dismissWithDelay(1800L)
            }, 700)
        }
        binding.ibtnPCDTop.setOnClickListener { it.showAlignBottom(balloon2) }
    }
    private fun View.setOnSingleClickListener(action: (v: View) -> Unit) {
        val listener = View.OnClickListener { action(it) }
        setOnClickListener(OnSingleClickListener(listener))
    }

    private fun initVMValue() {
        pvm.selectWeek.value = null
        pvm.selectedSequence.value  =null
        pvm.currentWeek = 0
        pvm.currentSequence = 0
        pvm.currentProgresses.clear()
        pvm.recommendationSn = 0
        pvm.selectedWeek.value = null
        pvm.dailySeqFinished = false
    }
}