package com.tangoplus.tangoq.dialog

import android.annotation.SuppressLint
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
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
import com.tangoplus.tangoq.fragment.ExtendedFunctions.setOnSingleClickListener
import com.tangoplus.tangoq.vision.MathHelpers.isTablet
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.coroutines.resume
import androidx.core.graphics.drawable.toDrawable

class ProgramCustomDialogFragment : DialogFragment(), OnCustomCategoryClickListener {
    lateinit var binding : FragmentProgramCustomDialogBinding
    private val evm : ExerciseViewModel by activityViewModels()
    private val pvm : ProgressViewModel by activityViewModels()
    private val mvm : MeasureViewModel by activityViewModels()
    private lateinit var ssm : SaveSingletonManager
    private var programSn = 0
    private var isResume = false
    private lateinit var program : ProgramVO
    private lateinit var userJson : JSONObject
    private var dailyFinishDialog : ProgramAlertDialogFragment? = null
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

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentProgramCustomDialogBinding.inflate(inflater)
        return binding.root
    }

    override fun onResume() {
        super.onResume()
        // full Screen code
        dialog?.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        dialog?.window?.setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
        dialog?.window?.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
        if (isResume) {
            updateUI()
        }
    }

    /* 1. program 데이터 가져오기 + 마지막 운동 기록 가져오기
    *  2. 마지막 운동 기록에서 현재 week 판단
    *  3. week -> 관찰을 통해 seq 계산
    *  4. 해당 exerciseItem들과 결합해서 표출
    *  5. 프로그램 줃간 실행 시 
    * */

    @SuppressLint("SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // api35이상 화면 크기 조절
        ViewCompat.setOnApplyWindowInsetsListener(view) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            // 상태 표시줄 높이만큼 상단 패딩 적용
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        // -------# 기본 셋팅 #-------
        initVMValue()

        // 어떤 화면을 이리저리 하다가 현재 server_sn 안맞아서 server_sn은 맘ㅈ는데 recommendation이 옛날꺼라
        binding.clPCD.visibility = View.GONE
        binding.ibtnPCDBack.setOnClickListener { dismiss() }
        // main으로 돌아갈시 업데이트
        pvm.fromProgramCustom = true

        userJson = Singleton_t_user.getInstance(requireContext()).jsonObject ?: JSONObject()
        programSn = arguments?.getInt(ARG_PROGRAM_SN) ?: -1
        pvm.recommendationSn = arguments?.getInt(ARG_RECOMMENDATION_SN) ?: -1
        ssm = SaveSingletonManager(requireContext(), requireActivity())
        isResume = false

        dailyFinishDialog = ProgramAlertDialogFragment.newInstance(this@ProgramCustomDialogFragment, 1)

        // 프로그램에 들어가는 운동
        binding.sflPCD.startShimmer()
        binding.sflPCD.visibility = View.VISIBLE

        // 초기값 가져오기
        lifecycleScope.launch {
            val result = async {
                fetchProgramData() // 프로그램 데이터 한 번만 가져오기
                calculateCurrentWeek()
            }
            result.await()
            pvm.selectedWeek.observe(viewLifecycleOwner) {
                updateUI()
            }
        }

        binding.btnPCDRight.setOnSingleClickListener {
            when (binding.btnPCDRight.text) {
                "운동 시작하기" -> {

                    // PreferencesManager 초기화 및 추천 저장 -> Main에서 쓰는 용도
                    val prefManager = PreferencesManager(requireContext())
                    prefManager.saveLatestRecommendation(pvm.recommendationSn)
//                    Log.w("마지막시청rec", "${prefManager.getLatestRecommendation()}")
                    evm.latestProgram = pvm.currentProgram

                    val currentSequenceProgresses = pvm.currentProgresses
                    // 가장 최근에 시작한 운동의 인덱스 찾기
                    val startIndex = findCurrentIndex(currentSequenceProgresses)

                    // startIndex가 유효한지 확인
                    if (startIndex >= 0 && startIndex < currentSequenceProgresses.size) {
//                        Log.w("startIndex", "${startIndex}, currentSequenceProgresses: ${currentSequenceProgresses.size}")
                        val videoUrls = mutableListOf<String>()
                        val exerciseIds = mutableListOf<String>()
                        val uvpIds = mutableSetOf<String>() // 중복 제거를 위해 Set 사용

                        for (i in startIndex until currentSequenceProgresses.size) {
                            // 재생완료인 것들은 빼버리기
                            val progress = currentSequenceProgresses[i]
                            Log.w("progress", "${progress.exerciseId}, ${progress.uvpSn}")
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
                        intent.putExtra("current_position", currentSequenceProgresses[startIndex].cycleProgress.toLong())
                        requireContext().startActivity(intent)

                        startActivityForResult(intent, 8080)
                        Log.w("인텐트담은것들", "${videoUrls}, $exerciseIds, $uvpIds")
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
                    val result = async { pvm.currentProgram = program }
                    result.await()
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
                // 1주에 해당하는 전체 progresses를 통해서 계산.
                val result = withContext(Dispatchers.IO) {
                    calculateCurrentSeq()

                    val recentData = async { pvm.getProgressData(requireContext()) }
                    recentData.await()
                    Triple(pvm.currentSequence, pvm.currentProgram, pvm.currentProgresses)
                }

                withContext(Dispatchers.Main) {
                    val (currentSeq, currentProgram, currentProgresses) = result
                    if (currentProgram != null) {

                        // ------# 현재 기록으로 어댑터 연걸 #------
                        /* 어댑터에는 현재 회차를 자체적으로 가져와서 넣기.
                        * */
                        if (pvm.selectedSequence.value != null) {
                            Log.w("업데이트UI", "어댑터갱신전: ${pvm.currentProgresses.map { it.cycleProgress }}, ${currentSeq}, ${pvm.selectedSequence.value}")
                            setAdapter(currentProgram, currentProgresses, Pair(currentSeq, pvm.selectedSequence.value))
                        }

                        // ------# UI 업데이트 로직 #------
                        binding.etPCDTitle.isEnabled = false
                        binding.btnPCDRight.text = "운동 시작하기"
                        binding.etPCDTitle.setText(currentProgram.programName)
//                        Log.w("시간", "programTime: ${currentProgram.programTime}")
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

                        binding.hpvPCD.progress = pvm.selectedRecProgress
                        lifecycleScope.launch(Dispatchers.Main) {
                            isTodayEnd()
                            setButtonFlavor()
                            val maxSeq = pvm.currentProgram?.programFrequency
                            val newSequence = pvm.selectedWeek.value?.let { pvm.calculateCurrentSeq(it) } // 다른 주차일 경우 무조건 0 임. newSequence는. 이전 완료한 주차일 경우 3
                            if (newSequence == maxSeq) {
                                pvm.selectedSequence.value = 0
                                binding.rvPCDHorizontal.isEnabled = false
                            } else {
                                pvm.selectedSequence.value = newSequence
                                binding.rvPCDHorizontal.isEnabled = true
                            }
                            // 주차 설정 하는 곳
                            val tvTotalWeek = pvm.currentProgram?.programWeek ?: 0
                            val tvWeek = pvm.selectedWeek.value ?: 0
                            val tvPCDWeekText = "${tvWeek.plus(1)}/$tvTotalWeek 주차"
                            binding.tvPCDWeek.text = tvPCDWeekText
                        }
                    } else {
                        Log.e("Error", "Some required data is null or empty. seq: $currentSeq, size: ${currentProgresses.size}")
                        // 에러 처리 로직 추가
                    }

                    showBalloon()
                    isResume = true

                    // 모든 ui가 업데이트 됐을 때 shimmer 종료
                    Handler(Looper.getMainLooper()).postDelayed({
                        binding.sflPCD.visibility = View.GONE
                        binding.sflPCD.stopShimmer()
                        binding.clPCD.visibility = View.VISIBLE
                    }, 150)
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
            val adapter2 = program.exercises?.let { it -> ExerciseRVAdapter(this@ProgramCustomDialogFragment, it, progresses?.sortedBy { it.uvpSn }?.toMutableList(), null, sequence, "PCD") }
            program.exercises?.let {
                adapter2?.itemStates = MutableList(it.size) { 0 } // 모든 아이템을 초기 상태로 설정
            }
            progresses?.forEachIndexed { index, progressUnitVO ->
                if (progressUnitVO.isWatched == 1) {
                    Log.v("프로그레스watched", "$index, ${progressUnitVO.isWatched}")
                    adapter2?.setTouchLockedForItem(index, 2)
                }
            }

            binding.rvPCD.adapter = adapter2
            val layoutManager2 = LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
            binding.rvPCD.layoutManager = layoutManager2
            adapter2?.notifyDataSetChanged()
        }
    }

    override fun customCategoryClick(sequence: Int) {
        if ((pvm.selectedWeek.value ?: 0) <= pvm.currentWeek) {
            pvm.selectedSequence.value = sequence
            CoroutineScope(Dispatchers.IO).launch {
                pvm.getProgressData(requireContext())
                withContext(Dispatchers.Main) {
                    //            Log.w("historys", "selectedSequence : ${pvm.selectedSequence.value}, currentSequence: ${pvm.currentSequence}")
                    val selectedWeekValue = pvm.selectedWeek.value
                    val selectedSeqValue = pvm.selectedSequence.value
                    if (selectedWeekValue != null && selectedSeqValue != null) {
//                        Log.w("회차들", "selectedWeekValue: $selectedWeekValue, selectedSeqValue: $selectedSeqValue, VM.selectedSeq: ${pvm.selectedSequence.value}, VM.currentSeq: ${pvm.currentSequence}")
                        setAdapter(pvm.currentProgram, pvm.currentProgresses, Pair(pvm.currentSequence, selectedSeqValue))
                        setButtonFlavor()
                    }
                }
            }
        }
    }
    private fun isTodayEnd() {
        // 시퀀스, 계산하는곳에서 flag인 pvm.dailySeqFinished를 넘겨줌.
        val isAllFinish = pvm.currentProgresses.map { it.isWatched }.all { it == 1 }
        Log.w("isTodayEnd", "${pvm.currentProgresses.map { it.isWatched }}")

        val rightNow = LocalDate.now()
        if (pvm.currentProgresses.isNotEmpty()) {
            val recentUpdatedAt = pvm.currentProgresses.sortedByDescending { it.updatedAt }[0].updatedAt
//            Log.w("recentUpdateAt", "$recentUpdatedAt")
            val recentUpdateDate = if (!recentUpdatedAt.isNullOrBlank() && recentUpdatedAt != "null") {
                LocalDate.parse(recentUpdatedAt, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
            } else {
                LocalDate.now().plusDays(1)
            }

            if (isAllFinish && rightNow == recentUpdateDate) {
                pvm.dailySeqFinished = true

                if (dailyFinishDialog?.isVisible == false || dailyFinishDialog?.isAdded == false) {
                    dailyFinishDialog?.show(requireActivity().supportFragmentManager, "ProgramAlertDialogFragment")
                }
            }
        }
    }

    private fun setButtonFlavor() {
        Log.e("버튼", "operate function setButtonFlavor.")
        val selectedWeekValue = pvm.selectedWeek.value
        val selectedSeqValue = pvm.selectedSequence.value
        if (selectedWeekValue != null && pvm.currentProgresses.isNotEmpty()) {

            // 모든 운동을 완료했을 때
            if (selectedWeekValue == pvm.currentWeek && pvm.dailySeqFinished) {
                (binding.rvPCD.adapter as ExerciseRVAdapter).setTouchLockedForAll(2)
                binding.btnPCDRight.apply {
                    isEnabled = false
                    backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.subColor150))
                    text = "프로그램을 모두 완료 했습니다"
                }
                // 현재 주차 ok > 현재 회차 벗어남
            } else if (selectedWeekValue < pvm.currentWeek || selectedWeekValue > pvm.currentWeek) {
                (binding.rvPCD.adapter as ExerciseRVAdapter).setTouchLockedForAll(2)
                binding.btnPCDRight.apply {
                    isEnabled = false
                    backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.subColor150))
                    text = "이번주 운동을 진행해주세요"
                }
                // 현재 주차 ok > 현재 회차 벗어남
            } else if (selectedSeqValue != null &&  selectedSeqValue < pvm.currentSequence) {
                // rv와 버튼 잠금
                if (binding.rvPCD.adapter != null) {
                    (binding.rvPCD.adapter as ExerciseRVAdapter).setTouchLockedForAll(2)
                }
                binding.btnPCDRight.apply {
                    isEnabled = false
                    backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.subColor150))
                    text = "오늘자 운동을 진행해주세요"
                }
                // 오늘자인데.
            }  else if (selectedSeqValue == pvm.currentSequence  && pvm.dailySeqFinished) {
                if (binding.rvPCD.adapter != null) {
                    (binding.rvPCD.adapter as ExerciseRVAdapter).setTouchLockedForAll(1)
                }
                binding.btnPCDRight.apply {
                    isEnabled = false
                    backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.subColor150))
                    text = "오늘 필요한 운동을 완료했습니다"
                }
            } else if (selectedSeqValue == pvm.currentSequence) {
                if (binding.rvPCD.adapter != null) {
                    (binding.rvPCD.adapter as ExerciseRVAdapter)
                }
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
        val sortedProgresses = progresses.sortedBy { it.exerciseId }
        val progressItem = sortedProgresses.firstOrNull { it.isWatched == 0 } ?: -1
        val progressIndex1 = sortedProgresses.indexOf(progressItem)
        if (progressIndex1 != -1) {
            return progressIndex1
        } else if (progresses.all { it.isWatched == 0 }) {
            Log.w("가장 최근Index", "0")
            return 0
        }  else {
            return -1
        }
    }
    private suspend fun calculateCurrentSeq() {
        return suspendCancellableCoroutine { continuation ->
            Log.e("init", "initialize calculateCurrentSeq")
            val jo = JSONObject().apply {
                put("recommendation_sn", pvm.recommendationSn)
                put("exercise_program_sn", programSn)
                put("server_sn", mvm.selectedMeasure?.sn)
            }
            Log.w("json>Progress", "$jo")
            lifecycleScope.launch(Dispatchers.IO) {
                getProgress(getString(R.string.API_progress), jo, requireContext()) { (_, week, seq), result ->
                    CoroutineScope(Dispatchers.Main).launch {  // LiveData 업데이트는 메인 스레드에서
                        val adjustedWeek = if (week == -1) 0 else week - 1
                        val adjustedSeq = if (seq == -1) 0 else seq - 1
                        if (result?.isNotEmpty() == true) {
                            val rightNow = LocalDate.now()

                            // 결과에서 만약 uvp가 중복되게 들어갔을 때 먼저 들어간 것만 사용하기.
                            val resultCount = result.getOrNull(0)?.size ?: 0
                            val programCount = pvm.currentProgram?.exercises?.size ?: 0
                            val countSets = if (resultCount > programCount) {
                                result[adjustedWeek].sortedBy { it.uvpSn }.subList(0, programCount).map { it.countSet }
                            } else {
                                result[adjustedWeek].map { it.countSet }
                            }
                            val isMaxSeq = countSets.map { it == 3 }.all { it }
                            val isMinSeq = countSets.minOrNull()

                            val editedProgresses = if (resultCount > programCount) {
                                result[adjustedWeek].sortedBy { it.uvpSn }.subList(0, programCount)
                            } else {
                                result[adjustedWeek]
                            }
                            val isAllFinish = countSets.map { it == adjustedSeq + 1 }.all { it } && editedProgresses.map { LocalDate.parse(it.updatedAt?.substring(0, 10), DateTimeFormatter.ofPattern("yyyy-MM-dd")) < LocalDate.now() }.all { it }
                            // 현재 주차 날짜 끝나는 곳 parsing
                            val currentWeekCompleted = editedProgresses.map { it.completed == 1 }.all { it }
                            val currentWeekEndDate = editedProgresses.sortedByDescending { it.updatedAt }.getOrNull(0)?.weekEndAt
                            val weekDDay = if (currentWeekEndDate != null && currentWeekEndDate.length >= 10) {
                                LocalDate.parse(currentWeekEndDate, DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                            } else LocalDate.now().minusMonths(1)

                            val isCurrentWeekEnd = LocalDate.now() > weekDDay

                            val recentUpdatedAt = if (editedProgresses.isNotEmpty()) editedProgresses.sortedByDescending { it.updatedAt }.getOrNull(0)?.updatedAt else "null"

                            val recentUpdateDate = if (!recentUpdatedAt.isNullOrBlank() && recentUpdatedAt != "null") {
                                LocalDate.parse(recentUpdatedAt, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                            } else {
                                LocalDate.now().plusDays(1)
                            }
                            // 해당 프로그램 끝
                            if (isAllFinish && adjustedWeek == 4 && adjustedSeq == 3) {
                                pvm.dailySeqFinished = true
                                if (dailyFinishDialog?.isVisible == false || dailyFinishDialog?.isAdded == false) {
                                    dailyFinishDialog = ProgramAlertDialogFragment.newInstance(this@ProgramCustomDialogFragment, 2)
                                    dailyFinishDialog?.show(requireActivity().supportFragmentManager, "ProgramAlertDialogFragment")
                                }
                            }
                            // 당일 운동 끝났는데 이번주 아직 안지남
                            else if (isAllFinish &&  rightNow == recentUpdateDate && pvm.selectedWeek.value == pvm.currentWeek) {
                                Log.w("회차", "회차1번")
                                pvm.dailySeqFinished = true
                                if (dailyFinishDialog?.isVisible == false || dailyFinishDialog?.isAdded == false) {
                                    dailyFinishDialog?.show(requireActivity().supportFragmentManager, "ProgramAlertDialogFragment")
                                }

                            } // 운동 끝났는데 이번주 아직 안지남
                            else if (isAllFinish&& pvm.selectedWeek.value == pvm.currentWeek && currentWeekCompleted  && !isCurrentWeekEnd) {
                                Log.w("회차", "회차2번")
                                pvm.dailySeqFinished = true
                                if (dailyFinishDialog?.isVisible == false || dailyFinishDialog?.isAdded == false) {
                                    dailyFinishDialog = ProgramAlertDialogFragment.newInstance(this@ProgramCustomDialogFragment, 3)
                                    dailyFinishDialog?.show(requireActivity().supportFragmentManager, "ProgramAlertDialogFragment")
                                }
                            }

                            Log.w("seq계산하기", "week,seq: ($adjustedWeek, $adjustedSeq), max: $isMaxSeq, min: $isMinSeq, $isAllFinish, ")
                            // 이전 주차로 감
                            if ((pvm.selectedWeek.value ?: 0) < pvm.currentWeek) {
                                pvm.currentSequence = 2
                                pvm.selectedSequence.value = 2

                                // 모든 회차가 끝남 ( 주차가 넘어가야하는 상황 )
                            } else if (isMaxSeq && isAllFinish && isCurrentWeekEnd) {
                                pvm.currentSequence = 0
                                pvm.selectedSequence.value = 0
                            }     // 회차 진행중
                            else {
                                if (isMinSeq != null) {

                                    // 3회차가 아님. 최근 받아온 회차가 모두 끝남. 마지막 업데이트일자가 오늘이 아님 (어제 이전에 회차가 완료된 상황)
                                    if (!isMaxSeq && isAllFinish && isMinSeq > 0  && rightNow != recentUpdateDate) {
                                        pvm.currentSequence = adjustedSeq + 1
                                        pvm.selectedSequence.value= adjustedSeq + 1
                                    } else {
                                        pvm.currentSequence = adjustedSeq
                                        pvm.selectedSequence.value= adjustedSeq
                                    }
                                }
                            }
                            continuation.resume(Unit)
                        }

                        Log.w("초기Seq", "selectedWeek: ${pvm.selectedWeek.value} selectWeek: ${pvm.selectWeek.value}, currentWeek: ${pvm.currentWeek}, currentSeq: ${pvm.currentSequence}, selectedSequence: ${pvm.selectedSequence.value}")
                    }
                }
            }
        }
    }

    private suspend fun calculateCurrentWeek() {
        Log.e("init", "initialize calculateCurrentWeek")
        withContext(Dispatchers.IO) {
            val jo = JSONObject().apply {
                put("recommendation_sn", pvm.recommendationSn)
                put("exercise_program_sn", programSn)
                put("server_sn", mvm.selectedMeasure?.sn)
            }

            Log.w("json>Progress", "$jo")

            getProgress(getString(R.string.API_progress), jo, requireContext()) { (_, week, seq), result ->
                CoroutineScope(Dispatchers.Main).launch {  // LiveData 업데이트는 메인 스레드에서
                    val adjustedWeek = if (week == -1) 0 else week - 1
                    val adjustedSeq = if (seq == -1) 0 else seq - 1 // 마지막으로 한게 0, 0 인게 맞음.
                    if (result?.isNotEmpty() == true) {
                        val rightNow = LocalDate.now()

                        // 결과에서 만약 uvp가 중복되게 들어갔을 때 먼저 들어간 것만 사용하기.
                        val resultCount = result.get(0).size
                        val programCount = pvm.currentProgram?.exercises?.size ?: 0
                        val countSets = if (resultCount > programCount) {
                            result[adjustedWeek].sortedBy { it.uvpSn }.subList(0, programCount).map { it.countSet }
                        } else {
                            result[adjustedWeek].map { it.countSet }
                        }
                        val isMaxSeq = countSets.map { it == 3 }.all { it }
                        val isMinSeq = countSets.min()
                        val isSeqFinish = countSets.distinct().size == 1

                        val editedProgresses = if (resultCount > programCount) {
                            result[adjustedWeek].sortedBy { it.uvpSn }.subList(0, programCount)
                        } else {
                            result[adjustedWeek]
                        }
                        val recentUpdatedAt = editedProgresses.sortedByDescending { it.updatedAt }[0].updatedAt
                        val recentUpdateDate = if (!recentUpdatedAt.isNullOrBlank() && recentUpdatedAt != "null") {
                            LocalDate.parse(recentUpdatedAt, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                        } else {
                            LocalDate.now().plusDays(1)
                        }
                        // completed로 회차가 다 끝났을 때 판단/1개의 seq모두다 끝났는지 판단하기

                        val isAllFinish = countSets.map { it == adjustedSeq + 1 }.all { it } && editedProgresses.map { LocalDate.parse(it.updatedAt?.substring(0, 10), DateTimeFormatter.ofPattern("yyyy-MM-dd")) < LocalDate.now() }.all { it }

                        // 현재 주차 날짜 끝나는 곳 parsing
                        val currentWeekCompleted = editedProgresses.map { it.completed == 1 }.all { it }
                        val currentWeekEndDate = editedProgresses.sortedByDescending { it.updatedAt }.getOrNull(0)?.weekEndAt
                        val weekDDay = if (currentWeekEndDate != null && currentWeekEndDate.length >= 10) {
                            LocalDate.parse(currentWeekEndDate, DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                        } else LocalDate.now().minusMonths(1)
                        val isCurrentWeekEnd = LocalDate.now() > weekDDay
//                        Log.v("isCurrenWeek", "$isAllFinish, $rightNow == $recentUpdateDate 주차 끝남 판단: $isCurrentWeekEnd, ${LocalDate.now()} > $weekDDay")

                        // 해당 프로그램 끝
                        if (isAllFinish && adjustedWeek == 4 && adjustedSeq == 3) {
                            pvm.dailySeqFinished = true
                            if (dailyFinishDialog?.isVisible == false || dailyFinishDialog?.isAdded == false) {
                                dailyFinishDialog = ProgramAlertDialogFragment.newInstance(this@ProgramCustomDialogFragment, 2)
                                dailyFinishDialog?.show(requireActivity().supportFragmentManager, "ProgramAlertDialogFragment")
                            }

                        // 오늘 운동 끝남
                        } else if (isAllFinish && rightNow == recentUpdateDate) {
                            pvm.dailySeqFinished = true
                            if (dailyFinishDialog?.isVisible == false || dailyFinishDialog?.isAdded == false) {
                                dailyFinishDialog?.show(requireActivity().supportFragmentManager, "ProgramAlertDialogFragment")
                            }
                        // 운동 끝났는데 이번주 아직 안지남
                        } else if (isAllFinish && rightNow == recentUpdateDate && currentWeekCompleted  && !isCurrentWeekEnd) {
                            pvm.dailySeqFinished = true
                            if (dailyFinishDialog?.isVisible == false || dailyFinishDialog?.isAdded == false) {
                                dailyFinishDialog = ProgramAlertDialogFragment.newInstance(this@ProgramCustomDialogFragment, 3)
                                dailyFinishDialog?.show(requireActivity().supportFragmentManager, "ProgramAlertDialogFragment")
                            }
                        }

                        if (isMaxSeq && isSeqFinish && isCurrentWeekEnd) {
                            // 모든 회차가 끝남 ( 주차가 넘어가야하는 상황 )
                            pvm.currentWeek = adjustedWeek + 1
                            pvm.selectWeek.value = adjustedWeek + 1
                            pvm.selectedWeek.value = adjustedWeek + 1
                        } else if (isMaxSeq && isSeqFinish) {
                            pvm.currentWeek = adjustedWeek
                            pvm.selectWeek.value = adjustedWeek
                            pvm.selectedWeek.value = adjustedWeek
                        } else if (!isMaxSeq && isSeqFinish && isMinSeq > 0) {
                            pvm.currentWeek = adjustedWeek
                            pvm.selectWeek.value = adjustedWeek
                            pvm.selectedWeek.value = adjustedWeek

                        } // 이전에 다 완료하고 다른 날이 됐을 때
                         else if (isMinSeq > 0 && isAllFinish && rightNow != recentUpdateDate ) {
                            pvm.currentWeek = adjustedWeek
                            pvm.selectWeek.value = adjustedWeek
                            pvm.selectedWeek.value = adjustedWeek
                        } else if (isMinSeq > 0) {
                            pvm.currentWeek = adjustedWeek
                            pvm.selectWeek.value = adjustedWeek
                            pvm.selectedWeek.value = adjustedWeek
                        } else {
                            pvm.currentWeek = adjustedWeek
                            pvm.selectWeek.value = adjustedWeek
                            pvm.selectedWeek.value = adjustedWeek
                        }
                    }
                    Log.w("초기WeekSeq", "selectedWeek: ${pvm.selectedWeek.value} selectWeek: ${pvm.selectWeek.value}, currentWeek: ${pvm.currentWeek}, currentSeq: ${pvm.currentSequence}, selectedSequence: ${pvm.selectedSequence.value}")
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
        balloon2.setOnBalloonClickListener { balloon2.dismiss() }
        binding.ibtnPCDTop.setOnClickListener { it.showAlignBottom(balloon2) }
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