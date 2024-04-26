package com.example.mhg

import android.R
import android.annotation.SuppressLint
import android.app.UiModeManager
import android.content.ContentValues
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.viewpager2.widget.ViewPager2
import com.example.mhg.Adapter.HomeBannerRecyclerViewAdapter
import com.example.mhg.Adapter.HomeHorizontalRecyclerViewAdapter
import com.example.mhg.Adapter.HomeVerticalRecyclerViewAdapter

import com.example.mhg.VO.ExerciseVO
import com.example.mhg.VO.ExerciseViewModel
import com.example.mhg.VO.HomeBannerItem
import com.example.mhg.VO.UserViewModel
import com.example.mhg.databinding.FragmentHomeBeginnerBinding
import com.example.mhg.`object`.NetworkExerciseService.fetchExerciseJson
import com.example.mhg.`object`.NetworkUserService
import com.example.mhg.`object`.Singleton_t_user
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.tabs.TabLayout
import kotlinx.coroutines.launch


class HomeBeginnerFragment : Fragment() {
    lateinit var binding: FragmentHomeBeginnerBinding
    private var bannerPosition = Int.MAX_VALUE/2
    private var homeBannerHandler = HomeBannerHandler()
    private val intervalTime = 2200.toLong()
    lateinit var ExerciseList : MutableList<ExerciseVO>
    private val exerciseTypeList = listOf("목관절", "어깨", "팔꿉", "손목", "몸통전면(복부)", "몸통 후면(척추)", "몸통 코어", "엉덩", "무릎", "발목", "유산소")
    val viewModel : UserViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentHomeBeginnerBinding.inflate(layoutInflater)
        val bannerList = arrayListOf<HomeBannerItem>()

