package com.tangoplus.tangoq.Fragment

import android.content.ContentValues
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import androidx.activity.viewModels
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.tangoplus.tangoq.Adapter.BannerVPAdapter
import com.tangoplus.tangoq.Adapter.ExerciseRVAdapter
import com.tangoplus.tangoq.Adapter.RecommendRVAdapter
import com.tangoplus.tangoq.Adapter.SpinnerAdapter
import com.tangoplus.tangoq.Listener.OnRVClickListener
import com.tangoplus.tangoq.Object.NetworkExerciseService.fetchExerciseJson
import com.tangoplus.tangoq.R
import com.tangoplus.tangoq.ViewModel.BannerViewModel
import com.tangoplus.tangoq.ViewModel.ExerciseVO
import com.tangoplus.tangoq.ViewModel.ExerciseViewModel
import com.tangoplus.tangoq.ViewModel.ProgramVO
import com.tangoplus.tangoq.databinding.FragmentMainBinding
import kotlinx.coroutines.launch


class MainFragment : Fragment(), OnRVClickListener {
    lateinit var binding: FragmentMainBinding
    val viewModel: ExerciseViewModel by activityViewModels()
    val bViewModel : BannerViewModel by activityViewModels()
    private var bannerPosition = Int.MAX_VALUE/2
    private var bannerHandler = HomeBannerHandler()
    private val intervalTime = 2400.toLong()
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentMainBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // -----! 스크롤 관리 !-----
        binding.nsvM.isNestedScrollingEnabled = false
        binding.rvM.isNestedScrollingEnabled = false
        binding.rvM.overScrollMode = 0
        // -----! spinner 연결 시작 !-----
        val filterList = arrayListOf<String>()
        filterList.add("최신순")
        filterList.add("인기순")
        filterList.add("추천순")
        binding.spnMFilter.adapter = SpinnerAdapter(requireContext(), R.layout.item_spinner, filterList)
        binding.spnMFilter.onItemSelectedListener = object : AdapterView.OnItemSelectedListener{
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                binding.spnMFilter.getItemAtPosition(position).toString()
                // TODO 필터로 recyclerview 순서 변경
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        } // ------! spinner 연결 끝 !------

        // ------! 중앙 홍보 배너 시작 !------
        bViewModel.BannerList.add("dummy_banner1")
        bViewModel.BannerList.add("dummy_banner2")
        bViewModel.BannerList.add("dummy_banner3")
        bViewModel.BannerList.add("dummy_banner4")

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
                    }
                }
            })
        }
        // ------! 중앙 홍보 배너 끝 !------

        // ------! db에서 받아서 뿌려주기 시작 !------
        lifecycleScope.launch {
            val responseArrayList = fetchExerciseJson(getString(R.string.IP_ADDRESS_t_Exercise_Description))
//            Log.v("MainF>RV", "$responseArrayList")
            try {
                val verticalDataList = responseArrayList.toMutableList()
                val programFliterList = arrayOf("목", "어깨", "팔꿉", "손목", "몸통", "복부", "엉덩", "무릎", "발목", "전신", "유산소", "코어", "몸통")

                val groupedExercises = mutableMapOf<String, MutableList<ExerciseVO>>()

                // -----! horizontal 추천 rv adapter 시작 !------
                verticalDataList.forEach { exerciseVO ->
                    programFliterList.forEach { filter ->
                        if (exerciseVO.exerciseName?.contains(filter) == true) {
                            if (!groupedExercises.containsKey(filter)) {
                                groupedExercises[filter] = mutableListOf()
                            }
                            groupedExercises[filter]?.add(exerciseVO)
                        }
                    }
                }
                // TODO 자체 프로그램 수정해야할 곳
                groupedExercises.forEach { (filter, exercises) ->

                    val programVO = ProgramVO(
                        programName = when (filter) {
                            "유산소" -> "유산소 프로그램"
                            "코어" -> "코어 프로그램"
                            else -> filter + "관절 프로그램"
                        },
                        programImageUrl = "",
                        programCount = "",
                        programStep = exercises.first().exerciseIntensity,
                        exercises = exercises
                    )
                    programVO.programTime = programVO.exercises?.sumOf {
                        ((it.videoTime?.toInt())!! / 60)
                    }.toString()
                    programVO.programCount = programVO.exercises?.size.toString()
                    if (viewModel.programList.value?.any { it.programName == programVO.programName } == false ) viewModel.programList.value?.add(programVO)
//                    Log.v("VM>프로그램", "${viewModel.programList.value}")
                }

                val recommendAadpter = RecommendRVAdapter(viewModel.programList, this@MainFragment, this@MainFragment)
                binding.rvMRecommend.adapter = recommendAadpter
                val linearLayoutManager2 = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
                binding.rvMRecommend.layoutManager = linearLayoutManager2
                // ------! horizontal 추천 rv adapter 끝 !------

                // ------! horizontal과 progressbar 연동 시작 !------
                binding.rvMRecommend.addOnScrollListener(object: RecyclerView.OnScrollListener() {
                    override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                        super.onScrolled(recyclerView, dx, dy)
                        val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                        val totalItemCount = layoutManager.itemCount
                        val lastVisibleItemPosition = layoutManager.findLastVisibleItemPosition()
                        Log.v("게이지바", "현재 ${lastVisibleItemPosition.toFloat()}, 전체: ${totalItemCount.toFloat()}")
                        val progress = ((lastVisibleItemPosition.toFloat() + 1) * 100 ) / totalItemCount.toFloat()
                        binding.hpvMRecommend.progress = progress.toInt()
                    }
                }) // ------! horizontal과 progressbar 연동 끝 !------

                // ------! 하단 RV Adapter 시작 !------
                val adapter = ExerciseRVAdapter(this@MainFragment, verticalDataList,"main" )
                adapter.exerciseList = verticalDataList
                binding.rvM.adapter = adapter
                val linearLayoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
                binding.rvM.layoutManager = linearLayoutManager

                // -----! vertical 어댑터 끝 !-----
            } catch (e: Exception) {
                Log.e(ContentValues.TAG, "Error storing exercises", e)
            } // -----! 하단 RV Adapter 끝 !-----
        }
    }

    override fun onRVClick(item: String) {

    }
    private fun autoScrollStart(intervalTime: Long) {
        bannerHandler.removeMessages(0)
        bannerHandler.sendEmptyMessageDelayed(0, intervalTime)

    }
    private fun autoScrollStop() {
        bannerHandler.removeMessages(0)
    } // -----! 배너 끝 !-----

    private inner class HomeBannerHandler: Handler(Looper.getMainLooper()) {
        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)
            if (msg.what == 0 && bViewModel.BannerList.isNotEmpty()) {
                binding.vpMBanner.setCurrentItem(++bannerPosition, true)
                // ViewPager의 현재 위치를 이미지 리스트의 크기로 나누어 현재 이미지의 인덱스를 계산합니다.
                val currentIndex = bannerPosition % bViewModel.BannerList.size // 65536  % 5

//                // ProgressBar의 값을 계산합니다.
//                binding.hpvIntro.progress = (currentIndex ) * 100 / (viewModel.BannerList.size -1 )
                autoScrollStart(intervalTime)
            }
        }
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