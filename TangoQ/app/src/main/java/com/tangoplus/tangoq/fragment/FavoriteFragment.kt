package com.tangoplus.tangoq.fragment

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
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
import com.tangoplus.tangoq.`object`.NetworkFavorite.fetchFavoriteItemsJsonBySn
import kotlinx.coroutines.launch
import org.json.JSONObject


class FavoriteFragment : Fragment(), OnFavoriteDetailClickListener, OnFavoriteSelectedClickListener {
    lateinit var binding : FragmentFavoriteBinding
    val viewModel : FavoriteViewModel by activityViewModels()
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentFavoriteBinding.inflate(inflater)
        binding.sflF.startShimmer()
        return binding.root
    }

    @SuppressLint("SetTextI18n", "NotifyDataSetChanged")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // ------! singleton에서 전화번호 가져오기 시작 !------
        val t_userData = Singleton_t_user.getInstance(requireContext()).jsonObject?.optJSONObject("login_data")
        val userSn = t_userData?.optString("user_sn")

        binding.tvFTitle.text = "${t_userData?.optString("user_name")  ?: "미설정" } 님의\n플레이리스트 목록"
        viewModel.favoriteList.value?.clear()
        viewModel.exerciseUnits.value?.clear()
        // ------! 즐겨 찾기 1개 추가 시작 !------
        binding.btnFAdd.setOnClickListener {
            val dialog = FavoriteAddDialogFragment()
            dialog.show(requireActivity().supportFragmentManager, "FavoriteAddDialogFragment")
        }
        // ------! 즐겨 찾기 1개 추가 끝 !------

//        val encodedUserEmail = URLEncoder.encode(user_email, "UTF-8")
        lifecycleScope.launch {

            // ------! Sn으로 Favorites 가져오기 시작 !------
            val favoriteList = fetchFavoriteItemsJsonBySn(getString(R.string.IP_ADDRESS_t_favorite), userSn.toString())
            viewModel.favoriteList.value = favoriteList
            Log.v("favoriteCount", "${viewModel.favoriteList.value!!.size}")

            // ------! list관리 시작 !------
            val favoriteItems = mutableListOf<FavoriteVO>()
            if (favoriteList.isNotEmpty()) {
                for (i in favoriteList.indices) {

                    // ------! 1 favorite sn 목록가져오기 !------
                    val favoriteItem = fetchFavoriteItemJsonBySn(getString(R.string.IP_ADDRESS_t_favorite), favoriteList[i].favoriteSn.toString())

                    // ------! 2 운동 디테일 채우기 !------
                    favoriteItems.add(favoriteItem)
                }
                viewModel.favoriteList.value = favoriteItems
            }

            viewModel.favoriteList.observe(viewLifecycleOwner) { jsonArray ->
//                 아무것도 없을 때 나오는 캐릭터
                if (jsonArray.isEmpty()) {
                    binding.sflF.stopShimmer()
                    binding.sflF.visibility = View.GONE
                    binding.tvFEmpty.visibility = View.VISIBLE

//                    binding.ivPickNull.visibility = View.VISIBLE
                } else {
                    binding.sflF.stopShimmer()
                    binding.sflF.visibility = View.GONE
                    binding.tvFEmpty.visibility = View.GONE
                }
                val favoriteRvAdapter = FavoriteRVAdapter(viewModel.favoriteList.value!!, this@FavoriteFragment, this@FavoriteFragment, this@FavoriteFragment,"main")
                binding.rvF.adapter = favoriteRvAdapter
                val linearLayoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
                binding.rvF.layoutManager = linearLayoutManager
                favoriteRvAdapter.notifyDataSetChanged()
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

    override fun onFavoriteClick(sn: Int) {
        requireActivity().supportFragmentManager.beginTransaction().apply {
            replace(R.id.flMain, FavoriteDetailFragment.newInstance(sn))
            addToBackStack(null)
            commit()

        }
    }

    override fun onFavoriteSelected(favoriteVO: FavoriteVO) {
    }
}
