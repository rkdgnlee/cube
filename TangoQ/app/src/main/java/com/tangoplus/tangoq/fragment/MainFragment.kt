package com.tangoplus.tangoq.fragment

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Rect
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Gravity
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.google.android.gms.common.util.DeviceProperties.isTablet
import com.tangoplus.tangoq.MainActivity
import com.tangoplus.tangoq.R
import com.tangoplus.tangoq.db.Singleton_t_user
import com.tangoplus.tangoq.adapter.BalanceRVAdapter
import com.tangoplus.tangoq.adapter.ExerciseRVAdapter
import com.tangoplus.tangoq.adapter.PartRVAdapter
import com.tangoplus.tangoq.function.PreferencesManager
import com.tangoplus.tangoq.vo.MeasureVO
import com.tangoplus.tangoq.viewmodel.MeasureViewModel
import com.tangoplus.tangoq.vo.ProgressUnitVO
import com.tangoplus.tangoq.viewmodel.UserViewModel
import com.tangoplus.tangoq.databinding.FragmentMainBinding
import com.tangoplus.tangoq.dialog.AlarmDialogFragment
import com.tangoplus.tangoq.dialog.GuideDialogFragment
import com.tangoplus.tangoq.dialog.ProgramCustomDialogFragment
import com.tangoplus.tangoq.dialog.QRCodeDialogFragment
import com.tangoplus.tangoq.dialog.bottomsheet.MeasureBSDialogFragment
import com.tangoplus.tangoq.fragment.ExtendedFunctions.isFirstRun
import com.tangoplus.tangoq.function.TooltipManager
import com.tangoplus.tangoq.api.DeviceService.isNetworkAvailable
import com.tangoplus.tangoq.api.NetworkProgram.fetchProgram
import com.tangoplus.tangoq.api.NetworkProgress.convertToProgressUnitVO
import com.tangoplus.tangoq.api.NetworkProgress.getLatestProgress
import com.tangoplus.tangoq.api.NetworkProgress.getWeekProgress
import com.tangoplus.tangoq.db.Singleton_t_measure
import com.tangoplus.tangoq.function.MeasurementManager.createMeasureComment
import com.tangoplus.tangoq.viewmodel.ProgressViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainFragment : Fragment() {
    lateinit var binding: FragmentMainBinding
    private val uvm by activityViewModels<UserViewModel>()
    private val mvm : MeasureViewModel by activityViewModels()
    private val pvm : ProgressViewModel by activityViewModels()
    private lateinit var startForResult: ActivityResultLauncher<Intent>
    private lateinit var prefsManager : PreferencesManager
    private var measures : MutableList<MeasureVO>? = null
    private var singletonMeasure : MutableList<MeasureVO>? = null
    private var latestRecSn = -1


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentMainBinding.inflate(inflater)
        startForResult = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result -> }
        return binding.root
    }

    @SuppressLint("SetTextI18n", "NotifyDataSetChanged")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // ------# 스크롤 관리 #------
        binding.nsvM.isNestedScrollingEnabled = false
        prefsManager = PreferencesManager(requireContext())
        val sn = Singleton_t_user.getInstance(requireContext()).jsonObject?.optInt("sn")
        if (sn != null) {
            prefsManager.setUserSn(sn)
        }

        latestRecSn = prefsManager.getLatestRecommendation()
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
                    if (mvm.selectedMeasureDate.value == null) {
                        mvm.selectedMeasureDate.value = measures?.get(0)?.regDate
                    }
                    if (mvm.selectMeasureDate.value == null) {
                        mvm.selectMeasureDate.value = measures?.get(0)?.regDate
                    }

                    // ------# 측정결과 있을 때 도움말 툴팁 #------
                    if (isFirstRun("Tooltip_isFirstRun_existed")) {
                        existedMeasurementGuide()
                    }
                } else {
                    if (isFirstRun("Tooltip_isFirstRun_not_existed")) {
                        notExistedMeasurementGuide()
                    }
                }
                Log.v("선택된measureDate", "${mvm.selectedMeasureDate.value}")
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
        val filteredParts = measures?.get(index)?.dangerParts?.filter { it.second == 1f || it.second == 2f}
        val partAdapter = PartRVAdapter(this@MainFragment, filteredParts?.toMutableList())
        binding.rvM1.adapter = partAdapter
        binding.rvM1.isNestedScrollingEnabled = false