        val ImageUrl1 = "https://images.unsplash.com/photo-1572196459043-5c39f99a7555?q=80&w=2670&auto=format&fit=crop&ixlib=rb-4.0.3&ixid=M3wxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8fA%3D%3D"
        val ImageUrl2 = "https://images.unsplash.com/photo-1605558162119-2de4d9ff8130?q=80&w=2670&auto=format&fit=crop&ixlib=rb-4.0.3&ixid=M3wxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8fA%3D%3D"
        val ImageUrl3 = "https://images.unsplash.com/photo-1533422902779-aff35862e462?q=80&w=2670&auto=format&fit=crop&ixlib=rb-4.0.3&ixid=M3wxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8fA%3D%3D"
        val ImageUrl4 = "https://images.unsplash.com/photo-1587387119725-9d6bac0f22fb?q=80&w=2670&auto=format&fit=crop&ixlib=rb-4.0.3&ixid=M3wxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8fA%3D%3D"
        val ImageUrl5 = "https://images.unsplash.com/photo-1598449356475-b9f71db7d847?q=80&w=2670&auto=format&fit=crop&ixlib=rb-4.0.3&ixid=M3wxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8fA%3D%3D"
        bannerList.add(HomeBannerItem(ImageUrl1))
        bannerList.add(HomeBannerItem(ImageUrl2))
        bannerList.add(HomeBannerItem(ImageUrl3))
        bannerList.add(HomeBannerItem(ImageUrl4))
        bannerList.add(HomeBannerItem(ImageUrl5))
        val bannerAdapter = activity?.let { HomeBannerRecyclerViewAdapter(bannerList, it) }
        bannerAdapter?.notifyDataSetChanged()
        binding.vpHomeBanner.orientation = ViewPager2.ORIENTATION_HORIZONTAL
        binding.vpHomeBanner.adapter = bannerAdapter
        binding.vpHomeBanner.setCurrentItem(bannerPosition, false)
        binding.vpHomeBanner.apply {
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
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // ----- 로그인 시 변경 할 부분 시작 -----
        val t_userData = Singleton_t_user.getInstance(requireContext()).jsonObject?.optJSONObject("data")
        binding.tvHomeWeight.text = t_userData?.optString("user_weight")
        binding.tvHomeAchieve.text = "미설정"
        binding.tvHomeGoal.text = "미설정"
        // ----- 로그인 시 변경 할 부분 끝 -----

        val uiManager = requireActivity().getSystemService(Context.UI_MODE_SERVICE) as UiModeManager
        when (uiManager.nightMode) {
            UiModeManager.MODE_NIGHT_YES -> {
                if (AppCompatDelegate.getDefaultNightMode() != AppCompatDelegate.MODE_NIGHT_YES) {
                    showThemeSettingDialog()
                }
            }
        }

        lifecycleScope.launch {

            // -----! db에서 받아서 뿌려주기 시작 !-----
            val responseArrayList = fetchExerciseJson(getString(com.example.mhg.R.string.IP_ADDRESS_t_Exercise_Description))
            try {
                // -----! horizontal 어댑터 시작 !-----
                val adapter = HomeHorizontalRecyclerViewAdapter(
                    this@HomeBeginnerFragment,
                    exerciseTypeList
                )
                adapter.routineList = exerciseTypeList.slice(0..3)
                binding.rvHomeBeginnerHorizontal.adapter = adapter
                val linearlayoutmanager =
                    LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
                binding.rvHomeBeginnerHorizontal.layoutManager = linearlayoutmanager
                // -----! horizontal 어댑터 끝 !-----

                // -----! vertical 어댑터 시작 !-----
                val verticalDataList = responseArrayList.toMutableList()


                val adapter2 = HomeVerticalRecyclerViewAdapter(verticalDataList,"home" )
                adapter2.verticalList = verticalDataList
                binding.rvHomeBeginnerVertical.adapter = adapter2
                val linearLayoutManager2 =
                    LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
                binding.rvHomeBeginnerVertical.layoutManager = linearLayoutManager2
                // -----! vertical 어댑터 끝 !-----


                // -----! db에서 받아서 뿌려주기 끝 !-----
                binding.tvExerciseCount.text = verticalDataList.size.toString()
                binding.nsv.isNestedScrollingEnabled = false
                binding.rvHomeBeginnerVertical.isNestedScrollingEnabled = false
                binding.rvHomeBeginnerVertical.overScrollMode = 0

                // ----- autoCompleteTextView를 통해 sort 하는 코드 시작 -----
                val sort_list = listOf("인기순", "조회순", "최신순", "오래된순")
                val adapter3 = ArrayAdapter(
                    requireContext(),
                    R.layout.simple_dropdown_item_1line,
                    sort_list
                )
                binding.actHomeBeginner.setAdapter(adapter3)
                binding.actHomeBeginner.setText(sort_list.firstOrNull(), false)

                binding.actHomeBeginner.addTextChangedListener(object : TextWatcher {
                    override fun beforeTextChanged(
                        s: CharSequence?,
                        start: Int,
                        count: Int,
                        after: Int
                    ) {
                    }

                    @SuppressLint("NotifyDataSetChanged")
                    override fun onTextChanged(
                        s: CharSequence?,
                        start: Int,
                        before: Int,
                        count: Int
                    ) {
                        when (s.toString()) {
                            "인기순" -> {
                                // 추후에 이런 필터링을 거치려면, DATA를 받아올 때 운동이 있을 때, 수정날짜? 갱신날짜같은 걸 넣어서 받아올 때, 그것만 일주일마다 갱신되게? 유지보수 하면 될 듯?
                                verticalDataList.sortByDescending { it.exerciseName }
                            }
                            "조회순" -> {
                                verticalDataList.sortByDescending { it.videoTime }
                            }
                            "최신순" -> {
                                verticalDataList.sortBy { it.relatedMuscle }
                            }
                            "오래된순" -> {

                            }
                        }
                        adapter2.notifyDataSetChanged()
                    }
                    override fun afterTextChanged(s: Editable?) {}
                })
            } catch (e: Exception) {
                Log.e(ContentValues.TAG, "Error storing exercises", e)
            }
            // ---- vertical RV에 들어갈 데이터 담는 공간 시작 ----

            // ----- autoCompleteTextView를 통해 sort 하는 코드 끝 -----

            binding.tabRoutine.addOnTabSelectedListener(object :
                TabLayout.OnTabSelectedListener {
                override fun onTabSelected(tab: TabLayout.Tab?) {
                    when (tab?.position) {
                        0 -> {
                            val adapter = HomeHorizontalRecyclerViewAdapter(
                                this@HomeBeginnerFragment,
                                exerciseTypeList.slice(0..3)
                            )
                            binding.rvHomeBeginnerHorizontal.adapter = adapter
                        }

                        1 -> {
                            val adapter = HomeHorizontalRecyclerViewAdapter(
                                this@HomeBeginnerFragment,
                                exerciseTypeList.slice(4..6)
                            )
                            binding.rvHomeBeginnerHorizontal.adapter = adapter
                        }

                        2 -> {
                            val adapter = HomeHorizontalRecyclerViewAdapter(
                                this@HomeBeginnerFragment,
                                exerciseTypeList.slice(7..9)
                            )
                            binding.rvHomeBeginnerHorizontal.adapter = adapter
                        }

                        3 -> {
                            val adapter = HomeHorizontalRecyclerViewAdapter(
                                this@HomeBeginnerFragment,
                                exerciseTypeList.slice(10..10)
                            )
                            binding.rvHomeBeginnerHorizontal.adapter = adapter
                        }
                    }
                }

                override fun onTabUnselected(tab: TabLayout.Tab?) {}
                override fun onTabReselected(tab: TabLayout.Tab?) {}
            })
        }
        }
    private fun showThemeSettingDialog() {
        MaterialAlertDialogBuilder(requireContext(), com.example.mhg.R.style.ThemeOverlay_App_MaterialAlertDialog).apply {
            setTitle("알림")
            setMessage("다크모드를 설정하시겠습니까 ?")
            setPositiveButton("예") { dialog, _ ->

                val sharedPref = context.getSharedPreferences("deviceSettings", Context.MODE_PRIVATE)
                val modeEditor = sharedPref?.edit()
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                modeEditor?.putBoolean("darkMode", true) ?: true
                modeEditor?.apply()
            }
            setNegativeButton("아니오") { dialog, _ ->
            }
            create()
        }.show()
    }
    private fun autoScrollStart(intervalTime: Long) {
        homeBannerHandler.removeMessages(0)
        homeBannerHandler.sendEmptyMessageDelayed(0, intervalTime)
    }
    private fun autoScrollStop() {
        homeBannerHandler.removeMessages(0)
    }
    private inner class HomeBannerHandler: Handler(Looper.getMainLooper()) {
        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)
            if (msg.what == 0) {
                binding.vpHomeBanner.setCurrentItem(++bannerPosition, true)
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

