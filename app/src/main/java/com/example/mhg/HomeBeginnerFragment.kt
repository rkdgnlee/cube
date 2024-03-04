package com.example.mhg

import android.R
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.mhg.Adapter.HomeHorizontalRecyclerViewAdapter
import com.example.mhg.Adapter.HomeVerticalRecyclerViewAdapter
import com.example.mhg.VO.HomeRVBeginnerDataClass
import com.example.mhg.databinding.FragmentHomeBeginnerBinding
import com.google.android.material.tabs.TabLayout


class HomeBeginnerFragment : Fragment() {
    lateinit var binding: FragmentHomeBeginnerBinding

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
        // ---- 로그인 시 변경 할 부분 시작 ----
        binding.tvHomeWeight.text
        binding.tvHomeAchieve.text
        binding.tvHomeGoal.text
        // ---- 로그인 시 변경 할 부분 끝 ----

        // ---- horizontal RV에 들어갈 데이터 담는 공간 시작 ----
        var horizondatalist = arrayListOf(
            HomeRVBeginnerDataClass("https://s3-alpha-sig.figma.com/img/c35a/0add/45861c3e627195b650faca5d387128ac?Expires=1708905600&Key-Pair-Id=APKAQ4GOSFWCVNEHN3O4&Signature=h0znnXkKs1~XWt998q4i6d2kBHyHdRBGxk0iWIyQBK61O9G3YPSnZx8lWQqJ2lOq~CsXlRAITgqJ5oNivviV9oYUGcw2KwgJQ~yyvR2W1sJzppw9VkFdFCU7rp04Z5~fG7e0-MAvm9~9p1kJ5M9gj4TWBQP~KVWZeJP0fyAncoMxnqQxUg3nYcBR-W-aO0IPodnPuxhGHUJ8bDX3oGkFFuvx8GeNQXL4E6lfobblrkYhkdq91P0EdC7trA7QhQ4RrzIjX4OtIOZx~6QxPFItwa1OOAEFgcB9GgTOS4n6CY7JhINavwtFxoWCCzig7BCreoxYmub-8scnv02TUGRUmw__", "코어운동 루틴", duration = 5, explanation = "일상생활의 체형 불균형을 방지하고 트레이닝을 할 때 부상 방지에 큰 도움을 줌."),
            HomeRVBeginnerDataClass("https://s3-alpha-sig.figma.com/img/dc19/12cc/4957af1d5959ceb542be3eed3988ea03?Expires=1708905600&Key-Pair-Id=APKAQ4GOSFWCVNEHN3O4&Signature=OvZ-n0W9Z8vmvladE0yjAILDvb3hDgNk7C35nz75zsNuJUr5l3teuQw67qXWrq~mpu331Fgpg~dseIONet220iEIagRcth1mp2k9YfJMuR43jZO1YRf~YIB64Cc5sWR8pCIpgw1Kk9-gJ9mMqEhunYm6UbHUmJEpfG8U-lZyvLrlwgOvyP486iGr4~b59O7e9V9xT1U8xKbGNlbu1HU2mOda~aiUea19d5JYr8l6RR1H0QKUHhH0vs7uh2qmUyJrqZOVJ7YjAdzczXcusOr7S-f5dzNd~EMtqPJbFTl0Hwz~jkrWnlTgmfECjUi73Usk03GNpNE68bjiHkgZfj4hhw__", "전신운동 루틴", duration = 30, explanation = "초기의 근육발달과 신체의 밸런스를 높이는데 중점이 되는 운동. 다양한 근육을 활용하기 때문에 칼로리 소모도 크고, 시간 대비 효과적인 운동량을 가질 수 있음.")
        )
        // ---- horizontal RV에 들어갈 데이터 담는 공간 끝 ----


        val adapter = HomeHorizontalRecyclerViewAdapter(horizondatalist)
        adapter.routineList = horizondatalist
        binding.rvHomeBeginnerHorizontal.adapter = adapter
        val linearlayoutmanager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        binding.rvHomeBeginnerHorizontal.layoutManager = linearlayoutmanager

