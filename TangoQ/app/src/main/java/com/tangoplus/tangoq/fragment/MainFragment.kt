package com.tangoplus.tangoq.fragment

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupWindow
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.tangoplus.tangoq.AlarmActivity
import com.tangoplus.tangoq.MainActivity
import com.tangoplus.tangoq.adapter.BannerVPAdapter
import com.tangoplus.tangoq.adapter.ProgramRVAdapter
import com.tangoplus.tangoq.listener.OnRVClickListener
import com.tangoplus.tangoq.`object`.Singleton_t_user
import com.tangoplus.tangoq.PlayFullScreenActivity
import com.tangoplus.tangoq.R
import com.tangoplus.tangoq.data.BannerViewModel
import com.tangoplus.tangoq.data.FavoriteViewModel
import com.tangoplus.tangoq.data.MeasureViewModel
import com.tangoplus.tangoq.data.ProgramVO
import com.tangoplus.tangoq.databinding.FragmentMainBinding
import com.tangoplus.tangoq.`object`.NetworkProgram.fetchProgramVOBySn
import kotlinx.coroutines.launch
import java.util.ArrayList


class MainFragment : Fragment(), OnRVClickListener {
    lateinit var binding: FragmentMainBinding
    val viewModel: FavoriteViewModel by activityViewModels()
    val bViewModel : BannerViewModel by activityViewModels()
//    val mViewModel : MeasureViewModel by activityViewModels()
    private var bannerPosition = Int.MAX_VALUE/2
    private var bannerHandler = HomeBannerHandler()
    private val intervalTime = 2400.toLong()
//    var popupWindow : PopupWindow?= null
    private lateinit var startForResult: ActivityResultLauncher<Intent>
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentMainBinding.inflate(inflater)
        binding.sflM.startShimmer()

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

        // -----! 스크롤 관리 !-----
        binding.nsvM.isNestedScrollingEnabled = false
        binding.rvM.isNestedScrollingEnabled = false
        binding.rvM.overScrollMode = 0

        // ------! 알람 intent !------
        binding.ibtnMAlarm.setOnClickListener {
            val intent = Intent(requireContext(), AlarmActivity::class.java)
            startActivity(intent)
        }

        // ------! 점수 시작 !------
//        val t_userData = Singleton_t_user.getInstance(requireContext()).jsonObject?.optJSONObject("data")
        binding.ivMScoreDown.visibility = View.GONE
        binding.ivMScoreUp.visibility = View.GONE
//        // TODO 점수 변동에 따른 화살표 VISIBLE 처리
//
//        if (binding.tvMBalanceScore.text == "미설정") {
//
//        } else if (binding.tvMBalanceScore.text.toString().toInt() > 76) {
//            binding.ivMScoreDown.visibility = View.GONE
//            binding.ivMScoreDone.visibility = View.GONE
//            binding.ivMScoreUp.visibility = View.VISIBLE
//        } else if (binding.tvMBalanceScore.text.toString().toInt() < 76) {
//            binding.ivMScoreDown.visibility = View.VISIBLE
//            binding.ivMScoreDone.visibility = View.GONE
//            binding.ivMScoreUp.visibility = View.GONE
//        } else {
//            binding.ivMScoreDown.visibility = View.GONE
//            binding.ivMScoreDone.visibility = View.VISIBLE
//            binding.ivMScoreUp.visibility = View.GONE
//        }
        binding.btnMMeasure.setOnClickListener {
            val bnv = (activity as MainActivity).findViewById<BottomNavigationView>(R.id.bnbMain)
            bnv.selectedItemId = R.id.measure
        }
//        // TODO 운동 기록에 맞게 HPV 연동 필요
        binding.tvMTodayTime.text = "16"
        binding.tvMTodaySteps.text = "1138"
        binding.tvMTodaySet.text = "3/5"
        binding.hpvMDailyFirst.progress = 28
        binding.hpvMDailySecond.progress = 19
        binding.hpvMDailyThird.progress = 60
        binding.tvMBalanceScore.text = "87점"
//
//        mViewModel.totalSteps.observe(viewLifecycleOwner) { totalSteps ->
//            if (totalSteps == "" || totalSteps.toInt() == 0) {
//                binding.tvMTodaySteps.text = "0"
//                binding.hpvMDailySecond.progress = 0
//            } else if (totalSteps.toInt() > 0) {
//                binding.tvMTodaySteps.text = totalSteps
//                binding.hpvMDailySecond.progress = (totalSteps.toInt() * 100) / 8000
//            }
//        } // ------! 점수 끝 !------

