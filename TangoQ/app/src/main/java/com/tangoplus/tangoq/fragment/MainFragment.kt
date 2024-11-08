package com.tangoplus.tangoq.fragment

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Rect
import android.os.Bundle
import android.util.DisplayMetrics
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.google.android.gms.common.util.DeviceProperties.isTablet
import com.tangoplus.tangoq.MainActivity
import com.tangoplus.tangoq.R
import com.tangoplus.tangoq.`object`.Singleton_t_user
import com.tangoplus.tangoq.adapter.BalanceRVAdapter
import com.tangoplus.tangoq.adapter.ExerciseRVAdapter
import com.tangoplus.tangoq.adapter.StringRVAdapter
import com.tangoplus.tangoq.broadcastReceiver.AlarmController
import com.tangoplus.tangoq.db.PreferencesManager
import com.tangoplus.tangoq.data.MeasureVO
import com.tangoplus.tangoq.data.MeasureViewModel
import com.tangoplus.tangoq.data.ProgressUnitVO
import com.tangoplus.tangoq.data.UserViewModel
import com.tangoplus.tangoq.databinding.FragmentMainBinding
import com.tangoplus.tangoq.dialog.AlarmDialogFragment
import com.tangoplus.tangoq.dialog.GuideDialogFragment
import com.tangoplus.tangoq.dialog.ProgramCustomDialogFragment
import com.tangoplus.tangoq.dialog.QRCodeDialogFragment
import com.tangoplus.tangoq.dialog.MeasureBSDialogFragment
import com.tangoplus.tangoq.`object`.DeviceService.isNetworkAvailable
import com.tangoplus.tangoq.`object`.NetworkProgram.fetchProgram
import com.tangoplus.tangoq.`object`.NetworkProgress.getLatestProgress
import com.tangoplus.tangoq.`object`.NetworkProgress.getWeekProgress
import com.tangoplus.tangoq.`object`.Singleton_t_measure
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainFragment : Fragment() {
    lateinit var binding: FragmentMainBinding
    val viewModel : UserViewModel by activityViewModels()
    val mViewModel : MeasureViewModel by activityViewModels()
    private lateinit var startForResult: ActivityResultLauncher<Intent>
    lateinit var prefsManager : PreferencesManager
    private var measures : MutableList<MeasureVO>? = null
    private var singletonMeasure : MutableList<MeasureVO>? = null
    var latestRecSn = -1


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentMainBinding.inflate(inflater)

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
        prefsManager.setUserSn(Singleton_t_user.getInstance(requireContext()).jsonObject?.optInt("sn")!!)

        latestRecSn = prefsManager.getLatestRecommendation()

//        AlarmController(requireContext()).setNotificationAlarm("TangoQ", "점심공복에는 운동효과가 더 좋답니다.", 14, 2)
//        AlarmController(requireContext()).setNotificationAlarm("TangoQ", "식곤증을 위해 스트레칭을 추천드려요.", 13, 36)

        singletonMeasure = Singleton_t_measure.getInstance(requireContext()).measures

        // ------# 알람 intent #------
        binding.ibtnMAlarm.setOnClickListener {
            val dialog = AlarmDialogFragment()
            dialog.show(requireActivity().supportFragmentManager, "AlarmDialogFragment")
        }

        binding.ibtnMQRCode.setOnClickListener{
            val dialog = QRCodeDialogFragment()
            dialog.show(requireActivity().supportFragmentManager, "LoginScanDialogFragment")
        }

        if (isFirstRun("GuideDialogFragment_isFirstRun")) {
            val dialog = GuideDialogFragment()
            dialog.show(requireActivity().supportFragmentManager, "GuideDialogFragment")
        }

        when (isNetworkAvailable(requireContext())) {
            true -> {
                measures = Singleton_t_measure.getInstance(requireContext()).measures
//                measures = mutableListOf()
                binding.llM.visibility = View.VISIBLE
                binding.sflM.startShimmer()

                // ------# 초기 measure 설정 #------
                if (!measures.isNullOrEmpty()) {
                    mViewModel.selectedMeasureDate.value = measures!!.get(0).regDate
                    mViewModel.selectMeasureDate.value = measures!!.get(0).regDate
                }
                Log.v("선택된measureDate", "${mViewModel.selectedMeasureDate.value}")
                updateUI()

                binding.tvMMeasureDate.setOnClickListener {
                    val dialog = MeasureBSDialogFragment()
                    dialog.show(requireActivity().supportFragmentManager, "MeasureBSDialogFragment")
                }
            }
            false -> {
                // ------# 인터넷 연결이 없을 때 #------
            }
        }
    }

    // 상단 어댑터와 하단 어댑터 같이 나옴
    private fun setAdapter(index: Int) {
        val layoutManager1 = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        binding.rvM1.layoutManager = layoutManager1
        val muscleAdapter = StringRVAdapter(this@MainFragment, measures?.get(index)?.dangerParts?.map { it.first }?.toMutableList(), "part", mViewModel)
        binding.rvM1.adapter = muscleAdapter
        binding.rvM1.isNestedScrollingEnabled = false

        // ------# balance check #------
        /* 이미 순위가 매겨진 부위들을 넣어서 index별로 각 밸런스 체크에 들어간다 */
        // TODO 고정으로 일단 위험부위와 점수는 넣어놓기.
//        val dangerParts = measures?.get(index)?.dangerParts?.map { it.first }?.toMutableList()
//        val stages = mutableListOf<MutableList<String>>()
//        stages.add( dangerParts?.subList(0, 2)!!)
//        stages.add( dangerParts.subList(0, 2))
//        stages.add( dangerParts.subList(0, 2))
//        stages.add( dangerParts.subList(2, 3))
//        stages.add( dangerParts.subList(2, 3))
//
//        val dangerDegree = measures?.get(index)?.dangerParts?.map { it.second }?.toMutableList()
//        val degrees = mutableListOf<Pair<Int,Int>>()
//        for (i in 0 until 5) {
//            val degree = dangerDegree?.getOrNull(i) ?: -1  // null이면 기본값으로 -1 사용
//            when (degree) {
//                1 -> degrees.add(Pair(1, Random.nextInt(2, 4)))
//                2 -> degrees.add(Pair(2, Random.nextInt(-1, 2)))
//                else -> degrees.add(Pair(3, Random.nextInt(-2, -1)))  // 여기서 else는 null 또는 다른 값에 대한 처리
//            }
//        }
        val stages = mutableListOf<MutableList<String>>()
        val balanceParts1 = mutableListOf("어깨", "골반")
        stages.add(balanceParts1)
        val balanceParts2 = mutableListOf("어깨", "팔꿉", "좌측 전완")
        stages.add(balanceParts2)
        val balanceParts3 = mutableListOf("골반", "좌측 어깨", "목")
        stages.add(balanceParts3)
        val balanceParts4 = mutableListOf("허벅지",  "어깨")
        stages.add(balanceParts4)
        val balanceParts5 = mutableListOf("좌측 허벅지", "좌측 골반", "좌측 어깨")
        stages.add(balanceParts5)

        val degrees =  mutableListOf(Pair(1, 3), Pair(1,0), Pair(1, 2), Pair(2, -1), Pair(0 , -4))
//        val degrees =  mutableListOf(Pair(1, 3), Pair(1,0), Pair(1, 2), Pair(2, -1), Pair(0 , -4))
        // 부위에 대한 설명 타입 - 근육 긴장, 이상 감지, 불균형 등

        val layoutManager2 = LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
        binding.rvM3.layoutManager = layoutManager2
        val balanceAdapter = BalanceRVAdapter(this@MainFragment, stages, degrees)
        binding.rvM3.adapter = balanceAdapter
    }

    // 블러 유무 판단하기
    private fun updateUI() {
        Log.v("measure있는지", "${measures}")

        if (measures.isNullOrEmpty()) {
            // ------# measure에 뭐라도 들어있으면 위 코드 #-------
            binding.tvMTitle.text = "${Singleton_t_user.getInstance(requireContext()).jsonObject?.getString("user_name")}님"
            binding.constraintLayout2.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.white))
            binding.tvMMeasureDate.visibility = View.GONE
            binding.tvMOverall.text = "-"
            binding.tvMMeasureResult1.text = "측정 데이터가 없습니다."
            binding.tvMMeasureResult2.text = "키오스크, 모바일을 통해 측정을 진행해주세요"
            binding.rvM1.visibility = View.GONE
            binding.llM.visibility = View.GONE
            binding.vpM.visibility = View.GONE
            binding.tvMProgram.visibility = View.GONE

            binding.rvM3.visibility = View.GONE
            binding.tvMCustom.visibility = View.GONE

            binding.btnMProgram.apply {
                text = "프로그램 추천 받기"
                setOnClickListener{
                    (activity as? MainActivity)?.launchMeasureSkeletonActivity()
                }
            }
        } else {
            measures?.let { measure ->
                Log.v("measure있는지", "${measure}")
                if (measure.size > 0) {

                    binding.constraintLayout2.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.secondBgContainerColor))
                    binding.tvMMeasureDate.visibility = View.VISIBLE
                    binding.rvM1.visibility = View.VISIBLE
                    binding.tvMTitle.text = "측정정보"
                    binding.llM.visibility = View.VISIBLE
                    binding.vpM.visibility = View.VISIBLE
                    binding.tvMProgram.visibility = View.VISIBLE
                    binding.tvMCustom.visibility = View.VISIBLE
                    binding.rvM3.visibility = View.VISIBLE
                    binding.btnMProgram.setOnClickListener {
                        requireActivity().supportFragmentManager.beginTransaction().apply {
                            replace(R.id.flMain, ProgramSelectFragment())
                            addToBackStack(null)
                            commit()
                        }
                    }
                    // ------# 바텀시트에서 변한 selectedMeasureDate 에 맞게 변함.


                    mViewModel.selectedMeasureDate.observe(viewLifecycleOwner) { selectedDate ->
                        Log.v("현재 선택된 날짜", "$selectedDate")
                        val dateIndex = measures?.indexOf(measures?.find { it.regDate == selectedDate }!!)
                        // ------# 매칭 프로그램이 있는지 없는지 확인하기 #------
                        // measureVo에 매칭 프로그램스가 있는데 이걸 연결해줘야함 그럼 어떻게? -> MeasureVO에 일단 다 빈값인데 내가 만약 값들을 넣었을 때, 그냥
                        if (dateIndex != null) {
                            measures?.get(dateIndex)?.recommendations
                        }

                        if (selectedDate != measures?.get(0)?.regDate) {
                            setProgramButton(false)
                        } else {
                            setProgramButton(true)
                        }

                        binding.tvMMeasureDate.text = measure.get(dateIndex!!).regDate.substring(0, 10)
                        binding.tvMOverall.text = measure.get(dateIndex).overall.toString()
                        setAdapter(dateIndex)

                        Log.v("메인Date", "dateIndex: ${dateIndex}, selectedDate: $selectedDate, singletonMeasure: ${measures!![dateIndex].dangerParts}, ${measures!![dateIndex].recommendations}")
                        if (measures?.get(dateIndex)?.dangerParts?.size!! > 1) {
                            binding.tvMMeasureResult1.text = "${measures?.get(dateIndex)?.dangerParts?.get(0)?.first}부위가 부상 위험이 있습니다."
                            binding.tvMMeasureResult2.text = "${measures?.get(dateIndex)?.dangerParts?.get(1)?.first}부위가 부상 위험이 있습니다."
                        } else {
                            binding.tvMMeasureResult1.text = "${measures?.get(dateIndex)?.dangerParts?.get(0)?.first}부위가 부상 위험이 있습니다."
                        }

                        // ------# 측정 결과에 맞는 진행 프로그램 2번째 가져오기 #------
                        CoroutineScope(Dispatchers.IO).launch {
                            /* 1. 가장 최근에 한 운동 가져오기
                             *  2. 그 값에서 가져와서 해당 주차의 uvp 가져오기 week가 (동일한 uvp)
                             *  3. 그걸로만 rv 채우기.
                             * */
                            try {
                                val latestProgress = getLatestProgress(getString(R.string.API_progress), latestRecSn, requireContext())
                                val programSn = latestProgress.optInt("exercise_program_sn")  // 여기서 exception으로 나가짐.
                                var currentPage = 0
                                val adapter : ExerciseRVAdapter

                                // -------# 최근 진행 프로그램 가져오기 #------
                                withContext(Dispatchers.Main) {
                                    if (viewModel.processingProgram == null) {
                                        if (programSn != 0) {
                                            val week = latestProgress.optInt("week_number")
                                            Log.v("프로세싱있을때week", "week: ${week}")
                                            viewModel.processingProgram = fetchProgram(
                                                getString(R.string.API_programs),
                                                requireContext(),
                                                programSn.toString()
                                            )
                                            withContext(Dispatchers.IO) {
                                                val progresses = getWeekProgress(
                                                    getString(R.string.API_progress),
                                                    latestRecSn,
                                                    week,
                                                    requireContext()
                                                )
                                                currentPage = findCurrentIndex(progresses)
                                                withContext(Dispatchers.Main) {
                                                    Log.w("저장안된프로그램", "${viewModel.processingProgram?.programSn!!}")
                                                    adapter = ExerciseRVAdapter(
                                                        this@MainFragment,
                                                        viewModel.processingProgram?.exercises!!,
                                                        progresses,
                                                        Pair(currentPage, currentPage),
                                                        null,
                                                        "history"
                                                    )
                                                }
                                            }
                                        } else {
                                            viewModel.processingProgram = fetchProgram(
                                                getString(R.string.API_programs),
                                                requireContext(),
                                                mViewModel.selectedMeasure?.recommendations?.get(0)?.programSn.toString()
                                            )
                                            withContext(Dispatchers.Main) {
                                                Log.w("저장안된프로그램", "${viewModel.processingProgram?.programSn!!}")
                                                adapter = ExerciseRVAdapter(
                                                    this@MainFragment,
                                                    viewModel.processingProgram?.exercises!!,
                                                    null,
                                                    null,
                                                    null,
                                                    "history"
                                                )
                                            }
                                        }
                                    } else {

                                        if (programSn != 0) {
                                            val week = latestProgress.optInt("week_number")
                                            withContext(Dispatchers.IO) {
                                                val progresses = getWeekProgress(
                                                    getString(R.string.API_progress),
                                                    latestRecSn,
                                                    week,
                                                    requireContext()
                                                )
                                                currentPage = findCurrentIndex(progresses)
                                                withContext(Dispatchers.Main) {
                                                    Log.w("저장된프로그램", "${viewModel.processingProgram?.programSn!!}")

                                                    adapter = ExerciseRVAdapter(
                                                        this@MainFragment,
                                                        viewModel.processingProgram?.exercises!!,
                                                        progresses,
                                                        Pair(currentPage, currentPage),
                                                        null,
                                                        "history"
                                                    )
                                                }
                                            }

                                        } else {
                                            withContext(Dispatchers.Main) {
                                                Log.w("저장된프로그램", "${viewModel.processingProgram?.programSn!!}")
                                                adapter = ExerciseRVAdapter(
                                                    this@MainFragment,
                                                    viewModel.processingProgram?.exercises!!,
                                                    null,
                                                    null,
                                                    null,
                                                    "history"
                                                )
                                            }
                                        }
                                    }
                                }

                                CoroutineScope(Dispatchers.Main).launch {

                                    // -------# 최근 진행 프로그램 뷰페이저 #------
                                    binding.vpM.orientation = ViewPager2.ORIENTATION_HORIZONTAL
                                    binding.vpM.apply {
                                        clipToPadding = false
                                        clipChildren = false
                                        offscreenPageLimit = 3
                                        setAdapter(adapter)
                                        currentItem = currentPage
                                        var dp = 5
                                        try {
                                            dp = when (isTablet(requireContext())) {
                                                true -> 28
                                                false -> 5
                                            }
                                        } catch (e:IllegalStateException) {
                                            Log.e("MainFragmentError", "${e.printStackTrace()}")
                                        }

                                        (getChildAt(0) as RecyclerView).apply {
                                            setPadding(dpToPx(dp), 0, dpToPx(dp), 0)
                                            clipToPadding = false
                                        }
                                    }
                                    val itemDecoration = object : RecyclerView.ItemDecoration() {
                                        override fun getItemOffsets(
                                            outRect: Rect,
                                            view: View,
                                            parent: RecyclerView,
                                            state: RecyclerView.State
                                        ) {
                                            val position = parent.getChildAdapterPosition(view)
                                            val itemCount = state.itemCount

                                            if (position == 0) {
                                                outRect.left = 0
                                            } else {
                                                outRect.left = dpToPx(5)
                                            }

                                            if (position == itemCount - 1) {
                                                outRect.right = 0
                                            } else {
                                                outRect.right = dpToPx(5)
                                            }
                                        }
                                    }
                                    binding.vpM.addItemDecoration(itemDecoration)
                                    binding.llM.visibility = View.GONE
                                    binding.sflM.stopShimmer()


                                    // ------# 최근 진행 프로그램의 상세 보기로 넘어가기 #------
                                    binding.tvMProgram.setOnClickListener {
                                        try {
                                            val programSn = viewModel.processingProgram?.programSn ?: throw IllegalStateException("Program SN is null")
                                            val recommendationSn = when {
                                                prefsManager.getLatestRecommendation() != -1 -> {
                                                    prefsManager.getLatestRecommendation()
                                                }
                                                mViewModel.selectedMeasure?.recommendations?.isNotEmpty() == true -> {
                                                    mViewModel.selectedMeasure?.recommendations?.get(0)?.recommendationSn
                                                        ?: throw IllegalStateException("Recommendation SN is null")
                                                }
                                                else -> {
                                                    throw IllegalStateException("No valid recommendation available")
                                                }
                                            }
                                            ProgramCustomDialogFragment.newInstance(programSn, recommendationSn)
                                                .show(requireActivity().supportFragmentManager, "ProgramCustomDialogFragment")
                                        } catch (e: IllegalStateException) {
                                            Log.e("프로그램오류", "${e.printStackTrace()}")
                                        }
                                    }
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                                Log.e("Error>Program", "${e.message} $e")
                            }
                        }
                    }
                }
            }
        }
    }

    private fun dpToPx(dp: Int) : Int {
        return (dp * resources.displayMetrics.density).toInt()
    }

    private fun findCurrentIndex(progresses: MutableList<ProgressUnitVO>) : Int {
        val progressIndex = progresses.indexOfFirst { it.lastProgress > 0 && it.lastProgress < it.videoDuration }
        if (progressIndex != -1) {
            Log.v("progressIndex", "${progressIndex}")
            return progressIndex
        }

        for (i in 1 until progresses.size) {
            val prev = progresses[i - 1].currentSequence
            val current = progresses[i].currentSequence
            if ((prev == 3 && current == 2) ||
                (prev == 2 && current == 1) ||
                (prev == 1 && current == 0)) {
                return i
            }
        }
        Log.v("progressIndex", "${progressIndex}")
        return 0
    }

    private fun setProgramButton(isEnabled: Boolean) {
        if (isEnabled) {
            binding.btnMProgram.backgroundTintList = ContextCompat.getColorStateList(requireContext(), R.color.mainColor)
            binding.btnMProgram.text = "프로그램 선택하기"
        } else {
            binding.btnMProgram.text = "프로그램 완료, 재측정을 진행해 주세요"
            binding.btnMProgram.backgroundTintList = ContextCompat.getColorStateList(requireContext(), R.color.subColor150)
        }
    }
}