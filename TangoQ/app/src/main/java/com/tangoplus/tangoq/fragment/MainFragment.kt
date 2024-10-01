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
import com.tangoplus.tangoq.db.PreferencesManager
import com.tangoplus.tangoq.data.ExerciseViewModel
import com.tangoplus.tangoq.data.ProgressViewModel
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
import com.tangoplus.tangoq.`object`.NetworkExercise.fetchExerciseById
import com.tangoplus.tangoq.`object`.Singleton_t_measure
import com.tangoplus.tangoq.`object`.Singleton_t_progress
import jp.wasabeef.blurry.Blurry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainFragment : Fragment() {
    lateinit var binding: FragmentMainBinding
    val viewModel : UserViewModel by activityViewModels()
    val mViewModel : MeasureViewModel by activityViewModels()
    private lateinit var startForResult: ActivityResultLauncher<Intent>
    lateinit var prefsManager : PreferencesManager
    private var measures : MutableList<MeasureVO>? = null
    private var singletonMeasure : MutableList<MeasureVO>? = null
//    private  var sp : MutableList<MutableList<ProgressUnitVO>>? = null
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
        singletonMeasure = Singleton_t_measure.getInstance(requireContext()).measures
//        sp = Singleton_t_progress.getInstance(requireContext()).progresses?.get(0) // 0번 인덱스의 사용기록을 가져옴 -> 0번 인덱스가 가장 최근 추천 프로그램이 맞는지?
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

                // 측정결과를 가져올 때, 매칭이 이미 만들어진건지, 안만들어진건지에 대한 판단을 할 수 있는 기준이 있어야 함. 그게 여기 들어가야 함. 측정 데이터를 가져왔을 때.


                // ------# 키오스크에서 데이터 가져왔을 때 #------


                binding.tvMMeasureDate.setOnClickListener {
                    val dialog = MeasureBSDialogFragment()
                    dialog.show(requireActivity().supportFragmentManager, "MeasureBSDialogFragment")
                }

                measures = Singleton_t_measure.getInstance(requireContext()).measures



                updateUI()
                // 완료 갯수 넣기 현재 고정 0개임 TODO 여기에 오늘 날짜 운동 기록을 가져와야함. (번호 대조 후 여기에 넣기)
                binding.tvMProgram.setOnClickListener {  // 이력의 가장 마지막 프로그램을 가져와야 함
//                    val dateIndex = singletonMeasure?.indexOf(singletonMeasure?.find { it.regDate == mViewModel.selectedMeasureDate.value }!!) // 측정값의 인덱스임.
//                    // 마지막으로 recommendation
//                    val recommendations = singletonMeasure?.find { it.regDate == mViewModel.selectedMeasureDate.value }!!.recommendations // 측정의 제안프로그램들 가져옴.
//                    val lastRecommendation = sp.find { it.indexOf() }
//                    recommendations.find {   } // 여기서

                    val dialog = ProgramCustomDialogFragment.newInstance(10, 0, 0)
                    dialog.show(requireActivity().supportFragmentManager, "PorgramCustomDialogFragment")
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

                    Log.v("메인Date", "dateIndex: ${dateIndex}, selectedDate: $selectedDate, singletonMeasure ${measures?.get(dateIndex)?.fileUris}")
                    binding.tvMMeasureResult1.text = "${measures?.get(dateIndex)?.dangerParts?.get(0)?.first}부위가 부상 위험이 있습니다."
                    binding.tvMMeasureResult2.text = "${measures?.get(dateIndex)?.dangerParts?.get(1)?.first}부위가 부상 위험이 있습니다."

                    // ------# 측정 결과에 맞는 진행 프로그램 2번째 가져오기 #------

                    CoroutineScope(Dispatchers.IO).launch {
                        val dummyExercise = listOf(
                            fetchExerciseById(getString(R.string.API_exercise), 1.toString()),
                            fetchExerciseById(getString(R.string.API_exercise), 2.toString()),
                            fetchExerciseById(getString(R.string.API_exercise), 3.toString()),
                            fetchExerciseById(getString(R.string.API_exercise), 4.toString()),
                        )
                        val adapter = ExerciseRVAdapter(this@MainFragment, dummyExercise.toMutableList(), null, Pair(0,0) ,"history")
                        CoroutineScope(Dispatchers.Main).launch {
                            binding.vpM.orientation = ViewPager2.ORIENTATION_HORIZONTAL
                            binding.vpM.apply {
                                clipToPadding = false
                                clipChildren = false
                                offscreenPageLimit = 3
                                setAdapter(adapter)
                                currentItem = 1

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
//    private fun setEpisodeProgress(finishCount: Int) {
//        binding.tvMCustomProgress.text = "완료 ${finishCount}/${hViewModel.currentProgram?.exercises?.get(0)?.size}개"
//        binding.hpvMCustomProgress.progress = (finishCount * 100).div(hViewModel.currentProgram?.exercises?.get(0)?.size!!)
//    }

    private fun setProgramButton(isEnabled: Boolean) {
        if (isEnabled) {
            binding.btnMProgram.isEnabled = true
            binding.btnMProgram.backgroundTintList = ContextCompat.getColorStateList(requireContext(), R.color.mainColor)
            binding.btnMProgram.text = "프로그램 선택하기"
//            setEpisodeProgress(0)
        } else {
            binding.btnMProgram.isEnabled = false
            binding.btnMProgram.text = "프로그램 완료, 재측정을 진행해 주세요"
            binding.btnMProgram.backgroundTintList = ContextCompat.getColorStateList(requireContext(), R.color.subColor150)
//            setEpisodeProgress(hViewModel.currentProgram?.exercises?.get(0)?.size!!)

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