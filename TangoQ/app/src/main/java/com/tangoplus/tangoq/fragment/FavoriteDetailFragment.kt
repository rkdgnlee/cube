package com.tangoplus.tangoq.fragment

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
import com.tangoplus.tangoq.adapter.ExerciseRVAdapter
import com.tangoplus.tangoq.dialog.FavoriteBSDialogFragment
import com.tangoplus.tangoq.PlayFullScreenActivity
import com.tangoplus.tangoq.R
import com.tangoplus.tangoq.data.FavoriteViewModel
import com.tangoplus.tangoq.data.FavoriteVO
import com.tangoplus.tangoq.databinding.FragmentFavoriteDetailBinding
import com.tangoplus.tangoq.`object`.NetworkFavorite.fetchFavoriteItemJsonBySn
import com.tangoplus.tangoq.`object`.NetworkFavorite.jsonToFavoriteItemVO
import com.tangoplus.tangoq.`object`.Singleton_t_history
import kotlinx.coroutines.launch
import org.json.JSONObject


class FavoriteDetailFragment : Fragment(){
    lateinit var binding : FragmentFavoriteDetailBinding
    val viewModel : FavoriteViewModel by activityViewModels()
    var title = ""
    private var currentSn =  ""
    private var snData = JSONObject()
    private lateinit var favoriteItem : FavoriteVO
    lateinit var currentItem : FavoriteVO
    private lateinit var startForResult: ActivityResultLauncher<Intent>
    private lateinit var singletonInstance: Singleton_t_history


//    var popupWindow : PopupWindow?= null
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
        singletonInstance = Singleton_t_history.getInstance(requireContext())
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
        currentSn = viewModel.favoriteList.value?.find { it.favoriteName == title }?.favoriteSn.toString()

        binding.actFVDetail.setText(title)

        /** ============================= 흐름 =============================
         *  1. pickList에서 sn을 받아서 옴
         *  2. sn으로 조회한 즐겨찾기 1개의 이름, 설명, 운동 목록 가져옴 (favoritelist에서)
         *  3. 가져와서 뿌리기 (viewModel.pickitem
         *
         *
         *
         * */



        // -----! 운동 picklist, 제목 가져오기 시작 !-----
        lifecycleScope.launch {
            binding.sflFV.startShimmer()


            snData = fetchFavoriteItemJsonBySn(getString(R.string.IP_ADDRESS_t_favorite), currentSn)!!
            favoriteItem = jsonToFavoriteItemVO(snData)
//
//            // ------! 즐겨찾기 넣어서 가져오기 !------
            currentItem = viewModel.favoriteList.value?.find { it.favoriteSn == currentSn.toInt() }!!
            Log.v("Detail>CurrentItem", "sn: ${currentItem.favoriteSn} ,갯수: ${currentItem.imgThumbnails!!.size}")
            currentItem.exercises = favoriteItem.exercises
            currentSn = currentItem.favoriteSn.toString()
            setFVDetail(currentItem.favoriteSn.toString())
//
            if (currentItem.exercises!!.isEmpty()) {
                binding.sflFV.stopShimmer()
                binding.sflFV.visibility = View.GONE

            } else {
                binding.sflFV.stopShimmer()
                binding.sflFV.visibility = View.GONE

            }

            val pickList = mutableListOf<String>()
            viewModel.favoriteList.observe(viewLifecycleOwner) { array ->
                pickList.clear()
                for (i in 0 until array.size) {
                    pickList.add(array[i].favoriteName.toString())
                }

            }

            val adapter = ArrayAdapter(requireContext(), androidx.appcompat.R.layout.support_simple_spinner_dropdown_item, pickList)
            binding.actFVDetail.setAdapter(adapter)
            binding.actFVDetail.addTextChangedListener(object: TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {  }
                override fun afterTextChanged(s: Editable?) {
                    title = s.toString()
                    currentItem = viewModel.favoriteList.value?.find { it.favoriteName == title }!!
                    currentSn = viewModel.favoriteList.value?.find { it.favoriteSn == currentItem.favoriteSn }?.favoriteSn.toString() // Pair로 특정 pickList하나의 sn을 가져오기


                    Log.v("현재 sn(currentSn)", "현재 sn: $currentSn")
                    Log.v("현재Sn", "현재 sn: ${currentItem.favoriteSn}")
                    setFVDetail(currentSn)
                }
            })
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
        binding.ibtnFDMore.setOnClickListener {
            // ------! img bytearray로 만들기가 안됨.


            val bsFragment = FavoriteBSDialogFragment()
            val bundle = Bundle()
            Log.v("currentItem상태", "${currentItem.exercises?.size}")

            bundle.putParcelable("Favorite", currentItem)
            bsFragment.arguments = bundle
            val fragmentManager = requireActivity().supportFragmentManager
            bsFragment.show(fragmentManager, bsFragment.tag)
        }