        // ---- vertical RV에 들어갈 데이터 담는 공간 시작 ----
        val verticaldatalist = arrayListOf(
            HomeRVBeginnerDataClass("https://s3-alpha-sig.figma.com/img/77a1/e5c8/74f51112ca347020d9f125ef9dfd7b0e?Expires=1708905600&Key-Pair-Id=APKAQ4GOSFWCVNEHN3O4&Signature=RmOHalSCMCkCHq9fkTS1Ql-awL16xeVT3VnQYJJ6tK3A0~3XGAHMgH74ZKhohnq5ubnSXaeI7FaTQ-o6ox5PnKVbFNmnngJkw38~JigqQL-iwqsAzafPhz8FOisUy6KaMqhZmxHzLOkqlvgSItJr3FePdzBOh~exOJD1T1Y9mETlV0AKMuRyI~rvlhEPT8cd5UcDjRYjGwiymtoKyIXhBv78h0-WMYnVKVrmcg4ssZnWJYPhVIuwDeVIFE-d23FCqxT7yOQZLb7LhhWKSdTX85gG3DZStjCItIpp95Ksm8ehIzx6dHK51jVcFOCIZLygbWJmTPKEgy5jjrMZUbnbFA__", "Warm up", 5),
            HomeRVBeginnerDataClass("https://s3-alpha-sig.figma.com/img/0367/a926/45a7ad2e42d3e66fc639f06e796efdd3?Expires=1708905600&Key-Pair-Id=APKAQ4GOSFWCVNEHN3O4&Signature=j4bPb33sBiPAmUB2fCUrWTLonadjkYOaRW598jqoBvldokKK~RCq82H0UMTDcMcerGnmGydNcVZE~hEMtv0NjVIxvgYgHK6JvFScvlvR6HVJ-O2CtEwA-sAqRBA~8tCpQCQWDqe0Z1aX95JWi6wftHQu9OqZmdkeAQQ7fSeC9FkjYTygaanr37qPcccIBBjmkNjlfQ85IU0Wn6ywYIEmrxFwPNk6xSRfOJAn9HZtHOwOLfrjVFTVZRt9UDFZw4MCyhMWI0QPBbwWc~wTIKgFJ8xAohNxo~DGMi98Ypk0o1ZHzzZ49IzxNKr5taqVW4KRKK9iGumFCLd26g3uEvjCGA__", "Warm up", 8),
            HomeRVBeginnerDataClass("https://s3-alpha-sig.figma.com/img/5c8a/5cc3/c0ae026fa6ab97ba7d0c9fd63fb90d9c?Expires=1708905600&Key-Pair-Id=APKAQ4GOSFWCVNEHN3O4&Signature=cMNEYRJB6aZLUlojAdETQ5ZcUzWQgZL5gJkE8-f8iYyUiaFAnBJTkC6LyVTwj19d-8bfPxlWuauWPfNgNMmbvVx31l1XTrNji1JN8zeUhOfwlD8jp6kxjNHVZlwISepxUzOKEZWdS6wRvDbi6iOeUNYXLPAbfCiA5dCUaP-wlOurmoDhgFKN-XPKORDA-2-yx8A5xRbWpyMdcYjLMbCXek3c3IXZDKbXjtzQXOJyLS2HTJVmhLqN2c0DsMuK0UC0wqV0bwgeIf~0ZOcb2jddj-rsuaAUwQmtaH0xNwNGpaiBPMMwcZRv66sLfQbDYg2WMQE9ixsyYaIJIXZhxDsehA__", "Warm up", 6),
            HomeRVBeginnerDataClass("https://s3-alpha-sig.figma.com/img/b925/3e59/fe348ec1dee0634a041808309dfd74c4?Expires=1708905600&Key-Pair-Id=APKAQ4GOSFWCVNEHN3O4&Signature=FE7mAbkmJPzLrFYjGFsDLxsU1d4ByWAQaf9MKRFu6PaSIcoeqnzUoh7uwqde3uS-L~mT2ShERpv-zXiQ45NeIm6LvOvLhG3JRTo04UO7g0E4HqmvVl1aiwgde~XT~JoSTAg~l8-~ttyC-QvP73Sy-GHZQYvX6kLa02jkj509D6VaJTCB3Px67~XDolLAPwzKuHUnJVQ~a~Zhob7zK8Rppv7maBlIgI3etlmoH1FgsXz2CQi88q9753EJBqTdjTrWf4fQSOcELezLqGN8fQhWkFnLprSNWOfj8qSkEJBhAhjSkUp1xfvIefakzl4pplJ2lIPrMY-8JYZlHnQ~4ZK5Uw__", "Warm up", 6),
            HomeRVBeginnerDataClass("https://s3-alpha-sig.figma.com/img/dd28/d323/ee837bfe2c5d807feee37a78ff275737?Expires=1708905600&Key-Pair-Id=APKAQ4GOSFWCVNEHN3O4&Signature=g4NmwyApaTRDaVpXx795WAyVYcridHhJwlcOcv3Vu1HJnxYBTFtmssuTADmtZX5oIpcO6e4Y9mgbRQkoQtNInr4MXcOXZYZew6b7DZN9q0RJe~cIdKVlZ9gz8DVgIKDc3Q9eT2e9fft~t7OGNNfkpcAwU1WBI8Mpsdbu9rZAlyOjjjUkijUaN6Nqvv05UVKJuyTM9lpgakJh1z2dgfugEXs00hdEPIlQALRMsVe9jfD5qQR2GZUqN8dF~78-Thxn3c~OJFpG-DRe9H5iKoNsVaggVw3s~Wf~-R6S5ah7ZD6YLGigiqCCUcym2mqV4Y7VkNXZnaufA2N2uK~c~TnqpQ__", "Warm up", 6),
            HomeRVBeginnerDataClass("https://src.hidoc.co.kr/image/lib/2023/9/1/1693553295231_0.jpg", "Warm up", 6),
            HomeRVBeginnerDataClass("https://pds.joongang.co.kr/news/component/htmlphoto_mmdata/201912/13/ed91135f-1189-429d-ae2a-507664b03924.jpg", "Warm up", 6),
        )

