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
import com.tangoplus.tangoq.`object`.NetworkFavorite.fetchFavoriteItemsJsonBySn
import com.tangoplus.tangoq.`object`.NetworkFavorite.updateFavoriteItemJson
import com.tangoplus.tangoq.`object`.Singleton_t_history
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
    private lateinit var singletonInstance: Singleton_t_history

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentProgramAddFavoriteDialogBinding.inflate(inflater)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        singletonInstance = Singleton_t_history.getInstance(requireContext())
        viewModel.favoriteList.value = mutableListOf()
        binding.nsvPAFD.isNestedScrollingEnabled = false
        binding.rvPAFD.isNestedScrollingEnabled = false
        binding.rvPAFD.overScrollMode = 0

        // ------! program 데이터 bundle, singleton에서 전화번호 가져오기 시작 !------
        val t_userData = Singleton_t_user.getInstance(requireContext()).jsonObject?.optJSONObject("login_data")
        val userSn = t_userData?.optString("user_sn")
        val bundle = arguments


        program = bundle?.getParcelable("Program")!!
        // ------! program 데이터 bundle, singleton에서 전화번호 가져오기 끝 !------


        // ------! 먼저 즐겨찾기 목록을 보여주기 !------
        lifecycleScope.launch {
            val favoriteList = fetchFavoriteItemsJsonBySn(getString(R.string.IP_ADDRESS_t_favorite), userSn.toString())
            val favoriteItems = mutableListOf<FavoriteVO>()
            if (favoriteList.isNotEmpty()) {
                binding.tvPAFDGuide.visibility = View.INVISIBLE

                for (i in favoriteList.indices) {

                    // ------! 1 favorite sn 목록가져오기 !------
                    val favoriteItem = fetchFavoriteItemJsonBySn(getString(R.string.IP_ADDRESS_t_favorite), favoriteList[i].favoriteSn.toString())

                    // ------! 2 운동 디테일 채우기 !------
                    favoriteItems.add(favoriteItem)
                    viewModel.favoriteList.value = favoriteItems
                }
            } else {
                binding.tvPAFDGuide.visibility = View.VISIBLE
            }


            viewModel.favoriteList.observe(viewLifecycleOwner) {
//                 아무것도 없을 때 나오는 캐릭터
                linkFavoriteAdapter(viewModel.favoriteList.value!!)
            } // -----! appClass list관리 끝 !-----


            binding.ibtnPAFDBack.setOnClickListener {
                if (binding.btnPAFDFinish.text == "운동 고르기") {
                    dismiss()
                } else {
                    binding.btnPAFDFinish.text = "운동 고르기"
                    linkFavoriteAdapter(viewModel.favoriteList.value!!)
                }
            }

            // ------! 즐겨찾기 눌렀을 때만 다음으로 넘어가게 하기

            viewModel.selectedFavorite.observe(viewLifecycleOwner) {
                if (it != null) {
                    binding.btnPAFDFinish.isEnabled = true
                } else {
                    binding.btnPAFDFinish.isEnabled = false
                }
                Log.v("selectedFavorite", "${viewModel.selectedFavorite}")
            }


            // ------! 버튼 text로 finish 단계 감지 시작 !------
            binding.btnPAFDFinish.setOnClickListener {
                when (binding.btnPAFDFinish.text) {
                    "운동 고르기" -> {
                        binding.btnPAFDFinish.text = "완료하기"
                        linkBaksetAdapter(program.exercises!!)
                    }
                    "완료하기" -> {
                        val selectedItems = viewModel.getExerciseBasketUnit()
                        Log.v("selectedItems", "${selectedItems}")
                        viewModel.addExercises(selectedItems)
                        val exerciseIds = mutableListOf<Int>()

                        val updatedExercises = viewModel.selectedFavorite.value?.exercises?.plus(selectedItems)
                        for (i in updatedExercises!!.indices) {
                            updatedExercises[i].exerciseId.let {
                                exerciseIds.add(it!!.toInt())
                            }
                        }
                        Log.v("exerciseIds", "${exerciseIds}")
                        Log.v("viewModel.selectedFavorite", "${viewModel.selectedFavorite}")
                        val jsonObject = JSONObject()
                        jsonObject.put("exercise_ids", JSONArray(exerciseIds))
                        updateFavoriteItemJson(getString(R.string.IP_ADDRESS_t_favorite), viewModel.selectedFavorite.value?.favoriteSn.toString(), jsonObject.toString(), requireContext()) {
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

        binding.rvPAFD.adapter = adapter
        val linearLayoutManager =
            LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
        binding.rvPAFD.layoutManager = linearLayoutManager

    }

    private fun linkBaksetAdapter(list : MutableList<ExerciseVO>) {
        Log.v("program", "${program}")
        adapter = ExerciseRVAdapter(this@ProgramAddFavoriteDialogFragment,list, singletonInstance.viewingHistory?.toList() ?: listOf(),"basket")
        adapter.basketListener = this@ProgramAddFavoriteDialogFragment
        binding.rvPAFD.adapter = adapter
        val linearLayoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
        binding.rvPAFD.layoutManager = linearLayoutManager
        viewModel.allExercises.value = program.exercises
    }
    override fun onBasketItemQuantityChanged(descriptionId: String, newQuantity: Int) {
        val exercise = viewModel.allExercises.value?.find { it.exerciseId.toString() == descriptionId }
        if (exercise != null) {
            viewModel.addExerciseBasketUnit(exercise, newQuantity)
        }
        viewModel.setQuantity(descriptionId, newQuantity)
        Log.w("장바구니viewModel", "desId: ${viewModel.exerciseBasketUnits.value?.find { it.exerciseId.toString() == descriptionId }?.exerciseId}, 횟수: ${viewModel.exerciseBasketUnits.value?.find { it.exerciseId.toString() == descriptionId }?.quantity}")
    }

    override fun onFavoriteClick(sn: Int) { }

    override fun onFavoriteSelected(favoriteVO: FavoriteVO) {
        viewModel.selectedFavorite.value = favoriteVO

    }

}