package com.tangoplus.tangoq.dialog

import android.annotation.SuppressLint
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
import com.tangoplus.tangoq.viewmodel.UserViewModel
import com.tangoplus.tangoq.databinding.FragmentProgramCustomDialogBinding
import com.tangoplus.tangoq.dialog.bottomsheet.ProgramWeekBSDialogFragment
import com.tangoplus.tangoq.fragment.ExtendedFunctions.isFirstRun
import com.tangoplus.tangoq.function.PreferencesManager
import com.tangoplus.tangoq.listener.OnCustomCategoryClickListener
import com.tangoplus.tangoq.api.NetworkProgram.fetchProgram
import com.tangoplus.tangoq.function.SaveSingletonManager
import com.tangoplus.tangoq.db.Singleton_t_progress
import com.tangoplus.tangoq.db.Singleton_t_user
import com.tangoplus.tangoq.listener.OnSingleClickListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class ProgramCustomDialogFragment : DialogFragment(), OnCustomCategoryClickListener {
    lateinit var binding : FragmentProgramCustomDialogBinding
    val viewModel : ExerciseViewModel by activityViewModels()
    private val pvm : ProgressViewModel by activityViewModels()
    private val mvm : MeasureViewModel by activityViewModels()
    private val uvm : UserViewModel by activityViewModels()
    private lateinit var ssm : SaveSingletonManager
    private var programSn = 0
    private var recommendationSn = 0
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
        userJson = Singleton_t_user.getInstance(requireContext()).jsonObject ?: JSONObject()
        programSn = arguments?.getInt(ARG_PROGRAM_SN) ?: -1
        recommendationSn = arguments?.getInt(ARG_RECOMMENDATION_SN) ?: -1
        ssm = SaveSingletonManager(requireContext(), requireActivity())

        isResume = false
        lifecycleScope.launch {
            // 프로그램에 들어가는 운동
            binding.sflPCD.startShimmer()
            binding.sflPCD.visibility = View.VISIBLE
            initializeProgram()
        }

        pvm.selectedWeek.observe(viewLifecycleOwner) { selectedWeek ->
            if (pvm.currentProgram != null && pvm.selectedSequence.value != null) {
                val maxSeq = pvm.currentProgram?.programFrequency
                val selectedWeekValue = pvm.selectedWeek.value
                if (selectedWeekValue != null) {
                    val seqProgresses = pvm.currentProgresses.get(selectedWeekValue)
                    val minSequenceInWeek = seqProgresses.minOfOrNull { it.currentSequence } ?: 0

                    if (minSequenceInWeek != maxSeq) {
                        pvm.currentSequence = minSequenceInWeek // 지금 1주찬데 2주차로넘어갔을 때 0으로 가지겠지?
                    }

                    val newSequence = pvm.calculateSequenceForWeek(selectedWeek) // 다른 주차일 경우 무조건 0 임. newSequence는. 이전 완료한 주차일 경우 3
                    if (newSequence == maxSeq) {
                        pvm.selectedSequence.value = -1
                        binding.rvPCDHorizontal.isEnabled = false
                    } else {
                        pvm.selectedSequence.value = newSequence
                        binding.rvPCDHorizontal.isEnabled = true
                    }
                    pvm.currentSequence = newSequence
                    Log.v("주차별Seq재설정", "selectedWeek: ${pvm.selectedWeek.value}, currentWeek: ${pvm.currentWeek}, selectedSequence: ${pvm.selectedSequence.value}, newSequence: $newSequence")
                    val selectedSeqValue = pvm.selectedSequence.value
                    if (selectedSeqValue != null) {
                        setAdapter(pvm.currentProgram, pvm.currentProgresses[selectedWeek], Pair(pvm.currentSequence, selectedSeqValue))
                        val tvTotalWeek = pvm.currentProgram?.programWeek ?: 0
                        binding.tvPCDWeek.text = "${pvm.selectedWeek.value?.plus(1)}/$tvTotalWeek 주차"

                        setButtonFlavor()
                    }
                }
            }
        }
        binding.btnPCDRight.setOnSingleClickListener {
            when (binding.btnPCDRight.text) {
                "운동 시작하기" -> {

                    // PreferencesManager 초기화 및 추천 저장
                    val prefManager = PreferencesManager(requireContext())
                    prefManager.saveLatestRecommendation(recommendationSn)
                    Log.v("마지막시청rec", "${prefManager.getLatestRecommendation()}")
                    uvm.existedProgramData = pvm.currentProgram

                    val currentSequenceProgresses = pvm.currentProgresses[pvm.currentWeek]
                    // 가장 최근에 시작한 운동의 인덱스 찾기
                    val startIndex = findCurrentIndex(currentSequenceProgresses)
                    // startIndex가 유효한지 확인
                    if (startIndex >= 0 && startIndex < currentSequenceProgresses.size) {
                        Log.v("startIndex", "${startIndex}, currentSequenceProgresses: ${currentSequenceProgresses.size}")
                        val videoUrls = mutableListOf<String>()
                        val exerciseIds = mutableListOf<String>()
                        val uvpIds = mutableSetOf<String>() // 중복 제거를 위해 Set 사용

                        for (i in startIndex until currentSequenceProgresses.size) {

                            val progress = currentSequenceProgresses[i]
                            exerciseIds.add(progress.exerciseId.toString())
                            uvpIds.add(progress.uvpSn.toString())
                            videoUrls.add(program.exercises?.get(i)?.videoFilepath.toString())
                        }

                        val intent = Intent(requireContext(), PlayFullScreenActivity::class.java)
                        intent.putStringArrayListExtra("video_urls", ArrayList(videoUrls))
                        intent.putStringArrayListExtra("exercise_ids", ArrayList(exerciseIds))
                        intent.putStringArrayListExtra("uvp_sns", ArrayList(uvpIds))
                        intent.putExtra("current_position", currentSequenceProgresses[startIndex].lastProgress.toLong())
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

    private fun updateUI() {
        lifecycleScope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    // ------# 프로그렘 셋팅 #------
                    val jo = JSONObject().apply {
//                        put("user_sn", userJson.optString("sn"))
                        put("recommendation_sn", recommendationSn)
                        put("exercise_program_sn", programSn)
                        put("server_sn", mvm.selectedMeasure?.sn)
                    }
                    Log.v("json>Progress", "$jo")
                    // ------# 프로그레스 가져오기 ( IO ) #------
                    /* 현재 보고 있는 프로그램의 시청기록만 싱글턴에서 관리함. 여러 개 넣어서 하는게 아님. 계속 갱신 됨 */

                    ssm.getOrInsertProgress(jo)
                    pvm.currentProgresses = Singleton_t_progress.getInstance(requireContext()).programProgresses ?: mutableListOf()// 이곳에 프로그램하나에 해당되는 모든 upv들이 가져와짐.
                    // ------# 현재 시퀀스 찾기 #------
                    withContext(Dispatchers.Main) {
                        endSequence()
                        calculateInitialWeekAndSequence()
                    }
                    // 모든 IO 작업이 완료되면 결과를 반환
                    Triple(pvm.currentSequence, pvm.currentProgram, pvm.currentProgresses)
                }

                withContext(Dispatchers.Main) {
                    // 현재 12회차 통합본으로 계산하는 중. 4 * 3으로 나눠야 함.
                    val (currentSeq, currentProgram, currentProgresses) = result
                    if (currentProgram != null && currentProgresses.isNotEmpty() && currentSeq < currentProgresses.size) {

                        // ------# 프로그레스 바 계산 #------
                        val currentSequenceProgresses = pvm.currentProgresses[pvm.currentWeek]
                        val startIndex = findCurrentIndex(currentSequenceProgresses)
                        val exerciseCount = currentProgram.programCount?.toInt() ?: 0
                        val totalExercises = currentProgram.programFrequency * currentProgram.programWeek * exerciseCount
                        val previousWeeksExercises = pvm.currentWeek * (currentProgram.programFrequency * exerciseCount)
                        val previousSequencesExercises = pvm.currentSequence * exerciseCount
                        val currentExercises = startIndex + 1
                        val completedExercises = previousWeeksExercises + previousSequencesExercises + currentExercises
                        val hpvProgress = (completedExercises * 100) / totalExercises

                        Log.v("프로그레스들", "hpvProgress: $hpvProgress, currentSequence: ${pvm.currentSequence + 1}, currentWeek: ${pvm.currentWeek + 1}, startIndex: ${startIndex + 1}")
                        binding.hpvPCD.progress = hpvProgress.toFloat()

                        // ------# 현재 기록으로 어댑터 연걸 #------
                        /* 어댑터에는 현재 회차를 자체적으로 가져와서 넣기.
                        * */
                        val selectedSeqValue = pvm.selectedSequence.value
                        if (selectedSeqValue != null) {
                            setAdapter(currentProgram, currentProgresses[pvm.currentWeek], Pair(currentSeq, selectedSeqValue))
                        }

                        // ------# UI 업데이트 로직 #------
                        binding.etPCDTitle.isEnabled = false
                        binding.btnPCDRight.text = "운동 시작하기"

                        binding.etPCDTitle.setText(currentProgram.programName)
                        Log.v("시간", "programTime: ${currentProgram.programTime}")
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
                        setButtonFlavor()
                    } else {
                        Log.e("Error", "Some required data is null or empty, program: $currentProgram, seq: $currentSeq, size: ${currentProgresses.size}")
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
                Log.e("ProgramException", "${e}")
            }
        }
    }

    private fun initializeProgram() {
        lifecycleScope.launch {
            fetchProgramData() // 프로그램 데이터 한 번만 가져오기
            updateUI()  // UI 업데이트는 필요할 때마다 호출
        }
    }
    private fun setAdapter(program: ProgramVO?, progresses : MutableList<ProgressUnitVO>?, sequence: Pair<Int, Int>?) {

        /* sequence = Pair(현재 회차(currentSeq), 선택한 회차 (selectedSeq))
        currentSequence 는 진행중인 주차, 진행중인 회차, 선택된 회차 이렇게 나눠짐 */
        pvm.selectedSequence.value = sequence?.second
        Log.v("클릭>ProgressSeq", "${progresses?.map { it.uvpSn }} ,currentSeq: ${progresses?.map { it.currentSequence }}")
        val selectSeqValue = pvm.selectedSequence.value
        val frequency = program?.programFrequency
        val adapter : ProgramCustomRVAdapter
        val selectedWeekValue = pvm.selectedWeek.value

        if (selectSeqValue != null && frequency != null && selectedWeekValue != null && sequence?.second != null) {

            // progress를 계산하여 adapter에 연결
            Log.v("progresses", "${progresses?.map { it.lastProgress }}, ${progresses?.map { it.videoDuration }}")
            val pvMother = progresses?.sumOf {
                it.videoDuration
            }
            val pvChild = progresses?.sumOf {
                if (it.currentSequence > sequence.first) {
                    it.videoDuration
                } else {
                    it.lastProgress
                }
            }
            if (pvMother != null && pvChild != null) {
                val resultProgress =  (pvChild * 100).div(pvMother)
                adapter = ProgramCustomRVAdapter(this@ProgramCustomDialogFragment,
                    Triple(frequency, pvm.currentSequence, selectSeqValue),
                    Pair(pvm.currentWeek, selectedWeekValue),
                    resultProgress,
                    this@ProgramCustomDialogFragment)

                val layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
                binding.rvPCDHorizontal.layoutManager = layoutManager
                binding.rvPCDHorizontal.adapter = adapter
                adapter.notifyDataSetChanged()

                val adapter2 = program.exercises?.let { ExerciseRVAdapter(this@ProgramCustomDialogFragment, it, progresses, null, sequence, null,"PCD") }
                binding.rvPCD.adapter = adapter2
                val layoutManager2 = LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
                binding.rvPCD.layoutManager = layoutManager2
                adapter2?.notifyDataSetChanged()
            }

        }
    }

    override fun customCategoryClick(sequence: Int) {
        if (pvm.selectedWeek.value == pvm.currentWeek) {
            pvm.selectedSequence.value = sequence
//            Log.v("historys", "selectedSequence : ${pvm.selectedSequence.value}, currentSequence: ${pvm.currentSequence}")
            val selectedWeekValue = pvm.selectedWeek.value
            val selectedSequenceValue = pvm.selectedSequence.value
            if (selectedWeekValue != null && selectedSequenceValue != null) {
                setAdapter(pvm.currentProgram, pvm.currentProgresses[selectedWeekValue], Pair(pvm.currentSequence, selectedSequenceValue))
                setButtonFlavor()
            }

        }
    }
    private fun setButtonFlavor() {
        val selectedWeekValue = pvm.selectedWeek.value
        val selectedSeqValue = pvm.selectedSequence.value
        if (selectedWeekValue != null) {
            val lastWeekProgress = pvm.currentProgresses[pvm.currentWeek].last() // 현재 주차의 가장 마지막 item을 가져옴 팔꿉에서는 index 2
            Log.v("lastWeekProgress", "$lastWeekProgress, 목록: ${pvm.currentProgresses[pvm.currentWeek]}")
            if (lastWeekProgress.updateDate != null && lastWeekProgress.updateDate.length > 9) {
                val inputDate = LocalDate.parse(lastWeekProgress.updateDate.subSequence(0, 10), DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                val currentDate = LocalDate.now()
                // 현재 주차안에서만
                if (selectedWeekValue <= pvm.currentWeek) {
                    // 현재 날짜가 같고, 가장 마지막 lasWeekProgress가 currentSequence가 1보다 클 때, 그리고 선택한 seq가 같을 때

                    if (inputDate == currentDate
                        && lastWeekProgress.currentSequence > 0
                        && pvm.selectedSequence.value == pvm.currentSequence
                    ) {
                        // 오늘 시퀀스가 끝났으니 버튼 Flavor 맞추기
                        binding.btnPCDRight.apply {
                            isEnabled = false
                            backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.subColor150))
                            text = "오늘 회차의 운동을 완료했습니다"
                        }
                    }
                    else if (pvm.selectedSequence.value == pvm.currentSequence) {
                        binding.btnPCDRight.apply {
                            isEnabled = true
                            backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.mainColor))
                            text = "운동 시작하기"
                        }
                    } else {
                        binding.btnPCDRight.apply {
                            isEnabled = false
                            backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.subColor150))
                            if (selectedSeqValue != null) {
                                text = if (selectedSeqValue > pvm.currentSequence) {
                                    "현재 프로그램을 진행해주세요"
                                } else {
                                    "완료한 운동입니다"
                                }
                            }
                        }
                    }
                } else {
                    binding.btnPCDRight.apply {
                        isEnabled = false
                        backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.subColor150))
                        text = "현재 프로그램을 진행해주세요"
                    }
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
        val progressIndex1 = progresses.indexOfLast { it.lastProgress in 1 until it.videoDuration }
        if (progressIndex1 != -1) {
            return progressIndex1
        }

        // Case 2 다 보고 난 뒤 완료 처리가 된 후 (currentSequence가 1이 됐을 때) 그 다음 index 반환
        val progressIndex = progresses.indexOfLast { it.currentSequence - 1 == pvm.currentSequence }
        Log.v("프로그레스", "$progressIndex, currentSeq: ${pvm.currentSequence}")
        if (progressIndex != progresses.size) {
            Log.v("가장 최근Index", "${progressIndex +  1}")
            return progressIndex + 1
        }

        // Case 3 초기상태
        if (progresses.all { it.lastProgress == 0 }) {
            Log.v("가장 최근Index", "0")
            return 0
        }  else {
            return -1
        }
    }

    private fun endSequence() {
        val lastWeekProgress = pvm.currentProgresses[pvm.currentWeek].last() // 현재 주차의 가장 마지막 item을 가져옴 팔꿉에서는 index 2
        Log.v("업데이트확인하기", "$lastWeekProgress")
        if (lastWeekProgress.updateDate != null && lastWeekProgress.updateDate.length > 9) {
            val inputDate = LocalDate.parse(lastWeekProgress.updateDate.subSequence(0, 10), DateTimeFormatter.ofPattern("yyyy-MM-dd"))
            val currentDate = LocalDate.now()
            Log.v("오늘자", "오늘자 완료한걸까요. ${pvm.currentSequence} / ${lastWeekProgress.currentSequence} , $inputDate / $currentDate")

            if (inputDate == currentDate && lastWeekProgress.currentSequence > 0) {
                val dialog = ProgramAlertDialogFragment.newInstance(this, 1)
                dialog.show(requireActivity().supportFragmentManager, "ProgramAlertDialogFragment")

                // 버튼 잠금
                setButtonFlavor()
            }
        }
    }

    private fun calculateInitialWeekAndSequence() {
        val maxSeq = pvm.currentProgram?.programFrequency
        val maxWeek = pvm.currentProgram?.programWeek
        for (weekIndex in pvm.currentProgresses.indices) {
            val weekProgress = pvm.currentProgresses[weekIndex]
            val minSequenceInWeek = weekProgress.minOfOrNull { it.currentSequence } ?: 0

            // 현재 주차도 알아내서 -> 주차도 maxWeek에 맞는지 확인하고 나가게 하거나 계속 돌아가게 하면 됨.
            if (weekIndex + 1 == maxWeek  && minSequenceInWeek == maxSeq) {
                // 위크까지 반복문이 왔는데 minSequence까지 max 였다?

                val dialog = ProgramAlertDialogFragment.newInstance(this, 2)
                dialog.show(requireActivity().supportFragmentManager, "ProgramAlertDialogFragment")
                break
            } else if (weekIndex != maxWeek && minSequenceInWeek == maxSeq) {
                continue
            } else {
                pvm.selectWeek.value = weekIndex
                pvm.selectedWeek.value = weekIndex
                pvm.currentWeek = weekIndex
                pvm.currentSequence = minSequenceInWeek
                pvm.selectedSequence.value = minSequenceInWeek
                Log.v("초기WeekSeq", "selectedWeek: ${pvm.selectedWeek.value} selectWeek: ${pvm.selectWeek.value}, currentWeek: ${pvm.currentWeek}, currentSeq: ${pvm.currentSequence}, selectedSequence: ${pvm.selectedSequence.value}, maxWeek: $maxWeek, maxSeq: $maxSeq")
                val tvTotalWeek = pvm.currentProgram?.programWeek ?: 0
                binding.tvPCDWeek.text = "${pvm.selectedWeek.value?.plus(1)}/$tvTotalWeek 주차"
                break
            }
        }
    }

    private fun showBalloon() {
        val balloon2 = Balloon.Builder(requireContext())
            .setWidthRatio(0.8f)
            .setHeight(BalloonSizeSpec.WRAP)
            .setText("주차 요일에 맞추어 운동을 진행합니다.\n프로그램을 한 주동안 원하는 날짜에 진행해보세요")
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

}