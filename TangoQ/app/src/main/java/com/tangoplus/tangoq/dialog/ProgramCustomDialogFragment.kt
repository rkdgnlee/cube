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
import com.tangoplus.tangoq.data.ProgressVO
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
import com.tangoplus.tangoq.`object`.Singleton_t_measure
import com.tangoplus.tangoq.`object`.Singleton_t_progress
import com.tangoplus.tangoq.`object`.Singleton_t_user
import kotlinx.coroutines.CoroutineScope
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
    var programIndex = 0
    private lateinit var program : ProgramVO

    companion object {
        private const val ARG_PROGRAM_SN = "program_sn"
        private const val ARG_PROGRAM_INDEX = "program_index"
        private const val ARG_RECOMMENDATION_SN = "recommendation_sn"

        fun newInstance(programSn: Int, recommendationSn: Int, programIndex: Int) : ProgramCustomDialogFragment {
            val fragment = ProgramCustomDialogFragment()
            val args = Bundle()
            args.putInt(ARG_PROGRAM_SN, programSn)
            args.putInt(ARG_RECOMMENDATION_SN, recommendationSn)
            args.putInt(ARG_PROGRAM_INDEX, programIndex)
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
        recommendationSn = arguments?.getInt(ARG_RECOMMENDATION_SN)!!
        programIndex = arguments?.getInt(ARG_PROGRAM_INDEX)!!
        ssm = SaveSingletonManager(requireContext())

        lifecycleScope.launch {
            // 프로그램에 들어가는 운동
            program = fetchProgram("https://gym.tangostar.co.kr/tango_gym_admin/programs/read.php", programSn.toString())
            pvm.currentProgram = program

            val jo = JSONObject()
            jo.put("user_sn", userJson?.optString("user_sn"))
            jo.put("recommendation_sn", recommendationSn)
            jo.put("exercise_program_sn", programSn)
            jo.put("measure_sn", mvm.selectedMeasure?.measureId?.toInt())
            Log.v("json프로그레스", "${jo}")

            withContext(Dispatchers.IO) {
                ssm.getOrInsertProgress(jo)
                pvm.currentProgresses = Singleton_t_progress.getInstance(requireContext()).progresses?.get(programIndex)!! // mutableListOf<MutableList<ProgressUnitVO>> 에서 program index에 맞게 가져오기.
                Log.v("현재진행기록", "pvm.currentProgresses.size: ${pvm.currentProgresses.size}" ) // week * seq 12개의 list인 mutableLIst<ProgressUnitVO>임.  seq는 맞음. 그러면 각각 21개의 seq가 들어가있음 12 * 21 근데 여기서

                // ------# 가장 최근 sequence 찾기 #------
                val currentProgresses = pvm.currentProgresses // MutableList<MutableList<ProgressUnitVO>>
                var currentWeek = 0
                var currentSequence = 0
                for (weekIndex in currentProgresses.indices) { // 12개의 리스트 (week)
                    val weekProgress = currentProgresses[weekIndex]
                    // week에서 가장 큰 sequence 값 찾기
                    val maxSequenceInWeek = weekProgress.maxOf { it.currentSequence }

                    if (maxSequenceInWeek == 3) {
                        // 해당 주의 모든 운동이 완료되어 다음 주로 넘어감
                        continue
                    } else {
                        // 가장 큰 sequence 값이 3이 아니면, 그 주차가 현재 주차임
                        currentWeek = weekIndex + 1 // 주차는 1부터 시작하므로 +1
                        currentSequence = maxSequenceInWeek
                        break
                    }
                }

                val currentRound = (currentWeek - 1) * 3 + currentSequence
                pvm.currentSequence = currentRound
                Log.v("currentSeq", "currentSequence: ${pvm.currentSequence}")
            }
            withContext(Dispatchers.Main) {
                setAdapter(pvm.currentProgram!!, pvm.currentProgresses[pvm.currentSequence],  Pair(pvm.currentSequence, pvm.currentSequence))

                    // ------! 요약 시작 !------
                binding.etPCDTitle.isEnabled = false
                binding.btnPCDRight.text = "운동 시작하기"
                binding.btnPCDLeft.visibility = View.GONE
                binding.etPCDTitle.setText(pvm.currentProgram?.programName)
                binding.tvPCDTime.text = (if (pvm.currentProgram?.programTime!! <= 60) {
                    "${pvm.currentProgram?.programTime}초"
                } else {
                    "${pvm.currentProgram?.programTime!! / 60 }분 ${pvm.currentProgram?.programTime!! % 60}초"
                }).toString()
                when (pvm.currentProgram?.programStage) {
                    "초급" -> binding.tvPCDStage.text = "초급자"
                    "중급" -> binding.tvPCDStage.text = "중급자"
                    "고급" -> binding.tvPCDStage.text = "상급자"
                }
                binding.tvPCDCount.text = "${pvm.currentProgram?.programCount} 개"
            // ------! 요약 끝 !------
            }
        }

        binding.btnPCDLeft.setOnClickListener { dismiss() }
        binding.btnPCDRight.setOnClickListener {
            when (binding.btnPCDRight.text) {
                "운동 시작하기" -> {
                    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                    val currentSequenceProgresses = pvm.currentProgresses[pvm.currentSequence]
                    val mostRecentProgress = currentSequenceProgresses.maxByOrNull {
                        LocalDateTime.parse(it.updateDate, formatter)
                    }

                    mostRecentProgress?.let { recentProgress ->
                        // 가장 최근 업데이트된 ProgressUnitVO의 인덱스를 찾습니다.
                        val startIndex = currentSequenceProgresses.indexOf(recentProgress)

                        val filteredExercises = pvm.currentProgram?.exercises?.dropWhile {
                            it.exerciseId != recentProgress.exerciseId.toString()
                        } ?: emptyList()

                        val videoUrls = storeUrls(filteredExercises.toMutableList())

                        val exerciseIds = currentSequenceProgresses.subList(startIndex, currentSequenceProgresses.size)
                            .filter { it.lastProgress > 0 }
                            .map { it.exerciseId.toString() }
                            .toMutableList()

                        val uvpIds = currentSequenceProgresses.subList(startIndex, currentSequenceProgresses.size)
                            .map { it.uvpSn.toString() }
                            .distinct()

                        Log.v("안본것들만filter", "exerciseIds: ${exerciseIds}, videoUrls: $videoUrls, uvpIds: $uvpIds")

                        val intent = Intent(requireContext(), PlayFullScreenActivity::class.java)
                        intent.putStringArrayListExtra("video_urls", ArrayList(videoUrls))
                        intent.putStringArrayListExtra("exercise_ids", ArrayList(exerciseIds))
                        intent.putStringArrayListExtra("uvp_sns", ArrayList(uvpIds))
                        intent.putExtra("total_time", pvm.currentProgram?.programTime)

                        requireContext().startActivity(intent)
                        startActivityForResult(intent, 8080)
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
        return pvm.currentProgram?.exerciseTimes?.find { it.first == id }?.second!!
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

//    private fun getWeeklyExerciseHistory(data: MutableList<Triple<String, Int, Int>>?): MutableList<Triple<String, Int, Int>>? {
//        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
//        val currentDateTime = LocalDateTime.now(ZoneId.of("Asia/Seoul"))
//        val sevenDaysAgo = currentDateTime.minusDays(6).truncatedTo(ChronoUnit.DAYS)
//
//        val filteredData = data?.filter {
//            it.first != "null" &&
//                    LocalDateTime.parse(it.first, formatter).isAfter(sevenDaysAgo.minusSeconds(1)) &&
//                    LocalDateTime.parse(it.first, formatter).isBefore(currentDateTime.plusDays(1).truncatedTo(
//                        ChronoUnit.DAYS))
//        }?.toMutableList()
//
//        val completeData = mutableListOf<Triple<String, Int, Int>>()
//        for (i in 0..6) {
//            val date = sevenDaysAgo.plusDays(i.toLong())
//            val nextDate = date.plusDays(1)
//
//            val entry = filteredData?.find {
//                val entryDateTime = LocalDateTime.parse(it.first, formatter)
//                entryDateTime.isAfter(date.minusSeconds(1)) && entryDateTime.isBefore(nextDate)
//            }
//            if (entry != null) {
//                completeData.add(entry)
//            } else {
//                // 빈 데이터의 경우 해당 날짜의 자정(00:00:00)으로 설정
//                val dateString = date.format(formatter)
//                completeData.add(Triple(dateString, 0, 0))
//            }
//        }
//        Log.v("completedData", "$completeData")
//        return completeData
//    }

    private fun stringToLocalDate(dateTimeString: String): LocalDate {
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        val localDateTime = LocalDateTime.parse(dateTimeString, formatter)
        return localDateTime.toLocalDate()
    }

    private fun setAdapter(program: ProgramVO, progresses : MutableList<ProgressUnitVO>?, sequence: Pair<Int, Int>) {

        /* currentSequence 는 진행중인 주차, 진행중인 회차, 선택된 회차 이렇게 나눠짐 */
        pvm.selectedSequence.value = sequence.second
        val adapter = ProgramCustomRVAdapter(this@ProgramCustomDialogFragment,
            program.programWeek * program.programFrequency,
            Pair(pvm.currentSequence, pvm.selectedSequence.value!!),
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
        // TODO viewModel에 담은 program에서 필터링해서 보여주기
        pvm.selectedSequence.value = sequence
        Log.v("historys", "selectedSequence : ${pvm.selectedSequence.value}, currentSequence: ${pvm.currentSequence} sequence : $sequence")
        setAdapter(pvm.currentProgram!!, pvm.currentProgresses[sequence], Pair(pvm.currentSequence, pvm.selectedSequence.value!!))
    }

    fun dismissThisFragment() {
        dismiss()
    }
}