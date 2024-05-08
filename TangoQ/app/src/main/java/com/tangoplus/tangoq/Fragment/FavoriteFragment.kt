package com.tangoplus.tangoq.Fragment

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.tangoplus.tangoq.Adapter.FavoriteRVAdapter
import com.tangoplus.tangoq.Listener.OnFavoriteDetailClickListener
import com.tangoplus.tangoq.Object.NetworkExerciseService
import com.tangoplus.tangoq.Object.NetworkExerciseService.fetchFavoriteItemsJsonByMobile
import com.tangoplus.tangoq.Object.Singleton_t_user
import com.tangoplus.tangoq.R
import com.tangoplus.tangoq.ViewModel.ExerciseViewModel
import com.tangoplus.tangoq.ViewModel.FavoriteItemVO
import com.tangoplus.tangoq.ViewModel.FavoriteVO
import com.tangoplus.tangoq.databinding.FragmentFavoriteBinding
import kotlinx.coroutines.launch


class FavoriteFragment : Fragment(), OnFavoriteDetailClickListener {
    lateinit var binding : FragmentFavoriteBinding
    val viewModel : ExerciseViewModel by activityViewModels()
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentFavoriteBinding.inflate(inflater)
        binding.sflFV.startShimmer()
        return binding.root
    }

    @SuppressLint("SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // ------! singleton에서 전화번호 가져오기 시작 !------
        val t_userData = Singleton_t_user.getInstance(requireContext()).jsonObject?.optJSONObject("data")
        val user_mobile = t_userData?.optString("user_mobile")

        binding.tvFrTitle.text = "${t_userData?.optString("user_name")} 님의\n플레이리스트 목록"
//        val encodedUserMobile = URLEncoder.encode(user_mobile, "UTF-8")
        lifecycleScope.launch {

            // ------! 핸드폰 번호로 PickItems 가져오기 시작 !------
            val pickList = fetchFavoriteItemsJsonByMobile(getString(R.string.IP_ADDRESS_t_favorite), user_mobile.toString()) // user_mobile 넣기

            // ------! list관리 시작 !------
            if (pickList != null) {
                viewModel.favoriteList.value?.clear()
                viewModel.favoriteItems.value?.clear()
                viewModel.exerciseUnits.value?.clear()
                for (i in 0 until pickList.length()) {
                    // 일단 favorite
                    val pickItemVO = FavoriteItemVO(
                        favoriteSn = pickList.getJSONObject(i).optInt("favorite_sn"),
                        favoriteName = pickList.getJSONObject(i).optString("favorite_name"),
                        favoriteExplain = pickList.getJSONObject(i).optString("favorite_description"),
                        favoriteDisclosure = "",
                        exercises = mutableListOf()
                    ) // 각각의 FavoriteItemVO 만들고,  그후 추가적으로 조회해서 썸네일 넣기.
                    val snData = NetworkExerciseService.fetchFavoriteItemJsonBySn(getString(R.string.IP_ADDRESS_t_favorite),
                        pickItemVO.favoriteSn.toString()
                    )
                    val FavoriteItem = NetworkExerciseService.jsonToFavoriteItemVO(snData!!)
                    val imgList = mutableListOf<String>()
                    var time = 0
                    if (FavoriteItem.exercises?.size!! >= 4) {
                        for (i in 0 until FavoriteItem.exercises?.size!!) {
                            time += (FavoriteItem.exercises!![i].videoTime!!.toInt())
                            Log.v("총 second", "$time")
                        }
                        for (j in 0 until 4) {
                            imgList.add(FavoriteItem.exercises!![j].imgUrl.toString())
                        }
                    }
                    Log.v("pickItem", "$pickItemVO")
                    val favorite = FavoriteVO(
                        imgThumbnailList = imgList,
                        sn = pickList.getJSONObject(i).optInt("favorite_sn"),
                        name = pickList.getJSONObject(i).getString("favorite_name"),
                        regDate = pickList.getJSONObject(i).getString("reg_date").substring(0, 11),
                        count = FavoriteItem.exercises!!.size.toString(),
                        time = (time / 60).toString(),
                    )

                    Log.v("favorite", favorite.time.toString())
                    viewModel.favoriteList.value?.add(favorite) // 썸네일, 시리얼넘버, 이름까지 포함한 dataclass로 만든 favoriteVO형식의 리스트
                    viewModel.favoriteItems.value?.add(pickItemVO) // 일단 운동은 비워놓고, detail에서 넣음
                }
            }


            viewModel.favoriteList.observe(viewLifecycleOwner) { jsonArray ->
//                 아무것도 없을 때 나오는 캐릭터
                if (jsonArray.isEmpty()) {
                    binding.sflFV.stopShimmer()
                    binding.sflFV.visibility = View.GONE
//                    binding.ivPickNull.visibility = View.VISIBLE
                } else {
                    binding.sflFV.stopShimmer()
                    binding.sflFV.visibility = View.GONE
                }
            } // -----! appClass list관리 끝 !-----

            val FavoriteRVAdapter = FavoriteRVAdapter(viewModel.favoriteList.value!!, this@FavoriteFragment, this@FavoriteFragment)
            binding.rvFv.adapter = FavoriteRVAdapter
            val linearLayoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
            binding.rvFv.layoutManager = linearLayoutManager
            FavoriteRVAdapter.notifyDataSetChanged()

//            binding.btnFavoriteadd.setOnClickListener {
//                viewModel.exerciseUnits.value?.clear()
//                requireActivity().supportFragmentManager.beginTransaction().apply {
//                    setCustomAnimations(R.anim.slide_in_left, R.anim.slide_in_right)
//                    replace(R.id.flPick, PickAddFragment())
//                    addToBackStack(null)
//                    commit()
//                }
            }
            // ------! 핸드폰 번호로 PickItems 가져오기 끝 !------
        }

    override fun onFavoriteClick(title: String) {
        requireActivity().supportFragmentManager.beginTransaction().apply {
            replace(R.id.flMain, FavoriteDetailFragment.newInstance(title))
            addToBackStack(null)
            commit()

        }
    }
}
