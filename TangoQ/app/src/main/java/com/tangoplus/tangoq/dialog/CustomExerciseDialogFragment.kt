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
import com.tangoplus.tangoq.adapter.CustomExerciseRVAdapter
import com.tangoplus.tangoq.adapter.ExerciseRVAdapter
import com.tangoplus.tangoq.data.EpisodeVO
import com.tangoplus.tangoq.data.ExerciseVO
import com.tangoplus.tangoq.data.ExerciseViewModel
import com.tangoplus.tangoq.data.ProgramVO
import com.tangoplus.tangoq.databinding.FragmentCustomExerciseDialogBinding
import com.tangoplus.tangoq.db.PreferencesManager
import com.tangoplus.tangoq.fragment.isFirstRun
import com.tangoplus.tangoq.listener.OnCustomCategoryClickListener
import com.tangoplus.tangoq.`object`.Singleton_t_history
import com.tangoplus.tangoq.`object`.Singleton_t_user

class CustomExerciseDialogFragment : DialogFragment(), OnCustomCategoryClickListener {
    lateinit var binding : FragmentCustomExerciseDialogBinding
    val viewModel : ExerciseViewModel by activityViewModels()
    private var historys =  mutableListOf<MutableList<EpisodeVO>>()
    private lateinit var  singletonHistory : Singleton_t_history
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentCustomExerciseDialogBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)



        binding.ibtnCEDMore.setOnClickListener {  }

        val userJson = Singleton_t_user.getInstance(requireContext()).jsonObject


        if (viewModel.currentProgram?.programSn != -1) {
            binding.etCEDTitle.isEnabled = false
            binding.btnCEDRight.text = "운동 시작하기"
            binding.btnCEDLeft.visibility = View.GONE

            // ------# select program으로 필터링 했을 때 #------
        } else if (viewModel.currentProgram?.programSn == -1) {
            binding.btnCEDRight.text = "프로그램 선택하기"
            binding.ibtnCEDTop.setImageDrawable(resources.getDrawable(R.drawable.icon_edit))
            binding.etCEDTitle.isEnabled = true
            binding.btnCEDLeft.visibility = View.VISIBLE
        }

//      program = ProgramVO(-1, "",  "", "",0,0 ,0,mutableListOf())


        // ------! 요약 시작 !------
        // TODO 주차별 운동 변동해야함.
        binding.etCEDTitle.setText("어깨 재활 프로그램")

        binding.tvCEDTime.text = (if (viewModel.currentProgram?.programTime!! <= 60) {
            "${viewModel.currentProgram?.programTime}초"
        } else {
            "${viewModel.currentProgram?.programTime!! / 60}분 ${viewModel.currentProgram?.programTime!! % 60}초"
        }).toString()
        when (viewModel.currentProgram?.programStage) {
            "초급" -> binding.tvCEDStage.text = "초급자"
            "중급" -> binding.tvCEDStage.text = "중급자"
            "고급" -> binding.tvCEDStage.text = "상급자"
        }
        binding.tvCEDCount.text = "${viewModel.currentProgram?.programCount} 개"

        val userSn = userJson?.optString("user_sn").toString()
        val prefsManager = PreferencesManager(requireContext())
        // ------! 요약 끝 !------

        // ------# 주차 회차 데이터 넣는 곳 #------
        singletonHistory = Singleton_t_history.getInstance(requireContext())
        historys = singletonHistory.historys!!
        Log.v("싱글턴historys2", "${singletonHistory.historys}")
        viewModel.currentEpisode = 2
        viewModel.currentWeek = 1
        viewModel.selectedWeek.value = 1
        setAdapter(viewModel.currentProgram!!, historys, viewModel.selectedWeek.value!!,   viewModel.currentEpisode)

        // ------! 주차 변경 시작 !------
        binding.tvCEDWeekly.setOnClickListener {
            val dialog = ExerciseWeeklyBSDialogFragment()
            dialog.show(requireActivity().supportFragmentManager, "ExerciseWeeklyBSDialogFragment")
        }
        viewModel.selectedWeek.observe(viewLifecycleOwner) {
            binding.tvCEDWeekly.text = "${it+1}/4 주차"
            setAdapter(viewModel.currentProgram!!, historys, it,  viewModel.currentEpisode)
        }
        // ------! 주차 변경 끝 !------



        binding.btnCEDLeft.setOnClickListener { dismiss() }
        binding.btnCEDRight.setOnClickListener {
            when (binding.btnCEDRight.text) {
                "운동 시작하기" -> {
                    val videoUrls = storeUrls(viewModel.currentProgram?.exercises!![viewModel.selectedWeek.value!!].toMutableList())
                    val exerciseIds = viewModel.currentProgram?.exercises!![viewModel.selectedWeek.value!!].filter { exercise ->
                        val relevantHistories = historys[viewModel.selectedWeek.value!!][viewModel.selectedEpisode.value!!].doingExercises.filter { history ->
                            history.exerciseId == exercise.exerciseId
                        }

                        relevantHistories.any { it.viewCount == 0 } || relevantHistories.any { it.lastPosition!! > 0 }
                    }.map { it.exerciseId }.toMutableList()
                    Log.v("안본 것들만 filter", "${exerciseIds}")

                    val intent = Intent(requireContext(), PlayFullScreenActivity::class.java)
                    intent.putStringArrayListExtra("video_urls", ArrayList(videoUrls))
                    intent.putStringArrayListExtra("exercise_ids", ArrayList(exerciseIds))
                    intent.putExtra("total_time", viewModel.currentProgram?.programTime)

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
            binding.ibtnCEDTop.showAlignBottom(balloon2)
            balloon2.dismissWithDelay(1800L)
        }


        binding.ibtnCEDTop.setOnClickListener { it.showAlignBottom(balloon2) }


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
        viewModel.selectedEpisode.value = episode
        val adapter = CustomExerciseRVAdapter(this@CustomExerciseDialogFragment, historys[week], Pair(viewModel.currentWeek, week), Pair(viewModel.currentEpisode, viewModel.selectedEpisode.value!!), this@CustomExerciseDialogFragment)
        val layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        binding.rvCEDHorizontal.layoutManager = layoutManager
        binding.rvCEDHorizontal.adapter = adapter
        adapter.notifyDataSetChanged()

        val adapter2 = ExerciseRVAdapter(this@CustomExerciseDialogFragment, program.exercises!![week].toMutableList(), historys.get(week).get(episode), "main")
        binding.rvCED.adapter = adapter2
        val layoutManager2 = LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
        binding.rvCED.layoutManager = layoutManager2

        adapter2.notifyDataSetChanged()
    }

    override fun customCategoryClick(episode: Int) {
        // TODO viewModel에 담은 program에서 필터링해서 보여주기
        viewModel.selectedEpisode.value = episode
        Log.v("historys", "selectEpisode :${viewModel.selectedEpisode.value}, currentEpisode: ${viewModel.currentEpisode} position : $episode")

        setAdapter(viewModel.currentProgram!!, historys, viewModel.selectedWeek.value!!,  viewModel.selectedEpisode.value!!)

    }
}