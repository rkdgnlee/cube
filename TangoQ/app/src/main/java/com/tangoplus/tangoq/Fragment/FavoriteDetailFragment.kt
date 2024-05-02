package com.tangoplus.tangoq.Fragment

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.tangoplus.tangoq.Adapter.ExerciseRVAdapter
import com.tangoplus.tangoq.Object.NetworkExerciseService
import com.tangoplus.tangoq.Object.NetworkExerciseService.jsonToFavoriteItemVO
import com.tangoplus.tangoq.Object.Singleton_t_user
import com.tangoplus.tangoq.PlayFullScreenActivity
import com.tangoplus.tangoq.R
import com.tangoplus.tangoq.ViewModel.ExerciseViewModel
import com.tangoplus.tangoq.ViewModel.FavoriteItemVO
import com.tangoplus.tangoq.databinding.FragmentFavoriteDetailBinding
import kotlinx.coroutines.launch
import org.json.JSONObject


class FavoriteDetailFragment : Fragment() {
    lateinit var binding : FragmentFavoriteDetailBinding
    val viewModel : ExerciseViewModel by activityViewModels()
    var title = ""
    private var currentSn =  ""
    private var snData = JSONObject()
    lateinit var FavoriteItem : FavoriteItemVO
    lateinit var currentItem : FavoriteItemVO
    private lateinit var startForResult: ActivityResultLauncher<Intent>

    companion object {
        private const val ARG_TITLE = "title"
        fun newInstance(title: String): FavoriteDetailFragment {
            val fragment = FavoriteDetailFragment()
            val args = Bundle()
            args.putString(ARG_TITLE, title)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        startForResult = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
            }
        }
    }
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentFavoriteDetailBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.nsvFV.isNestedScrollingEnabled = true
        binding.rvFV.isNestedScrollingEnabled = false
        binding.rvFV.overScrollMode = View.OVER_SCROLL_NEVER

        title = requireArguments().getString(ARG_TITLE).toString()
        currentSn = viewModel.favoriteList.value?.find { it.second == title }?.first.toString()

        binding.actFVDetail.setText(title)

        // -----! singleton에서 전화번호 가져오기 시작 !-----
        val t_userData = Singleton_t_user.getInstance(requireContext())
        val user_mobile = t_userData.jsonObject?.optString("user_mobile")
        // -----! singleton에서 전화번호 가져오기 끝 !-----

        // -----! 운동 picklist, 제목 가져오기 시작 !-----
        lifecycleScope.launch {
            binding.sflFV.startShimmer()


            snData = NetworkExerciseService.fetchFavoriteItemJsonBySn(getString(R.string.IP_ADDRESS_t_favorite), currentSn)!!
            FavoriteItem = jsonToFavoriteItemVO(snData)

            currentItem = viewModel.favoriteItems.value?.find { it.favoriteName == title }!!
            currentItem.exercises = FavoriteItem.exercises

            if (currentItem.exercises!!.isEmpty()) {
                binding.sflFV.stopShimmer()
                binding.sflFV.visibility = View.GONE

            } else {
                binding.sflFV.stopShimmer()
                binding.sflFV.visibility = View.GONE
            }

            val pickList = mutableListOf<Pair<Int, String>>()
            viewModel.favoriteList.observe(viewLifecycleOwner) { Array ->
                pickList.clear()
                for (i in 0 until Array.size) {
                    pickList.add(Array[i])
                }
                currentSn = currentItem.favoriteSn.toString()
                setFVDetail(currentItem.favoriteSn.toString())
            }
            val adapter = ArrayAdapter(requireContext(), androidx.appcompat.R.layout.support_simple_spinner_dropdown_item, pickList.map { it.second })
            binding.actFVDetail.setAdapter(adapter)
            binding.actFVDetail.addTextChangedListener(object: TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {  }
                override fun afterTextChanged(s: Editable?) {
                    title = s.toString()
                    currentSn = pickList.find { it.first == currentItem.favoriteSn }?.first.toString() // Pair로 특정 pickList하나의 sn을 가져오기

                    currentItem = viewModel.favoriteItems.value?.find { it.favoriteName == title }!!
                    Log.v("현재 sn(currentSn)", "현재 sn: $currentSn")
                    Log.v("현재Sn", "현재 sn: ${currentItem.favoriteSn}")
                    setFVDetail(currentSn)

                }
            })

            // -----! 운동 url list 만들기 시작 !-----
