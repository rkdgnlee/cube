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
import com.tangoplus.tangoq.data.EpisodeVO
import com.tangoplus.tangoq.data.ExerciseVO
import com.tangoplus.tangoq.data.ExerciseViewModel
import com.tangoplus.tangoq.data.HistoryViewModel
import com.tangoplus.tangoq.data.ProgramVO
import com.tangoplus.tangoq.databinding.FragmentProgramCustomDialogBinding
import com.tangoplus.tangoq.db.PreferencesManager
import com.tangoplus.tangoq.fragment.isFirstRun
import com.tangoplus.tangoq.listener.OnCustomCategoryClickListener
import com.tangoplus.tangoq.`object`.Singleton_t_history
import com.tangoplus.tangoq.`object`.Singleton_t_user
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

class ProgramCustomDialogFragment : DialogFragment(), OnCustomCategoryClickListener {
    lateinit var binding : FragmentProgramCustomDialogBinding
    val viewModel : ExerciseViewModel by activityViewModels()
    val hViewModel : HistoryViewModel by activityViewModels()
    private var historys =  mutableListOf<MutableList<EpisodeVO>>()
    private lateinit var  singletonHistory : Singleton_t_history
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentProgramCustomDialogBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val userJson = Singleton_t_user.getInstance(requireContext()).jsonObject


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

//      program = ProgramVO(-1, "",  "", "",0,0 ,0,mutableListOf())


        // ------! 요약 시작 !------
        // TODO 주차별 운동 변동해야함.
        binding.etPCDTitle.setText(hViewModel.currentProgram?.programName)

