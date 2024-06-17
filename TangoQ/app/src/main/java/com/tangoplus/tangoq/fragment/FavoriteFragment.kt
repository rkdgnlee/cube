package com.tangoplus.tangoq.fragment

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
import com.tangoplus.tangoq.adapter.FavoriteRVAdapter
import com.tangoplus.tangoq.listener.OnFavoriteDetailClickListener
import com.tangoplus.tangoq.`object`.Singleton_t_user
import com.tangoplus.tangoq.R
import com.tangoplus.tangoq.data.ExerciseVO
import com.tangoplus.tangoq.data.FavoriteViewModel
import com.tangoplus.tangoq.data.FavoriteVO
import com.tangoplus.tangoq.databinding.FragmentFavoriteBinding
import com.tangoplus.tangoq.dialog.FavoriteAddDialogFragment
import com.tangoplus.tangoq.listener.OnFavoriteSelectedClickListener
import com.tangoplus.tangoq.`object`.NetworkExercise.jsonToExerciseVO
import com.tangoplus.tangoq.`object`.NetworkFavorite.fetchFavoriteItemJsonBySn
import com.tangoplus.tangoq.`object`.NetworkFavorite.fetchFavoriteItemsJsonByEmail
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.net.URLEncoder


class FavoriteFragment : Fragment(), OnFavoriteDetailClickListener, OnFavoriteSelectedClickListener {
    lateinit var binding : FragmentFavoriteBinding
    val viewModel : FavoriteViewModel by activityViewModels()
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
        val user_email = t_userData?.optString("user_email")

        binding.tvFrTitle.text = "${t_userData?.optString("user_name")} 님의\n플레이리스트 목록"

        // ------! 즐겨 찾기 1개 추가 시작 !------
        binding.btnFrAdd.setOnClickListener {
            val dialog = FavoriteAddDialogFragment()
            dialog.show(requireActivity().supportFragmentManager, "FavoriteAddDialogFragment")
        }
        // ------! 즐겨 찾기 1개 추가 끝 !------

        val encodedUserEmail = URLEncoder.encode(user_email, "UTF-8")
        lifecycleScope.launch {

            // ------! 핸드폰 번호로 PickItems 가져오기 시작 !------
            val favoriteList = fetchFavoriteItemsJsonByEmail(getString(R.string.IP_ADDRESS_t_favorite), user_email.toString()) // user_mobile 넣기

            // ------! list관리 시작 !------
            if (favoriteList != null) {

                viewModel.favoriteList.value?.clear()
                viewModel.exerciseUnits.value?.clear()
                for (i in favoriteList.indices) { // 즐겨찾기 루프
                    // 일단 favorite 1
                    val favoriteItem = favoriteList[i]
                    val imgList = mutableListOf<String>()
                    val exerciseItemBySn = fetchFavoriteItemJsonBySn(getString(R.string.IP_ADDRESS_t_favorite),
                        favoriteItem.favoriteSn.toString()
                    )
                    // ------! 1 운동 항목에 넣기 !------
                    val exerciseUnits = mutableListOf<ExerciseVO>()
                    if (exerciseItemBySn != null) {
                        val exercises = exerciseItemBySn.optJSONArray("exercise_detail_data")
                        if (exercises != null) {
                            var time = 0
                            for (j in 0 until exercises.length()) {
                                // ------! json array만큼 전부 일단 반복해서 변수 추가 !------
                                exerciseUnits.add(jsonToExerciseVO(exercises.get(j) as JSONObject))
                                time += ( (exercises[j] as JSONObject).optString("video_duration").toInt())
                            }
                            favoriteItem.favoriteTotalTime = (time / 60).toString()
                            favoriteItem.exercises = exerciseUnits
                        }
                    } // ------! 2 시간 항목에 넣기 !------

                    // ------! 3 이미지썸네일 항목에 넣기 !------
                    val ExerciseSize = favoriteItem.exercises?.size

                    for (k in 0 until ExerciseSize!!) {
                        if (k < 4) {
                            imgList.add( favoriteItem.exercises!![k].imageFilePathReal.toString())
                            Log.v("썸네일", "${imgList}")
                        }
                    }
                    favoriteItem.imgThumbnails = imgList

                    viewModel.favoriteList.value?.add(favoriteItem) // 썸네일, 시리얼넘버, 이름까지 포함한 dataclass로 만든 favoriteVO형식의 리스트
                    // 일단 운동은 비워놓고, detail에서 넣음
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
                val FavoriteRVAdapter = FavoriteRVAdapter(viewModel.favoriteList.value!!, this@FavoriteFragment, this@FavoriteFragment, this@FavoriteFragment,"main")
                binding.rvFv.adapter = FavoriteRVAdapter
                val linearLayoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
                binding.rvFv.layoutManager = linearLayoutManager
                FavoriteRVAdapter.notifyDataSetChanged()
            } // -----! appClass list관리 끝 !-----



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

    override fun onFavoriteSelected(favoriteVO: FavoriteVO) {
    }
}