//            binding.btnPickStart.setOnClickListener {
//                if (currentItem.exercises?.isNotEmpty() == true) {
//                    val resourceList = storePickUrl(viewModel)
//                    Log.w("url in resourceList", "$resourceList")
//                    val intent = Intent(requireContext(), PlayFullScreenActivity::class.java)
//                    intent.putStringArrayListExtra("resourceList", ArrayList(resourceList))
//                    startForResult.launch(intent)
//                } else {
//                    val snackbar = Snackbar.make(requireView(), "운동을 추가해주세요 ! ", Snackbar.LENGTH_SHORT)
//                    snackbar.setAction("확인", object: View.OnClickListener {
//                        override fun onClick(v: View?) {
//                            snackbar.dismiss()
//                        }
//                    })
//                    snackbar.setTextColor(Color.WHITE)
//                    snackbar.setActionTextColor(Color.WHITE)
//                    snackbar.show()
//                }
//            } // -----! 운동 url list 만들기 끝 !-----
        }
        // ----- 운동 picklist, 제목 가져오기 끝 -----

        binding.ibtnFDBack.setOnClickListener {
            if (!it.isClickable) { return@setOnClickListener }
            it.isClickable = false
            requireActivity().supportFragmentManager.beginTransaction().apply {
                replace(R.id.flMain, FavoriteFragment())
                commit()
            }
            it.isClickable = true
        }
        // -----! 편집 버튼 시작 !-----
        binding.btnFVEdit.setOnClickListener {
            requireActivity().supportFragmentManager.beginTransaction().apply {
                setCustomAnimations(R.anim.slide_in_left, R.anim.slide_in_right)
                replace(R.id.flMain, FavoriteEditFragment.newInstance(title))


                remove(FavoriteDetailFragment()).commit()
            }
            requireContext()
        } // -----! 편집 버튼 끝 !-----

        // ------! 플롯팅액션버튼 시작 !------
        binding.fabtnFDPlay.setOnClickListener {
            if (currentItem.exercises?.isNotEmpty() == true) {
                    val resourceList = storePickUrl(viewModel)
                    Log.w("url in resourceList", "$resourceList")
                    val intent = Intent(requireContext(), PlayFullScreenActivity::class.java)
                    intent.putStringArrayListExtra("resourceList", ArrayList(resourceList))
                    startForResult.launch(intent)
                } else {
                    val snackbar = Snackbar.make(requireView(), "운동을 추가해주세요 ! ", Snackbar.LENGTH_SHORT)
                    snackbar.setAction("확인", object: View.OnClickListener {
                        override fun onClick(v: View?) {
                            snackbar.dismiss()
                        }
                    })
                    snackbar.setTextColor(Color.WHITE)
                    snackbar.setActionTextColor(Color.WHITE)
                    snackbar.show()
                }
        } // ------! 플롯팅액션버튼 끝 !------

    }
    @SuppressLint("NotifyDataSetChanged")
    private fun setFVDetail(sn: String){
        lifecycleScope.launch {
            binding.sflFV.startShimmer()
            Log.v("현재 Sn", sn)
            snData = NetworkExerciseService.fetchFavoriteItemJsonBySn(getString(R.string.IP_ADDRESS_t_favorite), sn)!!
            FavoriteItem = jsonToFavoriteItemVO(snData)

            currentItem = viewModel.favoriteItems.value?.find { it.favoriteName == title }!!
            currentItem.exercises = FavoriteItem.exercises
            viewModel.favoriteItems.value = viewModel.favoriteItems.value
            if (currentItem.exercises!!.isEmpty()) {
                binding.sflFV.stopShimmer()
                binding.sflFV.visibility = View.GONE

            } else {
                binding.sflFV.stopShimmer()
                binding.sflFV.visibility = View.GONE
            }

            Log.w("detail>currentItem", "$currentItem")
            binding.tvFDExplain.text = currentItem.favoriteExplain.toString()
            val RvAdapter = ExerciseRVAdapter(this@FavoriteDetailFragment, currentItem.exercises!!, "main")
            RvAdapter.exerciseList = currentItem.exercises!!
            val linearLayoutManager2 =
                LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
            binding.rvFV.layoutManager = linearLayoutManager2
            binding.rvFV.adapter = RvAdapter
            RvAdapter.notifyDataSetChanged()

//        binding.tvPickDetailUnitNumber.text = currentItem.exercises!!.size.toString()

            var totalTime = 0
            for (i in 0 until currentItem.exercises!!.size) {
                val exercises = currentItem.exercises!!.get(i)
                Log.w("운동각 시간" ,"${exercises.videoTime!!.toInt()}")
                totalTime += exercises.videoTime!!.toInt()
            }
            Log.w("총 시간", "$totalTime")
//        binding.tvPickDetailUnitTime.text = (totalTime.div(60)).toString()
        }
    }

    private fun storePickUrl(viewModel : ExerciseViewModel) : MutableList<String> {
        val resourceList = mutableListOf<String>()
        val title = requireArguments().getString(ARG_TITLE).toString()
        val currentItem = viewModel.favoriteItems.value?.find { it.favoriteName == title }
        Log.w("PreviousStoreURL", "$currentItem")
        for (i in 0 until currentItem!!.exercises!!.size) {
            val exercises = currentItem.exercises!!.get(i)
            resourceList.add(exercises.videoFilepath.toString())
            Log.w("Finish?storeUrl", "$resourceList")
        }
        return  resourceList
    }
}