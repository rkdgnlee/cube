package com.example.mhg

import android.annotation.SuppressLint
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.viewpager2.widget.ViewPager2
import com.example.mhg.Adapter.HomeBannerRecyclerViewAdapter
import com.example.mhg.VO.HomeBannerItem
import com.example.mhg.databinding.FragmentHomeIntermediateBinding


class HomeIntermediateFragment : Fragment() {
    lateinit var binding: FragmentHomeIntermediateBinding
    private var bannerPosition = Int.MAX_VALUE/2
    private var homeBannerHandler = HomeBannerHandler()
    private val intervalTime = 1800.toLong()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentHomeIntermediateBinding.inflate(inflater)

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
        val bannerAdapter = activity?.let {HomeBannerRecyclerViewAdapter(bannerList, it)}
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