        // -----! 편집 버튼 시작 !-----
        binding.btnFVEdit.setOnClickListener {
            requireActivity().supportFragmentManager.beginTransaction().apply {
//                setCustomAnimations(R.anim.slide_in_left, R.anim.slide_in_right)
                replace(R.id.flMain, FavoriteEditFragment.newInstance(title))


                remove(FavoriteDetailFragment()).commit()
            }
            requireContext()
        } // -----! 편집 버튼 끝 !-----


        // ------! 플롯팅액션버튼 시작 !------
        binding.fabtnFDPlay.setOnClickListener {
            if (currentItem.exercises?.isNotEmpty() == true) {
                    val urls = storeFavoriteUrl(viewModel)
//                    Log.w("url in resourceList", "$resourceList")
                    val intent = Intent(requireContext(), PlayFullScreenActivity::class.java)
                    intent.putStringArrayListExtra("urls", ArrayList(urls))
                    startForResult.launch(intent)
                } else {
                    val snackbar = Snackbar.make(requireView(), "운동을 추가해주세요 ! ", Snackbar.LENGTH_SHORT)
                    snackbar.setAction("확인") { snackbar.dismiss() }
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
            snData = fetchFavoriteItemJsonBySn(getString(R.string.IP_ADDRESS_t_favorite), sn)!!
            favoriteItem = jsonToFavoriteItemVO(snData)

            currentItem = viewModel.favoriteList.value?.find { it.favoriteName == title }!!
            currentItem.exercises = favoriteItem.exercises
            viewModel.favoriteList.value = viewModel.favoriteList.value
            if (currentItem.exercises!!.isEmpty()) {
                binding.sflFV.stopShimmer()
                binding.sflFV.visibility = View.GONE
            } else {
                binding.sflFV.stopShimmer()
                binding.sflFV.visibility = View.GONE
            }

//            Log.w("detail>currentItem", "$currentItem")
            binding.tvFDExplain.text = currentItem.favoriteExplain.toString()
            val rvAdapter = ExerciseRVAdapter(this@FavoriteDetailFragment, currentItem.exercises!!, singletonInstance.viewingHistory?.toList() ?: listOf(),"main")
            rvAdapter.exerciseList = currentItem.exercises!!
            val linearLayoutManager2 =
                LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
            binding.rvFV.layoutManager = linearLayoutManager2
            binding.rvFV.adapter = rvAdapter

            var totalTime = 0
            for (i in 0 until currentItem.exercises!!.size) {
                val exercises = currentItem.exercises!![i]
//                Log.w("운동각 시간" ,"${exercises.videoDuration!!.toInt()}")
                totalTime += exercises.videoDuration!!.toInt()
            }
            Log.w("총 시간", "$totalTime")
        }
    }

    private fun storeFavoriteUrl(viewModel : FavoriteViewModel) : MutableList<String> {
        val urls = mutableListOf<String>()
        val title = requireArguments().getString(ARG_TITLE).toString()
        val currentItem = viewModel.favoriteList.value?.find { it.favoriteName == title }
        Log.w("PreviousStoreURL", "$currentItem")
        for (i in 0 until currentItem!!.exercises!!.size) {
            val exercises = currentItem.exercises!![i]
            urls.add(exercises.videoFilepath.toString())
            Log.w("Finish?storeUrl", "$urls")
        }
        return  urls
    }
//    fun getBitMapFromView(view: View) : Bitmap {
//        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
//        val canvas = Canvas(bitmap)
//        view.draw(canvas)
//        return bitmap
//    }
}