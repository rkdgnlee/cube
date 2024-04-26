package com.tangoplus.tangoq.Fragment

import android.content.ContentValues
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
import com.tangoplus.tangoq.Interface.onFavoriteDetailClickListener
import com.tangoplus.tangoq.Object.NetworkExerciseService.fetchFavoriteItemsJsonByMobile
import com.tangoplus.tangoq.Object.Singleton_t_user
import com.tangoplus.tangoq.R
import com.tangoplus.tangoq.ViewModel.ExerciseViewModel
import com.tangoplus.tangoq.ViewModel.FavoriteItemVO
import com.tangoplus.tangoq.databinding.FragmentFavoriteBinding
import kotlinx.coroutines.launch
import java.net.URLEncoder


class FavoriteFragment : Fragment(), onFavoriteDetailClickListener {
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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // -----! singleton에서 전화번호 가져오기 시작 !-----
        val t_userData = Singleton_t_user.getInstance(requireContext())
        val user_mobile = t_userData.jsonObject?.optJSONObject("data")?.optString("user_mobile")
//        val encodedUserMobile = URLEncoder.encode(user_mobile, "UTF-8")
        lifecycleScope.launch {

            // -----! 핸드폰 번호로 PickItems 가져오기 시작 !-----
            val pickList = fetchFavoriteItemsJsonByMobile(getString(R.string.IP_ADDRESS_t_favorite), "+8210-4157-9173") // user_mobile 넣기

            // -----! appClass list관리 시작 !-----
            if (pickList != null) {
                viewModel.favoriteList.value?.clear()
                viewModel.favoriteItems.value?.clear()
                viewModel.exerciseUnits.value?.clear()
                for (i in 0 until pickList.length()) {
                    viewModel.favoriteList.value?.add(Pair(pickList.getJSONObject(i).optInt("favorite_sn"),pickList.getJSONObject(i).getString("favorite_name")))
                    val pickItemVO = FavoriteItemVO(
                        favoriteSn = pickList.getJSONObject(i).optInt("favorite_sn"),
                        favoriteName = pickList.getJSONObject(i).optString("favorite_name"),
                        favoriteExplain = pickList.getJSONObject(i).optString("favorite_description"),
                        favoriteDisclosure = "",
                        exercises = mutableListOf()
                    )
                    viewModel.favoriteItems.value?.add(pickItemVO)
                    Log.w("${ContentValues.TAG}, pickitem", "${viewModel.favoriteItems.value}")

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

            val FavoriteRVAdapter = FavoriteRVAdapter(viewModel.favoriteList.value!!.map { it.second }.toMutableList(), this@FavoriteFragment, requireActivity())
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
            // -----! 핸드폰 번호로 PickItems 가져오기 끝 !-----
        }

    override fun onFavoriteClick(title: String) {
        requireActivity().supportFragmentManager.beginTransaction().apply {
            replace(R.id.flMain, FavoriteDetailFragment.newInstance(title))
            addToBackStack(null)
            commit()

        }
    }
}