        // ------! 중앙 홍보 배너 시작 !------
        bViewModel.BannerList.add("drawable_banner_1")
        bViewModel.BannerList.add("drawable_banner_2")

        val bannerAdpater = BannerVPAdapter(bViewModel.BannerList, "main", requireContext())
        bannerAdpater.notifyDataSetChanged()
        binding.vpMBanner.orientation = ViewPager2.ORIENTATION_HORIZONTAL
        binding.vpMBanner.adapter = bannerAdpater
        binding.vpMBanner.setCurrentItem(bannerPosition, false)
        binding.vpMBanner.apply {
            registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                override fun onPageScrollStateChanged(state: Int) {
                    super.onPageScrollStateChanged(state)
                    when (state) {
                        ViewPager2.SCROLL_STATE_DRAGGING -> autoScrollStop()
                        ViewPager2.SCROLL_STATE_IDLE -> autoScrollStart(intervalTime)
                        ViewPager2.SCROLL_STATE_SETTLING -> {}
                    }
                }
            })
        } // ------! 중앙 홍보 배너 끝 !------

        // ------! db에서 받아서 뿌려주기 시작 !------
        lifecycleScope.launch {
//            // TODO 프로그램을 불러오거나 전체에서 필터링해서 만들어야 함
//            val responseArrayList = fetchExerciseJson(getString(R.string.IP_ADDRESS_t_exercise_description))
////            Log.v("MainF>RV", "$responseArrayList")
//            try {
//                val verticalDataList = responseArrayList.toMutableList()
//                val programFliterList = arrayOf("목", "어깨", "팔꿉", "손목", "몸통", "복부", "엉덩", "무릎", "발목", "전신", "유산소", "코어", "몸통")
//
//                val groupedExercises = mutableMapOf<String, MutableList<ExerciseVO>>()
//
//                // -----! horizontal 추천 rv adapter 시작 !------
//                verticalDataList.forEach { exerciseVO ->
//                    programFliterList.forEach { filter ->
//                        if (exerciseVO.exerciseName?.contains(filter) == true) {
//                            if (!groupedExercises.containsKey(filter)) {
//                                groupedExercises[filter] = mutableListOf()
//                            }
//                            groupedExercises[filter]?.add(exerciseVO)
//                        }
//                    }
//                }
//                // TODO 자체 프로그램 수정해야할 곳
//                groupedExercises.forEach { (filter, exercises) ->
//
//                    val programVO = ProgramVO(
//                        programName = when (filter) {
//                            "유산소" -> "유산소 프로그램"
//                            "코어" -> "코어 프로그램"
//                            else -> filter + "관절 프로그램"
//                        },
//                        imgThumbnails = mutableListOf(),
//                        programCount = "",
//                        programStage = exercises.first().exerciseStage,
//                        exercises = exercises
//                    )
//
//                    programVO.programTime = programVO.exercises?.sumOf {
//                        ((it.videoDuration?.toInt())!! / 60)
//                    }!!
//
//                    programVO.programCount = programVO.exercises?.size.toString()
//                    if (viewModel.programList.value?.any { it.programName == programVO.programName } == false ) viewModel.programList.value?.add(programVO)
//
//                }
//                if (viewModel.programList.value?.size!! > 1) {
//                    binding.sflM.stopShimmer()
//                    binding.sflM.visibility = View.GONE
//                }
//                val recommendAadpter = ProgramRVAdapter(viewModel.programList.value!!, this@MainFragment, this@MainFragment, "horizon")
//                binding.rvMRecommend.adapter = recommendAadpter
//                val linearLayoutManager2 = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
//                binding.rvMRecommend.layoutManager = linearLayoutManager2
//                // ------! horizontal 추천 rv adapter 끝 !------
//
//                // ------! horizontal과 progressbar 연동 시작 !------
//                binding.rvMRecommend.addOnScrollListener(object: RecyclerView.OnScrollListener() {
//                    override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
//                        super.onScrolled(recyclerView, dx, dy)
//                        val layoutManager = recyclerView.layoutManager as LinearLayoutManager
//                        val totalItemCount = layoutManager.itemCount
//                        val lastVisibleItemPosition = layoutManager.findLastVisibleItemPosition()
//                        Log.v("게이지바", "현재 ${lastVisibleItemPosition.toFloat()}, 전체: ${totalItemCount.toFloat()}")
//                        val progress = ((lastVisibleItemPosition.toFloat() + 1) * 100 ) / totalItemCount.toFloat()
//                        binding.hpvMRecommend.progress = progress.toInt()
//                    }
//                }) // ------! horizontal과 progressbar 연동 끝 !------

            val programVOList = mutableListOf<ProgramVO>()
            for (i in 10 downTo 2) {
                programVOList.add(fetchProgramVOBySn(getString(R.string.IP_ADDRESS_t_exercise_programs), i.toString()))
            }
            binding.sflM.stopShimmer()
            binding.sflM.visibility = View.GONE
            binding.hpvMRecommend.visibility = View.INVISIBLE
            binding.spnMFilter.visibility = View.INVISIBLE
            setRVAdapter(programVOList)



                // ------! 하단 RV Adapter 시작 !------
//                setRVAdapter(verticalDataList)
                // -----! vertical 어댑터 끝 !-----

                // -----! spinner 연결 시작 !-----
//                val filterList = arrayListOf<String>()
//                filterList.add("최신순")
//                filterList.add("인기순")
//                filterList.add("추천순")
//                binding.spnMFilter.adapter = SpinnerAdapter(requireContext(), R.layout.item_spinner, filterList)
//                binding.spnMFilter.onItemSelectedListener = object : AdapterView.OnItemSelectedListener{
//                    override fun onItemSelected(
//                        parent: AdapterView<*>?,
//                        view: View?,
//                        position: Int,
//                        id: Long
//                    ) {
//                        when (position) {
//                            0 -> {
////                                setRVAdapter(verticalDataList)
//                            }
//                            1 -> {
//                                val sortDataList = verticalDataList.sortedBy { it.videoDuration }.toMutableList()
////                                setRVAdapter(sortDataList)
//                                Log.v("정렬된 리스트", "$sortDataList")
//                            }
//                            2 -> {
//                                val sortDataList = verticalDataList.sortedBy { it.videoDuration }.reversed().toMutableList()
////                                setRVAdapter(sortDataList)
//                                Log.v("정렬된 리스트", "$sortDataList")
//                            }
//                        }
//                    }
//                    override fun onNothingSelected(parent: AdapterView<*>?) {}
//                } // ------! spinner 연결 끝 !------
//
//            } catch (e: Exception) {
//                Log.e(ContentValues.TAG, "Error storing exercises", e)
//            } // -----! 하단 RV Adapter 끝 !-----
        }
    }
    override fun onRVClick(program: ProgramVO) {
        val intent = Intent(requireContext(), PlayFullScreenActivity::class.java)
        val url = storeUrl(program)
        intent.putStringArrayListExtra("resourceList", ArrayList(url))
        startActivityForResult(intent, 8080)
    }
    private fun storeUrl(program: ProgramVO) : MutableList<String> {
        val exercises = program.exercises
        val resourceList = mutableListOf<String>()
        for (i in 0 until exercises!!.size) {
            resourceList.add(exercises[i].videoFilepath.toString())
        }
        return resourceList
    }
    private fun autoScrollStart(intervalTime: Long) {
        bannerHandler.removeMessages(0)
        bannerHandler.sendEmptyMessageDelayed(0, intervalTime)

    }
    private fun autoScrollStop() {
        bannerHandler.removeMessages(0)
    } // -----! 배너 끝 !------

    @SuppressLint("HandlerLeak")
    private inner class HomeBannerHandler: Handler(Looper.getMainLooper()) {
        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)
            if (msg.what == 0 && bViewModel.BannerList.isNotEmpty()) {
                binding.vpMBanner.setCurrentItem(++bannerPosition, true)
                // ViewPager의 현재 위치를 이미지 리스트의 크기로 나누어 현재 이미지의 인덱스를 계산
//                val currentIndex = bannerPosition % bViewModel.BannerList.size // 65536  % 5

//                // ProgressBar의 값을 계산
//                binding.hpvIntro.progress = (currentIndex ) * 100 / (viewModel.BannerList.size -1 )
                autoScrollStart(intervalTime)
            }
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun setRVAdapter (programList: MutableList<ProgramVO>) {
        val adapter = ProgramRVAdapter(programList, this@MainFragment, this@MainFragment,"rank", startForResult)
        adapter.programs = programList
        binding.rvM.adapter = adapter
        val linearLayoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
        binding.rvM.layoutManager = linearLayoutManager
        adapter.notifyDataSetChanged()
    }
    override fun onResume() {
        super.onResume()
        autoScrollStart(intervalTime)
    }

    override fun onPause() {
        super.onPause()
        autoScrollStop()
    }

}