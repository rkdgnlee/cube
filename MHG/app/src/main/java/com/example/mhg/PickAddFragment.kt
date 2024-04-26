package com.example.mhg

import android.annotation.SuppressLint
import android.content.ContentValues.TAG
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import com.example.mhg.VO.ExerciseViewModel
import com.example.mhg.VO.PickItemVO
import com.example.mhg.databinding.FragmentPickAddBinding
import com.example.mhg.`object`.NetworkExerciseService.insertFavoriteItemJson
import com.example.mhg.`object`.Singleton_t_user
import com.google.android.material.snackbar.Snackbar
import org.json.JSONObject


class PickAddFragment : Fragment() {
   lateinit var binding : FragmentPickAddBinding
    val viewModel: ExerciseViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentPickAddBinding.inflate(inflater)
        binding.clPickAddUnlisted.visibility = View.GONE
        binding.clPickAddPrivate.visibility = View.GONE

        return binding.root
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val t_userData = Singleton_t_user.getInstance(requireContext())
        var favoriteNameValidation = false
        // -----! 즐겨찾기 하나 만들기 시작 !-----
        binding.etPickAddName.addTextChangedListener(object :TextWatcher{
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                favoriteNameValidation = if (s?.length!! >= 4) true else false
                }
        })

        binding.btnPickAddExercise.setOnClickListener {
            if (favoriteNameValidation) {
                val pickItemVO = PickItemVO(
                    favoriteName = binding.etPickAddName.text.toString(),
                    favoriteExplainTitle = binding.etPickAddExplainTitle.text.toString(),
                    favoriteExplain = binding.etPickAddExplain.text.toString(),
                    favoriteDisclosure = when {
                        binding.clPickAddPublic.visibility == View.VISIBLE -> "public"
                        binding.clPickAddUnlisted.visibility == View.VISIBLE -> "unlisted"
                        else -> "private"
                    },
                    exercises = viewModel.exerciseUnits.value?.toMutableList()
                )
                // -----! 나중에Detail에서 꺼내볼 vm 만들기 !-----
//                val appClass = requireContext().applicationContext as AppClass
                if (pickItemVO.favoriteName?.isNotEmpty() == true) {
                    viewModel.favoriteItem.value = pickItemVO
                    viewModel.favoriteList.value?.add(Pair(viewModel.favoriteItem.value!!.favoriteSn, viewModel.favoriteItem.value!!.favoriteName.toString()))
                    viewModel.favoriteItems.value?.add(pickItemVO)
                }
                // -----! json으로 형식을 변환 !-----

                val jsonObj = JSONObject()
                jsonObj.put("favorite_name", pickItemVO.favoriteName)
                jsonObj.put("favorite_description_title", pickItemVO.favoriteExplainTitle)
                jsonObj.put("favorite_description", pickItemVO.favoriteExplain)
                jsonObj.put("user_mobile", t_userData.jsonObject?.optString("user_mobile"))
                Log.w("즐겨찾기 하나 만들기", "$jsonObj")

//                viewModel.addPick(jsonObj.optString("pickName"), pickItemVO.pickSn.toString())

                insertFavoriteItemJson(getString(R.string.IP_ADDRESS_t_favorite),jsonObj.toString()) {
                    requireActivity().supportFragmentManager.beginTransaction().apply {
                        replace(R.id.flPick, PickDetailFragment.newInstance(pickItemVO.favoriteName.toString()))
                        Log.w("$TAG, title", pickItemVO.favoriteName.toString())
                        commit()
                    }
                }

                // -----! 즐겨찾기 하나 만들기 끝 !-----


                // -----! 운동 만들기 버튼 클릭 끝 !-----
            } else {
                val snackbar = Snackbar.make(requireView(), "제목을 입력해주세요 ! ", Snackbar.LENGTH_SHORT)
                snackbar.setAction("확인", object: View.OnClickListener {
                    override fun onClick(v: View?) {
                        snackbar.dismiss()
                    }
                })
                snackbar.setTextColor(Color.WHITE)
                snackbar.setActionTextColor(Color.WHITE)
                snackbar.show()
            }

        }


        // -----! 공개 설정 코드 시작 !-----
        var rangeExpanded = false
        binding.clPickAddPublic.setOnClickListener{
            if (!rangeExpanded) {
                binding.clPickAddUnlisted.visibility = View.VISIBLE
                binding.clPickAddPrivate.visibility = View.VISIBLE
                rangeExpanded = true
            } else {
                binding.clPickAddUnlisted.visibility = View.GONE
                binding.clPickAddPrivate.visibility = View.GONE
                rangeExpanded = false
            }
        }

        binding.clPickAddUnlisted.setOnClickListener{
            if (!rangeExpanded) {
                binding.clPickAddPublic.visibility = View.VISIBLE
                binding.clPickAddPrivate.visibility = View.VISIBLE
                rangeExpanded = true
            } else {
                binding.clPickAddPublic.visibility = View.GONE
                binding.clPickAddPrivate.visibility = View.GONE
                rangeExpanded = false
            }
        }
        binding.clPickAddPrivate.setOnClickListener{
            if (!rangeExpanded) {
                binding.clPickAddPublic.visibility = View.VISIBLE
                binding.clPickAddUnlisted.visibility = View.VISIBLE
                rangeExpanded = true
            } else {
                binding.clPickAddPublic.visibility = View.GONE
                binding.clPickAddUnlisted.visibility = View.GONE
                rangeExpanded = false
            }
        }
        // -----! 공개 설정 코드 끝 !-----

    }
}