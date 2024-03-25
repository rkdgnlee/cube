package com.example.mhg

import android.R
import android.annotation.SuppressLint
import android.content.ContentValues
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.mhg.Adapter.HomeHorizontalRecyclerViewAdapter
import com.example.mhg.Adapter.HomeVerticalRecyclerViewAdapter

import com.example.mhg.VO.ExerciseItemVO
import com.example.mhg.VO.UserViewModel
import com.example.mhg.databinding.FragmentHomeBeginnerBinding
import com.example.mhg.`object`.NetworkService
import com.google.android.material.tabs.TabLayout
import kotlinx.coroutines.launch


class HomeBeginnerFragment : Fragment() {
    lateinit var binding: FragmentHomeBeginnerBinding

    lateinit var ExerciseList : MutableList<ExerciseItemVO>
    private val exerciseTypeList = listOf("목관절", "어깨", "팔꿉", "손목", "몸통전면(복부)", "몸통 후면(척추)", "몸통 코어", "엉덩", "무릎", "발목", "유산소")
    val viewModel : UserViewModel by activityViewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentHomeBeginnerBinding.inflate(layoutInflater)

        // Inflate the layout for this fragment
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // ----- 로그인 시 변경 할 부분 시작 -----
        binding.tvHomeWeight.text
        binding.tvHomeAchieve.text
        binding.tvHomeGoal.text
        // ----- 로그인 시 변경 할 부분 끝 -----


