package com.tangoplus.tangoq.fragment

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.graphics.Rect
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.tangoplus.tangoq.MainActivity
import com.tangoplus.tangoq.R
import com.tangoplus.tangoq.`object`.Singleton_t_user
import com.tangoplus.tangoq.adapter.BalanceRVAdapter
import com.tangoplus.tangoq.adapter.ExerciseRVAdapter
import com.tangoplus.tangoq.adapter.StringRVAdapter
import com.tangoplus.tangoq.broadcastReceiver.AlarmController
import com.tangoplus.tangoq.db.PreferencesManager
import com.tangoplus.tangoq.data.ExerciseViewModel
import com.tangoplus.tangoq.data.ProgressViewModel
import com.tangoplus.tangoq.data.MeasureVO
import com.tangoplus.tangoq.data.MeasureViewModel
import com.tangoplus.tangoq.data.ProgramVO
import com.tangoplus.tangoq.data.ProgressUnitVO
import com.tangoplus.tangoq.data.UserViewModel
import com.tangoplus.tangoq.databinding.FragmentMainBinding
import com.tangoplus.tangoq.dialog.AlarmDialogFragment
import com.tangoplus.tangoq.dialog.GuideDialogFragment
import com.tangoplus.tangoq.dialog.ProgramCustomDialogFragment
import com.tangoplus.tangoq.dialog.QRCodeDialogFragment
import com.tangoplus.tangoq.dialog.MeasureBSDialogFragment
import com.tangoplus.tangoq.`object`.DeviceService.isNetworkAvailable
import com.tangoplus.tangoq.`object`.NetworkExercise.fetchExerciseById
import com.tangoplus.tangoq.`object`.NetworkProgram.fetchProgram
import com.tangoplus.tangoq.`object`.NetworkProgress.getLatestProgress
import com.tangoplus.tangoq.`object`.NetworkProgress.getWeekProgress
import com.tangoplus.tangoq.`object`.Singleton_t_measure
import com.tangoplus.tangoq.`object`.Singleton_t_progress
import jp.wasabeef.blurry.Blurry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.random.Random

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
        latestRecSn = prefsManager.getLatestRecommendation()
        Log.v("latestRecSn", "${latestRecSn}")
        AlarmController(requireContext()).setNotificationAlarm("TangoQ", "점심공복에는 운동효과가 더 좋답니다.", 14, 2)
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
                val userJson = Singleton_t_user.getInstance(requireContext()).jsonObject
                measures = Singleton_t_measure.getInstance(requireContext()).measures
                binding.llM.visibility = View.VISIBLE
                binding.sflM.startShimmer()
                updateUI()


                binding.tvMMeasureDate.setOnClickListener {
                    val dialog = MeasureBSDialogFragment()
                    dialog.show(requireActivity().supportFragmentManager, "MeasureBSDialogFragment")
                }

                binding.btnMProgram.setOnClickListener {
                    requireActivity().supportFragmentManager.beginTransaction().apply {
//                        setCustomAnimations(R.anim.slide_in_left, R.anim.slide_in_right)
                        replace(R.id.flMain, ProgramSelectFragment())
                        addToBackStack(null)
                        commit()
                    }
                }

                binding.ivMBlur.setOnClickListener {
                    val bnb : BottomNavigationView = requireActivity().findViewById(R.id.bnbMain)
                    bnb.selectedItemId = R.id.measure
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
        measures?.let { measure ->
            if (measure.size > 0) {
                binding.constraintLayout2.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.secondBgContainerColor))
                binding.tvMMeasureDate.visibility = View.VISIBLE
                binding.rvM3.visibility = View.VISIBLE
                binding.tvMCustom.visibility = View.VISIBLE
                binding.tvMBlur.visibility = View.GONE
                binding.ivMBlur.visibility = View.GONE
                binding.clMMeasure.visibility = View.VISIBLE
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
                        val latestProgress = getLatestProgress(getString(R.string.API_progress), latestRecSn, requireContext())
                        val programSn = latestProgress.optInt("programSn")
                        Log.v("프로그램sn", "$programSn") //8 이 나옴
                        val adapter : ExerciseRVAdapter
                        val program : ProgramVO?
                        var currentPage = 0
                        try {

                            // -------# 최근 진행 프로그램 가져오기 #------
                            if (programSn != 0) {
                                val week = latestProgress.optInt("week_number")
                                program  = fetchProgram("https://gym.tangostar.co.kr/tango_gym_admin/programs/read.php", programSn.toString())

                                val progresses = getWeekProgress(getString(R.string.API_progress), latestRecSn, week, requireContext())
                                currentPage = findCurrentIndex(progresses)
                                adapter = ExerciseRVAdapter(this@MainFragment, program?.exercises!!, progresses, Pair(0,0) ,"history")

                            } else { // 기록이 없을 때를 말하는거임. 그렇다고 recommend가 없는 건아니니 dummy가 불완전한 상태.
                                Log.v("뷰모델현재측정추천0번쨰", "${mViewModel.selectedMeasure?.recommendations?.get(0)?.programSn.toString()}, ${mViewModel.selectedMeasure?.recommendations}")
                                program = fetchProgram("https://gym.tangostar.co.kr/tango_gym_admin/programs/read.php", mViewModel.selectedMeasure?.recommendations?.get(0)?.programSn.toString())
                                adapter = ExerciseRVAdapter(this@MainFragment, program?.exercises!!, null, Pair(0,0) ,"history")
                            }
                            CoroutineScope(Dispatchers.Main).launch {

                                // ------# 최근 진행 프로그램의 상세 보기로 넘어가기 #------
                                binding.tvMProgram.setOnClickListener {
                                    val dialog = ProgramCustomDialogFragment.newInstance(program.programSn, prefsManager.getLatestRecommendation())
                                    Log.v("프로그램direct", "$programSn, ${prefsManager.getLatestRecommendation()}")
                                    dialog.show(requireActivity().supportFragmentManager, "ProgramCustomDialogFragment")
                                }

                                // -------# 최근 진행 프로그램 뷰페이저 #------
                                binding.vpM.orientation = ViewPager2.ORIENTATION_HORIZONTAL
                                binding.vpM.apply {
                                    clipToPadding = false
                                    clipChildren = false
                                    offscreenPageLimit = 3
                                    setAdapter(adapter)
                                    currentItem = currentPage

                                    (getChildAt(0) as RecyclerView).apply {
                                        setPadding(dpToPx(5), 0, dpToPx(5), 0)
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
                            }
                        } catch (e: Exception) {
                            Log.e("Error>Program", "${e.message}")
                        }


                    }
                }
            } else {
                binding.tvMTitle.text = "${Singleton_t_user.getInstance(requireContext()).jsonObject?.getString("user_name")}님"
                binding.constraintLayout2.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.white))
                binding.tvMMeasureDate.visibility = View.GONE
                binding.rvM3.visibility = View.GONE
                binding.tvMCustom.visibility = View.GONE
                binding.tvMBlur.visibility = View.VISIBLE
                applyBlurToConstraintLayout(binding.clMMeasure, binding.ivMBlur)
                binding.clMMeasure.visibility = View.INVISIBLE
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

    fun applyBlurToConstraintLayout(constraintLayout: ConstraintLayout, imageView: ImageView) {
        constraintLayout.post {
            Blurry.with(constraintLayout.context)
                .radius(10)
                .sampling(5)
                .async()
                .capture(constraintLayout) // ConstraintLayout의 스크린샷을 캡처하여 블러 처리
                .into(imageView) // 블러 처리된 이미지를 ImageView에 설정
        }

    }
}