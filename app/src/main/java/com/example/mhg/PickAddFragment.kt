package com.example.mhg

import android.annotation.SuppressLint
import android.content.ContentValues.TAG
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mhg.Adapter.HomeVerticalRecyclerViewAdapter
import com.example.mhg.VO.ExerciseViewModel
import com.example.mhg.VO.PickItemVO
import com.example.mhg.databinding.FragmentPickAddBinding
import com.example.mhg.`object`.NetworkExerciseService
import com.example.mhg.`object`.NetworkExerciseService.insertPickItemJson
import com.example.mhg.`object`.Singleton_t_user
import com.google.gson.Gson
import kotlinx.coroutines.launch
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


        // -----! 즐겨찾기 하나 만들기 시작 !-----
        binding.btnPickAddExercise.setOnClickListener {
            val pickItemVO = PickItemVO(
                pickName = binding.etPickAddName.text.toString(),
                pickExplainTitle = binding.etPickAddExplainTitle.text.toString(),
                pickExplain = binding.etPickAddExplain.text.toString(),
                pickDisclosure = when {
                    binding.clPickAddPublic.visibility == View.VISIBLE -> "public"
                    binding.clPickAddUnlisted.visibility == View.VISIBLE -> "unlisted"
                    else -> "private"
                },
                exercises = viewModel.exerciseUnits.value?.toMutableList()
            )
            // -----! 나중에Detail에서 꺼내볼 vm 만들기 !-----
            viewModel.pickItems.value?.add(pickItemVO)

            val appClass = requireContext().applicationContext as AppClass
            appClass.pickItem = pickItemVO
            appClass.pickList.value?.add(appClass.pickItem.pickName.toString())
            appClass.pickItems.value?.add(appClass.pickItem)


            // -----! json으로 형식을 변환 !-----

            val jsonObj = JSONObject()
            jsonObj.put("favorite_name", pickItemVO.pickName)
            jsonObj.put("favorite_explain_title", pickItemVO.pickExplainTitle)
            jsonObj.put("favorite_explain", pickItemVO.pickExplain)

            jsonObj.put("user_mobile", t_userData.jsonObject?.optString("user_mobile"))
            Log.w("즐겨찾기 하나 만들기", "$jsonObj")
            viewModel.pickItem.value = jsonObj

            // TODO 기능 구현을 위한 하드 코딩 (지워야 함)
            viewModel.addPick(jsonObj.optString("pickName"), "")

            insertPickItemJson(getString(R.string.IP_ADDRESS_t_favorite),jsonObj.toString()) {
                requireActivity().supportFragmentManager.beginTransaction().apply {
                    replace(R.id.flPick, PickDetailFragment.newInstance(pickItemVO.pickName.toString()))
                    Log.w("$TAG, title", pickItemVO.pickName.toString())
                    commit()
                }
            }

            // -----! 즐겨찾기 하나 만들기 끝 !-----


            // -----! 운동 만들기 버튼 클릭 끝 !-----
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