//        // ------# item 반짝이기 #------
//        Handler(Looper.getMainLooper()).postDelayed({
//            val animationController = PartAnimationController(
//                recyclerView = binding.rvM1,
//                itemCount = partAdapter.itemCount
//            )
//            animationController.startSequentialAnimation()
//        }, 500)
        // ------# balance check #------

        val layoutManager2 = LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
        binding.rvM3.layoutManager = layoutManager2
        val balanceAdapter = BalanceRVAdapter(this@MainFragment)
        binding.rvM3.adapter = balanceAdapter
    }

    // 블러 유무 판단하기
    @SuppressLint("SetTextI18n")
    private fun updateUI() {
        Log.v("measure있는지", "${measures?.size}")

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
                Log.v("measure있는지", "$measure")
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
                    // ------# 바텀시트에서 변한 selectedMeasureDate 에 맞게 변함 #------


                    mvm.selectedMeasureDate.observe(viewLifecycleOwner) { selectedDate ->
                        Log.v("VM선택날짜", "mvm.selectedMeasureDate: $selectedDate")
                        val dateIndex = measures?.indexOf(measures?.find { it.regDate == selectedDate })
                        Log.v("메인Date", "dateIndex: ")
                        // ------# 매칭 프로그램이 있는지 없는지 확인하기 #------
//                        if (selectedDate != measures?.get(0)?.regDate) {
//                            setProgramButton(false)
//                        } else {
//                            setProgramButton(true)
//                        }
                        if (dateIndex != null) {
                            measures?.get(dateIndex)?.recommendations
                            binding.tvMMeasureDate.text = measure[dateIndex].regDate.substring(0, 10)
                            binding.tvMOverall.text = measure[dateIndex].overall.toString()
                            setAdapter(dateIndex)

                            Log.v("메인Date", "dateIndex: ${dateIndex}, selectedDate: $selectedDate, singletonMeasure: ${measures?.get(dateIndex)?.dangerParts}, ${measures?.get(dateIndex)?.recommendations}")
                            Log.v("써머리들어가기 전", "${measures?.get(dateIndex)?.dangerParts}")
                            val measureSize = measures?.get(dateIndex)?.dangerParts?.size
                            if (measureSize != null) {
                                if (measureSize > 1) {
                                    val summaryComments  = createMeasureComment(measures?.get(dateIndex)?.dangerParts)

                                    Log.v("써머리들어간 후", "$summaryComments")
                                    if (summaryComments.size > 1) {
                                        binding.tvMMeasureResult1.text = summaryComments[0]
                                        binding.tvMMeasureResult2.text = summaryComments[1]
                                    } else if (summaryComments.size == 1) {
                                        binding.tvMMeasureResult1.text = summaryComments[0]
                                        binding.tvMMeasureResult2.visibility = View.INVISIBLE
                                    } else {
                                        binding.tvMMeasureResult1.text = ""
                                    }
                               }
                            }
                        }

                        // 내가 했던 프로그램의 운동 목록 가져오기
                        lifecycleScope.launch(Dispatchers.IO) {
                            try {
                                val progressResult = getLatestProgress(getString(R.string.API_progress), requireContext())
                                val uvpResults = progressResult?.first?.sortedBy { it.uvpSn }?.toMutableList()
                                val programExercises = progressResult?.second
                                Log.v("최근프로그램데이터", "$uvpResults")
                                val adapter =  ExerciseRVAdapter(this@MainFragment, programExercises, uvpResults, null,null, null, "M")
                                var currentPage = 0
                                if (uvpResults != null) {
                                   currentPage = findCurrentIndex(uvpResults)
                                }

                                // -------# 최근 진행 프로그램 뷰페이저 #------
                                withContext(Dispatchers.Main) {
                                    binding.vpM.orientation = ViewPager2.ORIENTATION_HORIZONTAL
                                    binding.vpM.apply {
                                        clipToPadding = false
                                        clipChildren = false
                                        offscreenPageLimit = 3

                                        // ------# viewpager의 setAdapter fun #------
                                        setAdapter(adapter)
                                        currentItem = currentPage
                                        var dp = 5
                                        try {
                                            dp = when (isTablet(requireContext())) {
                                                true -> 28
                                                false -> 5
                                            }
                                        } catch (e: IndexOutOfBoundsException) {
                                            Log.e("IntroIndex", "${e.message}")
                                        } catch (e: IllegalArgumentException) {
                                            Log.e("IntroIllegal", "${e.message}")
                                        } catch (e: IllegalStateException) {
                                            Log.e("IntroIllegal", "${e.message}")
                                        }catch (e: NullPointerException) {
                                            Log.e("IntroNull", "${e.message}")
                                        } catch (e: java.lang.Exception) {
                                            Log.e("IntroException", "${e.message}")
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
                                    // ------# 측정 날짜 바뀌면서 padding많이 먹으면서
                                    binding.vpM.addItemDecoration(itemDecoration)
                                    binding.llM.visibility = View.GONE
                                    binding.sflM.stopShimmer()


                                    // ------# 최근 진행 프로그램의 상세 보기로 넘어가기 #------
//                                    context.let { context ->
//                                        val colorAnimation = ValueAnimator.ofObject(ArgbEvaluator(),
//                                            ContextCompat.getColor(requireContext(), R.color.mainColor),
//                                            ContextCompat.getColor(requireContext(), R.color.mainRippleColor)).apply {
//                                            duration = 1000
//                                            repeatCount = ValueAnimator.INFINITE
//                                            repeatMode = ValueAnimator.REVERSE
//
//                                            addUpdateListener { animator ->
//                                                val color = animator.animatedValue as Int
//                                                binding.btnMProgram.backgroundTintList = ColorStateList.valueOf(color)
//                                            }
//                                        }
//                                        colorAnimation.start()
//                                    }

                                    binding.tvMProgram.setOnClickListener {
                                        try {
                                            val programSn = uvm.existedProgramData?.programSn ?: throw IllegalStateException("Program SN is null")
                                            val recommendationSn = when {
                                                prefsManager.getLatestRecommendation() != -1 -> {
                                                    prefsManager.getLatestRecommendation()
                                                }
                                                mvm.selectedMeasure?.recommendations?.isNotEmpty() == true -> {
                                                    mvm.selectedMeasure?.recommendations?.get(0)?.recommendationSn
                                                        ?: throw IllegalStateException("Recommendation SN is null")
                                                }
                                                else -> {
                                                    throw IllegalStateException("No valid recommendation available")
                                                }
                                            }
                                            ProgramCustomDialogFragment.newInstance(programSn, recommendationSn)
                                                .show(requireActivity().supportFragmentManager, "ProgramCustomDialogFragment")
                                        } catch (e: IndexOutOfBoundsException) {
                                            Log.e("MainIndex", "${e.message}")
                                        } catch (e: IllegalArgumentException) {
                                            Log.e("MainIllegalA", "${e.message}")
                                        } catch (e: IllegalStateException) {
                                            Log.e("MainIllegalS", "${e.message}")
                                        }catch (e: NullPointerException) {
                                            Log.e("MainNull", "${e.message}")
                                        } catch (e: java.lang.Exception) {
                                            Log.e("MainException", "${e.message}")
                                        }
                                    }
                                }
                            }  catch (e: IndexOutOfBoundsException) {
                                Handler(Looper.getMainLooper()).postDelayed({
                                    binding.sflM.stopShimmer() }, 3000)
                                Log.e("MainIndex", "${e.message}")
                            } catch (e: IllegalArgumentException) {
                                Handler(Looper.getMainLooper()).postDelayed({
                                    binding.sflM.stopShimmer() }, 3000)
                                Log.e("MainIllegalA", "${e.message}")
                            } catch (e: IllegalStateException) {
                                Handler(Looper.getMainLooper()).postDelayed({
                                    binding.sflM.stopShimmer() }, 3000)
                                Log.e("MainIllegalS", "${e.message}")
                            }catch (e: NullPointerException) {
                                Handler(Looper.getMainLooper()).postDelayed({
                                    binding.sflM.stopShimmer() }, 3000)
                                Log.e("MainNull", "${e.message}")
                            } catch (e: java.lang.Exception) {
                                Handler(Looper.getMainLooper()).postDelayed({
                                    binding.sflM.stopShimmer() }, 3000)
                                Log.e("MainException", "${e.message}")
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
            Log.v("progressIndex", "$progressIndex")
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
        Log.v("progressIndex", "$progressIndex")
        return 0
    }

    private fun setProgramButton(isEnabled: Boolean) {
        if (isEnabled) {
            binding.btnMProgram.backgroundTintList = ContextCompat.getColorStateList(requireContext(), R.color.mainColor)
            binding.btnMProgram.text = "프로그램 선택하기"
        }
    }

    private fun existedMeasurementGuide() {
        binding.constraintLayout2.isEnabled = false
        TooltipManager.createGuide(
            context = requireContext(),
            text = "가장 최근 측정 결과의 종합 점수입니다\n7가지 자세와 설문을 통해 종합적으로 산출됩니다",
            anchor = binding.tvMOverall,
            gravity = Gravity.BOTTOM,
            dismiss = {

                TooltipManager.createGuide(
                    context = requireContext(),
                    text = "탭하여 지난 측정 결과 선택하세요\n측정 결과와 지난 프로그램을 볼 수 있습니다",
                    anchor = binding.tvMMeasureDate,
                    gravity = Gravity.BOTTOM,
                    dismiss = {
                        TooltipManager.createGuide(
                            context = requireContext(),
                            text = " 탭해서 현재 위험 부위와 관련된\n운동 프로그램을 시작할 수 있습니다",
                            anchor = binding.btnMProgram,
                            gravity = Gravity.BOTTOM,
                            dismiss = {
                                binding.constraintLayout2.isEnabled = true
                            })
                    }
                )
            }
        )
    }

    private fun notExistedMeasurementGuide() {
        TooltipManager.createGuide(
            context = requireContext(),
            text = "가장 최근 측정 결과의 종합 점수입니다\n7가지 자세와 설문을 통해 종합적으로 산출됩니다",
            anchor = binding.tvMOverall,
            gravity = Gravity.BOTTOM,
            dismiss = {

                TooltipManager.createGuide(
                    context = requireContext(),
                    text = "7가지 자세 측정을 완료하고 운동 프로그램을 추천받으세요",
                    anchor = binding.btnMProgram,
                    gravity = Gravity.BOTTOM,
                    dismiss = {
                    }
                )
            }
        )
    }
}