package com.tangoplus.tangoq.dialog

import android.content.Intent
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
import com.tangoplus.tangoq.data.ExerciseVO
import com.tangoplus.tangoq.data.ExerciseViewModel
import com.tangoplus.tangoq.data.MeasureViewModel
import com.tangoplus.tangoq.data.ProgressUnitVO
import com.tangoplus.tangoq.data.ProgressViewModel
import com.tangoplus.tangoq.data.ProgramVO
import com.tangoplus.tangoq.databinding.FragmentProgramCustomDialogBinding
import com.tangoplus.tangoq.db.PreferencesManager
import com.tangoplus.tangoq.fragment.isFirstRun
import com.tangoplus.tangoq.listener.OnCustomCategoryClickListener
import com.tangoplus.tangoq.`object`.NetworkProgram.fetchProgram
import com.tangoplus.tangoq.`object`.SaveSingletonManager
import com.tangoplus.tangoq.`object`.Singleton_t_progress
import com.tangoplus.tangoq.`object`.Singleton_t_user
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class ProgramCustomDialogFragment : DialogFragment(), OnCustomCategoryClickListener {
    lateinit var binding : FragmentProgramCustomDialogBinding
    val viewModel : ExerciseViewModel by activityViewModels()
    val pvm : ProgressViewModel by activityViewModels()
    val mvm : MeasureViewModel by activityViewModels()
    private lateinit var ssm : SaveSingletonManager
    var programSn = 0
    var recommendationSn = 0

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
        updateUI()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // -------# 기본 셋팅 #-------
        userJson = Singleton_t_user.getInstance(requireContext()).jsonObject!!
        programSn = arguments?.getInt(ARG_PROGRAM_SN)!!
        recommendationSn = arguments?.getInt(ARG_RECOMMENDATION_SN)!!
        ssm = SaveSingletonManager(requireContext(), requireActivity())

        lifecycleScope.launch {
            // 프로그램에 들어가는 운동
            binding.sflPCD.startShimmer()
            binding.sflPCD.visibility = View.VISIBLE
            updateUI()

        }


        binding.btnPCDRight.setOnClickListener {
            when (binding.btnPCDRight.text) {
                "운동 시작하기" -> {
                    // PreferencesManager 초기화 및 추천 저장
                    val prefManager = PreferencesManager(requireContext())
                    prefManager.saveLatestRecommendation(recommendationSn)

                    // 현재 시퀀스의 ProgressUnitVO 리스트 가져오기
                    val currentSequenceProgresses = pvm.currentProgresses[pvm.currentSequence]

                    // 가장 최근에 시작한 운동의 인덱스 찾기
                    val startIndex = findCurrentIndex(currentSequenceProgresses)

                    // startIndex가 유효한지 확인
                    if (startIndex >= 0 && startIndex < currentSequenceProgresses.size) {
                        val remainingProgresses = currentSequenceProgresses.subList(startIndex, currentSequenceProgresses.size)
                        val currentExercises = pvm.currentProgram?.exercises ?: emptyList()
                        val filteredExercises = currentExercises.drop(startIndex)

                        val videoUrls = mutableListOf<String>()
                        val exerciseIds = mutableListOf<String>()
                        val uvpIds = mutableSetOf<String>() // 중복 제거를 위해 Set 사용
                        var totalDuration = 0
                        for (i in startIndex until currentSequenceProgresses.size) {
                            val progress = currentSequenceProgresses[i]
                            exerciseIds.add(progress.exerciseId.toString())
                            uvpIds.add(progress.uvpSn.toString())
                            videoUrls.add(program.exercises?.get(i)?.videoFilepath!!)
                            totalDuration += program.exercises?.get(i)?.videoDuration?.toInt()!!
                        }
                        val intent = Intent(requireContext(), PlayFullScreenActivity::class.java)
                        intent.putStringArrayListExtra("video_urls", ArrayList(videoUrls))
                        intent.putStringArrayListExtra("exercise_ids", ArrayList(exerciseIds))
                        intent.putStringArrayListExtra("uvp_sns", ArrayList(uvpIds))
                        intent.putExtra("current_position",currentSequenceProgresses[startIndex].lastProgress.toLong())
                        intent.putExtra("total_duration", totalDuration)
                        requireContext().startActivity(intent)
                        startActivityForResult(intent, 8080)
                        Log.v("인텐트담은것들", "${videoUrls}, $exerciseIds, $uvpIds")
                    } else {
                        Log.e("ExerciseProgress", "Invalid startIndex: $startIndex")
                    }
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
            Handler(Looper.getMainLooper()).postDelayed({
                binding.ibtnPCDTop.showAlignBottom(balloon2)
                balloon2.dismissWithDelay(1800L)
            }, 700)
        }
        binding.ibtnPCDTop.setOnClickListener { it.showAlignBottom(balloon2) }
    }

    private fun updateUI() {
        lifecycleScope.launch {
            try {

                val result = withContext(Dispatchers.IO) {
                    program = fetchProgram("https://gym.tangostar.co.kr/tango_gym_admin/programs/read.php", programSn.toString())!!
                    pvm.currentProgram = program

                    val jo = JSONObject()
                    jo.put("user_sn", userJson.optString("user_sn"))
                    jo.put("recommendation_sn", recommendationSn)
                    jo.put("exercise_program_sn", programSn)
                    jo.put("measure_sn", mvm.selectedMeasure?.measureSn)
                    Log.v("json프로그레스", "${jo}")

                    ssm.getOrInsertProgress(jo)
                    pvm.currentProgresses = Singleton_t_progress.getInstance(requireContext()).programProgresses!! // 이곳에 프로그램하나에 해당되는 모든 upv들이 가져와짐.
                    Log.v("현재진행기록", "pvm.currentProgresses.size: ${pvm.currentProgresses.size}")

                    // ------! 현재 시퀀스 찾기 시작 !------
                    val currentProgresses = pvm.currentProgresses // 현재 프로그램의 시퀀스 가져오기 <전체>

                    var currentWeek = 0
                    var currentSequence = 0
                    for (weekIndex in currentProgresses.indices) { // 1주차 2주차로 접근
                        val weekProgress = currentProgresses[weekIndex] // 1주차에 해당되는 mutableListOf<ProgressUnitVO>
                        // TODO 여기서 일단 sequence를 체크하는 방식이 수정될 수 있음. 여기서 부터 수정.


                        val maxSequenceInWeek = weekProgress.maxOfOrNull { it.currentSequence } ?: 0

                        if (maxSequenceInWeek == 3) {
                            continue
                        } else {
                            currentWeek = weekIndex + 1
                            currentSequence = maxSequenceInWeek
                            break
                        }
                    }
                    // ------# 기존에 12회차를 계산하는 방법인듯? #------
                    // 이제 이럴 필요 없이 그냥 seq쓰면됨.
                    val currentRound = (currentWeek - 1) * 3 + currentSequence
                    pvm.currentSequence = currentRound
                    Log.v("currentSeq", "currentSequence: ${pvm.currentSequence}")

                    // 모든 IO 작업이 완료되면 결과를 반환
                    Triple(currentRound, pvm.currentProgram, pvm.currentProgresses)
                }


                withContext(Dispatchers.Main) {
                    val (currentRound, currentProgram, currentProgresses) = result
                    val hpvProgress = (pvm.currentSequence * 100) / pvm.currentProgresses.size
                    Log.v("프로그레스바", "$hpvProgress")
                    Log.v("프로그레스들", "currentProgress.size: ${currentProgresses.size}, 현재회차[currentRound]: ${currentProgresses[currentRound]}")
                    binding.hpvPCD.progress = hpvProgress
                    // null 체크 추가
                    if (currentProgram != null && currentProgresses.isNotEmpty() && currentRound < currentProgresses.size) {

                        setAdapter(currentProgram, currentProgresses[currentRound], Pair(currentRound, currentRound))

                        // UI 업데이트 로직
                        binding.etPCDTitle.isEnabled = false
                        binding.btnPCDRight.text = "운동 시작하기"

                        binding.etPCDTitle.setText(currentProgram.programName)
                        binding.tvPCDTime.text = if (currentProgram.programTime <= 60) {
                            "${currentProgram.programTime}초"
                        } else {
                            "${currentProgram.programTime / 60}분 ${currentProgram.programTime % 60}초"
                        }
                        binding.tvPCDStage.text = when (currentProgram.programStage) {
                            "초급" -> "초급자"
                            "중급" -> "중급자"
                            "고급" -> "상급자"
                            else -> ""
                        }
                        binding.tvPCDCount.text = "${currentProgram.programCount} 개"
                    } else {
                        Log.e("Error", "Some required data is null or empty")
                        // 에러 처리 로직 추가
                    }
                    binding.sflPCD.stopShimmer()
                    binding.sflPCD.visibility = View.GONE
                }
            } catch (e: Exception) {
                Log.e("Error", "An error occurred: ${e.message}", e)
                // 에러 처리 로직 추가
            }
        }

        // ------# 프로그램 기간 만료 #------
//        val programEndDate = programStartDate.plusWeeks(viewModel.currentProgram!!.programWeek.toLong()).minusDays(1)
//        Log.v("programDates", "프로그램 시작날짜: ${programStartDate}, 종료날짜: ${programEndDate}")
//        if (LocalDate.now() == programEndDate) {
//            val programAlertDialogFragment = ProgramAlertDialogFragment.newInstance(this)
//            programAlertDialogFragment.show(childFragmentManager, "ProgramAlertDialogFragment")
//        }

    }

    private fun setAdapter(program: ProgramVO, progresses : MutableList<ProgressUnitVO>?, sequence: Pair<Int, Int>) {

        /* currentSequence 는 진행중인 주차, 진행중인 회차, 선택된 회차 이렇게 나눠짐 */
        pvm.selectedSequence.value = sequence.second
        val adapter = ProgramCustomRVAdapter(this@ProgramCustomDialogFragment,
            Triple(program.programFrequency, pvm.currentSequence, pvm.selectedSequence.value!!),
            this@ProgramCustomDialogFragment)

        val layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        binding.rvPCDHorizontal.layoutManager = layoutManager
        binding.rvPCDHorizontal.adapter = adapter
        adapter.notifyDataSetChanged()

        val adapter2 = ExerciseRVAdapter(this@ProgramCustomDialogFragment, program.exercises!!, progresses, sequence,"main")
        binding.rvPCD.adapter = adapter2
        val layoutManager2 = LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
        binding.rvPCD.layoutManager = layoutManager2

        adapter2.notifyDataSetChanged()
    }

    override fun customCategoryClick(sequence: Int) {
        pvm.selectedSequence.value = sequence
        Log.v("historys", "selectedSequence : ${pvm.selectedSequence.value}, currentSequence: ${pvm.currentSequence} sequence : $sequence")
        if (sequence % program.programFrequency == 0) {
            binding.rvPCD.visibility = View.GONE
            binding.tvPCDHealing.visibility = View.VISIBLE
        } else {
            binding.rvPCD.visibility = View.VISIBLE
            binding.tvPCDHealing.visibility = View.GONE
            setAdapter(pvm.currentProgram!!, pvm.currentProgresses[sequence], Pair(pvm.currentSequence, pvm.selectedSequence.value!!))
        }
    }

    fun dismissThisFragment() {
        dismiss()
    }

    // 해당 회차에서 가장
    private fun findCurrentIndex(progresses: MutableList<ProgressUnitVO>) : Int {
        // Case 1 초기상태
        if (progresses.all { it.lastProgress == 0 }) {
            Log.v("progressIndex", "0")
            return 0
        }
        val progressIndex1 = progresses.indexOfLast { it.lastProgress in 1 until it.videoDuration }
        if (progressIndex1 != -1) {
            return progressIndex1
        }

        val progressIndex = progresses.indexOfLast { it.lastProgress == it.videoDuration }
        if (progressIndex != progresses.size) {
            Log.v("progressIndex", "${progressIndex +  1}")
            return progressIndex + 1
        } else {
            return -1
        }
    }

}