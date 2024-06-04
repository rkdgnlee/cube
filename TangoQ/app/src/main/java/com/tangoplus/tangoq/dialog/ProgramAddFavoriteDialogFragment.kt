package com.tangoplus.tangoq.dialog

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.tangoplus.tangoq.R
import com.tangoplus.tangoq.adapter.ExerciseRVAdapter
import com.tangoplus.tangoq.adapter.FavoriteRVAdapter
import com.tangoplus.tangoq.data.ExerciseVO
import com.tangoplus.tangoq.data.FavoriteVO
import com.tangoplus.tangoq.data.FavoriteViewModel
import com.tangoplus.tangoq.data.ProgramVO
import com.tangoplus.tangoq.databinding.FragmentProgramAddFavoriteDialogBinding
import com.tangoplus.tangoq.listener.BasketItemTouchListener
import com.tangoplus.tangoq.listener.OnFavoriteDetailClickListener
import com.tangoplus.tangoq.listener.OnFavoriteSelectedClickListener
import com.tangoplus.tangoq.`object`.NetworkExercise
import com.tangoplus.tangoq.`object`.NetworkFavorite
import com.tangoplus.tangoq.`object`.NetworkFavorite.fetchFavoriteItemJsonBySn
import com.tangoplus.tangoq.`object`.NetworkFavorite.fetchFavoriteItemsJsonByMobile
import com.tangoplus.tangoq.`object`.NetworkFavorite.updateFavoriteItemJson
import com.tangoplus.tangoq.`object`.Singleton_t_user
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject


class ProgramAddFavoriteDialogFragment : DialogFragment(), BasketItemTouchListener,
    OnFavoriteDetailClickListener, OnFavoriteSelectedClickListener {
    lateinit var binding : FragmentProgramAddFavoriteDialogBinding
    val viewModel : FavoriteViewModel by activityViewModels()
    private lateinit var adapter: ExerciseRVAdapter
    private lateinit var program: ProgramVO

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentProgramAddFavoriteDialogBinding.inflate(inflater)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.favoriteList.value = mutableListOf()
        binding.nsvPAV.isNestedScrollingEnabled = false
        binding.rvPAV.isNestedScrollingEnabled = false
        binding.rvPAV.overScrollMode = 0

        // ------! program 데이터 bundle, singleton에서 전화번호 가져오기 시작 !------
        val t_userData = Singleton_t_user.getInstance(requireContext()).jsonObject?.optJSONObject("data")
        val user_mobile = t_userData?.optString("user_mobile")

        val bundle = arguments


        program = bundle?.getParcelable<ProgramVO>("Program")!!
        // ------! program 데이터 bundle, singleton에서 전화번호 가져오기 끝 !------


        binding.ibtnPAVBack.setOnClickListener {
            dismiss()
        }

        // ------! 먼저 즐겨찾기 목록을 보여주기 !------
        lifecycleScope.launch {
            val favoriteList = fetchFavoriteItemsJsonByMobile(getString(R.string.IP_ADDRESS_t_favorite), user_mobile.toString())

            if (favoriteList != null) {
                for (i in favoriteList.indices) {
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
                                exerciseUnits.add(NetworkExercise.jsonToExerciseVO(exercises.get(j) as JSONObject))
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
                linkFavoriteAdapter(viewModel.favoriteList.value!!)
            } // -----! appClass list관리 끝 !-----

            // ------! 버튼 text로 finish 단계 감지 시작 !------

            binding.btnPAVFinish.setOnClickListener {
                when (binding.btnPAVFinish.text) {
                    "운동 고르기" -> {
                        binding.btnPAVFinish.text = "완료하기"
                        linkBaksetAdapter(program.exercises!!)
                    }
                    "완료하기" -> {
                        val selectedItems = viewModel.getExerciseBasketUnit()
                        Log.v("selectedItems", "${selectedItems}")
                        viewModel.addExercises(selectedItems)
                        val exerciseIds = mutableListOf<Int>()

                        val updatedExercises = viewModel.selectedFavorite.exercises?.plus(selectedItems)
                        for (i in updatedExercises!!.indices) {
                            updatedExercises[i].exerciseId.let {
                                exerciseIds.add(it!!.toInt())
                            }
                        }
                        Log.v("exerciseIds", "${exerciseIds}")
                        Log.v("viewModel.selectedFavorite", "${viewModel.selectedFavorite}")
                        val jsonObject = JSONObject()
                        jsonObject.put("exercise_ids", JSONArray(exerciseIds))
                        updateFavoriteItemJson(getString(R.string.IP_ADDRESS_t_favorite), viewModel.selectedFavorite.favoriteSn.toString(), jsonObject.toString()) {
                            requireActivity().runOnUiThread{
                                viewModel.allExercises.value = mutableListOf()
                            }
                            dismiss()
                        }
                    }
                }
            }
            // ------! 버튼 text로 finish 단계 감지 끝 !------
        }
        // ------! 프로그램의 exercise 받아와서 뿌리기 !------
    }

    override fun onResume() {
        super.onResume()
        // full Screen code
        dialog?.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog?.window?.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
    }

    private fun linkFavoriteAdapter(list : MutableList<FavoriteVO>) {
        val adapter = FavoriteRVAdapter(list,this@ProgramAddFavoriteDialogFragment,this@ProgramAddFavoriteDialogFragment, this@ProgramAddFavoriteDialogFragment, "add")

        binding.rvPAV.adapter = adapter
        val linearLayoutManager =
            LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
        binding.rvPAV.layoutManager = linearLayoutManager

    }

    private fun linkBaksetAdapter(list : MutableList<ExerciseVO>) {
        Log.v("program", "${program}")
        adapter = ExerciseRVAdapter(this@ProgramAddFavoriteDialogFragment,list,"basket")
        adapter.basketListener = this@ProgramAddFavoriteDialogFragment
        binding.rvPAV.adapter = adapter
        val linearLayoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
        binding.rvPAV.layoutManager = linearLayoutManager
        viewModel.allExercises.value = program.exercises
    }
    override fun onBasketItemQuantityChanged(descriptionId: String, newQuantity: Int) {
        val exercise = viewModel.allExercises.value?.find { it.exerciseId.toString() == descriptionId }
        if (exercise != null) {
            viewModel.addExerciseBasketUnit(exercise, newQuantity)
        }
        viewModel.setQuantity(descriptionId, newQuantity)
        Log.w("장바구니viewmodel", "desId: ${viewModel.exerciseBasketUnits.value?.find { it.exerciseId.toString() == descriptionId }?.exerciseId}, 횟수: ${viewModel.exerciseBasketUnits.value?.find { it.exerciseId.toString() == descriptionId }?.quantity}")
    }

    override fun onFavoriteClick(title: String) { }

    override fun onFavoriteSelected(favoriteVO: FavoriteVO) {
        viewModel.selectedFavorite = favoriteVO
        Log.v("selectedFavorite", "selectedFavorite: ${viewModel.selectedFavorite}")
    }

}