        val adapter2 = HomeVerticalRecyclerViewAdapter(verticaldatalist)
        adapter2.warmupList = verticaldatalist
        binding.rvHomeBeginnerVertical.adapter = adapter2
        val linearLayoutManager2 = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
        binding.rvHomeBeginnerVertical.layoutManager = linearLayoutManager2


        // ---- vertical RV에 들어갈 데이터 담는 공간 끝 ----

        val sort_list = listOf("인기순", "조회순", "최신순", "오래된순")
        val adapter3 = ArrayAdapter(requireContext(), R.layout.simple_dropdown_item_1line, sort_list)
        binding.actHomeBeginner.setAdapter(adapter3)
        binding.actHomeBeginner.setText(sort_list.firstOrNull(), false)

        binding.nsv.isNestedScrollingEnabled = false
        binding.rvHomeBeginnerVertical.isNestedScrollingEnabled = false
        binding.rvHomeBeginnerVertical.overScrollMode = 0

        binding.tabRoutine.addOnTabSelectedListener(object: TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                when (tab?.position) {
                    0 -> {
                        horizondatalist = arrayListOf(
                            HomeRVBeginnerDataClass("https://s3-alpha-sig.figma.com/img/c35a/0add/45861c3e627195b650faca5d387128ac?Expires=1708905600&Key-Pair-Id=APKAQ4GOSFWCVNEHN3O4&Signature=h0znnXkKs1~XWt998q4i6d2kBHyHdRBGxk0iWIyQBK61O9G3YPSnZx8lWQqJ2lOq~CsXlRAITgqJ5oNivviV9oYUGcw2KwgJQ~yyvR2W1sJzppw9VkFdFCU7rp04Z5~fG7e0-MAvm9~9p1kJ5M9gj4TWBQP~KVWZeJP0fyAncoMxnqQxUg3nYcBR-W-aO0IPodnPuxhGHUJ8bDX3oGkFFuvx8GeNQXL4E6lfobblrkYhkdq91P0EdC7trA7QhQ4RrzIjX4OtIOZx~6QxPFItwa1OOAEFgcB9GgTOS4n6CY7JhINavwtFxoWCCzig7BCreoxYmub-8scnv02TUGRUmw__", "코어운동 루틴", duration = 5, explanation = "일상생활의 체형 불균형을 방지하고 트레이닝을 할 때 부상 방지에 큰 도움을 줌."),
                            HomeRVBeginnerDataClass("https://s3-alpha-sig.figma.com/img/dc19/12cc/4957af1d5959ceb542be3eed3988ea03?Expires=1708905600&Key-Pair-Id=APKAQ4GOSFWCVNEHN3O4&Signature=OvZ-n0W9Z8vmvladE0yjAILDvb3hDgNk7C35nz75zsNuJUr5l3teuQw67qXWrq~mpu331Fgpg~dseIONet220iEIagRcth1mp2k9YfJMuR43jZO1YRf~YIB64Cc5sWR8pCIpgw1Kk9-gJ9mMqEhunYm6UbHUmJEpfG8U-lZyvLrlwgOvyP486iGr4~b59O7e9V9xT1U8xKbGNlbu1HU2mOda~aiUea19d5JYr8l6RR1H0QKUHhH0vs7uh2qmUyJrqZOVJ7YjAdzczXcusOr7S-f5dzNd~EMtqPJbFTl0Hwz~jkrWnlTgmfECjUi73Usk03GNpNE68bjiHkgZfj4hhw__", "전신운동 루틴", duration = 30, explanation = "초기의 근육발달과 신체의 밸런스를 높이는데 중점이 되는 운동. 다양한 근육을 활용하기 때문에 칼로리 소모도 크고, 시간 대비 효과적인 운동량을 가질 수 있음.")
                        )
                        adapter.routineList = horizondatalist
                        binding.rvHomeBeginnerHorizontal.adapter = adapter
                    }
                    1 -> {
                        horizondatalist = arrayListOf(
                            HomeRVBeginnerDataClass("https://s3-alpha-sig.figma.com/img/c35a/0add/45861c3e627195b650faca5d387128ac?Expires=1708905600&Key-Pair-Id=APKAQ4GOSFWCVNEHN3O4&Signature=h0znnXkKs1~XWt998q4i6d2kBHyHdRBGxk0iWIyQBK61O9G3YPSnZx8lWQqJ2lOq~CsXlRAITgqJ5oNivviV9oYUGcw2KwgJQ~yyvR2W1sJzppw9VkFdFCU7rp04Z5~fG7e0-MAvm9~9p1kJ5M9gj4TWBQP~KVWZeJP0fyAncoMxnqQxUg3nYcBR-W-aO0IPodnPuxhGHUJ8bDX3oGkFFuvx8GeNQXL4E6lfobblrkYhkdq91P0EdC7trA7QhQ4RrzIjX4OtIOZx~6QxPFItwa1OOAEFgcB9GgTOS4n6CY7JhINavwtFxoWCCzig7BCreoxYmub-8scnv02TUGRUmw__", "유산소 루틴", duration = 5, explanation = "일상생활의 체형 불균형을 방지하고 트레이닝을 할 때 부상 방지에 큰 도움을 줌."),
                            HomeRVBeginnerDataClass("https://s3-alpha-sig.figma.com/img/dc19/12cc/4957af1d5959ceb542be3eed3988ea03?Expires=1708905600&Key-Pair-Id=APKAQ4GOSFWCVNEHN3O4&Signature=OvZ-n0W9Z8vmvladE0yjAILDvb3hDgNk7C35nz75zsNuJUr5l3teuQw67qXWrq~mpu331Fgpg~dseIONet220iEIagRcth1mp2k9YfJMuR43jZO1YRf~YIB64Cc5sWR8pCIpgw1Kk9-gJ9mMqEhunYm6UbHUmJEpfG8U-lZyvLrlwgOvyP486iGr4~b59O7e9V9xT1U8xKbGNlbu1HU2mOda~aiUea19d5JYr8l6RR1H0QKUHhH0vs7uh2qmUyJrqZOVJ7YjAdzczXcusOr7S-f5dzNd~EMtqPJbFTl0Hwz~jkrWnlTgmfECjUi73Usk03GNpNE68bjiHkgZfj4hhw__", "코어운동 루틴", duration = 30, explanation = "초기의 근육발달과 신체의 밸런스를 높이는데 중점이 되는 운동. 다양한 근육을 활용하기 때문에 칼로리 소모도 크고, 시간 대비 효과적인 운동량을 가질 수 있음.")
                        )
                        adapter.routineList = horizondatalist
                        binding.rvHomeBeginnerHorizontal.adapter = adapter
                    }
                    2 -> {
                        horizondatalist = arrayListOf(
                            HomeRVBeginnerDataClass("https://s3-alpha-sig.figma.com/img/c35a/0add/45861c3e627195b650faca5d387128ac?Expires=1708905600&Key-Pair-Id=APKAQ4GOSFWCVNEHN3O4&Signature=h0znnXkKs1~XWt998q4i6d2kBHyHdRBGxk0iWIyQBK61O9G3YPSnZx8lWQqJ2lOq~CsXlRAITgqJ5oNivviV9oYUGcw2KwgJQ~yyvR2W1sJzppw9VkFdFCU7rp04Z5~fG7e0-MAvm9~9p1kJ5M9gj4TWBQP~KVWZeJP0fyAncoMxnqQxUg3nYcBR-W-aO0IPodnPuxhGHUJ8bDX3oGkFFuvx8GeNQXL4E6lfobblrkYhkdq91P0EdC7trA7QhQ4RrzIjX4OtIOZx~6QxPFItwa1OOAEFgcB9GgTOS4n6CY7JhINavwtFxoWCCzig7BCreoxYmub-8scnv02TUGRUmw__", "고관절스트레칭 루틴", duration = 5, explanation = "일상생활의 체형 불균형을 방지하고 트레이닝을 할 때 부상 방지에 큰 도움을 줌."),
                            HomeRVBeginnerDataClass("https://s3-alpha-sig.figma.com/img/dc19/12cc/4957af1d5959ceb542be3eed3988ea03?Expires=1708905600&Key-Pair-Id=APKAQ4GOSFWCVNEHN3O4&Signature=OvZ-n0W9Z8vmvladE0yjAILDvb3hDgNk7C35nz75zsNuJUr5l3teuQw67qXWrq~mpu331Fgpg~dseIONet220iEIagRcth1mp2k9YfJMuR43jZO1YRf~YIB64Cc5sWR8pCIpgw1Kk9-gJ9mMqEhunYm6UbHUmJEpfG8U-lZyvLrlwgOvyP486iGr4~b59O7e9V9xT1U8xKbGNlbu1HU2mOda~aiUea19d5JYr8l6RR1H0QKUHhH0vs7uh2qmUyJrqZOVJ7YjAdzczXcusOr7S-f5dzNd~EMtqPJbFTl0Hwz~jkrWnlTgmfECjUi73Usk03GNpNE68bjiHkgZfj4hhw__", "허리재활 루틴", duration = 30, explanation = "초기의 근육발달과 신체의 밸런스를 높이는데 중점이 되는 운동. 다양한 근육을 활용하기 때문에 칼로리 소모도 크고, 시간 대비 효과적인 운동량을 가질 수 있음.")
                        )
                        adapter.routineList = horizondatalist
                        binding.rvHomeBeginnerHorizontal.adapter = adapter
                    }
                    3 -> {
                        horizondatalist = arrayListOf(
                            HomeRVBeginnerDataClass("https://s3-alpha-sig.figma.com/img/c35a/0add/45861c3e627195b650faca5d387128ac?Expires=1708905600&Key-Pair-Id=APKAQ4GOSFWCVNEHN3O4&Signature=h0znnXkKs1~XWt998q4i6d2kBHyHdRBGxk0iWIyQBK61O9G3YPSnZx8lWQqJ2lOq~CsXlRAITgqJ5oNivviV9oYUGcw2KwgJQ~yyvR2W1sJzppw9VkFdFCU7rp04Z5~fG7e0-MAvm9~9p1kJ5M9gj4TWBQP~KVWZeJP0fyAncoMxnqQxUg3nYcBR-W-aO0IPodnPuxhGHUJ8bDX3oGkFFuvx8GeNQXL4E6lfobblrkYhkdq91P0EdC7trA7QhQ4RrzIjX4OtIOZx~6QxPFItwa1OOAEFgcB9GgTOS4n6CY7JhINavwtFxoWCCzig7BCreoxYmub-8scnv02TUGRUmw__", "지지력 운동 루틴", duration = 5, explanation = "일상생활의 체형 불균형을 방지하고 트레이닝을 할 때 부상 방지에 큰 도움을 줌."),
                            HomeRVBeginnerDataClass("https://s3-alpha-sig.figma.com/img/dc19/12cc/4957af1d5959ceb542be3eed3988ea03?Expires=1708905600&Key-Pair-Id=APKAQ4GOSFWCVNEHN3O4&Signature=OvZ-n0W9Z8vmvladE0yjAILDvb3hDgNk7C35nz75zsNuJUr5l3teuQw67qXWrq~mpu331Fgpg~dseIONet220iEIagRcth1mp2k9YfJMuR43jZO1YRf~YIB64Cc5sWR8pCIpgw1Kk9-gJ9mMqEhunYm6UbHUmJEpfG8U-lZyvLrlwgOvyP486iGr4~b59O7e9V9xT1U8xKbGNlbu1HU2mOda~aiUea19d5JYr8l6RR1H0QKUHhH0vs7uh2qmUyJrqZOVJ7YjAdzczXcusOr7S-f5dzNd~EMtqPJbFTl0Hwz~jkrWnlTgmfECjUi73Usk03GNpNE68bjiHkgZfj4hhw__", "체형 교정 루틴", duration = 30, explanation = "초기의 근육발달과 신체의 밸런스를 높이는데 중점이 되는 운동. 다양한 근육을 활용하기 때문에 칼로리 소모도 크고, 시간 대비 효과적인 운동량을 가질 수 있음.")
                        )
                        adapter.routineList = horizondatalist
                        binding.rvHomeBeginnerHorizontal.adapter = adapter
                    }
                }
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }
}