        lifecycleScope.launch {

            // -----! db에서 받아서 뿌려주기 시작 !-----
            val responseArrayList = NetworkService.fetchExerciseJson(getString(com.example.mhg.R.string.IP_ADDRESS_t_Exercise_Description))
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


                val adapter2 = HomeVerticalRecyclerViewAdapter(verticalDataList, "home")
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

                // -----! 메인 유저 체중/달성률/업적 control 시작 !-----
                binding.tvHomeWeight.text = viewModel.User.value?.optString("user_weight")

                binding.tvHomeGoal


                // -----! 메인 유저 별 체중 달성률 업적 control 끝 !-----

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

                                // TODO 추후에 이런 필터링을 거치려면, DATA를 받아올 때 운동이 있을 때, 수정날짜? 갱신날짜같은 걸 넣어서 받아올 때, 그것만 일주일마다 갱신되게? 유지보수 하면 될 듯?
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

            // ----- 각각의 arrayList에 데이터 담는 공간 시작 ----

            // ---- 각각의 arrayList에 데이터 담는 공간 끝 -----


            // ---- vertical RV에 들어갈 데이터 담는 공간 시작 ----

//        val verticaldatalist = arrayListOf(
//            HomeRVBeginnerDataClass("https://s3-alpha-sig.figma.com/img/77a1/e5c8/74f51112ca347020d9f125ef9dfd7b0e?Expires=1708905600&Key-Pair-Id=APKAQ4GOSFWCVNEHN3O4&Signature=RmOHalSCMCkCHq9fkTS1Ql-awL16xeVT3VnQYJJ6tK3A0~3XGAHMgH74ZKhohnq5ubnSXaeI7FaTQ-o6ox5PnKVbFNmnngJkw38~JigqQL-iwqsAzafPhz8FOisUy6KaMqhZmxHzLOkqlvgSItJr3FePdzBOh~exOJD1T1Y9mETlV0AKMuRyI~rvlhEPT8cd5UcDjRYjGwiymtoKyIXhBv78h0-WMYnVKVrmcg4ssZnWJYPhVIuwDeVIFE-d23FCqxT7yOQZLb7LhhWKSdTX85gG3DZStjCItIpp95Ksm8ehIzx6dHK51jVcFOCIZLygbWJmTPKEgy5jjrMZUbnbFA__", "Warm up", 3),
//            HomeRVBeginnerDataClass("https://s3-alpha-sig.figma.com/img/0367/a926/45a7ad2e42d3e66fc639f06e796efdd3?Expires=1708905600&Key-Pair-Id=APKAQ4GOSFWCVNEHN3O4&Signature=j4bPb33sBiPAmUB2fCUrWTLonadjkYOaRW598jqoBvldokKK~RCq82H0UMTDcMcerGnmGydNcVZE~hEMtv0NjVIxvgYgHK6JvFScvlvR6HVJ-O2CtEwA-sAqRBA~8tCpQCQWDqe0Z1aX95JWi6wftHQu9OqZmdkeAQQ7fSeC9FkjYTygaanr37qPcccIBBjmkNjlfQ85IU0Wn6ywYIEmrxFwPNk6xSRfOJAn9HZtHOwOLfrjVFTVZRt9UDFZw4MCyhMWI0QPBbwWc~wTIKgFJ8xAohNxo~DGMi98Ypk0o1ZHzzZ49IzxNKr5taqVW4KRKK9iGumFCLd26g3uEvjCGA__", "Warm up", 8),
//            HomeRVBeginnerDataClass("https://s3-alpha-sig.figma.com/img/5c8a/5cc3/c0ae026fa6ab97ba7d0c9fd63fb90d9c?Expires=1708905600&Key-Pair-Id=APKAQ4GOSFWCVNEHN3O4&Signature=cMNEYRJB6aZLUlojAdETQ5ZcUzWQgZL5gJkE8-f8iYyUiaFAnBJTkC6LyVTwj19d-8bfPxlWuauWPfNgNMmbvVx31l1XTrNji1JN8zeUhOfwlD8jp6kxjNHVZlwISepxUzOKEZWdS6wRvDbi6iOeUNYXLPAbfCiA5dCUaP-wlOurmoDhgFKN-XPKORDA-2-yx8A5xRbWpyMdcYjLMbCXek3c3IXZDKbXjtzQXOJyLS2HTJVmhLqN2c0DsMuK0UC0wqV0bwgeIf~0ZOcb2jddj-rsuaAUwQmtaH0xNwNGpaiBPMMwcZRv66sLfQbDYg2WMQE9ixsyYaIJIXZhxDsehA__", "Warm up", 19),
//            HomeRVBeginnerDataClass("https://s3-alpha-sig.figma.com/img/b925/3e59/fe348ec1dee0634a041808309dfd74c4?Expires=1708905600&Key-Pair-Id=APKAQ4GOSFWCVNEHN3O4&Signature=FE7mAbkmJPzLrFYjGFsDLxsU1d4ByWAQaf9MKRFu6PaSIcoeqnzUoh7uwqde3uS-L~mT2ShERpv-zXiQ45NeIm6LvOvLhG3JRTo04UO7g0E4HqmvVl1aiwgde~XT~JoSTAg~l8-~ttyC-QvP73Sy-GHZQYvX6kLa02jkj509D6VaJTCB3Px67~XDolLAPwzKuHUnJVQ~a~Zhob7zK8Rppv7maBlIgI3etlmoH1FgsXz2CQi88q9753EJBqTdjTrWf4fQSOcELezLqGN8fQhWkFnLprSNWOfj8qSkEJBhAhjSkUp1xfvIefakzl4pplJ2lIPrMY-8JYZlHnQ~4ZK5Uw__", "Warm up", 32),
//            HomeRVBeginnerDataClass("https://s3-alpha-sig.figma.com/img/dd28/d323/ee837bfe2c5d807feee37a78ff275737?Expires=1708905600&Key-Pair-Id=APKAQ4GOSFWCVNEHN3O4&Signature=g4NmwyApaTRDaVpXx795WAyVYcridHhJwlcOcv3Vu1HJnxYBTFtmssuTADmtZX5oIpcO6e4Y9mgbRQkoQtNInr4MXcOXZYZew6b7DZN9q0RJe~cIdKVlZ9gz8DVgIKDc3Q9eT2e9fft~t7OGNNfkpcAwU1WBI8Mpsdbu9rZAlyOjjjUkijUaN6Nqvv05UVKJuyTM9lpgakJh1z2dgfugEXs00hdEPIlQALRMsVe9jfD5qQR2GZUqN8dF~78-Thxn3c~OJFpG-DRe9H5iKoNsVaggVw3s~Wf~-R6S5ah7ZD6YLGigiqCCUcym2mqV4Y7VkNXZnaufA2N2uK~c~TnqpQ__", "Warm up", 28),
//            HomeRVBeginnerDataClass("https://src.hidoc.co.kr/image/lib/2023/9/1/1693553295231_0.jpg", "Warm up", 25),
//            HomeRVBeginnerDataClass("https://pds.joongang.co.kr/news/component/htmlphoto_mmdata/201912/13/ed91135f-1189-429d-ae2a-507664b03924.jpg", "Warm up", 12),
//        )
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
}