        binding.tvPCDTime.text = (if (hViewModel.currentProgram?.programTime!! <= 60) {
            "${hViewModel.currentProgram?.programTime}초"
        } else {
            "${hViewModel.currentProgram?.programTime!! / 60}분 ${hViewModel.currentProgram?.programTime!! % 60}초"
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

        // ------# 주차 회차 데이터 넣는 곳 #------
        singletonHistory = Singleton_t_history.getInstance(requireContext())
        historys = singletonHistory.historys!!
        Log.v("싱글턴historys2", "${singletonHistory.historys}")

        /* TODO 현재 회차, 주차, 선택된 주차 까지 명시돼 있는데, 이제 주 단위로 주차가 변경된다면
        * viewModel에 담기는 값들이 다 달라져야 함.
        * */

        val programStartDate = LocalDate.parse("2024-08-26")
        // TODO 사용자 만의 프로그램 시작 날짜를 넣어야 함.
        val nowDate = LocalDate.now()

        val weeksPassed = ChronoUnit.WEEKS.between(programStartDate, nowDate).toInt() // 3으로 나온다. 그러면 4주차가 나옴
        Log.v("weekPassed", "${weeksPassed}")
        hViewModel.currentWeek = weeksPassed
        hViewModel.selectWeek.value = weeksPassed
        hViewModel.selectedWeek.value = weeksPassed

        // ------! 회차 계산 시작 !------
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        val lastRegDateStr = hViewModel.allHistorys.find { it.regDate != null && it.lastPosition!! > 0 }?.regDate

        val lastRegDate = LocalDateTime.parse(lastRegDateStr, formatter).toLocalDate()

        val weeksPassed2 = ChronoUnit.WEEKS.between(programStartDate, lastRegDate).toInt() // 마지막 운동 날짜
        if (weeksPassed2 == weeksPassed) { // 마지막 운동기록의 날짜가 이번 주차일 경우
            val currentWeekStartDate = programStartDate.plusWeeks(weeksPassed2.toLong())
            val currentWeekRecords = hViewModel.allHistorys.filter {
                val regDate = LocalDateTime.parse(it.regDate, formatter).toLocalDate()
                regDate.isAfter(currentWeekStartDate.minusDays(1)) && regDate.isBefore(currentWeekStartDate.plusWeeks(1))
            }.sortedBy { it.regDate }

            // 해당 주차의 운동 날짜 별로 가져 와서 회차가 몇인지 가져오기 ?
            hViewModel.currentEpisode = currentWeekRecords.size - 1
        } else {
            hViewModel.currentEpisode = 0
        }
        // ------! 회차 계산 끝 !------

        // ------# 프로그램 기간 만료 #------
        val programEndDate = programStartDate.plusWeeks(hViewModel.currentProgram!!.programWeek.toLong()).minusDays(1)
        Log.v("programDates", "프로그램 시작날짜: ${programStartDate}, 종료날짜: ${programEndDate}")
        if (LocalDate.now().isAfter(programEndDate)) {
            val programAlertDialogFragment = ProgramAlertDialogFragment.newInstance(this)
            programAlertDialogFragment.show(childFragmentManager, "ProgramAlertDialogFragment")

        } else {
            // ------# 데이터 가져온 후 #------
            setAdapter(hViewModel.currentProgram!!, historys, hViewModel.selectedWeek.value!!,   hViewModel.currentEpisode)

            hViewModel.selectedWeek.observe(viewLifecycleOwner) {
                binding.tvPCDWeekly.text = "${it+1}/${hViewModel.currentProgram!!.programWeek} 주차"
                setAdapter(hViewModel.currentProgram!!, historys, it,  hViewModel.currentEpisode)
            }
        }

        // ------! 주차 변경 시작 !------
        binding.tvPCDWeekly.setOnClickListener {
            val dialog = ProgramWeeklyBSDialogFragment()
            dialog.show(requireActivity().supportFragmentManager, "ExerciseWeeklyBSDialogFragment")
        }




        // ------! 주차 변경 끝 !------



        binding.btnPCDLeft.setOnClickListener { dismiss() }
        binding.btnPCDRight.setOnClickListener {
            when (binding.btnPCDRight.text) {
                "운동 시작하기" -> {
                    val videoUrls = storeUrls(hViewModel.currentProgram?.exercises!![hViewModel.selectedWeek.value!!].toMutableList())
                    val exerciseIds = hViewModel.currentProgram?.exercises!![hViewModel.selectedWeek.value!!].filter { exercise ->
                        val relevantHistories = historys[hViewModel.selectedWeek.value!!][hViewModel.selectedEpisode.value!!].doingExercises.filter { history ->
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

    private fun storeSns(currentItem: MutableList<ExerciseVO>) : MutableList<String> {
        val sns = mutableListOf<String>()
        for (i in currentItem.indices) {
            val exercises = currentItem[i]
            sns.add(exercises.exerciseId.toString())
        }
        Log.v("sns", "$sns")
        return sns
    }

    private fun setAdapter(program: ProgramVO, historys : MutableList<MutableList<EpisodeVO>>,week: Int, episode: Int) {

        /* currentEpisode 는 진행중인 주차, 진행중인 회차, 선택된 회차 이렇게 나눠짐 */
        hViewModel.selectedEpisode.value = episode
        val adapter = ProgramCustomRVAdapter(this@ProgramCustomDialogFragment, historys[week], Pair(hViewModel.currentWeek, week), Pair(hViewModel.currentEpisode, hViewModel.selectedEpisode.value!!), this@ProgramCustomDialogFragment)
        val layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        binding.rvPCDHorizontal.layoutManager = layoutManager
        binding.rvPCDHorizontal.adapter = adapter
        adapter.notifyDataSetChanged()

        val adapter2 = ExerciseRVAdapter(this@ProgramCustomDialogFragment, program.exercises!![week].toMutableList(), historys.get(week).get(episode), "main")
        binding.rvPCD.adapter = adapter2
        val layoutManager2 = LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
        binding.rvPCD.layoutManager = layoutManager2

        adapter2.notifyDataSetChanged()
    }

    override fun customCategoryClick(episode: Int) {
        // TODO viewModel에 담은 program에서 필터링해서 보여주기
        hViewModel.selectedEpisode.value = episode
        Log.v("historys", "selectEpisode :${hViewModel.selectedEpisode.value}, currentEpisode: ${hViewModel.currentEpisode} position : $episode")

        setAdapter(hViewModel.currentProgram!!, historys, hViewModel.selectedWeek.value!!,  hViewModel.selectedEpisode.value!!)

    }

    fun dismissThisFragment() {
        dismiss()
    }
}