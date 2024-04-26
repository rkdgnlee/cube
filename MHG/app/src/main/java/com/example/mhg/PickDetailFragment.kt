package com.example.mhg

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ContentValues.TAG
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
import com.example.mhg.Adapter.HomeVerticalRecyclerViewAdapter
import com.example.mhg.VO.ExerciseVO
import com.example.mhg.VO.ExerciseViewModel
import com.example.mhg.VO.PickItemVO
import com.example.mhg.databinding.FragmentPickDetailBinding
import com.example.mhg.`object`.NetworkExerciseService
import com.example.mhg.`object`.NetworkExerciseService.jsonToExerciseVO
import com.example.mhg.`object`.NetworkExerciseService.jsonToPickItemVO
import com.example.mhg.`object`.Singleton_t_user
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch
import org.json.JSONObject


class PickDetailFragment : Fragment() {
    lateinit var binding : FragmentPickDetailBinding
    val viewModel : ExerciseViewModel by activityViewModels()
    var title = ""

    private lateinit var startForResult: ActivityResultLauncher<Intent>
    companion object {
        private const val ARG_TITLE = "title"
        fun newInstance(title: String): PickDetailFragment {
            val fragment = PickDetailFragment()
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
        binding = FragmentPickDetailBinding.inflate(inflater)
        return binding.root

    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.nsvPickDetail.isNestedScrollingEnabled = true
        binding.rvPickDetail.isNestedScrollingEnabled = false
        binding.rvPickDetail.overScrollMode = View.OVER_SCROLL_NEVER

        title = requireArguments().getString(ARG_TITLE).toString()
        val currentSn = viewModel.favoriteList.value?.find { it.second == title }?.first.toString()
        binding.actPickDetail.setText(title)
//        val appClass = requireContext().applicationContext as AppClass

        // -----! singleton에서 전화번호 가져오기 시작 !-----
        val t_userData = Singleton_t_user.getInstance(requireContext())
        val user_mobile = t_userData.jsonObject?.optString("user_mobile")
        // -----! singleton에서 전화번호 가져오기 끝 !-----

        // -----! 운동 picklist, 제목 가져오기 시작 !-----
        // TODO 여기서 받아온 거 전처리, JSONObject에서 exercise만 접근할 건지.
        lifecycleScope.launch {
            binding.sflPickDetail1.startShimmer()
            binding.sflPickDetail2.startShimmer()
            binding.sflPickDetail3.startShimmer()
            binding.sflPickDetail4.startShimmer()
            binding.ivPickDetailNull.visibility = View.GONE
            val snData = NetworkExerciseService.fetchFavoriteItemJsonBySn(getString(R.string.IP_ADDRESS_t_favorite), currentSn)
            val pickItem = if (snData != null) jsonToPickItemVO(snData) else PickItemVO(0, "", "", "", "", mutableListOf())

            val currentItem = viewModel.favoriteItems.value?.find { it.favoriteName == title }
            if (currentItem != null) {
                currentItem.exercises = pickItem.exercises
            }
            viewModel.favoriteItems.value = viewModel.favoriteItems.value

            if (currentItem?.exercises!!.isEmpty()) {
                binding.sflPickDetail1.stopShimmer()
                binding.sflPickDetail2.stopShimmer()
                binding.sflPickDetail3.stopShimmer()
                binding.sflPickDetail4.stopShimmer()
                binding.sflPickDetail1.visibility = View.GONE
                binding.sflPickDetail2.visibility = View.GONE
                binding.sflPickDetail3.visibility = View.GONE
                binding.sflPickDetail4.visibility = View.GONE
                binding.llPickDetail.visibility = View.GONE
            } else {
                binding.sflPickDetail1.stopShimmer()
                binding.sflPickDetail2.stopShimmer()
                binding.sflPickDetail3.stopShimmer()
                binding.sflPickDetail4.stopShimmer()
                binding.sflPickDetail1.visibility = View.GONE
                binding.sflPickDetail2.visibility = View.GONE
                binding.sflPickDetail3.visibility = View.GONE
                binding.sflPickDetail4.visibility = View.GONE
                binding.llPickDetail.visibility = View.VISIBLE
            }

            val pickList = mutableListOf<Pair<Int, String>>()
            viewModel.favoriteList.observe(viewLifecycleOwner) { Array ->
                pickList.clear()
                for (i in 0 until Array.size) {
                    pickList.add(Array[i])
                }
                setPickDetail(currentItem)
            }
            val adapter = ArrayAdapter(requireContext(), androidx.appcompat.R.layout.support_simple_spinner_dropdown_item, pickList.map { it.second })
            binding.actPickDetail.setAdapter(adapter)
            binding.actPickDetail.addTextChangedListener(object: TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {  }
                override fun afterTextChanged(s: Editable?) {
                    title = s.toString()
                    setPickDetail(currentItem)
                }
            })

            // -----! 운동 url list 만들기 시작 !-----
            binding.btnPickStart.setOnClickListener {
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
            } // -----! 운동 url list 만들기 끝 !-----
        }
        // ----- 운동 picklist, 제목 가져오기 끝 -----

        binding.btnPickDetailBack.setOnClickListener {
            if (!it.isClickable) { return@setOnClickListener }
            it.isClickable = false
            requireActivity().supportFragmentManager.beginTransaction().apply {
                replace(R.id.flMain, PickFragment())
                commit()
            }
            it.isClickable = true
        }
        // -----! 편집 버튼 시작 !-----
        binding.btnPickDetailGoEdit.setOnClickListener {
            requireActivity().supportFragmentManager.beginTransaction().apply {
                setCustomAnimations(R.anim.slide_in_left, R.anim.slide_in_right)
                replace(R.id.flMain, PickEditFragment.newInstance(title))


                remove(PickDetailFragment()).commit()
            }
            requireContext()
        } // -----! 편집 버튼 끝 !-----
    }
    @SuppressLint("NotifyDataSetChanged")
     private fun setPickDetail(currentItem: PickItemVO){
        Log.w("detail>currentItem", "$currentItem")
        binding.tvPickDetailExplainTitle.text = currentItem.favoriteExplain.toString()
        binding.tvPickDetailExplain.text = currentItem.favoriteExplain.toString()
        val RvAdapter = HomeVerticalRecyclerViewAdapter(currentItem.exercises!!, "home")
        RvAdapter.verticalList = currentItem.exercises!!
        val linearLayoutManager2 =
            LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
        binding.rvPickDetail.layoutManager = linearLayoutManager2
        binding.rvPickDetail.adapter = RvAdapter
        RvAdapter.notifyDataSetChanged()

        binding.tvPickDetailUnitNumber.text = currentItem.exercises!!.size.toString()

        var totalTime = 0
        for (i in 0 until currentItem.exercises!!.size) {
            val exercises = currentItem.exercises!!.get(i)
            Log.w("운동각 시간" ,"${exercises.videoTime!!.toInt()}")
            totalTime += exercises.videoTime!!.toInt()
        }
        Log.w("총 시간", "$totalTime")
        binding.tvPickDetailUnitTime.text = (totalTime.div(60)).toString()
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

//    private fun jsontoPickItemVO(jsonObj : JSONObject) : PickItemVO {
//        val exerciseUnits = mutableListOf<ExerciseVO>()
//        val exercises = jsonObj.optJSONArray("exercise_detail_data")
//        if (exercises != null) {
//            for (i in 0 until exercises.length()) {
//                exerciseUnits.add(jsonToExerciseVO(exercises.get(i) as JSONObject))
//            }
//        }
//        val pickItemVO = PickItemVO(
//            pickName = jsonObj.optString("favorite_name"),
//            pickExplainTitle = jsonObj.optString("favorite_description"),
//            pickExplain = jsonObj.optString("favorite_description"),
//            exercises = exerciseUnits
//        )
//        Log.w("jsonObj>pickItemVO", "$pickItemVO")
//        return pickItemVO
//    }
}
